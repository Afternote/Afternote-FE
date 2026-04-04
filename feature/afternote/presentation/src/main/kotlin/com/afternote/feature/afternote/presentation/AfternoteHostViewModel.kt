package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import com.afternote.feature.afternote.domain.model.ListItem
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
        val dataProviderSwitch: DataProviderSwitch,
    ) : ViewModel() {
        private val _items = MutableStateFlow<List<ListItem>>(emptyList())
        val items: StateFlow<List<ListItem>> = _items.asStateFlow()

        fun updateItems(newListItems: List<ListItem>) {
            _items.value = newListItems
        }
    }
