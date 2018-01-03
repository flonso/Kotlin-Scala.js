package benchmarks.collections

interface MyMutableList<E> {

    val size: Int

    val lastIndex: Int

    fun isEmpty(): Boolean

    fun contains(element: E): Boolean

    fun add(element: E): Boolean

    fun remove(element: E): Boolean

    fun removeAt(index: Int): E

    fun get(index: Int): E

    fun indexOf(element: E): Int

    fun <F>map(p: (E) -> F): LinkedList<F>

    fun joinToString(separator: String): String
}

class Node<E>(var value: E, var next: Node<E>?) {
    override fun toString(): String {
        return "Node("+ value +")"
    }
}

class LinkedList<E>: MyMutableList<E> {
    var head: Node<E>? = null

    override val size: Int
        get() {
            return _length(head)
        }

    override val lastIndex: Int
        get() {
            return _length(head) - 1
        }

    constructor()

    constructor(element: E) {
        head = Node(element, null)
    }

    constructor(values: Array<E>) {
        for (v in values) {
            add(v)
        }
    }

    override fun isEmpty(): Boolean = head == null

    override fun contains(element: E): Boolean = _contains(element, head)

    override fun add(element: E): Boolean {
        val newNode = Node(element, null)

        if (head == null) {
            head = newNode
        } else {
            var lastNode = head
            while (lastNode!!.next != null) {
                lastNode = lastNode.next
            }

            lastNode.next = newNode
        }

        return true
    }

    override fun remove(element: E): Boolean {
        if (!contains(element)) return false

        val index = indexOf(element)

        if (index == 0) {
            if (size == 1)
                head = null
            else
                head = head!!.next
        }
        else {
            val nodeBefore = _get(index - 1)
            val node = nodeBefore.next!!

            nodeBefore.next = node.next
        }

        return true
    }

    override fun removeAt(index: Int): E {
        val node = _get(index)
        remove(node.value)

        return node.value
    }

    override fun get(index: Int): E = _get(index).value

    override fun indexOf(element: E): Int = _indexOf(element, head)

    override fun <F> map(p: (E) -> F): LinkedList<F> {
        val newList = LinkedList<F>()
        for (i in 0 .. size) {
            val e = get(i)
            newList.add(p(e))
        }

        return newList
    }

    override fun joinToString(separator: String): String {
        var ret = ""
        for (i in 0 until size) {
            val e = get(i)
            ret += e
            if (i != lastIndex){
                ret += separator
            }
        }

        return ret
    }

    private fun _length(node: Node<E>?): Int {
        if (node == null) return 0
        else return _length(node.next) + 1
    }

    private fun _contains(element: E, node: Node<E>?): Boolean {
        return if (node == null) false
        else {
            if (node.value == element) true
            else _contains(element, node.next)
        }
    }

    private fun _indexOf(element: E, node: Node<E>?): Int = _indexOf(element, node, 0)

    private fun _indexOf(element: E, node: Node<E>?, index: Int): Int {
        if (node == null) return -1
        else {
            if (node.value == element) return index
            else return _indexOf(element, node.next, index + 1)
        }
    }

    private fun _get(index: Int): Node<E> {
        return _get(index, head, 0)
    }

    private fun _get(index: Int, node: Node<E>?): Node<E> {
        return _get(index, node, 0)
    }

    private fun _get(index: Int, node: Node<E>?, currentIndex: Int): Node<E> {
        if (node == null) throw Exception("Undefined index")
        else {
            if (currentIndex == index) return node
            else return _get(index, node.next, currentIndex + 1)
        }
    }

    override fun toString(): String {
        return "LinkedList(" + joinToString(",") + ")"
    }
}

fun LinkedList<Int>.sum(): Int {
    var sum = 0
    for (i in 0 until size) {
        sum += get(i).toInt()
    }

    return sum
}

fun LinkedList<Double>.sum(): Double {
    var sum = 0.0
    for (i in 0 until size) {
        sum += get(i).toDouble()
    }

    return sum
}


class MyListEntry(val value: Int) {
    override fun toString(): String {
        return "MyListEntry(value=$value)"
    }
}

object LinkedListRunner {
    fun main(args: Array<String>) {
        val l = LinkedList<Int>()

        for (i in 0 .. 10) {
            l.add(i)
        }
        println(l)

        l.remove(5)
        l.removeAt(0)
        l.removeAt(6)

        println(l)

        println(l.sum())
        println("" + l.contains(3) + " " + l.contains(42))

        val l2 = LinkedList<MyListEntry>()
        val e1 = MyListEntry(3)
        val e2 = MyListEntry(95)

        l2.add(e1)
        l2.add(e2)
        println(l2)
        l2.remove(e1)
        println(l2)
    }
}