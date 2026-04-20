package com.example.transfer_money_sdk.model

enum class Currency(val code: String, val symbol: String, val displayName: String) {
    USD("USD", "$",  "US Dollar"),
    EUR("EUR", "€",  "Euro"),
    VND("VND", "₫",  "Vietnamese Dong"),
    JPY("JPY", "¥",  "Japanese Yen"),
    GBP("GBP", "£",  "British Pound"),
    SGD("SGD", "S$", "Singapore Dollar"),
    KRW("KRW", "₩",  "South Korean Won"),
    THB("THB", "฿",  "Thai Baht"),
    CNY("CNY", "¥",  "Chinese Yuan"),
    AUD("AUD", "A$", "Australian Dollar");

    companion object {
        fun fromCode(code: String): Currency? =
            values().find { it.code == code.uppercase() }
    }
}