package com.afternote.feature.afternote.presentation.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 애프터노트 피처 공통 TopAppBar.
 *
 * 필요한 파라미터만 넘기면 모든 형태의 탑바를 커버합니다.
 * - 타이틀만: `AfternoteTopBar(title = "제목")`
 * - 뒤로가기: `AfternoteTopBar(onBackClick = { ... })`
 * - 뒤로가기 + 액션: `AfternoteTopBar(onBackClick = { ... }, actionIcon = ..., onActionClick = { ... })`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfternoteTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBackClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (title != null) {
                Text(title)
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로 가기",
                    )
                }
            }
        },
        actions = {
            if (actionIcon != null && onActionClick != null) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionContentDescription,
                    )
                }
            }
        },
    )
}
