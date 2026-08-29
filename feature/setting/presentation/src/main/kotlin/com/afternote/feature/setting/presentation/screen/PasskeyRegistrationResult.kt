package com.afternote.feature.setting.presentation.screen

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import com.afternote.feature.setting.presentation.viewmodel.PassKeyViewModel

internal sealed interface PasskeyRegistrationResult {
    data object Success : PasskeyRegistrationResult

    data object Canceled : PasskeyRegistrationResult

    data class Error(
        val message: String,
    ) : PasskeyRegistrationResult
}

/**
 * 실제 서버 패스키 등록 왕복 — registerOptions 조회 → Android Credential Manager로 플랫폼 자격 증명 생성
 * (시스템 지문/얼굴/화면 잠금 확인 UI가 여기서 뜬다) → register로 등록 응답 전달.
 *
 * [PassKeyMakingScreen]·[PassKeyPasswordScreen] 양쪽 다 자체적인 로컬 확인(지문/PIN) 이후 이 함수를
 * 호출한다. 로컬 확인은 Credential Manager 호출 전 화면 자체의 안내 단계일 뿐, 실제 자격 증명 생성 시
 * 시스템이 별도로 한 번 더 본인 확인을 요구할 수 있다.
 */
internal suspend fun registerPasskeyWithCredentialManager(
    context: Context,
    credentialManager: CredentialManager,
    viewModel: PassKeyViewModel,
): PasskeyRegistrationResult {
    val optionsJson =
        viewModel.getPasskeyRegisterOptions().getOrElse {
            return PasskeyRegistrationResult.Error("패스키 등록 정보를 불러오지 못했습니다.")
        }

    return try {
        val request = CreatePublicKeyCredentialRequest(requestJson = optionsJson)
        val response = credentialManager.createCredential(context, request) as CreatePublicKeyCredentialResponse
        viewModel
            .completeRegistration(response.registrationResponseJson)
            .fold(
                onSuccess = { PasskeyRegistrationResult.Success },
                onFailure = { PasskeyRegistrationResult.Error("패스키 등록에 실패했습니다.") },
            )
    } catch (e: CreateCredentialCancellationException) {
        PasskeyRegistrationResult.Canceled
    } catch (e: CreateCredentialException) {
        PasskeyRegistrationResult.Error("패스키 등록에 실패했습니다.")
    }
}
