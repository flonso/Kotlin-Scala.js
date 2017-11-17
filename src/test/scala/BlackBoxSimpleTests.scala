

class BlackBoxSimpleTests extends BlackBoxTest {

  test("TestJsFunc.kt") {
    val res = consoleToString {
      println(1)
      println(3)
      println(6)

      def _asFloatJSNotation(d: Double): String = {
        if (d == Float.MinValue)
          return "-3.4028234663852886e+38"

        return d.toString.replace("E", "e+")
      }

      println(_asFloatJSNotation(Double.MaxValue) + " "
        + _asFloatJSNotation(Double.MinValue) + " "
        + Double.PositiveInfinity + " "
        + Double.NegativeInfinity + " "
        + Double.NaN
      )


      // TODO: Check why there is more precision in the generated JS file than when using Float.MinValue
      println(_asFloatJSNotation(Float.MaxValue) + " "
        + _asFloatJSNotation(Float.MinValue) + " "
        + Float.PositiveInfinity + " "
        + Float.NegativeInfinity + " "
        + Float.NaN + " "
      )

      println("9223372036854776000" + " "
        + "-9223372036854776000"
      )
    }

    assertExecResult(res, "TestJsFunc.kt", mainClass = "kotlin.js.TestJsFuncKt")
  }

  test("TestAccessorsGen.kt") {
    val res = consoleToString {

      class MyClassWithAccessors {
        private var _myVar: Int = 0
        def myVar: Int = _myVar
        def myVar_=(value: Int) = {
          if (value >= 2)
            _myVar = value - 1
          else
            _myVar = -1
        }

        val _name: String = "name"
        def name(): String = _name

        val id: Int = 1

        private var _tmp: String = "tmp"

        def tmp = _tmp
        def tmp_= (value: String): Unit = {
          _tmp = value + "_set"
        }
      }


      val a = new MyClassWithAccessors

      printlnJSFormat(a.id)
      printlnJSFormat(a.name)
      printlnJSFormat(a.myVar)
      a.myVar = 4
      printlnJSFormat(a.myVar)
      a.myVar = 0
      printlnJSFormat(a.myVar)
      printlnJSFormat(a.tmp)
      a.tmp = "tmp2"
      printlnJSFormat(a.tmp)
    }

    assertExecResult(res, "TestAccessorsGen.kt", mainClass = "TestAccessorsGenKt")
  }

  // TODO: Complete this test
  test("TestThisKeyword.kt") {
    class MyClass(val num: Int) {
      def myDef: Unit = {
        println("myDef")
      }

      def equals(b: MyClass): Boolean = {
        this.myDef
        this.num == b.num
      }
    }

    val res = consoleToString {
      val a = new MyClass(1)
      val b = new MyClass(1)
      val c = new MyClass(2)

      printlnJSFormat(a.equals(b))
      printlnJSFormat(a.equals(c))
    }
    assertExecResult(res, "TestThisKeyword.kt", mainClass = "TestThisKeywordKt")
  }

  test("TestIsOperator.kt") {
    val expectedResult =
      """
        |1.7 is a Double
        |1.7 is not an Int
        |1.7 is not a Long
        |1.7 is not Derived class
        |4 is an Int
        |4 is a Double
        |4 is not a Long
        |4 is not Derived class
        |23432542300000 is a Long
        |23432542300000 is not an Int
        |23432542300000 is not a Double
        |23432542300000 is not Derived class
        |Derived with p = 4 is not an Int
        |Derived with p = 4 is not a Double
        |Derived with p = 4 is not a Long
        |Derived with p = 4 is Base class
        |Derived with p = 4 is Derived class
        |Base with p = 4 is not an Int
        |Base with p = 4 is not a Double
        |Base with p = 4 is not a Long
        |Base with p = 4 is Base class
        |Base with p = 4 is not Derived class
      """.stripMargin

    assertExecResult(expectedResult, "TestIsOperator.kt", mainClass = "TestIsOperatorKt")
  }

