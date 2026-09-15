package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoParentDeclaration
import com.lemonappdev.konsist.api.provider.KoParentProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * MVI 계약 회귀 가드 (#1801).
 *
 * 강제 없이 시작하면 반쪽 도입으로 파편화만 늘어난다 — 단일 `UiState` 노출은 32개 화면에
 * 정착했는데 그 아래 전이 경로는 화면마다 갈렸고, 진입점을 하나로 모은 화면은 51개 중
 * 1개뿐이었다. 규약을 문서로만 두면 같은 일이 반복된다.
 *
 * 규칙은 셋이다.
 * - **A.** `MviViewModel` 상속체는 `MutableStateFlow`·`MutableSharedFlow`·`Channel` 을 직접
 *   선언하지 않는다. 별도 상태 홀더를 두면 전이가 `reduce` 밖으로 새고 베이스를 도입한 의미가
 *   사라진다. `Channel` 금지는 #1502 가 세우려는 규칙과 같은 방향이라 **두 규칙을 하나로 합친다** —
 *   따로 두면 나중에 서로를 덮는다.
 * - **B.** `feature/…/presentation` 의 ViewModel 은 `MviViewModel` 을 상속한다. 전환 전
 *   49개는 [PENDING_MVI_MIGRATION] 예외로 두고, 모듈 전환 이슈가 닫힐 때마다 뺀다.
 * - **C.** `MviIntent`·`ReducerEvent` 를 직접 구현하는 화면 계약 타입은 `sealed interface` 다.
 *   열려 있으면 `when` 이 전수 분기를 보장하지 못해 진입점 단일화의 이득이 사라진다.
 *
 * 기준 문서는 `docs/convention/mvi.md` 다.
 *
 * ### `Effect` 규칙이 없는 이유
 * 이슈 본문의 규칙 C 는 `Effect` 를 함께 적었지만, #1800 이 베이스에서 `Effect` 타입
 * 파라미터를 뺐다(3타입). 일회성 신호는 `UiState` 의 nullable 필드로 나르는 것이 이 저장소의
 * 정본 규약(#228)이라, 존재하지 않는 타입에 규칙을 걸지 않는다.
 *
 * ### [PENDING_MVI_MIGRATION]
 * 해소된 항목이 남으면 **경고만** 낸다 — 전환 PR 과 목록 정리 PR 이 서로를 깨뜨리지 않게
 * 하는 이 저장소의 관례다([ResponseDtoContractKonsistTest] #933 전례).
 */
class MviContractKonsistTest {
    @get:Rule
    val fixture = TemporaryFolder()

    @Test
    fun `MviViewModel 상속체는 상태 홀더를 직접 선언하지 않는다`() {
        val violations = stateHolderDeclarations(AfternoteKonsistScope.productionFiles)

        check(violations.isEmpty()) {
            buildString {
                appendLine("MviViewModel 상속체가 상태 홀더를 직접 선언한다 (${violations.size}건).")
                appendLine("전이가 reduce 밖으로 새면 베이스를 상속한 의미가 없다 (#1800).")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("상태는 dispatch(ReducerEvent) 로만 바꾸고, 일회성 신호는 UiState 의")
                appendLine("nullable 필드로 흡수한다 (#228). 기준: docs/convention/mvi.md")
            }
        }
    }

    @Test
    fun `feature presentation 의 ViewModel 은 MviViewModel 을 상속한다`() {
        val violations = unmigratedViewModels(AfternoteKonsistScope.productionFiles) - PENDING_MVI_MIGRATION

        check(violations.isEmpty()) {
            buildString {
                appendLine("MviViewModel 을 상속하지 않는 ViewModel 이 새로 생겼다 (${violations.size}건).")
                appendLine("진입점이 화면마다 갈리면 소비 함수를 빠뜨려도 컴파일이 통과한다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("MviViewModel<Intent, UiState, ReducerEvent> 를 상속해 onIntent 하나로 모은다.")
                appendLine("기준: docs/convention/mvi.md")
            }
        }
    }

    @Test
    fun `해소된 항목은 경고로 알린다`() {
        val stale = PENDING_MVI_MIGRATION - unmigratedViewModels(AfternoteKonsistScope.productionFiles)
        if (stale.isEmpty()) return

        println(
            buildString {
                appendLine("[경고] PENDING_MVI_MIGRATION 에 지금 소스에 없는 항목이 남아 있다 (${stale.size}건).")
                appendLine("전환이 끝났거나(그러면 지운다), 아직 도착하지 않은 선등재다(PENDING_ARRIVAL_*).")
                appendLine("목록에서 지워야 다음 미전환 ViewModel 이 이 자리에 숨지 않는다.")
                appendLine("목록이 비면 규칙 B 의 예외 자체를 지운다.")
                appendLine()
                stale.sorted().forEach { appendLine("  $it") }
            },
        )
    }

    @Test
    fun `Intent 와 ReducerEvent 는 sealed interface 다`() {
        val violations = openContractTypes(AfternoteKonsistScope.productionFiles)

        check(violations.isEmpty()) {
            buildString {
                appendLine("MviIntent·ReducerEvent 를 sealed interface 밖에서 구현한다 (${violations.size}건).")
                appendLine("열려 있으면 onIntent·reduce 의 when 이 전수 분기를 보장하지 못한다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("화면 계약은 `sealed interface XxxIntent : MviIntent` 로 닫고,")
                appendLine("각 갈래를 그 안의 data object·data class 로 둔다. 기준: docs/convention/mvi.md")
            }
        }
    }

    @Test
    fun `규칙 A - 베이스를 상속한 곳의 상태 홀더만 잡는다`() {
        val root = fixture.newFolder("rule-a")
        root.writeKotlin(
            "feature/sample/presentation/src/main/kotlin/sample/HolderViewModel.kt",
            """
            package sample

            class HolderViewModel : MviViewModel<SampleIntent, SampleUiState, SampleEvent>(SampleUiState()) {
                private val _extra = MutableStateFlow(0)
                private val signals = Channel<String>()
                private val plain = 0
            }
            """,
        )
        root.writeKotlin(
            "feature/sample/presentation/src/main/kotlin/sample/LegacyViewModel.kt",
            """
            package sample

            class LegacyViewModel : ViewModel() {
                private val _uiState = MutableStateFlow(SampleUiState())
            }
            """,
        )

        val violations = stateHolderDeclarations(fixtureFiles(root))

        // 전환 전 ViewModel 의 상태 홀더는 규칙 A 의 대상이 아니다 — 그건 규칙 B 가 본다.
        assertEquals(2, violations.size)
        check(violations.any { it.endsWith("HolderViewModel._extra") }) { violations.toString() }
        check(violations.any { it.endsWith("HolderViewModel.signals") }) { violations.toString() }
    }

    @Test
    fun `규칙 B - feature presentation 의 미전환 ViewModel 만 잡는다`() {
        val root = fixture.newFolder("rule-b")
        root.writeKotlin(
            "feature/sample/presentation/src/main/kotlin/sample/LegacyViewModel.kt",
            """
            package sample

            class LegacyViewModel : ViewModel()
            """,
        )
        root.writeKotlin(
            "feature/sample/presentation/src/main/kotlin/sample/MigratedViewModel.kt",
            """
            package sample

            class MigratedViewModel : MviViewModel<SampleIntent, SampleUiState, SampleEvent>(SampleUiState())
            """,
        )
        root.writeKotlin(
            "feature/sample/presentation/src/main/kotlin/sample/BaseViewModel.kt",
            """
            package sample

            abstract class BaseViewModel : ViewModel()
            """,
        )
        root.writeKotlin(
            "app/src/main/kotlin/app/AppViewModel.kt",
            """
            package app

            class AppViewModel : ViewModel()
            """,
        )

        val violations = unmigratedViewModels(fixtureFiles(root))

        // app 은 규칙 B 의 대상이 아니고(#1809 몫), 추상 베이스와 전환된 것도 위반이 아니다.
        assertEquals(setOf("sample.LegacyViewModel"), violations)
    }

    @Test
    fun `규칙 C - 마커를 직접 구현하는 열린 타입만 잡는다`() {
        val root = fixture.newFolder("rule-c")
        root.writeKotlin(
            "feature/sample/presentation/src/main/kotlin/sample/SampleContract.kt",
            """
            package sample

            sealed interface SampleIntent : MviIntent {
                data object Submit : SampleIntent
            }

            interface LooseIntent : MviIntent

            class EventCarrier : ReducerEvent

            data object Dismiss : ReducerEvent

            sealed interface SampleEvent : ReducerEvent
            """,
        )

        val violations = openContractTypes(fixtureFiles(root))

        // sealed 하위 갈래는 마커가 아니라 자기 계약 타입을 구현하므로 대상이 아니다.
        // 최상위 `data object` 는 KoObjectDeclaration 이라 classes() 에 안 들어오는데,
        // `class EventCarrier` 와 계약상 같은 구멍이므로 함께 잡아야 한다.
        assertEquals(3, violations.size)
        check(violations.any { it.endsWith("interface LooseIntent") }) { violations.toString() }
        check(violations.any { it.endsWith("class EventCarrier") }) { violations.toString() }
        check(violations.any { it.endsWith("object Dismiss") }) { violations.toString() }
    }

    @Test
    fun `규칙 A·B - 중간 추상 베이스를 낀 상속도 MviViewModel 로 본다`() {
        val root = fixture.newFolder("indirect-parents")
        root.writeKotlin(
            "feature/sample/presentation/src/main/kotlin/sample/BaseFooViewModel.kt",
            """
            package sample

            abstract class BaseFooViewModel<I, S, E>(initial: S) : MviViewModel<I, S, E>(initial)
            """,
        )
        root.writeKotlin(
            "feature/sample/presentation/src/main/kotlin/sample/FooViewModel.kt",
            """
            package sample

            class FooViewModel : BaseFooViewModel<SampleIntent, SampleUiState, SampleEvent>(SampleUiState()) {
                private val nav = Channel<String>()
            }
            """,
        )

        val files = fixtureFiles(root)

        // 규칙 A — 직계만 보면 이 Channel 을 통째로 놓친다.
        val holders = stateHolderDeclarations(files)
        assertEquals(1, holders.size)
        check(holders.single().endsWith("FooViewModel.nav")) { holders.toString() }

        // 규칙 B — 같은 이유로 정상 MVI ViewModel 을 「미전환」으로 헛짚지 않아야 한다.
        assertEquals(emptySet<String>(), unmigratedViewModels(files))
    }

    /** 규칙 B 위반 후보 — `feature/…/presentation` main 소스의 미전환 ViewModel FQN. */
    private fun unmigratedViewModels(files: List<KoFileDeclaration>): Set<String> {
        val extendsMvi = mviViewModelSubclassTest(files)
        return files
            .filter { FEATURE_PRESENTATION_MAIN.containsMatchIn(it.normalizedProjectPath()) }
            .flatMap { file ->
                file
                    .classes()
                    .filter { it.name.endsWith(VIEW_MODEL_SUFFIX) }
                    .filterNot { it.hasAbstractModifier }
                    .filterNot(extendsMvi)
                    .map { "${file.packagee?.name}.${it.name}" }
            }.toSet()
    }

    /** 규칙 A 위반 — 베이스를 상속한 ViewModel 이 직접 든 상태 홀더. */
    private fun stateHolderDeclarations(files: List<KoFileDeclaration>): List<String> {
        val extendsMvi = mviViewModelSubclassTest(files)
        return files.flatMap { file ->
            file
                .classes()
                .filter(extendsMvi)
                .flatMap { declaration ->
                    declaration
                        .properties()
                        .filter { property -> STATE_HOLDER.containsMatchIn(property.text) }
                        .map { property -> "${file.normalizedProjectPath()} — ${declaration.name}.${property.name}" }
                }
        }
    }

    /** 규칙 C 위반 — 마커를 직접 구현하는데 `sealed interface` 가 아닌 타입. */
    private fun openContractTypes(files: List<KoFileDeclaration>): List<String> =
        files.flatMap { file ->
            val openInterfaces =
                file
                    .interfaces()
                    .filter { it.implementsMarker() }
                    .filterNot { it.hasSealedModifier }
                    .map { "${file.normalizedProjectPath()} — interface ${it.name}" }
            val classes =
                file
                    .classes()
                    .filter { it.implementsMarker() }
                    .map { "${file.normalizedProjectPath()} — class ${it.name}" }
            // object 선언은 KoObjectDeclaration 이라 classes() 에 안 들어온다. 최상위
            // `data object Dismiss : ReducerEvent` 는 `class EventCarrier : ReducerEvent` 와
            // 계약상 같은 구멍이므로 선언 종류로 갈라 판정하지 않는다.
            val objects =
                file
                    .objects()
                    .filter { it.implementsMarker() }
                    .map { "${file.normalizedProjectPath()} — object ${it.name}" }
            openInterfaces + classes + objects
        }

    /**
     * 상속 사슬을 **직접 걷는다.**
     *
     * 중간 추상 베이스를 하나 끼우면(`class Foo : BaseFoo`, `abstract class BaseFoo : MviViewModel`)
     * 직계 판정으로는 규칙 A 의 대상에서 빠져 상태 홀더를 놓치고, 규칙 B 는 그 정상 ViewModel 을
     * 「미전환」으로 헛짚는다.
     *
     * **Konsist 의 `parents(indirectParents = true)` 로는 안 닫힌다** — 0.17.3 에서 이 스코프
     * 구성으로는 직계와 **같은 목록**을 돌려준다. 마커를 같은 스코프에 정의해 두고 재어도 그렇다.
     * 그래서 스캔한 파일에서 이름→선언 색인을 만들어 사슬을 직접 따라간다.
     */
    private fun mviViewModelSubclassTest(files: List<KoFileDeclaration>): (KoClassDeclaration) -> Boolean {
        val byName = files.flatMap { it.classes() }.associateBy { it.name }

        fun extends(
            declaration: KoClassDeclaration,
            seen: MutableSet<String>,
        ): Boolean {
            if (!seen.add(declaration.name)) return false
            return declaration.parents().any { parent ->
                val parentName = parent.markerName()
                parentName == MVI_VIEW_MODEL || byName[parentName]?.let { extends(it, seen) } == true
            }
        }

        return { extends(it, mutableSetOf()) }
    }

    /** 가드마다 같은 형태로 두는 경로 정규화 — `projectPath` 는 OS 구분자와 선행 `/` 가 섞인다. */
    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private fun File.writeKotlin(
        path: String,
        content: String,
    ) {
        val target = File(this, path)
        target.parentFile.mkdirs()
        target.writeText(content.trimIndent())
    }

    private fun fixtureFiles(root: File): List<KoFileDeclaration> = AfternoteKonsistScope.scanExternalDirectories(listOf(root)).files

    private companion object {
        const val MVI_VIEW_MODEL = "MviViewModel"
        const val VIEW_MODEL_SUFFIX = "ViewModel"

        /** 시작에 붙이지 않는다 — 회귀 fixture 는 저장소 밖 임시 디렉터리 밑에서 같은 구조를 만든다. */
        val FEATURE_PRESENTATION_MAIN = Regex("""feature/[^/]+/presentation/src/main/""")

        /**
         * 상태 홀더 선언. 타입을 생략하고 `= MutableStateFlow(...)` 로 추론시키는 것이 이 저장소의
         * 관례라, 선언 타입만 보면 대부분을 놓친다. 그래서 선언 텍스트에서 찾는다.
         */
        val STATE_HOLDER = Regex("""\b(MutableStateFlow|MutableSharedFlow|Channel)\s*[(<]""")

        /** #1804 가 뺀다. */
        private val ISSUE_1804_AFTERNOTE =
            setOf(
                "com.afternote.feature.afternote.presentation.AfternoteHostViewModel",
                "com.afternote.feature.afternote.presentation.detail.AfternoteDetailViewModel",
                "com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel",
                "com.afternote.feature.afternote.presentation.editor.memorial.AddSongViewModel",
                "com.afternote.feature.afternote.presentation.editor.receiver.SelectReceiverViewModel",
                "com.afternote.feature.afternote.presentation.home.AfternoteHomeViewModel",
                "com.afternote.feature.afternote.presentation.receiver.afternotelist.ReceiverAfternoteHomeViewModel",
                "com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel",
                "com.afternote.feature.afternote.presentation.receiver.playlist.ReceiverMemorialPlaylistViewModel",
            )

        /** #1808 이 뺀다. */
        private val ISSUE_1808_HOME =
            setOf(
                "com.afternote.feature.home.presentation.HomeTabViewModel",
                // #1666 이 receiver.presentation.home 에서 옮겼다 — 소속 이슈도 #1803 이 아니라 여기다.
                "com.afternote.feature.home.presentation.receiver.ReceiverHomeViewModel",
            )

        /** #1807 이 뺀다. */
        private val ISSUE_1807_MINDRECORD =
            setOf(
                "com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.DraftListViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.MemoriesCardViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.MemorySpaceViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.RecordDetailViewModel",
                "com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportViewModel",
            )

        /**
         * 아직 develop 에 없다. #457(PR #1624, 승인 완료)이 들여오는 네 번째 onboarding ViewModel 이라,
         * 이 가드가 먼저 머지되면 규칙이 생기기 전에 쓰인 그 PR 이 규칙 B 로 빨개진다. 머지 순서가
         * 어느 쪽이든 develop 이 red 가 되지 않도록 미리 등재한다 — 그때까지는 「해소된 항목」 경고로만
         * 남는다. 전환은 #1802 후속 몫이다.
         */
        private val PENDING_ARRIVAL_ONBOARDING =
            setOf(
                "com.afternote.feature.onboarding.presentation.findaccount.FindPasswordViewModel",
            )

        /** #1803 이 뺀다. */
        private val ISSUE_1803_RECEIVER =
            setOf(
                "com.afternote.feature.receiver.presentation.deliveryverification.DeliveryVerificationFlowViewModel",
                "com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadViewModel",
                "com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationViewModel",
                "com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyViewModel",
                // 이 스택의 base 에는 아직 여기 있다. develop 은 #1666 으로 feature/home 으로 옮겼고
                // 그쪽 FQN 은 ISSUE_1808_HOME 에 있다 — 스택이 develop 을 들이면 이 줄을 지운다.
                "com.afternote.feature.receiver.presentation.home.ReceiverHomeViewModel",
                "com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsViewModel",
                "com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationViewModel",
                "com.afternote.feature.receiver.presentation.senderdetail.SenderDetailViewModel",
            )

        /** #1805 가 뺀다. `Channel` 5곳 흡수(#1502)가 선행이다. */
        private val ISSUE_1805_SETTING =
            setOf(
                "com.afternote.feature.setting.presentation.viewmodel.AppLockSetupViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.InsertPasswordViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.PassKeyViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.ProfileEditViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.PushNotificationViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.ReceiverEditViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.ReceiverListViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.ReceiverRegisterViewModel",
                "com.afternote.feature.setting.presentation.viewmodel.SettingViewModel",
            )

        /** #1806 이 뺀다. */
        private val ISSUE_1806_TIMELETTER =
            setOf(
                "com.afternote.feature.timeletter.presentation.viewmodel.DraftLetterViewModel",
                "com.afternote.feature.timeletter.presentation.viewmodel.RecipientListViewModel",
                "com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeLetterDetailViewModel",
                "com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeletterViewModel",
                "com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterDetailViewModel",
                "com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteViewModel",
                "com.afternote.feature.timeletter.presentation.viewmodel.TimeletterViewModel",
            )

        /**
         * 전환 전 46개 — onboarding 3개는 #1802 파일럿이 전환해 빠졌다. `app` 의 ViewModel 2개는 이 규칙의 대상이 아니라 목록에도 없다 —
         * 규칙 B 가 `feature/…/presentation` 만 보기 때문이고, 그 2개는 #1809 가 처리한다.
         *
         * 목록이 비면 규칙 B 의 예외(`- PENDING_MVI_MIGRATION`)도 함께 지운다.
         */
        val PENDING_MVI_MIGRATION =
            PENDING_ARRIVAL_ONBOARDING +
                ISSUE_1803_RECEIVER +
                ISSUE_1804_AFTERNOTE +
                ISSUE_1805_SETTING +
                ISSUE_1806_TIMELETTER +
                ISSUE_1807_MINDRECORD +
                ISSUE_1808_HOME
    }
}

/** `MviViewModel<A, B, C>` 처럼 타입 인자가 붙어도 이름만 남긴다. */
private fun KoParentDeclaration.markerName(): String = name.substringBefore('<').trim()

/**
 * **직계 부모만 본다.** 간접까지 보면 `sealed interface XxxIntent : MviIntent` 의 하위 갈래
 * (`data object Submit : XxxIntent`)가 전부 「마커를 직접 구현」으로 잡혀 규칙 C 가 자기 처방을
 * 위반으로 신고한다 — fixture 로 확인했다.
 */
private fun KoParentProvider.implementsMarker(): Boolean = parents().any { it.markerName() in MVI_MARKERS }

private val MVI_MARKERS = setOf("MviIntent", "ReducerEvent")
