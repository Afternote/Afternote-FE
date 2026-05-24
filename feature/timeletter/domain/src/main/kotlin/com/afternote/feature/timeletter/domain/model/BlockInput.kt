package com.afternote.feature.timeletter.domain.model

sealed class BlockInput {
    data class Text(
        val content: String,
    ) : BlockInput()

    data class Media(
        val uriString: String,
        val mimeType: String?,
        val blockType: TimeLetterBlockType,
    ) : BlockInput()

    data class Link(
        val url: String,
    ) : BlockInput()
}
