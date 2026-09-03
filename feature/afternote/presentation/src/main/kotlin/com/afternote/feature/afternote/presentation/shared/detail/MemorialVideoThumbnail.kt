package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * 추모 영상 썸네일 — 썸네일 이미지 + 어두운 그라디언트 + 중앙 재생 버튼 + 좌하단 오버레이 라벨.
 *
 * 발신자 상세([com.afternote.feature.afternote.presentation.detail.MemorialDetailScreen])와
 * 수신자 상세([com.afternote.feature.afternote.presentation.receiver.detail.MemorialReceivedDetailScreen])가
 * **같은 시안 한 장**을 그리므로 한 벌을 공유한다. 두 벌로 두면 한쪽만 고쳐 놓고 다른 쪽이 남는다 (#463).
 *
 * 치수·색은 정본 시안 「NEW 추억 노트」 섹션의 애프터노트 상세페이지
 * [node 4327:72864](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72864) 실측이다:
 * - 카드 350×183, corner radius 6
 * - 이미지 fill 위 세로 그라디언트 `#000` 40% → `#000` 50%
 *   (Figma `gradientTransform [[0,1,0],[-0.5,0,0.75]]` → 위→아래 방향)
 * - 중앙 재생 버튼 48dp 원, 배경 `#000` 60%(= `iconBk` 토큰), 흰 재생 삼각형 20dp
 * - 좌하단 16dp 오프셋 라벨 「추모 영상」 14sp/20sp(= `bodySmallR`), 흰색 90%
 *
 * 시안 둘째 줄 「2분 34초」(10sp/16sp, 흰색 70%)는 **넣지 않는다** — 서버 계약
 * `AfternotePlaylist.MemorialVideo` 가 `videoUrl`·`thumbnailUrl` 두 필드뿐이라 재생 길이를 만들어
 * 낼 수 없다. 하드코딩은 없는 사실을 표시하는 것이라 계약이 생길 때까지 생략한다 (#463).
 */
@Composable
fun MemorialVideoThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(183.dp)
                .clip(RoundedCornerShape(6.dp)),
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription =
                    stringResource(R.string.afternote_content_description_memorial_video_thumbnail),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.5f),
                                    ),
                            ),
                    ),
        )

        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .background(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.afternote_ic_video_play),
                contentDescription = stringResource(R.string.afternote_content_description_video_play),
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = stringResource(R.string.afternote_memorial_video_overlay_label),
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = Color.White.copy(alpha = 0.9f),
                ),
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .testTag(MEMORIAL_VIDEO_OVERLAY_LABEL_TEST_TAG)
                    .clearAndSetSemantics {},
        )
    }
}
