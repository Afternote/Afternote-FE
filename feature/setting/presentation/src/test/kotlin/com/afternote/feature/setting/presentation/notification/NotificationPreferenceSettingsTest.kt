package com.afternote.feature.setting.presentation.notification

import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.model.user.Receiver
import com.afternote.feature.setting.domain.NotificationAnniversary
import com.afternote.feature.setting.domain.NotificationDelegate
import com.afternote.feature.setting.domain.NotificationPreferenceRepository
import com.afternote.feature.setting.domain.NotificationPreferences
import com.afternote.feature.setting.presentation.notification.anniversary.AnniversaryIntent
import com.afternote.feature.setting.presentation.notification.anniversary.AnniversaryViewModel
import com.afternote.feature.setting.presentation.notification.delegate.DelegateIntent
import com.afternote.feature.setting.presentation.notification.delegate.DelegateViewModel
import com.afternote.feature.setting.presentation.notification.preference.NotificationPreferenceIntent
import com.afternote.feature.setting.presentation.notification.preference.NotificationPreferenceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/** 서버 알림 설정 세 화면(기록 알림·대리인·기념일)의 ViewModel 회귀 기준 (#2321). */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferenceSettingsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `기록 알림 - 고친 값을 저장 한 번에 보낸다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationPreferenceRepository()
            val viewModel = NotificationPreferenceViewModel(repository)
            advanceUntilIdle()

            viewModel.onIntent(NotificationPreferenceIntent.ChangeTime(LocalTime.of(21, 30, 15)))
            viewModel.onIntent(NotificationPreferenceIntent.ToggleWeekday(DayOfWeek.MONDAY))
            viewModel.onIntent(NotificationPreferenceIntent.ChangeWeeklyGoal(9))
            assertTrue(repository.savedPreferences.isEmpty())

            viewModel.onIntent(NotificationPreferenceIntent.Save)
            advanceUntilIdle()

            val saved = repository.savedPreferences.single()
            assertEquals(LocalTime.of(21, 30), saved.time)
            assertEquals(DayOfWeek.entries.toSet() - DayOfWeek.MONDAY, saved.weekdays)
            assertEquals(7, saved.weeklyGoal)
            assertTrue(viewModel.uiState.value.isSaved)
        }

    @Test
    fun `기록 알림 - 요일을 모두 끄면 보내지 않고 이유를 알린다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationPreferenceRepository()
            val viewModel = NotificationPreferenceViewModel(repository)
            advanceUntilIdle()

            DayOfWeek.entries.forEach { viewModel.onIntent(NotificationPreferenceIntent.ToggleWeekday(it)) }
            viewModel.onIntent(NotificationPreferenceIntent.Save)
            advanceUntilIdle()

            assertTrue(repository.savedPreferences.isEmpty())
            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `기록 알림 - 불러오기 전에는 기본값을 그리지 않는다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationPreferenceRepository(loadFails = true)
            val viewModel = NotificationPreferenceViewModel(repository)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.form)
            assertTrue(viewModel.uiState.value.loadFailed)
        }

    @Test
    fun `대리인 - 연락처 숫자만 남겨 저장하고 해제한다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationPreferenceRepository()
            val viewModel = DelegateViewModel(repository)
            advanceUntilIdle()

            viewModel.onIntent(DelegateIntent.Save(" 김대리 ", "010-1234-5678"))
            advanceUntilIdle()
            assertEquals(NotificationDelegate("김대리", "01012345678"), repository.delegate)
            assertEquals(NotificationDelegate("김대리", "01012345678"), viewModel.uiState.value.delegate)

            viewModel.onIntent(DelegateIntent.Remove)
            advanceUntilIdle()
            assertNull(repository.delegate)
            assertNull(viewModel.uiState.value.delegate)
        }

    @Test
    fun `대리인 - 이름이나 연락처가 비면 보내지 않는다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationPreferenceRepository()
            val viewModel = DelegateViewModel(repository)
            advanceUntilIdle()

            viewModel.onIntent(DelegateIntent.Save(" ", "01012345678"))
            viewModel.onIntent(DelegateIntent.ConsumeError)
            viewModel.onIntent(DelegateIntent.Save("김대리", ""))
            advanceUntilIdle()

            assertNull(repository.delegate)
            assertNotNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `기념일 - 수신인 이름을 붙여 날짜순으로 보이고 추가·삭제가 반영된다`() =
        runTest(dispatcher) {
            val repository =
                FakeNotificationPreferenceRepository(
                    anniversaries = mutableListOf(NotificationAnniversary(id = 1, receiverId = 10, month = 5, day = 8)),
                )
            val receivers = FakeUserReceiverRepository(receivers = listOf(Receiver(10, "엄마", "MOTHER"), Receiver(11, "아빠", "FATHER")))
            val viewModel = AnniversaryViewModel(repository, receivers)
            advanceUntilIdle()

            assertEquals(
                listOf("엄마"),
                viewModel.uiState.value.items
                    .map { it.receiverName },
            )

            viewModel.onIntent(AnniversaryIntent.Add(receiverId = 11, month = 2, day = 29))
            advanceUntilIdle()
            assertEquals(
                listOf("아빠", "엄마"),
                viewModel.uiState.value.items
                    .map { it.receiverName },
            )

            viewModel.onIntent(AnniversaryIntent.Delete(1))
            advanceUntilIdle()
            assertEquals(
                listOf("아빠"),
                viewModel.uiState.value.items
                    .map { it.receiverName },
            )
        }

    private class FakeNotificationPreferenceRepository(
        private val loadFails: Boolean = false,
        val anniversaries: MutableList<NotificationAnniversary> = mutableListOf(),
    ) : NotificationPreferenceRepository {
        val savedPreferences = mutableListOf<NotificationPreferences>()
        var delegate: NotificationDelegate? = null

        override suspend fun getPreferences(): Result<NotificationPreferences> =
            if (loadFails) {
                Result.failure(IllegalStateException("offline"))
            } else {
                Result.success(NotificationPreferences(true, LocalTime.of(18, 0), DayOfWeek.entries.toSet(), 3))
            }

        override suspend fun updatePreferences(preferences: NotificationPreferences): Result<NotificationPreferences> {
            savedPreferences += preferences
            return Result.success(preferences)
        }

        override suspend fun getDelegate(): Result<NotificationDelegate?> = Result.success(delegate)

        override suspend fun updateDelegate(delegate: NotificationDelegate): Result<Unit> {
            this.delegate = delegate
            return Result.success(Unit)
        }

        override suspend fun deleteDelegate(): Result<Unit> {
            delegate = null
            return Result.success(Unit)
        }

        override suspend fun getAnniversaries(): Result<List<NotificationAnniversary>> = Result.success(anniversaries.toList())

        override suspend fun addAnniversary(
            receiverId: Long,
            month: Int,
            day: Int,
        ): Result<NotificationAnniversary> {
            val anniversary = NotificationAnniversary(id = (anniversaries.maxOfOrNull { it.id } ?: 0) + 1, receiverId, month, day)
            anniversaries += anniversary
            return Result.success(anniversary)
        }

        override suspend fun deleteAnniversary(anniversaryId: Long): Result<Unit> {
            anniversaries.removeAll { it.id == anniversaryId }
            return Result.success(Unit)
        }
    }
}
