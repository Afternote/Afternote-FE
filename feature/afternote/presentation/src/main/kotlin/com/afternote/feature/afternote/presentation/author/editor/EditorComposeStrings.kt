package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory

@Composable
internal fun editorCategoryDropdownLabels(): List<String> = EditorCategory.entries.map { it.toDropdownLabel() }

@Composable
internal fun EditorCategory.toDropdownLabel(): String =
    when (this) {
        EditorCategory.SOCIAL -> stringResource(R.string.afternote_editor_category_social)
        EditorCategory.BUSINESS -> stringResource(R.string.afternote_editor_category_business)
        EditorCategory.GALLERY -> stringResource(R.string.afternote_editor_category_gallery)
        EditorCategory.ESTATE -> stringResource(R.string.afternote_editor_category_estate)
        EditorCategory.MEMORIAL -> stringResource(R.string.afternote_editor_category_memorial)
    }
