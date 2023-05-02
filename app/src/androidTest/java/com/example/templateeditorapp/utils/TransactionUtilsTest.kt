package com.example.templateeditorapp.utils

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionUtilsTest {

    init {
        TransactionUtils.initCountries(ApplicationProvider.getApplicationContext())
    }

    /**
     * TransactionUtils.isValidIban tests
     * Blank IBAN is valid
     * Should check if length is between 5 and 34
     * Should check if country is valid
     * If country is valid should then check for correct length again
     * If country is valid and a list of banks exists, should check if bank is valid
     * If all conditions are met, should check if checksum is correct
     */

    @Test
    fun blankIBAN_returnsTrue() {
        val result = TransactionUtils.isValidIban("")
        assertThat(result).isTrue()
    }

    @Test
    fun invalidInitialLengthLt5_returnsFalse() {
        val result = TransactionUtils.isValidIban("SK")
        assertThat(result).isFalse()
    }

    @Test
    fun invalidInitialLengthMt34_returnsFalse() {
        val result = TransactionUtils.isValidIban("11111111111111111111111111111111111")
        assertThat(result).isFalse()
    }

    @Test
    fun invalidCountry_returnsFalse() {
        val result = TransactionUtils.isValidIban("AR000000000000000000")
        assertThat(result).isFalse()
    }

    @Test
    fun validCountryIncorrectLength_returnsFalse() {
        val result = TransactionUtils.isValidIban("SK641234") // should be 24
        assertThat(result).isFalse()
    }

    @Test
    fun validCountryIncorrectBank_returnsFalse() {
        val result = TransactionUtils.isValidIban("SK4699990000005030099075")
        assertThat(result).isFalse()
    }

    @Test
    fun validCountryIncorrectChecksum_returnsFalse() {
        val result = TransactionUtils.isValidIban("SK5609000000005030099075")
        assertThat(result).isFalse()
    }

    @Test
    fun validIban_returnsTrue() {
        val result = TransactionUtils.isValidIban("SK4609000000005030099075")
        assertThat(result).isTrue()
    }

    /**
     * TransactionUtils.fixIban tests
     * Attempts to fix iban by replacing letter `O` with zero `0`, and
     * replacing country and bank code with their nearest matches respectively using Levenshtein distance matcher
     */

    @Test
    fun fixIbanOs_returnsTrue() {
        val result = TransactionUtils.fixIban("SK4609000ooOO05030099075")
        assertThat(result).isEqualTo("SK4609000000005030099075")
    }

    @Test
    fun fixIbanCountry_returnsTrue() {
        val result = TransactionUtils.fixIban("KK")
        assertThat(result).isEqualTo("DK")
    }

    @Test
    fun fixIbanBankCode_returnsTrue() {
        val result = TransactionUtils.fixIban("SK001900")
        assertThat(result).isEqualTo("SK000900")
    }
}