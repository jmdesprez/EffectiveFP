import exceptions.ValidationException
import exceptions.isFatal
import java.io.IOException

fun main(args: Array<String>) {
    val success: Result<Int> = Result.success(12)
    val error: Result<Int> = Result.failure("It's an error")
    println(success.isSuccess() == true)
    println(success.isFailure() == false)
    println(error.isSuccess() == false)
    println(error.isFailure() == true)

    val ofSuccess: Result<Int> = Result.of { 15 }
    val ofFailure: Result<Int> = Result.of { throw IOException("IO Test") }
    println(ofSuccess.isSuccess() == true)
    println(ofSuccess.isFailure() == false)
    println(ofFailure.isSuccess() == false)
    println(ofFailure.isFailure() == true)

    println(error.withValue { Result.success("this value is $this") }.isFailure())
    success
            .withValue {
                println("It's a success and the value is $this")
                Result.success(this)
            }
            .withError {
                println("It's a error and the value is $this")
                Result.failure(this)
            }

    error.withValue { println("It's a success and the value is $this"); Result.success(this) }
            .withError { println("It's a error and the value is $this"); Result.failure(this) }

    // recover
    println(Result.failure<String>("error").withError { Result.success("recover from $this!") })
    Result.failure<String>("error").withError { Result.success("recover from $this!") }.withValue { Result.success(this.hashCode()) }
    Result.failure<Int>("error parse int").withError { Result.success(0) }.withValue { Result.success("converted is $this") }
    Result.failure<Int>("err").fold({ this }, { 0 })

    println(success.fold({ "success $this" }, { "error $this" }))
    println(error.fold({ "success $this" }, { "error $this" }))

    Result.failure<Int>("test").fold({ 200 }, {
        when (this) {
            is IllegalArgumentException -> 400
            is ValidationException -> 400
            is SecurityException -> 403
            else -> 500
        }
    })

    println("Extensions")
    println(Result.success(true).not())
    println({ 45 }.toResult().isSuccess())
    println({ throw IllegalArgumentException() }.toResult().isSuccess())
    println(14.toResult())
    println(IllegalArgumentException().toResult())


    println(Result.success(14).withValue { Result.failure<Any>("test") }.isFailure() == true)

    val rString = Result.success("ok")
    val rAny: Result<out Any> = rString
    val rAny2: Result<Any> = Result.success("ok!")

    println(Result.success(-45).validate { this > 0 })
    println(Result.failure<String>("Original message").validate { length > 10 })
    // http response code
    println(Result.success(300).validate {
        when (this) {
            in 200..299 -> true
            else -> false
        }
    })

    println(Result.success(300).validate { this in 200..299 })
    // https://kotlinlang.org/docs/reference/extensions.html#extensions-are-resolved-statically
    infix fun IntRange.contains(value: Int) = this.contains(value)
    println(Result.success(250).validate { 200..299 contains this })

    println("Operator overloading")
    println(!Result.success(true))
    println(40 in Result.success(40))
    println(40 in Result.success(450))
    println(40 in Result.failure<Int>("nope"))
    println("Infix operator")
    println(Result.success(40) contains 40)
    println(Result.success(450) contains 40)
    println(Result.failure<Int>("nope") contains 40)

    Result.success(200).onSuccess { responseCode -> println("OK! response is [$responseCode]") ; Result.success(responseCode) }
    Result.success(200).logSuccess { println("OK! response is [$this]") }
    Result.success(200).withValue { println("OK! reponse is [$this]") ; Result.success(this) }

    println(Result.success(18).getOrElse(-1))
    println(Result.failure<Int>("oups").getOrElse(-1))
    println(Result.success(45) or 4)
    println(Result.failure<Int>("I did it again") or -1)

    val handleStatus: (Int) -> Result<Int> = { responseCode ->
        when (responseCode) {
            in 200..299 -> Result.success(responseCode)
            else -> Result.failure("Bad return code: $responseCode")
        }
    }
    Result.success(201).withValue(handleStatus)

    val handleStatusFun = fun(responseCode: Int): Result<Int> {
        return when (responseCode) {
            in 400..499 -> Result.failure("Bad request: $responseCode")
            in 500..599 -> Result.failure("Olol they failed: $responseCode")
            else -> Result.success(responseCode)
        }
    }
    Result.success(201).withValue(handleStatusFun)

    Result.success(1).logSuccess(::log)
    Result.success(1).logSuccess({ logTruc(this) })
    Result.success(1).withValue(::tryDoSomething)
    Result.success(1).onSuccess(::tryDoSomething)

}

fun tryDoSomething(value: Int): Result<String> = Result.success("it's works with $value")

fun log(i: Int) = println(i)

fun logTruc(i: Int): Boolean {
    println(i)
    return true
}