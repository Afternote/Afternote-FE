package com.afternote.afternote_fe

import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 설치된 앱의 프로필 촬영 provider와 좁힌 캐시 경로를 공개 Android API로 검증한다. */
@RunWith(AndroidJUnit4::class)
class ProfilePhotoFileProviderAndroidTest {
    @Test
    fun installedProvider_onlySharesProfileCaptureFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authority = "${context.packageName}.setting.fileprovider"
        val provider =
            requireNotNull(context.packageManager.resolveContentProvider(authority, PackageManager.GET_META_DATA))

        assertEquals(context.packageName, provider.packageName)
        assertEquals("com.afternote.feature.setting.presentation.SettingFileProvider", provider.name)
        assertFalse(provider.exported)
        assertTrue(provider.grantUriPermissions)

        val captureDirectory = File(context.cacheDir, "profile_capture")
        assertTrue(captureDirectory.isDirectory || captureDirectory.mkdirs())
        val capture = File.createTempFile("provider-test-", ".jpg", captureDirectory)
        val outsideCapture = File.createTempFile("provider-test-outside-", ".jpg", context.cacheDir)
        try {
            val uri = FileProvider.getUriForFile(context, authority, capture)
            val resolver = context.contentResolver
            val contents = "profile capture provider test".toByteArray()

            assertEquals("content", uri.scheme)
            assertEquals(authority, uri.authority)
            assertEquals("image/jpeg", resolver.getType(uri))
            requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(contents) }
            assertArrayEquals(contents, capture.readBytes())
            val received = requireNotNull(resolver.openInputStream(uri)).use { it.readBytes() }
            assertArrayEquals(contents, received)

            assertThrows(IllegalArgumentException::class.java) {
                FileProvider.getUriForFile(context, authority, outsideCapture)
            }
            assertThrows(IllegalArgumentException::class.java) {
                FileProvider.getUriForFile(context, authority, File(captureDirectory, "../${outsideCapture.name}"))
            }
        } finally {
            capture.delete()
            outsideCapture.delete()
        }
    }
}
