package ch.epfl.k2sjsir.utils

import ch.epfl.k2sjsir.utils.NameEncoder._
import org.jetbrains.kotlin.descriptors._
import org.jetbrains.kotlin.resolve.DescriptorUtils._
import org.jetbrains.kotlin.types.{KotlinType, TypeUtils}
import org.scalajs.core.ir.Trees._
import org.scalajs.core.ir.Types._
import org.scalajs.core.ir.{Definitions, Position, Types}
import org.scalajs.core.ir.ClassKind
import org.jetbrains.kotlin.descriptors.{ClassKind => KtClassKind}
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorUtils

import scala.collection.JavaConverters._

object Utils {

  implicit class DeclarationDescriptorTranslator(d: DeclarationDescriptor) {
    private val name = d.getName.asString()

    def toJsClosureName: String = "closureargs$" + encodeName(name)

    def toJsName: String = encodeName(name)

    def toJsIdent(implicit pos: Position): Ident = Ident(d.toJsName, Some(name))

    def toJsClosureIdent(implicit pos: Position): Ident = Ident(d.toJsClosureName, Some(name))
  }

  implicit class CallableDescriptorTranslator(d: CallableDescriptor) {
    def toJsMethodIdent(implicit pos: Position): Ident = encodeMethodIdent(d)

    def toJsBridgeIdent(implicit  pos: Position): Ident = {
      val clsName = d match {
        case fd: FunctionDescriptor => Option(fd.getContainingDeclaration.asInstanceOf[ClassDescriptor].toJsClassName)
      }

      val toReplace = clsName.fold("")(x => x + "__")

      val tmpIdt = d.toJsMethodIdent

      Ident(tmpIdt.name.replaceFirst(toReplace, ""), tmpIdt.originalName)
    }

  }

  implicit class ClassDescriptorTranslator(d: ClassDescriptor) {
    def toJsClassName: String = encodeClassFullName(d)

    def toJsClassIdent(implicit pos: Position): Ident = encodeClassFullNameIdent(d)

    def toJsDefaultImplIdent(implicit pos: Position): Ident = encodeClassFullNameIdent(d, kotlinNaming = true)

    def toJsClassType: ClassType = ClassType(d.toJsClassName)

    def toJsDefaultImplType(implicit pos: Position): ClassType = ClassType(d.toJsDefaultImplIdent.name)

    def toJsClassKind: ClassKind = getClassKind(d.getKind)

    def isInterface: Boolean = d.toJsClassKind == ClassKind.Interface
  }

  implicit class ParameterTranslator(d: ParameterDescriptor) {
    private val tpe = d.getReturnType.toJsType

    def toJsClosureParamDef(implicit pos: Position): ParamDef = _toJsParamDef(Some(AnyType), Some(d.toJsClosureIdent))

    def toJsAnyParamDef(implicit pos: Position): ParamDef = _toJsParamDef(Some(AnyType))

    def toJsParamDef(implicit pos: Position): ParamDef = _toJsParamDef()

    def toJsInternal: String = toInternal(tpe)

    private def _toJsParamDef(paramTpe: Option[Type] = None, ident: Option[Ident] = None)(implicit pos: Position) = {
      ParamDef(ident.getOrElse(d.toJsIdent), paramTpe.getOrElse(tpe), mutable = false, rest = false)
    }
  }

  def isVarArg(d: ParameterDescriptor): Boolean = d match {
    case dd: ValueParameterDescriptor if dd.getVarargElementType != null => true
    case _ => false
  }

  implicit class VariableTranslator(d: VariableDescriptor) {
    def toJsVarRef(implicit pos: Position): VarRef = VarRef(d.toJsIdent)(d.getReturnType.toJsType)
  }

  implicit class KotlinTypeTranslator(t: KotlinType) {
    def toJsType: Type = getType(t)

    def toJsTypeRef: TypeRef = getTypeRef(t)

    def toJsClassType: ClassType = getClassType(t)

    def toJsArrayType: ArrayType = getArrayType(t)

    def toJsInternal: String = toInternal(t.toJsType)
  }

  implicit class KtPropertyHelper(p: KtProperty) {
    def hasGetterImpl = p.getGetter != null

    def hasSetterImpl = p.isVar && p.getSetter != null

    def hasDefinedAccessors: Boolean = hasGetterImpl || hasSetterImpl
  }

