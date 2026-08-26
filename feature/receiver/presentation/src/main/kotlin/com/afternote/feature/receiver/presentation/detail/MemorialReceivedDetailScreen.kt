package com.afternote.feature.receiver.presentation.detail

import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.bottombar.BottomBar
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.MemorialContent
import com.afternote.feature.afternote.presentation.shared.detail.InfoCard
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.song.MemorialPlaylist
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel

/**
 * MEMORIAL(추억 노트) 카테고리의 수신자 측 상세 화면.
 *
 * [ReceivedAfternoteDetailRoute] 의 MEMORIAL 분기에서 호출된다. 표시 데이터는
 * ReceivedAfternoteDetailSuccessMapper.kt 의 매퍼가 만든 [ReceivedMemorialDetailContent] 를
 * Route 가 풀어 파라미터로 전달한다.
 *
 * 페어 sub-screen: [com.afternote.feature.receiver.presentation.playlist.MemorialPlaylistScreen]
 * (추억 플레이리스트 카드 클릭 → 전체보기 진입).
 */
@Composable
fun MemorialReceivedDetailScreen(
    senderName: String,
    onNavigateToFullList: () -> Unit,
    onNavigateToPlaylist: () -> Unit,
    onBackClick: () -> Unit,
    messageBlocks: List<MessageBlockUiModel> = emptyList(),
    profileImageResId: Int? = null,
    albumCovers: List<AlbumCover>,
    songCount: Int = 16,
    memorialVideoUrl: String? = null,
    memorialThumbnailUrl: String? = null,
) {
    profileImageResId ?: R.drawable.feature_afternote_img_default_profile_deceased

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // statusBarsPadding: 엣지투엣지로 그려 콘텐츠가 상태바 아래까지 깔리므로, 상태바 높이만큼 top 패딩
            // → 탑바가 상태바(시계·배터리) 밑에서 시작(겹침 방지). 동적 인셋이라 회전·분할화면에도 대응.
            Column(modifier = Modifier.statusBarsPadding()) {
                DetailTopBar(
                    title = "故 ${senderName}님의 애프터노트",
                    onBackClick = { onBackClick() },
                )
            }
        },
        bottomBar = {
            // 시안: 상세에도 하단 바 노출 + 애프터노트(NOTE) 탭 선택. 수신자 흐름이라 탭 이동은 미정 — 시각만 (#274).
            BottomBar(
                selectedNavTab = BottomNavTab.NOTE,
                onTabClick = {},
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(20.dp)
                    .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 20.dp),
        ) {
            item {
                MemorialContent(
                    // 시안: 상세 타이틀(TopBar) 아래 바로 프로필 — 별도 안내 문구 없음 (#274).
                    introContent = {},
                    photoContent = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ProfileImage()
                        }
                    },
                    playlistContent = {
                        MemorialPlaylist(
                            label = "추억 플레이리스트",
                            songCount = songCount,
                            albumCovers = albumCovers,
                            onCardClick = onNavigateToPlaylist,
                        )
                    },
                    messageContent = {
                        // 시안: "남기신 말씀" 섹션 — 형제 수신자 상세와 동일하게 공용 MessageSection(💬 헤더 + 인용 카드) 사용 (#274).
                        MessageSection(blocks = messageBlocks)
                    },
                    sectionSpacing = 32.dp,
                    videoContent = {
                        // 시안: 수신자 화면은 영상이 있을 때만 노출 (없으면 섹션 자체를 숨김, #274).
                        if (!memorialVideoUrl.isNullOrBlank()) {
                            ReceiverVideoSection(
                                memorialVideoUrl = memorialVideoUrl,
                                memorialThumbnailUrl = memorialThumbnailUrl,
                            )
                        }
                    },
                )
            }
            item {
                Spacer(modifier = Modifier.height(70.dp))

                AfternoteButton(
                    text = "애프터노트 확인하기",
                    onClick = onNavigateToFullList,
                    type = AfternoteButtonType.Default,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private const val LABEL_VIDEO_SECTION = "장례식에 남길 영상"

@Composable
private fun ReceiverVideoSection(
    memorialVideoUrl: String? = null,
    memorialThumbnailUrl: String? = null,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        ReceiverSectionHeader()
        Spacer(modifier = Modifier.height(12.dp))
        if (!memorialVideoUrl.isNullOrBlank()) {
            InfoCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // clip 을 clickable 앞에: 눌림 피드백이 InfoCard 의 12dp 둥근 모서리 안에서만 그려지게 (모서리 밖 사각 번짐 방지)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, memorialVideoUrl.toUri())
                            if (context.packageManager.resolveActivity(
                                    intent,
                                    PackageManager.MATCH_DEFAULT_ONLY,
                                ) != null
                            ) {
                                context.startActivity(intent)
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        "영상을 재생할 수 있는 앱이 없습니다.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        },
            ) {
                ReceiverMemorialVideoThumbnail(thumbnailUrl = memorialThumbnailUrl)
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AfternoteDesign.colors.gray3),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.feature_afternote_ic_play_arrow),
                    contentDescription = "Play",
                    tint = AfternoteDesign.colors.white,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(
                                AfternoteDesign.colors.black.copy(alpha = 0.3f),
                                CircleShape,
                            ).padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun ReceiverMemorialVideoThumbnail(thumbnailUrl: String?) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(183.dp)
                .clip(RoundedCornerShape(16.dp)),
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "장례식에 남길 영상 썸네일",
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
                                        AfternoteDesign.colors.gray6.copy(alpha = 153f / 255f),
                                        AfternoteDesign.colors.gray9.copy(alpha = 153f / 255f),
                                    ),
                            ),
                    ),
        )
        Image(
            painter = painterResource(R.drawable.feature_afternote_ic_playback),
            contentDescription = "영상 재생",
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(32.dp),
        )
    }
}

