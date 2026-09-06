package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.feature.afternote.presentation.R

/**
 * 추모 영상 상자의 시안 종횡비 350:183 (≈ 1.91:1) — 높이 기준의 정본이다 (#1780).
 *
 * **고정 높이 183dp 를 쓰지 않는 이유.** 시안이 담고 있는 정보는 「높이 183」이 아니라 「폭 350 에
 * 대한 183」이라는 **관계**다. 이 상자는 폭이 유연해서(`fillMaxWidth`) 실제 렌더 폭은 화면 폭 − 72
 * (화면 여백 40 + [InfoCard] 패딩 32)이고, 390dp 기기에서 318dp 라 이미 350 이 아니다. 폭이 350 이
 * 아닌 순간 절대값 183 은 근거를 잃고 종횡비만 흔들린다 — 320dp 기기에서 1.36:1 로, 시안 대비 −29%.
 *
 * 내용물이 [ContentScale.Crop] 이라 이 흔들림은 상자 모양에서 끝나지 않는다. 폭이 좁아질수록 가로가
 * 더 잘려 나가 **기기마다 다른 그림**이 보인다. 이미지 컨테이너에서는 상자 높이가 일정한 것보다
 * 어느 기기에서든 같은 화면이 보이는 쪽이 중요하다. 폭이 350 이면 두 방식 모두 183 으로 수렴하므로
 * 시안 재현도 잃지 않는다 — 갈리는 건 폭이 350 이 아닐 때 무엇을 지키느냐뿐이다.
 *
 * **#1735(PR #1610)가 고정 183 을 택하며 든 근거 하나는 성립하지 않는다.** 「비율로 바꿔도 카드
 * 패딩 32 때문에 시안이 재현되지 않는다」는 근거인데, 패딩 32 는 고정 높이 쪽에서도 똑같이 빠지므로
 * 두 방식을 가르지 못한다. 게다가 폭이 유연한 컴포넌트에서 「시안 재현」은 절대 픽셀이 아니라 비율
 * 보존을 뜻한다 — 패딩이 폭을 줄이는 만큼 어긋나는 쪽은 오히려 고정 높이다.
 *
 * **넓은 화면(600dp+) 상한은 두지 않는다.** 앱은 `screenOrientation="portrait"` 고정이고, 폴더블
 * 펼침·태블릿에서 폭이 벌어질 때 벌어지는 건 이 상자만이 아니라 화면 여백·카드·본문 전체다. 이
 * 상자에만 상한을 걸면 시안에 없는 숫자를 하나 더 만들면서 주변과 어긋난다. 상한이 필요해지는
 * 시점은 화면 전체의 최대 폭을 정할 때다.
 */
private const val MEMORIAL_VIDEO_ASPECT_RATIO = 350f / 183f

/** 추모 영상 상자의 모서리 반경 — 시안 [node 4327:72864](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72864) 실측 6dp. */
private val MemorialVideoCornerRadius = 6.dp

/**
 * 추모 영상 썸네일 — 썸네일 이미지 + 어두운 그라디언트 + 중앙 재생 버튼.
 *
 * 발신자 상세([com.afternote.feature.afternote.presentation.detail.MemorialDetailScreen])와
 * 수신자 상세([com.afternote.feature.afternote.presentation.receiver.detail.MemorialReceivedDetailScreen])가
 * **같은 시안 한 장**을 그리므로 한 벌을 공유한다. 두 벌로 두면 한쪽만 고쳐 놓고 다른 쪽이 남는다 (#463).
 *
 * 치수·색은 정본 시안 「NEW 추억 노트」 섹션의 애프터노트 상세페이지
 * [node 4327:72864](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72864) 실측이다:
 * - 카드 350×183, corner radius 6 — 높이는 고정 183dp 가 아니라 [MEMORIAL_VIDEO_ASPECT_RATIO] 로 둔다 (#1780)
 * - 이미지 fill 위 세로 그라디언트 `#000` 40% → `#000` 50%
 *   (Figma `gradientTransform [[0,1,0],[-0.5,0,0.75]]` → 위→아래 방향)
 * - 중앙 재생 버튼 48dp 원, 배경 `#000` 60%(= `iconBk` 토큰), 흰 재생 삼각형 20dp
 *
 * 시안 좌하단 라벨 「추모 영상」(14sp/20sp, 흰색 90%)은 **넣지 않는다** — 바로 위 8dp 간격에 섹션 헤더
 * 「장례식에 남길 영상」(`afternote_editor_funeral_video_label`)이 같은 정보를 이미 말하고, 라벨은
 * `clearAndSetSemantics {}` 로 접근성 트리에서도 빠져 있어 남는 값이 배지 하나의 시각 효과뿐이었다.
 * 그 대가로 헤더와 문구가 갈리는 상태를 떠안는 쪽을 접었다. 정본 시안을 거스르는 FE 판단이다 (#1779).
 *
 * 시안 둘째 줄 「2분 34초」(10sp/16sp, 흰색 70%)도 **넣지 않는다** — 서버 계약
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
                .aspectRatio(MEMORIAL_VIDEO_ASPECT_RATIO)
                .clip(RoundedCornerShape(MemorialVideoCornerRadius)),
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
    }
}
