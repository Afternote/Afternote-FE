package com.afternote.feature.mindrecord.presentation.screen.memoryspace

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemoryDetailOverlay
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemorySpaceCardField
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemorySpaceGridBackground
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemorySpaceHeader
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem
import com.afternote.feature.mindrecord.presentation.viewmodel.MemorySpaceUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.MemorySpaceViewModel

/**
 * 마인드레코드 피처의 기억 공간(MEMORIES). [com.afternote.core.ui.Route.MemorySpace]로 앱 셸에서 직접 진입한다.
 *
 * [MemorySpaceViewModel]에서 목록을 구독하고, UI는 [MemorySpaceContent]에만 상태를 넘긴다.
 */
@Composable
fun MemorySpaceScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemorySpaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MemorySpaceContent(
        onBackClick = onBackClick,
        modifier = modifier,
        memories = (uiState as? MemorySpaceUiState.Success)?.memories.orEmpty(),
        statusText = uiState.statusText(),
        // 전체 조회 실패만 재시도로 풀린다 — 로딩·0건은 다시 눌러 봐야 결과가 같다.
        onRetryClick = viewModel::retry.takeIf { uiState is MemorySpaceUiState.Error },
    )
}

/**
 * 카드가 한 장도 없을 때 격자 한가운데에 띄울 문구. 카드가 있으면 null.
 *
 * 로딩·에러·0건이 모두 "텅 빈 격자" 하나로 수렴하던 자리라, 셋을 문구로 갈라 준다.
 */
@Composable
private fun MemorySpaceUiState.statusText(): String? =
    when (this) {
        MemorySpaceUiState.Loading -> {
            stringResource(R.string.mindrecord_memory_space_loading)
        }

        is MemorySpaceUiState.Error -> {
            stringResource(messageRes)
        }

        is MemorySpaceUiState.Success -> {
            stringResource(R.string.mindrecord_memory_space_empty).takeIf { memories.isEmpty() }
        }
    }

/** 상태 없는 본문. screenshotTest 가 이 자리에서 좁은 화면 회귀를 잡는다 (#1131). */
@Composable
internal fun MemorySpaceContent(
    onBackClick: () -> Unit,
    memories: List<MemoryItem>,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    onRetryClick: (() -> Unit)? = null,
) {
    var selectedMemoryId: Long? by rememberSaveable { mutableStateOf(null) }
    val selectedMemory = selectedMemoryId?.let { id -> memories.firstOrNull { it.id == id } }

    LaunchedEffect(memories, selectedMemoryId) {
        val id = selectedMemoryId ?: return@LaunchedEffect
        if (memories.none { it.id == id }) {
            selectedMemoryId = null
        }
    }

    BackHandler {
        if (selectedMemoryId != null) {
            selectedMemoryId = null
        } else {
            onBackClick()
        }
    }

    val backgroundBlurAmount by animateDpAsState(
        targetValue = if (selectedMemory != null) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "backgroundBlur",
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(AfternoteDesign.colors.gray1),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (backgroundBlurAmount > 0.dp) {
                            Modifier.blur(
                                radius = backgroundBlurAmount,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded,
                            )
                        } else {
                            Modifier
                        },
                    ),
        ) {
            MemorySpaceGridBackground(modifier = Modifier.fillMaxSize())

            MemorySpaceCardField(
                memories = memories,
                onMemoryClick = { selectedMemoryId = it },
            )

            if (statusText != null) {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = statusText,
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray6,
                        textAlign = TextAlign.Center,
                    )
                    if (onRetryClick != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onRetryClick) {
                            Text(
                                text = stringResource(R.string.mindrecord_memory_space_retry),
                                style = AfternoteDesign.typography.captionLargeB,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    }
                }
            }

            MemorySpaceHeader(onBackClick = onBackClick)
        }

        AnimatedVisibility(
            visible = selectedMemory != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            selectedMemory?.let { memory ->
                MemoryDetailOverlay(
                    memory = memory,
                    onClose = { selectedMemoryId = null },
                    modifier = Modifier.background(AfternoteDesign.colors.black.copy(alpha = 0.4f)),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun MemorySpaceScreenPreview() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories =
                listOf(
                    MemoryItem(
                        1L,
                        "https://picsum.photos/400/600?random=1",
                        "기억 1",
                        "2024.11.11",
                        "미리보기",
                        listOf("태그"),
                    ),
                    MemoryItem(
                        2L,
                        "https://picsum.photos/400/600?random=2",
                        "기억 2",
                        "2024.11.12",
                        "미리보기",
                        emptyList(),
                    ),
                    MemoryItem(
                        3L,
                        "https://picsum.photos/400/600?random=3",
                        "기억 3",
                        "2024.11.13",
                        "미리보기",
                        emptyList(),
                    ),
                    MemoryItem(
                        4L,
                        "https://picsum.photos/400/600?random=4",
                        "기억 4",
                        "2024.11.14",
                        "미리보기",
                        emptyList(),
                    ),
                ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun MemorySpaceScreenEmptyPreview() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = emptyList(),
            statusText = "아직 담긴 기록이 없어요.\n일기나 데일리 질문에 답하면 이곳에 쌓입니다.",
        )
    }
}

/** 조회 실패 — 0건과 달리 재시도 버튼이 함께 그려진다. */
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun MemorySpaceScreenErrorPreview() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = emptyList(),
            statusText = "기록을 불러오지 못했습니다.",
            onRetryClick = {},
        )
    }
}
