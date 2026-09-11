package com.afternote.feature.setting.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingLocalNavActionsTest {
    private val stack = NavBackStack<NavKey>(SettingRoute.SettingHomeRoute)
    private var exits = 0
    private var logouts = 0
    private var withdrawals = 0
    private val actions =
        SettingLocalNavActions(
            stack,
            FeatureStackBoundary { exits++ },
            object : SettingExternalActions {
                override fun onLogoutSuccess() {
                    logouts++
                }

                override fun onWithdrawSuccess() {
                    withdrawals++
                }
            },
        )

    @Test
    fun `delivery selection and edit preserve the receiver and back chain`() {
        actions.onNavigateToRecipientListForDeliveryConditions()
        actions.onNavigateToAfterDelivery(37L)
        actions.onNavigateToRecipientEdit(37L)
        assertEquals(
            listOf(
                SettingRoute.SettingHomeRoute,
                SettingRoute.RecipientListRoute(true),
                SettingRoute.AfterDeliveryRoute(37L),
                SettingRoute.RecipientEditRoute(37L),
            ),
            stack.toList(),
        )
        repeat(3) { actions.popBack() }
        assertEquals(listOf(SettingRoute.SettingHomeRoute), stack.toList())
        assertEquals(0, exits)
        actions.popBack()
        assertEquals(1, exits)
        assertEquals(1, stack.size)
    }

    @Test
    fun `recipient management and registration return to their origin`() {
        actions.onNavigateToRecipientList()
        assertEquals(SettingRoute.RecipientListRoute(false), stack.last())
        actions.popBack()
        actions.onNavigateToRecipientRegister()
        actions.popBack()
        assertEquals(listOf(SettingRoute.SettingHomeRoute), stack.toList())
        stack[0] = SettingRoute.RecipientRegisterRoute
        actions.popBack()
        assertEquals(1, exits)
        assertEquals(listOf(SettingRoute.RecipientRegisterRoute), stack.toList())
    }

    @Test
    fun `withdrawal and logout only notify the root auth boundary`() {
        actions.onNavigateToWithdrawGuide()
        actions.onNavigateToWithdrawConfirm()
        val before = stack.toList()
        actions.onWithdrawSuccess()
        actions.onLogoutSuccess()
        assertEquals(1, withdrawals)
        assertEquals(1, logouts)
        assertEquals(before, stack.toList())
    }

    @Test
    fun `passkey and notification nested routes pop one screen at a time`() {
        actions.onNavigateToPasskey()
        actions.onNavigateToPasskeyMaking()
        actions.onNavigateToPasskeyPassword()
        actions.popBack()
        assertEquals(SettingRoute.PasskeyMakingRoute, stack.last())
        repeat(2) { actions.popBack() }
        actions.onNavigateToNotification()
        actions.onNavigateToPushNotification()
        actions.popBack()
        assertEquals(SettingRoute.NotificationRoute, stack.last())
    }

    @Test
    fun `all destination keys and route arguments survive serialization`() {
        val routes =
            listOf(
                SettingRoute.SettingHomeRoute,
                SettingRoute.WithdrawGuideRoute,
                SettingRoute.WithdrawConfirmRoute,
                SettingRoute.ProfileEditRoute,
                SettingRoute.LinkedAccountRoute,
                SettingRoute.NotificationRoute,
                SettingRoute.PushNotificationRoute,
                SettingRoute.RecipientListRoute(true),
                SettingRoute.RecipientRegisterRoute,
                SettingRoute.RecipientEditRoute(37L),
                SettingRoute.AfterDeliveryRoute(91L),
                SettingRoute.PasskeyRoute,
                SettingRoute.PasskeyMakingRoute,
                SettingRoute.PasskeyPasswordRoute,
                SettingRoute.AppLockSetupRoute,
                SettingRoute.NoticeRoute,
            )
        assertEquals(routes, Json.decodeFromString<List<SettingRoute>>(Json.encodeToString(routes)))
    }
}
