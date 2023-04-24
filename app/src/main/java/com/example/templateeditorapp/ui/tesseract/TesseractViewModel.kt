package com.example.templateeditorapp.ui.tesseract

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.Observer
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.editor.BoundingBox
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TAG_IMAGE
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.ProgressValues
import kotlinx.coroutines.*
import java.math.BigInteger
import java.util.*

class TesseractViewModel(private val database: ImageDatabase) : ViewModel() {
    private val TAG = "MainViewModel"

    private var tessApi: TessBaseAPI
    private var tessInit = false
    private var stopped = false
    private var processing = false

    private val _progress = MutableLiveData<Int>()
    private val _partialProgress = MutableLiveData<Int>()
    private val _reqFieldsValid = MutableLiveData<BooleanArray>(
        booleanArrayOf(
            false, // recipient
            false, // iban
            false, // amount
            false, // vs
            false, // cs
            false, // ss
        )
    )

    private val colorPurple = Color.parseColor("#6200EE")
    private val colorRed = Color.RED


    // these fields can be modified by user so should be public in order to work with data binding
    val amount = MutableLiveData<String>("")
    val recipientName = MutableLiveData<String>("")
    val variableSymbol = MutableLiveData<String>("")
    val constantSymbol = MutableLiveData<String>("")
    val specificSymbol = MutableLiveData<String>("")
    val note = MutableLiveData<String>("")
    val iban = MutableLiveData<String>("")
    val paymentReference = MutableLiveData<String>("")
    val swift = MutableLiveData<String>("")
    val address1 = MutableLiveData<String>("")
    val address2 = MutableLiveData<String>("")


    // these fields are responsible for controlling the state of the UI and controlling the validity of the data
    private val _btnConfirmEnabled = MutableLiveData<Boolean>(false)

    private val _ibanLength = MutableLiveData<String>("")
    private val _vsLength = MutableLiveData<String>("0")
    private val _csLength = MutableLiveData<String>("0")
    private val _ssLength = MutableLiveData<String>("0")

    private val _ibanColor = MutableLiveData<Int>(colorPurple)
    private val _vsColor = MutableLiveData<Int>(colorPurple)
    private val _csColor = MutableLiveData<Int>(colorPurple)
    private val _ssColor = MutableLiveData<Int>(colorPurple)

    private val _ibanValid = MutableLiveData<Boolean>(true)
    private val _amountValid = MutableLiveData<Boolean>(true)
    private val _vsValid = MutableLiveData<Boolean>(true)
    private val _csValid = MutableLiveData<Boolean>(true)
    private val _ssValid = MutableLiveData<Boolean>(true)

    val btnConfirmEnabled: LiveData<Boolean> = _btnConfirmEnabled

    val ibanLength: LiveData<String> = _ibanLength
    val vsLength: LiveData<String> = _vsLength
    val csLength: LiveData<String> = _csLength
    val ssLength: LiveData<String> = _ssLength

    val ibanColor: LiveData<Int> = _ibanColor
    val vsColor: LiveData<Int> = _vsColor
    val csColor: LiveData<Int> = _csColor
    val ssColor: LiveData<Int> = _ssColor

    val ibanValid: LiveData<Boolean> = _ibanValid
    val amountValid: LiveData<Boolean> = _amountValid
    val vsValid: LiveData<Boolean> = _vsValid
    val csValid: LiveData<Boolean> = _csValid
    val ssValid: LiveData<Boolean> = _ssValid

    val progress: LiveData<Int> = _progress

    val resultMap = HashMap<String, String>()


    // observers need to be initialized here in order for them to be able to get destroyed in onCleared
    private val recipientObserver = Observer<String?> {
        val value = (recipientName.value?.length ?: 0) > 0
        updateReqArray(value, 0)
    }

    private val ibanObserver = Observer<String?> {
        _ibanLength.value = "${iban.value?.length ?: '0'}"
        _ibanColor.value = if (_ibanLength.value!!.toInt() > 24) colorRed else colorPurple
        val value = isIbanValid(iban.value ?: "")
        _ibanValid.value = value
        updateReqArray(value, 1)
    }

