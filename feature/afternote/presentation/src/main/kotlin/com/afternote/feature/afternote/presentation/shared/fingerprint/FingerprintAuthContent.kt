package com.afternote.feature.afternote.presentation.shared.fingerprint

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * 지문 인증 컨텐츠 컴포넌트
 *
 * 안내 텍스트, 지문 아이콘, 인증 버튼을 포함하는 컨텐츠 영역
 */
@Composable
fun FingerprintAuthContent(
    modifier: Modifier = Modifier,
    onFingerprintAuthClick: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(0.35f))
        // 안내 텍스트
        Text(
            text = stringResource(R.string.biometric_prompt_subtitle),
            style =
                AfternoteDesign.typography.bodyBase,
        )

        Spacer(modifier = Modifier.height(39.dp))

        // 지문 아이콘
        Image(
            painter = painterResource(R.drawable.feature_afternote_ic_fingerprint),
            contentDescription = stringResource(R.string.biometric_prompt_title),
            modifier = Modifier.size(100.dp, 114.dp),
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 지문 인증 버튼
        AfternoteButton(
            text = stringResource(R.string.feature_afternote_fingerprint_auth_button),
            onClick = onFingerprintAuthClick,
            type = AfternoteButtonType.Default,
        )
        Spacer(Modifier.weight(0.65f))
    }
}

@Preview
@Composable
private fun FingerprintAuthContentPreview() {
    AfternoteTheme {
        FingerprintAuthContent(
            onFingerprintAuthClick = {},
        )
    }
}
