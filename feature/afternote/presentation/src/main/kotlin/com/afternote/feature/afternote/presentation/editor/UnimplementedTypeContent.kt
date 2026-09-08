package com.afternote.feature.afternote.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * 디자인 미확정 카테고리(ESTATE) 의 임시 placeholder.
 *
 * 카테고리 enum 은 디자인-코드 정합을 위해 추가했지만, form/payload 구조는 디자인 확정 후
 * 별도 작업에서 정의한다. 본 화면에서 사용자가 카테고리 ESTATE 를 골랐을 때 입력 자리를
 * 의도적으로 비워 두고 "준비 중" 메시지만 보여 준다. 저장은 Validator·FormMapper 양쪽에서 차단된다.
 * (BUSINESS 는 시안 700:38735 확정으로 소셜 폼을 재사용하도록 개통 — 이슈 #467)
 */
@Composable
internal fun UnimplementedTypeContent(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .padding(vertical = 48.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.afternote_editor_category_unimplemented_title),
                style = AfternoteDesign.typography.h2,
                color = AfternoteDesign.colors.gray9,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.afternote_editor_category_unimplemented_description),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
                textAlign = TextAlign.Center,
            )
        }
    }
}
