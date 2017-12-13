package ch.epfl.k2sjsir.translate

import ch.epfl.k2sjsir.SJSIRCodegen
import ch.epfl.k2sjsir.lower.SJSIRLower
import ch.epfl.k2sjsir.utils.Utils._
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.CodegenUtilKt
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.{ClassConstructorDescriptor, ClassDescriptor, DeclarationDescriptor}
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.js.translate.utils.BindingUtils._
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getPrimaryConstructorParameters
import org.jetbrains.kotlin.lexer.{KtModifierKeywordToken, KtTokens}
import org.jetbrains.kotlin.psi._
import org.jetbrains.kotlin.resolve.{DescriptorToSourceUtils, DescriptorUtils, FunctionDescriptorUtil}
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt._
import org.jetbrains.kotlin.types.KotlinType
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.{ClassType, NoType}
import org.scalajs.core.ir.{ClassKind, Trees}

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class GenClass(d: KtClassOrObject)(implicit val c: TranslationContext) extends IRNodeGen[KtClassOrObject, ClassDef] {

  private val desc = getClassDescriptor(c.bindingContext(), d)
  private val optimizerHints = OptimizerHints.empty
  private val kind = desc.toJsClassKind
  private val superClass = if(isInterface) None else Some(DescriptorUtilsKt.getSuperClassOrAny(desc))
  private val interfaces = getSuperInterfaces(desc).asScala

  // Body abstract = none
  // Interface superclass = none
  // Interface interfaces = list !
  override def tree: ClassDef = {
    val idt = desc.toJsClassIdent
    val jsNativeLoadSpec = None

    /*
    For interfaces :
      => there are no constructors, we therefore need to generate a DefaultImpl file which will
      contain all default implementations provided in the interface.
      These implementation take a class instance as parameter

    - Generate the complete class abstracted
    - Generate all implementations in separate file containing
      - static declarations of the implementations
      - first parameter should be instance of the interface
      - calls to this keyword matching the interface should be replaced by this instance
      - naming convention : <interface_name>$DefaultImpls

    For classes implementing interfaces :
    - Generate all overriden methods and variables as usual
    - Generate missing methods (from interfaces) by calling the default implementation of the interfaces

     */

    val defaultImpls = new ListBuffer[KtDeclaration]

    // Declarations from interfaces
    val inheritedDeclarations = _inheritedDeclarations(interfaces)
    // Declarations from the class
    val classDeclarations = d.getDeclarations.asScala

    // Overriden declarations inside the class
    val overridenDeclarations = classDeclarations.filter(x => x.hasModifier(KtTokens.OVERRIDE_KEYWORD))
      .map{
        case nf: KtNamedFunction => BindingUtils.getFunctionDescriptor(c.bindingContext(), nf).toJsName
        case p: KtProperty => BindingUtils.getPropertyDescriptor(c.bindingContext(), p).toJsName
      }.toSet

    // Missing interface declarations
    val missingInterfaceDeclarations = inheritedDeclarations.collect {
      case p: KtProperty =>
        val pDesc = BindingUtils.getPropertyDescriptor(c.bindingContext(), p)
        if (overridenDeclarations.contains(pDesc.toJsName))
          Seq()
        else
          Seq(p)

      case nf: KtNamedFunction =>
        val nfDesc = BindingUtils.getFunctionDescriptor(c.bindingContext(), nf)
        if (overridenDeclarations.contains(nfDesc.toJsName))
          Seq()
        else
          Seq(nf)
    }.flatten

    // TODO: Add missing declarations to the list of definitions
    val defs : List[MemberDef] = classDeclarations.collect {
      case p : KtProperty =>
        val prop = GenProperty(p)
        if (isInterface) {
          if (prop.hasDefinedAccessors)
            defaultImpls += p

          prop.withAbstractGetterAndSetter
        }
        else
          prop.withGetterAndSetter
      case _: KtClassInitializer | _: KtSecondaryConstructor | _: KtClassOrObject =>
        /**
          * This is a special case, all those element are either used in the constructor (for property init) or
          * by the lowering at the top level (nested classes for instance)
          * Nested classes are handle with lowering, but they remain as declaration in the top class
          */
        List()
      case decl =>
        val genDecl = GenDeclaration(decl)

        if (isInterface) {
          decl match {
            case nf: KtNamedFunction if nf.hasBody => defaultImpls += decl
            case _ => // Do nothing
          }

          Seq(genDecl.withAbstractBodies)
        }
        else
          Seq(genDecl.tree)
    }.flatten.toList

    val bridges = missingInterfaceDeclarations.collect {
      case nf: KtNamedFunction if !isInterface =>
        /*
         def myInterfaceMethodName(myInterfaceMethodArgs) {
          MyInterface$DefaultImpls.myInterfaceMethodName(this, myInterfaceMethodArgs)
         }
         */
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

    /**
      * By default constructor parameters are local to constructor, if they are marked as val we need to create a
      * property to export them to the rest of the class
      */
    val paramsInit = d.getPrimaryConstructorParameters.asScala.flatMap { param =>
      val propDesc = getPropertyDescriptorForConstructorParameter(c.bindingContext(), param)

      if (propDesc == null)
        Seq()
      else {
        val isMutable = propDesc.getSetter != null

        if (propDesc != null) {
          val name = propDesc.toJsIdent
          val tpe = propDesc.getType.toJsType

          val fd = Seq(FieldDef(static = false, name, tpe, mutable = isMutable))
          val g = Seq(GenProperty.getter(propDesc))
          val s = if (isMutable) Seq(GenProperty.setter(propDesc)) else Seq()

          fd ++ g ++ s

        } else {
          Seq()
        }
      }
    }.toList

    val constructors = genConstructors
    val allDefs = paramsInit ++ defs ++ bridges ++ constructors

    val sprCls = superClass.fold(None: Option[Trees.Ident])(x => Some(x.toJsClassIdent))

    // Generate SJSIR files if necessary
    // TODO: Refactor to move this generation to the PackageDeclarationTranslator
    genDefaultImpls(defaultImpls.toList)

    val classDef = ClassDef(idt, kind, None, sprCls, interfaces.map(_.toJsClassIdent).toList, None, jsNativeLoadSpec, allDefs, Nil)(optimizerHints)

    classDef
  }

  private def genConstructors : Seq[MethodDef] = {
    val constructors = d.getSecondaryConstructors.asScala.map(genSecondaryConstructor).toList
    genPrimaryConstructor.toList ++ constructors
  }

  private def genSecondaryConstructor(k: KtSecondaryConstructor) : MethodDef = {
      val callSuper = {
        val delegationCall = CallUtilKt.getResolvedCall(k.getDelegationCall, c.bindingContext())
        val callDesc = delegationCall.getResultingDescriptor
        val args = delegationCall.getValueArgumentsByIndex.asScala
          .map(x => GenExpr(x.getArguments.get(0).getArgumentExpression).tree).toList
        val receiver = genThisFromContext(desc.toJsClassType)
        ApplyStatically(receiver, desc.toJsClassType, callDesc.toJsMethodIdent, args)(NoType)
      }

      val constrDesc = getDescriptorForElement(c.bindingContext(), k).asInstanceOf[ClassConstructorDescriptor]

      val body = Block(callSuper :: GenBody(k.getBodyExpression).treeOption.toList)
      val args = constrDesc.getValueParameters.asScala.map(_.toJsParamDef).toList
      MethodDef(static = false, constrDesc.toJsMethodIdent, args, NoType, Some(body))(optimizerHints, None)
  }

  private def genPrimaryConstructor : Option[MethodDef] = {
    /**
      * Declarations (for properties) must be in the primary constructors
      * Secondary constructors will call primary one
      */

    /**
      * If primary constructor params are marked as 'val', we have to create a class field to "export" them
      * out of the constructor scope, this can only be done in primary constructors
      */
    val paramsInit = getPrimaryConstructorParameters(d)
      .asScala
      .map(getPropertyDescriptorForConstructorParameter(c.bindingContext(), _))
      .filter(_ != null)
      .map(p => {
        val name =  p.toJsIdent
        val tpe = p.getType.toJsType
        val rcv = genThisFromContext(desc.toJsClassType)

        Assign(Select(rcv, name)(tpe), VarRef(name)(tpe))
      }).toList

    val declsInit: List[Tree] = d.getDeclarations.asScala
        .collect {
          case p: KtProperty if p.hasDelegateExpressionOrInitializer =>
            val propDesc = BindingUtils.getPropertyDescriptor(c.bindingContext(), p)
            val initExpr = p.getDelegateExpressionOrInitializer
            val expr = GenExpr(initExpr).tree
            val rcv = genThisFromContext(desc.toJsClassType)

            Assign(Select(rcv, Ident(p.getName))(propDesc.getType.toJsType), expr)

          case i: KtClassInitializer =>

            GenBody(i.getBody).tree
    }.toList

    val superCall = Option(getSuperCall(c.bindingContext(), d)) match {
      case Some(call) =>
        val name =  call.getResultingDescriptor.asInstanceOf[ClassConstructorDescriptor].toJsMethodIdent
        val params = call.getValueArgumentsByIndex.asScala
          .map(x => GenExpr(x.getArguments.get(0).getArgumentExpression).tree)
        val tpe = superClass.get.toJsClassType
        val rcv = genThisFromContext(tpe)
        ApplyStatically(rcv, tpe, name, params.toList)(NoType)
      case None => // We have no superclass, hence we need to call init on Object
        val o = ClassType("O")
        ApplyStatically(genThisFromContext(o), o, Ident("init___"), List())(NoType)
    }

    val primary = desc.getUnsubstitutedPrimaryConstructor
    if(primary != null) {
      val args = primary.getValueParameters.asScala.map(_.toJsParamDef).toList

      val stats = Block(superCall :: paramsInit ++ declsInit)
      Some(MethodDef(static = false, primary.toJsMethodIdent, args, NoType, Some(stats))(optimizerHints, None))
    } else None
  }

  private def genDefaultImpls(impls: List[KtDeclaration]): Seq[ClassDef] = {
    if (impls.isEmpty) return Seq()

    val kind = ClassKind.Class
    val name = desc.toJsDefaultImplIdent
    val superClass = Some(Ident("O"))
    val defs: List[MemberDef] = impls.collect {
      case p: KtProperty => GenProperty(p).withGetterAndSetter
      case decl => Seq(GenDeclaration(decl).tree)
    }.flatten

    val classDef = ClassDef(name, kind, None, superClass, List(), None, None, defs, Nil)(OptimizerHints.empty)

    val n = name.name.drop(1).replace("_", "/")
    val output = c.getConfig.getConfiguration.get(CommonConfigurationKeys.MODULE_NAME)
    SJSIRCodegen.genIRFile(output, n, classDef)

    Seq(classDef)
  }

  // Store declarations contained in the interfaces
  private def _inheritedDeclarations(cd: mutable.Buffer[ClassDescriptor], acc: Set[String] = Set()): Seq[KtDeclaration] = cd.flatMap {
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
                val repl = nfDesc.getContainingDeclaration.asInstanceOf[ClassDescriptor].toJsClassName
                //TODO: Replace this dirty code by a more generic method --> use another descriptor ?
                !acc.contains(nfDesc.toJsMethodIdent.name.replaceFirst(repl + "__", ""))
            }
          else
            Seq()
      }.flatten

      res ++ _inheritedDeclarations(getSuperInterfaces(i).asScala, acc ++ res.map{
        case p: KtProperty => getPropertyDescriptor(c.bindingContext(), p).toJsName
        case nf: KtNamedFunction =>
          val nfDesc = getFunctionDescriptor(c.bindingContext(), nf)
          val repl = nfDesc.getContainingDeclaration.asInstanceOf[ClassDescriptor].toJsClassName
          nfDesc.toJsMethodIdent.name.replaceFirst(repl + "__", "")
      })
    }
  }

  private def isInterface: Boolean = {
    ClassKind.Interface == kind
  }
}
