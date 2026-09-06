package com.afternote.konsist

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.declaration.KoTypeAliasDeclaration
import com.lemonappdev.konsist.api.provider.modifier.KoVisibilityModifierProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 테스트 참조가 프로덕션 최상위 선언의 공개 범위를 넓히지 못하게 하는 회귀 가드 (#1678).
 *
 * Kotlin 컴파일러 explicit API 모드는 암묵적 public을 확실히 찾지만, 명시한 `internal`·`public`이
 * 실제 사용 범위보다 넓은지는 판단하지 않는다. 이 가드는 그 빈틈 중 정적 참조를 안전하게 식별할
 * 수 있는 **최상위 선언**만 보완한다.
 *
 * 검사 조건은 모두 만족해야 한다.
 *
 * 1. `private`가 아닌 프로덕션 최상위 함수·프로퍼티·클래스·object다.
 * 2. 다른 프로덕션 파일은 해당 FQN을 참조하지 않는다.
 * 3. 테스트 소스가 해당 FQN을 직접 import하거나 같은 패키지에서 이름을 참조한다.
 *
 * member·constructor의 최소 visibility는 Kotlin 의미 분석 없이는 동명이인과 생성 코드 사용을
 * 안전하게 구분할 수 없다. 그 경계는 #1671~#1677과 코드 리뷰가 담당한다. 단 **PR 이 새로 넣은**
 * 멤버 함수가 main 참조 없이 테스트만 참조하는 경우는 Repository Quality 의
 * `validate-test-only-production-declarations.mjs` 가 PR diff 단위로 막는다 (#1895).
 */
class ProductionVisibilityKonsistTest {
    @get:Rule
    val fixture = TemporaryFolder()

    @Test
    fun `테스트만 직접 참조하는 새 프로덕션 선언을 허용하지 않는다`() {
        VisibilityBaseline.checkExact(
            actual =
                ProductionVisibilityAnalyzer(
                    scope = AfternoteKonsistScope.scope,
                    additionalProductionSources = productionBuildScripts(),
                    intentionalProductionContracts = INTENTIONAL_PRODUCTION_CONTRACTS.keys,
                ).violations(),
            legacy = LEGACY_TEST_DRIVEN_VISIBILITY,
        )
    }

    @Test
    fun `프로덕션 선언에 VisibleForTesting을 새로 붙이지 않는다`() {
        val actual =
            AfternoteKonsistScope
                .productionFiles
                .filter { "@VisibleForTesting" in it.text }
                .mapTo(sortedSetOf()) { it.normalizedProjectPath() }

        VisibilityBaseline.checkExact(actual = actual, legacy = LEGACY_VISIBLE_FOR_TESTING_FILES)
    }

    @Test
    fun `1574 회귀 - 테스트 참조가 있어도 file-local helper는 위반이다`() {
        val root = fixture.newFolder("issue-1574")
        root.writeKotlin(
            "src/main/kotlin/sample/BaseResponse.kt",
            """
            package sample

            data class BaseResponse(val status: Int)

            val BaseResponse.isSuccess: Boolean
                get() = status in 200..299

            fun BaseResponse.requireStatus() {
                check(isSuccess)
            }
            """,
        )
        root.writeKotlin(
            "src/test/kotlin/sample/BaseResponseTest.kt",
            """
            package sample

            class BaseResponseTest {
                fun boundary() = check(BaseResponse(200).isSuccess)
            }
            """,
        )

        val violations = fixtureViolations(root)

        val violation = violations.single { "isSuccess" in it }
        check(violation.endsWith("src/main/kotlin/sample/BaseResponse.kt::property isSuccess [implicit-public]"))
    }

    @Test
    fun `다른 프로덕션 파일이 소비하는 선언은 공개 계약으로 남긴다`() {
        val root = fixture.newFolder("production-consumer")
        root.writeKotlin(
            "src/main/kotlin/sample/Envelope.kt",
            """
            package sample

            data class Envelope(val status: Int)

            val Envelope.isSuccess: Boolean
                get() = status in 200..299
            """,
        )
        root.writeKotlin(
            "src/main/kotlin/sample/EnvelopeConsumer.kt",
            """
            package sample

            fun requireSuccess(envelope: Envelope) = check(envelope.isSuccess)
            """,
        )
        root.writeKotlin(
            "src/main/kotlin/consumer/EnvelopeContractConsumer.kt",
            """
            package consumer

            fun statusOf(envelope: sample.Envelope) = envelope.status
            """,
        )
        root.writeKotlin(
            "src/test/kotlin/sample/EnvelopeTest.kt",
            """
            package sample

            fun boundary() = check(Envelope(200).isSuccess)
            """,
        )

        val violations = fixtureViolations(root)

        assertEquals(emptySet<String>(), violations.filterTo(mutableSetOf()) { "isSuccess" in it })
        assertEquals(emptySet<String>(), violations.filterTo(mutableSetOf()) { "::class Envelope " in it })
    }

    @Test
    fun `interface와 typealias의 완전 수식 테스트 참조도 위반이다`() {
        val root = fixture.newFolder("interface-typealias-fqn")
        root.writeKotlin(
            "src/main/kotlin/sample/TestContracts.kt",
            """
            package sample

            internal interface TestHook
            internal typealias TestStatus = Int
            """,
        )
        root.writeKotlin(
            "src/test/kotlin/consumer/TestContractsTest.kt",
            """
            package consumer

            val hook: sample.TestHook? = null
            val status: sample.TestStatus = 200
            """,
        )

        val violations = fixtureViolations(root)

        check(violations.single { "TestHook" in it }.endsWith("::interface TestHook [internal]"))
        check(violations.single { "TestStatus" in it }.endsWith("::typealias TestStatus [internal]"))
    }

    @Test
    fun `문자열 template 안의 테스트 참조도 실행 코드로 검사한다`() {
        val root = fixture.newFolder("test-string-template")
        root.writeKotlin(
            "src/main/kotlin/sample/TemplateValue.kt",
            """
            package sample

            internal fun templateValue(value: String) = value
            internal fun rawTemplateValue() = "owner"
            """,
        )
        val testSource =
            """
            package sample

            fun rendersValue() = check("${'$'}{templateValue("x")}" == "x")
            """.trimIndent() +
                "\n\nfun rendersRawValue() = check(\"\"\"\\${'$'}{rawTemplateValue()}\"\"\" == \"\\\\owner\")\n"
        root.writeKotlin(
            "src/test/kotlin/sample/TemplateValueTest.kt",
            testSource,
        )

        val violations = fixtureViolations(root)

        check(violations.single { "templateValue" in it }.endsWith("::function templateValue [internal]"))
        check(violations.single { "rawTemplateValue" in it }.endsWith("::function rawTemplateValue [internal]"))
    }

    @Test
    fun `문자열 template 안의 프로덕션 소비도 실제 사용처로 인정한다`() {
        val root = fixture.newFolder("production-string-template")
        root.writeKotlin(
            "src/main/kotlin/sample/TemplateValue.kt",
            """
            package sample

            internal fun templateValue(value: String) = value
            internal fun rawTemplateValue() = "owner"
            """,
        )
        val productionConsumer =
            """
            package sample

            fun renderValue() = "${'$'}{templateValue("owner")}"
            """.trimIndent() +
                "\n\nfun renderRawValue() = \"\"\"\\${'$'}{rawTemplateValue()}\"\"\"\n"
        root.writeKotlin(
            "src/main/kotlin/sample/TemplateRenderer.kt",
            productionConsumer,
        )
        root.writeKotlin(
            "src/test/kotlin/sample/TemplateValueTest.kt",
            """
            package sample

            fun readsValue() = check(templateValue("owner") == rawTemplateValue())
            """,
        )

        val violations = fixtureViolations(root)

        assertEquals(emptySet<String>(), violations.filterTo(mutableSetOf()) { "templateValue" in it })
        assertEquals(emptySet<String>(), violations.filterTo(mutableSetOf()) { "rawTemplateValue" in it })
    }

    @Test
    fun `baseline에 없는 신규 위반은 실패한다`() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                VisibilityBaseline.checkExact(actual = setOf("new"), legacy = emptySet())
            }

        check("신규" in exception.message.orEmpty())
    }

    @Test
    fun `해소된 위반이 baseline에 남으면 실패한다`() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                VisibilityBaseline.checkExact(actual = emptySet(), legacy = setOf("stale"))
            }

        check("해소" in exception.message.orEmpty())
    }

    private fun fixtureViolations(root: File): Set<String> =
        ProductionVisibilityAnalyzer(
            AfternoteKonsistScope.scanExternalDirectories(listOf(File(root, "src"))),
        ).violations()

    private fun productionBuildScripts(): List<ReferenceSource> =
        AfternoteKonsistScope
            .projectRoot
            .walkTopDown()
            .onEnter { directory -> directory == AfternoteKonsistScope.projectRoot || directory.name !in SKIPPED_DIRECTORIES }
            .filter { file -> file.isFile && file.name.endsWith(".gradle.kts") }
            .map { file -> ReferenceSource(packageName = "", code = file.readText()) }
            .toList()

    private fun File.writeKotlin(
        relativePath: String,
        source: String,
    ) {
        resolve(relativePath).apply {
            parentFile.mkdirs()
            writeText(source.trimIndent() + "\n")
        }
    }

    private companion object {
        val ISSUE_1671_PLATFORM =
            setOf(
                "app/src/main/java/com/afternote/afternote_fe/messaging/AfternoteFirebaseMessagingService.kt::class FcmNotificationContent [internal]",
                "app/src/main/java/com/afternote/afternote_fe/messaging/AfternoteFirebaseMessagingService.kt::object FcmNotificationContentResolver [internal]",
                "app/src/main/java/com/afternote/afternote_fe/messaging/AfternoteFirebaseMessagingService.kt::object FcmNotificationIdentity [internal]",
                "build-logic/src/main/kotlin/BuildFingerprint.kt::function debugVersionNameSuffix [implicit-public]",
                "build-logic/src/main/kotlin/BuildFingerprint.kt::function shortCommitSha [implicit-public]",
                "build-logic/src/main/kotlin/BuildFingerprint.kt::property UNKNOWN_COMMIT_MARKER [implicit-public]",
                "build-logic/src/main/kotlin/VersionCode.kt::property DEFAULT_AFTERNOTE_VERSION_CODE [implicit-public]",
                "build-logic/src/main/kotlin/VersionCode.kt::property MAX_PLAY_VERSION_CODE [implicit-public]",
            )

        val ISSUE_1672_CORE =
            setOf(
                "core/data/src/main/java/com/afternote/core/data/mapper/delivery/DeliveryConditionMapper.kt::function toDto [implicit-public]",
                "core/network/src/main/kotlin/com/afternote/core/network/di/NetworkModule.kt::class SlowEndpointCallFactory [internal]",
                "core/network/src/main/kotlin/com/afternote/core/network/interceptor/TokenAuthenticator.kt::class TokenReissueFailureException [internal]",
                "core/ui/src/main/kotlin/com/afternote/core/ui/ViewModeSwitcher.kt::property VIEW_MODE_INDICATOR_TEST_TAG [internal]",
                "core/ui/src/main/kotlin/com/afternote/core/ui/ViewModeSwitcher.kt::property VIEW_MODE_PILL_TEST_TAG [internal]",
                "core/ui/src/main/kotlin/com/afternote/core/ui/calendar/BottomSheetCalendar.kt::function DatePickerContent [implicit-public]",
                "core/ui/src/main/kotlin/com/afternote/core/ui/popup/ErrorPopup.kt::function AfternoteErrorPopupContent [internal]",
                "core/ui/src/main/kotlin/com/afternote/core/ui/popup/Popup.kt::function PopupContent [internal]",
                "core/ui/src/main/kotlin/com/afternote/core/ui/topbar/HomeTopBar.kt::property PROFILE_ICON_TEST_TAG [implicit-public]",
            )

        val ISSUE_1673_AFTERNOTE =
            setOf(
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/detail/AfternoteDetailRoute.kt::function DeleteInProgressOverlay [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/detail/MemorialDetailScreen.kt::property MEMORIAL_VIDEO_CARD_TEST_TAG [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/AfternoteEditorContent.kt::function EditorContent [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/AfternoteEditorScreen.kt::function editorContentSignature [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/AfternoteEditorViewModel.kt::function toAfternoteEditorError [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/mapper/EditorReceiverMapping.kt::function toAfternoteEditorReceiver [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/memorial/MemorialVideoUpload.kt::property MEMORIAL_VIDEO_ADD_TEST_TAG [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/selection/EditorServiceSelectionSheet.kt::function EditorServiceSelectionSheetContent [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/selection/EditorServiceSelectionSheet.kt::function filterEditorServiceOptions [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/selection/EditorServiceSelectionSheet.kt::function serviceSelectionSheetTitleResOrNull [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/editor/state/AfternoteEditorState.kt::property editorMessagesSaver [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/home/AfternoteTypeFilterRow.kt::property AFTERNOTE_CATEGORY_MORE_INDICATOR_TEST_TAG [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/navigation/AfternoteNavGraphTheme.kt::function AfternoteLightTheme [implicit-public]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/receiver/afternotelist/ReceiverAfternoteHomeViewModel.kt::function toUiModel [internal]",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/presentation/shared/util/AfternoteDisplayRes.kt::function getIconResForType [implicit-public]",
            )

        val ISSUE_1674_MINDRECORD =
            setOf(
                "feature/mindrecord/data/src/main/kotlin/com/afternote/feature/mindrecord/data/mapper/WeeklyReportMapper.kt::function toDomainOrNull [implicit-public]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/hometab/HomeTabMindRecordLazyItems.kt::function MemoriesSectionContent [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/screen/memoryspace/MemorySpaceScreen.kt::function MemorySpaceContent [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/screen/receiver/ReceiverMindRecordScreen.kt::function findOpenedRecord [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/screen/sender/DailyQuestionWriteScreen.kt::function DailyQuestionWriteScreenContent [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/screen/sender/DiaryScreen.kt::function DiaryListContent [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/screen/sender/DiaryWriteScreen.kt::function DiaryWriteScreenContent [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/screen/sender/WeeklyReportScreen.kt::function emotionCardDescription [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/screen/sender/WeeklyReportScreen.kt::function recordedSummaryHighlights [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/viewmodel/ReceiverMindRecordViewModel.kt::function toDomainMessage [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/viewmodel/RecordDetailViewModel.kt::function firstImageUrl [internal]",
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/viewmodel/WeeklyReportRecordedDays.kt::function resolveDateInWeekOrNull [internal]",
            )

        val ISSUE_1675_ONBOARDING =
            setOf(
                "feature/onboarding/presentation/src/main/java/com/afternote/feature/onboarding/presentation/OnboardingProfileScreen.kt::function handleProfileImagePickerResult [internal]",
            )

        val ISSUE_1676_RECEIVER =
            setOf(
                "feature/receiver/data/src/main/kotlin/com/afternote/feature/receiver/data/mapper/ReceiverAfternoteListItemDtoToDomain.kt::class ReceiverListDecodingFailure [internal]",
                "feature/receiver/data/src/main/kotlin/com/afternote/feature/receiver/data/mapper/ReceiverAfternoteListItemDtoToDomain.kt::class ReceiverListMappingFailure [internal]",
                "feature/receiver/data/src/main/kotlin/com/afternote/feature/receiver/data/mapper/ReceiverAfternoteListItemDtoToDomain.kt::function toDomainOrNull [implicit-public]",
                "feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/deliveryverification/DocumentUploadScreen.kt::function DocumentUploadScreenContent [internal]",
                "feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/deliveryverification/IdentityVerificationEmailScreen.kt::function IdentityVerificationEmailScreenContent [internal]",
                "feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/deliveryverification/MasterKeyScreen.kt::function MasterKeyScreenContent [internal]",
                "feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/recordsbox/ReceivedRecordsScreen.kt::function ReceivedRecordsScreenContent [internal]",
                "feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/recordsbox/SenderRegistrationScreen.kt::function SenderRegistrationScreenContent [internal]",
                "feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/senderdetail/SenderDetailScreen.kt::function SenderDetailScreenContent [internal]",
            )

        val ISSUE_1677_TIMELETTER =
            setOf(
                "feature/timeletter/presentation/src/main/kotlin/com/afternote/feature/timeletter/presentation/screen/sender/TimeLetterWriteScreen.kt::function collectTextBlockContents [internal]",
            )

        /**
         * #1789 가 배선하면 해소된다.
         *
         * 비밀번호 찾기 3화면과 그 ViewModel 은 #457 에서 구현됐지만, 그래프 등록은 Nav3
         * 이관(#1698) 뒤로 미뤘다 — Nav2 로 먼저 배선하면 같은 32줄을 두 번 쓴다. 그동안
         * 프로덕션 참조가 없어 여기 실린다. private 축소는 답이 아니다 (파일 밖 소비가 곧 생긴다).
         */
        val ISSUE_1789_ONBOARDING_PENDING_WIRING =
            setOf(
                "feature/onboarding/presentation/src/main/java/com/afternote/feature/onboarding/presentation/findaccount/FindPasswordCompleteScreen.kt::function FindPasswordCompleteScreen [implicit-public]",
                "feature/onboarding/presentation/src/main/java/com/afternote/feature/onboarding/presentation/findaccount/FindPasswordResetScreen.kt::function FindPasswordResetScreen [implicit-public]",
                "feature/onboarding/presentation/src/main/java/com/afternote/feature/onboarding/presentation/findaccount/FindPasswordScreen.kt::function FindPasswordScreen [implicit-public]",
                "feature/onboarding/presentation/src/main/java/com/afternote/feature/onboarding/presentation/findaccount/FindPasswordViewModel.kt::class FindPasswordViewModel [implicit-public]",
            )

        /** #1671~#1677이 줄인다. 신규와 stale 항목 모두 실패해 목록은 정확한 현재 부채다. */
        val LEGACY_TEST_DRIVEN_VISIBILITY =
            ISSUE_1671_PLATFORM +
                ISSUE_1672_CORE +
                ISSUE_1673_AFTERNOTE +
                ISSUE_1674_MINDRECORD +
                ISSUE_1675_ONBOARDING +
                ISSUE_1676_RECEIVER +
                ISSUE_1677_TIMELETTER +
                ISSUE_1789_ONBOARDING_PENDING_WIRING

        val LEGACY_VISIBLE_FOR_TESTING_FILES =
            setOf(
                "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/presentation/hometab/HomeTabMindRecordLazyItems.kt",
            )

        /**
         * 소스 내 이름 참조가 없어도 프로덕션 계약인 선언. public signature의 노출 타입, 문서화된
         * 공용 UI, 아직 navigation graph가 소비하지 않는 화면 entry처럼 단순 텍스트 참조로는
         * 최소 범위를 판정할 수 없는 항목만 사유와 함께 둔다.
         */
        val INTENTIONAL_PRODUCTION_CONTRACTS =
            mapOf(
                "app/src/main/java/com/afternote/afternote_fe/navigation/" +
                    "NotificationDestinationRoute.kt::function toRoute [internal]" to
                    "알림 목적지→Route 매핑. 소비자인 앱 루트 결선은 Navigation 3 루트 전환 뒤 #1795 가 붙인다",
                "app/src/main/java/com/afternote/afternote_fe/notification/" +
                    "NotificationIntentContract.kt::class NotificationEntrySource [internal]" to
                    "NotificationEntryRequest.source의 내부 계약 타입",
                "core/ui/src/main/kotlin/com/afternote/core/ui/button/" +
                    "AfternoteRadioGroup.kt::function AfternoteRadioGroup [implicit-public]" to
                    "core:ui README에 문서화된 공용 컴포넌트",
                "core/ui/src/main/kotlin/com/afternote/core/ui/popup/" +
                    "AfternoteActionMenu.kt::class ActionMenuItem [implicit-public]" to
                    "AfternoteActionMenu 파라미터와 editDeleteActionMenuItems 반환값을 외부 feature가 타입 추론 소비",
                "feature/afternote/domain/src/main/java/com/afternote/feature/afternote/domain/model/author/" +
                    "ListItem.kt::class Account [implicit-public]" to
                    "ListItem.account가 노출하는 도메인 타입",
                "feature/afternote/presentation/src/main/kotlin/com/afternote/feature/afternote/" +
                    "presentation/editor/memorial/MemorialMediaSourceState.kt::class MemorialMediaSourceState [internal]" to
                    "다른 프로덕션 파일이 반환값을 타입 추론으로 소비",
                "feature/onboarding/presentation/src/main/java/com/afternote/feature/onboarding/presentation/terms/" +
                    "OnboardingTermsScreen.kt::class TermsType [implicit-public]" to
                    "OnboardingTermsScreen 콜백이 노출하는 화면 계약 타입",
                "feature/receiver/data/src/main/kotlin/com/afternote/feature/receiver/data/dto/" +
                    "ReceiverAfternoteDto.kt::class ReceivedMemorialVideoDto [implicit-public]" to
                    "ReceivedPlaylistDto.memorialVideo가 노출하는 직렬화 계약 타입",
                "feature/timeletter/presentation/src/main/kotlin/com/afternote/feature/timeletter/presentation/screen/recipient/" +
                    "RecipientTimeLetterDetailScreen.kt::function RecipientTimeLetterDetailScreen [implicit-public]" to
                    "수신 타임레터 상세 화면 entry 계약",
                "feature/timeletter/presentation/src/main/kotlin/com/afternote/feature/timeletter/presentation/screen/recipient/" +
                    "RecipientTimeletterScreen.kt::function RecipientTimeletterScreen [implicit-public]" to
                    "수신 타임레터 목록 화면 entry 계약",
            )

        val SKIPPED_DIRECTORIES = setOf(".git", ".gradle", ".claude", ".codex", "build")
    }
}

private class ProductionVisibilityAnalyzer(
    scope: KoScope,
    additionalProductionSources: List<ReferenceSource> = emptyList(),
    private val intentionalProductionContracts: Set<String> = emptySet(),
) {
    private val files = scope.files
    private val productionFiles = files.filterNot(KoFileDeclaration::isTestSource)
    private val testFiles = files.filter(KoFileDeclaration::isTestSource)
    private val productionSources =
        (productionFiles.map(KoFileDeclaration::asReferenceSource) + additionalProductionSources)
            .map { source -> source.asSearchableSource() }
    private val testSources = testFiles.map(KoFileDeclaration::asReferenceSource).map { source -> source.asSearchableSource() }

    fun violations(): Set<String> {
        val rawViolations =
            productionFiles
                .flatMap { it.topLevelVisibilityCandidates() }
                .filter { candidate -> testSources.any { it.references(candidate) } }
                .filter { candidate ->
                    productionSources
                        .asSequence()
                        .filterNot { it.path == candidate.path }
                        .none { it.references(candidate) }
                }.mapTo(sortedSetOf(), VisibilityCandidate::id)
        val staleContracts = intentionalProductionContracts - rawViolations
        check(staleContracts.isEmpty()) {
            buildString {
                appendLine("더 이상 예외가 필요 없는데 intentional production contract 목록에 남은 항목 (${staleContracts.size}건).")
                staleContracts.sorted().forEach { appendLine("  stale contract: $it") }
            }
        }
        return rawViolations - intentionalProductionContracts
    }

    private fun KoFileDeclaration.topLevelVisibilityCandidates(): List<VisibilityCandidate> =
        functions(includeNested = false, includeLocal = false).mapNotNull { it.toCandidate() } +
            properties(includeNested = false).mapNotNull { it.toCandidate() } +
            classes(includeNested = false, includeLocal = false).mapNotNull { it.toCandidate() } +
            objects(includeNested = false).mapNotNull { it.toCandidate() } +
            interfaces(includeNested = false).mapNotNull { it.toCandidate() } +
            typeAliases.mapNotNull { it.toCandidate() }

    private fun KoFunctionDeclaration.toCandidate(): VisibilityCandidate? =
        candidate(
            kind = "function",
            name = name,
            fullyQualifiedName = fullyQualifiedName,
            file = containingFile,
            visibility = visibilityName(),
            annotationNames = annotations.mapTo(mutableSetOf()) { it.name },
        )

    private fun KoPropertyDeclaration.toCandidate(): VisibilityCandidate? =
        candidate(
            kind = "property",
            name = name,
            fullyQualifiedName = fullyQualifiedName,
            file = containingFile,
            visibility = visibilityName(),
            annotationNames = annotations.mapTo(mutableSetOf()) { it.name },
        )

    private fun KoClassDeclaration.toCandidate(): VisibilityCandidate? =
        candidate(
            kind = "class",
            name = name,
            fullyQualifiedName = fullyQualifiedName,
            file = containingFile,
            visibility = visibilityName(),
            annotationNames = annotations.mapTo(mutableSetOf()) { it.name },
        )

    private fun KoObjectDeclaration.toCandidate(): VisibilityCandidate? =
        candidate(
            kind = "object",
            name = name,
            fullyQualifiedName = fullyQualifiedName,
            file = containingFile,
            visibility = visibilityName(),
            annotationNames = annotations.mapTo(mutableSetOf()) { it.name },
        )

    private fun KoInterfaceDeclaration.toCandidate(): VisibilityCandidate? =
        candidate(
            kind = "interface",
            name = name,
            fullyQualifiedName = fullyQualifiedName,
            file = containingFile,
            visibility = visibilityName(),
            annotationNames = annotations.mapTo(mutableSetOf()) { it.name },
        )

    private fun KoTypeAliasDeclaration.toCandidate(): VisibilityCandidate? =
        candidate(
            kind = "typealias",
            name = name,
            fullyQualifiedName = fullyQualifiedName,
            file = containingFile,
            visibility = visibilityName(),
            annotationNames = annotations.mapTo(mutableSetOf()) { it.name },
        )

    private fun candidate(
        kind: String,
        name: String,
        fullyQualifiedName: String?,
        file: KoFileDeclaration,
        visibility: String?,
        annotationNames: Set<String>,
    ): VisibilityCandidate? {
        if (
            visibility == null ||
            !KOTLIN_IDENTIFIER.matches(name) ||
            annotationNames.any(FRAMEWORK_ENTRY_ANNOTATIONS::contains)
        ) {
            return null
        }
        val packageName = file.packagee?.name.orEmpty()
        return VisibilityCandidate(
            kind = kind,
            name = name,
            fullyQualifiedName = fullyQualifiedName ?: listOf(packageName, name).filter(String::isNotEmpty).joinToString("."),
            packageName = packageName,
            path = file.normalizedProjectPath(),
            visibility = visibility,
        )
    }

    private fun KoVisibilityModifierProvider.visibilityName(): String? =
        when {
            hasPrivateModifier || hasProtectedModifier -> null
            hasInternalModifier -> "internal"
            hasPublicModifier -> "public"
            hasPublicOrDefaultModifier -> "implicit-public"
            else -> null
        }

    private fun ReferenceSource.references(candidate: VisibilityCandidate): Boolean {
        val exactImport =
            Regex(
                pattern = "(?m)^\\s*import\\s+${Regex.escape(candidate.fullyQualifiedName)}(?:\\s+as\\s+([A-Za-z_][A-Za-z0-9_]*))?\\s*$",
            ).find(code)
        val codeWithoutImports = IMPORT_LINE.replace(code, " ")
        if (identifierReference(candidate.fullyQualifiedName).containsMatchIn(codeWithoutImports)) return true
        if (exactImport != null) {
            val localName = exactImport.groupValues[1].ifBlank { candidate.name }
            return identifierReference(localName).containsMatchIn(codeWithoutImports)
        }

        val wildcardImport =
            candidate.packageName.isNotEmpty() &&
                Regex("(?m)^\\s*import\\s+${Regex.escape(candidate.packageName)}\\.\\*\\s*$").containsMatchIn(code)
        val samePackage = packageName == candidate.packageName
        if (!wildcardImport && !samePackage) return false

        return identifierReference(candidate.name).containsMatchIn(codeWithoutImports)
    }

    private fun ReferenceSource.asSearchableSource(): ReferenceSource = copy(code = code.withoutCommentsAndStrings())

    private fun identifierReference(identifier: String): Regex = Regex("(?<![A-Za-z0-9_])${Regex.escape(identifier)}(?![A-Za-z0-9_])")

    private fun String.withoutCommentsAndStrings(): String = KotlinReferenceSanitizer(this).sanitize()

    private data class VisibilityCandidate(
        val kind: String,
        val name: String,
        val fullyQualifiedName: String,
        val packageName: String,
        val path: String,
        val visibility: String,
    ) {
        val id: String = "$path::$kind $name [$visibility]"
    }

    private companion object {
        val KOTLIN_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
        val FRAMEWORK_ENTRY_ANNOTATIONS = setOf("AndroidEntryPoint", "Module")
        val IMPORT_LINE = Regex("(?m)^\\s*import\\s+.*$")
    }
}

private data class ReferenceSource(
    val packageName: String,
    val code: String,
    val path: String? = null,
)

/**
 * 이름 검색 전에 주석·문자열 literal을 지우되 문자열 template의 실행 표현만 보존한다.
 * 일반 문자열의 escape와 raw 문자열의 비-escape 규칙, template 내부의 중첩 문자열·중괄호를 구분한다.
 */
private class KotlinReferenceSanitizer(
    private val source: String,
) {
    private val output = StringBuilder(source.length)

    fun sanitize(): String {
        appendCode(startIndex = 0, stopAtTemplateEnd = false)
        return output.toString()
    }

    private fun appendCode(
        startIndex: Int,
        stopAtTemplateEnd: Boolean,
    ): Int {
        var index = startIndex
        var braceDepth = if (stopAtTemplateEnd) 1 else 0
        while (index < source.length) {
            when {
                source.startsWith("//", index) -> {
                    index = skipLineComment(index + 2)
                }

                source.startsWith("/*", index) -> {
                    index = skipBlockComment(index + 2)
                }

                source.startsWith("\"\"\"", index) -> {
                    index = appendRawString(index + 3)
                }

                source[index] == '"' -> {
                    index = appendRegularString(index + 1)
                }

                source[index] == '\'' -> {
                    index = skipCharacter(index + 1)
                }

                stopAtTemplateEnd && source[index] == '{' -> {
                    braceDepth++
                    output.append(' ')
                    index++
                }

                stopAtTemplateEnd && source[index] == '}' -> {
                    braceDepth--
                    output.append(' ')
                    index++
                    if (braceDepth == 0) return index
                }

                else -> {
                    output.append(source[index])
                    index++
                }
            }
        }
        return index
    }

    private fun appendRegularString(startIndex: Int): Int {
        output.append(' ')
        var index = startIndex
        while (index < source.length) {
            when {
                source[index] == '\\' -> index = (index + 2).coerceAtMost(source.length)
                source[index] == '"' -> return index + 1
                source[index] == '$' -> index = appendTemplate(index)
                else -> index++
            }
        }
        return index
    }

    private fun appendRawString(startIndex: Int): Int {
        output.append(' ')
        var index = startIndex
        while (index < source.length) {
            when {
                source.startsWith("\"\"\"", index) -> return index + 3
                source[index] == '$' -> index = appendTemplate(index)
                else -> index++
            }
        }
        return index
    }

    private fun appendTemplate(dollarIndex: Int): Int {
        if (dollarIndex + 1 >= source.length) return dollarIndex + 1
        val next = source[dollarIndex + 1]
        if (next == '{') {
            output.append(' ')
            return appendCode(startIndex = dollarIndex + 2, stopAtTemplateEnd = true)
        }
        if (next != '_' && !next.isLetter()) return dollarIndex + 1

        var index = dollarIndex + 2
        while (index < source.length && (source[index] == '_' || source[index].isLetterOrDigit())) index++
        output.append(source, dollarIndex + 1, index).append(' ')
        return index
    }

    private fun skipLineComment(startIndex: Int): Int {
        val newline = source.indexOf('\n', startIndex)
        if (newline < 0) return source.length
        output.append('\n')
        return newline + 1
    }

    private fun skipBlockComment(startIndex: Int): Int {
        output.append(' ')
        var index = startIndex
        var depth = 1
        while (index < source.length && depth > 0) {
            when {
                source.startsWith("/*", index) -> {
                    depth++
                    index += 2
                }

                source.startsWith("*/", index) -> {
                    depth--
                    index += 2
                }

                source[index] == '\n' -> {
                    output.append('\n')
                    index++
                }

                else -> {
                    index++
                }
            }
        }
        return index
    }

    private fun skipCharacter(startIndex: Int): Int {
        output.append(' ')
        var index = startIndex
        while (index < source.length) {
            when {
                source[index] == '\\' -> index = (index + 2).coerceAtMost(source.length)
                source[index] == '\'' -> return index + 1
                else -> index++
            }
        }
        return index
    }
}

private object VisibilityBaseline {
    fun checkExact(
        actual: Set<String>,
        legacy: Set<String>,
    ) {
        val added = actual - legacy
        val stale = legacy - actual
        check(added.isEmpty() && stale.isEmpty()) {
            buildString {
                if (added.isNotEmpty()) {
                    appendLine("테스트 참조 때문에 넓어진 프로덕션 visibility 신규 위반 (${added.size}건).")
                    added.sorted().forEach { appendLine("  신규: $it") }
                }
                if (stale.isNotEmpty()) {
                    appendLine("이미 해소됐지만 visibility baseline에 남은 항목 (${stale.size}건).")
                    stale.sorted().forEach { appendLine("  해소: $it") }
                }
                appendLine("프로덕션 사용 범위가 같은 파일뿐이면 private로 줄이고 테스트는 공개 owner 계약을 검증한다.")
            }
        }
    }
}

private fun KoFileDeclaration.isTestSource(): Boolean = sourceSetName.substringAfter(':').lowercase().contains("test")

private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

private fun KoFileDeclaration.asReferenceSource(): ReferenceSource =
    ReferenceSource(
        packageName = packagee?.name.orEmpty(),
        code = text,
        path = normalizedProjectPath(),
    )
