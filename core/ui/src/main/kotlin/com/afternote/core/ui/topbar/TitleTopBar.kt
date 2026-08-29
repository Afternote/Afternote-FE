package com.afternote.core.ui.topbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = AfternoteDesign.typography.bodyLargeB,
            )
        },
        actions = {
            actions()
            Spacer(modifier = Modifier.width(17.dp))
        },
        modifier = modifier,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = AfternoteDesign.colors.gray1,
            ),
    )
}
