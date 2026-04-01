package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.timeletter.domain.TimeLetters

@Composable
private fun TimeLetterContent(
    letters: TimeLetters,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) {
        EmptyTimeLetterContent(modifier = modifier)
    } else {
        TimeLetterContent(letters = letters, modifier = modifier)
    }
}
