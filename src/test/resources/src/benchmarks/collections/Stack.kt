package benchmarks.collections

class Stack<T> {
    private val stack = LinkedList<T>()

    fun pop(): T {
        val last = stack.lastIndex
        return stack.removeAt(last)
    }

    fun push(elem: T): Stack<T> {
        stack.add(elem)

        return this
    }

    fun isEmpty(): Boolean {
        return stack.isEmpty()
    }
}
