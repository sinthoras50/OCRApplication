package com.example.templateeditorapp.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.math.roundToInt

data class Country(
    @SerializedName("country") val country: String,
    @SerializedName("code") val countryCode: String,
    @SerializedName("length") val length: Int,
    @SerializedName("bank_codes") val bankCodes: List<String>
)

/**
 * Transaction utils for validation and correction of transaction fields
 */
object TransactionUtils {

    private lateinit var countries: Map<String, Country>

    /**
     * Initializes the <String, Country> [Map] of countries. Needs to be run after countries.json has been populated from [Assets]
     * @param context Application context
     */
    fun initCountries(context: Context) {
        val file = File("${Assets.getDataPath(context)}/countriesdata/countries.json")
        val jsonString = file.readText()
        val countryListType = object : TypeToken<List<Country>>() {}.type
        val countryList: List<Country> = Gson().fromJson(jsonString, countryListType)

        val map = HashMap<String, Country>()
        for (country in countryList) {
            val code = country.countryCode
            map[code] = country
        }

        countries = map.toMap()
    }

    /**
     * Checks the validity of the IBAN based on checksum calculation, length and country. Only SEPA countries are currently supported.
     * @iban The iban string to be validated
     * @return Returns `true` if the IBAN is valid, `false` otherwise
     */
    fun isValidIban(iban: String): Boolean {
        if (iban == "") return true
        if (iban.length < 5 || iban.length > 34) return false

        val regex = Regex("[a-zA-Z0-9]+")
        if (!regex.matches(iban)) return false

        val countryCode = iban.take(2).uppercase() // take first two chars
        val length = countries[countryCode]?.length ?: return false // check if the country is valid
        if (iban.length != length) return false // check if the length is correct
        if (iban.substring(2, 4).toIntOrNull() == null) return false // check if checksum is an integer

        countries[countryCode]!!.bankCodes.firstOrNull()?.length?.let { // check if bank code is valid against the list of bank codes associated with the country
            val bankCode = iban.substring(4, 4+it)

            if (bankCode !in countries[countryCode]!!.bankCodes) return@isValidIban false
        }

        val rearranged = iban.substring(4).uppercase() + countryCode + iban.substring(2, 4)

        val shift = 55
        val converted = rearranged // replace letters with digits, starting with A = 10
            .map {
                if (it.isLetter()) (it.code - shift).toString()
                else it.toString()
            }
            .joinToString(separator="")

        val remainder = converted.toBigInteger() % "97".toBigInteger() // calculate remainder after division by 97
        return remainder == "1".toBigInteger() // remainder of 1 indicates that checksum is correct
    }

    /**
     * Returns the length of a country's IBAN
     * @param country The country code for which to get the IBAN length
     * @return The length of the IBAN associated with the specified country code, or `-1` if the country code is not found
     */
    fun getIbanLength(country: String): Int {
        return countries[country.uppercase()]?.length ?: -1
    }

    /**
     * Returns the country code associated with the IBAN
     * @param iban The iban for which to get the country
     * @return Returns country code or "DEFAULT" if IBAN is too short
     */
    fun getIbanCountry(iban: String): String {
        val country = iban.take(2)
        if (country.trim().length != 2) return "DEFAULT"

        return country
    }

    /**
     * Checks the validity of the amount
     * @param amount The amount to be validated
     * @return Returns `true` if the amount is a positive number that represents an amount of money, `false` otherwise
     */
    fun isValidAmount(amount: String): Boolean {
        if (amount == "") return true
        val containsNoChars = amount.all { char -> char.isDigit() || char == '.' || char == ',' } // number can only contain digits or , or .

        if (!containsNoChars) return false

        val num = amount.replace(',', '.').toDoubleOrNull() ?: return false // number has to be able to be converted into a double
        if (num <= 0) return false // number has to be more than 0

        val numStr = num.toString()
        val idx = numStr.indexOf('.')
        if (idx == -1) return true // number is an integer

        val decimalPlaces = numStr.length - idx - 1 // count number of decimal places
        return decimalPlaces <= 2
    }

    /**
     * Checks if the string is a valid number consisting of digits only
     * @param num The number to be validated
     * @return Returns `true` if the number is a valid number, `false` otherwise
     */
    fun isNumeric(num: String): Boolean {
        return num.all { char -> char.isDigit() }
    }

    /**
     * Attempts to fix the iban, fixing the most common OCR errors and also using levenshtein distance to find the closest match
     * of the country code and bank code - if bank codes have been specified for the given country
     *
     * @param iban Iban to be fixed
     * @return Returns fixed iban.
     */
    fun fixIban(iban: String): String {
        val replaceOs = iban.replace("[oO]".toRegex(), "0")
        val removeDisallowed = replaceOs
            .filter { it.isLetterOrDigit() }
            .uppercase()

        if (iban.length < 2) return removeDisallowed

        val country = removeDisallowed.take(2)

        val closestCountry = findClosest(country, countries.keys.toSet())
        val bankCodeLength = countries[closestCountry]?.bankCodes?.firstOrNull()?.length ?: 99

        return when(removeDisallowed.length) {
            2 -> closestCountry
            in 4+bankCodeLength..Int.MAX_VALUE -> {
                val checksum = removeDisallowed.substring(2, 4)
                val bankCode = removeDisallowed.substring(4, 4+bankCodeLength)
                val bankCodes = countries[closestCountry]!!.bankCodes.toSet()
                val closestBankCode = findClosest(bankCode, bankCodes)

                closestCountry + checksum + closestBankCode + removeDisallowed.substring(4+bankCodeLength)
            } else -> closestCountry + removeDisallowed.substring(2)
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        var str1 = s1.lowercase().replace(" ", "")
        var str2 = s2.lowercase().replace(" ", "")

        if (str1.length > str2.length) {
            str1 = str2.also { str2 = str1 }
        }

        var distances = (0..str1.length).toList()

        for (i2 in str2.indices) {
            val distances_ = mutableListOf(i2+1)
            for (i1 in str1.indices) {
                if (str1[i1] == str2[i2]) {
                    distances_.add(distances[i1])
                } else {
                    distances_.add(1 + minOf(distances[i1], distances[i1+1], distances_.last()))
                }
            }
            distances = distances_.toList()
        }

        return distances[str1.length]
    }

    private fun findClosest(word: String, dataset: Set<String>): String {
        val ft = dataset.associateWith { levenshteinDistance(word, it) }
        return ft.minByOrNull { it.value }?.key ?: ""
    }
}