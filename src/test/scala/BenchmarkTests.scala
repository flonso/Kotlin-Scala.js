class BenchmarkTests extends BlackBoxTest {
  val linkedListImpl = "benchmarks/collections/LinkedList.kt"
  val stackImpl = "benchmarks/collections/Stack.kt"

  test("Benchmarks - LinkedList") {
    val result =
      """
        |LinkedList(0,1,2,3,4,5,6,7,8,9,10)
        |LinkedList(1,2,3,4,6,7,9,10)
        |42
        |true false
        |LinkedList(MyListEntry(value=3),MyListEntry(value=95))
        |LinkedList(MyListEntry(value=95))
      """.stripMargin
    assertExecResult(result,
      Seq(linkedListImpl), mainClass = "benchmarks.collections.LinkedListRunner.main")
  }

  test("Benchmarks - DeltaBlue") {
    compileAndCheckIr(Seq("benchmarks/deltablue", "benchmarks/Benchmark.kt", linkedListImpl, stackImpl), "")
  }

  test("Benchmarks - LongMicro") {
    compileAndCheckIr(Seq("benchmarks/LongMicro.kt", "benchmarks/Benchmark.kt", linkedListImpl), mainPath = "benchmarks.longmicro.LongMicroAll.main")
  }

}
