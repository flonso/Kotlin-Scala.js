
fun hello() : Unit = println("Hello World")

class TopLevelCaller {
    fun call() {
        println("Hey")
        hello()
        welcome("Toto")
    }
}

fun main(args: Array<String>) {
    val c = TopLevelCaller()
    c.call()

    hello()
    welcome("Kotlin")
}