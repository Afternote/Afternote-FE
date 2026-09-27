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

**Intent 하나가 ReducerEvent 를 0개에서 N개까지 낳는다.** 중복 요청을 가드에서 거절하면 0개, 로드 Intent 는 `Loading` → `Loaded` 로 2개다. 이 분리가 없으면 비동기 중간 상태를 표현할 곳이 없어 다시 `_uiState.value = ...` 로 돌아간다. 화면 이동만 하는 클릭은 아래 네비게이션 경계에 따라 콜백으로 전달한다.

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
internal fun FindIdScreen(viewModel: FindIdViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FindIdContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
private fun FindIdContent(state: FindIdUiState, onIntent: (FindIdIntent) -> Unit, modifier: Modifier = Modifier)
```

- `Screen` 은 stateful 이고 ViewModel 을 주입받는다.
- `Content` 는 상태와 콜백으로 화면을 그린다. 별도 프로덕션 `Screen` 파일에서 사용하는 렌더 계약은 `internal` 이며, 프리뷰 · screenshotTest · Robolectric 도 같은 계약을 소비한다.

### 파일럿에서 확정한 것 (#1802)

- **ViewModel 을 화면이 만들지 않는 경우가 있다.** onboarding 은 `Route.Onboarding` 그래프 스코프로 여러 화면이 한 인스턴스를 공유하므로, `Screen` 이 `hiltViewModel()` 을 부르지 않고 **VM 을 파라미터로 받는다.** 화면이 스스로 만들면 공유가 끊긴다. 공유가 없는 화면은 `viewModel: XxxViewModel = hiltViewModel()` 로 둔다.
- **수명·소비와 렌더 책임을 가른다.** onboarding 파일럿은 `FindIdScreen.kt` ↔ `FindIdContent.kt` 처럼 `Screen` 이 ViewModel 상태 수집과 신호 소비를 맡고, 별도 `Content` 파일이 렌더를 맡는다(#1829). 같은 파일 안에만 사용하는 렌더 helper 는 `private` 로 둔다.
- **플랫폼에 매인 콜백은 `Content` 의 파라미터로 남는다.** 카카오 SDK·Credential Manager 는 Activity·Context 의존이라 stateful 층이 토큰을 받아낸 뒤 문자열만 Intent 로 보낸다. ViewModel 은 플랫폼 독립을 유지한다.
- **공개 범위는 프로덕션 소비처로 정한다.** 같은 파일에서만 호출하면 `private`, 같은 모듈의 다른 파일이 호출하면 `internal`, 다른 모듈이 실제 소비할 때만 `public` 이다. 테스트 접근을 위해 넓히거나 visibility baseline 에 남기지 않는다. 파일럿의 다섯 `Content` 는 별도 `Screen` 파일의 실제 호출로 `internal` 이고, 이전 예외 baseline 은 제거했다(#1829).
- **파생값을 화면에 넘기지 않는다.** `isNextEnabled` 같은 값은 `UiState` 의 계산 프로퍼티로 두고 `Content` 가 `state` 에서 읽는다. 호출부가 따로 계산해 넘기면 화면마다 판정이 갈린다.

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

신호가 **값 없이 「올라갔다/내려갔다」 로만** 표현되면 `ObserveFlag` 를 쓴다 — `isLoggedIn` · `shouldNavigateToXxx` 처럼 나를 값이 없는 신호다. 안에서 `ObserveSignal` 로 접히므로 소비 규약은 하나다.

```kotlin
ObserveFlag(
    raised = state.isLoggedIn,
    consumed = LoginIntent.ConsumeLoggedIn,
    onIntent = onIntent,
    onRaised = onLoginSuccess,
)
```

`Effect` 타입 파라미터는 베이스에 없다. MVI 가 요구하는 것은 「일회성 효과를 상태 전이에서 분리한다」 까지고, 전달 수단은 아키텍처 계약 밖이다.

## 네비게이션과 만나는 경계 (#1810)

**ViewModel 은 작업과 상태를, 피처의 entry·host 는 목적지와 백스택을 소유한다.** MVI 전환은 네비게이션 콜백을 모두 Intent 로 바꾸는 작업이 아니다.

| 발생한 일 | 화면과 ViewModel | 피처 entry·host |
| --- | --- | --- |
| 뒤로가기·편집 화면 열기처럼 이동만 하는 클릭 | 기존 `onBackClick`·`onEditClick` 등 화면 콜백을 호출한다. 이동을 중계하기 위한 Intent 를 만들지 않는다. | 콜백을 해당 피처의 `NavActions` 에 연결해 로컬 스택을 조작한다. |
| 저장·삭제·인증처럼 작업 결과에 따라 이동하는 입력 | 작업 Intent 를 ViewModel 에 보낸다. ViewModel 은 결과 신호를 `UiState` 에 반영하고, 화면이 이를 관찰해 `onSaved` 같은 결과 콜백을 호출한다. | 결과 콜백을 받아 이동할 목적지와 pop·replace 범위를 결정한다. |
| 로컬 스택 바닥에서의 back·다른 피처로의 이동 | ViewModel 에 앱 루트나 다른 피처의 스택을 주입하지 않는다. | 로컬 host 가 셸에 제공된 경계 콜백으로 위임한다. |

### 작업 성공 신호의 소비

저장 성공 후 돌아가는 경우에는 다음 순서를 따른다.

1. 화면이 저장 Intent 를 보내고 ViewModel 이 작업을 실행한다.
2. ViewModel 이 성공을 `reduce` 로 상태에 기록한다. route·`NavController`·`NavBackStack` 을 실행하거나 보관하지 않는다.
3. 화면의 `ObserveSignal` 또는 기존 상태 관찰 어댑터가 성공 신호를 받아 결과 콜백을 호출한다. 실제 스택 변경은 그 콜백을 연결한 entry·host 가 수행한다.
4. 화면 어댑터가 `Intent.ConsumeXxx` 를 보내 성공 신호를 초기화한다. 재구성 때 같은 신호로 이동을 반복하지 않도록 결과 관찰과 소비를 함께 연결한다.

별도의 `MviEffect`·네비게이션 `Channel`·전역 `NavigationHelper` 는 도입하지 않는다. 기존 `LaunchedEffect` 기반 관찰도 같은 상태·소비 계약을 지키면 유지할 수 있다. 관찰 어댑터 이름을 통일하는 것까지 MVI 전환의 완료 조건으로 삼지 않는다.

### NavActions 와 화면 콜백

- `AfternoteNavActions`·`ReceivedAfternoteNavActions`·`MindRecordNavActions`·`SettingNavActions`·`TimeLetterNavActions`·`ReceiverNavActions`·`OnboardingNavActions` 는 피처의 네비게이션 계약으로 유지한다. MVI 전환만을 이유로 삭제하거나 ViewModel 에 주입하지 않는다.
- Navigation 3 이관은 각 계약의 구현을 로컬 스택에 연결한다. 이관에 필요한 계약 변경·분리는 해당 네비게이션 작업이 담당하며 MVI 의 선행 조건으로 기존 Actions 를 일괄 교체하지 않는다.
- `ReceiverHomeActions` 는 화면에서 셸로 전달하는 라우팅 콜백 묶음으로 유지한다. 수신자 홈 ViewModel 의 MVI 전환과 이 콜백 묶음의 소유권은 별개다.
- 화면의 이벤트 이름은 유지하고, 단순 back 위임은 각 그래프가 기능별 `popBack()` 에 매핑한다 (#1301). 모든 콜백을 공통 네비게이션 명령 이름으로 바꾸지 않는다.
- 콜백 332개 축소는 MVI 전환의 목표가 아니다. `Screen`/`Content` 분리 뒤에도 화면 이동에 필요한 콜백을 전달한다. 필수 네비게이션 콜백에 no-op 기본값을 추가하지 않는다.

### 이관 작업의 책임과 검증

MVI 작업은 Intent·상태 전이·결과 소비와 `Screen`/`Content` 연결을 바꾸고 기존 목적지·이동 콜백·백스택 동작을 보존한다. Navigation 3 작업은 entry·로컬 스택·복원·ViewModel 소유 범위와 셸 연결을 바꾸고 화면의 상태·결과 소비 계약을 보존한다. 두 작업이 같은 소비 경계를 바꾸면 확정된 선행 브랜치를 실제 base 로 사용해 연결을 검증한다.

ViewModel 테스트는 작업 결과 신호와 소비 후 초기화를, 화면 테스트는 결과 콜백과 소비 Intent 의 연결을 확인한다. 네비게이션 테스트는 콜백을 받은 뒤의 스택·복원·ViewModel 수명을 확인한다. 상태 신호만 확인한 테스트를 실제 이동 검증으로 대신하지 않는다.

이 경계는 현재 [공용 MVI 베이스](../../core/ui/src/main/kotlin/com/afternote/core/ui/mvi/MviViewModel.kt), [상태 신호 관찰](../../core/ui/src/main/kotlin/com/afternote/core/ui/mvi/ObserveSignal.kt), [수신자 host](../../feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/navigation/ReceiverNavHost.kt), [로컬 Actions](../../feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/navigation/ReceiverLocalNavActions.kt)의 역할을 문서화한다. 현재 호출 계약만으로 적용할 수 있어 전체 entry 이관이나 앱 루트 `NavDisplay` 전환 완료에 의존하지 않는다.

## `composable-callback-defaults.md` (#1388) 와의 관계

두 규칙은 충돌하지 않는다. MVI 의 상태 변경 입력은 `onIntent` 로 모으고, 네비게이션 콜백은 위 경계에 따라 유지한다. `Content` 는 `state`·`onIntent` 외에도 화면 이동에 필요한 콜백을 받을 수 있다.

- 전환한 화면에서 상태·비즈니스 작업 입력을 늘릴 때는 `Intent` 갈래를 추가한다. 갈래를 빠뜨리면 `when` 이 컴파일 에러를 낸다 — 디폴트를 없애 「누락 = 컴파일 에러」 를 만든 #1388 의 목적을 타입으로 더 강하게 지킨다.
- 네비게이션 콜백은 필수 인자로 전달하며, `= {}` 디폴트를 두지 않는 #1388 규칙을 그대로 따른다. 목적지·이동 방식은 피처 entry·host 가 결정한다.
- `core:ui` 리프 컴포넌트는 MVI 대상이 아니다. 거기서는 nullable 핸들러·오버로드로 선택성을 모델링하는 #1388 처분 기준이 그대로다.

## 강제

`MviContractKonsistTest` 가 셋을 본다.

| 규칙 | 내용 |
| --- | --- |
| A | `MviViewModel` 상속체는 `MutableStateFlow`·`MutableSharedFlow`·`Channel` 을 직접 선언하지 않는다 |
| B | `feature/*/presentation` 의 ViewModel 은 `MviViewModel` 을 상속한다 |
| C | `MviIntent`·`ReducerEvent` 를 직접 구현하는 타입은 `sealed interface` 다 |

규칙 B 는 아직 전환하지 않은 ViewModel 을 `PENDING_MVI_MIGRATION` 예외로 둔다(가드 도입 시점 49개, onboarding 파일럿 이후 46개). 모듈 전환 이슈가 닫힐 때마다 목록에서 빼고, **목록이 비면 예외 자체를 지운다.** `app` 의 ViewModel 2개는 규칙 B 의 대상이 아니다 — #1809 가 처리한다.

세 규칙은 **프로덕션 소스만** 본다. 테스트 더블이 `MviViewModel` 을 상속하며 보조 상태 홀더를 드는 것은
규칙 A 의 대상이 아니다 — 더블은 계약을 지키는 대상이 아니라 계약을 흉내 내는 도구다.

규칙 A·B 의 상속 판정은 **중간 추상 베이스를 낀 사슬까지** 따라간다. Konsist 의
`parents(indirectParents = true)` 로는 안 되고(0.17.3 에서 이 스코프 구성으로는 직계와 같은 목록을
돌려준다 — 실측), 스캔한 파일에서 이름 색인을 만들어 직접 걷는다.

### 가드가 «안» 보는 것

- **`onIntent` 밖의 public 진입점.** 상속체가 `fun refreshOnReturn()` 같은 public 함수를 노출해도
  세 규칙 어디도 막지 않는다. 화면이 ViewModel 을 직접 부르는 통로가 `onIntent` 하나여야 한다는 것은
  규약이지 CI 계약이 아니다. 리뷰에서 본다.
- **`reduce` 밖의 전이.** 규칙 A 는 «상태 홀더 선언» 을 막을 뿐, `dispatch` 를 거치지 않는 다른 경로를
  전수로 잡지는 못한다.

둘 다 #1801 의 완료 조건 밖이다. 넓힐 값이 생기면 별도 이슈로 다룬다.
