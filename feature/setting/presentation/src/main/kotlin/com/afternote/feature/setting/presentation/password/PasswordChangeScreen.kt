package com.afternote.feature.setting.presentation.password

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 설정 > 계정 > 비밀번호 변경 (#564).
 *
 * ViewModel 은 여기서 들고, 그리는 일은 [PasswordChangeContent] 가 한다.
 */
@Composable
internal fun PasswordChangeScreen(
    onBackClick: () -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordChangeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PasswordChangeContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        onChanged = onChanged,
        modifier = modifier,
    )
}
