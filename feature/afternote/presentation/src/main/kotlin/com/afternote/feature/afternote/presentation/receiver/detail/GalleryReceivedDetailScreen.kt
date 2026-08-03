package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.AfternoteDetailServiceHeader
import com.afternote.feature.afternote.presentation.shared.detail.MessageSection
import com.afternote.feature.afternote.presentation.shared.detail.ProcessingMethodsSection
import com.afternote.feature.afternote.presentation.shared.model.AfternoteServiceDisplay

/**
 * 수신 갤러리 상세 (Stateless).
 *
 * 발신자 [com.afternote.feature.afternote.presentation.author.detail.GalleryDetailScreen]
 * 과 달리 ReceiversCard 와 편집/삭제 액션을 두지 않는다(받은 본인이 수신자).
 */
@Composable
fun GalleryReceivedDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: ReceivedGalleryDetailContent = ReceivedGalleryDetailContent(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.feature_afternote_detail_title),
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->
        GalleryReceivedDetailScrollContent(
            content = content,
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        )
    }
}

@Composable
private fun GalleryReceivedDetailScrollContent(
    content: ReceivedGalleryDetailContent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp)
                .padding(horizontal = 20.dp),
    ) {
        AfternoteDetailServiceHeader(
            service =
                AfternoteServiceDisplay.fromService(
                    serviceName = content.serviceName,
                    type = AfternoteType.GALLERY_AND_FILES,
                ),
            finalWriteDate = content.finalWriteDate,
        )

        Spacer(modifier = Modifier.height(31.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProcessingMethodsSection(methods = content.processingMethods)
            MessageSection(message = content.message)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryReceivedDetailScreenPreview() {
    AfternoteTheme {
        GalleryReceivedDetailScreen(
            onBackClick = {},
            content =
                ReceivedGalleryDetailContent(
                    serviceName = "갤러리",
                    finalWriteDate = "2025.11.26",
                    processingMethods = listOf("'엽사' 폴더 박선호에게 전송", "'흑역사' 폴더 삭제"),
                    message = "이 계정에는 우리 가족 여행 사진이 많아.\n계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
                ),
        )
    }
}
