package ch.epfl.k2sjsir.translate

import ch.epfl.k2sjsir.utils.Utils
import ch.epfl.k2sjsir.utils.Utils._
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.kotlin.descriptors.{PropertyGetterDescriptor, PropertySetterDescriptor}
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.{KtProperty, KtPropertyAccessor, KtPsiUtil}
import org.jetbrains.kotlin.resolve.{DescriptorToSourceUtils, DescriptorUtils}
import org.jetbrains.kotlin.resolve.DescriptorUtils._
import org.jetbrains.kotlin.types.KotlinType
import org.scalajs.core.ir.Position
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.{AnyType, ClassType, Type}

case class GenProperty(d: KtProperty)(implicit val c: TranslationContext) extends Gen[KtProperty] {

  import GenProperty._

  val desc = BindingUtils.getPropertyDescriptor(c.bindingContext(), d)


  override def tree: Tree = {
    val idt = desc.toJsIdent
    val static = DescriptorUtils.isStaticDeclaration(desc)

    val tpe = getLambdaTypeIfNecessary(desc.getType)

    FieldDef(static, idt, tpe, desc.isVar)
  }

  def withGetterAndSetter: Seq[Tree] = {
    val t =
      if (d.isVar || d.hasInitializer)
        List(tree)
      else
        Nil

    val g =
      if (d.getGetter == null || !d.getGetter.hasBody)
        Option(desc.getGetter).map(getter)
      else
        Option(d.getGetter).map(getter)

    val s =
      if (d.getSetter == null || !d.getSetter.hasBody)
        Option(desc.getSetter).map(setter)
      else
        Option(d.getSetter).map(setter)

    t ++ g.toList ++ s.toList
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
    val methodIdent =
      if (isLambdaType(property.getReturnType)) {
        val rtpeName = Utils.getName(property.getReturnType)
        val suffix = rtpeName.replace("kotlin.Function", "")

        Ident(s"${property.getName}__sjs_js_Function$suffix")
      }
      else
        f.toJsMethodIdent

    val rtpe = getLambdaTypeIfNecessary(property.getReturnType)
    val tpe = getClassDescriptorForType(property.getDispatchReceiverParameter.getType).toJsClassType

    val body = Select(This()(tpe), property.toJsIdent)(rtpe)

    MethodDef(static = false, methodIdent, List(), rtpe, Some(body))(OptimizerHints.empty, None)
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

  private def isLambdaType(tpe: KotlinType): Boolean = {
    tpe.toString.matches("Function[0-9]+.*")
  }

  private def getLambdaTypeIfNecessary(tpe: KotlinType): Type = {
    if (isLambdaType(tpe))
      AnyType
    else
      tpe.toJsType
  }

}
