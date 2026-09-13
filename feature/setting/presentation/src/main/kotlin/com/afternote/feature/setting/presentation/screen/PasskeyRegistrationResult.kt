package com.afternote.feature.setting.presentation.screen

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager

/** 모든 실패와 취소는 ViewModel의 등록 경계에서 처리한다. */
internal suspend fun createPasskeyCredential(
    context: Context,
    credentialManager: CredentialManager,
    optionsJson: String,
): String {
    val request = CreatePublicKeyCredentialRequest(requestJson = optionsJson)
    val response = credentialManager.createCredential(context, request)
    val publicKeyResponse =
        response as? CreatePublicKeyCredentialResponse
            ?: error("Credential Manager returned an unexpected credential response type")
    return publicKeyResponse.registrationResponseJson
}
