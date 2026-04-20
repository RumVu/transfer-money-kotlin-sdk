package com.example.transfermoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.transfer_money_sdk.TransferMoneySDK
import com.example.transfer_money_sdk.model.Currency
import com.example.transfer_money_sdk.model.CurrencyResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val sdk = TransferMoneySDK()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CurrencyScreen(sdk = sdk, onConvert = { amount, from, to, onResult ->
                    lifecycleScope.launch {
                        val result = sdk.convert(amount, from, to)
                        onResult(result)
                    }
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    sdk: TransferMoneySDK,
    onConvert: (Double, Currency, Currency, (CurrencyResult<*>) -> Unit) -> Unit
) {
    val currencies = Currency.values().toList()
    var amountText by remember { mutableStateOf("100") }
    var fromCurrency by remember { mutableStateOf(Currency.USD) }
    var toCurrency by remember { mutableStateOf(Currency.VND) }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "💱 Currency Exchange",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "SDK Demo",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Amount input
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Số tiền") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // From currency
        Text("Từ đồng tiền", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExposedDropdownMenuBox(
            expanded = fromExpanded,
            onExpandedChange = { fromExpanded = it }
        ) {
            OutlinedTextField(
                value = "${fromCurrency.symbol} ${fromCurrency.code} - ${fromCurrency.displayName}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = fromExpanded,
                onDismissRequest = { fromExpanded = false }
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text("${currency.symbol} ${currency.code} - ${currency.displayName}") },
                        onClick = {
                            fromCurrency = currency
                            fromExpanded = false
                        }
                    )
                }
            }
        }

        // To currency
        Text("Sang đồng tiền", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExposedDropdownMenuBox(
            expanded = toExpanded,
            onExpandedChange = { toExpanded = it }
        ) {
            OutlinedTextField(
                value = "${toCurrency.symbol} ${toCurrency.code} - ${toCurrency.displayName}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = toExpanded,
                onDismissRequest = { toExpanded = false }
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text("${currency.symbol} ${currency.code} - ${currency.displayName}") },
                        onClick = {
                            toCurrency = currency
                            toExpanded = false
                        }
                    )
                }
            }
        }

        // Convert button
        Button(
            onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null) {
                    resultText = " Số tiền không hợp lệ!"
                    isError = true
                    return@Button
                }
                isLoading = true
                resultText = ""
                onConvert(amount, fromCurrency, toCurrency) { result ->
                    isLoading = false
                    when (result) {
                        is CurrencyResult.Success -> {
                            val data = result.data
                            if (data is com.example.transfer_money_sdk.model.ConversionResult) {
                                resultText = " ${String.format("%,.2f", data.originalAmount)} " +
                                        "${data.from.code}\n= ${String.format("%,.2f", data.convertedAmount)} " +
                                        "${data.to.code}\n\nTỷ giá: 1 ${data.from.code} " +
                                        "= ${String.format("%,.4f", data.rate)} ${data.to.code}\nNgày: ${data.date}"
                                isError = false
                            }
                        }
                        is CurrencyResult.Error -> {
                            resultText = " ${result.message}"
                            isError = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Đổi tiền", fontSize = 16.sp)
            }
        }

        // Result
        if (resultText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = resultText,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = if (isError)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}