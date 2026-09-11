package com.afternote.feature.setting.data

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.setting.domain.Passkey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasskeyRepositoryTest {
    @Test
    fun registration_roundTripsCompleteServerOptionsAndPlatformCredential() =
        runBlocking {
            val options = Json.parseToJsonElement("""{"challenge":"challenge","rp":{"id":"example.test"},"extension":{"future":true}}""")
            val credential = Json.parseToJsonElement("""{"id":"credential-id","response":{"attestationObject":"attestation"}}""")
            var submitted: JsonElement? = null
            val service =
                object : PasskeyApiService {
                    override suspend fun getPasskeys(): BaseResponse<List<PasskeyDto>> = error("Not used")

                    override suspend fun getRegistrationOptions(): BaseResponse<JsonElement> =
                        BaseResponse(status = 200, code = 200, data = options)

                    override suspend fun registerPasskey(credential: JsonElement): BaseResponse<PasskeyDto> {
                        submitted = credential
                        return BaseResponse(status = 200, code = 200, data = DTO)
                    }
                }
            val repository = PasskeyRepositoryImpl(service)
            assertEquals(options, Json.parseToJsonElement(repository.getRegistrationOptions()))
            assertEquals(Passkey(DTO.id, DTO.displayName, DTO.createdAt), repository.registerPasskey(credential.toString()))
            assertEquals(credential, submitted)
        }

    @Test
    fun list_preservesEveryRegisteredPasskeyAndAnActualEmptyList() =
        runBlocking {
            val service = ListPasskeyService()
            val repository = PasskeyRepositoryImpl(service)
            assertTrue(repository.getPasskeys().isEmpty())
            service.response = BaseResponse(status = 200, code = 200, data = listOf(DTO, DTO.copy(id = 8L)))
            assertEquals(listOf(7L, 8L), repository.getPasskeys().map { it.id })
        }

    @Test
    fun list_failedOrMissingResponseDoesNotBecomeAnEmptyList() =
        runBlocking {
            val service = ListPasskeyService()
            val repository = PasskeyRepositoryImpl(service)
            service.response = BaseResponse(status = 500, code = 500, data = emptyList())
            assertTrue(runCatching { repository.getPasskeys() }.isFailure)
            service.response = BaseResponse(status = 200, code = 200, data = null)
            assertTrue(runCatching { repository.getPasskeys() }.isFailure)
        }

    @Test(expected = SerializationException::class)
    fun list_missingRequiredIdentityIsNotDefaulted() {
        Json.decodeFromString<PasskeyDto>("""{"displayName":"Passkey","createdAt":"2026-09-06T10:00:00"}""")
    }
}

private class ListPasskeyService : PasskeyApiService {
    var response = BaseResponse(status = 200, code = 200, data = emptyList<PasskeyDto>())

    override suspend fun getPasskeys(): BaseResponse<List<PasskeyDto>> = response

    override suspend fun getRegistrationOptions(): BaseResponse<JsonElement> = error("Not used")

    override suspend fun registerPasskey(credential: JsonElement): BaseResponse<PasskeyDto> = error("Not used")
}

private val DTO = PasskeyDto(id = 7L, displayName = "Passkey", createdAt = "2026-09-06T10:00:00")
