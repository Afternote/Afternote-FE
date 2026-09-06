package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle

/**
 * 이번 작성에서 업로드한 `fileUrl` → 서버가 준 `fileKey` (#1125).
 *
 * ### 왜 [SavedStateHandle] 에 싣는가
 *
 * 제출 정확성이 이 대응표에 걸려 있다. 본문에는 미리보기가 뜨는 **전체 URL** 이 들어가고,
 * 서버로 나갈 때 `toWireContent` 가 이 표를 보고 fileKey 로 바꾼다. 표가 비면 전체 URL 이
 * 그대로 나가 서버가 호스트를 한 번 더 붙이고 403 이 된다 (#549).
 *
 * 에디터 본문은 프로세스 사망을 **건너뛴다** — `rememberRichTextState()` 가
 * `rememberSaveable(saver = RichTextState.Saver)` 라 HTML 을 복원하고, 그 값이 다시 ViewModel
 * 로 흘러든다. 그래서 표만 인메모리로 두면 복원된 본문에는 전체 URL 이 남아 있는데 바꿀 근거가
 * 사라진 상태가 된다. 표도 같은 수명을 가져야 짝이 맞는다 (#1125 리뷰).
 *
 * 값은 `fileUrl`·`fileKey` 문자열뿐이라 그대로 [SavedStateHandle] 에 실을 수 있다.
 */
internal class UploadedFileKeys(
    private val savedStateHandle: SavedStateHandle,
) {
    private val entries: MutableMap<String, String> =
        savedStateHandle.get<HashMap<String, String>>(KEY)?.toMutableMap() ?: mutableMapOf()

    /** 제출 직전 치환에 넘길 스냅샷. 호출부가 들고 있는 동안 바뀌지 않도록 복사해 준다. */
    fun snapshot(): Map<String, String> = entries.toMap()

    operator fun set(
        fileUrl: String,
        fileKey: String,
    ) {
        entries[fileUrl] = fileKey
        persist()
    }

    /** 제출 성공 뒤 비운다 — 다음 작성이 남의 키를 물려받지 않게 한다. */
    fun clear() {
        if (entries.isEmpty()) return
        entries.clear()
        persist()
    }

    private fun persist() {
        savedStateHandle[KEY] = HashMap(entries)
    }

    private companion object {
        /** 두 작성 화면이 각자 제 [SavedStateHandle] 을 쓰므로 키가 겹쳐도 섞이지 않는다. */
        const val KEY = "mindrecord_uploaded_file_keys"
    }
}
