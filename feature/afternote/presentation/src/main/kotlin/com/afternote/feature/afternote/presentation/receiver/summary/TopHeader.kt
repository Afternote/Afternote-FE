package com.afternote.feature.afternote.presentation.receiver.summary

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R

@Composable
fun TopHeader(modifier: Modifier = Modifier) {
    DetailTopBar(
        modifier = modifier,
        title = "AFTERNOTE",
        actions = {
            Icon(
                painter = painterResource(R.drawable.feature_afternote_ic_person),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AfternoteDesign.colors.gray9,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(R.drawable.feature_afternote_ic_settings),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AfternoteDesign.colors.gray9,
            )
        },
    )
}
