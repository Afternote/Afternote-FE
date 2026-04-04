package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.editor.provider.DataProviderSwitch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AfternoteHostViewModel
    @Inject
    constructor(
        private val dataProviderSwitch: DataProviderSwitch,
    ) : ViewModel() {
        private val _items = MutableStateFlow<List<ListItem>>(emptyList())
        val items: StateFlow<List<ListItem>> = _items.asStateFlow()

        val useFakeState: StateFlow<Boolean> = dataProviderSwitch.useFakeState

        val currentAfternoteEditorDataProvider: AfternoteEditorDataProvider
            get() = dataProviderSwitch.currentAfternoteEditorDataProvider

        fun updateListItems(newListItems: List<ListItem>) {
            _items.value = newListItems
        }
    }
