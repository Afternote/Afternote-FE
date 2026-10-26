package com.afternote.feature.setting.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 설정 로컬 스택([SettingNavHost]) 안의 화면 키.
 *
 * [NavKey] 는 로컬 Navigation 3 스택에 실릴 수 있다는 표식이다 — `@Serializable` 과 함께
 * 있어야 프로세스 재생성 뒤 스택이 복원된다 (#1695).
 */
internal sealed interface SettingRoute : NavKey {
    @Serializable
    data object SettingHomeRoute : SettingRoute

    @Serializable
    data object WithdrawGuideRoute : SettingRoute

    @Serializable
    data object WithdrawConfirmRoute : SettingRoute

    @Serializable
    data object ProfileEditRoute : SettingRoute

    @Serializable
    data object PasswordChangeRoute : SettingRoute

    @Serializable
    data object LinkedAccountRoute : SettingRoute

    @Serializable
    data object NotificationRoute : SettingRoute

    /**
     * 수신자 목록. [selectForDeliveryConditions] 가 참이면 관리 목록이 아니라 사후 전달 조건을
     * 정할 수신자를 고르는 선택 목록이다 — 진입 경로 결정(#1998)과 무관하게 현행 분기를 그대로 나른다.
     */
    @Serializable
    data class RecipientListRoute(
        val selectForDeliveryConditions: Boolean = false,
    ) : SettingRoute

    @Serializable
    data object PushNotificationRoute : SettingRoute

    @Serializable
    data object RecipientRegisterRoute : SettingRoute

    @Serializable
    data class RecipientEditRoute(
        val receiverId: Long,
    ) : SettingRoute

    @Serializable
    data class AfterDeliveryRoute(
        val receiverId: Long,
    ) : SettingRoute

    @Serializable
    data object PasskeyRoute : SettingRoute

    @Serializable
    data object PasskeyMakingRoute : SettingRoute

    @Serializable
    data object AppLockSetupRoute : SettingRoute

    @Serializable
    data object PasskeyPasswordRoute : SettingRoute

    @Serializable
    data object NoticeRoute : SettingRoute
}
