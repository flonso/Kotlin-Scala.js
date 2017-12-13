package ch.epfl.k2sjsir.utils

import ch.epfl.k2sjsir.translate.{GenDeclaration, GenProperty}
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi._
import org.scalajs.core.ir.Trees._

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import Utils._
import ch.epfl.k2sjsir.lower.SJSIRLower
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.{getFunctionDescriptor, getPropertyDescriptor}
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.{DescriptorToSourceUtils, DescriptorUtils}
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt
import org.scalajs.core.ir.Position

object GenClassUtils {

  implicit class KtClassOrObjectHelper(clsOrObj: KtClassOrObject)(implicit val c: TranslationContext, pos: Position) {

    private val desc = BindingUtils.getClassDescriptor(c.bindingContext(), clsOrObj)

    /**
      * Retrieves a list of all inherited declarations from super interfaces
      *
      * @return
      */
    def getInterfaceInheritedDeclarations: List[KtDeclaration] =
      _inheritedDeclarations(DescriptorUtilsKt.getSuperInterfaces(desc).asScala.toList, Set())

    /**
      * Builds a set containing signatures of all overridden methods in the current class
      *
      * @return A set of String of the overridden methods signatures
      */
    def getOverriddenDeclarations: Set[String] = {
      clsOrObj.getDeclarations.asScala
        .filter(x => x.hasModifier(KtTokens.OVERRIDE_KEYWORD))
        .map {
          case nf: KtNamedFunction =>
            BindingUtils.getFunctionDescriptor(c.bindingContext(), nf).toJsName

          case p: KtProperty =>
            BindingUtils.getPropertyDescriptor(c.bindingContext(), p).toJsName

        }.toSet
    }

    /**
      * Retrieves a list of all missing declarations provided in the super interfaces
      * (implicitly this means that theses declarations have a default implementation)
      *
      * @return The list of missing declarations
      */
    def getMissingInterfaceDeclarations: List[KtDeclaration] = {
      lazy val overriddenDeclarations = clsOrObj.getOverriddenDeclarations

      clsOrObj.getInterfaceInheritedDeclarations.collect {
        case p: KtProperty =>
          val pDesc = BindingUtils.getPropertyDescriptor(c.bindingContext(), p)
          if (overriddenDeclarations.contains(pDesc.toJsName))
            Nil
          else
            List(p)

        case nf: KtNamedFunction =>
          val nfDesc = BindingUtils.getFunctionDescriptor(c.bindingContext(), nf)
          if (overriddenDeclarations.contains(nfDesc.toJsName))
            Nil
          else
            List(nf)

      }.flatten
    }

    /**
      * Retrieves all default implementations for an interface
      * @return The list of default declarations
      */
    def getDefaultImplementations: List[KtDeclaration] = {
      clsOrObj.getDeclarations.asScala.collect {
        case p: KtProperty =>
          if (desc.isInterface && p.hasDefinedAccessors) List(p)
          else Nil

        case _: KtClassInitializer | _: KtSecondaryConstructor | _: KtClassOrObject =>
          Nil

        case declaration =>
          if (desc.isInterface) {
            declaration match {
              case nf: KtNamedFunction if nf.hasBody => List(declaration)
              case _ => Nil
            }
          } else Nil

      }.flatten.toList
    }

    /**
      * Generates method bridges for classes with interfaces that provide default implementations
      *
      * For example :
      *
      * def myInterfaceMethodName(myInterfaceMethodArgs) {
      *   MyInterface$DefaultImpls.myInterfaceMethodName(this, myInterfaceMethodArgs)
      * }
      *
      * @return A list of MethodDef bridges
      */
    def getInterfaceBridges: List[MethodDef] = {
      clsOrObj.getMissingInterfaceDeclarations.collect {
        case nf: KtNamedFunction if !desc.isInterface =>
          val methodDef = GenDeclaration(nf).tree match {

            case md@MethodDef(_, name, argsParamDef, resultType, _) =>
              val funDesc = BindingUtils.getFunctionDescriptor(c.bindingContext(), nf)
              val clsDesc = DescriptorUtils.getContainingClass(funDesc)

              val newName = Ident(name.encodedName.replaceFirst("__" + clsDesc.toJsClassName, ""))

              val newBody = {
                val methodIdent = funDesc.toJsMethodIdent
                val defaultImplCls = clsDesc.toJsDefaultImplType
                val cls = clsDesc.toJsClassType
                val self = genThisFromContext(cls)
                val args = self :: argsParamDef.tail.map(_.ref)
                ApplyStatic(defaultImplCls, methodIdent, args)(md.resultType)
              }

              MethodDef(static = false, newName, argsParamDef.tail, resultType, Option(newBody))(OptimizerHints.empty, None)
          }

          methodDef
      }
    }

    /**
      * Generates all MemberDef for the current class or object
      *
      * Note : KtClassInitializer, KtSecondaryConstructor and KtClassOrObject are skipped because they
      * are all either used in the constructor (for property init) or by the lowering at the top level (nested classes for instance).
      * Nested classes are handled with lowering, but they remain as declaration in the top class.
      *
      * @return A list of MemberDef corresponding to the member class or object declarations
      */
    def getMemberDefinitions: List[MemberDef] = {
      clsOrObj.getDeclarations.asScala.collect {
        case p : KtProperty =>
          GenProperty(p).withGetterAndSetter.toList

        case _: KtClassInitializer | _: KtSecondaryConstructor | _: KtClassOrObject =>
          Nil /* Skip */

        case declaration =>
          List(GenDeclaration(declaration).tree)

      }.flatten.toList
    }


    /**
      * Retrieves the list of all inherited declarations defined in interfaces
      *
      * @param cd A list of ClassDescriptor (the super interfaces)
      * @param acc A set containing all already added declarations (only keep first override)
      * @return A list of declarations
      */
    private def _inheritedDeclarations(cd: List[ClassDescriptor], acc: Set[String] = Set()): List[KtDeclaration] = cd.flatMap {
      i => {
        val file = DescriptorToSourceUtils.getContainingFile(i)
        val declarations = new SJSIRLower().lower(file)

        val res = declarations.collect {
          case d: KtClassOrObject =>
            val desc = BindingUtils.getClassDescriptor(c.bindingContext(), d)

            if (desc.toJsClassIdent == i.toJsClassIdent)
              d.getDeclarations.asScala.filter{
                case p: KtProperty =>
                  val pDesc = getPropertyDescriptor(c.bindingContext(), p)
                  !acc.contains(pDesc.toJsName)

                case nf: KtNamedFunction =>
                  val nfDesc = getFunctionDescriptor(c.bindingContext(), nf)
                  !acc.contains(nfDesc.toJsBridgeIdent.name)

                case _: KtClassInitializer | _: KtSecondaryConstructor | _: KtClassOrObject =>
                  false
              }
            else
              Seq()

        }.flatten

        res ++ _inheritedDeclarations(DescriptorUtilsKt.getSuperInterfaces(i).asScala.toList, acc ++ res.map{
          case p: KtProperty =>
            getPropertyDescriptor(c.bindingContext(), p).toJsName

          case nf: KtNamedFunction =>
            val nfDesc = getFunctionDescriptor(c.bindingContext(), nf)
            nfDesc.toJsBridgeIdent.name

        })
      }
    }
  }
}
