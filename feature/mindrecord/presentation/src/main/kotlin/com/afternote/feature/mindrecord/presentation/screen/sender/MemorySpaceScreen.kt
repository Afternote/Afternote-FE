package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.core.ui.R as CoreUiR

/** MEMORIES와 동일한 에셋을 쓰는 자리 표시. 추후 URL·로컬 경로 리스트로 교체 가능. */
private val placeholderMemoryPhotos =
    listOf(
        R.drawable.mindrecord_img,
        R.drawable.mindrecord_img,
        R.drawable.mindrecord_img,
        R.drawable.mindrecord_img,
    )

@Composable
fun MemorySpaceScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    DisposableEffect(context, onBackClick) {
        val activity = context as? ComponentActivity
        if (activity != null) {
            val callback =
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        onBackClick()
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
        MemorySpaceGridBackground(
            modifier = Modifier.fillMaxSize(),
        )

        var touchPosition by remember { mutableStateOf<Offset?>(null) }
        val photos = placeholderMemoryPhotos

        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { touchPosition = it },
                            onDrag = { change, _ ->
                                touchPosition = change.position
                            },
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
                MemorySpacePhotoCard(
                    imageRes = photos[0],
                    baseRotationZ = -5f,
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = 20.dp, y = (-100).dp),
                )
                MemorySpacePhotoCard(
                    imageRes = photos[1],
                    baseRotationZ = 2f,
                    shadowElevation = 10.dp,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = (-40).dp),
                )
                MemorySpacePhotoCard(
                    imageRes = photos[2],
                    baseRotationZ = -2f,
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = (-10).dp, y = (-80).dp),
                )
                MemorySpacePhotoCard(
                    imageRes = photos[3],
                    baseRotationZ = 3f,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-120).dp),
                )
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
                    RowWithBackLabel()
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
}

@Composable
private fun RowWithBackLabel() {
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

@Composable
private fun MemorySpacePhotoCard(
    imageRes: Int,
    baseRotationZ: Float,
    modifier: Modifier = Modifier,
    shadowElevation: Dp = 4.dp,
) {
    Surface(
        modifier =
            modifier
                .width(160.dp)
                .height(220.dp)
                .graphicsLayer {
                    rotationZ = baseRotationZ
                },
        shape = RoundedCornerShape(8.dp),
        color = AfternoteDesign.colors.white,
        shadowElevation = shadowElevation,
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun MemorySpaceGridBackground(modifier: Modifier = Modifier) {
    val lineColor = AfternoteDesign.colors.gray3.copy(alpha = 0.45f)
    Canvas(modifier = modifier) {
        val gridCount = 10
        val cellWidth = size.width / gridCount
        val cellHeight = size.height / gridCount
        for (i in 0..gridCount) {
            drawLine(
                color = lineColor,
                start = Offset(i * cellWidth, 0f),
                end = Offset(i * cellWidth, size.height),
                strokeWidth = 1f,
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, i * cellHeight),
                end = Offset(size.width, i * cellHeight),
                strokeWidth = 1f,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun MemorySpaceScreenPreview() {
    AfternoteTheme {
        MemorySpaceScreen(onBackClick = {})
    }
}
