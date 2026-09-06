package com.afternote.feature.afternote.presentation.shared.detail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R

// 작성자·수신자 상세가 함께 쓰는 «내용 대신 상태를 보여 주는» 화면들이다. 한쪽 기능 폴더에
// 두면 다른 쪽이 남의 폴더를 import 해야 한다 (#1514).

@Composable
fun DesignPendingDetailContent(onBackClick: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(title = "", onBackClick = onBackClick)
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.afternote_design_pending))
        }
    }
}

/**
 * 상세 데이터 로드 실패 화면.
 *
 * [com.afternote.feature.afternote.presentation.detail.AfternoteDetailUiState.Error] 계약대로
 * [messageRes] 를 [stringResource] 로 변환해 표시한다 (없으면 [R.string.afternote_detail_load_error] 폴백).
 * 예외 원문은 받지 않는다 — 서버 5xx 본문·역직렬화 예외 메시지에 내부 SQL·응답 원문 발췌가 섞여 오기 때문.
 *
 * 표시 방식 통일(#446) 결론이 나오면 이 컴포저블의 본문 표현만 교체한다 — Route 의 Error 분기 배선은 유지.
 * [DesignPendingDetailContent] 는 ESTATE 등 아직 구현되지 않은 상세 타입의 폴백으로만 유지한다.
 *
 * @param messageRes 앱에 박힌 문자열 리소스 ID(`R.string.*`). `@StringRes` 는 이 Int 가 임의 정수가 아니라
 *   string 리소스 id 임을 Lint 에 알리는 표식이며, [stringResource] 로 실제 텍스트로 변환한다.
 * @param onRetryClick 재조회 진입점. `null` 이면 재시도 버튼을 그리지 않는다 — 잘못된 항목 ID 처럼
 *   같은 요청을 다시 보내도 결과가 달라지지 않는 실패에 쓴다.
 */
@Composable
fun DetailLoadErrorContent(
    @StringRes messageRes: Int?,
    onBackClick: () -> Unit,
    onRetryClick: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(title = "", onBackClick = onBackClick)
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(messageRes ?: R.string.afternote_detail_load_error))
                if (onRetryClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onRetryClick) {
                        Text(
                            text = stringResource(R.string.afternote_detail_retry),
                            style = AfternoteDesign.typography.captionLargeB,
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
                }
            }
        }
    }
}
