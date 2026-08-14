package com.afternote.feature.timeletter.presentation.navigation

interface TimeLetterNavActions {
    fun onSettingClick()

    fun onNavigateToWrite()

    fun onNavigateToEdit(timeLetterId: Long)

    fun onWriteBack()

    fun onNavigateToDraft()

    fun onDraftBack()

    fun onNavigateToRecipient()

    fun onRecipientBack()

    fun onNavigateToDetail(timeLetterId: Long)

    fun onDetailBack()

    fun onNavigateToRecipientFilter()

    fun onRecipientFilterBack()
}
