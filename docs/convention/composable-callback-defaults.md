# 컴포저블 콜백 디폴트 규칙

**컴포저블의 `on*` 콜백 파라미터에 no-op 디폴트(`onXxx: (...) -> Unit = {}`)를 두지 않는다.** 선택적 상호작용은 디폴트가 아니라 타입(nullable 핸들러·오버로드)으로 모델링한다.

`app` · `feature/*` 모듈의 `src/main` 에 적용하고, `konsist` 의 `NoOpCallbackDefaultKonsistTest` 가 강제한다 (#1388).

## 왜 금지인가

no-op 디폴트는 **배선을 빠뜨려도 컴파일이 통과**하게 만든다. 화면은 그려지고, 버튼도 눌리는데, 아무 일도 일어나지 않는다 — 실패가 조용해서 QA 나 사용자 신고로만 드러난다.

실사고 전례가 반복됐다:

- #582 · #618 · #722 — mindrecord 미배선 실사고. 콜백을 넘기지 않은 채 화면이 출고됐다.
- #777 — 137건 전수 대조로 실결함을 적출했지만, **디폴트 자체가 남아 있어 같은 부류가 재발할 통로가 열려 있었다.** 신설 코드가 패턴을 답습하는 것도 확인됐다(PR #1336 의 `onVideoClick`).
- #778 (open) — `TimeLetterWriteScreen.onTextStyleClick` 이 지금도 미배선 의심으로 추적 중이다.

디폴트가 없으면 같은 실수가 **컴파일 에러**로 드러난다. 폴백보다 값 명시·타입으로 강제한다는 #934 방침과 같은 방향이다.

## 처분 기준

### 화면 컴포저블 — 디폴트 전면 제거

실호출부가 NavGraph 한 곳인 화면 컴포저블은 콜백을 전부 required 로 한다. 프리뷰 · screenshotTest · unit test 는 `{}` 를 **명시**한다 (#602 전례).

```kotlin
// Before
fun GalleryDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},      // 배선 누락이 조용히 no-op
)

// After
fun GalleryDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,           // 누락 = 컴파일 에러
)

// Preview / screenshotTest
GalleryDetailScreen(onBackClick = {}, onEditClick = {})
```

`{}` 명시는 디폴트와 렌더링이 동일하므로 스크린샷 baseline 이 유지된다. reference PNG 재생성이 필요한 처분이라면 그 처분이 틀린 것이다(골든 정본은 CI 컨테이너 — `docs/testing` 참고).

### 다중 호출부 리프 컴포넌트 — 건별 판정

- **전 호출부가 실값을 넘긴다** → 디폴트만 제거한다. 선택성이 실재하지 않았던 것이다.
- **상호작용이 진짜 선택적이다** → `= {}` 로 "눌러도 아무 일 없는 버튼"을 그리는 대신, 상호작용 UI 자체를 접는다:

```kotlin
// nullable 핸들러 + UI 숨김 (editDeleteActionMenuItems, #1388)
fun editDeleteActionMenuItems(
    onDeleteClick: () -> Unit,
    onEditClick: (() -> Unit)?,        // null = 편집 항목을 그리지 않는다
): List<ActionMenuItem> = buildList {
    if (onEditClick != null) {
        add(ActionMenuItem(label = "수정", onClick = onEditClick))
    }
    add(ActionMenuItem(label = "삭제", onClick = onDeleteClick))
}
```

nullable 핸들러에도 **`= null` 디폴트를 두지 않는다** — 호출부가 "핸들러 있음 / 의도적으로 없음" 을 매번 명시해야 미배선과 의도적 생략이 구분된다. `showEditItem: Boolean` 같은 플래그 + no-op 콜백 짝은 nullable 핸들러 하나로 합친다(플래그와 콜백이 어긋나는 상태를 타입에서 제거).

상호작용 조합이 많아 nullable 이 지저분해지면 오버로드 분리(상호작용 있는 시그니처 / 없는 시그니처)를 쓴다.

### 콜백 홀더 클래스도 같다

`ReceiverHomeActions` 처럼 콜백을 프로퍼티로 묶은 클래스의 `val onXxx: () -> Unit = {}` 도 같은 방식으로 미배선을 숨긴다. 가드가 클래스 주 생성자까지 본다.

## 강제 수단

`konsist/src/test/kotlin/com/afternote/konsist/NoOpCallbackDefaultKonsistTest.kt` 가 app · feature `src/main` 의 `@Composable` 함수 파라미터와 클래스 주 생성자 파라미터를 스캔해, `on`+대문자 이름에 no-op 람다 기본값(`{}` · `{ }` · `{ _ -> }`)이 있으면 실패시킨다.

가드 도입 시점의 잔여 파일은 테스트 안 `LEGACY_NO_OP_DEFAULT_FILES` 에 있다. 목록의 파일은 위반이 있어도 없어도 통과한다(관대 판정 — 청소 PR 과 목록 갱신 PR 의 머지 순서가 develop 을 red 로 만들지 않게). 모듈 담당별 후속 PR(#1388 의 자식 이슈)이 청소하면서 목록에서 빼면 되고, 빼는 걸 잊으면 「해소된 항목은 경고로 알린다」 테스트가 CI 로그에 경고를 남긴다.
