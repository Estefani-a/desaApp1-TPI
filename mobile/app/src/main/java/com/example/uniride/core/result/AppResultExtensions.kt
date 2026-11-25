package com.example.uniride.core.result

/** Transforms the data of a [AppResult.Success]; a [AppResult.Failure] is returned untouched. */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/** Runs [action] with the data if this is a [AppResult.Success] and returns the same result. */
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

/** Runs [action] with the error if this is a [AppResult.Failure] and returns the same result. */
inline fun <T> AppResult<T>.onFailure(action: (DataError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

/** Returns the data of a [AppResult.Success], or `null` if this is a [AppResult.Failure]. */
fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> null
}

/** Collapses both cases into a single value. */
inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DataError) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}
