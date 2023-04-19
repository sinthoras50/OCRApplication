package com.example.templateeditorapp.ui.qrgen

import com.example.templateeditorapp.R
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar

enum class Currency(val value: String, val image: Int) {
    EUR("EUR", R.drawable.currency_flag_eu),
    CZK("CZK", R.drawable.currency_flag_cz),
    USD("USD", R.drawable.currency_flag_us);

    companion object {
        val valuesList = values().map { it.value }
    }
}

data class Transaction(
    val amount: BigDecimal,
    val currency: String,
    val iban: String,
    val beneficiaryName: String,
    val paymentId: String = "",
    val variableSymbol: String = "",
    val constantSymbol: String = "",
    val specificSymbol: String = "",
    val note: String = "",
    val swift: String = "",
    val isRecurring: Boolean = false,
    val isIncasso: Boolean = false,
    val beneficiaryAddress1: String = "",
    val beneficiaryAddress2: String = ""
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val currentDate = LocalDate.now()
    private val paymentType = "1"
    val date = currentDate.format(formatter)
    private val sepaFormat = ""
    private val paymentTargetAccountType = "1"
    private val recurring = if (isRecurring) "1" else "0"
    private val incasso = if (isIncasso) "1" else "0"

    fun getFormattedData(): String {
        return "$paymentId\t$paymentType\t$paymentType\t${"%.2f".format(amount)}\t${currency}\t$date\t$variableSymbol\t$constantSymbol\t$specificSymbol\t$sepaFormat\t$note\t$paymentTargetAccountType\t$iban\t$swift\t$recurring\t$incasso\t$beneficiaryName\t$beneficiaryAddress1\t$beneficiaryAddress2\t"
    }
}
