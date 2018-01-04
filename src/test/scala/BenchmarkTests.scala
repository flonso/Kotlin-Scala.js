class BenchmarkTests extends BlackBoxTest {
  val linkedListImpl = "benchmarks/collections/LinkedList.kt"
  val stackImpl = "benchmarks/collections/Stack.kt"

  test("Benchmarks - LinkedList") {
    val result =
      """
        |LinkedList(0,1,2,3,4,5,6,7,8,9,10)
        |mapping 0
        |mapping 1
        |mapping 2
        |mapping 3
        |mapping 4
        |mapping 5
        |mapping 6
        |mapping 7
        |mapping 8
        |mapping 9
        |mapping 10
        |LinkedList(0,2,4,6,8,10,12,14,16,18,20)
        |LinkedList(1,2,3,4,6,7,9,10)
        |42
        |true false
        |LinkedList(MyListEntry(value=3),MyListEntry(value=95))
        |LinkedList(MyListEntry(value=95))
        |a
        |b
        |c
      """.stripMargin
    assertExecResult(result,
      Seq(linkedListImpl), mainClass = "benchmarks.collections.LinkedListRunner.main")
  }

  test("Benchmarks - DeltaBlue") {
    compileAndCheckIr(Seq("benchmarks/deltablue", "benchmarks/Benchmark.kt", linkedListImpl, stackImpl), "benchmarks.deltablue.DeltaBlue.main")
  }

  test("Benchmarks - LongMicro") {
    compileAndCheckIr(Seq("benchmarks/longmicro", "benchmarks/Benchmark.kt", linkedListImpl), "benchmarks.longmicro.LongMicroAll.main")
  }

  test("Benchmarks - Richards") {
    compileAndCheckIr(Seq("benchmarks/richards", "benchmarks/Benchmark.kt", linkedListImpl), "benchmarks.richards.Richards.main")
  }

}
