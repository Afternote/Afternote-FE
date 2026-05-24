package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.model.memoryspace.CardTransform
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem

@Composable
fun MemorySpaceCardField(
    memories: List<MemoryItem>,
    onMemoryClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { touchPosition = it },
                        onDrag = { change, _ ->
                            change.consume()
                            touchPosition = change.position
                        },
                        onDragEnd = { touchPosition = null },
                        onDragCancel = { touchPosition = null },
                    )
                },
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
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

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        cameraDistance = 32f * density
                        rotationX = tiltX
                        rotationY = tiltY
                    },
        ) {
            val transforms =
                remember {
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
                }

            memories.forEachIndexed { index, memory ->
                if (index < transforms.size) {
                    val transform = transforms[index]
                    MemorySpacePhotoCard(
                        memory = memory,
                        transform = transform,
                        onClick = { onMemoryClick(memory.id) },
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
}

@Preview(showBackground = true)
@Composable
private fun MemorySpaceCardFieldPreview() {
    AfternoteTheme {
        MemorySpaceCardField(
            memories =
                listOf(
                    MemoryItem(1, "https://picsum.photos/400/600?random=1", "기억 1", "2024.11.11", "내용 1", listOf("태그1")),
                    MemoryItem(2, "https://picsum.photos/400/600?random=2", "기억 2", "2024.11.12", "내용 2", listOf("태그2")),
                    MemoryItem(3, "https://picsum.photos/400/600?random=3", "기억 3", "2024.11.13", "내용 3", listOf("태그3")),
                    MemoryItem(4, "https://picsum.photos/400/600?random=4", "기억 4", "2024.11.14", "내용 4", listOf("태그4")),
                ),
            onMemoryClick = {},
        )
    }
}
