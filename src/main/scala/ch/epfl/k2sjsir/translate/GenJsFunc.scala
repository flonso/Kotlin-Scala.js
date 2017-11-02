package ch.epfl.k2sjsir.translate

import ch.epfl.k2sjsir.utils.Utils._
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.psi.{KtCallExpression, KtStringTemplateExpression, KtTypeReference}
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt
import org.scalajs.core.ir.Trees
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.{ClassType, DoubleType, FloatType}

import scala.collection.JavaConverters._

case class GenJsFunc(d: KtCallExpression)(implicit val c: TranslationContext)
    extends Gen[KtCallExpression] {

  private val resolvedCall = CallUtilKt.getResolvedCall(d, c.bindingContext())
  private val descriptor = resolvedCall.getResultingDescriptor
  private val name = descriptor.getName.asString()
  private val arguments = genArguments().toList

  override def tree: Trees.Tree = {
    assert(name == "js")

    // JS function should only have one argument
    val tmp = resolvedCall.getCall.getValueArguments.asScala
      .map(_.getArgumentExpression.getText)
      .mkString("")

    // Remove quotes from start and end
    val argAsString = tmp.slice(1, tmp.length - 1)

    argAsString match {
      case "Kotlin.identityHashCode" =>
        Apply(LoadModule(ClassType("jl_System$")),
              Ident("identityHashCode__O__I"),
              List())(descriptor.getReturnType.toJsType)
      case "Number.MIN_VALUE" =>
        findClosestTypeRef(d) match {
          case Some(n) =>
            val rcv = LoadModule(ClassType("s_"+ n +"$"))
            val method = Ident(s"MinValue__${n.charAt(0)}")
            val rtpe =
              if (n == "Double")
                DoubleType
            else
                FloatType

            Apply(rcv, method, Nil)(rtpe)
          case _ => notImplemented("Number.MIN_VALUE should only be used for Float or Double")
        }
      case "Number.MAX_VALUE" =>
        findClosestTypeRef(d) match {
          case Some("Double") => DoubleLiteral(Double.MaxValue)
          case Some("Float") => FloatLiteral(Float.MaxValue)
          case _ => notImplemented("Number.MAX_VALUE should only be used for Float or Double")
        }
      case "Number.POSITIVE_INFINITY" =>
        findClosestTypeRef(d) match {
          case Some("Double") => DoubleLiteral(Double.PositiveInfinity)
          case Some("Float") => FloatLiteral(Float.PositiveInfinity)
          case _ => notImplemented("Number.POSITIVE_INFINITY should only be used for Float or Double")
        }
      case "Number.NEGATIVE_INFINITY" =>
        findClosestTypeRef(d) match {
          case Some("Double") => DoubleLiteral(Double.NegativeInfinity)
          case Some("Float") => FloatLiteral(Float.NegativeInfinity)
          case _ => notImplemented("Number.NEGATIVE_INFINITY should only be used for Float or Double")
        }
      case "Number.NaN" =>
        findClosestTypeRef(d) match {
          case Some("Double") => DoubleLiteral(Double.NaN)
          case Some("Float") => FloatLiteral(Float.NaN)
          case _ => notImplemented("Number.NaN should only be used for Float or Double")
        }
      case "Kotlin.Long.MIN_VALUE" =>
        LongLiteral(Long.MinValue)
      case "Kotlin.Long.MAX_VALUE" =>
        LongLiteral(Long.MaxValue)
      case _ => notImplemented(s"using argument $argAsString")
    }
  }

  private def findClosestTypeRef(pe: PsiElement): Option[String] = {
    def _findClosestTypeRef(psi: PsiElement): Option[KtTypeReference] = {
      if (psi == null)
        None

      if (psi.isInstanceOf[KtTypeReference])
        Some(psi.asInstanceOf[KtTypeReference])
      else
        _findClosestTypeRef(psi.getPrevSibling)
    }

    val psi = _findClosestTypeRef(pe)

    if (psi.nonEmpty)
      Some(psi.get.getText())
    else
      None
  }

  private def genArguments(): Seq[Tree] = {
    resolvedCall.getCall.getValueArguments.asScala.map(x =>
      GenExpr(x.getArgumentExpression).tree)
  }
}
