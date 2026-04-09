package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecipientListViewModel @Inject constructor() : ViewModel() {
    val searchState = TextFieldState()
}