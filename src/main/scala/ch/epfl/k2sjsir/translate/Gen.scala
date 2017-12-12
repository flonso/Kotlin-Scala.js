package ch.epfl.k2sjsir.translate

import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.psi.KtElement
import org.scalajs.core.ir.Position
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.AnyType



trait IRNodeGen[T <: KtElement, TreeType <: IRNode] {
  def d: T
  implicit def c: TranslationContext
  implicit def pos: Position = {
    if (d == null) return Position.NoPosition

    val lc = DiagnosticUtils.getLineAndColumnInPsiFile(d.getContainingFile, d.getTextRange)
    val file = d.getContainingFile.getName
    Position(Position.SourceFile(file), lc.getLine, lc.getColumn)
  }

  def tree: TreeType

  private def notImplementedPrint(debugStr: String = "") = {
    val c = getClass.getSimpleName
    val name = if (d != null) d.getClass.getSimpleName else ""
    println(s"Not supported $c: $name" + (if (!debugStr.isEmpty) s" with message $debugStr" else ""))
    println(s"at pos : $pos")
  }

  def notImplemented(debugStr: String = ""): Tree = {
    notImplementedPrint(debugStr)

    Debugger()
  }

  def notImplementedMemberDef(debugStr: String = ""): MemberDef = {
    notImplementedPrint(debugStr)
    FieldDef(false, Ident("debug"), AnyType, false)
  }

}

trait Gen[T <: KtElement] extends IRNodeGen[T, Tree]
