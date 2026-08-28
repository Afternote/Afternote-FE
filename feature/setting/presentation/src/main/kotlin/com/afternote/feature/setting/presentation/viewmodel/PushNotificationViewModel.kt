package com.afternote.feature.setting.presentation.viewmodel

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PushNotificationViewModel internal constructor(
    private val userRepository: UserRepository,
    deviceAlarmOn: Boolean,
) : ViewModel() {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        userRepository: UserRepository,
    ) : this(
        userRepository = userRepository,
        deviceAlarmOn = NotificationManagerCompat.from(context).areNotificationsEnabled(),
    )

    private val _uiState = MutableStateFlow(PushNotificationUiState())
    val uiState: StateFlow<PushNotificationUiState> = _uiState.asStateFlow()

    private var failedUpdate: PushSettingUpdate? = null

    init {
        _uiState.update { it.copy(isDeviceAlarmOn = deviceAlarmOn) }
        loadPushSettings()
    }

    private fun loadPushSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatchingCancellable { userRepository.getMyPushSettings() }
                .onSuccess { setting ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNewsletterOn = setting.timeLetter,
                            isMindRecordOn = setting.mindRecord,
                            isAfternoteOn = setting.afterNote,
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun onSmsChecked(checked: Boolean) = _uiState.update { it.copy(isSmsChecked = checked) }

    fun onEmailChecked(checked: Boolean) = _uiState.update { it.copy(isEmailChecked = checked) }

    fun onPushChecked(checked: Boolean) = _uiState.update { it.copy(isPushChecked = checked) }

    fun onNewsletterToggle(on: Boolean) {
        updatePushSetting(PushSettingUpdate(PushSetting.NEWSLETTER, on))
    }

    fun onMindRecordToggle(on: Boolean) {
        updatePushSetting(PushSettingUpdate(PushSetting.MIND_RECORD, on))
    }

    fun onAfternoteToggle(on: Boolean) {
        updatePushSetting(PushSettingUpdate(PushSetting.AFTERNOTE, on))
    }

    fun onSaveFailureDismiss() {
        failedUpdate = null
        _uiState.update { it.copy(saveFailure = null) }
    }

    fun onSaveFailureRetry() {
        val update = failedUpdate ?: return
        failedUpdate = null
        _uiState.update { it.copy(saveFailure = null) }
        updatePushSetting(update)
    }

    private fun updatePushSetting(update: PushSettingUpdate) {
        if (_uiState.value.isUpdating(update.setting)) return
        val previousValue = _uiState.value.valueOf(update.setting)
        _uiState.update {
            it.withValue(update.setting, update.on).withUpdating(update.setting, updating = true)
        }
        viewModelScope.launch {
            runCatchingCancellable {
                userRepository.updateMyPushSettings(
                    timeLetter = update.on.takeIf { update.setting == PushSetting.NEWSLETTER },
                    mindRecord = update.on.takeIf { update.setting == PushSetting.MIND_RECORD },
                    afterNote = update.on.takeIf { update.setting == PushSetting.AFTERNOTE },
                )
            }.onSuccess {
                _uiState.update { it.withUpdating(update.setting, updating = false) }
            }.onFailure { failure ->
                failedUpdate = update
                _uiState.update {
                    it
                        .withValue(update.setting, previousValue)
                        .withUpdating(update.setting, updating = false)
                        .copy(saveFailure = failure.toSaveFailure())
                }
            }
        }
    }
}

private enum class PushSetting {
    NEWSLETTER,
    MIND_RECORD,
    AFTERNOTE,
}

private data class PushSettingUpdate(
    val setting: PushSetting,
    val on: Boolean,
)

private fun PushNotificationUiState.valueOf(setting: PushSetting): Boolean =
    when (setting) {
        PushSetting.NEWSLETTER -> isNewsletterOn
        PushSetting.MIND_RECORD -> isMindRecordOn
        PushSetting.AFTERNOTE -> isAfternoteOn
    }

private fun PushNotificationUiState.withValue(
    setting: PushSetting,
    on: Boolean,
): PushNotificationUiState =
    when (setting) {
        PushSetting.NEWSLETTER -> copy(isNewsletterOn = on)
        PushSetting.MIND_RECORD -> copy(isMindRecordOn = on)
        PushSetting.AFTERNOTE -> copy(isAfternoteOn = on)
    }

private fun PushNotificationUiState.isUpdating(setting: PushSetting): Boolean =
    when (setting) {
        PushSetting.NEWSLETTER -> isNewsletterUpdating
        PushSetting.MIND_RECORD -> isMindRecordUpdating
        PushSetting.AFTERNOTE -> isAfternoteUpdating
    }

private fun PushNotificationUiState.withUpdating(
    setting: PushSetting,
    updating: Boolean,
): PushNotificationUiState =
    when (setting) {
        PushSetting.NEWSLETTER -> copy(isNewsletterUpdating = updating)
        PushSetting.MIND_RECORD -> copy(isMindRecordUpdating = updating)
        PushSetting.AFTERNOTE -> copy(isAfternoteUpdating = updating)
    }

private fun Throwable.toSaveFailure(): PushNotificationSaveFailure =
    when (this) {
        is PushSettingFailure.NetworkUnavailable -> PushNotificationSaveFailure.NETWORK
        else -> PushNotificationSaveFailure.SERVER
    }
