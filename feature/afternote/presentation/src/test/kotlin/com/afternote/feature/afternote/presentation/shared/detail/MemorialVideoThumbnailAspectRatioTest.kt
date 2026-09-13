package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MemorialVideoThumbnail] 이 폭에 관계없이 시안 종횡비를 지키는지 고정한다 (#1780).
 *
 * 종전 코드는 `fillMaxWidth().height(183.dp)` 라 폭만 유연했고, 화면 폭에 따라 종횡비가 1.36:1까지
 * 흔들렸다. 내용물이 `ContentScale.Crop` 이라 모양이 흔들리면 잘려 나가는 그림도 기기마다 달라진다.
 *
 * 실기기 폭(화면 폭 − 72)이 350dp 를 넘지 않으므로 좁은 폭 쪽이 회귀의 실제 무대다. 고정 높이로
 * 되돌아가면 두 번째 테스트가 깨진다 — 240dp 폭에서 높이가 183dp 로 남기 때문이다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h2000dp")
class MemorialVideoThumbnailAspectRatioTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `폭이 시안과 같은 350dp 면 시안 높이 183dp 로 수렴한다`() {
        val (width, height) = boundsAtWidth(350)

        assertEquals(350f, width, WIDTH_TOLERANCE_DP)
        assertEquals(183f, height, HEIGHT_TOLERANCE_DP)
    }

    @Test
    fun `폭이 좁아져도 시안 종횡비를 유지한다`() {
        val (width, height) = boundsAtWidth(240)

        assertEquals(240f, width, WIDTH_TOLERANCE_DP)
        // 프로덕션 상수를 참조하지 않고 시안 수치를 다시 적는다 — 상수가 잘못 바뀌어도 이 단언은 깨진다.
        assertEquals(350f / 183f, width / height, RATIO_TOLERANCE)
    }

    /** 폭 [widthDp] 인 부모 안에 썸네일을 그리고 실제 레이아웃 크기를 dp 로 돌려준다. */
    private fun boundsAtWidth(widthDp: Int): Pair<Float, Float> {
        composeRule.setContent {
            AfternoteTheme {
                Box(modifier = Modifier.width(widthDp.dp)) {
                    MemorialVideoThumbnail(
                        thumbnailUrl = null,
                        modifier = Modifier.testTag(THUMBNAIL_TAG),
                    )
                }
            }
        }

        val bounds = composeRule.onNodeWithTag(THUMBNAIL_TAG, useUnmergedTree = true).getUnclippedBoundsInRoot()
        return (bounds.right - bounds.left).value to (bounds.bottom - bounds.top).value
    }

    private companion object {
        const val THUMBNAIL_TAG = "memorialVideoThumbnailUnderTest"

        /** 레이아웃은 픽셀 정수로 반올림되므로 dp 단언에는 1px 미만의 여유가 필요하다. */
        const val WIDTH_TOLERANCE_DP = 0.5f
        const val HEIGHT_TOLERANCE_DP = 0.5f
        const val RATIO_TOLERANCE = 0.01f
    }
}
