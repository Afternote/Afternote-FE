package com.afternote.feature.timeletter.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.HTTP

class TimeLetterApiServiceContractTest {
    @Test
    fun `deleteTimeLetters uses DELETE time-letters with a request body`() {
        val method =
            TimeLetterApiService::class.java.declaredMethods.single {
                it.name == "deleteTimeLetters"
            }
        val http = requireNotNull(method.getAnnotation(HTTP::class.java))

        assertEquals("DELETE", http.method)
        assertEquals("time-letters", http.path)
        assertTrue(http.hasBody)
        assertTrue(method.parameterAnnotations.flatten().any { it is Body })
    }
}
