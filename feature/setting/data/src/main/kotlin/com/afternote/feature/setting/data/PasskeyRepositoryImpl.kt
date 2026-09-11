package com.afternote.feature.setting.data

import com.afternote.core.network.model.requireData
import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.domain.PasskeyRepository
import kotlinx.serialization.json.Json

internal class PasskeyRepositoryImpl(
    private val apiService: PasskeyApiService,
) : PasskeyRepository {
    override suspend fun getPasskeys(): List<Passkey> = apiService.getPasskeys().requireData().map { it.toDomain() }

    override suspend fun getRegistrationOptions(): String = apiService.getRegistrationOptions().requireData().toString()

    override suspend fun registerPasskey(credentialJson: String): Passkey =
        apiService.registerPasskey(Json.parseToJsonElement(credentialJson)).requireData().toDomain()
}

private fun PasskeyDto.toDomain(): Passkey = Passkey(id = id, displayName = displayName, createdAt = createdAt)
