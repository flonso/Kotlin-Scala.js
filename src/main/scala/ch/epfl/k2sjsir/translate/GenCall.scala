package ch.epfl.k2sjsir.translate

import ch.epfl.k2sjsir.utils.NameEncoder
import ch.epfl.k2sjsir.utils.NameEncoder._
import ch.epfl.k2sjsir.utils.Utils._
import org.jetbrains.kotlin.descriptors.{ClassConstructorDescriptor, SimpleFunctionDescriptor}
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils._
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt
import org.jetbrains.kotlin.resolve.scopes.receivers.{ExpressionReceiver, ExtensionReceiver, ImplicitClassReceiver, ReceiverValue}
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.ClassType

import scala.collection.JavaConverters._

case class GenCall(d: KtCallExpression)(implicit val c: TranslationContext) extends Gen[KtCallExpression] {

  private val resolved = CallUtilKt.getResolvedCall(d, c.bindingContext())
  private val desc = resolved.getResultingDescriptor
  private lazy val rtpe = desc.getReturnType.toJsType
  private val name = desc.getName.asString()
  private val args = genArgs().toList

  override def tree: Tree = {
    desc match {
      case sf: DeserializedSimpleFunctionDescriptor =>
        if (name == "println" || name == "print") {
          val rec = LoadModule(ClassType("s_Predef$"))
          val method = Ident(s"${name}__O__V", Some(name))
          Apply(rec, method, args)(rtpe)
        }
        else if (name == "js") {
          GenJsFunc(d).tree
        }
        else if (GenArray.isArrayOps(desc)) GenArray(d, args).tree
        else notImplemented("while looking at DeserializedSimpleFunctionDescriptor")

      case cc: ClassConstructorDescriptor =>
        val ctpe =
          if(cc.getContainingDeclaration.getName.toString == "Exception")
            ClassType("jl_Exception")
          else
            cc.getContainingDeclaration.toJsClassType

        if(!cc.getContainingDeclaration.isExternal)
          New(ctpe, desc.toJsMethodIdent, args)
        else
          JSNew(LoadJSConstructor(ctpe), args)

      case sf: SimpleFunctionDescriptor =>
        val dr = Option(desc.getDispatchReceiverParameter).getOrElse(desc.getExtensionReceiverParameter)

        if(DescriptorUtils.isExtension(desc)) {

          genExtensionCall(VarRef(dr.toJsIdent)(dr.getType.toJsType))
        }
        else if(isTopLevelFunction(sf)) {

          ApplyStatic(ClassType(NameEncoder.encodeWithSourceFile(sf)), desc.toJsMethodIdent, args)(desc.getReturnType.toJsType)
        }
        else {
          val dispatchReceiver = resolved.getDispatchReceiver

          if (isDirectCallToJsFunc(dispatchReceiver))
            GenJsFunc(d).tree
          else {
            val receiver = dispatchReceiver match {
              case i: ImplicitClassReceiver => This()(i.getClassDescriptor.toJsClassType)
              case e: ExtensionReceiver => VarRef(dr.toJsIdent)(dr.getType.toJsType)
              case e: ExpressionReceiver =>
                if (isReceiverJsFunc(dispatchReceiver))
                  GenJsFunc(e.getExpression.asInstanceOf[KtCallExpression]).tree
                else
                  GenExpr(e.getExpression).tree
              case _ => notImplemented("while looking for receiver")

            }

            // If it's a call to kotlin js func, pass the arguments to the call directly
            if (isReceiverJsFunc(dispatchReceiver)) {
              receiver match {
                case Apply(r, n, _) =>
                  Apply(r, n, args)(rtpe)
              }
            }
            else {
              val name =
                if (desc.getName.toString == "invoke")
                  NameEncoder.encodeApply(desc)
                else
                  desc.toJsMethodIdent

              Apply(receiver, name, args)(rtpe)
            }
          }
        }
      case _ =>
        notImplemented("in tree method")
    }
  }

  private def isDirectCallToJsFunc(rcv: ReceiverValue): Boolean = {
    rcv == null && name == "js"
  }

  private def isReceiverJsFunc(rcv: ReceiverValue): Boolean = {
    rcv match {
      case e: ExpressionReceiver if name == "invoke" =>
        val subResolvedCall = CallUtilKt
          .getResolvedCall(e.getExpression, c.bindingContext())

        if (subResolvedCall != null) {

          val name = subResolvedCall
            .getResultingDescriptor
            .getName
            .asString()

          name == "js"
        } else {
          false
        }
      case _ => false
    }
  }

  private def genArgs(): Seq[Tree] = {
    resolved.getCall.getValueArguments.asScala.map(x => GenExpr(x.getArgumentExpression).tree)
  }

  def genExtensionCall(receiver: Tree) : Tree = {
    val ext = desc.getDispatchReceiverParameter
    if (null != ext) {
      val cnt = ext.getContainingDeclaration

      val className = getFqName(cnt).asString()
      val suffix =
        if (DescriptorUtils.isObject(cnt) || DescriptorUtils.isCompanionObject(cnt)) "$"
        else ""
      val c = ClassType(encodeClassName(className, suffix))
      Apply(This()(c), desc.toJsMethodIdent, receiver :: args)(rtpe)
    } else {
      val o = desc.getExtensionReceiverParameter
      val cnt = o.getContainingDeclaration
      val name = JvmFileClassUtil.getFileClassInfoNoResolve(d.getContainingKtFile).getFileClassFqName.asString()
      val encodedName = NameEncoder.encodeClassName(name, "")

      ApplyStatic(ClassType(encodedName), desc.toJsMethodIdent, receiver :: args)(rtpe)
    }
  }

  private def isTopLevelFunction(sf: SimpleFunctionDescriptor) =
    sf.getExtensionReceiverParameter == null && sf.getContainingDeclaration.getName.asString() == "<root>"

}
