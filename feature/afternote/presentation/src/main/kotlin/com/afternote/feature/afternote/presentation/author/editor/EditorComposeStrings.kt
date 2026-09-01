package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.editor.mapper.editScreenLabelRes

@Composable
internal fun AfternoteType.toDropdownLabel(): String = stringResource(editScreenLabelRes)
