package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.core.ui.R as CoreUiR

@Composable
fun MemorySpaceHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(
        modifier =
            modifier
                .fillMaxWidth()
                // 이 화면은 Scaffold 없이 전체 화면 Box 위에 직접 얹히므로 시스템 인셋이 적용되지
                // 않는다. statusBarsPadding 을 빼면 "돌아가기" 가 상태바 아래로 들어가 터치가
                // 상태바에 먹혀 실제로 눌리지 않는다 (#559) — 아래 24.dp 는 그 위에 얹는 여백이다.
                .statusBarsPadding()
                // 좌우를 같이 준다. 종전에는 start 만 있어서 제목 블록이 오른쪽 화면 끝까지
                // 밀고 나갔다 — 360dp + 글자 1.5배 실측에서 우측 여백이 1.5dp 였다 (#1153).
                .padding(top = 24.dp, start = 24.dp, end = 24.dp),
    ) { constraints ->
        val gap = HEADER_GAP.roundToPx()
        val width = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        val back = subcompose(HeaderSlot.Back) { BackPill(onBackClick) }.first().measure(loose)

        // **먼저 시안대로 놓아 본다** — 정본(4327:67706)은 제목 블록을 화면 중앙에 둔다.
        // 폭이 넉넉하면(기본 배율·기본 화면) 그대로 중앙이고 develop 과 달라지지 않는다.
        val centered = subcompose(HeaderSlot.TitleCentered) { HeaderTitle() }.first().measure(loose)
        val centeredX = (width - centered.width) / 2

        // 중앙에 두면 「돌아가기」와 겹치는 경우에만 남은 폭으로 접는다. 좁은 화면과 큰 글자가
        // 곱해지는 조건에서만 걸리고, 그때는 «겹치지 않는 것» 이 «중앙» 보다 우선한다 (#1153).
        val fitsCentered = centeredX >= back.width + gap
        val slotStart = back.width + gap
        val remaining = (width - slotStart).coerceAtLeast(0)
        val title =
            if (fitsCentered) {
                centered
            } else {
                subcompose(HeaderSlot.TitleConstrained) { HeaderTitle() }
                    .first()
                    .measure(loose.copy(maxWidth = remaining))
            }
        // 겹칠 때는 **필요한 만큼만** 오른쪽으로 민다. 남은 영역의 가운데로 옮기면 겹침은
        // 똑같이 풀리면서 화면 중심에서 더 멀어진다(360dp 실측 236dp vs 199dp) — 시안이 요구하는
        // 것은 중앙이므로, 중앙에서의 이탈을 최소로 두는 쪽이 맞다.
        val titleX = maxOf(centeredX, slotStart)

        layout(width, maxOf(back.height, title.height)) {
            back.place(0, 0)
            title.place(titleX, 0)
        }
    }
}

/** 서브컴포즈 슬롯 키. 같은 내용을 두 제약으로 재어 보므로 키가 갈려 있어야 한다. */
private enum class HeaderSlot { Back, TitleCentered, TitleConstrained }

/** 「돌아가기」와 제목 블록 사이 최소 간격. */
private val HEADER_GAP = 13.dp

@Composable
private fun BackPill(onBackClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .shadow(2.dp, CircleShape)
                .background(AfternoteDesign.colors.white, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onBackClick),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(start = 20.dp, end = 15.dp)
                    .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_arrow_left),
                contentDescription = stringResource(R.string.mindrecord_memory_space_back),
                modifier = Modifier.size(width = 4.dp, height = 7.dp),
                tint = AfternoteDesign.colors.gray7,
            )
            Text(
                text = stringResource(R.string.mindrecord_memory_space_back),
                style = AfternoteDesign.typography.inter,
                color = AfternoteDesign.colors.gray7,
            )
        }
    }
}

/**
 * 두 문구 모두 `inter` 토큰(13sp / 21sp)을 그대로 쓰거나 `fontSize` 만 덮고 있었다.
 *
 * `copy(fontSize = …)` 는 **lineHeight 를 원 토큰 값 그대로 남긴다.** 부제는 9sp 글자에
 * 21sp 행간이 붙어 행간 배율이 233% 였다 (#1487). 제목은 덮지도 않아 13/21 이었다.
 *
 * 시안(정리 Screen Design › 홈 › 기억공간)은 둘 다 150% 다 — 제목 11/16.5, 부제 9/13.5.
 * 그래서 `fontSize` 를 덮을 때는 `lineHeight` 를 **같이** 덮는다.
 *
 * **자간과 부제 색도 시안에 맞춘다** (#1548). `inter` 토큰의 `letterSpacing` 은 `-0.006em`(좁힘)인데
 * 시안은 두 문구 모두 **넓힘**이다 — 부호가 반대라 「MEMORY SPACE」 가 시안보다 붙어 보였다.
 *
 * | | 시안 tracking | em 환산 | 여기 값 |
 * |---|---|---|---|
 * | 제목 `4327:67831` | `+0.6145px` / 11px | +0.055864 | `0.0559.em` |
 * | 부제 `4327:67833` | `+0.167px` / 9px | +0.018556 | `0.0186.em` |
 *
 * **토큰을 고치지 않고 이 자리에서만 덮는다.** `typography.inter` 는 afternote·home·mindrecord·
 * timeletter 4개 모듈 12곳이 쓰고 담당도 갈려, 토큰을 움직이면 그 화면들이 전부 따라 움직인다 —
 * 소비자 전수 시안 대조가 선행돼야 하는 별건이다.
 */
@Composable
private fun HeaderTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.mindrecord_memory_space_title),
            style =
                AfternoteDesign.typography.inter.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.5.sp,
                    letterSpacing = 0.0559.em,
                ),
            color = AfternoteDesign.colors.gray6,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.mindrecord_memory_space_subtitle),
            style =
                AfternoteDesign.typography.inter.copy(
                    fontSize = 9.sp,
                    lineHeight = 13.5.sp,
                    letterSpacing = 0.0186.em,
                ),
            // 시안(4327:67833)은 #757575 = gray6 다. 종전 gray5(#9E9E9E)는 한 단계 옅어
            // 같은 노드 묶음 안에서 제목만 진하고 부제만 흐렸다 (#1548).
            color = AfternoteDesign.colors.gray6,
            // Column 의 CenterHorizontally 는 Text 상자를 가운데 놓을 뿐이고, 접힌 줄을
            // 서로 가운데로 맞추는 것은 textAlign 이다 — 없으면 둘째 줄이 왼쪽에 붙는다.
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorySpaceHeaderPreview() {
    AfternoteTheme {
        MemorySpaceHeader(
            onBackClick = {},
        )
    }
}
