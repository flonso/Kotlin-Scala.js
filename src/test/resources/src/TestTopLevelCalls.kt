
fun welcome(other: String) = println("Hello $other")
fun hello() : Unit = println("Hello World")

class TopLevelCaller {
    fun call() = {
        println("Hey")
    }
}

fun main(args: Array<String>) {
    hello()
    welcome("Kotlin")
}