package com.afternote.feature.setting.presentation.social

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

suspend fun requestGoogleIdToken(
    context: Context,
    credentialManager: CredentialManager,
    serverClientId: String,
): Result<String> =
    try {
        val googleIdOption =
            GetGoogleIdOption
                .Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()

        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(googleIdOption)
                .build()

        val response = credentialManager.getCredential(context, request)
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Result.success(googleIdTokenCredential.idToken)
        } else {
            Result.failure(IllegalStateException("지원되지 않는 인증 형식입니다."))
        }
    } catch (e: GetCredentialCancellationException) {
        Result.failure(UserCancelledAuthException())
    } catch (e: GetCredentialException) {
        Result.failure(e)
    }
