# MVI 규칙

**화면 ViewModel 은 `MviViewModel` 을 상속해 진입점 하나(`onIntent`)와 순수 전이 하나(`reduce`)만 갖는다.**

`feature/*/presentation` 의 ViewModel 에 적용하고, `konsist` 의 `MviContractKonsistTest` 가 강제한다 (#1801).
베이스와 마커는 `core/ui` 의 `com.afternote.core.ui.mvi` 에 있다 (#1800).

## 왜인가

단일 `UiState` 노출은 이미 32개 화면에 정착했다(sealed 19 / data 13). 그런데 그 아래가 갈려 있었다.

- **전이 경로가 두 갈래였다.** `AfternoteEditorViewModel.mutateForm` 은 순수 리듀서에 가까운데(`withXxx` 가 전부 `copy()`), `ReceiverHomeViewModel` 은 코루틴 안에서 `_uiState.value = ReceiverHomeUiState.Error(...)` 를 직접 썼다. 전이가 어디서 일어나는지 화면마다 달랐다.
- **소비 함수가 화면마다 public fun 이었다.** `onErrorConsumed()` 를 부르지 않아도 컴파일은 통과한다 — 신호가 남은 채 다음 실패를 덮는다.
- **단일 진입점은 51개 중 1개였다.** `ReceiverHomeViewModel.onEvent` 하나.

## 3타입 — Intent · UiState · ReducerEvent

```kotlin
sealed interface FindIdIntent : MviIntent {
    data class UpdateEmail(val value: String) : FindIdIntent
    data object RequestCode : FindIdIntent
    data object ConsumeError : FindIdIntent
}

data class FindIdUiState(
    val email: String = "",
    val isSendingCode: Boolean = false,
    val errorMessage: UiText? = null,        // 일회성 신호도 상태다 (#228)
) : UiState

sealed interface FindIdReducerEvent : ReducerEvent {
    data class EmailChanged(val value: String) : FindIdReducerEvent
    data object SendingCode : FindIdReducerEvent
    data class SendFailed(val message: UiText) : FindIdReducerEvent
    data object ErrorConsumed : FindIdReducerEvent
}
```

### Intent 와 ReducerEvent 를 가르는 기준

| | Intent | ReducerEvent |
| --- | --- | --- |
| 무엇인가 | 사용자가 **하려는 것** | 상태가 **겪은 것** |
| 누가 만드는가 | 화면 | ViewModel 만 |
| 예 | `RequestCode` · `SelectFilter` | `SendingCode` · `Loaded` · `SendFailed` |

**Intent 하나가 ReducerEvent 를 0개에서 N개까지 낳는다.** 네비게이션만 하는 Intent 는 0개, 로드 Intent 는 `Loading` → `Loaded` 로 2개다. 이 분리가 없으면 비동기 중간 상태를 표현할 곳이 없어 다시 `_uiState.value = ...` 로 돌아간다.

### 부수효과는 `onIntent` 에, 전이는 `reduce` 에

```kotlin
override fun onIntent(intent: FindIdIntent) {
    when (intent) {
        is FindIdIntent.UpdateEmail -> dispatch(FindIdReducerEvent.EmailChanged(intent.value))
        FindIdIntent.RequestCode -> requestCode()
        FindIdIntent.ConsumeError -> dispatch(FindIdReducerEvent.ErrorConsumed)
    }
}

private fun requestCode() {
    if (!currentState.isSendCodeEnabled) return          // 가드는 currentState 를 읽는다
    viewModelScope.launch {
        dispatch(FindIdReducerEvent.SendingCode)          // 중간 상태도 event 다
        accountRepository.sendFindCode(currentState.email)
            .onFailure { dispatch(FindIdReducerEvent.SendFailed(it.toDisplayMessage())) }
    }
}
```

`reduce` 는 저장소 호출·로깅·계측을 하지 않는다. `MutableStateFlow.update` 는 경합하면 람다를 다시 부르므로, 부수효과를 리듀서에 두면 그 부수효과가 두 번 일어난다.

`when` 에 `else` 를 두지 않는다. 갈래가 늘면 컴파일이 빠진 분기를 알려야 한다 (#1771 과 같은 방향).

## 화면은 `Screen` / `Content` 2단이다

```kotlin
@Composable
fun FindIdScreen(modifier: Modifier = Modifier, viewModel: FindIdViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FindIdContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun FindIdContent(state: FindIdUiState, onIntent: (FindIdIntent) -> Unit, modifier: Modifier = Modifier)
```

- `Screen` 은 stateful 이고 ViewModel 을 주입받는다.
- `Content` 는 `internal` stateless 이고, **프리뷰 · screenshotTest · Robolectric 의 진입점**이다. 화면을 그리려고 ViewModel 을 조립하지 않는다.

## 일회성 신호 — `UiState` 흡수 + `Intent.ConsumeXxx`

**`Channel`·`MutableSharedFlow` 를 쓰지 않는다.** producer(ViewModel)가 consumer(UI)보다 오래 사는 순간 `Channel` 은 전달을 보장하지 못한다 — 구성 변경·프로세스 사망·분할 화면이다([공식 가이드](https://developer.android.com/topic/architecture/ui-layer/events#handle-viewmodel-events)). 이 저장소의 정본 규약은 #228 이고 MVI 와 충돌하지 않는다. **바뀌는 것은 소비 경로뿐이다.**

```kotlin
ObserveSignal(
    signal = state.errorMessage,
    consumed = FindIdIntent.ConsumeError,
    onIntent = onIntent,
) { message -> showSnackbar(message) }
```

- **`onXxxConsumed()` 를 public fun 으로 노출하지 않는다.** 진입점이 화면 수만큼 늘고, 배선을 빠뜨려도 컴파일이 통과해 신호가 남은 채 다음 실패를 덮는다. `Intent.ConsumeXxx` 로 접으면 소비도 `onIntent` 라는 같은 문을 지난다.
- **소비가 신호를 null 로 되돌리므로 같은 값이 연속으로 와도 두 번 소비된다** (`A → null → A`). reset 없이 같은 값을 다시 쓰면 두 번째는 조용히 묻힌다.
- `onSignal` 안에서 suspend 를 직접 기다리지 않는다. 소비 직후의 상태 변화가 `LaunchedEffect` 를 재시작시켜 이전 코루틴을 취소한다 — 스낵바처럼 시간이 걸리는 표출은 `rememberCoroutineScope()` 에 launch 한다.

`Effect` 타입 파라미터는 베이스에 없다. MVI 가 요구하는 것은 「일회성 효과를 상태 전이에서 분리한다」 까지고, 전달 수단은 아키텍처 계약 밖이다.

## `composable-callback-defaults.md` (#1388) 와의 관계

두 규칙은 충돌하지 않는다. **MVI 화면은 콜백 파라미터 자체가 사라져 no-op 디폴트를 둘 자리가 없어진다** — `Content` 가 받는 것은 `state` 와 `onIntent` 둘뿐이다.

- 전환한 화면에서 상호작용을 늘릴 때 **콜백 파라미터를 되살리지 않는다.** `Intent` 갈래를 하나 더한다. 갈래를 빠뜨리면 `when` 이 컴파일 에러를 낸다 — 디폴트를 없애 「누락 = 컴파일 에러」 를 만든 #1388 의 목적을 타입으로 더 강하게 지킨다.
- 네비게이션 콜백은 **전환 범위 밖이다.** 목적지 결정은 Navigation 3 이관 계열이 정한다(#1810 에서 접점 판정). 그때까지 `Screen` 이 받는 네비게이션 콜백은 기존 방식·기존 개수를 유지하고, `= {}` 디폴트를 두지 않는 #1388 규칙을 그대로 따른다.
- `core:ui` 리프 컴포넌트는 MVI 대상이 아니다. 거기서는 nullable 핸들러·오버로드로 선택성을 모델링하는 #1388 처분 기준이 그대로다.

## 강제

`MviContractKonsistTest` 가 셋을 본다.

| 규칙 | 내용 |
| --- | --- |
| A | `MviViewModel` 상속체는 `MutableStateFlow`·`MutableSharedFlow`·`Channel` 을 직접 선언하지 않는다 |
| B | `feature/*/presentation` 의 ViewModel 은 `MviViewModel` 을 상속한다 |
| C | `MviIntent`·`ReducerEvent` 를 직접 구현하는 타입은 `sealed interface` 다 |

규칙 B 는 전환 전 ViewModel 49개를 `PENDING_MVI_MIGRATION` 예외로 둔다. 모듈 전환 이슈가 닫힐 때마다 목록에서 빼고, **목록이 비면 예외 자체를 지운다.** `app` 의 ViewModel 2개는 규칙 B 의 대상이 아니다 — #1809 가 처리한다.
