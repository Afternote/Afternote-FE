package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R

/**
 * 패스키 안내(제목+설명+열쇠 아이콘) 레이아웃 — 미등록 안내([PassKeyScreen])와 목록의 0건 상태
 * (빈 상태)가 문구만 다르고 레이아웃이 같아 공유한다. 문구는 호출부가 각자의 문자열 리소스로 전달.
 */
@Composable
internal fun PasskeyGuideContent(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = AfternoteDesign.typography.bodyLargeB,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = AfternoteDesign.typography.bodySmallR,
        )
        Spacer(modifier = Modifier.height(84.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .width(326.dp)
                    .height(260.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_passkey_main),
                contentDescription = "패스키 메인 로고",
                contentScale = ContentScale.FillWidth,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PasskeyGuideContentPrev() {
    PasskeyGuideContent(
        title = "패스키 관리",
        description = "패스키를 만들어 비밀번호 대신 지문 및 얼굴 인식으로\n쉽고 안전하게 로그인할 수 있습니다.",
    )
}
