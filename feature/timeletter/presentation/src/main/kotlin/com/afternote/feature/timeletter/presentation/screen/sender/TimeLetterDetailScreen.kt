package com.afternote.feature.timeletter.presentation.screen.sender

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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.component.TimeLetterHeroEmptyCaption
import com.afternote.feature.timeletter.presentation.component.TimeLetterHeroEmptyTitle
import com.afternote.feature.timeletter.presentation.component.TimeLetterHeroGradientEnd
import com.afternote.feature.timeletter.presentation.component.TimeLetterHeroGradientStart
import com.afternote.feature.timeletter.presentation.component.TimeLetterHeroImageCaption
import com.afternote.feature.timeletter.presentation.component.TimeLetterHeroImageOverlay
import com.afternote.feature.timeletter.presentation.component.TimeLetterHeroImageTitle
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterDetailUiState
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterDetailViewModel

@Composable
fun TimeLetterDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimeLetterDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { DetailTopBar(title = "타임레터", onBackClick = onBackClick) },
    ) { innerPadding ->
        when (val state = uiState) {
            is TimeLetterDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is TimeLetterDetailUiState.Error -> {
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

            is TimeLetterDetailUiState.Success -> {
                TimeLetterDetailContent(
                    letter = state.letter,
                    receiverNameMap = state.receiverNameMap,
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
private fun TimeLetterDetailContent(
    letter: TimeLetter,
    receiverNameMap: Map<Long, String>,
    contentPadding: PaddingValues,
) {
    val receiverText =
        letter.receiverIds
            .mapNotNull { receiverNameMap[it] }
            .joinToString(", ")
            .ifEmpty { "${letter.receiverIds.size}명" }
    val sendAtText =
        letter.sendAt
            ?.takeIf { it.isNotBlank() }
            ?.let { "${it.take(10).replace("-", ".")}." }
            .orEmpty()

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
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to TimeLetterHeroImageOverlay.copy(alpha = 0f),
                                        0.5f to TimeLetterHeroImageOverlay.copy(alpha = 0f),
                                        1f to TimeLetterHeroImageOverlay.copy(alpha = 0.4f),
                                    ),
                                ),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .drawWithCache {
                                    val brush =
                                        Brush.radialGradient(
                                            colors = listOf(TimeLetterHeroGradientStart, TimeLetterHeroGradientEnd),
                                            center = Offset(size.width / 2f, size.height),
                                            radius = size.width,
                                        )
                                    onDrawBehind { drawRect(brush) }
                                },
                    )
                }
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                ) {
                    Text(
                        text = letter.title ?: "제목 없음",
                        style = AfternoteDesign.typography.h2,
                        color =
                            if (heroImageUrl != null) {
                                TimeLetterHeroImageTitle
                            } else {
                                TimeLetterHeroEmptyTitle
                            },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "수신인  $receiverText",
                            style = AfternoteDesign.typography.footnoteCaption,
                            color =
                                if (heroImageUrl != null) {
                                    TimeLetterHeroImageCaption
                                } else {
                                    TimeLetterHeroEmptyCaption
                                },
                        )
                        Text(
                            text = "발송 예정일  $sendAtText",
                            style = AfternoteDesign.typography.footnoteCaption,
                            color =
                                if (heroImageUrl != null) {
                                    TimeLetterHeroImageCaption
                                } else {
                                    TimeLetterHeroEmptyCaption
                                },
                        )
                    }
                }
            }
        }

        items(letter.blocks.sortedBy { it.blockOrder }) { block ->
            TimeLetterBlockView(block = block)
        }
    }
}

@Composable
private fun TimeLetterBlockView(block: TimeLetterBlock) {
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
            // TODO: 재생 UI 구현 예정
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

@Preview
@Composable
private fun TimeLetterDetailScreenPrev() {
    TimeLetterDetailContent(
        letter =
            TimeLetter(
                id = 1L,
                title = "미래의 나에게",
                sendAt = "2026-12-31T00:00:00",
                status = TimeLetterStatus.SCHEDULED,
                blocks =
                    listOf(
                        TimeLetterBlock(
                            id = 1L,
                            blockType = TimeLetterBlockType.TEXT,
                            blockOrder = 1,
                            textContent = "10년 뒤에도 잘 지내고 있길 바라.",
                            url = null,
                            mimeType = null,
                        ),
                    ),
                receiverIds = listOf(1L),
            ),
        receiverNameMap = mapOf(1L to "박경민"),
        contentPadding = PaddingValues(0.dp),
    )
}
