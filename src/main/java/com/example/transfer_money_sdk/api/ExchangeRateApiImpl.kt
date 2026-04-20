package com.example.transfer_money_sdk.api
import com.example.transfer_money_sdk.model.ExchangeRateResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal class ExchangeRateApiImpl(
    private val client: HttpClient,
    private val baseUrl: String = "https://open.er-api.com/v6"
) : ExchangeRateApi {

    override suspend fun getRate(baseCurrency: String): ExchangeRateResponse {
        return client
            .get("$baseUrl/latest/$baseCurrency")
            .body()
    }
}

internal fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues  = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }
}