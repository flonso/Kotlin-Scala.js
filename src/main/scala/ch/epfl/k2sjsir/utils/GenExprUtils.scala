package ch.epfl.k2sjsir.utils

import ch.epfl.k2sjsir.translate.{GenCall, GenExpr}
import org.jetbrains.kotlin.descriptors.{ClassDescriptor, PropertyDescriptor}
import org.jetbrains.kotlin.js.translate.utils.BindingUtils
import org.jetbrains.kotlin.psi.{KtCallExpression, KtNameReferenceExpression, KtQualifiedExpression, KtSafeQualifiedExpression}
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types.{ArrayType, ClassRef, Type}
import ch.epfl.k2sjsir.utils.Utils._
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.scalajs.core.ir.Position

object GenExprUtils {
  def genDotQualified(k: KtQualifiedExpression, notImplemented: String => Tree)(implicit c: TranslationContext, pos: Position): Tree = {
    val isSafe = k.isInstanceOf[KtSafeQualifiedExpression]

    // (MyObjectClass.MyObject.MyOtherObject).inner
    val receiver = k.getReceiverExpression
    // If it's an object/module gen a module load and drop the rest (see GenJsCode)
    val receiverExpr = GenExpr(receiver).tree

    val selector = k.getSelectorExpression

    selector match {
      case call: KtCallExpression =>
        val rcvOpt = Option(k.getReceiverExpression)
        GenCall(call).withReceiver(rcvOpt, isSafe = isSafe)

      case kn: KtNameReferenceExpression =>
        val selectorDesc = BindingUtils.getDescriptorForReferenceExpression(c.bindingContext(), kn)

        val isArray = receiverExpr.tpe.isInstanceOf[ArrayType]

        val tpe = selectorDesc match {
          case cd: ClassDescriptor =>
            if (cd.isEnumEntry) {
              cd.getContainingDeclaration.asInstanceOf[ClassDescriptor].toJsClassType
            } else {
              cd.toJsClassType
            }
          case _ => BindingUtils.getTypeForExpression(c.bindingContext(), kn).toJsType
        }

        val ao = if(isArray) arrayOps(receiverExpr, tpe, kn.getReferencedName, List()) else None
        val dotExpr = ao.getOrElse {
          selectorDesc match {
            case m: PropertyDescriptor =>
              /*
               * If the expression returns a nullable type, it might hide the real type
               * of the property, we therefore need to obtain it directly from the descriptor.
               * We cannot do it above because it would prevent safe calls to type check in the
               * ScalaJS IR.
               */
              val tpe = m.getType.toJsType
              Apply(receiverExpr, m.getterIdent(), List())(tpe)

            case cls: ClassDescriptor =>
              if (cls.isEnumEntry) {
                val parent = cls.getContainingDeclaration.asInstanceOf[ClassDescriptor]

                Apply(receiverExpr, Ident(cls.getName().asString() + "__" + parent.toJsClassType.className), Nil)(tpe)
              } else {
                Select(receiverExpr, Ident(selectorDesc.getName.asString()))(tpe)
              }

            case desc => notImplemented(s"after KtDotQualifiedExpression > KtNameReferenceExpression with descriptor $desc")
          }
        }

        if (isSafe) {
          val cond = genNotNullCond(receiverExpr)
          val thenp = dotExpr
          val elsep = Null()

          If(cond, thenp, elsep)(tpe)
        } else {
          dotExpr
        }

      case _ =>
        notImplemented("after KtDotQualifiedExpression (default case)")
    }
  }

  private def arrayOps(receiver: Tree, tpe: Type, method: String, args: List[Tree])(implicit pos: Position) : Option[Tree] = {
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