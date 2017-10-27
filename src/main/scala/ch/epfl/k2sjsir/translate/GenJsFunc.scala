package ch.epfl.k2sjsir.translate

import ch.epfl.k2sjsir.utils.Utils._
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.psi.{KtCallExpression, KtStringTemplateExpression}
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt
import org.scalajs.core.ir.Trees
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.ClassType

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
      case _ => notImplemented(s"using argument $argAsString")
    }
  }

  private def genArguments(): Seq[Tree] = {
    resolvedCall.getCall.getValueArguments.asScala.map(x =>
      GenExpr(x.getArgumentExpression).tree)
  }
}
