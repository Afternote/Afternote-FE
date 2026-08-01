package com.afternote.feature.timeletter.presentation.screen.recipient

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.R
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeLetterDetailUiState
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeLetterDetailViewModel

@Composable
fun RecipientTimeLetterDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipientTimeLetterDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { DetailTopBar(title = "타임레터", onBackClick = onBackClick) },
    ) { innerPadding ->
        when (val state = uiState) {
            is RecipientTimeLetterDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is RecipientTimeLetterDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "타임레터를 불러올 수 없습니다.",
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray6,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.load() }) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            is RecipientTimeLetterDetailUiState.Success -> {
                RecipientTimeLetterDetailContent(
                    letter = state.letter,
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
private fun RecipientTimeLetterDetailContent(
    letter: ReceivedTimeLetter,
    contentPadding: PaddingValues,
) {
    val sendAtText = letter.sendAt?.take(10)?.replace("-", ".") ?: ""

    val heroImageUrl =
        remember(letter.blocks) {
            letter.blocks.firstOrNull { it.blockType == TimeLetterBlockType.IMAGE }?.url
        }

    LazyColumn(contentPadding = contentPadding) {
        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
            ) {
                if (heroImageUrl != null) {
                    AsyncImage(
                        model = heroImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ex_box_img),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            AfternoteDesign.colors.black.copy(alpha = 0f),
                                            AfternoteDesign.colors.black.copy(alpha = 0.6f),
                                        ),
                                ),
                            ),
                )
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                ) {
                    Text(
                        text = letter.title ?: "제목 없음",
                        style = AfternoteDesign.typography.h2,
                        color = AfternoteDesign.colors.white,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "발신인  ${letter.senderName ?: ""}",
                            style = AfternoteDesign.typography.footnoteCaption,
                            color = AfternoteDesign.colors.white,
                        )
                        Text(
                            text = "발송 예정일  $sendAtText",
                            style = AfternoteDesign.typography.footnoteCaption,
                            color = AfternoteDesign.colors.white,
                        )
                    }
                }
            }
        }

        items(letter.blocks.sortedBy { it.blockOrder }) { block ->
            RecipientTimeLetterBlockView(block = block)
        }
    }
}

@Composable
private fun RecipientTimeLetterBlockView(block: TimeLetterBlock) {
    when (block.blockType) {
        TimeLetterBlockType.TEXT -> {
            Text(
                text = block.textContent ?: "",
                style = AfternoteDesign.typography.bodySmallR,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        TimeLetterBlockType.IMAGE -> {
            AsyncImage(
                model = block.url,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        TimeLetterBlockType.AUDIO -> {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "🎵 ${block.url?.substringAfterLast('/') ?: "오디오"}",
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray6,
                )
                Text(
                    text = "재생 기능 준비 중",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray4,
                )
            }
        }

        TimeLetterBlockType.FILE -> {
            Text(
                text = "📎 ${block.url?.substringAfterLast('/') ?: "파일"}",
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        TimeLetterBlockType.LINK -> {
            Text(
                text = "🔗 ${block.url ?: ""}",
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipientTimeLetterDetailScreenPrev() {
    RecipientTimeLetterDetailContent(
        letter =
            ReceivedTimeLetter(
                id = 1L,
                timeLetterReceiverId = 1L,
                title = "채연아 20번째 생일을 축하해",
                sendAt = "2027-11-24T00:00:00",
                status = TimeLetterStatus.SENT,
                senderName = "박경민",
                deliveredAt = "2027-11-24T00:00:00",
                createdAt = "2026-01-01T00:00:00",
                isRead = false,
                blocks =
                    listOf(
                        TimeLetterBlock(
                            id = 1L,
                            blockType = TimeLetterBlockType.TEXT,
                            blockOrder = 1,
                            textContent = "생일 축하해, 앞으로도 잘 부탁해.",
                            url = null,
                            mimeType = null,
                        ),
                    ),
            ),
        contentPadding = PaddingValues(0.dp),
    )
}
