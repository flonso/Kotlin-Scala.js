package ch.epfl.k2sjsir.translate

import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.psi._
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator.getConstant
import org.scalajs.core.ir.Trees._

case class GenConst(d: KtConstantExpression)(implicit val c: TranslationContext) extends Gen[KtConstantExpression] {

  override def tree: Tree = {
    val tpe = c.bindingContext().getType(d)
    val exp = getConstant(d, c.bindingContext())
    val value = exp.getValue(tpe)
    value match {
      case c: Char => CharLiteral(c)
      case s: Short => ShortLiteral(s)
      case b: Byte => ByteLiteral(b)
      case i: Int => IntLiteral(i)
      case l: Long => LongLiteral(l)
      case f: Float => FloatLiteral(f)
      case d: Double => DoubleLiteral(d)
      case b: Boolean => BooleanLiteral(b)
      case null => Null()
      case _ => notImplemented()
    }
  }

  def treeOption: Option[Tree] = if (d == null) None else Some(tree)

}
