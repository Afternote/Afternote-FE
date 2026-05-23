package com.afternote.feature.timeletter.presentation.navigation

interface TimeLetterNavActions {
    fun onNavigateToWrite()

    fun onWriteBack()

    fun onNavigateToDraft()

    fun onDraftBack()

    fun onNavigateToRecipient()

    fun onRecipientBack()

    fun onNavigateToDetail(timeLetterId: Long)

    fun onDetailBack()
}
