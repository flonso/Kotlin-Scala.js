class MyObjectClass {
    companion object MyCompanion {
        val inner = "Hello from a companion object"
    }

    // FIXME: A call to MyObjectClass.MyObject.inner should work !!!
    object MyObject {
        val inner = "Hello from an object"

        object MyOtherObject {
            val inner = "Hello from an object within an object"
        }
    }
}

fun main(args: Array<String>) {
    //fun myfun(): MyObjectClass.MyObject = MyObjectClass.MyObject

    //myfun().inner

    println(MyObjectClass.MyObject.MyOtherObject.inner)
    println(MyObjectClass.MyObject.inner)


}