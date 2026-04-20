package com.example.transfer_money_sdk.model

import kotlinx.serialization.Serializable


@Serializable
internal data class ExchangeRateResponse(
    val success: Boolean = false,
    val base: String = "USD",
    val date: String = "",
    val rates: Map<String, Double> = emptyMap()
)

data class ExchangeRate(
    val base: Currency,
    val date: String,
    val rates: Map<Currency, Double>
) {
    fun getRate(target: Currency): Double? = rates[target]
}

data class ConversionResult(
    val from: Currency,
    val to: Currency,
    val originalAmount: Double,
    val convertedAmount: Double,
    val rate: Double,
    val date: String
) {
    override fun toString(): String =
        "${String.format("%.2f", originalAmount)} ${from.code} " +
                "= ${String.format("%.2f", convertedAmount)} ${to.code} " +
                "(tỷ giá: ${String.format("%.4f", rate)})"
}