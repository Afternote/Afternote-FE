package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.presentation.R

@Composable
fun TimeLetterDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { DetailTopBar(title = "타임레터", onBackClick = onBackClick) },
    ) { innerPadding ->

        LazyColumn(contentPadding = innerPadding) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                ) {
                    // 배경 이미지
                    Image(
                        painter = painterResource(R.drawable.ex_box_img),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )

                    // 텍스트 영역
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                    ) {
                        Text(
                            text = "채연아 20번째 생일을 축하해",
                            style = AfternoteDesign.typography.h2,
                            color = AfternoteDesign.colors.white,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "수신인  박채연",
                                style = AfternoteDesign.typography.footnoteCaption,
                                color = AfternoteDesign.colors.white,
                            )
                            Text(
                                text = "발송 예정일  2027.11.24.",
                                style = AfternoteDesign.typography.footnoteCaption,
                                color = AfternoteDesign.colors.white,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TimeLetterDetailScreenPrev() {
    TimeLetterDetailScreen(
        onBackClick = {},
    )
}
