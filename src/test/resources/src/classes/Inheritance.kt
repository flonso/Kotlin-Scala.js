open class A(val a: Int, val b: String) {

    class A1: A(0, "coucou"){}
    class A2: A(1, "hey") {}
    class A3: A(2, "hello") {}
}

class B(val c: Int) : A(c,"32") {

}

open class MyEmptyClass {
    fun dummy() = println("Hello World")
}

class B2 : MyEmptyClass()