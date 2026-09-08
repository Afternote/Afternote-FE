package com.afternote.feature.afternote.presentation.draft

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.component.toUiModel
import com.android.tools.screenshot.PreviewTest
import kotlinx.coroutines.flow.flowOf

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
internal fun draftListEmptyScreenshot() {
    DraftListPreview(emptyList())
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
internal fun draftListItemsScreenshot() {
    DraftListPreview(
        listOf(
            ListItem(1, "가족에게 남기는 사진 기록", "2026.09.08", AfternoteType.GALLERY_AND_FILES, isDraft = true).toUiModel(),
            ListItem(2, "아직 작성 중인 계정 기록", "2026.09.07", AfternoteType.SOCIAL_NETWORK, isDraft = true).toUiModel(),
        ),
    )
}

@Composable
private fun DraftListPreview(items: List<ListItemUiModel>) {
    val paging =
        remember(items) {
            flowOf(
                PagingData.from(
                    items,
                    sourceLoadStates =
                        LoadStates(
                            refresh = LoadState.NotLoading(endOfPaginationReached = true),
                            prepend = LoadState.NotLoading(endOfPaginationReached = true),
                            append = LoadState.NotLoading(endOfPaginationReached = true),
                        ),
                ),
            )
        }.collectAsLazyPagingItems()
    AfternoteTheme {
        AfternoteDraftListScreen(items = paging, onBackClick = {}, onDraftClick = { _, _ -> })
    }
}
