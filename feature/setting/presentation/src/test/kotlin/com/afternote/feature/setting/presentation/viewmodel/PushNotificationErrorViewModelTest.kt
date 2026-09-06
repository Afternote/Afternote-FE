package com.afternote.feature.setting.presentation.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.NoOpErrorReporter
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PushNotificationErrorViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `푸시 설정은 조회 실패 후 재시도 성공 상태로 복구한다`() =
        runTest(dispatcher) {
            var attempts = 0
            val expected = UserPushSetting(timeLetter = true, mindRecord = false, afterNote = true)
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyPushSettings = {
                        attempts += 1
                        if (attempts == 1) error("offline")
                        expected
                    }
                }
            val viewModel =
                PushNotificationViewModel(
                    context = ApplicationProvider.getApplicationContext<Context>(),
                    userRepository = repository,
                    errorReporter = NoOpErrorReporter,
                )

            assertTrue(viewModel.uiState.value.isLoading)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(UiText.Resource(R.string.setting_push_load_error), viewModel.uiState.value.errorMessage)

            viewModel.retryLoadPushSettings()
            advanceUntilIdle()

            with(viewModel.uiState.value) {
                assertFalse(isLoading)
                assertNull(errorMessage)
                assertEquals(expected.timeLetter, isNewsletterOn)
                assertEquals(expected.mindRecord, isMindRecordOn)
                assertEquals(expected.afterNote, isAfternoteOn)
            }
            assertEquals(2, repository.getMyPushSettingsCalls)
        }
}
