package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.util.RecordContentBlock
import com.afternote.feature.mindrecord.presentation.viewmodel.RecordDetailUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.RecordDetailViewModel
import java.time.format.DateTimeFormatter

/**
 * 기록 상세(열람) 화면 (#759).
 *
 * Figma 3814:18721 / 18739 / 18796 / 18813 — 시안 4종은 두 축의 조합이다.
 * **첨부 이미지 유무**(헤더 배경과 글자색이 바뀐다)와 **기록 종류**(일기에만 오늘의 기분 줄).
 * 그래서 화면은 하나만 두고 상태로 가른다.
 */
@Composable
fun RecordDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordDetailViewModel = hiltViewModel(),
    isDiary: Boolean = false,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = AfternoteDesign.colors.gray1,
        topBar = {
            DetailTopBar(
                title =
                    stringResource(
                        if (isDiary) {
                            R.string.mindrecord_detail_title_diary
                        } else {
                            R.string.mindrecord_detail_title_daily_question
                        },
                    ),
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                RecordDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                RecordDetailUiState.NotFound -> {
                    // 통신 실패와 구분한다 — 여기서는 재시도가 의미 없고 목록으로 돌아가야 한다.
                    Box(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.mindrecord_detail_not_found),
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
                }

                is RecordDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = state.message.asString(), color = AfternoteDesign.colors.gray9)
                    }
                }

                is RecordDetailUiState.Success -> {
                    DetailContent(state = state)
                }
            }
        }
    }
}

@Composable
private fun DetailContent(state: RecordDetailUiState.Success) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { DetailHero(state = state) }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 시안은 본문을 «문단 → 이미지 → 문단» 처럼 섞어서 보여준다.
                state.blocks.forEach { block ->
                    when (block) {
                        is RecordContentBlock.Text -> {
                            Text(
                                text = block.text,
                                style = AfternoteDesign.typography.bodySmallR,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }

                        is RecordContentBlock.Image -> {
                            AsyncImage(
                                model = block.url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(CONTENT_IMAGE_HEIGHT)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(AfternoteDesign.colors.gray2),
                            )
                        }
                    }
                }

                state.mood?.let { MoodRow(emoji = it) }
            }
        }
    }
}

/**
 * 헤더 밴드 — 시안의 «이미지 O / X» 를 가르는 자리.
 *
 * 사진이 있으면 사진 위에 아래로 갈수록 짙어지는 검은 그라데이션을 얹고 글자를 밝게 쓴다.
 * 없으면 옅은 그라데이션을 깔고 글자를 어둡게 쓴다 — 배경이 밝아지므로 반대로 뒤집힌다.
 */
@Composable
private fun DetailHero(state: RecordDetailUiState.Success) {
    val hasImage = state.heroImageUrl != null
    val titleColor = if (hasImage) AfternoteDesign.colors.white else AfternoteDesign.colors.gray9
    val metaColor = if (hasImage) AfternoteDesign.colors.gray3 else AfternoteDesign.colors.gray8

    Box(modifier = Modifier.fillMaxWidth().height(HERO_HEIGHT)) {
        if (hasImage) {
            AsyncImage(
                model = state.heroImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                            ),
                        ),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(EmptyHeroCenter, EmptyHeroEdge),
                                center = Offset.Unspecified,
                            ),
                        ),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = state.title, style = AfternoteDesign.typography.h2, color = titleColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 수신자를 지정하지 않은 기록에는 이 줄 자체를 만들지 않는다.
                if (state.receiverNames.isNotEmpty()) {
                    Text(
                        text =
                            stringResource(R.string.mindrecord_detail_receiver) +
                                " " + state.receiverNames.joinToString(", "),
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = metaColor,
                    )
                } else {
                    Box(modifier = Modifier)
                }
                // 날짜를 못 구하면 줄을 그리지 않는다 — 지어낸 날짜를 그리면 사용자는 그
                // 기록이 실제로 그 날 쓰인 것으로 읽는다 (#759 리뷰).
                state.date?.let { date ->
                    Text(
                        text = date.format(DetailDateFormatter),
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = metaColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodRow(emoji: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 시안은 라벨 앞에 16dp 아이콘을 둔다 — 작성 화면과 같은 표정 아이콘이다.
        Icon(
            painter = painterResource(R.drawable.mindrecord_emotion),
            contentDescription = null,
            tint = AfternoteDesign.colors.gray6,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.mindrecord_detail_mood),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AfternoteDesign.colors.gray2),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, style = AfternoteDesign.typography.bodyLargeB)
        }
    }
}

private val HERO_HEIGHT = 160.dp
private val CONTENT_IMAGE_HEIGHT = 248.dp

/** 시안 표기 — `2024.03.21.` (끝의 마침표 포함). */
private val DetailDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.")

/** 이미지가 없을 때의 헤더 그라데이션 (시안 실측). */
private val EmptyHeroCenter = Color(0xFFB7CDC0)
private val EmptyHeroEdge = Color(0xFFF8F8F7)
