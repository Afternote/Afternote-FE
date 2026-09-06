package com.afternote.feature.setting.data

import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

internal interface PasskeyApiService {
    @GET("users/passkeys")
    suspend fun getPasskeys(): BaseResponse<List<PasskeyDto>>

    @POST("auth/passkey/register/options")
    suspend fun getRegistrationOptions(): BaseResponse<JsonElement>

    @POST("auth/passkey/register")
    suspend fun registerPasskey(
        @Body credential: JsonElement,
    ): BaseResponse<PasskeyDto>
}

@Serializable
internal data class PasskeyDto(
    @SerialName("id") val id: Long,
    @SerialName("displayName") val displayName: String,
    @SerialName("createdAt") val createdAt: String,
)
