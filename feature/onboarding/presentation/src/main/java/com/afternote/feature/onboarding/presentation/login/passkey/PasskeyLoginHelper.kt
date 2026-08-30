package com.afternote.feature.onboarding.presentation.login.passkey

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.afternote.core.domain.error.CoreAuthFailure

/**
 * Credential Manager 로 이 앱(RP)에 등록된 패스키의 assertion 을 받아온다.
 *
 * 구글 로그인([com.afternote.feature.onboarding.presentation.login.social.requestGoogleIdToken])과
 * 같은 자리·같은 규약이다 — 플랫폼 의존 호출을 UI 계층에 두고, ViewModel 아래로는 순수 문자열만
 * 내려보낸다.
 *
 * @return 성공 시 `PublicKeyCredential` assertion JSON 원문. 사용자가 시트를 닫으면
 *   [CoreAuthFailure.UserCancelledAuth], 이 기기에 쓸 패스키가 없으면
 *   `NoCredentialException` 이 그대로 담긴 실패 [Result].
 */
suspend fun requestPasskeyAssertion(
    context: Context,
    credentialManager: CredentialManager,
    requestJson: String,
): Result<String> =
    requestPasskeyAssertion(requestJson) { request ->
        credentialManager.getCredential(context, request)
    }

/**
 * 위 함수에서 플랫폼 호출만 떼어낸 본체.
 *
 * `CredentialManager` 는 구현체를 만들려면 프레임워크 콜백 API 를 통째로 채워야 해서, 경계를
 * 이 한 줄짜리 함수형 파라미터로 끊는다. 그래야 옵션 조립·응답 파싱·실패 갈래를 단위 테스트가
 * 실제로 지나갈 수 있다.
 */
internal suspend fun requestPasskeyAssertion(
    requestJson: String,
    getCredential: suspend (GetCredentialRequest) -> GetCredentialResponse,
): Result<String> =
    try {
        val response = getCredential(buildPasskeyRequest(requestJson))
        when (val credential = response.credential) {
            is PublicKeyCredential -> Result.success(credential.authenticationResponseJson)

            // 요청에 패스키 옵션만 실었으므로 다른 형식이 오면 계약 위반이다. 조용히 흘리면
            // "패스키가 없는 기기" 와 구분되지 않으니 실패로 드러낸다.
            else -> Result.failure(IllegalStateException("패스키가 아닌 자격 형식입니다: ${credential.type}"))
        }
    } catch (e: GetCredentialCancellationException) {
        // 시스템 선택기를 사용자가 닫은 경우. 장애가 아니라 정상적인 이탈이라 도메인 타입으로 옮긴다.
        Result.failure(CoreAuthFailure.UserCancelledAuth())
    } catch (e: GetCredentialException) {
        // NoCredentialException 도 여기로 온다 — 호출부가 "이 기기에 패스키 없음" 으로 소비한다.
        Result.failure(e)
    }

/**
 * 통합 자격 요청을 만든다.
 *
 * `setPreferImmediatelyAvailableCredentials(true)` 인 이유 — 이 호출은 사용자가 누른 것이 아니라
 * 화면 진입 시 앱이 먼저 던지는 시도다. 쓸 자격이 없을 때 시스템이 "자격 없음" 안내를 대신
 * 띄우면, 아무것도 요청하지 않은 사용자에게 설명 없는 시트가 뜬다. 이 플래그를 켜면 그런 경우
 * UI 없이 `NoCredentialException` 으로 즉시 돌아와 기존 로그인 폼이 무간섭으로 남는다.
 */
private fun buildPasskeyRequest(requestJson: String): GetCredentialRequest =
    GetCredentialRequest
        .Builder()
        .addCredentialOption(GetPublicKeyCredentialOption(requestJson))
        .setPreferImmediatelyAvailableCredentials(true)
        .build()