@Composable
private fun ReceiverSectionHeader(title: String = LABEL_VIDEO_SECTION) {
    Text(
        text = title,
        style =
            AfternoteDesign.typography.textField.copy(
                fontWeight = FontWeight.Medium,
                color = AfternoteDesign.colors.gray9,
            ),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Preview(showBackground = true, name = "No video")
@Composable
private fun PreviewMemorialReceivedDetail() {
    AfternoteTheme {
        MemorialReceivedDetailScreen(
            senderName = "박서연",
            onNavigateToFullList = {},
            onNavigateToPlaylist = {},
            onBackClick = {},
            messageBlocks =
                listOf(
                    MessageBlockUiModel(
                        body = "이 계정에는 우리 가족 여행 사진이 많아. 계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
                    ),
                ),
            // 프리뷰 대표 데이터: 실앱은 곡마다 coverUrl → 커버 로드. 프리뷰/스크린샷은 네트워크 미지원이라 회색 박스로 표시.
            albumCovers =
                listOf(
                    AlbumCover(),
                    AlbumCover(),
                    AlbumCover(),
                ),
            songCount = 3,
        )
    }
}

@Preview(showBackground = true, name = "With video")
@Composable
private fun PreviewMemorialReceivedDetailWithVideo() {
    AfternoteTheme {
        MemorialReceivedDetailScreen(
            senderName = "박서연",
            onNavigateToFullList = {},
            onNavigateToPlaylist = {},
            onBackClick = {},
            messageBlocks =
                listOf(
                    MessageBlockUiModel(
                        body = "이 계정에는 우리 가족 여행 사진이 많아. 계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
                    ),
                ),
            albumCovers =
                listOf(
                    AlbumCover(),
                    AlbumCover(),
                    AlbumCover(),
                ),
            songCount = 3,
            // 영상 섹션은 URL 있을 때만 노출 — 조건부 분기 상태 확인용 프리뷰 (썸네일은 네트워크 미지원이라 회색).
            memorialVideoUrl = "https://example.com/memorial.mp4",
        )
    }
}