    private val amountObserver = Observer<String?> {
        val num = amount.value?.trim()?.replace(",+".toRegex(), ".") ?: ""
        val isDouble = isDouble(num)
        _amountValid.value = isDouble
        val value = (amount.value?.trim() ?: "").isNotEmpty() && isDouble
        updateReqArray(value, 2)
    }
    private val variableSymbolObserver = Observer<String?> {
        _vsLength.value = "${variableSymbol.value?.length ?: '0'}"
        _vsColor.value = if (_vsLength.value!!.toInt() > 10) colorRed else colorPurple
        val num = variableSymbol.value ?: "0"
        _vsValid.value = isLong(num)
        updateReqArray(_vsValid.value!! && num.length <= 10, 3)
    }

    private val constantSymbolObserver = Observer<String?> {
        _csLength.value = "${constantSymbol.value?.length ?: '0'}"
        _csColor.value = if (_csLength.value!!.toInt() > 4) colorRed else colorPurple
        val num = constantSymbol.value ?: "0"
        _csValid.value = isLong(num)
        updateReqArray(_csValid.value!! && num.length <= 4, 4)
    }

    private val specificSymbolObserver = Observer<String?> {
        _ssLength.value = "${specificSymbol.value?.length ?: '0'}"
        _ssColor.value = if (_ssLength.value!!.toInt() > 10) colorRed else colorPurple
        val num = specificSymbol.value ?: "0"
        _ssValid.value = isLong(num)
        updateReqArray(_ssValid.value!! && num.length <= 10, 5)
    }

    private val reqFieldsObserver = Observer<BooleanArray?> {
        _btnConfirmEnabled.value = it.all { it2 -> it2 }
    }

    init {
        tessApi =
            TessBaseAPI { progressValues: ProgressValues ->
                _partialProgress.postValue(
                    progressValues.percent
                )
            }

        recipientName.observeForever(recipientObserver)
        iban.observeForever(ibanObserver)
        amount.observeForever(amountObserver)
        variableSymbol.observeForever(variableSymbolObserver)
        constantSymbol.observeForever(constantSymbolObserver)
        specificSymbol.observeForever(specificSymbolObserver)
        _reqFieldsValid.observeForever(reqFieldsObserver)

    }

    fun prepareResultMap(currency: String): HashMap<String, String?> {
        val amount = amount.value?.replace(",", ".")
        return hashMapOf(
            "amount" to amount,
            "currency" to currency,
            "iban" to iban.value,
            "beneficiary name" to recipientName.value,
            "variable symbol" to variableSymbol.value,
            "constant symbol" to constantSymbol.value,
            "specific symbol" to specificSymbol.value,
            "note" to note.value,
            "beneficiary address 1" to address1.value,
            "beneficiary address 2" to address2.value
        )
    }

    private fun updateReqArray(value: Boolean, position: Int) {
        val oldArray = _reqFieldsValid.value?.clone() ?: booleanArrayOf(false, false, false)
        oldArray[position] = value
        _reqFieldsValid.value = oldArray
    }

    private fun isIbanValid(iban: String): Boolean {
        if (iban.length != 24) return false

        val shift = 55
        val country = iban.substring(0, 2).uppercase()

        // TODO make list of valid countries an external file
        if (!listOf("SK", "CZ").contains(country)) return false

        val checksum = iban.substring(2, 4).toIntOrNull() ?: return false

        val countryNums = country.map { (it.code - shift).toString() }
        val countryCode = countryNums.joinToString(separator="")

        val bban = iban.substring(4) + countryCode + "00"
        val num = (bban.toBigIntegerOrNull() ?: return false) % BigInteger("97")

        return checksum == 98 - num.toInt()
    }

    private fun isLong(string: String): Boolean {
        val input = if (string.trim().isBlank()) "0" else string
        return input.toLongOrNull() != null
    }

    private fun isDouble(string: String): Boolean {
        val input = if (string.trim().isBlank()) "0" else string
        return input.toDoubleOrNull() != null
    }

    override fun onCleared() {
        if (isProcessing()) {
            tessApi.stop()
        }

        tessApi.recycle()

        recipientName.removeObserver(recipientObserver)
        iban.removeObserver(ibanObserver)
        amount.removeObserver(amountObserver)
        variableSymbol.removeObserver(variableSymbolObserver)
        constantSymbol.removeObserver(constantSymbolObserver)
        specificSymbol.removeObserver(specificSymbolObserver)
        _reqFieldsValid.removeObserver(reqFieldsObserver)
    }

