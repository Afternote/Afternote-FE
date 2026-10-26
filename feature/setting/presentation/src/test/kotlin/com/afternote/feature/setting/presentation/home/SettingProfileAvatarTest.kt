package com.afternote.feature.setting.presentation.home

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.home.component.SettingProfile
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException
import java.util.Collections
import kotlin.math.sqrt
import com.afternote.core.ui.R as CoreUiR

@OptIn(DelicateCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class SettingProfileAvatarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var imageLoader: ImageLoader
    private lateinit var hostView: View
    private var imageUrl by mutableStateOf<String?>(null)
    private val requests = Collections.synchronizedList(mutableListOf<Any>())
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        imageLoader =
            ImageLoader
                .Builder(context)
                .components {
                    add(
                        Interceptor { chain ->
                            requests += chain.request.data
                            if (chain.request.data == BROKEN) {
                                ErrorResult(null, chain.request, IOException("missing image"))
                            } else {
                                val bitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(if (chain.request.data == FIRST) Color.RED else Color.GREEN)
                                SuccessResult(bitmap.asImage(), chain.request)
                            }
                        },
                    )
                }.build()
        SingletonImageLoader.setUnsafe(imageLoader)
        composeRule.setContent {
            hostView = LocalView.current
            AfternoteTheme {
                SettingProfile(
                    name = "name",
                    email = "user@example.com",
                    profileImageUrl = imageUrl,
                    onInquiryClick = {},
                    onNoticeClick = {},
                    onRecipientListClick = {},
                )
            }
        }
    }

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
        imageLoader.shutdown()
    }

    @Test
    fun `등록 사진을 그리고 다른 URL로 바뀌면 새 사진을 그린다`() {
        show(FIRST)
        waitForColor(Color.RED)
        show(SECOND)
        waitForColor(Color.GREEN)
        assertEquals(listOf(FIRST, SECOND), requests)
    }

    @Test
    fun `사진 삭제와 빈 URL은 기본 이미지로 돌아가고 새 이미지 요청을 하지 않는다`() {
        val defaultPixels = avatarPixels()
        show(FIRST)
        waitForColor(Color.RED)

        show(null)
        assertArrayEquals(defaultPixels, avatarPixels())
        show(" ")
        assertArrayEquals(defaultPixels, avatarPixels())
        assertEquals(listOf(FIRST), requests)
    }

    @Test
    fun `새 사진 로드 실패는 이전 사진 대신 기본 이미지를 그린다`() {
        val defaultPixels = avatarPixels()
        show(FIRST)
        waitForColor(Color.RED)

        show(BROKEN)
        composeRule.waitUntil(5_000) { requests.contains(BROKEN) }
        composeRule.waitUntil(5_000) { avatarPixels().contentEquals(defaultPixels) }
        assertArrayEquals(defaultPixels, avatarPixels())
    }

    private fun show(url: String?) {
        composeRule.runOnIdle { imageUrl = url }
        composeRule.waitForIdle()
    }

    private fun waitForColor(color: Int) {
        composeRule.waitUntil(5_000) {
            val pixels = avatarPixels()
            val width = sqrt(pixels.size.toDouble()).toInt()
            pixels[pixels.size / 2 + width / 2] == color
        }
    }

    private fun avatarPixels(): IntArray {
        val description = context.getString(CoreUiR.string.core_ui_content_description_profile_image)
        val bounds = composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        return composeRule.runOnIdle {
            val bitmap = Bitmap.createBitmap(hostView.width, hostView.height, Bitmap.Config.ARGB_8888)
            hostView.draw(Canvas(bitmap))
            val width = bounds.width.toInt()
            val height = bounds.height.toInt()
            IntArray(width * height).also {
                bitmap.getPixels(it, 0, width, bounds.left.toInt(), bounds.top.toInt(), width, height)
            }
        }
    }

    private companion object {
        const val FIRST = "https://images.example/first.jpg"
        const val SECOND = "https://images.example/second.jpg"
        const val BROKEN = "https://images.example/missing.jpg"
    }
}
