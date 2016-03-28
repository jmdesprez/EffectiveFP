sealed class MayBe<T> {

    abstract fun isPresent(): Boolean
    fun isAbsent(): Boolean = !isPresent()

    abstract fun <U> withValue(operation: T.() -> MayBe<U>): MayBe<U>
    abstract fun withoutValue(operation: () -> MayBe<T>): MayBe<T>

    companion object {
        @Suppress("CAST_NEVER_SUCCEEDS")
        fun <T> absent(): MayBe<T> = None as MayBe<T>

        fun <T> some(value: T): MayBe<T> = Some(value)
    }

    private class Some<T>(val value: T) : MayBe<T>() {

        override fun isPresent(): Boolean = true

        override fun <U> withValue(operation: T.() -> MayBe<U>): MayBe<U> = value.operation()
        override fun withoutValue(operation: () -> MayBe<T>): MayBe<T> = this

        override fun toString(): String = "Some[$value]"
    }

    private object None : MayBe<Any>() {

        override fun isPresent(): Boolean = false

        override fun <U> withValue(operation: Any.() -> MayBe<U>): MayBe<U> = absent()
        override fun withoutValue(operation: () -> MayBe<Any>): MayBe<Any> = operation()

        override fun toString(): String = "None"
    }
}

// fun fromBDD(): MayBe<String> = MayBe.some("BDD")
fun fromBDD(): MayBe<String> = MayBe.absent()

fun fromServer1(): MayBe<String> = MayBe.some("Server1")
// fun fromServer1(): MayBe<String> = MayBe.absent()

fun fromServer2(): MayBe<String> = MayBe.some("Server2")
// fun fromServer2(): MayBe<String> = MayBe.absent()

fun waitFor(duration: Long): MayBe<String> = MayBe.absent<String>().apply { println("Fake datasource which wait for $duration") }

fun main(args: Array<String>) {
    println(MayBe.absent<Any>().withValue { MayBe.some("ok!!") })
    println(MayBe.absent<Any>().withoutValue { MayBe.some("ok!!") })

    val mayBeString: MayBe<String> = MayBe.absent()
    val failedRecover = mayBeString.withoutValue { MayBe.absent() }
    val recover = mayBeString.withoutValue { MayBe.some("recovered!") }

    val dataSources = listOf(::fromBDD, ::fromServer1, { waitFor(1500) }, ::fromBDD, ::fromServer2)
    val mayBe: MayBe<String> = dataSources.fold(MayBe.absent<String>(), { prev, next -> prev.withoutValue(next) })
    println(mayBe)
}