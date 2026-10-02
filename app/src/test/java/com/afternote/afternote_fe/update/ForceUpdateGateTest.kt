package com.afternote.afternote_fe.update

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.appversion.AppVersionRepository
import com.afternote.core.model.appversion.AppVersionCheck
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 강제 업데이트 관문의 판정 계약 회귀 가드 (#1539).
 *
 * 이 관문이 잘못 켜지면 **모든 사용자가 앱에 들어오지 못한다**. 그래서 "막는다" 보다
 * "막지 않는다" 쪽 갈래를 더 촘촘히 고정한다 — 조회 실패·보낼 곳 없음·갱신 불가 빌드.
 */
class ForceUpdateGateTest {
    @Test
    fun `조회에 실패하면 관문을 걸지 않고 실패만 남긴다`() =
        runTest {
            val errorReporter = RecordingErrorReporter()
            val gate =
                gate(
                    repository = FailingAppVersionRepository(IOException("서버 무응답")),
                    errorReporter = errorReporter,
                )

            gate.refresh()

            assertNull(gate.prompt.value)
            val reported = errorReporter.attributes.single()
            assertEquals("force_update_check", reported["stage"])
            assertEquals(IOException::class.java.name, reported["error_type"])
        }

    @Test
    fun `업데이트가 필요 없으면 관문을 걸지 않는다`() =
        runTest {
            val errorReporter = RecordingErrorReporter()
            val gate =
                gate(
                    repository = check(updateRequired = false, storeUrl = null),
                    errorReporter = errorReporter,
                )

            gate.refresh()

            assertNull(gate.prompt.value)
            assertTrue(errorReporter.attributes.isEmpty())
        }

    @Test
    fun `스토어 배포 빌드에 실 스토어 주소가 오면 관문을 건다`() =
        runTest {
            val gate =
                gate(
                    repository = check(updateRequired = true, storeUrl = PLAY_URL),
                    installedBuild = storeBuild,
                )

            gate.refresh()

            assertEquals(ForceUpdatePrompt(PLAY_URL), gate.prompt.value)
        }

    @Test
    fun `market 스킴도 실 스토어 주소로 본다`() =
        runTest {
            val marketUrl = "market://details?id=com.afternote.afternote_fe"
            val gate =
                gate(
                    repository = check(updateRequired = true, storeUrl = marketUrl),
                    installedBuild = storeBuild,
                )

            gate.refresh()

            assertEquals(ForceUpdatePrompt(marketUrl), gate.prompt.value)
        }

    @Test
    fun `보낼 스토어 주소가 없으면 관문을 걸지 않고 실패를 남긴다`() =
        runTest {
            val errorReporter = RecordingErrorReporter()
            val gate =
                gate(
                    repository = check(updateRequired = true, storeUrl = null),
                    installedBuild = storeBuild,
                    errorReporter = errorReporter,
                )

            gate.refresh()

            assertNull(gate.prompt.value)
            val reported = errorReporter.attributes.single()
            assertEquals("force_update_store_url", reported["stage"])
            // 타입을 이름으로 부르지 않는다 — 그 타입은 이 파일 밖으로 나갈 이유가 없다(#1678).
            // 콘솔에서 이 갈래를 가르는 것이 곧 이 문자열이라, 관측되는 값 그대로 고정한다.
            assertEquals(
                "com.afternote.afternote_fe.update.UnroutableStoreUrlException",
                reported["error_type"],
            )
        }

    @Test
    fun `플레이스홀더 주소는 스토어로 보지 않는다`() =
        runTest {
            val errorReporter = RecordingErrorReporter()
            val gate =
                gate(
                    // dev 서버가 APP_ANDROID_STORE_URL 미설정으로 실제 내려주는 값 (2026-08-30 실측).
                    repository = check(updateRequired = true, storeUrl = "http://your-playstore-domain"),
                    installedBuild = storeBuild,
                    errorReporter = errorReporter,
                )

            gate.refresh()

            assertNull(gate.prompt.value)
            assertEquals("force_update_store_url", errorReporter.attributes.single()["stage"])
        }

    @Test
    fun `스토어가 갱신할 수 없는 빌드면 관문을 걸지 않는다`() =
        runTest {
            val errorReporter = RecordingErrorReporter()
            val gate =
                gate(
                    repository = check(updateRequired = true, storeUrl = PLAY_URL),
                    installedBuild = InstalledBuild(versionCode = 1, storeDistributed = false),
                    errorReporter = errorReporter,
                )

            gate.refresh()

            assertNull(gate.prompt.value)
            assertTrue(errorReporter.attributes.isEmpty())
        }

    @Test
    fun `설치본의 versionCode 를 그대로 서버에 묻는다`() =
        runTest {
            val repository = RecordingAppVersionRepository(AppVersionCheck(false, 10_001, null))
            val gate = gate(repository = repository, installedBuild = InstalledBuild(10_002, true))

            gate.refresh()

            assertEquals(listOf(10_002), repository.asked)
        }

    private fun gate(
        repository: AppVersionRepository,
        installedBuild: InstalledBuild = InstalledBuild(versionCode = 1, storeDistributed = false),
        errorReporter: ErrorReporter = RecordingErrorReporter(),
    ) = ForceUpdateGate(
        appVersionRepository = repository,
        installedBuild = installedBuild,
        errorReporter = errorReporter,
    )

    private fun check(
        updateRequired: Boolean,
        storeUrl: String?,
    ) = RecordingAppVersionRepository(
        AppVersionCheck(updateRequired = updateRequired, latestVersionCode = 10_001, storeUrl = storeUrl),
    )

    private companion object {
        const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.afternote.afternote_fe"
        val storeBuild = InstalledBuild(versionCode = 10_000, storeDistributed = true)
    }
}

private class RecordingAppVersionRepository(
    private val result: AppVersionCheck,
) : AppVersionRepository {
    val asked = mutableListOf<Int>()

    override suspend fun checkAndroidVersion(versionCode: Int): Result<AppVersionCheck> {
        asked += versionCode
        return Result.success(result)
    }
}

private class FailingAppVersionRepository(
    private val error: Throwable,
) : AppVersionRepository {
    override suspend fun checkAndroidVersion(versionCode: Int): Result<AppVersionCheck> = Result.failure(error)
}

/**
 * [ErrorReporter] 는 예외 문구를 지우고 타입만 속성으로 남긴다. 그래서 단언은 `error_type` ·
 * `stage` 로 한다 — 던진 예외 인스턴스는 리포터를 통과하면서 사라진다.
 */
private class RecordingErrorReporter : ErrorReporter {
    val attributes = mutableListOf<Map<String, String>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        this.attributes += attributes
    }
}
