package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.shared.LastWishOption

@Composable
internal fun editorCategoryDropdownLabels(): List<String> = EditorCategory.entries.map { it.toDropdownLabel() }

@Composable
internal fun EditorCategory.toDropdownLabel(): String =
    when (this) {
        EditorCategory.SOCIAL -> stringResource(R.string.afternote_editor_category_social)
        EditorCategory.GALLERY -> stringResource(R.string.afternote_editor_category_gallery)
        EditorCategory.MEMORIAL -> stringResource(R.string.afternote_editor_category_memorial)
    }

@Composable
internal fun editorRelationshipOptions(): List<String> =
    listOf(
        stringResource(R.string.afternote_editor_relationship_friend),
        stringResource(R.string.afternote_editor_relationship_family),
        stringResource(R.string.afternote_editor_relationship_lover),
    )

@Composable
internal fun editorLastWishOptions(): List<LastWishOption> =
    listOf(
        LastWishOption(
            text = stringResource(R.string.afternote_editor_last_wish_calm),
            value = "calm",
        ),
        LastWishOption(
            text = stringResource(R.string.afternote_editor_last_wish_bright),
            value = "bright",
        ),
        LastWishOption(
            text = stringResource(R.string.afternote_editor_last_wish_other),
            value = "other",
        ),
    )