  // FIXME: println Float and Double with decimal points
  test("TestMethodsBaseTypes.kt") {
    val expectedResult = {
    """
      |1000000
      |1234567890123456
      |999999999
      |4293713502
      |3530134674
      |=== Type cast : from Int to X ===
      |Int 1
      |Byte 1
      |Short 1
      |Long 1
      |Float 1
      |Double 1
      |=== Type cast : from Byte to X ===
      |Byte 1
      |Int 1
      |Short 1
      |Long 1
      |Float 1
      |Double 1
      |=== Type cast : from Short to X ===
      |Short 1
      |Int 1
      |Byte 1
      |Long 1
      |Float 1
      |Double 1
      |=== Type cast : from Long to X ===
      |Long 1
      |Int 1
      |Byte 1
      |Short 1
      |Float 1
      |Double 1
      |=== Type cast : from Float to X ===
      |Float 1
      |Int 1
      |Byte 1
      |Short 1
      |Long 1
      |Double 1
      |=== Type cast : from Double to X ===
      |Double 1
      |Int 1
      |Byte 1
      |Short 1
      |Float 1
      |Long 1
    """.stripMargin
    }

    assertExecResult(expectedResult, "TestMethodsBaseTypes.kt", mainClass = "TestMethodsBaseTypesKt")
  }

  test("TestSimplePrint.kt") {
    assertExecResult("1", "TestSimplePrint.kt", mainClass = "TestSimplePrintKt")
  }

  test("TestBinaryOps.kt") {
    val scalaResult = consoleToString {
      class MyClassA(val num: Int)

      val a = new MyClassA(0)
      val b = new MyClassA(0)
      val c = a
      val d = new MyClassA(1)

      printlnJSFormat(a === b)
      printlnJSFormat(b === a)
      printlnJSFormat(a == b)
      printlnJSFormat(b == a)
      printlnJSFormat(a === a)
      printlnJSFormat(b === b)
      printlnJSFormat(a === c)
      printlnJSFormat(a !== b)
      printlnJSFormat(a != d)

      printlnJSFormat(1.0f)
      printlnJSFormat(2 + 2)
      printlnJSFormat(2 - 2)
      printlnJSFormat(2 * 2)
      printlnJSFormat(2 / 2)
      printlnJSFormat(2 % 2)
      printlnJSFormat(2 | 2)
      printlnJSFormat(2 & 2)
      printlnJSFormat(2 ^ 2)
      printlnJSFormat(2 << 1)
      printlnJSFormat(-10 >> 2)
      printlnJSFormat(-10 >>> 2)

      printlnJSFormat(true & true)
      printlnJSFormat(true & false)

      printlnJSFormat(true | false)
      printlnJSFormat(true | true)

      printlnJSFormat(2L + 2L)
      printlnJSFormat(2L - 2L)
      printlnJSFormat(2L * 2L)
      printlnJSFormat(2L / 2L)
      printlnJSFormat(2L % 2L)
      printlnJSFormat(2L | 2L)
      printlnJSFormat(2L & 2L)
      printlnJSFormat(2L ^ 2L)
      printlnJSFormat(2L << 1)
      printlnJSFormat(-10L >> 2)
      printlnJSFormat(-1000000L >>> 2)

      printlnJSFormat(2.5 + 2.5)
      printlnJSFormat(2.5 - 2.5)
      printlnJSFormat(2.5 * 2.5)
      printlnJSFormat(2.5 / 2.5)
      printlnJSFormat(2.5 % 2.5)

      printlnJSFormat(2.5f + 2.5f)
      printlnJSFormat(2.5f - 2.5f)
      printlnJSFormat(2.5f * 2.5f)
      printlnJSFormat(2.5f / 2.5f)
      printlnJSFormat(2.5f % 2.5f)

      printlnJSFormat(2L + 2)
      printlnJSFormat(100000L + 2147483647)
      printlnJSFormat(2147483647L + 10000)

      val e = "First"
      val f = "Second"

      println(e + f)
    }
    assertExecResult(scalaResult, "TestBinaryOps.kt", mainClass = "TestBinaryOpsKt")
  }

  test("TestMultipleConstructors.kt") {
    assertExecResult(
      """
        |Init 5
        |5
        |9
        |5
        |7
        |b 6
        |5
      """.stripMargin, "TestMultipleConstructors.kt", mainClass = "TestMultipleConstructorsKt")
  }

  test("TestEqualities.kt") {
    val result = consoleToString {
      val a = 1
      println(a == 1)
      println(1 == 2)
      println(2.0 == 3.0)
      println(true == false)
      println(false == false)

      println(a != 1)
      println(1 != 2)
      println(2.0 != 3.0)
      println(true != false)
      println(false != false)

      println(!true)
      println(!false)

      println("a" == "b")
      println("a" == "a")
    }
    assertExecResult(result, "TestEqualities.kt", mainClass = "TestEqualitiesKt")
  }

