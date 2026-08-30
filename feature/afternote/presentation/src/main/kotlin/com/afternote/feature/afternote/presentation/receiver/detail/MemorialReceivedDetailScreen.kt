package com.afternote.feature.afternote.presentation.receiver.detail

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.bottombar.BottomBar
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.MemorialContent
import com.afternote.feature.afternote.presentation.shared.detail.InfoCard
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.song.MemorialPlaylist
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel
import kotlinx.coroutines.launch

/**
 * MEMORIAL(추억 노트) 카테고리의 수신자 측 상세 화면.
 *
 * [ReceivedAfternoteDetailRoute] 의 MEMORIAL 분기에서 호출된다. 표시 데이터는
 * ReceivedAfternoteDetailSuccessMapper.kt 의 매퍼가 만든 [ReceivedMemorialDetailContent] 를
 * Route 가 풀어 파라미터로 전달한다.
 *
 * 페어 sub-screen: [com.afternote.feature.afternote.presentation.receiver.playlist.MemorialPlaylistScreen]
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    profileImageResId ?: R.drawable.receiver_img_default_profile_deceased
    val onVideoClick = rememberReceivedMemorialVideoClickHandler(snackbarHostState)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // statusBarsPadding: 엣지투엣지로 그려 콘텐츠가 상태바 아래까지 깔리므로, 상태바 높이만큼 top 패딩
            // → 탑바가 상태바(시계·배터리) 밑에서 시작(겹침 방지). 동적 인셋이라 회전·분할화면에도 대응.
            Column(modifier = Modifier.statusBarsPadding()) {
                DetailTopBar(
                    title = stringResource(R.string.afternote_receiver_detail_title, senderName),
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
                                onVideoClick = onVideoClick,
                            )
                        }
                    },
                )
            }
            item {
                Spacer(modifier = Modifier.height(70.dp))

                AfternoteButton(
                    text = stringResource(R.string.afternote_receiver_detail_confirm),
                    onClick = onNavigateToFullList,
                    type = AfternoteButtonType.Default,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * 추모 영상 카드의 클릭 처리를 만든다.
 *
 * 실행 실패는 서버 작업 실패가 아니라 이 기기의 사정(막힌 URL·재생 앱 없음)이라 재시도 팝업(#446)이 아니라
 * 스낵바로 알린다 — 작성자 쪽 상세(#1336)와 같은 채널이다. 원인이 다르면 문구도 다르므로 콜백 둘을 각각
 * 다른 리소스에 붙인다 (#1391).
 */
@Composable
private fun rememberReceivedMemorialVideoClickHandler(snackbarHostState: SnackbarHostState): (String) -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    return remember(context, resources, scope, snackbarHostState) {
        { videoUrl ->
            launchReceivedMemorialVideo(
                videoUrl = videoUrl,
                startActivity = context::startActivity,
                onRejected = {
                    val message = resources.getString(R.string.receiver_memorial_video_invalid_url)
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
                onUnavailable = {
                    val message = resources.getString(R.string.receiver_memorial_video_no_app)
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
            )
        }
    }
}

@Composable
private fun ReceiverVideoSection(
    onVideoClick: (String) -> Unit,
    memorialVideoUrl: String? = null,
    memorialThumbnailUrl: String? = null,
) {
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
                        .clickable { onVideoClick(memorialVideoUrl) },
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
                    painter = painterResource(R.drawable.receiver_ic_play_arrow),
                    // 영상이 없을 때만 그리는 플레이스홀더다. clickable 이 없어 재생 액션이 없으므로
                    // 라벨을 붙이면 없는 어포던스를 알린다. 맥락은 위 ReceiverSectionHeader 가 읽어 준다.
                    contentDescription = null,
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

/**
 * 서버가 준 추모 영상 URL을 외부 재생 앱으로 연다.
 *
 * 수신자 화면은 발신자가 저장한 값을 여는 쪽인데 서버는 비관리 URL 을 원문 그대로 반환하므로,
 * http/https 가 아닌 스킴은 실행하지 않는다 (#1394 — 발신자발 위험 스킴 차단). 불합격 URL 은
 * [onRejected] 로 알린다.
 * Android 11+ 패키지 가시성에서는 외부 앱 사전 조회가 실제 처리 가능한 앱이 있어도 실패할 수
 * 있다. 따라서 http/https URL만 선별한 뒤 실행을 직접 시도하고, OS가 명시적으로 거부한 경우에만
 * [onUnavailable] 로 폴백한다.
 *
 * 두 콜백을 나눈 이유는 **원인이 다르면 안내도 달라야 하기 때문**이다. 하나로 합치면 URL 이 막힌
 * 경우에도 «재생할 앱이 없습니다» 가 나가는데, 그건 앱 유무와 무관한 거짓이다.
 *
 * 작성자 쪽 상세의 `launchMemorialVideo`(PR #1336, develop 머지됨)와 같은 패턴이나 — 그쪽은
 * afternote 모듈 `internal` 이라 공유 없이 이식했다 — 콜백 분리는 이 판에만 있어 본문이 갈라져
 * 있다. `core:common` 승격(#1436)은 이 시그니처를 계약으로 삼고, 승격 시 작성자 판도 같이
 * 갈라야 한다.
 */
internal fun launchReceivedMemorialVideo(
    videoUrl: String,
    startActivity: (Intent) -> Unit,
    onRejected: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val uri =
        try {
            videoUrl
                .takeUnless { it.isBlank() || it.any(Char::isWhitespace) }
                ?.toUri()
                ?.takeIf {
                    val scheme = it.scheme
                    (
                        scheme.equals("http", ignoreCase = true) ||
                            scheme.equals("https", ignoreCase = true)
                    ) &&
                        !it.host.isNullOrBlank()
                }
        } catch (_: IllegalArgumentException) {
            null
        }

    if (uri == null) {
        onRejected()
        return
    }

    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        onUnavailable()
    } catch (_: SecurityException) {
        onUnavailable()
    } catch (_: IllegalArgumentException) {
        onUnavailable()
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
                                        AfternoteDesign.colors.gray6.copy(alpha = 153f / 255f),
                                        AfternoteDesign.colors.gray9.copy(alpha = 153f / 255f),
                                    ),
                            ),
                    ),
        )
        Image(
            painter = painterResource(R.drawable.feature_afternote_ic_playback),
            contentDescription = stringResource(R.string.content_description_video_play),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(32.dp),
        )
    }
}

@Composable
private fun ReceiverSectionHeader(title: String = stringResource(R.string.afternote_editor_funeral_video_label)) {
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
