package com.afternote.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.R as HomeR

@Composable
fun WeeklySummaryGrid(
    modifier: Modifier = Modifier,
    /**
     * 이번 주 기록 수. **기본값을 두지 않는다** — 넘기지 않은 호출부가 컴파일에서 걸려야
     * 이 자리가 잊히지 않는다 (#207 리뷰).
     *
     * 종전 기본값 `7` 은 명백한 목업이라 오히려 눈에 띄었는데, `0` 은 «이번 주에 아무것도
     * 안 썼다» 는 **그럴듯한 거짓**이라 사용자가 그대로 믿는다.
     *
     * `null` 은 «아직 모름» 이라 숫자 대신 대시를 그린다. 0 은 확정값이라 그대로 그린다.
     */
    recordedCount: Int?,
    onImageClick: () -> Unit,
    onCountCardClick: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = 8.dp
        // 수학적 계산: 전체 너비를 3등분하여 작은 사각형의 기준 사이즈를 구함
        val smallSquareSize = (maxWidth - (gap * 2)) / 3
        val largeSquareSize = (smallSquareSize * 2) + gap

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            // [좌측] 큰 정사각형 카드 (RECORDED MOMENT)
            Surface(
                modifier = Modifier.size(largeSquareSize),
                shape = RoundedCornerShape(6.dp),
                onClick = onImageClick,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(R.drawable.core_ui_img_recorded_moment),
                        // **장식이다 — 라벨은 바로 위에 겹쳐 그리는 «RECORDED MOMENT» 텍스트가 준다.**
                        // 종전에는 "recorded moment" 를 하드코딩해 두어, 스크린리더가 같은 뜻을 두 번
                        // 읽었다(이미지 라벨 + 그 텍스트). 리소스로 옮기는 대신 중복을 없앤다 (#1471).
                        // 누를 대상은 이 이미지가 아니라 감싼 Surface 이고, 그쪽이 클릭 semantics 를 갖는다.
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = "RECORDED MOMENT",
                        style = AfternoteDesign.typography.mono,
                        color = AfternoteDesign.colors.white,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                    )
                }
            }

            // [우측] 이번 주 기록 횟수 카드.
            //
            // 종전에는 이 자리에 작은 카드 2장(기록 횟수 / 최근 깊은생각)이 있었다. 깊은
            // 생각이 기획에서 제거되면서 아래 카드가 사라져, 남은 한 장이 왼쪽 큰 카드와
            // 같은 높이를 채운다 — 한 장만 남기고 아래를 비우면 그리드가 어긋난다.
            Column(
                modifier = Modifier.height(largeSquareSize),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                Surface(
                    modifier = Modifier.width(smallSquareSize).height(largeSquareSize),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
                    color = AfternoteDesign.colors.white,
                    onClick = onCountCardClick,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "THIS WEEK",
                            style = AfternoteDesign.typography.mono,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Text(
                            text = "기록된 순간들",
                            // 시안(4327:99094)은 11 / 16.5px(150%)다. `footnoteCaption` 은 10/16 이라
                            // fontSize 만 덮으면 행간 16sp 가 그대로 상속돼 145% 가 된다 — letterSpacing 은
                            // 명시하면서 lineHeight 만 빠져 있었다 (#1580).
                            style =
                                AfternoteDesign.typography.footnoteCaption.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 16.5.sp,
                                    letterSpacing = 0.005.em,
                                ),
                            color = AfternoteDesign.colors.gray6,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text =
                                recordedCount?.toString()
                                    ?: stringResource(HomeR.string.home_tab_weekly_count_unavailable),
                            style =
                                AfternoteDesign.typography.inter.copy(
                                    fontSize = 24.sp,
                                    lineHeight = 36.sp,
                                    letterSpacing = 0.003.em,
                                ),
                            color = AfternoteDesign.colors.black,
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklySummaryGridPreview() {
    AfternoteTheme {
        WeeklySummaryGrid(
            recordedCount = 3,
            onCountCardClick = {},
            onImageClick = {},
        )
    }
}
