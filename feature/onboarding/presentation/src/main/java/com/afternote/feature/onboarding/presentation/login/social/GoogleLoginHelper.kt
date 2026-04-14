package com.afternote.feature.onboarding.presentation.login.social

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Credential Manager를 이용해 Google ID Token을 획득한다.
 *
 * 카카오 로그인과 동일하게 플랫폼 의존 로직을 UI 레이어에 두어
 * Data 레이어와 ViewModel은 순수한 토큰 문자열만 다루도록 한다.
 *
 * @param serverClientId Google Cloud Console에서 발급받은 Web Client ID.
 *                       노출 방지를 위해 로컬 설정(`local.properties`)이나 DI로 주입하는 것을 권장한다.
 * @return 성공 시 Google ID Token, 사용자가 취소한 경우 [UserCancelledAuthException] 실패 Result.
 */
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
