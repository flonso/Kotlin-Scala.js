fun myEnumTests() {
    // Test values()
    val values = MyEnumClass.values()
    for (v in values) {
        println(v)
    }

    // Test valueOf
    val valueOfZero = MyEnumClass.valueOf("ZERO")
    println(valueOfZero)

    try {
        val valueOfUnknow = MyEnumClass.valueOf("Unknown")
        println(valueOfUnknow)
    } catch (e: IllegalStateException) {
        println("Exception correctly raised")
    }

    // Accessing members
    val one = MyEnumClass.ONE
    println(one)

    if (valueOfZero == MyEnumClass.ZERO)
        println("Zero is Zero !")
    else
        println("Zero is not Zero !?")

    // Test enum with constructor
    //val tmp = Fruits.BANANA
    //println(tmp.name)
}

fun myInheritanceTest() {
    val a = A(0, "hey")
    val b = B(42)
    println("${b.a} ${b.b} ${b.c}")

    val empty = B2()
    empty.dummy()
}

fun myCompanionTest() {
    //FIXME: This call should return the content of inner
    //println(MyObjectClass.MyObject.inner)
}

fun main(args: Array<String>) {
    myEnumTests()
    myInheritanceTest()
    myCompanionTest()
}