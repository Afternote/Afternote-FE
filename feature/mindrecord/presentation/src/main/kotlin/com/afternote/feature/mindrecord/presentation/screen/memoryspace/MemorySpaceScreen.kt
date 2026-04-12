package com.afternote.feature.mindrecord.presentation.screen.memoryspace

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemoryDetailOverlay
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemorySpaceGridBackground
import com.afternote.feature.mindrecord.presentation.component.memoryspace.MemorySpacePhotoCard
import com.afternote.feature.mindrecord.presentation.model.memoryspace.CardTransform
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem
import com.afternote.feature.mindrecord.presentation.viewmodel.MemorySpaceViewModel
import com.afternote.core.ui.R as CoreUiR

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
    val context = LocalContext.current
    var selectedMemory by remember { mutableStateOf<MemoryItem?>(null) }

    // 카드 선택 시 뒷배경을 뿌옇게 만드는 블러 강도. 선택 해제 시 0dp로 복귀.
    val backgroundBlurAmount by animateDpAsState(
        targetValue = if (selectedMemory != null) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "backgroundBlur",
    )

    DisposableEffect(context, onBackClick, selectedMemory) {
        val activity = context as? ComponentActivity
        if (activity != null) {
            val callback =
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (selectedMemory != null) {
                            selectedMemory = null
                        } else {
                            onBackClick()
                        }
                    }
                }
            activity.onBackPressedDispatcher.addCallback(callback)
            onDispose { callback.remove() }
        } else {
            onDispose { }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
    ) {
        // 그리드 배경 + 카드 + 헤더를 감싸는 블러 대상 컨테이너. 오버레이는 이 바깥에 둬야 블러가 안 먹힌다.
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

            var touchPosition by remember { mutableStateOf<Offset?>(null) }

            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { touchPosition = it },
                                onDrag = { change, _ -> touchPosition = change.position },
                                onDragEnd = { touchPosition = null },
                                onDragCancel = { touchPosition = null },
                            )
                        },
            ) {
                val screenWidth = constraints.maxWidth.toFloat()
                val screenHeight = constraints.maxHeight.toFloat()
                // 전체 뷰를 돌릴 때는 각도를 조금 줄여야 화면 밖으로 크게 벗어나지 않음
                val maxTiltAngle = 20f

                val targetTiltX =
                    touchPosition?.let {
                        ((screenHeight / 2) - it.y) / (screenHeight / 2) * maxTiltAngle
                    } ?: 0f
                val targetTiltY =
                    touchPosition?.let {
                        (it.x - (screenWidth / 2)) / (screenWidth / 2) * maxTiltAngle
                    } ?: 0f

                val tiltX by animateFloatAsState(
                    targetValue = targetTiltX,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                    label = "tiltX",
                )
                val tiltY by animateFloatAsState(
                    targetValue = targetTiltY,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                    label = "tiltY",
                )

                // 개별 카드가 아닌, 카드들을 담은 부모 Box 자체를 회전시켜 화면 중앙을 축으로 공간감을 만든다.
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // 카메라 거리를 넉넉히 줘서 외곽 카드의 심한 왜곡 방지
                                cameraDistance = 32f * density
                                rotationX = tiltX
                                rotationY = tiltY
                            },
                ) {
                    val transforms =
                        listOf(
                            CardTransform(
                                offsetX = (-80).dp,
                                offsetY = (-150).dp,
                                rotationX = 15f,
                                rotationY = 25f,
                                rotationZ = -10f,
                                zIndex = 1f,
                            ),
                            CardTransform(
                                offsetX = 40.dp,
                                offsetY = (-200).dp,
                                rotationX = -20f,
                                rotationY = -15f,
                                rotationZ = 5f,
                                zIndex = 0f,
                            ),
                            CardTransform(
                                offsetX = (-30).dp,
                                offsetY = 20.dp,
                                rotationX = -10f,
                                rotationY = 40f,
                                rotationZ = -5f,
                                zIndex = 2f,
                            ),
                            CardTransform(
                                offsetX = 90.dp,
                                offsetY = 60.dp,
                                rotationX = 30f,
                                rotationY = -25f,
                                rotationZ = 12f,
                                zIndex = 3f,
                            ),
                        )

                    memories.forEachIndexed { index, memory ->
                        if (index < transforms.size) {
                            val transform = transforms[index]
                            MemorySpacePhotoCard(
                                memory = memory,
                                transform = transform,
                                onClick = { selectedMemory = memory },
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .offset(x = transform.offsetX, y = transform.offsetY)
                                        .zIndex(transform.zIndex),
                            )
                        }
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                ) {
                    Surface(
                        onClick = onBackClick,
                        shape = CircleShape,
                        color = AfternoteDesign.colors.white,
                        shadowElevation = 2.dp,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(CoreUiR.drawable.core_ui_arrow_left),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = AfternoteDesign.colors.gray9,
                            )
                            Text(
                                text = stringResource(R.string.mindrecord_memory_space_back),
                                style = AfternoteDesign.typography.bodySmallR,
                                color = AfternoteDesign.colors.gray7,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.mindrecord_memory_space_title),
                    style = AfternoteDesign.typography.mono,
                    color = AfternoteDesign.colors.gray6,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.mindrecord_memory_space_subtitle),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                )
            }
        }

        // 블러 컨테이너 바깥에 위치해야 오버레이 자체는 선명하게 보인다.
        AnimatedVisibility(
            visible = selectedMemory != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            selectedMemory?.let { memory ->
                MemoryDetailOverlay(
                    memory = memory,
                    onClose = { selectedMemory = null },
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
                    MemoryItem(1, "https://picsum.photos/400/600?random=1", "기억 1", "2024.11.11", "미리보기", listOf("태그")),
                    MemoryItem(2, "https://picsum.photos/400/600?random=2", "기억 2", "2024.11.12", "미리보기", emptyList()),
                    MemoryItem(3, "https://picsum.photos/400/600?random=3", "기억 3", "2024.11.13", "미리보기", emptyList()),
                    MemoryItem(4, "https://picsum.photos/400/600?random=4", "기억 4", "2024.11.14", "미리보기", emptyList()),
                ),
        )
    }
}
