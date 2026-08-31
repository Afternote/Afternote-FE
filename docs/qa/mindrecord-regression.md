# 마음의 기록 회귀 체크리스트

#269 의 체크리스트를 **현재 코드 기준으로 다시 쓴 것**이다. 원 이슈는 `fix/253`(PR #255) 시점에
작성돼 지금 없는 기능(깊은 생각·카테고리 CRUD)을 포함하고 있었다.

각 항목은 둘 중 하나다.

- **[자동]** — 테스트가 이미 고정한다. 회귀하면 CI 가 막으므로 손으로 다시 볼 필요가 없다.
  테스트 이름을 그대로 적어 두어 «무엇을 고정하는지» 를 문서가 아니라 코드에서 확인할 수 있게 했다.
- **[수동]** — 자동으로 덮지 못한다. 실기기·실서버가 필요하거나(권한 다이얼로그·IME·피커),
  픽셀 판정이거나, 서버 상태를 만들어야 하는 경우다.

정본은 테스트 코드다. 이 문서가 코드와 어긋나면 코드가 맞다.

현재 마음의 기록 3개 모듈의 자동 테스트는 **156건**이다 ().

## 지금 없는 기능 — 원 체크리스트에서 뺀 것

| 원 항목 | 근거 |
|---|---|
| 깊은 생각 목록·작성 | 도메인에 값이 없다 — `MindRecordCategory` 는 `DAILY_QUESTION`·`DIARY`·`WEEKLY_REPORT` 뿐이고 `DeepThoughtScreen` 도 없다 |
| 카테고리 CRUD (`CategoryError` sealed 분기) | 화면·저장소 모두 없다 |
| 위클리 «깊은 생각» 카운트 | 서버가 `deepThoughtAmount` 를 계속 내려주지만 DTO 에 선언하지 않아 무시된다 |
| 위치 추가 버튼 | 작성 화면에 없다 |

## 0. 횡단

- [자동] 날짜 해석 실패를 오늘로 메우지 않는다 — `MindRecordUiMapperTest`(7건) · `DailyQuestionDateFallbackTest`(4건)
- [자동] 조회 취소가 실패로 둔갑하지 않는다 — `MindRecordCancellationTest`(2건)
- [자동] 계약 키 누락이 «정상값» 으로 접히지 않는다 — `DiaryListContractTest` · `TodayDailyQuestionContractTest` ·
  `WeeklyReportContractTest` · `MindRecordDtoContractTest` (총 31건)
- [수동] 회전 · 다크모드 · 폰트 스케일 large 에서 레이아웃이 깨지지 않는다
- [수동] 기내모드 진입 → 각 실패 화면 → 해제 후 재시도로 복구된다

## 1. 데일리 질문

### 목록
- [자동] 임시저장은 목록에 새지 않는다 — `ReceiverMindRecordDraftTest`(3건)
- [자동] 재진입 갱신이 정렬·기간 필터를 지우지 않는다 — `ReceiverRefreshFilterTest`
- [수동] 캘린더 뷰에서 답변한 날만 점이 찍히고 오늘이 강조된다
- [수동] 월 이동 시 그 달 데이터로 갱신된다

### 작성
- [자동] 오늘 질문 조회가 계속 실패해도 저장은 사유를 남기고 요청을 보내지 않는다 — `DailyQuestionWriteViewModelTest`
- [자동] 재조회가 사용자가 방금 쓴 답변·고른 이미지를 덮지 않는다 — 같은 클래스 2건
- [자동] 본문 이미지의 `src` 는 저장 시 fileKey 로 나가고, 이미 저장된 영구 URL 은 건드리지 않는다 — 같은 클래스 2건
- [자동] 화면이 비어 있으면(`<p></p>`) 저장이 열리지 않는다 — `HtmlBlankTest`(7건)
- [자동] 이어쓰기가 사용자가 이미 쓴 내용을 덮지 않는다 — `DailyQuestionResumeDraftTest`(6건)
- [수동] 키보드 툴바 — 링크 시트, 텍스트 스타일 패널, 미디어 시트가 IME 전환에서 살아남는다
- [수동] 사진·음성·파일 첨부 후 본문에 표시되는 이름이 로컬 `content://` 가 아니다

## 2. 일기

### 목록
- [자동] 삭제 실패 안내가 다음 성공·재조회에서 걷힌다 — `MindRecordFailureRecoveryTest`(3건)
- [자동] 임시저장 목록 삭제의 부분 실패가 «완료» 로 접히지 않는다 — `DraftListDeleteTest`(3건)
- [수동] 리스트/그리드 전환이 스크롤 위치·선택 달을 유지한다
- [수동] 월 이동 시 그 달 데이터로 갱신된다

### 작성
- [자동] 이미지를 올리는 중에는 임시저장도 나가지 않는다 — `MindRecordFailureRecoveryTest`
- [수동] 무드 3종 토글, 날짜 선택 시트
- [수동] 등록 성공 후 목록에 즉시 반영된다

## 3. 주간 리포트

- [자동] 기록일 집계 — sparse `week`, 월 경계, 같은 날 합산, 주 범위 밖 제외 — `WeeklyReportRecordedDaysTest`(17건)
- [자동] 주차 선택지 구성과 스크롤 선택 — `WeeklyReportWeekOptionsTest`(4건) · `WeeklyReportWeekMenuTest`(3건)
- [자동] 감정 분석 상태(대기·실패·완료)가 «키워드 0건» 으로 확정되지 않는다 —
  `WeeklyReportEmotionAnalysisTest`(7건) · `EmotionAnalysisContractTest`(8건) · `EmotionCardDescriptionTest`(3건)
- [자동] 요약 문구의 강조 구간이 이름·기록일수와 겹치지 않는다 — `WeeklyReportSummaryTest`(4건)
- [수동] 이모지·점 조합 4종이 실제 캘린더에서 시안대로 보인다
- [수동] 폴링(8회 × 8초)이 소진된 뒤 화면을 나갔다 들어오면 갱신된다

## 4. 수신자 열람

- [자동] 전달 조건 미충족이 서버 원문 대신 도메인 문구가 된다 — `ReceiverDeadEndTest`(3건) ·
  `MindRecordReceiverRepositoryImplTest`(6건)
- [자동] 목록에서 탭한 항목의 본문 시트가 제목·본문·기분을 보여준다 — `ReceiverRecordDetailTest`(6건)
- [수동] `delivery-verification` 이 APPROVED 인 계정으로 실제 진입 — 자동 테스트가 덮지 못하는 유일한 경로다

## 5. 디바이스 매트릭스

- [수동] minSdk · 최신 SDK 각 1대
- [수동] 폰트 스케일 large 에서 텍스트 잘림 없음
- [수동] ko-KR (en/ja 미지원)
