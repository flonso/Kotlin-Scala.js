
class Dog(val name: String) {
    fun bark() = println("Barking !")
    fun beHappy() = println("Your dog is moving his tail in happiness")

    override fun toString(): String {
        return "Dog("+name+")"
    }
}