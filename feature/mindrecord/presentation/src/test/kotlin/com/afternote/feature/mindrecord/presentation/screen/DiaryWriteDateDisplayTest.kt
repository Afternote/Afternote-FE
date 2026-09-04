package com.afternote.feature.mindrecord.presentation.screen

import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 일기 기록 날짜가 «고를 수 있지만 반영되지 않는» 상태가 아닌지 (#1008).
 *
 * **기록 날짜는 작성일로 고정한다** — 임시 조치가 아니라 확정된 기획 판정이다. 서버도 같은
 * 전제라, 생성·수정 어느 쪽에서도 `date` 를 받지 않고 기록 날짜를 요청 시각으로 정한다
 * (실측 2026-08-25: `POST`·`PATCH` 에 `date: "2026-08-01"` 을 실어 보내도 저장분은
 * `2026-08-25`, Swagger 스키마에도 필드가 없다). 그 상태에서 날짜 피커를 열어 두면
 * 사용자는 고른 날짜로 남았다고 믿는다.
 *
 * 화면에서 피커를 걷어냈으므로 **ViewModel 에 날짜를 바꾸는 창구가 남아 있으면 안 된다** —
 * 남겨 두면 다음 사람이 그 함수를 보고 UI 를 되살린다. 이 가드는 그때까지가 아니라 계속
 * 남는다.
 */
class DiaryWriteDateDisplayTest {
    @Test
    fun `날짜를 바꾸는 공개 창구가 없다`() {
        // "update" 가 "date" 를 부분 문자열로 품는다 — 날짜와 무관한 updateXxx 가 하나만
        // 생겨도 빨개지므로, 이름 규칙이 아니라 **LocalDate 를 받는 공개 함수**로 좁힌다.
        val dateSetters =
            DiaryWriteViewModel::class.java.methods
                .filter {
                    java.lang.reflect.Modifier
                        .isPublic(it.modifiers)
                }.filter { it.parameterTypes.any { type -> type == LocalDate::class.java } }
                .map { it.name }

        assertTrue("날짜 변경 창구가 남아 있다: $dateSetters", dateSetters.isEmpty())
    }
}
