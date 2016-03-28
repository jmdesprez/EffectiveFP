package exceptions

fun Exception.isFatal(): Boolean =
    when(this) {
        is InterruptedException -> true
        is LinkageError -> true
        is ThreadDeath -> true
        is VirtualMachineError -> true
        else -> false
    }
