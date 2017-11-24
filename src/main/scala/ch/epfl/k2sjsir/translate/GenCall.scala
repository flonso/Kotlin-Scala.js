package ch.epfl.k2sjsir.translate

import ch.epfl.k2sjsir.utils.NameEncoder
import ch.epfl.k2sjsir.utils.NameEncoder._
import ch.epfl.k2sjsir.utils.Utils._
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.{ClassConstructorDescriptor, SimpleFunctionDescriptor}
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.psi.{KtCallExpression, KtExpression}
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils._
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt
import org.jetbrains.kotlin.resolve.scopes.receivers.{ExpressionReceiver, ExtensionReceiver, ImplicitClassReceiver, ReceiverValue}
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.{ArrayType, ClassType, Type}

import scala.collection.JavaConverters._

case class GenCall(d: KtCallExpression)(implicit val c: TranslationContext) extends Gen[KtCallExpression] {

  private val resolved = CallUtilKt.getResolvedCall(d, c.bindingContext())
  private val desc = resolved.getResultingDescriptor
  private lazy val rtpe = desc.getReturnType.toJsType
  private val name = desc.getName.asString()
  private val args = genArgs().toList

  override def tree: Tree = withReceiver(None)

  def withReceiver(rcv: Option[KtExpression] = None): Tree = {
    desc match {
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
        if (name == "println" || name == "print") {
          val rec = LoadModule(ClassType("s_Predef$"))
          val method = Ident(s"${name}__O__V", Some(name))
          Apply(rec, method, args)(rtpe)
        }
        else if (name == "js") {
          GenJsFunc(d).tree
        }
        else if (GenArray.isArrayOps(desc)) {
          if (rcv.isEmpty)
            GenArray(d, args).tree
          else
            arrayOps(GenExpr(rcv.get).tree, rtpe, name, args).getOrElse(notImplemented("Missing array operation"))
        }
        else {
          if (rcv.nonEmpty) {
            val receiver = GenExpr(rcv.get).tree
            val isLambdaCall = name == "invoke"
            val isDirectInvokeCall = d.getText.matches("^invoke[(].*[)]$")
            val isArray = receiver.tpe.isInstanceOf[ArrayType]


            if (DescriptorUtils.isExtension(desc)) {
              genExtensionCall(receiver)
            }
            else if (isArray) {
              arrayOps(receiver, rtpe, name, args).getOrElse(notImplemented("Missing array operation"))
            }
            else if (GenUnary.isUnaryOp(name)) {
              val op = GenUnary.convertToOp(receiver.tpe, rtpe, receiver)

              op.getOrElse(notImplemented(s"missing UnaryOp from ${receiver.tpe} to $rtpe"))
            }
            else {
              // FIXME: do not match on the receiver but on its tpe
              // if the type is AnyType then its either a call to java.lang.Object or a JS call
              receiver match {
                case _: LoadJSModule | _: JSNew =>
                  JSBracketMethodApply(receiver, StringLiteral(name), args)
                case _ =>
                  val name = if (desc.getName.toString == "invoke") NameEncoder.encodeApplyLambda(desc) else desc.toJsMethodIdent

                  if (isLambdaCall && !isDirectInvokeCall) {
                    val lambdaFuncName = """((.+)\(.*\))""".r.replaceAllIn(d.getText, "$2")
                    val sjsJsFunction = s"sjs_js_Function${args.size}"
                    val lambdaType = ClassType(sjsJsFunction)
                    val lambdaName = Ident(s"${lambdaFuncName}__$sjsJsFunction")
                    JSFunctionApply(Apply(receiver, lambdaName, Nil)(lambdaType), args)
                  }
                  else if (isLambdaCall && isDirectInvokeCall)
                    JSFunctionApply(receiver, args)
                  else
                    Apply(receiver, name, args)(rtpe)
              }
            }

          } else {

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
                  case _: ExtensionReceiver => VarRef(dr.toJsIdent)(dr.getType.toJsType)
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
                  if (desc.getName.toString == "invoke")
                    JSFunctionApply(receiver, args)
                  else
                    Apply(receiver, desc.toJsMethodIdent, args)(rtpe)
                }
              }
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


  private def arrayOps(receiver: Tree, tpe: Type, method: String, args: List[Tree]) : Option[Tree] = {
    method match {
      case "get" =>
        require(args.size == 1)
        Some(ArraySelect(receiver, args.head)(tpe))
      case "set" =>
        require(args.size == 2)
        Some(Assign(ArraySelect(receiver, args.head)(args(1).tpe), args(1)))
      case "size" =>
        Some(ArrayLength(receiver))
      case "iterator" =>
        None
      case _ => None
    }
  }
}
