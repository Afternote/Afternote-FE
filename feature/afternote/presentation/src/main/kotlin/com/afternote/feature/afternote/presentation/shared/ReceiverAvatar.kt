package com.afternote.feature.afternote.presentation.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.R
import com.afternote.core.ui.R as CoreUiR

@Composable
fun ReceiverAvatar(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(CoreUiR.drawable.core_ui_ic_profile_placeholder),
        contentDescription = stringResource(R.string.afternote_content_description_recipient_profile),
        modifier =
            modifier
                .size(50.dp)
                .clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}
