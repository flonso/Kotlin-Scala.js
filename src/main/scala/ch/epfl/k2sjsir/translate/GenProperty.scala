package ch.epfl.k2sjsir.translate

import ch.epfl.k2sjsir.utils.Utils._
import org.jetbrains.kotlin.descriptors.{ClassKind, PropertyDescriptor, PropertyGetterDescriptor, PropertySetterDescriptor}
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.{KtProperty, KtPropertyAccessor}
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils._
import org.scalajs.core.ir.Position
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.{AnyType, NoType, StringType, Type}

case class GenProperty(d: KtProperty)(implicit val c: TranslationContext) extends IRNodeGen[KtProperty, FieldDef] {

  import GenProperty._

  val desc = BindingUtils.getPropertyDescriptor(c.bindingContext(), d)


  override def tree: FieldDef = {
    val idt = desc.toJsIdent
    val static = DescriptorUtils.isStaticDeclaration(desc)

    val tpe = desc.getType.toJsType

    FieldDef(static, idt, tpe, desc.isVar)
  }

  def withGetterAndSetter: Seq[MemberDef] = {
    val t = {
      if (d.isVar || d.hasInitializer)
        Seq(tree)
      else
        Seq()
    }

    t ++ genGetterAndSetter(isAbstract = false)
  }

  def withOnlyGetterAndSetter: Seq[MethodDef] = {
    genGetterAndSetter(isAbstract = false)
  }

  def withAbstractGetterAndSetter: Seq[MethodDef] = {
    genGetterAndSetter(isAbstract = true)
  }

  private def genGetterAndSetter(isAbstract: Boolean): Seq[MethodDef] = {
    /*
    1. Do not generate a FieldDef if there is no body (this has to be done inside the implementing class)
    2. Generate in every case an (abstract) getter (and an (abstract) setter if it's a var)
    3. In GenClass create a DefaultImpls file containing all defined implementations
     */

    val g = Seq(getter(d, isAbstract))

    val s = if (d.isVar) Seq(setter(d, isAbstract)) else Seq()

    g ++ s
  }

  def hasGetterImpl = d.getGetter != null

  def hasSetterImpl = d.isVar && d.getSetter != null

  def hasDefinedAccessors: Boolean = hasGetterImpl || hasSetterImpl

}

object GenProperty {

  /**
    * If no getter was defined, generates the default implementation.
    * Otherwise generates a getter provided by the user, for example :
    * val name: String
    *  get() = "some value for name"
    *
    */
  private def getter(p: KtProperty, isAbstract: Boolean = false)(implicit c: TranslationContext, pos: Position): MethodDef = {
    val propAccessor = p.getGetter
    val desc = BindingUtils.getPropertyDescriptor(c.bindingContext(), p)
    val propGetterDesc = desc.getGetter

    getter(propAccessor, desc, propGetterDesc, isAbstract)
  }

  private[translate] def getter(desc: PropertyDescriptor)(implicit c: TranslationContext, pos: Position): MethodDef = {
    val propertyAccessor = null
    val propertyGetterDescriptor = desc.getGetter

    getter(propertyAccessor, desc, propertyGetterDescriptor, false)
  }

  private def getter(propAccessor: KtPropertyAccessor, desc: PropertyDescriptor, propGetterDesc: PropertyGetterDescriptor, isAbstract: Boolean)(implicit c: TranslationContext, pos: Position): MethodDef = {
    assert(propGetterDesc != null)

    val rtpe = desc.getReturnType.toJsType
    val tpe = desc.getDispatchReceiverParameter.getType
    val cd = DescriptorUtils.getClassDescriptorForType(tpe)
    val methodIdent = propGetterDesc.toJsMethodIdent
    val static = isInterface(cd)

    val body = {
      if (!isAbstract) {

        if (propAccessor != null && propAccessor.hasBody)
          Option(GenBody(propAccessor.getBodyExpression).tree)
        else
          Option(genSelectForAccessor(desc))

      } else {
        None
      }
    }

    // FIXME: Dirty hack to pass instance of interface to the default implementation
    val args = {
      if (isInterface(cd))
        List(ParamDef(Ident("$this"), cd.toJsClassType, false, false))
      else
        List()
    }


    MethodDef(static, methodIdent, args, rtpe, body)(OptimizerHints.empty, None)
  }

