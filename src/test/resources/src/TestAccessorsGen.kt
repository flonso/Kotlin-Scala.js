
class MyClassWithAccessors {

    var myVar: Int = 0
        get
        set(value) {
            if (value >= 2)
                field = value - 1
            else
                field = -1
        }

    val name: String
        get() = "name"

    val isEmpty: Boolean
        get() {
            if (this.myVar >= 0)
                return true
            else
                return false
        }

    val id: Int = 1

    var tmp: String = "tmp"
        set(value) {
            field = value + "_set"
        }
}

object TestAccessorsGen {
    fun main(args: Array<String>) {
        val a = MyClassWithAccessors()

        println(a.id)
        println(a.name)
        println(a.myVar)
        a.myVar = 4
        println(a.myVar)
        a.myVar = 0
        println(a.myVar)
        println(a.tmp)
        a.tmp = "tmp2"
        println(a.tmp)

    }
}