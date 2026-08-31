package com.afternote.feature.setting.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverPhoneValidatorTest {
    @Test
    fun `empty phone is valid for registration because it is optional`() {
        assertEquals(ReceiverPhoneValidation.VALID, "".validateReceiverPhone(isRequired = false))
        assertTrue("".isValidReceiverPhone(isRequired = false))
    }

    @Test
    fun `empty phone is required for edit and blocks submission`() {
        assertEquals(ReceiverPhoneValidation.REQUIRED, "".validateReceiverPhone(isRequired = true))
        assertFalse("".isValidReceiverPhone(isRequired = true))
    }

    @Test
    fun `valid mobile phone with or without hyphens is accepted`() {
        assertTrue("01012345678".isValidReceiverPhone())
        assertTrue("010-1234-5678".isValidReceiverPhone())
        assertTrue("011-123-4567".isValidReceiverPhone())
    }

    @Test
    fun `invalid prefix or length is rejected`() {
        assertFalse("02012345678".isValidReceiverPhone())
        assertFalse("0101234567".isValidReceiverPhone())
        assertFalse("010123456789".isValidReceiverPhone())
        assertFalse("abc01012345678".isValidReceiverPhone())
        assertFalse("010--1234-5678".isValidReceiverPhone())
    }

    @Test
    fun `phone is normalized to digits`() {
        assertEquals("01012345678", "010-1234-5678".normalizeReceiverPhone())
    }

    @Test
    fun `email requires local and domain parts`() {
        assertTrue("receiver@example.com".isValidReceiverEmail())
        assertTrue(" receiver@example.com ".isValidReceiverEmail())
        assertFalse("".isValidReceiverEmail())
        assertFalse("receiver".isValidReceiverEmail())
        assertFalse("receiver@example".isValidReceiverEmail())
    }
}
