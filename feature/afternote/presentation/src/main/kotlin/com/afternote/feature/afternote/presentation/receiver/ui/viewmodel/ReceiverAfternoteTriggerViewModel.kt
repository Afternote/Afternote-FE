package com.afternote.feature.afternote.presentation.receiver.ui.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.port.LoadMindRecordsByAuthCodePort
import com.afternote.feature.afternote.domain.port.LoadSenderMessageByAuthCodePort
import com.afternote.feature.afternote.domain.port.LoadTimeLettersByAuthCodePort
import com.afternote.feature.afternote.domain.usecase.receiver.GetAfterNotesByAuthCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 HOME에서 콘텐츠 개수(마음의 기록, 타임레터, 애프터노트) 및 leaveMessage를 로드하는 ViewModel.
 *
 * [loadHomeSummary]로 네 API를 병렬 호출하여 totalCount와 leaveMessage를 수집합니다.
 * leaveMessage는 GET /api/receiver-auth/message의 message 필드에서 가져옵니다.
 * [loadAfterNotes]는 "애프터노트 확인하러 가기" 버튼 클릭 시 afternoteTotalCount 갱신용으로 호출됩니다.
 */
@HiltViewModel
class ReceiverAfternoteTriggerViewModel
    @Inject
    constructor(
        private val getAfterNotesByAuthCodeUseCase: GetAfterNotesByAuthCodeUseCase,
        private val loadMindRecordsByAuthCode: LoadMindRecordsByAuthCodePort,
        private val loadTimeLettersByAuthCode: LoadTimeLettersByAuthCodePort,
        private val loadSenderMessageByAuthCode: LoadSenderMessageByAuthCodePort,
    ) : ViewModel() {
        private val _leaveMessage = MutableStateFlow<String?>(null)
        val leaveMessage: StateFlow<String?> = _leaveMessage.asStateFlow()

        private val _mindRecordTotalCount = MutableStateFlow(0)
        val mindRecordTotalCount: StateFlow<Int> = _mindRecordTotalCount.asStateFlow()

        private val _timeLetterTotalCount = MutableStateFlow(0)
        val timeLetterTotalCount: StateFlow<Int> = _timeLetterTotalCount.asStateFlow()

        private val _afternoteTotalCount = MutableStateFlow(0)
        val afternoteTotalCount: StateFlow<Int> = _afternoteTotalCount.asStateFlow()

        /**
         * 홈 화면용 요약 데이터 로드. 마음의 기록, 타임레터, 애프터노트 totalCount와 leaveMessage를 가져옵니다.
         *
         * @param authCode 수신자 인증번호 (마스터키)
         */
        fun loadHomeSummary(authCode: String) {
            viewModelScope.launch {
                val afternotesDeferred = async { getAfterNotesByAuthCodeUseCase(authCode) }
                val mindRecordsDeferred = async { loadMindRecordsByAuthCode(authCode) }
                val timeLettersDeferred = async { loadTimeLettersByAuthCode(authCode) }
                val messageDeferred = async { loadSenderMessageByAuthCode(authCode) }
                awaitAll(
                    afternotesDeferred,
                    mindRecordsDeferred,
                    timeLettersDeferred,
                    messageDeferred,
                )

                messageDeferred.await().onSuccess { message ->
                    _leaveMessage.value = message?.takeIf { it.isNotBlank() }
                }
                afternotesDeferred.await().onSuccess { result ->
                    _afternoteTotalCount.value = result.totalCount
                }
                mindRecordsDeferred.await().onSuccess { result ->
                    _mindRecordTotalCount.value = result.totalCount
                }
                timeLettersDeferred.await().onSuccess { result ->
                    _timeLetterTotalCount.value = result.totalCount
                }
            }
        }

        /**
         * GET /api/receiver-auth/after-notes (X-Auth-Code) API를 호출합니다.
         * leaveMessage 갱신 및 afternoteTotalCount 업데이트.
         *
         * @param authCode 수신자 인증번호 (마스터키)
         */
        fun loadAfterNotes(authCode: String) {
            viewModelScope.launch {
                getAfterNotesByAuthCodeUseCase(authCode)
                    .onSuccess { result ->
                        _afternoteTotalCount.value = result.totalCount
                    }
            }
        }
    }
