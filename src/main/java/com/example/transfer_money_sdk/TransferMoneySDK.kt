package com.example.transfer_money_sdk

import com.example.transfer_money_sdk.api.ExchangeRateApi
import com.example.transfer_money_sdk.api.ExchangeRateApiImpl
import com.example.transfer_money_sdk.api.createHttpClient
import com.example.transfer_money_sdk.model.ConversionResult
import com.example.transfer_money_sdk.model.Currency
import com.example.transfer_money_sdk.model.CurrencyResult
import com.example.transfer_money_sdk.model.ExchangeRate

class TransferMoneySDK internal constructor(
    private val api: ExchangeRateApi,
    private val cacheTtlMs: Long = 5 * 60 * 1000L
) {
    constructor(
        baseUrl: String,
        cacheTtlMs: Long = 5 * 60 * 1000L
    ) : this(
        api = ExchangeRateApiImpl(createHttpClient(), baseUrl),
        cacheTtlMs = cacheTtlMs
    )

    constructor(cacheTtlMs: Long = 5 * 60 * 1000L) : this(
        api = ExchangeRateApiImpl(createHttpClient()),
        cacheTtlMs = cacheTtlMs
    )
    private val cache = mutableMapOf<String, Pair<ExchangeRate,Long>>()
    // lấy tỉ giá
    suspend fun getRates(base: Currency = Currency.USD): CurrencyResult<ExchangeRate> {
        val cached = cache[base.code]
        if (cached != null && !isCacheExpired(cached.second)) {
            return CurrencyResult.Success(cached.first)
        }
        return try {
            val response = api.getRate(base.code)

            val ratesMap = response.rates
                .mapNotNull { (code, rate) ->
                    Currency.fromCode(code)?.let{it to rate}
                }
                .toMap()
            val exchangeRate = ExchangeRate(
                base = base,
                date = response.date,
                rates = ratesMap
            )
            cache[base.code] = exchangeRate to System.currentTimeMillis()
            CurrencyResult.Success(exchangeRate)
        } catch (e: Exception){
            CurrencyResult.Error(
                message = "haven't ti gia: ${e.message}",
                cause = e
            )
        }
    }

    //doi tien
    suspend fun convert(
        amount: Double,
        from: Currency,
        to: Currency
    ): CurrencyResult<ConversionResult>{
        if(amount <= 0){
            return CurrencyResult.Error("So tien phai lon hon 0")
        }
        if(from == to){
            return CurrencyResult.Success(
                ConversionResult(from,to,amount,amount,1.0,"N/A")
            )
        }

        return when (val result = getRates(from)) {
            is CurrencyResult.Success -> {
                val rate = result.data.getRate(to)
                    ?: return CurrencyResult.Error("Không có tỷ giá cho ${to.code}")

                CurrencyResult.Success(
                    ConversionResult(
                        from            = from,
                        to              = to,
                        originalAmount  = amount,
                        convertedAmount = amount * rate,
                        rate            = rate,
                        date            = result.data.date
                    )
                )
            }
            is CurrencyResult.Error -> result
        }
    }
    //doi sang nhieu dong tien cung luc

    suspend fun convertToMultiple(
        amount: Double,
        from: Currency,
        target: List<Currency>
    ): Map<Currency, CurrencyResult<ConversionResult>> {
        return target.associateWith { to -> convert(amount,from,to) }
    }
    fun clearCache()= cache.clear()

    private fun isCacheExpired(timestamp: Long): Boolean =
        System.currentTimeMillis() - timestamp > cacheTtlMs

}
