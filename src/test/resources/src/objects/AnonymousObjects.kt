open class MyObjectOpenClass {
    open var x: String = ""
}

interface MyObjectInterface {
    var x: String

    fun doNothing()

    fun printSomething() = println("printing something")
}

// This object cannot be accessed by calling myObj().a
fun myObj() = object {
    val a: String = "Hello"
    val b: String = "Kotlin"
}

class AnonymousObjects {
    // Private function, so the return type is the anonymous object type
    private fun foo() = object {
        val x: String = "foo.x"
    }


    // Public function, so the return type is Any (cannot access inner variable)
    fun publicFoo() = object {
        val x: String = "publicFoo.x"
    }

    // Public function, so the return type is the type of the interfaced object
    fun publicFooWithClass() = object: MyObjectOpenClass() {
        override var x: String = "publicFooWithClass.x"
    }

    fun publicFooWithInterface() = object: MyObjectInterface {
        override var x: String = "publicFooWithInterface.x"

        override fun doNothing() {
            println("nothing to be done, inside anonymous object")
        }
    }

    fun bar() {
        val x0 = publicFoo()
        val x1 = foo().x
        val x2 = publicFooWithClass().x
        publicFooWithInterface().doNothing()
        publicFooWithInterface().printSomething()
        val x3 = publicFooWithInterface().x
        println("$x0 $x1 $x2 $x3")
    }

}
