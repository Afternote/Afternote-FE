package com.afternote.feature.setting.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 설정 로컬 스택([SettingNavHost]) 안의 화면 키.
 *
 * [NavKey] 는 로컬 Navigation 3 스택에 실릴 수 있다는 표식이다 — `@Serializable` 과 함께
 * 있어야 프로세스 재생성 뒤 스택이 복원된다 (#1695).
 */
public sealed interface SettingRoute : NavKey {
    @Serializable
    public data object SettingHomeRoute : SettingRoute

    @Serializable
    public data object WithdrawGuideRoute : SettingRoute

    @Serializable
    public data object WithdrawConfirmRoute : SettingRoute

    @Serializable
    public data object ProfileEditRoute : SettingRoute

    @Serializable
    public data object PasswordChangeRoute : SettingRoute

    @Serializable
    public data object LinkedAccountRoute : SettingRoute

    @Serializable
    public data object NotificationRoute : SettingRoute

    /**
     * 수신자 목록. [selectForDeliveryConditions] 가 참이면 관리 목록이 아니라 사후 전달 조건을
     * 정할 수신자를 고르는 선택 목록이다 — 진입 경로 결정(#1998)과 무관하게 현행 분기를 그대로 나른다.
     */
    @Serializable
    public data class RecipientListRoute(
        public val selectForDeliveryConditions: Boolean = false,
    ) : SettingRoute

    @Serializable
    public data object PushNotificationRoute : SettingRoute

    @Serializable
    public data object RecipientRegisterRoute : SettingRoute

    @Serializable
    public data class RecipientEditRoute(
        public val receiverId: Long,
    ) : SettingRoute

    @Serializable
    public data class AfterDeliveryRoute(
        public val receiverId: Long,
    ) : SettingRoute

    @Serializable
    public data object PasskeyRoute : SettingRoute

    @Serializable
    public data object PasskeyMakingRoute : SettingRoute

    @Serializable
    public data object AppLockSetupRoute : SettingRoute

    @Serializable
    public data object PasskeyPasswordRoute : SettingRoute

    @Serializable
    public data object NoticeRoute : SettingRoute
}