    fun initTesseract(dataPath: String, language: String, engineMode: Int) {
        Log.i(
            TAG, "Initializing Tesseract with: dataPath = [" + dataPath + "], " +
                    "language = [" + language + "], engineMode = [" + engineMode + "]"
        )
        try {
            tessInit = tessApi.init(dataPath, language, engineMode)
            tessApi.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
        } catch (e: IllegalArgumentException) {
            tessInit = false
            Log.e(TAG, "Cannot initialize Tesseract:", e)
        }
    }

    fun recognizeImage(
        imagePath: String,
        templatePath: String,
        scalingFactor: Double,
        context: Context,
        cropRect: Rect? = null
    ) {
        if (!tessInit) {
            Log.e(TAG, "recognizeImage: Tesseract is not initialized")
            return
        }
        if (isProcessing()) {
            Log.e(TAG, "recognizeImage: Processing is in progress")
            return
        }

        _progress.value = 0
        processing = true
        stopped = false

        // Start process in a coroutine

        viewModelScope.launch(Dispatchers.Default) {

            var boundingBoxes: List<BoundingBox>? = null

            withContext(Dispatchers.IO) {
                val annotatedImage = database.annotatedImageDao().getImage(templatePath)
                boundingBoxes = annotatedImage?.boundingBoxes
            }

            val bitmap = ImageUtils.loadPhoto(imagePath, context)

            if (boundingBoxes.isNullOrEmpty() || bitmap == null) {
                stopped = true
                processing = false
                _progress.postValue(100)
                return@launch
            }

            val progressIncrements = 100 / boundingBoxes!!.size
            resultMap.clear()

            tessApi.setImage(bitmap)

            boundingBoxes!!.forEach {

                if (stopped) {
                    return@forEach
                }

                val (x, y, w, h) = it.rect.run {
                    listOf(
                        ((left - (cropRect?.left ?: 0)) * scalingFactor).toInt(),
                        ((top - (cropRect?.top ?: 0)) * scalingFactor).toInt(),
                        (width() * scalingFactor).toInt(),
                        (height() * scalingFactor).toInt()
                    )
                }

                Log.d(TAG_IMAGE, "[$x, $y, $w, $h]")

                tessApi.setRectangle(x, y, w, h)
                tessApi.getHOCRText(0)
                val text = tessApi.utF8Text
                resultMap[it.fieldName] = text
                Log.d(TAG_IMAGE, "field = ${it.fieldName} bbox = [$x, $y, $w, $h] text = $text")
                updateField(it.fieldName, text)
                _progress.postValue(_progress.value!! + progressIncrements)
            }

            _progress.postValue(100)
            stopped = true
            processing = false
        }
    }

    private fun updateField(key: String, value: String) {
        // remove whitespace
        val res = value.replace("\\s+".toRegex(), "")

        when (key) {
            "amount" -> {
                val replaceOs = res.replace("[oO]".toRegex(), "0")
                val onlyDigits = replaceOs.filter { it.isDigit() }
                amount.postValue(onlyDigits)
            }
            "variable symbol" -> variableSymbol.postValue(res)
            "constant symbol" -> constantSymbol.postValue(res)
            "specific symbol" -> specificSymbol.postValue(res)
            "note" -> note.postValue(res)
            "iban" -> {
                val replaceOs = res.replace("[oO]".toRegex(), "0")
                val removeDisallowed = replaceOs
                    .filter { it.isLetterOrDigit() }
                    .uppercase()
                iban.postValue(removeDisallowed)
            }
            "swift" -> swift.postValue(res)
            "beneficiary name" -> recipientName.postValue(value)
            "beneficiary address 1" -> address1.postValue(value)
            "beneficiary address 2" -> address2.postValue(value)
        }
    }

    fun stop() {
        if (!isProcessing()) {
            return
        }
        tessApi.stop()
        processing = false
        stopped = true
    }

    fun isProcessing(): Boolean {
        return processing
    }

    fun isInitialized(): Boolean {
        return tessInit
    }


}