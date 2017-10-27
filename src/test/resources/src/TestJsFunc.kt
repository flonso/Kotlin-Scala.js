
fun main(args: Array<String>) {
    val c = MyClassForJsFunc()

    c.dummy()
}

class MyClassForJsFunc {
    fun dummy() {
        /*
        // This won't work, since we use a matching on the string itself
        val str = "Kotlin.identityHashCode"
        js(str)(this)
        */

        val res1 = js("Kotlin.identityHashCode")(this)
        val res2 = js("Kotlin.identityHashCode")(3)
        val res3 = js("Kotlin.identityHashCode")(1 + 2 + 3)

        println(res1)
        println(res2)
        println(res3)
    }
}