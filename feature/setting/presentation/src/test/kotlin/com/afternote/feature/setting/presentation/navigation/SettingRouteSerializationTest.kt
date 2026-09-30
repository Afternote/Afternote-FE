package com.afternote.feature.setting.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 프로세스 재생성은 `rememberNavBackStack` 이 키를 직렬화해 되살린다 — 설정 키 전부와 인자가 왕복해야 한다 (#1695).
 *
 * `SavedState` 가 Bundle 이라 Robolectric 이 필요하다. back 체인은 순수 JVM 인 [SettingLocalNavActionsTest] 가 본다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingRouteSerializationTest {
    @Test
    fun `모든 키와 route 인자가 직렬화를 왕복한다`() {
        val routes =
            listOf(
                SettingRoute.SettingHomeRoute,
                SettingRoute.WithdrawGuideRoute,
                SettingRoute.WithdrawConfirmRoute,
                SettingRoute.ProfileEditRoute,
                SettingRoute.PasswordChangeRoute,
                SettingRoute.LinkedAccountRoute,
                SettingRoute.NotificationRoute,
                SettingRoute.PushNotificationRoute,
                SettingRoute.RecipientListRoute(selectForDeliveryConditions = true),
                SettingRoute.RecipientRegisterRoute,
                SettingRoute.RecipientEditRoute(37L),
                SettingRoute.AfterDeliveryRoute(91L),
                SettingRoute.PasskeyRoute,
                SettingRoute.PasskeyMakingRoute,
                SettingRoute.PasskeyPasswordRoute,
                SettingRoute.AppLockSetupRoute,
                SettingRoute.NoticeRoute,
            )
        val backStack = NavBackStack<NavKey>(*routes.toTypedArray())
        val serializer = NavBackStackSerializer(NavKeySerializer<NavKey>())

        val restored = decodeFromSavedState(serializer, encodeToSavedState(serializer, backStack))

        assertEquals(routes, restored.toList())
    }
}
