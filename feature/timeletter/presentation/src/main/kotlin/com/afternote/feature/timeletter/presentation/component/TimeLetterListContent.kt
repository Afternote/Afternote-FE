package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.timeletter.domain.TimeLetters

@Composable
fun TimeLetterContent(
    letters: TimeLetters,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(letters.toList()) { letter ->
            TimeLetterListItem(letter = letter)
        }
    }
}
