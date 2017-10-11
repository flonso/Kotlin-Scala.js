package ch.epfl.k2sjsir.translate

import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.KtIsExpression
import org.scalajs.core.ir.Trees
import org.scalajs.core.ir.Trees.{IsInstanceOf, UnaryOp}
import org.scalajs.core.ir.Types.{ClassType, ReferenceType}
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
  private def getTypeForIs(tpe: String): ReferenceType = tpe match {
    case "I" => ClassType("jl_Integer")
    case "D" => ClassType("jl_Double")
    case "F" => ClassType("jl_Float")
    case "Z" => ClassType("jl_Boolean")
    case "J" => ClassType("jl_Long")
    case other => ClassType(other)
  }
}
