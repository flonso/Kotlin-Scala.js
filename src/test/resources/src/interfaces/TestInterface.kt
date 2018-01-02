
object TestInterface {
    fun main(args: Array<String>) {
        val myClass = MyInterfacedClass("Hello World")
        println(myClass)

        myClass.changeCToSomeValue()
        println(myClass)

        myClass.dog.bark()
        myClass.petYourDog()
        myClass.dummyFun()

        myClass.dog = Dog("Lassie")
        println(myClass)

        myClass.assignSomeValues()
        println(myClass)
        println(myClass.abstractMethod(2, "is a great number"))
        myClass.doSomething()

    }
}