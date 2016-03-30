import org.assertj.core.api.*
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.It
import org.jetbrains.spek.api.Spek
import java.math.BigDecimal
import java.nio.file.Path
import java.util.*
import java.util.List

class ResultSpecs : Spek() { init {

    given("A standard success result") {
        val result = Result.success(143)
        on("call onSuccess method on it") {
            var called: Boolean = false
            val onSuccessValue = "ok Spek"

            val onSuccess = result.onSuccess {
                called = true
                Result.success(onSuccessValue)
            }

            it("should have called the method") {
                assertThat(called).isTrue()
                so(called) {
                    isExpectedTo("be true") { this }
                    //assertThat { isTrue() }
                }

                so(called) {
                    satisfy {
                        this == false
                        this == true
                    }
                }
            }

            it("should return the new value of the success") {
                with(onSuccess) {
                    assertThat(this).isNotNull()
                    assertThat(isSuccess()).isTrue()
                    assertThat(getOrElse("")).isEqualTo(onSuccessValue)
                }

                so(onSuccess) {
                    //satisfy { isNotNull() }
                    isExpectedTo("be a success") { isSuccess() }
                    isExpectedTo("have a value of '%s'", onSuccessValue) { getOrElse("") == onSuccessValue }
                }
            }
        }
    }

    given("An optional") {
        val option = Optional.of(255)
        on("test it") {
            it("should not be empty") {
                so(option) {
                    isExpectedTo { isPresent }
                }
            }
        }
    }

    given("A list") {
        val list = mutableListOf(1, 2, 3)
        on("test list order") {
            it("Should be 1 then 2 then 3") {
                withAssert("123") {
                    hasSize(3)
                }
                with(assertThat(list)) {
                    containsExactly(1, 2, 3)
                }
                gen("str")
            }
        }
    }
}
}

fun mystery(t:String): String = t
fun mystery(t:Int): Int = t
fun mystery(t:Any): Any = t

fun <T> gen(t: T) = assertThat(t)

fun withAssert(assertOn: String, builder: AbstractCharSequenceAssert<*,String>.() -> Unit) {
    assertThat(assertOn).builder()
}

class AssertBuilder<T>(val assertOn: T) {

    fun isExpectedTo(message: String = "satisfy", vararg params: Any, predicate: T.() -> Boolean) {
        assertThat(assertOn).`is`(Condition(predicate, message, params))
    }

    fun satisfy(predicate: T.() -> Boolean) {
        isExpectedTo(predicate = predicate)
    }
}

fun <T> It.so(assertOn: T, builder: AssertBuilder<T>.() -> Unit): AssertBuilder<T> {
    return AssertBuilder(assertOn).apply(builder)
}