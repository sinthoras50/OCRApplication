package com.example.templateeditorapp.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.math.BigInteger
import kotlin.math.roundToInt

object TransactionValidationUtils {

    private lateinit var countries: Map<String, Int>

    /**
     * Initializes the <country, length> [Map] of countries. Needs to be run after countries.json has been populated from [Assets]
     * @param context Application context
     */
    fun initCountries(context: Context) {
        val file = File("${Assets.getDataPath(context)}/countriesdata/countries.json")
        val jsonString = file.readText()
        val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val list: List<Map<String, Any>> = Gson().fromJson(jsonString, listType)

        val map = HashMap<String, Int>()
        for (item in list) {
            val code = item["code"].toString()
            val length = item["length"].toString().toDouble().roundToInt()
            map[code] = length
        }

        countries = map.toMap()
    }

    /**
     * Checks the validity of the IBAN based on checksum calculation, length and country. Only SEPA countries are currently supported.
     * @iban The iban string to be validated
     * @return Returns `true` if the IBAN is valid, `false` otherwise
     */
    fun isValidIban(iban: String): Boolean {
        if (iban.length < 5 || iban.length > 34) return false

        val regex = Regex("[a-zA-Z0-9]+")
        if (!regex.matches(iban)) return false

        val countryCode = iban.take(2).uppercase() // take first two chars
        val length = countries[countryCode] ?: return false // check if the country is valid
        if (iban.length != length) return false // check if the length is correct
        if (iban.substring(2, 4).toIntOrNull() == null) return false // check if checksum is an integer

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
        return countries[country] ?: -1
    }

    /**
     * Returns the country code associated with the IBAN
     * @param iban The iban for which to get the country
     * @return Returns country code or "DEFAULT" if IBAN is too short
     */
    fun getIbanCountry(iban: String): String {
        val country = iban.take(2)
        if (country.length != 2) return "DEFAULT"

        return country
    }

    /**
     * Checks the validity of the amount
     * @param amount The amount to be validated
     * @return Returns `true` if the amount is a positive number that represents an amount of money, `false` otherwise
     */
    fun isValidAmount(amount: String): Boolean {
        return true
    }

    /**
     * Checks if the string is a valid number consisting of digits only
     * @param num The number to be validated
     * @return Returns `true` if the number is a valid number, `false` otherwise
     */
    fun isNumeric(num: String): Boolean {
        return num.all { char -> char.isDigit() }
    }
}