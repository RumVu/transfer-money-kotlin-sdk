package com.example.transfer_money_sdk.model

sealed class CurrencyResult<out T>{
    data class Success<T>(val data: T): CurrencyResult<T>()
    data class Error(
            val message: String,
            val code: Int? = null,
            val cause: Throwable? = null,

        ): CurrencyResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun getOrNull(): T? = if ( this is Success) data else null
    fun <R> map(transform: (T) -> R): CurrencyResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error   -> this
    }
    fun onSuccess(block: (T) -> Unit): CurrencyResult<T>{
        if (this is Success) block (data)
        return this
    }
    fun onError(block: (Error) -> Unit): CurrencyResult<T> {
        if (this is Error) block(this)
        return this
    }
}