  /**
    * Generate a default setter or a custom one, if defined by the user, such as :
    * var myVar: Int = 0
    *   set(value) {
    *     if (value >= 0)
    *       field = value
    *     else
    *       field = 0
    *   }
    */
  private def setter(p: KtProperty, isAbstract: Boolean = false)(implicit c: TranslationContext, pos: Position): MethodDef = {
    val propAccessor = p.getSetter
    val desc = BindingUtils.getPropertyDescriptor(c.bindingContext(), p)
    val propSetterDesc = desc.getSetter

    setter(propAccessor, desc, propSetterDesc, isAbstract)
  }

  private[translate] def setter(desc: PropertyDescriptor)(implicit c: TranslationContext, pos: Position): MethodDef = {
    val propertyAccessor = null
    val propertySetterDescriptor = desc.getSetter

    setter(propertyAccessor, desc, propertySetterDescriptor, false)
  }

  private def setter(propAccessor: KtPropertyAccessor, desc: PropertyDescriptor, propGetterDesc: PropertySetterDescriptor, isAbstract: Boolean)(implicit c: TranslationContext, pos: Position): MethodDef = {
    assert(propGetterDesc != null)

    val rtpe = NoType // Kotlin setters always return Unit
    val propTpe = desc.getType.toJsType
    val methodIdent = propGetterDesc.toJsMethodIdent
    val setterIdent = Ident(s"value")
    val params = List(ParamDef(setterIdent, desc.getType.toJsType, mutable = false, rest = false))
    val body = {
      if (!isAbstract) {
        if (propAccessor != null && propAccessor.hasBody)
          Option(GenBody(propAccessor.getBodyExpression).tree)
        else {
          // TODO: Define a method that generates the Select based on the desc
          Some(Assign(genSelectForAccessor(desc), VarRef(setterIdent)(propTpe)))
        }
      } else {
        None
      }
    }

    MethodDef(static = false, methodIdent, params, rtpe, body)(OptimizerHints.empty, None)
  }


  /**
   * Generate default getter
   */
  private[translate] def getter(f: PropertyGetterDescriptor)(implicit pos: Position): MethodDef = {

    val propDesc = f.getCorrespondingProperty
    val methodIdent = f.toJsMethodIdent

    val rtpe = propDesc.getReturnType.toJsType
    val tpe = getClassDescriptorForType(propDesc.getDispatchReceiverParameter.getType).toJsClassType

    val body = genSelectForAccessor(propDesc)

    MethodDef(static = false, methodIdent, List(), rtpe, Some(body))(OptimizerHints.empty, None)
  }

  private[translate] def setter(f: KtPropertyAccessor)(implicit c: TranslationContext, pos: Position): MethodDef = {
    val body = GenBody(f.getBodyExpression).tree
    val ident = BindingUtils.getPropertyDescriptor(c.bindingContext(), f.getProperty)
    val rtpe = body.tpe

    val params = f.getNode().findChildByType(KtStubElementTypes.VALUE_PARAMETER_LIST).getChildren(null).map(
      p =>
        ParamDef(Ident(p.getText), ident.getType.toJsType, mutable = false, rest = false)
    ).toList

    MethodDef(static = false, ident.getSetter.toJsMethodIdent, params, rtpe, Some(body))(OptimizerHints.empty, None)
  }


  /**
   * Generate default setter
   */
  private[translate] def setter(f: PropertySetterDescriptor)(implicit pos: Position): MethodDef = {
    val propDesc = f.getCorrespondingProperty

    val tpe = getClassDescriptorForType(propDesc.getDispatchReceiverParameter.getType).toJsClassType
    val rtpe = propDesc.getReturnType.toJsType

    val params = List(ParamDef(Ident("value"), rtpe, mutable = false, rest = false))
    val body = Some(Assign(genSelectForAccessor(propDesc), VarRef(Ident("value"))(rtpe)))
    MethodDef(static = false, f.toJsMethodIdent, params, rtpe, body)(OptimizerHints.empty, None)
  }

  private def genSelectForAccessor(desc: PropertyDescriptor)(implicit pos: Position): Select = {

    val tpe = desc.getDispatchReceiverParameter.getType
    val clsTpe = DescriptorUtils.getClassDescriptorForType(tpe).toJsClassType
    val slctTpe = desc.getType.toJsType
    val rcv = genThisFromContext(clsTpe, desc)

    Select(rcv, desc.toJsIdent)(slctTpe)
  }
}
