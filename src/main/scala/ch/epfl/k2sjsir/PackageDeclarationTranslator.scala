package ch.epfl.k2sjsir

import java.{util => ju}

import ch.epfl.k2sjsir.lower.SJSIRLower
import ch.epfl.k2sjsir.translate.{GenClass, GenExternalClass, GenFun, GenProperty}
import ch.epfl.k2sjsir.utils.{NameEncoder, Utils}
import org.jetbrains.kotlin.backend.jvm.lower.InterfaceLowering
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.js.facade.exceptions.TranslationRuntimeException
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.utils.BindingUtils._
import org.jetbrains.kotlin.js.translate.utils.{AnnotationsUtils, BindingUtils}
import org.jetbrains.kotlin.psi._
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types._
import org.scalajs.core.ir.{ClassKind, Definitions, Position}

import scala.collection.JavaConversions._
import scala.collection.immutable.{List, Nil}
import scala.collection.mutable


object PackageDeclarationTranslator {
  def translateFiles(files: ju.Collection[KtFile], context: TranslationContext): Unit = {
    new PackageDeclarationTranslator(files, context).translate()
  }
}

final class PackageDeclarationTranslator private(
                                                  val files: Iterable[KtFile],
                                                  override val context: TranslationContext
                                                ) extends AbstractTranslator(context) {
  private def translate(): Unit = {
    val output = context.getConfig.getConfiguration.get(CommonConfigurationKeys.MODULE_NAME)

    for (file <- files) {
      try {
        val topLevelFunctions = new mutable.MutableList[KtNamedFunction]()
        val topLevelValues = new mutable.MutableList[KtProperty]()
        val declarations = new SJSIRLower().lower(file)

        for (declaration <- declarations) {
          val predefinedObject = AnnotationsUtils.isPredefinedObject(BindingUtils.getDescriptorForElement(bindingContext, declaration))

          declaration match {
            case d: KtClassOrObject if !predefinedObject=>
              val genClass = GenClass(d)(context)

              val tree = genClass.tree
              val treeDefaultImpls: Option[ClassDef] = genClass.treeDefaultImpls
              val treeEnumCompanion: Option[ClassDef] = genClass.treeEnumCompanion

              val cd = getClassDescriptor(context.bindingContext(), d)

              SJSIRCodegen.genIRFile(output, cd, tree)
              treeDefaultImpls.foreach(SJSIRCodegen.genIRDefaultImpls(output, cd, _))
              treeEnumCompanion.foreach(SJSIRCodegen.genIREnumCompanion(output, cd, _))

            case d: KtClassOrObject =>
              val tree = GenExternalClass(d)(context).tree
              val cd = getClassDescriptor(context.bindingContext(), d)
              SJSIRCodegen.genIRFile(output, cd, tree)

            case f: KtNamedFunction =>
              topLevelFunctions += f

            case p: KtProperty =>
              topLevelValues += p

            case t => sys.error(s"Not implemented yet: ${t.getClass}")
          }
        }

        if (topLevelFunctions.nonEmpty || topLevelValues.nonEmpty) {
          val className = JvmFileClassUtil.getFileClassInfoNoResolve(file).getFileClassFqName.asString()
          val encodedName = NameEncoder.encodeClassName(className, "")

          implicit val pos = Position(Position.SourceFile(file.getName), 0, 0)

          val defs = topLevelFunctions.toList.map(x => GenFun(x)(context).tree)
          val vals = topLevelValues.map(x => GenProperty(x)(context).tree)
          val receiver = Utils.genThisFromContext(ClassType(encodedName))
          val ctor = MethodDef(false, Ident("init___"), Nil, NoType,
            Some(ApplyStatically(receiver, ClassType(Definitions.ObjectClass), Ident("init___"), Nil)(NoType)))(OptimizerHints.empty, None)
          val ctorAndDefs = ctor :: (defs ++ vals)

          val hasMain = defs.exists {
            case MethodDef(_, Ident("main__AT__V", _), _, _, _) => true
            case _ => false
          }


          // TODO: Remove after using option mm in Scalajsld (as well as TopLevelModuleExportDef)
          def manualExports(): List[MemberDef] = {
            val args = List(ArrayValue(ArrayType(ArrayTypeRef.of(ClassRef(Definitions.StringClass))), Nil))
            val body = Block(ApplyStatic(ClassType(encodedName), Ident("main__AT__V", Some("main"))(pos), args)(NoType)(pos), Undefined()(pos))(pos)
            val main = MethodDef(static = false, StringLiteral("main")(pos), Nil, AnyType, Some(body))(OptimizerHints.empty, None)(pos)
            List(main)
          }

          val cls =
            ClassDef(
              Ident(encodedName)(pos),
              ClassKind.ModuleClass,
              None,
              Some(Ident("O")(pos)),
              List(),
              None,
              None,
              ctorAndDefs ++ (if (hasMain) manualExports() else Nil),
              List(TopLevelModuleExportDef(className)(pos)))(OptimizerHints.empty)(pos)

          val name = encodedName.drop(1).replace("_", "/")
          SJSIRCodegen.genIRFile(output, name, cls)
        }
      } catch {
        case e: TranslationRuntimeException => throw e
        case e: RuntimeException => throw new TranslationRuntimeException(file, e)
        case e: AssertionError => throw new TranslationRuntimeException(file, e)
      }
    }
  }

}
