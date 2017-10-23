class MyClass(val num: Int) {
    fun myDef(): Unit {
        println("myDef")
    }

    fun equals(b: MyClass): Boolean {
        this.myDef()
        return this.num == b.num
    }
}

fun main(args: Array<String>) {
    val a = MyClass(1)
    val b = MyClass(1)
    val c = MyClass(2)

    println(a.equals(b))
    println(a.equals(c))
}