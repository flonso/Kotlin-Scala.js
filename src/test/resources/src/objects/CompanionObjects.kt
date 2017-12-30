class MyObjectClass(val inner: String) {
    companion object MyCompanion {
        val inner = "Hello from a companion object"
        // FIXME: instance = MyCompanion should be translated to instance = this (to avoid loops)
        val instance = MyObject.MyOtherObject
    }

    object MyObject {
        val inner = "Hello from an object"

        object MyOtherObject {
            val inner = "Hello from an object within an object"
        }
    }

    fun new() = MyObjectClass("some new inner value")

}

fun myfun() = MyObjectClass.MyObject