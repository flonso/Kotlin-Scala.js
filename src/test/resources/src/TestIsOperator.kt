open class Base (val p: Int) {

    override fun toString(): String {
        return "Base with p = $p"
    }
}

class Derived(p: Int) : Base(p) {

    override fun toString(): String {
        return "Derived with p = $p"
    }
}

fun main(args: Array<String>) {
    val myDouble = 1.7
    val myInt = 4
    val myLong = 23432542300000
    val myDerived = Derived(myInt)
    val myBase = Base(myInt)

    check(myDouble)
    check(myInt)
    check(myLong)
    check(myDerived)
    check(myBase)
}

fun check(a: Any) {

    if (a is Int) println("$a is an Int")
    if (a is Double) println("$a is a Double")
    if (a is Long) println("$a is a Long")


    if (a !is Int) println("$a is not an Int")
    if (a !is Double) println("$a is not a Double")
    if (a !is Long) println("$a is not a Long")

    if (a is Base) println("$a is Base class")
    if (a is Derived) println("$a is Derived class")
    if (a !is Derived) println("$a is not Derived class")
}