class TestClass {

    val myLambdaWithNoParamAndNoType: () -> Unit = {
        println("I'm the body of a lambda with no param and no ret type")
    }

    val myLambdaWithNoParamAndSring: () -> String = {
        val ret = "I'm the body of a lambda with no param and ret type string"
        println(ret)
        ret
    }

    val myLambdaWithParamAndNoType: (String) -> Unit = { a ->
        println(a)
    }

    val myLambdaWithParamsAndString: (Int, String, Double) -> String = { a, b, c ->
        val ret: String = a.toString() + " " + b + " " + c
        println("Lambda with params and ret string : " + ret)
        ret
    }

    fun oneLine(x: Int) = x * 2

    fun oneLine2(x: Int) : Int {
        return x * 2
    }

    fun funWithEqualSign(b: Int): (String) -> Unit = { a ->
        println(a)
        println(b)
        println("I'm a lambda defined with fun keyword")
    }

    fun funWithEqSign2(b: Int): (String) -> Unit {
        return {
            a ->
            println(a)
            println(b)
            println("blah")
        }
    }

    fun myFun() {
        println("Nothing, I'm a function")
    }
}

object TestLambda {
    fun main(args: Array<String>) {
        val myLocalLambda: () -> Unit = {
            println("I'm in the body of a local lambda expression")
        }

        myLocalLambda()
        val ml = TestClass()
        ml.myFun()
        ml.funWithEqualSign(2)("Hello World")
        ml.myLambdaWithNoParamAndNoType()
        ml.myLambdaWithParamAndNoType("I'm a lambda with no return type and string param")
        ml.myLambdaWithNoParamAndSring()
        ml.myLambdaWithParamsAndString(1, "plus 0.42 is", 1.42)
    }
}