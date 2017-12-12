package ch.epfl.k2sjsir.translate

import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.KtIsExpression
import org.scalajs.core.ir.Trees
import org.scalajs.core.ir.Trees.{IsInstanceOf, UnaryOp}
import org.scalajs.core.ir.Types.{ClassRef, TypeRef}
import ch.epfl.k2sjsir.utils.Utils._

case class GenIs(d: KtIsExpression)(implicit val c: TranslationContext) extends Gen[KtIsExpression] {
  override def tree: Trees.Tree = {
    val lhs = GenExpr(d.getLeftHandSide).tree
    val typeRef = BindingUtils.getTypeByReference(c.bindingContext(), d.getTypeReference).toJsInternal
    val isTypeRef = getTypeForIs(typeRef)
    val isNegated = d.isNegated

    val scalaEquivOp = IsInstanceOf(lhs, isTypeRef)

    if (isNegated)
      UnaryOp(UnaryOp.Boolean_!, scalaEquivOp)
    else
      scalaEquivOp
  }

  // TODO: Export this in a Util library instead (to avoid repetitions)
  private def getTypeForIs(tpe: String): TypeRef = tpe match {
    case "I" => ClassRef("jl_Integer")
    case "D" => ClassRef("jl_Double")
    case "F" => ClassRef("jl_Float")
    case "Z" => ClassRef("jl_Boolean")
    case "J" => ClassRef("jl_Long")
    case other => ClassRef(other)
  }
}