  implicit class PropertyAccessor(d: PropertyDescriptor)(implicit pos: Position) {
    def getterIdent(): Ident = NameEncoder.encodeMethodIdent(d)
  }

  def getName(tpe: KotlinType): String = {
    val desc = tpe.getConstructor.getDeclarationDescriptor
    if (TypeUtils.isTypeParameter(tpe)) {
      val tps = desc.asInstanceOf[TypeParameterDescriptor].getUpperBounds
      if (tps.isEmpty) "kotlin.Any"
      else getFqName(tps.get(0).getConstructor.getDeclarationDescriptor).asString()
    }
    else getFqName(desc).asString()
  }

  def getFreshName(prefix: String = "x$"): String = {
    prefix + java.util.UUID.randomUUID().toString.replaceAllLiterally("-", "")
  }

  def ensureBoxed(t: Tree): Tree = {
    if (t.tpe == NoType)
      Block(t, Undefined()(t.pos))(t.pos)
    else
      t
  }

  def cast(t: Tree, rtpe: KotlinType)(implicit pos: Position): Tree = {
    // TODO: Add a function to determine if it's a JS type
    if (rtpe.toJsType == NoType || isLambdaType(rtpe)) {
      t
    } else if (Utils.isPrimitiveType(rtpe.toJsType)) {
      Unbox(t, rtpe.toJsInternal.charAt(0))
    } else {
      AsInstanceOf(t, rtpe.toJsTypeRef)
    }
  }


  private def getType(tpe: KotlinType, isVararg: Boolean = false): Type = {
    if (isLambdaType(tpe)) AnyType
    else {
      val ret = getTypeRef(tpe) match {
        case ClassRef(cls) => ClassType(cls)
        case typeRef: ArrayTypeRef => ArrayType(typeRef)
      }

      types.getOrElse(getName(tpe), ret)
    }
  }

  def adaptPrimitive(value: Tree, to: Type)(
    implicit pos: Position): Tree = {
    genConversion(value.tpe, to, value)
  }

  /* This method corresponds to the method of the same name in
   * BCodeBodyBuilder of the JVM back-end. It ends up calling the method
   * BCodeIdiomatic.emitT2T, whose logic we replicate here.
   */
  private def genConversion(from: Type, to: Type, value: Tree)(
    implicit pos: Position): Tree = {
    import UnaryOp._

    if (from == to || from == NothingType) {
      value
    } else if (from == BooleanType || to == BooleanType) {
      throw new AssertionError(s"Invalid genConversion from $from to $to")
    } else {
      def intValue = (from: @unchecked) match {
        case IntType    => value
        case CharType   => UnaryOp(CharToInt, value)
        case ByteType   => UnaryOp(ByteToInt, value)
        case ShortType  => UnaryOp(ShortToInt, value)
        case LongType   => UnaryOp(LongToInt, value)
        case FloatType  => UnaryOp(DoubleToInt, UnaryOp(FloatToDouble, value))
        case DoubleType => UnaryOp(DoubleToInt, value)
      }

      def doubleValue = from match {
        case DoubleType => value
        case FloatType  => UnaryOp(FloatToDouble, value)
        case LongType   => UnaryOp(LongToDouble, value)
        case _                => UnaryOp(IntToDouble, intValue)
      }

      (to: @unchecked) match {
        case CharType =>
          UnaryOp(IntToChar, intValue)
        case ByteType =>
          UnaryOp(IntToByte, intValue)
        case ShortType =>
          UnaryOp(IntToShort, intValue)
        case IntType =>
          intValue
        case LongType =>
          from match {
            case FloatType | DoubleType =>
              UnaryOp(DoubleToLong, doubleValue)
            case _ =>
              UnaryOp(IntToLong, intValue)
          }
        case FloatType =>
          UnaryOp(UnaryOp.DoubleToFloat, doubleValue)
        case DoubleType =>
          doubleValue
      }
    }
  }

  def isPrimitiveType(value: Type): Boolean = {
    value match {
      case BooleanType | CharType | ByteType |
           ShortType | IntType | LongType | FloatType |
           DoubleType =>
        true
      case _ => false
    }
  }

  def genThisFromContext(tpe: Type, clsDesc: ClassDescriptor)(implicit pos: Position): Tree = {
    if (clsDesc != null && clsDesc.toJsClassKind == ClassKind.Interface)
      return VarRef(Ident("$this"))(clsDesc.toJsClassType)
    else
      This()(tpe)
  }

