package com.afternote.feature.afternote.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.afternote.core.ui.TopBar
import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.presentation.viewmodel.AfternoteDetailUiState
import com.afternote.feature.afternote.presentation.viewmodel.AfternoteDetailViewModel

@Composable
fun AfternoteDetailScreen(
    afternoteId: Long,
    onDeleted: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AfternoteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(afternoteId) {
        viewModel.loadDetail(afternoteId)
    }

    LaunchedEffect(uiState) {
        if (uiState is AfternoteDetailUiState.Deleted) {
            onDeleted()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopBar() },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is AfternoteDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is AfternoteDetailUiState.Success -> {
                    AfternoteDetailContent(
                        detail = state.detail,
                        onDelete = { viewModel.delete(afternoteId) },
                    )
                }

                is AfternoteDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadDetail(afternoteId) }) {
                                Text("다시 시도")
                            }
                        }
                    }
                }

                AfternoteDetailUiState.Deleted -> Unit
            }
        }
    }
}

@Composable
private fun AfternoteDetailContent(
    detail: Detail,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.5.dp, vertical = 16.dp),
    ) {
        Text(
            text = detail.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = detail.timestamps.createdAt,
            fontSize = 12.sp,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        detail.processing?.let { processing ->
            if (processing.method != null) {
                AfternoteDetailRow(label = "처리 방법", value = processing.method)
            }
            if (processing.actions.isNotEmpty()) {
                AfternoteDetailRow(label = "액션", value = processing.actions.joinToString(", "))
            }
            if (processing.leaveMessage != null) {
                AfternoteDetailRow(label = "남길 메시지", value = processing.leaveMessage)
            }
        }

        detail.credentials?.let { credentials ->
            if (!credentials.id.isNullOrBlank()) {
                AfternoteDetailRow(label = "계정 아이디", value = credentials.id)
            }
            if (!credentials.password.isNullOrBlank()) {
                AfternoteDetailRow(label = "계정 비밀번호", value = credentials.password)
            }
        }

        if (detail.receivers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "수신자",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            detail.receivers.forEach { receiver ->
                Text(
                    text = "${receiver.name} (${receiver.relation})",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }

        detail.playlist?.let { playlist ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "플레이리스트",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            playlist.songs.forEach { song ->
                Text(
                    text = "${song.title} - ${song.artist}",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onDelete,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text("삭제")
            }
        }
    }
}

@Composable
private fun AfternoteDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            fontSize = 14.sp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteDetailScreenPreview() {
    AfternoteDetailScreen(afternoteId = 1L)
}
