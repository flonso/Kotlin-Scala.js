/*                     __                                               *\
**     ________ ___   / /  ___      __ ____  Scala.js Benchmarks        **
**    / __/ __// _ | / /  / _ | __ / // __/  (c) 2013, Jonas Fonseca    **
**  __\ \/ /__/ __ |/ /__/ __ |/_// /_\ \                               **
** /____/\___/_/ |_/____/_/ | |__/ /____/                               **
**                          |/____/                                     **
\*                                                                      */

/* Based on code from: http://norvig.com/sudopy.shtml */
// Translated to Kotlin by Florian Alonso 2017

package sudoku

fun <T> List<T>.sliding(windowSize: Int): List<List<T>> {
    return this.dropLast(windowSize - 1).mapIndexed { i, s -> this.subList(i, i + windowSize) }
}

object Sudoku {

    fun prefix() = "Sudoku"

    fun run() {
        val values = solve(grid1)
        if (values != null) {
            if (!grid1Solutions.contains(asString(values)))
                println("Invalid solution found: " + asString(values))
        }
        else {
            println("No solution found")
        }
    }

    fun cross(s1: String, s2: String): List<String> {
        return s1.flatMap { val a = it.toString(); s2.map { a + it.toString() } }
    }

    val digits = "123456789"
    val rows = "ABCDEFGHI"
    val cols = digits
    val squares = cross(rows, cols)

    val unitlist =
            cols.map{ it.toString() }.map{ cross(rows, it) } +
            rows.map{ it.toString() }.map { cross(it, cols) } +
            listOf("ABS", "DEF", "GHI").flatMap { val rs = it; listOf("123", "456", "789").map { cross(rs, it) }}

    val units = squares.map{ val s = it; Pair(it, unitlist.filter{ it.contains(s)}) }.toMap()
    val peers = squares.map{ val s = it; Pair(s, units[s]!!.flatten().toSet().filterNot{it == s}) }.toMap()

    val False = mutableMapOf<String, String>()
    fun gridToBoolean(grid: Map<String, String>): Boolean = grid.isNotEmpty()

    // ################ Parse a Grid ################

    fun parseGrid(grid: String): Map<String, String> {
        val values = mutableMapOf<String, String>()
        values += squares.map{ Pair(it, digits) }.toMap()

        val iter = gridValues(grid).iterator()
        while (iter.hasNext()) {
            val (s, d) = iter.next()
            if (digits.contains(d) && assign(values, s, d) == False)
                return False
        }

        return values
    }

    fun gridValues(grid: String): Map<String, String> {
        val chars = grid.map{ it.toString() }.filter{ digits.contains(it) || "0.".contains(it) }
        return squares.zip(chars).toMap()
    }

    // ################ Constraint Propagation ################

    /* Eliminate all the other values (except d) from values[s] and propagate.
     * Return values, except return False if a contradiction is detected. */
    fun assign(values: MutableMap<String, String>, s: String, d: String): Map<String, String> {
        val otherValues = values[s]!!.replace(d, "")

        if (otherValues.all { x -> eliminate(values, s, x.toString()) != False })
            return values
        else
            return False
    }

    /* Eliminate d from values[s]; propagate when values or places <= 2.
     * Return values, except return False if a contradiction is detected. */
    fun eliminate(values: MutableMap<String, String>, s: String, d: String): Map<String, String> {
        if (!values[s]!!.contains(d))
            return values // Already eliminated

        values[s] = values[s]!!.replace(d, "")

        // (1) If a square s is reduced to one value d2, then eliminate d2 from the peers.
        if (values[s]!!.isEmpty()) {
            return False // Contradiction: removed last value
        } else if (values[s]!!.length == 1) {
            val d2 = values[s]
            if (!peers[s]!!.all {s2 -> eliminate(values, s2, d2!!) != False })
            return False
        }

        // (2) If a unit u is reduced to only one place for a value d, then put it there.
        val iter = units[s]!!.iterator()
        while (iter.hasNext()) {
            val u = iter.next()
            val dplaces = u.filter { values[it]!!.contains(d) }
            if (dplaces.isEmpty())
                return False // Contradiction: no place for d
            if (dplaces.size == 1) {
                if (assign(values, dplaces[0], d) == False)
                    return False
            }
        }

        return values
    }

    // ################ Unit Tests ################

    val grid1 = "003020600900305001001806400008102900700000008006708200002609500800203009005010300"
    val grid2 = "4.....8.5.3..........7......2.....6.....8.4......1.......6.3.7.5..2.....1.4......"
    val hard1 = ".....6....59.....82....8....45........3........6..3.54...325..6.................."
    val grid1Solutions = listOf(
            "483921657967345821251876493548132976729564138136798245372689514814253769695417382")
    val grid2Solutions = listOf(
            "417369825632158947958724316825437169791586432346912758289643571573291684164875293")
    val hard1Solutions = listOf(
            "874196325359742618261538497145679832783254169926813754417325986598461273632987541",
            "834596217659712438271438569745169382923854671186273954417325896562987143398641725")

    fun test() {
        require(squares.size == 81)
        require(unitlist.size == 27)
        require(squares.all {s -> units[s]!!.size == 3})
        require(squares.all{s -> peers[s]!!.size == 20})
        require(units["C2"] == listOf(listOf("A2", "B2", "C2", "D2", "E2", "F2", "G2", "H2", "I2"),
                listOf("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9"),
                listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3")))
        require(peers["C2"] == setOf("A2", "B2", "D2", "E2", "F2", "G2", "H2", "I2",
                "C1", "C3", "C4", "C5", "C6", "C7", "C8", "C9",
                "A1", "A3", "B1", "B3"))
        println("All tests pass")
    }

    // ################ Display as 2-D grid ################

    // Display these values as a 2-D grid.
    fun display(values: Map<String, String>) {
        val width = squares.map{values[it]!!.length}.max()!! + 1
        val line = (0 .. 2).map { "-".repeat(width * 3) }.joinToString("+")
        for (r in rows.map{ it.toString() }) {
            val cells = cols.map { c -> center(values[r + c]!!, width) }
            println(cells.sliding(3).map{ it.joinToString("") }.joinToString("|"))
            if ("CF".contains(r))
                println(line)
        }
        println()
    }

    fun asString(values: Map<String, String>): String =
        rows.flatMap { r ->  cols.map { c -> values[r.toString() + c.toString()] }}.joinToString("")

    // ################ Search ################

    fun solve(grid: String) = search(parseGrid(grid))

    // Using depth-first search and propagation, try all possible values.
    fun search(values: Map<String, String>): Map<String, String>? {
        if (values.isEmpty())
            return null // Failed earlier

        if (squares.all{s -> values[s]!!.length == 1})
            return values // Solved!

        // Chose the unfilled square s with the fewest possibilities
        val (s, n) = values.filter{ it.component2().length > 1 }.minBy{ it.component2().length }!!

        return values[s]!!.map { d ->
            val solution = values.toMutableMap() // clone
            if (assign(solution, s, d.toString()) != False)
                search(solution)
            else
                null
        }.find{ it != null }
    }

    // ################ Utilities ################

    fun center(s: String, max: Int, pad: String = " ") {
        fun repeat(s: String, n: Int) = s.repeat(n)

        val padLen = max - s.length
        if (padLen <= 0)
            s
        else
            repeat(pad, padLen / 2) + s + repeat(pad, (padLen + 1) / 2)
    }

}
