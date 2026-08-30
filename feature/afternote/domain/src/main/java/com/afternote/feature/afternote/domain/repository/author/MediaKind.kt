package com.afternote.feature.afternote.domain.repository.author

/** 추억 노트 미디어의 종류. 업로드 위임 대상을 가른다. */
enum class MediaKind {
    PHOTO,
    VIDEO,

    /** 추모 음성 (#1118). 서버 `playlist.memorialAudioUrl` 에 실린다 — 추억 노트당 1개. */
    AUDIO,
}
