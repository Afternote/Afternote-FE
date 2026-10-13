package com.afternote.feature.setting.presentation.home

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.model.user.User
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingProfileImageTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeMyProfileRepository.strict()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `프로필 조회와 갱신 결과의 사진 URL을 홈 상태에 전달한다`() =
        runTest(dispatcher) {
            var imageUrl = "https://images.example/first.jpg"
            repository.onGetMyProfile = { User("name", "user@example.com", null, imageUrl) }
            val viewModel = viewModel()
            runCurrent()
            assertEquals(imageUrl, viewModel.profile().profileImageUrl)

            imageUrl = "https://images.example/second.jpg"
            viewModel.onIntent(SettingIntent.Refresh)
            runCurrent()
            assertEquals(imageUrl, viewModel.profile().profileImageUrl)
        }

    @Test
    fun `갱신 결과에 사진이 없으면 이전 URL을 남기지 않는다`() =
        runTest(dispatcher) {
            var imageUrl: String? = "https://images.example/first.jpg"
            repository.onGetMyProfile = { User("name", "user@example.com", null, imageUrl) }
            val viewModel = viewModel()
            runCurrent()
            assertEquals(imageUrl, viewModel.profile().profileImageUrl)

            imageUrl = null
            viewModel.onIntent(SettingIntent.Refresh)
            runCurrent()
            assertNull(viewModel.profile().profileImageUrl)
        }

    @Test
    fun `프로필 갱신 실패 뒤에는 이전 사진을 담은 성공 상태가 남지 않는다`() =
        runTest(dispatcher) {
            repository.onGetMyProfile = { User("name", "user@example.com", null, "https://images.example/first.jpg") }
            val viewModel = viewModel()
            runCurrent()

            repository.onGetMyProfile = { throw IOException("offline") }
            viewModel.onIntent(SettingIntent.Refresh)
            runCurrent()
            assertEquals(SettingProfileState.Error, viewModel.uiState.value.profile)
        }

    private fun viewModel() =
        SettingViewModel(
            authRepository = FakeAuthRepository.strict(),
            myProfileRepository = repository,
            accountRepository = FakeSettingAccountRepository.strict(),
        )

    private fun SettingViewModel.profile() = uiState.value.profile as SettingProfileState.Success
}
