package com.afternote.feature.timeletter.presentation.navigation

interface TimeLetterNavActions {
    fun popBack()

    fun onSettingClick()

    fun onNavigateToWrite()

    fun onNavigateToEdit(timeLetterId: Long)

    fun onNavigateToDraft()

    fun onNavigateToRecipient()

    fun onNavigateToDetail(timeLetterId: Long)

    fun onNavigateToRecipientFilter()
}
