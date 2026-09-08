package com.afternote.feature.mindrecord.domain.sync

import java.util.concurrent.atomic.AtomicLong

/**
 * 마음의 기록 데이터가 바뀐 횟수. **자동 갱신을 데이터 변경 기준으로 좁히기 위한 값**이다 (#736).
 *
 * 종전에는 탭 전환과 `ON_RESUME` 이 무조건 재조회를 걸었다. 화면 off/on, 홈 버튼 복귀,
 * 권한 다이얼로그 닫기까지 같은 조회를 다시 내보내, 마음의 기록 첫 진입 한 번에 요청이
 * 7건 나갔다(화면에 필요한 건 2건).
 *
 * 갱신 자체를 없애면 #520 이 되돌아온다 — 작성하고 돌아왔는데 목록이 그대로인 문제다.
 * 그래서 "돌아왔으니 다시 부른다" 를 **"바뀌었으니 다시 부른다"** 로 바꾼다. 쓰기가
 * 성공하면 [notifyChanged] 로 버전을 올리고, 화면은 자기가 읽어 둔 버전과 다를 때만
 * 다시 부른다.
 *
 * 프로세스 전역 단일 인스턴스이며 스레드 안전하다. 어느 화면에서 쓴 변경이든 다른 화면의
 * 갱신 판단에 그대로 반영돼야 하므로 저장소는 공유한다.
 */
class MindRecordChangeTracker {
    private val counter = AtomicLong(0L)

    /** 현재 데이터 버전. 값 자체에 의미는 없고 **달라졌는지**만 본다. */
    val version: Long
        get() = counter.get()

    /** 일기·데일리질문의 생성·수정·삭제가 성공했을 때 호출한다. */
    fun notifyChanged() {
        counter.incrementAndGet()
    }
}
