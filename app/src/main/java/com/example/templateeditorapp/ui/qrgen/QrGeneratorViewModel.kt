package com.example.templateeditorapp.ui.qrgen

import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TAG_IMAGE
import net.glxn.qrgen.android.QRCode
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.LZMAOutputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * ViewModel for generating QR codes and payme.sk hyperlinks for transactions.
 *
 * @property transaction The current transaction being processed.
 * @property _bitmap A [MutableLiveData] object containing the generated QR code as a [Bitmap].
 * @property _paymentLink A [MutableLiveData] object containing the payment link associated with the current transaction.
 * @property bitmap A [LiveData] object containing the generated QR code as a [Bitmap].
 * @property paymentLink A [LiveData] object containing the payment link associated with the current transaction.
 */
class QrGeneratorViewModel : ViewModel() {

    private lateinit var transaction: Transaction

    private val _bitmap: MutableLiveData<Bitmap> = MutableLiveData()
    private val _paymentLink = MutableLiveData<String>("")

    val bitmap: LiveData<Bitmap> = _bitmap
    val paymentLink: LiveData<String> = _paymentLink


    /**
     * Generates a QR code for the provided [map] of transaction data using the given [context].
     *
     * @param map A [Map] containing the transaction data.
     * @param context The [Context] used to generate the QR code.
     */
    fun generateQrCode(map: Map<String, String>, context: Context) {
        val transaction = createTransaction(map)
        this.transaction = transaction

        val encodedString = transaction.getCompressedData()
        val width = context.resources.displayMetrics.widthPixels
        val bitmap = QRCode.from(encodedString).withSize(width, width).bitmap()

        Log.d(TAG_IMAGE, transaction.getPaymentLink())

        _paymentLink.value = transaction.getPaymentLink()
        _bitmap.value = bitmap
    }

    /**
     * Saves the generated QR code [Bitmap] to external storage using the provided [context].
     *
     * @param context The [Context] used to save the QR code [Bitmap] to external storage.
     * @return True if the QR code [Bitmap] was successfully saved, false otherwise.
     */
    fun saveBitmap(context: Context): Boolean {
        if (_bitmap.value == null) return false

        val filename = createFileName()

        return ImageUtils.savePhotoToExternalStorage(filename, _bitmap.value!!, context)
    }

    /**
     * Generates a filename for the saved QR code [Bitmap].
     *
     * @return A [String] representing the filename for the saved QR code [Bitmap].
     */
    private fun createFileName(): String {
        return "testing"
    }

    /**
     * Creates a new [Transaction] object using the provided [map] of transaction data.
     *
     * @param map A [Map] containing the transaction data.
     * @return A new [Transaction] object initialized with the provided transaction data.
     */
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
}