package com.afternote.afternote_fe.notification

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelStore
import com.afternote.afternote_fe.R
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.ui.theme.AfternoteTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** 실제 Effect를 렌더하고 시스템 권한 결과만 테스트 registry로 돌려준다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class NotificationPermissionEffectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val viewModelStore = ViewModelStore()
    private val store = RecordingPermissionStore()
    private val registry = PermissionResultRegistry()
    private val snackbar = SnackbarHostState()

    @After
    fun tearDown() {
        composeRule.runOnIdle { viewModelStore.clear() }
    }

    @Test
    @Config(sdk = [32])
    fun `API 32에서는 관찰과 권한 요청을 시작하지 않는다`() {
        setPermissionContent()
        composeRule.waitForIdle()

        assertEquals(0, store.subscriptions)
        assertEquals(emptyList<String>(), registry.permissions)
        assertEquals(0, store.recordedRequests)
    }

    @Test
    fun `로그인 뒤 한 번 요청하고 허용 결과를 Intent로 기록한다`() {
        val auth = FakeAuthRepository(loggedIn = false)
        setPermissionContent(auth)
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), registry.permissions)

        composeRule.runOnIdle { auth.loggedIn = true }
        composeRule.waitForIdle()
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), registry.permissions)

        composeRule.runOnIdle { registry.respond(granted = true) }
        composeRule.waitForIdle()
        assertEquals(1, store.recordedRequests)
        assertNull(snackbar.currentSnackbarData)
        assertEquals(1, registry.permissions.size)
    }

    @Test
    fun `이미 허용된 권한은 다이얼로그 없이 기록한다`() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        setPermissionContent()
        composeRule.waitForIdle()

        assertEquals(emptyList<String>(), registry.permissions)
        assertEquals(1, store.recordedRequests)
        assertNull(snackbar.currentSnackbarData)
    }

    @Test
    fun `거부 결과를 기록하고 안내 액션으로 앱 알림 설정을 연다`() {
        setPermissionContent()
        composeRule.waitForIdle()
        composeRule.runOnIdle { registry.respond(granted = false) }
        composeRule.waitForIdle()

        assertEquals(1, store.recordedRequests)
        val action = composeRule.activity.getString(R.string.notification_permission_denied_action)
        composeRule.onNodeWithText(action).performClick()
        composeRule.waitForIdle()
        val intent = shadowOf(composeRule.activity).nextStartedActivity
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(composeRule.activity.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertEquals(1, registry.permissions.size)
    }

    private fun setPermissionContent(auth: FakeAuthRepository = FakeAuthRepository(loggedIn = true)) {
        val viewModel = NotificationPermissionViewModel(auth, store)
        viewModelStore.put("permission", viewModel)
        val registryOwner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = registry
            }
        composeRule.setContent {
            AfternoteTheme {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                    SnackbarHost(snackbar)
                    NotificationPermissionEffect(snackbarHostState = snackbar, viewModel = viewModel)
                }
            }
        }
    }
}

private class RecordingPermissionStore : NotificationPermissionRequestStore {
    private val state = MutableStateFlow(false)
    var subscriptions = 0
    var recordedRequests = 0
    override val hasRequested: Flow<Boolean> = state.onStart { subscriptions++ }

    override suspend fun markRequested() {
        recordedRequests++
        state.value = true
    }
}

private class PermissionResultRegistry : ActivityResultRegistry() {
    val permissions = mutableListOf<String>()
    private var pendingRequestCode: Int? = null

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        permissions += input as String
        pendingRequestCode = requestCode
    }

    fun respond(granted: Boolean) {
        dispatchResult(checkNotNull(pendingRequestCode), granted)
    }
}
