package com.afternote.feature.afternote.presentation.author.editor.message

import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessagesCodec.BLOCK_SEPARATOR

/**
 * API에 저장되는 "남기실 말씀" 단일 문자열 ↔ UI 블록 목록.
 * UI는 `TextFieldState`로 편집하고, SSOT 백업은 [com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState.messageBlocks]에 둔다. 직렬화 규칙만 이 객체에 둔다.
 */
data class EditorMessageTextBlock(
    val title: String,
    val body: String,
)

object EditorMessagesCodec {
    /** 제목/본문 구분 (본문에 쓰이지 않을 가능성이 높은 구분자). */
    private const val TITLE_BODY_SEPARATOR = "\u001E"

    private const val BLOCK_SEPARATOR = "\n---\n"

    /**
     * API에 저장된 단일 문자열을 블록 목록으로 파싱한다.
     * 블록은 [BLOCK_SEPARATOR], 제목/본문은 [TITLE_BODY_SEPARATOR](신규);
     * 구버전(`title\nbody` 한 줄 제목 가정)은 첫 줄/나머지로 나눈다.
     */
    fun parsePersistedToBlocks(persisted: String): List<EditorMessageTextBlock> {
        val body = persisted.trim()
        if (body.isEmpty()) return emptyList()
        val blocks = body.split(BLOCK_SEPARATOR)
        val result = mutableListOf<EditorMessageTextBlock>()
        for (rawBlock in blocks) {
            val block = rawBlock.trim()
            if (block.isEmpty()) continue
            val sepIdx = block.indexOf(TITLE_BODY_SEPARATOR)
            when {
                sepIdx >= 0 -> {
                    result.add(
                        EditorMessageTextBlock(
                            title = block.substring(0, sepIdx),
                            body = block.substring(sepIdx + TITLE_BODY_SEPARATOR.length),
                        ),
                    )
                }

                else -> {
                    val nl = block.indexOf('\n')
                    if (nl >= 0) {
                        result.add(
                            EditorMessageTextBlock(
                                title = block.substring(0, nl),
                                body = block.substring(nl + 1),
                            ),
                        )
                    } else {
                        result.add(EditorMessageTextBlock(title = "", body = block))
                    }
                }
            }
        }
        return result
    }

    /** UI 블록들을 API 저장용 단일 문자열로 합친다. */
    fun serializeBlocksToPersisted(blocks: List<EditorMessageTextBlock>): String =
        blocks
            .map { (t, c) ->
                if (t.isNotEmpty()) {
                    "$t$TITLE_BODY_SEPARATOR$c"
                } else {
                    c
                }
            }.filter { it.isNotBlank() }
            .joinToString(BLOCK_SEPARATOR)
}
