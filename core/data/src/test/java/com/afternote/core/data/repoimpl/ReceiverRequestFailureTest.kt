package com.afternote.core.data.repoimpl

import com.afternote.core.domain.error.ReceiverRequestRejectedException
import com.afternote.core.network.model.ApiException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverRequestFailureTest {
    @Test
    fun `400 server message becomes user-facing domain error`() =
        runBlocking {
            val apiError =
                ApiException(
                    status = 400,
                    code = 400,
                    serverMessage = "수신자 이메일은 필수입니다.",
                    fallbackMessage = "수신자 이메일은 필수입니다.",
                )

            val result = runCatching { mapReceiverRequestFailure { throw apiError } }.exceptionOrNull()

            assertTrue(result is ReceiverRequestRejectedException)
            val domainError = result as ReceiverRequestRejectedException
            assertEquals("수신자 이메일은 필수입니다.", domainError.userMessage)
            assertSame(apiError, domainError.cause)
        }

    @Test
    fun `409 server message becomes user-facing domain error`() =
        runBlocking {
            val apiError =
                ApiException(
                    status = 409,
                    code = 409,
                    serverMessage = "이미 등록된 수신자입니다.",
                    fallbackMessage = "이미 등록된 수신자입니다.",
                )

            val result = runCatching { mapReceiverRequestFailure { throw apiError } }.exceptionOrNull()

            assertTrue(result is ReceiverRequestRejectedException)
        }

    @Test
    fun `other 4xx server message is not converted to input error`() =
        runBlocking {
            val apiError =
                ApiException(
                    status = 404,
                    code = 404,
                    serverMessage = "존재하지 않는 엔드포인트입니다.",
                    fallbackMessage = "존재하지 않는 엔드포인트입니다.",
                )

            val result = runCatching { mapReceiverRequestFailure { throw apiError } }.exceptionOrNull()

            assertSame(apiError, result)
        }

    @Test
    fun `5xx server message is not converted to user-facing error`() =
        runBlocking {
            val apiError =
                ApiException(
                    status = 500,
                    code = 500,
                    serverMessage = "internal SQL details",
                    fallbackMessage = "internal SQL details",
                )

            val result = runCatching { mapReceiverRequestFailure { throw apiError } }.exceptionOrNull()

            assertSame(apiError, result)
        }
}
