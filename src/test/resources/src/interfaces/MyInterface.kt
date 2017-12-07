interface MyInterface : MySuperInterface, DummyInterface {
    val a: Int
        get() = 10
    val b: String
    var c: String

    fun abstractMethod(arg1: Int, arg2: String): String

    fun petYourDog(): Unit

    fun changeCToSomeValue(): Unit

    fun doSomething(): Unit {
        println("It's something !")
    }

    fun assignSomeValues(): Unit {
        c = "Hey there."
    }

    override fun toString(): String
}