  def genThisFromContext(tpe: Type, cmd: CallableMemberDescriptor = null)(implicit pos: Position): Tree = {
    if (cmd != null) {
      val dr = cmd.getDispatchReceiverParameter

      if (dr != null) {
        val clsDesc = DescriptorUtils.getClassDescriptorForType(dr.getType)

        return genThisFromContext(tpe, clsDesc)
      }
    }

    This()(tpe)
  }

  private def isLambdaType(tpe: KotlinType): Boolean = {
    tpe.toString.matches("Function[0-9]+.*")
  }

  private def getTypeRef(tpe: KotlinType): TypeRef = {
    val name = getName(tpe)
    if (name == "kotlin.Array" || arrayTypes.contains(name)) getArrayTypeRef(tpe)
    else if (name.startsWith("kotlin.Function")) getFunctionType(name)
    else ClassRef(toInternal(types.getOrElse(name, getClassType(tpe))))
  }

  private def getFunctionType(name: String): TypeRef = {
    // FIXME: Keep kotlin.Function when stdlib is working
    val suffix = name.replace("kotlin.Function", "")
    ClassRef(s"sjs_js_Function$suffix")
  }

  private def getClassType(tpe: KotlinType): ClassType = {
    ClassType(encodeClassName(getName(tpe), ""))
  }

  private def getClassKind(kind: KtClassKind): ClassKind = {
    classKinds(kind)
  }

  private def getArrayTypeRef(tpe: KotlinType): ArrayTypeRef = {
    val name = getName(tpe)
    val args = tpe.getArguments.asScala.map(_.getType)
    if (name == "kotlin.Array") ArrayTypeRef.of(getTypeRef(args.head))
    else arrayTypes(name)
  }

  private def getArrayType(tpe: KotlinType): ArrayType = {
    ArrayType(getArrayTypeRef(tpe))
  }

  private val arrayTypes = Map(
    "kotlin.IntArray" -> ClassRef("I"),
    "kotlin.BooleanArray" -> ClassRef("Z"),
    "kotlin.CharArray" -> ClassRef("C"),
    "kotlin.ByteArray" -> ClassRef("B"),
    "kotlin.ShortArray" -> ClassRef("S"),
    "kotlin.FloatArray" -> ClassRef("F"),
    "kotlin.LongArray" -> ClassRef("J"),
    "kotlin.DoubleArray" -> ClassRef("D")
  ).mapValues(t => ArrayTypeRef.of(t))

  private val types = Map(
    "kotlin.Any" -> AnyType,
    "kotlin.Unit" -> NoType,
    "kotlin.Nothing" -> NothingType,
    "kotlin.Boolean" -> BooleanType,
    "kotlin.Char" -> IntType,
    "kotlin.Byte" -> IntType,
    "kotlin.Short" -> IntType,
    "kotlin.Int" -> IntType,
    "kotlin.Float" -> FloatType,
    "kotlin.Long" -> LongType,
    "kotlin.Double" -> DoubleType,
    "kotlin.Null" -> NullType,
    "kotlin.String" -> ClassType(Definitions.StringClass),
    "kotlin.Throwable" -> AnyType,
    "kotlin.Comparable" -> AnyType
  )

  private val classKinds = Map(
    KtClassKind.CLASS -> ClassKind.Class,
    KtClassKind.INTERFACE -> ClassKind.Interface,
    KtClassKind.OBJECT -> ClassKind.ModuleClass
  )

  private def toInternal(t: Type): String = t match {
    case NoType => "V"
    case AnyType => "O"
    case BooleanType => "Z"
    case IntType => "I"
    case LongType => "J"
    case FloatType => "F"
    case DoubleType => "D"
    case StringType => "T"
    case ArrayType(ArrayTypeRef(elem, dims)) => "A"*dims + encodeName(elem)
    // FIXME: Remove this after kotlin-stdlib is compiled correctly
    case ClassType(name) if name.matches("kotlin.Function*") => name.replace("kotlin.Function", "sjs_js_Function")
    case ClassType(name) => name
    case NothingType => Definitions.RuntimeNothingClass
    case NullType => Definitions.RuntimeNullClass
    case _ => throw new Error(s"Unknown Scala.js type: $t")
  }

  val isValueType = Set[Types.Type](IntType, LongType, DoubleType, BooleanType, FloatType)

}
