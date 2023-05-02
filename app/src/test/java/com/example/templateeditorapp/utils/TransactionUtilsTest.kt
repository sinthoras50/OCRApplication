package com.example.templateeditorapp.utils


import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransactionUtilsTest {

    /**
     * TransactionUtils.isNumeric() tests
     * Should check whether a string is a non negative Integer number
     */
    @Test
    fun `empty number returns false`() {
        val result = TransactionUtils.isNumeric("")
        assertThat(result).isFalse()
    }

    @Test
    fun `negative number returns false`() {
        val result = TransactionUtils.isNumeric("-1")
        assertThat(result).isFalse()
    }

    @Test
    fun `number containing non numeric symbols returns false`() {
        val result = TransactionUtils.isNumeric("123d")
        assertThat(result).isFalse()
    }

    @Test
    fun `correct number returns true`() {
        val result = TransactionUtils.isNumeric("123")
        assertThat(result).isTrue()
    }

    /**
     * TransactionUtils.isValidAmount() tests
     * Should return `true` if the amount string is a positive number that represents an amount of money, `false` otherwise.
     * Can also be blank
     */
    @Test
    fun `blank amount returns true`() {
        val result = TransactionUtils.isValidAmount("")
        assertThat(result).isTrue()
    }

    @Test
    fun `negative amount returns false`() {
        val result = TransactionUtils.isValidAmount("-1")
        assertThat(result).isFalse()
    }

    @Test
    fun `non numeric amount returns false`() {
        val result = TransactionUtils.isValidAmount("abcd")
        assertThat(result).isFalse()
    }

    @Test
    fun `too many decimal spaces returns false`() {
        val result = TransactionUtils.isValidAmount("1.0000")
        assertThat(result).isFalse()
    }

    @Test
    fun `zero returns false`() {
        val result = TransactionUtils.isValidAmount("0")
        assertThat(result).isFalse()
    }

    @Test
    fun `correct amount returns false`() {
        val result = TransactionUtils.isValidAmount("15.50")
        assertThat(result).isTrue()
    }
}