package com.afternote.feature.setting.presentation.social

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

suspend fun requestGoogleIdToken(
    context: Context,
    credentialManager: CredentialManager,
    serverClientId: String,
): Result<String> =
    requestWithOption(
        context = context,
        credentialManager = credentialManager,
        serverClientId = serverClientId,
        useSignInPicker = false,
    ).recoverCatching { e ->
        if (e is NoCredentialException) {
            requestWithOption(
                context = context,
                credentialManager = credentialManager,
                serverClientId = serverClientId,
                useSignInPicker = true,
            ).getOrThrow()
        } else {
            throw e
        }
    }

private suspend fun requestWithOption(
    context: Context,
    credentialManager: CredentialManager,
    serverClientId: String,
    useSignInPicker: Boolean,
): Result<String> =
    try {
        val credentialOption =
            if (useSignInPicker) {
                GetSignInWithGoogleOption.Builder(serverClientId).build()
            } else {
                GetGoogleIdOption
                    .Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(true)
                    .build()
            }

        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(credentialOption)
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
