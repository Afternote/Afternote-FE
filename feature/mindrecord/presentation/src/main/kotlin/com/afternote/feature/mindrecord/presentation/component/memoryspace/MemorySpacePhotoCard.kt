package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.modifierextention.shimmerLoadingPlaceholder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.model.memoryspace.CardTransform
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem
import com.afternote.core.ui.R as CoreUiR

@Composable
fun MemorySpacePhotoCard(
    memory: MemoryItem,
    transform: CardTransform,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shadowElevation: Dp = 4.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val contentAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 1f,
        label = "contentAlpha",
    )

    val imageZoomScale by animateFloatAsState(
        targetValue = if (isPressed) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "imageZoom",
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        interactionSource = interactionSource,
        modifier =
            modifier
                .width(160.dp)
                .height(220.dp)
                .graphicsLayer {
                    cameraDistance = 16f * density
                    rotationX = transform.rotationX
                    rotationY = transform.rotationY
                    rotationZ = transform.rotationZ
                },
        shape = RoundedCornerShape(8.dp),
        color = AfternoteDesign.colors.gray4.copy(alpha = contentAlpha),
        shadowElevation = shadowElevation,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            var imageReady by remember(memory.imageUrl) { mutableStateOf(false) }

            AsyncImage(
                model = memory.imageUrl,
                contentDescription = memory.title,
                contentScale = ContentScale.Crop,
                onLoading = { imageReady = false },
                onSuccess = { imageReady = true },
                onError = { imageReady = true },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = imageZoomScale
                            scaleY = imageZoomScale
                            alpha = contentAlpha
                        }.then(
                            if (!imageReady) {
                                Modifier.shimmerLoadingPlaceholder()
                            } else {
                                Modifier
                            },
                        ),
            )

            PressedOverlay(
                isVisible = isPressed,
                date = memory.date,
                title = memory.title,
            )
        }
    }
}

@Composable
private fun PressedOverlay(
    isVisible: Boolean,
    date: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(AfternoteDesign.colors.black.copy(alpha = 0.15f)),
        ) {
            Surface(
                shape = CircleShape,
                color = AfternoteDesign.colors.white.copy(alpha = 0.9f),
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp),
            ) {
                Icon(
                    painter = painterResource(id = CoreUiR.drawable.core_ui_right_arrow),
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp),
                    tint = AfternoteDesign.colors.gray8,
                )
            }

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(AfternoteDesign.colors.white.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = date,
                    style = AfternoteDesign.typography.bodySmallB,
                    color = AfternoteDesign.colors.gray8,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorySpacePhotoCardPreview() {
    AfternoteTheme {
        MemorySpacePhotoCard(
            memory = MemoryItem(1, "https://picsum.photos/400/600?random=1", "기억 1", "2024.11.11", "미리보기", listOf("태그")),
            transform =
                CardTransform(
                    offsetX = 0.dp,
                    offsetY = 0.dp,
                    rotationX = 15f,
                    rotationY = 25f,
                    rotationZ = -10f,
                ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
