import exceptions.ValidationException
import exceptions.isFatal

val <T> (() -> T).result: () -> Result<T> get() = { Result.of(this) }
val <T, A> ((A) -> T).result: (A) -> Result<T> get() = { a -> Result.of({ this(a) }) }
val <T, A, B> ((A, B) -> T).result: (A, B) -> Result<T> get() = { a, b -> Result.of({ this(a, b) }) }
val <T, A, B, C> ((A, B, C) -> T).result: (A, B, C) -> Result<T> get() = { a, b, c -> Result.of({ this(a, b, c) }) }
val <T, A, B, C, D> ((A, B, C, D) -> T).result: (A, B, C, D) -> Result<T> get() = { a, b, c, d -> Result.of({ this(a, b, c, d) }) }

fun <T> (() -> T).toResult() = { Result.of(this) }
fun <T : Exception> T.toResult() = Result.failure<Any>(this)
fun <T> T.toResult() = Result.success(this)
fun <T> T.validate(errorMessage: String = "Validation error", validator: T.() -> Boolean): Result<T> = toResult().validate(errorMessage, validator)
fun Result<Boolean>.validate(errorMessage: String = "Validation error") = validate(errorMessage, { this == true })
operator fun Result<Boolean>.not(): Result<Boolean> = withValue { Result.success(!this) }

sealed class Result<S> {

    abstract fun isSuccess(): Boolean
    fun isFailure(): Boolean = !isSuccess()

    fun logSuccess(operation: S.() -> Unit): Result<S> = withValue { this@Result.apply { operation() } }
    fun logSuccess2(operation: S.() -> Unit): Result<S> = withValue { this.operation(); this@Result }
    fun logSuccess3(operation: S.() -> Unit): Result<S> = withValue { this@Result.apply { this@withValue.operation() } }
    //abstract fun logSuccess(operation: S.() -> Unit): Result<S>
    abstract fun <T> withValue(operation: S.() -> Result<T>): Result<T>

    fun <T> onSuccess(operation: (S) -> Result<T>): Result<T> = withValue(operation)

    abstract fun logFailure(operation: Exception.() -> Unit): Result<S>
    abstract fun withError(operation: Exception.() -> Result<S>): Result<S>
    fun onFailure(operation: (Exception) -> Result<S>): Result<S> = withError(operation)

    abstract fun validate(errorMessage: String = "Validation error", validator: S.() -> Boolean): Result<S>

    abstract fun <T> fold(foldValue: S.() -> T, foldError: Exception.() -> T): T
    fun getOrElse(defaultValue: S): S = getOrElse { defaultValue }
    fun getOrElse(provider: Exception.() -> S): S = fold({ this }, provider)
    //abstract fun getOrElse(provider: () -> S): S
    infix fun or(defaultValue: S) = getOrElse(defaultValue)

    infix fun orElse(defaultValue: S) = getOrElse(defaultValue)

    // I don't like this one ;)
    abstract fun getOrThrow(): S

    abstract operator infix fun contains(value: S): Boolean

    companion object {
        fun <S> success(value: S): Result<S> = Success(value)
        fun <S> failure(error: Exception): Result<S> = if (error.isFatal()) throw error else Failure(error)
        fun <S> failure(error: String): Result<S> = Failure(ValidationException(error))

        fun <S> of(operation: () -> S): Result<S> =
                try {
                    success(operation())
                } catch(e: Exception) {
                    failure(e)
                }

        fun <S> chain(first: () -> Result<S>, vararg actions: () -> Result<S>): Result<S> {
            //return actions.fold(first(), { prev, next -> prev.onFailure(fun(Exception) = next()) })
            return actions.fold(first(), { prev, next -> prev.onFailure { next() } })
        }
    }

    private class Success<S>(val value: S) : Result<S>() {
        override fun isSuccess(): Boolean = true
        override fun getOrThrow(): S = value

        override fun <T> withValue(operation: S.() -> Result<T>): Result<T> =
                try {
                    value.operation()
                } catch(e: Exception) {
                    Result.failure(e)
                }


        override fun withError(operation: Exception.() -> Result<S>): Result<S> = this
        override fun logFailure(operation: Exception.() -> Unit): Result<S> = this

        //override fun <T> fold(foldValue: (S) -> T, foldError: (Exception) -> T): T = foldValue(value)
        override fun <T> fold(foldValue: S.() -> T, foldError: Exception.() -> T): T = value.foldValue()

        override fun validate(errorMessage: String, validator: S.() -> Boolean): Result<S> {
            if (value.validator()) {
                return this
            } else {
                return failure(errorMessage)
            }
        }

        override fun contains(value: S): Boolean = this.value == value

        override fun toString(): String {
            return "Success[${value.toString()}]"
        }
    }

    private class Failure<S>(val error: Exception) : Result<S>() {

        init {
            // this is already checked in the companion object
            if (error.isFatal()) throw error
        }

        override fun isSuccess(): Boolean = false
        //override fun getOrElse(provider: () -> S): S = provider()
        override fun getOrThrow(): S = throw error

        override fun <T> withValue(operation: S.() -> Result<T>): Result<T> = Result.failure(error)

        override fun logFailure(operation: Exception.() -> Unit): Result<S> {
            try {
                error.operation()
                return this
            } catch(e: Exception) {
                return Result.failure(e)
            }
        }

        override fun withError(operation: Exception.() -> Result<S>): Result<S> {
            try {
                return error.operation()
            } catch(e: Exception) {
                return Result.failure(e)
            }
        }

        override fun <T> fold(foldValue: S.() -> T, foldError: Exception.() -> T): T = error.foldError()

        override fun validate(errorMessage: String, validator: S.() -> Boolean): Result<S> = this

        override fun contains(value: S): Boolean = false

        override fun toString(): String {
            return "Error[${error.toString()}]"
        }
    }
}
