# core/ui

Afternote-FE 의 공용 Composable·UI 헬퍼 모듈.

**다른 feature 에서 비슷한 기능 만들기 전에 먼저 본 README 검색.** 이미 있으면 재사용, 부족하면 본 모듈에 추가 후 갱신.

## 시나리오별 가이드

자주 마주치는 케이스 → 사용할 헬퍼.

### VM 이 화면에 알릴 일이 생겼을 때 (스낵바·화면 닫기 등)
**🚫 `Channel`/`MutableSharedFlow` + 화면에서 collect 하는 이벤트 스트림 X**
**✅ UiState 의 nullable 필드로 흡수 + 소비 콜백**

```kotlin
// VM: _uiState.update { it.copy(deleteResult = …) } / fun onDeleteResultConsumed()
LaunchedEffect(uiState.deleteResult) { … ; viewModel.onDeleteResultConsumed() }
```

> [Google 가이드](https://developer.android.com/topic/architecture/ui-layer/events) — «ViewModel 이벤트는 항상 UI state 갱신으로 이어져야 한다». producer(VM)가 consumer(UI)보다 오래 사는 순간 Channel/SharedFlow 는 전달을 보장하지 못한다(구성 변경·프로세스 사망·분할 화면). 이 레포는 #228 에서 이 방식을 정본으로 정했고, 선례는 `AfternoteDetailUiState.Success.deleteResult` 다.

### 단순 둥근 배경 컨테이너
**🚫 Material3 `Card` 사용 X** (앱 컨벤션)
**✅ `Column + clip(RoundedCornerShape) + background + padding` 체이닝**

```kotlin
Column(
    modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(AfternoteDesign.colors.gray1)
        .padding(16.dp),
) { /* ... */ }
```

### 디자인 시스템 컴포넌트
- **버튼**: [`AfternoteButton`](src/main/kotlin/com/afternote/core/ui/button/AfternoteButton.kt) (`Default`·`Active`·`Plain`·`Un`·`Variant5`) / [`AfternoteActionButton`](src/main/kotlin/com/afternote/core/ui/button/AfternoteButton.kt) / [`PlusBadgeButton`](src/main/kotlin/com/afternote/core/ui/button/PlusBadgeButton.kt)
- **TopBar**: [`HomeTopBar`](src/main/kotlin/com/afternote/core/ui/topbar/HomeTopBar.kt) / [`DetailTopBar`](src/main/kotlin/com/afternote/core/ui/topbar/DetailTopBar.kt) / [`TitleTopBar`](src/main/kotlin/com/afternote/core/ui/topbar/TitleTopBar.kt)
- **BottomBar**: [`BottomBar`](src/main/kotlin/com/afternote/core/ui/bottombar/BottomBar.kt)
- **Popup**: [`Popup(type, ...)`](src/main/kotlin/com/afternote/core/ui/popup/Popup.kt) (`Default`·`Variant2`) / [`AfternoteErrorPopup`](src/main/kotlin/com/afternote/core/ui/popup/ErrorPopup.kt) — 오류 팝업은 시안 4종 중 [`NetworkErrorPopup`](src/main/kotlin/com/afternote/core/ui/popup/ErrorPopup.kt) · [`ServerErrorPopup`](src/main/kotlin/com/afternote/core/ui/popup/ErrorPopup.kt) · [`UploadErrorPopup`](src/main/kotlin/com/afternote/core/ui/popup/ErrorPopup.kt) 3종을 세웠다(403 접근 권한 없음은 생산자가 없어 아직 없다) (#446)
- **TextField**: [`CaptionLabeledTextField`](src/main/kotlin/com/afternote/core/ui/CaptionLabeledTextField.kt) / [`AfternoteTextField`](src/main/kotlin/com/afternote/core/ui/TextFieldShort.kt)
- **Card / Section**: [`AfternoteOutlinedCard`](src/main/kotlin/com/afternote/core/ui/AfternoteOutlinedCard.kt) / [`AfternoteSectionHeader`](src/main/kotlin/com/afternote/core/ui/AfternoteSectionHeader.kt)
- **Profile**: [`ProfileImage`](src/main/kotlin/com/afternote/core/ui/ProfileImage.kt) / [`ProfileImagePicker`](src/main/kotlin/com/afternote/core/ui/ProfileImage.kt)
- **Calendar**: [`BottomSheetCalendar`](src/main/kotlin/com/afternote/core/ui/calendar/BottomSheetCalendar.kt)
- **Checkbox / Radio**: [`AfternoteCircularCheckbox`](src/main/kotlin/com/afternote/core/ui/button/AfternoteCircularCheckbox.kt) / [`AfternoteRadioGroup`](src/main/kotlin/com/afternote/core/ui/button/AfternoteRadioGroup.kt) (`selectedValue: T?` + `onSelect(T)` 단일 선택 계약)
  - 단품 [`CustomRadioButton`](src/main/kotlin/com/afternote/core/ui/button/CustomRadioButton.kt) 은 **`@Deprecated` — 신규 사용 금지.** 선택값을 소유하지 않아 단일 선택이 구조로 강제되지 않는다. `core:ui` 밖 신규 사용은 `SingleSelectionRadioKonsistTest` 가 막고, 잔여 사용처(setting #1396) 이관이 끝나면 선언을 걷는다 (#649).
- **FAB**: [`AfternoteFloatingActionButton`](src/main/kotlin/com/afternote/core/ui/button/FAB/AfternoteFloatingActionButton.kt) / [`PenFloatingActionButton`](src/main/kotlin/com/afternote/core/ui/button/FAB/PenFloatingActionButton.kt)
- **Icon**: [`CloseIcon`](src/main/kotlin/com/afternote/core/ui/icon/CloseIcon.kt) / [`RightArrowIcon`](src/main/kotlin/com/afternote/core/ui/icon/RightArrowIcon.kt)
- **Badge**: [`RecipientDesignationBadge`](src/main/kotlin/com/afternote/core/ui/badge/RecipientDesignationBadge.kt) / [`CircularCheckboxOutlineChip`](src/main/kotlin/com/afternote/core/ui/badge/CircularCheckboxOutlineChip.kt)
- **View mode 전환**: [`ViewModeSwitcher`](src/main/kotlin/com/afternote/core/ui/ViewModeSwitcher.kt)
- **한국어 자모 인덱스**: [`KoreanConsonantIndex`](src/main/kotlin/com/afternote/core/ui/KoreanConsonantIndex.kt)
- **단계형 흐름**: [`FlowStepScaffold`](src/main/kotlin/com/afternote/core/ui/scaffold/FlowStepScaffold.kt) / [`FlowStepProgressBar`](src/main/kotlin/com/afternote/core/ui/scaffold/FlowStepProgressBar.kt)
- **Modifier 확장**: [`addFocusCleaner`·`noRippleClickable`](src/main/kotlin/com/afternote/core/ui/modifierextention/ModifierExt.kt) / [`bottomBorder`·`dropShadow`·`horizontalFadingEdge`](src/main/kotlin/com/afternote/core/ui/modifierextention/DrawModifiers.kt) / [`shimmerLoadingPlaceholder`](src/main/kotlin/com/afternote/core/ui/modifierextention/ShimmerModifier.kt)

### 색·타입·테마
- [`AfternoteDesign.colors`](src/main/kotlin/com/afternote/core/ui/theme/Color.kt) — 디자인 토큰. **`Color(0xFF...)` 하드코드 X**, 토큰만 사용.
- [`AfternoteDesign.typography`](src/main/kotlin/com/afternote/core/ui/theme/Theme.kt) — `h1`/`h2`/`h3`/`bodyBase`/`bodySmallR`/`captionLargeB` 등
- [`AfternoteTheme { ... }`](src/main/kotlin/com/afternote/core/ui/theme/Theme.kt) — Preview·Screen root wrapper

### Navigation route
- [`Route`](src/main/kotlin/com/afternote/core/ui/Route.kt) — top-level route 정의 (`Home`/`Afternote`/`MindRecord`/`TimeLetter`/`Setting`/`Onboarding`/`Receiver` 등)

## 카테고리별 패키지 트리

```
core/ui/
├── theme/         색·타입·테마 토큰
├── button/        버튼·체크박스·라디오·FAB
├── topbar/        TopBar 3종
├── bottombar/     BottomBar
├── popup/         Popup 다이얼로그
├── calendar/      날짜 선택 BottomSheet
├── badge/         배지·칩
├── icon/          공용 아이콘
├── scaffold/      단계형 화면 Scaffold·진행 인디케이터
├── modifierextention/  공용 Modifier 확장
└── (root)         Card / TextField / Profile / Route 등
```

## 추가 시 약속

본 모듈에 새 헬퍼·컴포넌트 추가 시:
1. **본 README 의 "시나리오별 가이드" 또는 카테고리 섹션에 한 줄 추가**
2. KDoc 으로 "언제 사용" 명시
3. 다른 feature 에서 같은 기능 reinvent 되지 않도록 PR review 에서 catch

## Anti-patterns (놓치기 쉬운 룰)

| 안티패턴 | 권장 |
|---|---|
| `Material3 Card` 단순 컨테이너 | `Column + clip/background/padding` (위 참조) |
| `Color(0xFF...)` 하드코드 | `AfternoteDesign.colors.*` 토큰 |
| 라디오 인디케이터를 단품(`CustomRadioButton`)으로 조합해 선택 상태 직접 관리 | `AfternoteRadioGroup(selectedValue, onSelect)`로 단일 선택 구조화 (단품은 `@Deprecated`) |
| ViewModel 에 `Context` 주입 (string resource 용) | UiState 에 sealed error / `@StringRes Int?` 노출 → UI 가 `stringResource` 변환 |
| ViewModel 에 `Channel<Event>` / `SharedFlow<Event>` | UiState 의 nullable 필드 + `onXxxShown()` 콜백 |
| Repository 우회해 ViewModel 이 `DataSource` / cache 직접 주입 | Repository 만 진입점, cache 는 Repository 내부 책임 |
| Proxy UseCase (Repository 1:1 위임) | ViewModel 이 Repository 직접 주입 |
