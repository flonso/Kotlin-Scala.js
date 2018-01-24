@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("RangesKt")
package kotlin.ranges

/**
 * Represents a range of floating point numbers.
 * Extends [ClosedRange] interface providing custom operation [lessThanOrEquals] for comparing values of range domain type.
 *
 * This interface is implemented by floating point ranges returned by [Float.rangeTo] and [Double.rangeTo] operators to
 * achieve IEEE-754 comparison order instead of total order of floating point numbers.
 */
@SinceKotlin("1.1")
public interface ClosedFloatingPointRange<T: Comparable<T>> : ClosedRange<T> {
    override fun contains(value: T): Boolean = lessThanOrEquals(start, value) && lessThanOrEquals(value, endInclusive)
    override fun isEmpty(): Boolean = !lessThanOrEquals(start, endInclusive)

    /**
     * Compares two values of range domain type and returns true if first is less than or equal to second.
     */
    fun lessThanOrEquals(a: T, b: T): Boolean
}

/**
 * Represents a range of [Comparable] values.
 */
private open class ComparableRange<T: Comparable<T>> (
        override val start: T,
        override val endInclusive: T
): ClosedRange<T> {

    override fun equals(other: Any?): Boolean {
        return other is ComparableRange<*> && (isEmpty() && other.isEmpty() ||
                start == other.start && endInclusive == other.endInclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * start.hashCode() + endInclusive.hashCode()
    }

    override fun toString(): String = "$start..$endInclusive"
}

/**
 * A closed range of values of type `Double`.
 *
 * Numbers are compared with the ends of this range according to IEEE-754.
 */
private class ClosedDoubleRange (
        start: Double,
        endInclusive: Double
) : ClosedFloatingPointRange<Double> {
    private val _start = start
    private val _endInclusive = endInclusive
    override val start: Double get() = _start
    override val endInclusive: Double get() = _endInclusive

    override fun lessThanOrEquals(a: Double, b: Double): Boolean = a <= b

    override fun contains(value: Double): Boolean = value >= _start && value <= _endInclusive
    override fun isEmpty(): Boolean = !(_start <= _endInclusive)

    override fun equals(other: Any?): Boolean {
        return other is ClosedDoubleRange && (isEmpty() && other.isEmpty() ||
                _start == other._start && _endInclusive == other._endInclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * _start.hashCode() + _endInclusive.hashCode()
    }
    override fun toString(): String = "$_start..$_endInclusive"
}

/* Not available on platform: JS */






























/**
 * Creates a range from this [Comparable] value to the specified [that] value.
 *
 * This value needs to be smaller than [that] value, otherwise the returned range will be empty.
 */
public operator fun <T: Comparable<T>> T.rangeTo(that: T): ClosedRange<T> = ComparableRange(this, that)

/**
 * Creates a range from this [Double] value to the specified [other] value.
 *
 * Numbers are compared with the ends of this range according to IEEE-754.
 */
@SinceKotlin("1.1")
public operator fun Double.rangeTo(that: Double): ClosedFloatingPointRange<Double> = ClosedDoubleRange(this, that)

/* Not available on platform: JS */









internal fun checkStepIsPositive(isPositive: Boolean, step: Number) {
    if (!isPositive) throw IllegalArgumentException("Step must be positive, was: $step.")
}