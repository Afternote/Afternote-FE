package com.afternote.feature.setting.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverPhoneValidatorTest {
    @Test
    fun `empty phone is valid when not required`() {
        assertEquals(ReceiverPhoneValidation.VALID, "".validateReceiverPhone(isRequired = false))
    }

    @Test
    fun `empty phone is rejected as required when required`() {
        assertEquals(ReceiverPhoneValidation.REQUIRED, "".validateReceiverPhone(isRequired = true))
    }

    @Test
    fun `valid mobile phone with or without hyphens is accepted`() {
        assertEquals(ReceiverPhoneValidation.VALID, "01012345678".validateReceiverPhone(isRequired = true))
        assertEquals(ReceiverPhoneValidation.VALID, "010-1234-5678".validateReceiverPhone(isRequired = true))
        assertEquals(ReceiverPhoneValidation.VALID, "011-123-4567".validateReceiverPhone(isRequired = true))
    }

    @Test
    fun `invalid prefix or length is rejected`() {
        assertEquals(ReceiverPhoneValidation.INVALID, "02012345678".validateReceiverPhone(isRequired = true))
        assertEquals(ReceiverPhoneValidation.INVALID, "0101234567".validateReceiverPhone(isRequired = true))
        assertEquals(ReceiverPhoneValidation.INVALID, "010123456789".validateReceiverPhone(isRequired = true))
        assertEquals(ReceiverPhoneValidation.INVALID, "abc01012345678".validateReceiverPhone(isRequired = true))
        assertEquals(ReceiverPhoneValidation.INVALID, "010--1234-5678".validateReceiverPhone(isRequired = true))
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
