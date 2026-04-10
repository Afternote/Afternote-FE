// [WIP / 소속 미확정] MemorySpace — UI 컴포넌트(Photo card).
// 경로: feature/mindrecord/presentation/.../component/memoryspace/
// 화면 전용이며 screen/memoryspace 와 같은 묶음으로 유지할 것.

package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun MemorySpacePhotoCard(
    imageRes: Int,
    baseRotationZ: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shadowElevation: Dp = 4.dp,
) {
    Surface(
        onClick = onClick,
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