  test("TestIf.kt") {
    assertExecResult(
      """
        |1
        |1
        |1
        |1
        |1
      """.stripMargin, "TestIf.kt", mainClass = "TestIfKt")
  }

  //  test("TestNullable.kt") {
  //    assertExecResult(
  //      """7
  //        |7
  //        |null
  //        |7
  //        |-1
  //        |NonNullnull""".stripMargin, "TestNullable.kt")
  //  }


  test("TestTryCatch.kt") {
    assertExecResult(
      """Exception caught
        |Reached finally""".stripMargin, "TestTryCatch.kt", mainClass = "TestTryCatchKt")
  }

  test("TestClassExtension.kt") {
    assertExecResult(
      """
        |8
        |32
        |D.bar
        |C.baz
      """.stripMargin, "TestClassExtension.kt", mainClass = "TestClassExtensionKt")
  }

  test("TestTopClassExtension.kt") {
    assertExecResult(
      """
        |10
      """.stripMargin, "TestTopClassExtension.kt", mainClass = "TestTopClassExtensionKt")
  }

  test("TestStringConcat.kt") {
    assertExecResult("5 Hello World", "TestStringConcat.kt", mainClass = "TestStringConcatKt")
  }

  test("TestAnonClass.kt") {
      assertExecResult("Hello World", "TestAnonClass.kt", mainClass = "TestAnonClassKt")
  }


  test("TestGenParentConstructor.kt") {
    assertExecResult("5 1", "TestGenParentConstructor.kt", mainClass = "TestGenParentConstructorKt")
  }

  test("TestAssignationOrder.kt") {
    assertExecResult(
      """
        |true
        |6
        |12
        |false
        |5
        |11
      """.stripMargin, "TestAssignationOrder.kt", mainClass = "TestAssignationOrderKt")
  }

  test("TestHighFunction.kt") {
    assertExecResult(
      """
        |12
        |5.5
        |10.5
        |11
        |true
        |4
        |21
        |1
      """.stripMargin, "TestHighOrderFunction.kt", mainClass = "TestHighOrderFunctionKt")
  }

    test("TestLambdaTopLevel.kt") {
      assertExecResult(
        """
          |11
        """.stripMargin, "TestLambdaTopLevel.kt", mainClass = "TestLambdaTopLevelKt")
  }

  test("TestLambda.kt") {
    assertExecResult(
      """
        |I'm in the body of a local lambda expression
        |Nothing, I'm a function
        |Hello World
        |2
        |I'm a lambda defined with fun keyword
        |I'm the body of a lambda with no param and no ret type
        |I'm a lambda with no return type and string param
        |I'm the body of a lambda with no param and ret type string
        |Lambda with params and ret string : 1 plus 0.42 is 1.42
      """.stripMargin, "TestLambda.kt", mainClass = "TestLambdaKt")
  }

  //  test("TestTypeCast.kt") {
  //    assertExecResult(
  //      """
  //        |15
  //        |12.5
  //      """.stripMargin, "TestTypeCast.kt")
  //  }

  test("TestArraysBase.kt") {
    val result = consoleToString {
      val a = Seq(1, 2, 3, 12.5)
      printlnJSFormat(a(0))
      printlnJSFormat(a(3))
      printlnJSFormat(a.size)

      println(3)
    }
    assertExecResult(result, "TestArraysBase.kt", mainClass = "TestArraysBaseKt")
  }

  /*
  test("TestArrayIterator.kt") {
    val result = consoleToString {
      val a = Seq(3, 2, 3, 12.5)
      a.foreach(printlnJSFormat)

      val b = Seq(12, 42)
      b.foreach(println)
    }
    assertExecResult(result, "TestArrayIterator.kt", mainClass = "TestArrayIteratorKt")
  }*/

  test("TestVarIncrease.kt") {
    assertExecResult(
      """
        |30
        |16
        |60
        |60
        |6
      """.stripMargin, "TestVarIncrease.kt", mainClass = "TestVarIncreaseKt")
  }



  test("TestDynamic.kt") {
    assertExecResult("2", "TestDynamic.kt", mainClass = "TestDynamicKt")
  }

  test("TestTopLevelCalls.kt") {
    assertExecResult(
      """
        |Hello World
        |Hello Kotlin
      """.stripMargin, "TestTopLevelCalls.kt", mainClass = "TestTopLevelCallsKt")
  }

}

