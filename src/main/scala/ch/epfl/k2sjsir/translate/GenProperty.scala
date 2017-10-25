package ch.epfl.k2sjsir.translate

import ch.epfl.k2sjsir.utils.Utils._
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.kotlin.descriptors.{PropertyGetterDescriptor, PropertySetterDescriptor}
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.{KtProperty, KtPropertyAccessor, KtPsiUtil}
import org.jetbrains.kotlin.resolve.{DescriptorToSourceUtils, DescriptorUtils}
import org.jetbrains.kotlin.resolve.DescriptorUtils._
import org.scalajs.core.ir.Position
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.{AnyType, NoType}

case class GenProperty(d: KtProperty)(implicit val c: TranslationContext) extends Gen[KtProperty] {

  import GenProperty._

  val desc = BindingUtils.getPropertyDescriptor(c.bindingContext(), d)

  override def tree: Tree = {
    val idt = desc.toJsIdent
    val static = DescriptorUtils.isStaticDeclaration(desc)
    FieldDef(static, idt, desc.getType.toJsType, desc.isVar)
  }

  def withGetterAndSetter: Seq[Tree] = {
    val t =
      if (d.isVar || d.hasInitializer)
        List(tree)
      else
        Nil

    t ++ {
      if (d.getGetter == null || !d.getGetter.hasBody)
        Option(desc.getGetter).map(getter)
      else
        Option(d.getGetter).map(getter)
    }.toList ++ {
      if (d.getSetter == null || !d.getSetter.hasBody)
        Option(desc.getSetter).map(setter)
      else
        Option(d.getSetter).map(setter)
    }.toList
  }

}

object GenProperty {
  /**
   * Generate user defined getters for example :
   * val name: String
   *  get() = "name"
   */
  private[translate] def getter(f: KtPropertyAccessor)(implicit c: TranslationContext, pos: Position): Tree = {
    val body = GenBody(f.getBodyExpression).tree
    val ident = BindingUtils.getPropertyDescriptor(c.bindingContext(), f.getProperty)
    val rtpe = body.tpe

    MethodDef(static = false, ident.toJsMethodIdent, List(), rtpe, Some(body))(OptimizerHints.empty, None)

  }

  /**
   * Generate default getter
   */
  private[translate] def getter(f: PropertyGetterDescriptor)(implicit pos: Position): Tree = {

    val property = f.getCorrespondingProperty
    val rtpe = property.getReturnType.toJsType
    val tpe = getClassDescriptorForType(property.getDispatchReceiverParameter.getType).toJsClassType

    val body = Select(This()(tpe), property.toJsIdent)(rtpe)

    MethodDef(static = false, f.toJsMethodIdent, List(), rtpe, Some(body))(OptimizerHints.empty, None)
  }

  /**
    * Generate custom setter such as :
    * var myVar: Int = 0
    *   set(value) {
    *     if (value >= 0)
    *       field = value
    *     else
    *       field = 0
    *   }
    * @param f
    * @param c
    * @param pos
    * @return
    */
  private[translate] def setter(f: KtPropertyAccessor)(implicit c: TranslationContext, pos: Position): Tree = {
    val body = GenBody(f.getBodyExpression).tree
    val ident = BindingUtils.getPropertyDescriptor(c.bindingContext(), f.getProperty).getSetter
    val rtpe = body.tpe

    val params = f.getNode().findChildByType(KtStubElementTypes.VALUE_PARAMETER_LIST).getChildren(null).map(
      p =>
        ParamDef(Ident(p.getText), rtpe, mutable = false, rest = false)
    ).toList

    //val params = List(ParamDef(Ident("set"), rtpe, mutable = false, rest = false))
    //val body = Some(Assign(Select(This()(tpe), property.toJsIdent)(rtpe), VarRef(Ident("set"))(rtpe)))
    MethodDef(static = false, ident.toJsMethodIdent, params, rtpe, Some(body))(OptimizerHints.empty, None)
  }


  /**
   * Generate default setter
   */
  private[translate] def setter(f: PropertySetterDescriptor)(implicit pos: Position): Tree = {
    val property = f.getCorrespondingProperty

    val tpe = getClassDescriptorForType(property.getDispatchReceiverParameter.getType).toJsClassType
    val rtpe = property.getReturnType.toJsType

    val params = List(ParamDef(Ident("set"), rtpe, mutable = false, rest = false))
    val body = Some(Assign(Select(This()(tpe), property.toJsIdent)(rtpe), VarRef(Ident("set"))(rtpe)))
    MethodDef(static = false, f.toJsMethodIdent, params, rtpe, body)(OptimizerHints.empty, None)
  }

}
