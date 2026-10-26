package com.afternote.feature.setting.presentation.shared.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.button.PlusBadgeButton

/**
 * 수신자 등록·수정의 표시 전용 아바타. 사진 선택 계약이 없는 화면이므로
 * 클릭을 요구하는 ProfileImagePicker 대신 표시 컴포넌트와 장식 배지를 조합한다.
 */
@Composable
internal fun ProfilePhotoWithAddBadge(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(134.dp)) {
        ProfileImage()
        PlusBadgeButton(
            contentDescription = null,
            onClick = null,
            paddingValues = PaddingValues(17.dp),
            modifier = Modifier.align(Alignment.BottomEnd),
            size = 48.dp,
        )
    }
}
