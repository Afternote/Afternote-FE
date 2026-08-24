package com.afternote.afternote_fe.test

import android.graphics.Bitmap
import android.util.Log
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorageRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.FileInputStream

/** 실패 원인을 재시도 없이 진단할 수 있도록 화면과 최근 logcat을 UTP 추가 산출물에 남긴다. */
class FailureArtifactRule(
    private val captureScreenshot: () -> Bitmap,
) : TestWatcher() {
    override fun failed(
        e: Throwable?,
        description: Description,
    ) {
        val artifactName =
            "${description.className.substringAfterLast('.')}_${description.methodName}"
                .replace(Regex("[^A-Za-z0-9_.-]"), "_")

        runCatching {
            captureScreenshot().writeToTestStorage("${artifactName}_failure")
        }.onFailure { captureError ->
            Log.w(TAG, "실패 화면 캡처를 남기지 못했습니다.", captureError)
        }

        runCatching {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val descriptor =
                instrumentation.uiAutomation.executeShellCommand(
                    "logcat -d -v threadtime -t $LOGCAT_LINE_LIMIT",
                )
            val bytes =
                descriptor.use {
                    FileInputStream(it.fileDescriptor).use { input -> input.readBytes() }
                }
            PlatformTestStorageRegistry
                .getInstance()
                .openOutputFile("${artifactName}_logcat.txt")
                .buffered()
                .use { output -> output.write(bytes) }
        }.onFailure { logcatError ->
            Log.w(TAG, "실패 logcat을 남기지 못했습니다.", logcatError)
        }
    }

    private companion object {
        const val TAG = "FailureArtifactRule"
        const val LOGCAT_LINE_LIMIT = 2_000
    }
}
