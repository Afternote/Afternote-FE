package com.afternote.feature.afternote.presentation.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 추억 노트 공통 세로 레이아웃(섹션 순서·간격만 담당).
 * 에디터·수신자(View) 화면에서 동일 골격을 쓰기 위해 분리했습니다.
 *
 * 슬롯은 data class가 아닌 **컴포저블의 직접 파라미터**로 열어, 호출 지점 기준 리컴포지션 추적이 끊기지 않게 합니다.
 *
 * @param sectionSpacing 섹션 사이 세로 간격
 * @param trailingSpacerHeight 하단 여백(편집 화면 등). 마지막 섹션 직후에만 두어 [spacedBy] 간격에 섞이지 않게 합니다.
 * @param audioContent 추모 음성 섹션 (#1118). 화면에 자리가 없으면 아무것도 emit 하지 않아 간격도 생기지 않습니다.
 */
@Composable
fun MemorialContent(
    introContent: @Composable () -> Unit,
    photoContent: @Composable () -> Unit,
    playlistContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sectionSpacing: Dp = 32.dp,
    trailingSpacerHeight: Dp = 0.dp,
    /** 플레이리스트와 수신자 사이 섹션. 수신자 상세는 "남기신 말씀"을 여기 넣는다(#274). */
    messageContent: @Composable () -> Unit = {},
    recipientContent: @Composable () -> Unit = {},
    videoContent: @Composable () -> Unit,
    audioContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
    ) {
        introContent()
        photoContent()
        playlistContent()
        messageContent()
        recipientContent()
        Column(modifier = Modifier.fillMaxWidth()) {
            // 영상·음성은 한 묶음으로 두고 그 안에서만 간격을 준다. 하단 여백은 이 묶음 *밖* 이 아니라
            // 안쪽 끝에 붙어야 spacedBy 간격에 섞이지 않는다 — 바깥 Column 의 자식이 되면 32dp 가 더 붙는다.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                videoContent()
                audioContent()
            }
            if (trailingSpacerHeight > 0.dp) {
                Spacer(modifier = Modifier.height(trailingSpacerHeight))
            }
        }
    }
}
