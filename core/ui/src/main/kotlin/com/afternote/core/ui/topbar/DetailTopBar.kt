package com.afternote.core.ui.topbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    // TopAppBar -> CenterAlignedTopAppBar 로 변경
    CenterAlignedTopAppBar(
        title = {
            // Row로 감싸서 강제 정렬할 필요가 없어집니다.
            Text(
                text = title,
                style = AfternoteDesign.typography.bodyLargeB,
                // CenterAlignedTopAppBar가 알아서 정중앙에 꽂아줍니다.
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.core_ui_arrow_left),
                        contentDescription = stringResource(R.string.core_ui_content_description_back),
                    )
                }
            }
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
