package com.example.templateeditorapp.ui.qrgen

import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.templateeditorapp.utils.TAG_IMAGE
import net.glxn.qrgen.android.QRCode
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.LZMAOutputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.ByteOrder
import java.util.zip.CRC32

class QrGeneratorViewModel : ViewModel() {

    private val _bitmap: MutableLiveData<Bitmap> = MutableLiveData()

    val bitmap: LiveData<Bitmap> = _bitmap

    fun generateQrCode(map: Map<String, String>, context: Context) {
        val transaction = createTransaction(map)
        val delimitedString = transaction.getFormattedData()
        Log.d(TAG_IMAGE, delimitedString)
        Log.d(TAG_IMAGE, "formattedDate = ${transaction.date} date = ${transaction.currentDate}")
        val encodedString = compress("placeholder")
        val width = context.resources.displayMetrics.widthPixels
        val bitmap = QRCode.from(encodedString).withSize(width, width).bitmap()
        _bitmap.value = bitmap
    }

    private fun createTransaction(map: Map<String, String>): Transaction {
        return Transaction(
            amount = BigDecimal(map["amount"]),
            currency = map["currency"] ?: "EUR",
            iban = map["iban"] ?: "",
            beneficiaryName = map["beneficiary name"] ?: "",
            variableSymbol = map["variable symbol"] ?: "",
            constantSymbol = map["constant symbol"] ?: "",
            specificSymbol = map["specific symbol"] ?: "",
            note = map["note"] ?: "",
            beneficiaryAddress1 = map["beneficiary address 1"] ?: "",
            beneficiaryAddress2 = map["beneficiary address 2"] ?: ""
        )
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

    private fun compress(input: String) : String {

        val crc = CRC32()
        val data = input.toByteArray()
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