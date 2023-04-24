package com.example.templateeditorapp.ui.qrgen

import android.net.Uri
import android.util.Log
import com.example.templateeditorapp.R
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.LZMAOutputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.zip.CRC32

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

    private fun getFormattedData(): String {
        return "$paymentId\t$paymentType\t$paymentType\t${"%.2f".format(amount)}\t${currency}\t$date\t$variableSymbol\t$constantSymbol\t$specificSymbol\t$sepaFormat\t$note\t$paymentTargetAccountType\t$iban\t$swift\t$recurring\t$incasso\t$beneficiaryName\t$beneficiaryAddress1\t$beneficiaryAddress2\t"
    }

    private fun Long.toByteArray(bytes: Int = 4, endianness: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
        val result = ByteArray(bytes)
        for (i in 0 until bytes) {
            result[i] = this.ushr(i * 8).toByte()
        }

        return if (endianness == ByteOrder.LITTLE_ENDIAN) result else result.reversedArray()
    }

    private fun Int.toByteArray(bytes: Int = 4, endianness: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
        val result = ByteArray(bytes)
        for (i in 0 until bytes) {
            result[i] = this.ushr(i * 8).toByte()
        }

        return if (endianness == ByteOrder.LITTLE_ENDIAN) result else result.reversedArray()
    }

    fun getPaymentLink(): String {
        val paymentReference = "/VS$variableSymbol/SS$specificSymbol/KS$constantSymbol"
        val uri = Uri.Builder()
            .scheme("https")
            .authority("payme.sk")
            .appendQueryParameter("V", "1")
            .appendQueryParameter("IBAN", iban)
            .appendQueryParameter("AM", amount.toString())
            .appendQueryParameter("CC", currency)
            .appendQueryParameter("DT", date)
            .appendQueryParameter("PI", paymentReference)
            .appendQueryParameter("CN", beneficiaryName)
            .appendQueryParameter("MSG", note)

        return uri.toString()
    }

    fun getCompressedData(): String {

        val crc = CRC32()
        val data = getFormattedData().toByteArray()
        crc.update(data)
        val checksum = crc.value.toByteArray(endianness = ByteOrder.LITTLE_ENDIAN)
        val total = checksum + data

        Log.d("STRING", data.joinToString { String.format("%02X", it) })
        Log.d("STRING", checksum.joinToString { String.format("%02X", it) })
        Log.d("STRING", total.joinToString { String.format("%02X", it) })

        val out = ByteArrayOutputStream()
        val options = LZMA2Options()
        options.lc = 3
        options.lp = 0
        options.pb = 2
        options.dictSize = 128 * 1024

        val lzmaOut = LZMAOutputStream(out, options, true)
        lzmaOut.write(total)
        lzmaOut.close()

        val compressedData = out.toByteArray()

        Log.d("STRING", compressedData.joinToString { String.format("%02X", it) })

        val length = total.size.toByteArray(2, ByteOrder.LITTLE_ENDIAN)

        Log.d("STRING", length.joinToString { String.format("%02X", it) })

        val compressedWithLength = byteArrayOf(0x00, 0x00) + length + compressedData

        Log.d("STRING", compressedWithLength.joinToString { String.format("%02X", it) })

        // padded binary string

        var binary = compressedWithLength.joinToString(separator="") {
            it.toInt().and(0xFF).toString(2).padStart(8, '0')
        }

        Log.d("STRING", binary)

        // Pad with zeros on the right up to a multiple of 5
        var binaryLength = binary.length
        val remainder = binaryLength % 5
        if (remainder != 0) {
            binary += "0".repeat(5 - remainder)
            binaryLength += 5 - remainder
        }

        // Substitute each quintet of bits with corresponding character
        val subst = "0123456789ABCDEFGHIJKLMNOPQRSTUV"
        val result = StringBuilder()
        for (i in binary.indices step 5) {
            val quintet = binary.substring(i, i + 5)
            val index = Integer.parseInt(quintet, 2)
            result.append(subst[index])
        }

        Log.d("STRING", result.toString())

        return result.toString()
    }

}
