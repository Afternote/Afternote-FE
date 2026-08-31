package com.afternote.afternote_fe

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.feature.timeletter.data.repositoryImpl.VoiceRecorderRepositoryImpl
import com.afternote.feature.timeletter.domain.model.RecordedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `VoiceRecorderRepositoryImpl` 이 실제 `MediaRecorder → filesDir → FileProvider` 경계를
 * 실행하는지 검증한다. 기존 `FakeVoiceRecorderRepository` 는 `start`/`stop` 호출을 항상
 * 실패시키도록 되어 있어, fake 로만 짠 테스트는 이 경계를 한 번도 타지 않는다 (#437 리뷰).
 */
@RunWith(AndroidJUnit4::class)
class VoiceRecordingAndroidTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = VoiceRecorderRepositoryImpl(context, Dispatchers.IO)

    @Before
    fun grantMicrophonePermission() {
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
    }

    @Test
    fun startThenStop_returnsPlayableAudioMp4File() =
        runBlocking {
            val audio = recordFor(RECORDING_DURATION_MILLIS)
            val uri = Uri.parse(audio.uriString)

            assertEquals("audio/mp4", audio.mimeType)
            assertTrue("파일명이 .m4a 로 끝나야 한다: ${audio.fileName}", audio.fileName.endsWith(".m4a"))
            assertTrue(
                "기록된 길이가 실제 녹음 시간에 못 미친다: ${audio.durationMillis}",
                audio.durationMillis >= RECORDING_DURATION_MILLIS - DURATION_TOLERANCE_MILLIS,
            )
            assertEquals("${context.packageName}.timeletter.fileprovider", uri.authority)

            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            assertTrue("녹음 파일이 비어 있으면 안 된다", bytes.isNotEmpty())

            repository.deleteRecordedFile(audio.uriString)
        }

    @Test
    fun discard_afterStop_deletesTheUnretainedFile() =
        runBlocking {
            val audio = recordFor(RECORDING_DURATION_MILLIS)
            val uri = Uri.parse(audio.uriString)

            repository.discard()

            assertFileGone(uri)
        }

    @Test
    fun retainRecordedFile_survivesDiscardUntilExplicitDelete() =
        runBlocking {
            val audio = recordFor(RECORDING_DURATION_MILLIS)
            val uri = Uri.parse(audio.uriString)
            repository.retainRecordedFile()

            // retain 이후에는 discard() 가 이 파일을 건드리지 않아야 한다.
            repository.discard()
            context.contentResolver.openInputStream(uri)!!.close()

            repository.deleteRecordedFile(audio.uriString)
            assertFileGone(uri)
        }

    @Test
    fun release_deletesRetainedFilesAsynchronously() =
        runBlocking {
            val audio = recordFor(RECORDING_DURATION_MILLIS)
            val uri = Uri.parse(audio.uriString)
            repository.retainRecordedFile()

            repository.release()

            val deleted =
                awaitCondition(timeoutMillis = RELEASE_TIMEOUT_MILLIS) {
                    runCatching { context.contentResolver.openInputStream(uri)?.close() }.isFailure
                }
            assertTrue("release() 이후에도 retain 된 파일이 정리되지 않았다", deleted)
        }

    private suspend fun recordFor(durationMillis: Long): RecordedAudio {
        repository.start().getOrThrow()
        delay(durationMillis)
        return repository.stop().getOrThrow()
    }

    private fun assertFileGone(uri: Uri) {
        val stillReadable = runCatching { context.contentResolver.openInputStream(uri)?.close() }.isSuccess
        assertTrue("파일이 삭제됐어야 한다: $uri", !stillReadable)
    }

    private fun awaitCondition(
        timeoutMillis: Long,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        return condition()
    }

    private companion object {
        const val RECORDING_DURATION_MILLIS = 1_200L
        const val DURATION_TOLERANCE_MILLIS = 300L
        const val RELEASE_TIMEOUT_MILLIS = 5_000L
        const val POLL_INTERVAL_MILLIS = 50L
    }
}
