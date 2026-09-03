package com.afternote.core.common.deeplink

/**
 * 링크를 목적지로 못 옮긴 이유 (#924).
 *
 * fail-closed 의 «관측 가능한» 쪽을 담당한다. 거절은 조용히 홈으로 보내는 것으로 끝나면 안 된다 —
 * 서버가 새 경로를 먼저 배포했는지, ID 형식을 잘못 만들었는지, 남이 우리 도메인을 흉내 냈는지는
 * 이유가 남아야 갈린다.
 *
 * [reportValue] 는 리포팅 속성에 싣는 고정 문자열이다. enum 이름을 바꿔도 이 값은 유지해야 한다 —
 * 대시보드 집계가 옛 값으로 쌓여 있다.
 *
 * **원본 링크 자체는 절대 싣지 않는다.** 경로에 애프터노트·타임레터 ID 가 들어 있어 그대로 로그에
 * 남기면 식별 가능한 사용자 자료가 리포팅으로 새 나간다. 관측에 쓰는 값은 이 이유뿐이다.
 */
enum class AppLinkRejectionReason(
    val reportValue: String,
) {
    /** 비어 있거나 URI 로 파싱조차 되지 않는다. */
    MALFORMED_URI("malformed_uri"),

    /** `https` 가 아니다. 커스텀 scheme 은 Digital Asset Links 검증을 못 받아 아무 앱이나 가로챌 수 있다. */
    UNSUPPORTED_SCHEME("unsupported_scheme"),

    /** 우리 도메인이 아니거나, 호스트 앞에 userinfo 가 붙어 도메인을 흉내 냈다. */
    UNSUPPORTED_HOST("unsupported_host"),

    /** 계약 표에 없는 경로다. 끝 슬래시·빈 세그먼트·퍼센트 인코딩 변형도 여기로 온다. */
    UNKNOWN_PATH("unknown_path"),

    /** 경로 모양은 계약에 있는데 ID 가 형식을 벗어났다. */
    MALFORMED_ID("malformed_id"),

    /** 아직 지원하는 query 가 없다. 하나라도 붙어 오면 거절이다. */
    UNSUPPORTED_QUERY("unsupported_query"),

    /** fragment 는 목적지를 바꾸지 않는데 붙어 왔다 — 계약 밖 입력이다. */
    UNSUPPORTED_FRAGMENT("unsupported_fragment"),
}
