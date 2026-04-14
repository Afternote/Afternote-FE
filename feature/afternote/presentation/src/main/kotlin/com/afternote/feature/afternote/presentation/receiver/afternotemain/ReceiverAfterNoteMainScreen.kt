package com.afternote.feature.afternote.presentation.receiver.afternotemain

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.bottombar.BottomBar
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.LastWishesRadioGroup
import com.afternote.feature.afternote.presentation.shared.MemorialGuidelineContent
import com.afternote.feature.afternote.presentation.shared.detail.components.InfoCard
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.detail.song.MemorialPlaylist

@Composable
fun ReceiverAfterNoteMainScreen(
    senderName: String,
    onNavigateToFullList: () -> Unit = {},
    onNavigateToPlaylist: () -> Unit = {},
    onBackClick: () -> Unit = {},
    profileImageResId: Int? = null,
    albumCovers: List<AlbumCover>,
    songCount: Int = 16,
    memorialVideoUrl: String? = null,
    memorialThumbnailUrl: String? = null,
    showBottomBar: Boolean = true,
) {
    var selectedBottomNavItem by remember { mutableStateOf(BottomNavTab.TIMELETTER) }
    val profileResId =
        profileImageResId ?: R.drawable.feature_afternote_img_default_profile_deceased

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                DetailTopBar(
                    title = "故${senderName}님의 애프터노트",
                    onBackClick = { onBackClick() },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    selectedNavTab = selectedBottomNavItem,
                    onTabClick = { selectedBottomNavItem = it },
                )
            }
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
                MemorialGuidelineContent(
                    introContent = {
                        Text(
                            text = "故 ${senderName}님의 애프터노트입니다.",
                            style =
                                AfternoteDesign.typography.textField.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = AfternoteDesign.colors.gray9,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    photoContent = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ProfileImage(
                                onClick = {},
                                isEditable = false,
                            )
                        }
                    },
                    playlistContent = {
                        MemorialPlaylist(
                            label = "추모 플레이리스트",
                            songCount = songCount,
                            albumCovers = albumCovers,
                            onAddSongClick = null,
                            onPlaylistClick = onNavigateToPlaylist,
                        )
                    },
                    lastWishContent = {
                        LastWishesRadioGroup(
                            displayTextOnly = "끼니 거르지 말고 건강 챙기고 지내.",
                        )
                    },
                    sectionSpacing = 32.dp,
                    videoContent = {
                        ReceiverVideoSection(
                            memorialVideoUrl = memorialVideoUrl,
                            memorialThumbnailUrl = memorialThumbnailUrl,
                        )
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
            val videoUrl = memorialVideoUrl
            InfoCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
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
                    imageVector = Icons.Default.PlayArrow,
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
            val ctx = LocalContext.current
            val imageRequest =
                remember(thumbnailUrl) {
                    ImageRequest
                        .Builder(ctx)
                        .data(thumbnailUrl)
                        .httpHeaders(
                            NetworkHeaders
                                .Builder()
                                .set("User-Agent", "Afternote Android App")
                                .build(),
                        ).build()
                }
            AsyncImage(
                model = imageRequest,
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

@Preview(showBackground = true)
@Composable
private fun PreviewReceiverAfterNoteMain() {
    AfternoteTheme {
        ReceiverAfterNoteMainScreen(senderName = "박서연", albumCovers = emptyList())
    }
}
