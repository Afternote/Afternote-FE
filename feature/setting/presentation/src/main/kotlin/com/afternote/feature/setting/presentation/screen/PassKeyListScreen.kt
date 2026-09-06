package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.PasskeyListItem

@Composable
internal fun PassKeyListScreen(
    passkeys: List<Passkey>,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isLoading && errorMessage == null && passkeys.isEmpty()) {
        PassKeyScreen(onBackClick = onBackClick, onRegisterClick = onRegisterClick, modifier = modifier)
        return
    }
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(id = R.string.passkey_management_title),
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        when {
            isLoading -> {
                LoadingBody(modifier = Modifier.padding(innerPadding))
            }

            errorMessage != null -> {
                PassKeyListErrorState(
                    message = errorMessage,
                    onRetryClick = onRetryClick,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp),
                )
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.passkey_section_title),
                            style = AfternoteDesign.typography.bodyLargeB,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.passkey_description),
                            style = AfternoteDesign.typography.bodySmallR,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(id = R.string.passkey_list_header),
                            style = AfternoteDesign.typography.bodyLargeB,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(passkeys, key = { it.id }) { passkey ->
                        PasskeyListItem(passkey = passkey)
                    }
                }
            }
        }
    }
}

@Composable
private fun PassKeyListErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = AfternoteDesign.typography.bodySmallR,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteButton(
                text = stringResource(id = R.string.passkey_list_retry),
                onClick = onRetryClick,
                type = AfternoteButtonType.Default,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PassKeyListScreenPrev() {
    PassKeyListScreen(
        passkeys =
            listOf(
                Passkey(id = 1L, displayName = "아이폰 15 Pro", createdAt = "2026-07-28T10:15:30"),
                Passkey(id = 2L, displayName = "갤럭시 S24", createdAt = "2026-08-01T09:00:00"),
            ),
        isLoading = false,
        errorMessage = null,
        onBackClick = {},
        onRegisterClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PassKeyListScreenEmptyPrev() {
    PassKeyListScreen(
        passkeys = emptyList(),
        isLoading = false,
        errorMessage = null,
        onBackClick = {},
        onRegisterClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PassKeyListScreenLoadingPrev() {
    PassKeyListScreen(
        passkeys = emptyList(),
        isLoading = true,
        errorMessage = null,
        onBackClick = {},
        onRegisterClick = {},
        onRetryClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PassKeyListScreenErrorPrev() {
    PassKeyListScreen(
        passkeys = emptyList(),
        isLoading = false,
        errorMessage = "패스키 목록을 불러올 수 없습니다.",
        onBackClick = {},
        onRegisterClick = {},
        onRetryClick = {},
    )
}
