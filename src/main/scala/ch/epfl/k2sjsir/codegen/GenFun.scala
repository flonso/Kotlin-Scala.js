package ch.epfl.k2sjsir.codegen

import ch.epfl.k2sjsir.Utils._
import org.jetbrains.kotlin.ir.declarations._
import org.scalajs.core.ir.Trees._

import scala.collection.JavaConversions._
import scala.language.implicitConversions

case class GenFun(d: IrFunction, p: Positioner) extends Gen[IrFunction] {

  def tree: Tree = d match {
    case c: IrConstructor => genFun(c)
    case f: IrFunction => genFun(f)
    case _ => notImplemented
  }

  private def genFun(f: IrFunction): Tree = {
    val desc = d.getDescriptor
    val tpe = desc.getReturnType.toJsType
    val body = GenBody(d.getBody, p).treeOption
    val idt = desc.toMethodIdent
    val args = desc.getValueParameters.map(_.toParamDef)
    val opt = OptimizerHints.empty
    MethodDef(static = false, idt, args.toList, tpe, body)(opt, None)
  }

}