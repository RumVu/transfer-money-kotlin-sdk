package com.example.transfer_money_sdk

import com.example.transfer_money_sdk.api.ExchangeRateApi
import com.example.transfer_money_sdk.model.Currency
import com.example.transfer_money_sdk.model.CurrencyResult
import com.example.transfer_money_sdk.model.ExchangeRateResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TransferMoneySDKTest {

    private lateinit var mockApi: ExchangeRateApi
    private lateinit var sdk: TransferMoneySDK

    private val fakeRates = mapOf(
        "VND" to 25000.0,
        "EUR" to 0.92,
        "JPY" to 153.0,
        "GBP" to 0.79,
        "USD" to 1.0
    )

    private val fakeResponse = ExchangeRateResponse(
        success = true,
        base = "USD",
        date = "2024-01-15",
        rates = fakeRates
    )

    @Before
    fun setUp() {
        mockApi = mockk()
        sdk = TransferMoneySDK(api = mockApi, cacheTtlMs = 5 * 60 * 1000L)
    }

    // ─── getRates ────────────────────────────────────────────────────────────

    @Test
    fun `getRates returns Success khi api tra ve du lieu hop le`() = runTest {
        coEvery { mockApi.getRate("USD") } returns fakeResponse

        val result = sdk.getRates(Currency.USD)

        assertTrue(result.isSuccess)
        val data = (result as CurrencyResult.Success).data
        assertEquals(Currency.USD, data.base)
        assertEquals("2024-01-15", data.date)
        assertEquals(25000.0, data.rates[Currency.VND])
    }

    @Test
    fun `getRates returns Error khi api nem exception`() = runTest {
        coEvery { mockApi.getRate(any()) } throws RuntimeException("Network error")

        val result = sdk.getRates(Currency.USD)

        assertTrue(result.isError)
        val error = result as CurrencyResult.Error
        assertTrue(error.message.contains("Network error"))
    }

    @Test
    fun `getRates su dung cache khi goi lan 2`() = runTest {
        coEvery { mockApi.getRate("USD") } returns fakeResponse

        sdk.getRates(Currency.USD)
        sdk.getRates(Currency.USD)

        // Api chi duoc goi 1 lan, lan 2 lay tu cache
        coVerify(exactly = 1) { mockApi.getRate("USD") }
    }

    @Test
    fun `getRates goi api lai sau khi clearCache`() = runTest {
        coEvery { mockApi.getRate("USD") } returns fakeResponse

        sdk.getRates(Currency.USD)
        sdk.clearCache()
        sdk.getRates(Currency.USD)

        coVerify(exactly = 2) { mockApi.getRate("USD") }
    }

    // ─── convert ─────────────────────────────────────────────────────────────

    @Test
    fun `convert tra ve Success voi so tien chinh xac`() = runTest {
        coEvery { mockApi.getRate("USD") } returns fakeResponse

        val result = sdk.convert(100.0, Currency.USD, Currency.VND)

        assertTrue(result.isSuccess)
        val data = (result as CurrencyResult.Success).data
        assertEquals(100.0, data.originalAmount, 0.001)
        assertEquals(2_500_000.0, data.convertedAmount, 0.001)
        assertEquals(25000.0, data.rate, 0.001)
        assertEquals(Currency.USD, data.from)
        assertEquals(Currency.VND, data.to)
    }

    @Test
    fun `convert tra ve Error khi amount bang 0 hoac am`() = runTest {
        val resultZero = sdk.convert(0.0, Currency.USD, Currency.VND)
        assertTrue(resultZero.isError)

        val resultNeg = sdk.convert(-50.0, Currency.USD, Currency.VND)
        assertTrue(resultNeg.isError)
    }

    @Test
    fun `convert tra ve cung so tien khi from == to`() = runTest {
        val result = sdk.convert(500.0, Currency.USD, Currency.USD)

        assertTrue(result.isSuccess)
        val data = (result as CurrencyResult.Success).data
        assertEquals(500.0, data.convertedAmount, 0.001)
        assertEquals(1.0, data.rate, 0.001)
        assertEquals("N/A", data.date)
    }

    @Test
    fun `convert tra ve Error khi api loi`() = runTest {
        coEvery { mockApi.getRate(any()) } throws RuntimeException("Timeout")

        val result = sdk.convert(100.0, Currency.USD, Currency.VND)

        assertTrue(result.isError)
    }

    // ─── convertToMultiple ───────────────────────────────────────────────────

    @Test
    fun `convertToMultiple tra ve ket qua cho tat ca cac dong tien`() = runTest {
        coEvery { mockApi.getRate("USD") } returns fakeResponse

        val targets = listOf(Currency.VND, Currency.EUR, Currency.JPY)
        val results = sdk.convertToMultiple(100.0, Currency.USD, targets)

        assertEquals(3, results.size)
        assertTrue(results[Currency.VND]!!.isSuccess)
        assertTrue(results[Currency.EUR]!!.isSuccess)
        assertTrue(results[Currency.JPY]!!.isSuccess)

        val vnd = (results[Currency.VND] as CurrencyResult.Success).data
        assertEquals(2_500_000.0, vnd.convertedAmount, 0.001)
    }

    @Test
    fun `convertToMultiple chi goi api 1 lan nho cache`() = runTest {
        coEvery { mockApi.getRate("USD") } returns fakeResponse

        val targets = listOf(Currency.VND, Currency.EUR, Currency.JPY, Currency.GBP)
        sdk.convertToMultiple(100.0, Currency.USD, targets)

        coVerify(exactly = 1) { mockApi.getRate("USD") }
    }

    // ─── CurrencyResult helpers ───────────────────────────────────────────────

    @Test
    fun `CurrencyResult map bien doi du lieu khi Success`() {
        val result: CurrencyResult<Double> = CurrencyResult.Success(42.0)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(84.0, (mapped as CurrencyResult.Success).data, 0.001)
    }

    @Test
    fun `CurrencyResult map giu nguyen Error`() {
        val result: CurrencyResult<Double> = CurrencyResult.Error("loi roi")
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isError)
        assertEquals("loi roi", (mapped as CurrencyResult.Error).message)
    }

    @Test
    fun `CurrencyResult getOrNull tra ve null khi Error`() {
        val result: CurrencyResult<String> = CurrencyResult.Error("ops")
        assertNull(result.getOrNull())
    }

    @Test
    fun `CurrencyResult getOrNull tra ve data khi Success`() {
        val result: CurrencyResult<String> = CurrencyResult.Success("ok")
        assertEquals("ok", result.getOrNull())
    }
}
