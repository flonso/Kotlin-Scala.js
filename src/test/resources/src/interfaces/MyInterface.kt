interface MyInterface : MySuperInterface, DummyInterface {
    val a: Int
        get() = 10
    val b: String
    val c: String

    fun abstractMethod(arg1: Int, arg2: String): String

    fun petYourDog(): Unit

    fun changeCToSomeValue(): Unit {
        this.b

        println("c doesn't exist anymore, sorry")
    }

    fun doSomething(): Unit {
        println("It's something !")
    }

    fun assignSomeValues(): Unit

    override fun toString(): String

    override fun dummyFun() {
        //super.dummyFun()
        println("And some more prints")
    }
}