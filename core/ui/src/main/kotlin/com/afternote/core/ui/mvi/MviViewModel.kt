package com.afternote.core.ui.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * MVI 베이스 (#1800). 화면 ViewModel 은 이것을 상속해 진입점 하나([onIntent])와
 * 순수 전이 하나([reduce])만 갖는다.
 *
 * ```kotlin
 * @HiltViewModel
 * class FindIdViewModel @Inject constructor(...) :
 *     MviViewModel<FindIdIntent, FindIdUiState, FindIdReducerEvent>(FindIdUiState()) {
 *
 *     override fun onIntent(intent: FindIdIntent) {
 *         when (intent) {
 *             is FindIdIntent.UpdateEmail -> dispatch(FindIdReducerEvent.EmailChanged(intent.value))
 *             FindIdIntent.RequestCode -> requestVerificationCode()
 *             FindIdIntent.ConsumeError -> dispatch(FindIdReducerEvent.ErrorConsumed)
 *         }
 *     }
 *
 *     override fun reduce(state: FindIdUiState, event: FindIdReducerEvent): FindIdUiState =
 *         when (event) {
 *             is FindIdReducerEvent.EmailChanged -> state.copy(email = event.value)
 *             FindIdReducerEvent.ErrorConsumed -> state.copy(errorMessage = null)
 *         }
 * }
 * ```
 *
 * ## 왜 베이스가 필요한가 (develop 실측)
 *
 * 단일 `UiState` 노출은 이미 32개 화면에 정착했지만 그 아래가 갈려 있었다. `mutateForm` 처럼
 * 순수 리듀서에 가까운 곳과 코루틴 안에서 `_uiState.value = ...` 를 직접 쓰는 곳이 섞였고,
 * 진입점을 하나로 모은 화면은 51개 중 1개뿐이었다. 상태 홀더를 베이스가 감추면 전이가
 * [reduce] 밖으로 샐 통로 자체가 없어진다 — konsist 규칙 A 가 이것을 강제한다(#1801).
 *
 * ## 상태를 바꾸는 길은 [dispatch] 하나다
 *
 * [uiState] 의 뒷단은 `private` 이고 [currentState] 는 읽기 전용이라, 상속체가 상태를 바꿀
 * 방법은 [dispatch] → [reduce] 밖에 없다. 비동기 작업은 중간 상태를 [ReducerEvent] 로 낸다
 * (`Loading` 을 dispatch → suspend 호출 → `Loaded` 를 dispatch).
 */
abstract class MviViewModel<I : MviIntent, S : UiState, E : ReducerEvent>(
    initialState: S,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)

    val uiState: StateFlow<S> = _uiState.asStateFlow()

    /**
     * 지금 상태. 비동기 작업을 시작하기 전에 입력값·가드를 읽는 용도다.
     *
     * 읽은 값으로 다음 상태를 계산해 다시 쓰지 않는다 — 그 계산은 [reduce] 의 몫이다.
     */
    protected val currentState: S get() = _uiState.value

    /**
     * 화면이 부르는 **유일한** 진입점. 소비 신호도 여기로 들어온다(`Intent.ConsumeXxx`).
     *
     * `when (intent)` 를 `else` 없이 전수 분기해, Intent 가 늘면 컴파일이 빠진 분기를 알린다.
     */
    abstract fun onIntent(intent: I)

    /**
     * 순수 전이. 같은 ([state], [event]) 에는 항상 같은 결과여야 한다.
     *
     * [MutableStateFlow.update] 는 경합 시 람다를 다시 부르므로, 부수효과를 여기 두면 그
     * 부수효과가 두 번 일어난다. 저장소 호출 · 로깅 · 계측은 [onIntent] 쪽에 둔다.
     */
    protected abstract fun reduce(
        state: S,
        event: E,
    ): S

    /** [event] 를 [reduce] 에 흘려 상태를 갱신한다. 상태를 바꾸는 유일한 수단이다. */
    protected fun dispatch(event: E) {
        _uiState.update { reduce(it, event) }
    }
}
