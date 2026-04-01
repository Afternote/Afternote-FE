package com.afternote.feature.afternote.presentation

import androidx.lifecycle.ViewModel
import com.afternote.feature.afternote.presentation.author.edit.provider.DataProviderSwitch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AfternoteHostViewModel
    @Inject
    constructor(
        val dataProviderSwitch: DataProviderSwitch,
    ) : ViewModel()
