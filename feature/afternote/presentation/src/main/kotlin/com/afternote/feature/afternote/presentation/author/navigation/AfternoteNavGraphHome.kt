package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeEntry

@Composable
internal fun AfternoteHomeNavigation(
    onNavigateToDetail: (itemId: Long) -> Unit,
    onNavigateToGalleryDetail: (itemId: Long) -> Unit,
    onNavigateToMemorialDetail: (itemId: Long) -> Unit,
    onNavigateToNewEditor: (initialCategory: String?) -> Unit,
    onNavigateToSetting: () -> Unit,
) {
    AfternoteHomeEntry(
        navigateToDetail = onNavigateToDetail,
        navigateToGalleryDetail = onNavigateToGalleryDetail,
        navigateToMemorialDetail = onNavigateToMemorialDetail,
        navigateToAdd = { selectedTab ->
            onNavigateToNewEditor(selectedTab?.toEditorNavKey())
        },
        onSettingClick = onNavigateToSetting,
    )
}

/**
 * 에디터 네비게이션 인자로 쓰는 [EditorCategory] 이름.
 *
 * 서버로 나가는 `category` 와는 다른 축이다. 한 값이 둘을 겸하던 것이 홈 탭 400 의 원인이었으므로
 * (추억 노트는 네비 키 `MEMORIAL`, 서버 값 `PLAYLIST`) 분리해 둔다. 서버 값은 data 계층만 안다.
 * [EditorCategory] 가 [AfternoteType] 으로 수렴되면(#695) 이 매핑은 사라진다.
 */
private fun AfternoteType.toEditorNavKey(): String =
    when (this) {
        AfternoteType.SOCIAL_NETWORK -> EditorCategory.SOCIAL
        AfternoteType.BUSINESS -> EditorCategory.BUSINESS
        AfternoteType.GALLERY_AND_FILES -> EditorCategory.GALLERY
        AfternoteType.ESTATE -> EditorCategory.ESTATE
        AfternoteType.MEMORIAL -> EditorCategory.MEMORIAL
    }.name
