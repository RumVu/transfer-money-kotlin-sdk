package com.example.transfer_money_sdk.api

import com.example.transfer_money_sdk.model.ExchangeRateResponse

internal interface ExchangeRateApi{
    suspend fun getRate(baseCurrency: String): ExchangeRateResponse
}