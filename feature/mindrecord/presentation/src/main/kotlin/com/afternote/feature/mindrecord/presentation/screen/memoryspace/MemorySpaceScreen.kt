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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemoryDetailOverlay
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemorySpaceCardField
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemorySpaceGridBackground
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemorySpaceHeader
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem
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
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    MemorySpaceContent(
        onBackClick = onBackClick,
        modifier = modifier,
        memories = memories,
    )
}

@Composable
private fun MemorySpaceContent(
    onBackClick: () -> Unit,
    memories: List<MemoryItem>,
    modifier: Modifier = Modifier,
) {
    var selectedMemoryId: Int? by rememberSaveable { mutableStateOf(null) }
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
                .background(Color(0xFFF5F5F5)),
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
                        1,
                        "https://picsum.photos/400/600?random=1",
                        "기억 1",
                        "2024.11.11",
                        "미리보기",
                        listOf("태그"),
                    ),
                    MemoryItem(
                        2,
                        "https://picsum.photos/400/600?random=2",
                        "기억 2",
                        "2024.11.12",
                        "미리보기",
                        emptyList(),
                    ),
                    MemoryItem(
                        3,
                        "https://picsum.photos/400/600?random=3",
                        "기억 3",
                        "2024.11.13",
                        "미리보기",
                        emptyList(),
                    ),
                    MemoryItem(
                        4,
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
