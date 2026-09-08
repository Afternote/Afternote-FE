package com.afternote.feature.afternote.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.presentation.NoopAuthorErrorReporter
import com.afternote.feature.afternote.presentation.afternoteAuthorUserProfileRepository
import com.afternote.feature.afternote.presentation.afternoteAuthorUserRepository
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 상세 재진입 갱신([AfternoteDetailViewModel.refreshOnReturn]) 계약 가드 (#701).
 *
 * 상세와 별도로 도착하는 작성자 표시명이 Success 에 실리는 경로도 함께 가드한다 — 두 요청의
 * 도착 순서에 따라 갈리고, 재진입 갱신이 만드는 새 Success 가 그 값을 떨어뜨리기 쉬운 자리다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteDetailViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `첫 진입 resume 은 재조회를 트리거하지 않는다`() =
        runTest {
            // init 로드가 «실패» 로 빠르게 끝난 뒤 첫 ON_RESUME 이 도착하는 CI 회귀 시나리오 —
            // 여기서 자동 재조회가 걸리면 에러 화면이 표시되지 않은 채 다음 응답을 소비해 버린다.
            val results =
                ArrayDeque<Result<Detail>>(
                    listOf(
                        Result.failure(IOException("offline")),
                        Result.success(detail(serviceName = "Instagram")),
                    ),
                )
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { results.removeFirst() }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            // 첫 진입 화면의 ON_RESUME (init 로드는 이미 실패로 종료됨).
            viewModel.refreshOnReturn()

            assertEquals(listOf(73L), repository.requestedDetailIds)
            assertTrue(states.last() is AfternoteDetailUiState.Error)
        }

    @Test
    fun `refreshOnReturn - 진행 중인 로드와 겹치면 건너뛴다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = {
                        gate.await()
                        Result.success(detail(serviceName = "Instagram"))
                    }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            // init 로드가 아직 도는 중 — 첫 resume(스킵) 뒤 또 한 번 resume 이 와도 중복이 없어야 한다.
            viewModel.refreshOnReturn()
            viewModel.refreshOnReturn()
            gate.complete(Unit)

            assertEquals(listOf(73L), repository.requestedDetailIds)
            assertEquals("Instagram", states.last().serviceNameOrNull())
        }

    @Test
    fun `refreshOnReturn - 복귀하면 로딩 없이 새 상세로 갱신한다`() =
        runTest {
            val details =
                ArrayDeque(
                    listOf(
                        Result.success(detail(serviceName = "Instagram")),
                        Result.success(detail(serviceName = "Facebook")),
                    ),
                )
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { details.removeFirst() }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME

            assertEquals(listOf(73L, 73L), repository.requestedDetailIds)
            assertEquals("Facebook", states.last().serviceNameOrNull())
            // 첫 Success 이후 Loading 을 다시 방출하지 않는다 — 재진입마다 스피너가 번쩍이지 않게.
            val firstSuccess = states.indexOfFirst { it is AfternoteDetailUiState.Success }
            assertTrue(states.drop(firstSuccess).none { it is AfternoteDetailUiState.Loading })
        }

    @Test
    fun `refreshOnReturn - 실패해도 보고 있던 상세를 유지하고 실패는 기록한다`() =
        runTest {
            val results =
                ArrayDeque(
                    listOf(
                        Result.success(detail(serviceName = "Instagram")),
                        Result.failure(IOException("일시적 실패")),
                    ),
                )
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { results.removeFirst() }
                }
            val reporter = RecordingErrorReporter()
            val viewModel = viewModel(repository, errorReporter = reporter)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME

            // 잘 보고 있던 상세가 에러 화면으로 대체되지 않는다.
            assertEquals("Instagram", states.last().serviceNameOrNull())
            assertTrue(states.none { it is AfternoteDetailUiState.Error })
            // 화면에 안 보이는 실패인 만큼 콘솔 기록은 남긴다.
            assertEquals(1, reporter.reportedErrors.size)
        }

    @Test
    fun `작성자 표시명이 상세보다 늦게 도착해도 Success 에 실린다`() =
        runTest {
            // 상세와 표시명은 출처가 다른 두 요청이라 도착 순서가 뒤집힌다. 이름이 늦는 쪽이 실제 경로 —
            // 그때 이미 그려진 Success 에 이름이 실려야 제목의 이름 세그먼트가 뒤늦게라도 채워진다.
            val profileGate = CompletableDeferred<Unit>()
            val userRepository =
                afternoteAuthorUserRepository().apply {
                    onGetMyProfile = {
                        profileGate.await()
                        profile
                    }
                }
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.success(detail(serviceName = "Instagram")) }
                }
            val viewModel = viewModel(repository, userRepository = userRepository)
            val states = recordStates(viewModel)

            // 이름이 도착하기 전 — 화면은 이미 상세를 그리고 있고 이름 자리는 비어 있다.
            assertEquals("", states.last().authorDisplayNameOrNull())

            profileGate.complete(Unit)

            assertEquals(userRepository.profile.name, states.last().authorDisplayNameOrNull())
        }

    @Test
    fun `재진입 갱신이 이미 도착한 작성자 표시명을 지우지 않는다`() =
        runTest {
            val results =
                ArrayDeque(
                    listOf(
                        Result.success(detail(serviceName = "Instagram")),
                        Result.success(detail(serviceName = "Threads")),
                    ),
                )
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { results.removeFirst() }
                }
            val userRepository = afternoteAuthorUserRepository()
            val viewModel = viewModel(repository, userRepository = userRepository)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME

            // 갱신이 만든 새 Success 도 이름을 들고 있어야 제목이 «…에 대한 기록» 으로 되돌아가지 않는다.
            assertEquals("Threads", states.last().serviceNameOrNull())
            assertEquals(userRepository.profile.name, states.last().authorDisplayNameOrNull())
        }

    @Test
    fun `캐시된 이름은 원격 응답을 기다리지 않고 제목에 실린다`() =
        runTest {
            // 원격만 쓰면 왕복이 끝나야 이름 세그먼트가 채워져, 진입마다 제목이 눈앞에서 다시 쓰인다.
            val profileGate = CompletableDeferred<Unit>()
            val userRepository =
                afternoteAuthorUserRepository().apply {
                    onGetMyProfile = {
                        profileGate.await()
                        profile.copy(name = "서버 이름")
                    }
                }
            val userProfileRepository = afternoteAuthorUserProfileRepository(cachedUserName = "캐시 이름")
            val viewModel =
                viewModel(
                    FakeAfternoteRepository.strict().apply {
                        onGetDetail = { Result.success(detail(serviceName = "Instagram")) }
                    },
                    userRepository = userRepository,
                    userProfileRepository = userProfileRepository,
                )
            val states = recordStates(viewModel)

            // 원격이 아직 오지 않은 시점에 이미 이름이 실려 있다.
            assertEquals("캐시 이름", states.last().authorDisplayNameOrNull())

            profileGate.complete(Unit)

            // 원격 값이 정본이라 캐시를 덮고, 다음 진입을 위해 캐시도 최신화한다.
            assertEquals("서버 이름", states.last().authorDisplayNameOrNull())
            assertEquals(listOf("서버 이름"), userProfileRepository.savedUserNames.toList())
        }

    @Test
    fun `캐시 조회가 실패해도 원격 이름으로 채운다`() =
        runTest {
            val userProfileRepository =
                afternoteAuthorUserProfileRepository().apply {
                    onGetCachedUserName = { throw IOException("DataStore 읽기 실패") }
                }
            val userRepository = afternoteAuthorUserRepository()
            val viewModel =
                viewModel(
                    FakeAfternoteRepository.strict().apply {
                        onGetDetail = { Result.success(detail(serviceName = "Instagram")) }
                    },
                    userRepository = userRepository,
                    userProfileRepository = userProfileRepository,
                )
            val states = recordStates(viewModel)

            assertEquals(userRepository.profile.name, states.last().authorDisplayNameOrNull())
        }

    @Test
    fun `캐시 저장이 실패해도 화면은 이름을 유지한다`() =
        runTest {
            // 저장은 «다음 진입» 을 위한 것이라, 실패해도 지금 보고 있는 화면을 흔들면 안 된다.
            val userProfileRepository =
                afternoteAuthorUserProfileRepository().apply {
                    onSaveUserName = { throw IOException("DataStore 쓰기 실패") }
                }
            val userRepository = afternoteAuthorUserRepository()
            val viewModel =
                viewModel(
                    FakeAfternoteRepository.strict().apply {
                        onGetDetail = { Result.success(detail(serviceName = "Instagram")) }
                    },
                    userRepository = userRepository,
                    userProfileRepository = userProfileRepository,
                )
            val states = recordStates(viewModel)

            assertEquals(userRepository.profile.name, states.last().authorDisplayNameOrNull())
            assertTrue(states.last() is AfternoteDetailUiState.Success)
        }

    @Test
    fun `재진입 갱신이 미소비 삭제 결과를 지우지 않는다`() =
        runTest {
            // 삭제 결과는 UI 가 LaunchedEffect 로 한 번 읽고 소비하는 신호다. 그 사이에 도착한 갱신이
            // 신호를 지우면 삭제에 성공하고도 화면이 닫히지 않는다.
            val details =
                ArrayDeque(
                    listOf(
                        Result.success(detail(serviceName = "Instagram")),
                        Result.success(detail(serviceName = "Threads")),
                    ),
                )
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { details.removeFirst() }
                    onDelete = { Result.success(Unit) }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            viewModel.deleteAfternote()
            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME

            val last = states.last() as AfternoteDetailUiState.Success
            assertEquals("Threads", states.last().serviceNameOrNull())
            assertEquals(AfternoteDetailDeleteResult.Succeeded(73L), last.deleteResult)
        }

    @Test
    fun `실패 화면에서 재시도하면 로딩을 띄우고 상세를 다시 조회한다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            var invocation = 0
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = {
                        invocation += 1
                        if (invocation == 1) {
                            Result.failure(IOException("offline"))
                        } else {
                            gate.await()
                            Result.success(detail(serviceName = "Instagram"))
                        }
                    }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            assertTrue(states.last() is AfternoteDetailUiState.Error)

            viewModel.retry()

            // 응답이 오기 전 — 사용자가 누른 동작이므로 기다림을 표시한다(자동 갱신과 갈리는 지점).
            assertTrue(states.last() is AfternoteDetailUiState.Loading)

            gate.complete(Unit)

            assertEquals("Instagram", states.last().serviceNameOrNull())
        }

    @Test
    fun `재시도가 자른 갱신은 그 응답으로 새 화면을 덮지 않는다`() =
        runTest {
            // 자동 갱신이 값을 받아 든 «뒤» 재시도가 그 로드를 자르는 창 — 여기서 옛 응답이 새 화면을
            // 덮으면 사용자가 재시도로 얻은 결과가 조용히 사라진다.
            lateinit var viewModelRef: AfternoteDetailViewModel
            var invocation = 0
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = {
                        invocation += 1
                        when (invocation) {
                            1 -> {
                                Result.success(detail(serviceName = "Instagram"))
                            }

                            2 -> {
                                viewModelRef.retry()
                                Result.success(detail(serviceName = "Stale"))
                            }

                            else -> {
                                Result.success(detail(serviceName = "Retry"))
                            }
                        }
                    }
                }
            val viewModel = viewModel(repository)
            viewModelRef = viewModel
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME — 이 로드가 재시도에 잘린다

            assertEquals("Retry", states.last().serviceNameOrNull())
        }

    @Test
    fun `상세를 보고 있지 않으면 삭제 요청을 보내지 않는다`() =
        runTest {
            // 상태 갱신은 updateSuccess 가 알아서 no-op 이지만 서버 호출은 아니다 — 막지 않으면
            // 노트는 지워지는데 화면은 아무것도 모른다(진행 표시·결과 안내·pop 전부 없음).
            val deletedIds = mutableListOf<Long>()
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.failure(IOException("offline")) }
                    onDelete = { id ->
                        deletedIds += id
                        Result.success(Unit)
                    }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            viewModel.deleteAfternote()

            assertTrue(states.last() is AfternoteDetailUiState.Error)
            assertEquals(emptyList<Long>(), deletedIds)
        }

    private fun TestScope.recordStates(viewModel: AfternoteDetailViewModel): List<AfternoteDetailUiState> {
        val states = mutableListOf<AfternoteDetailUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states += it }
        }
        return states
    }

    private fun viewModel(
        repository: FakeAfternoteRepository,
        errorReporter: ErrorReporter = NoopAuthorErrorReporter,
        userRepository: FakeUserRepository = afternoteAuthorUserRepository(),
        userProfileRepository: FakeUserProfileCacheRepository = afternoteAuthorUserProfileRepository(),
    ): AfternoteDetailViewModel =
        AfternoteDetailViewModel(
            route = AfternoteRoute.DetailRoute(itemId = 73L),
            afternoteRepository = repository,
            userRepository = userRepository,
            userProfileRepository = userProfileRepository,
            errorReporter = errorReporter,
        )
}

private fun AfternoteDetailUiState.serviceNameOrNull(): String? =
    ((this as? AfternoteDetailUiState.Success)?.contentUiModel as? DetailContentUiModel.SocialNetwork)
        ?.content
        ?.serviceName

private fun AfternoteDetailUiState.authorDisplayNameOrNull(): String? = (this as? AfternoteDetailUiState.Success)?.authorDisplayName

private fun detail(serviceName: String): Detail =
    Detail(
        id = 73L,
        serviceName = serviceName,
        timestamps = DetailTimestamps(updatedAt = "2026.08.22"),
        receivers = emptyList(),
        leaveMessageBlocks = emptyList(),
        content =
            DetailContent.SocialNetwork(
                credentials = DetailCredentials(id = "id@example.test", password = "pw"),
                processingMethods = listOf("계정 삭제"),
            ),
    )

private class RecordingErrorReporter : ErrorReporter {
    val reportedErrors = mutableListOf<Throwable>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        reportedErrors += throwable
    }
}
