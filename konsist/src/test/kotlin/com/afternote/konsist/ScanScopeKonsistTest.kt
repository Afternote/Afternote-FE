package com.afternote.konsist

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.ext.list.withPackage
import org.junit.Test
import java.io.File

/**
 * konsist 스캔 범위 회귀 가드 (#1659).
 *
 * 이 저장소는 루트 밑에 다른 브랜치의 체크아웃을 둔다(`.claude/worktrees/`·`.codex/worktrees/`, 실측 84개 초과).
 * 저장소 루트를 통째로 훑는 konsist 진입점으로 되돌아가면
 * 1. 로컬 `:konsist:test` 가 OOM 으로 죽고(실측: 심은 19,228개에서 `Test worker` OOM),
 * 2. 패키지·어노테이션으로만 거르는 가드 4종이 **남의 브랜치의 위반**을 지금 브랜치의 판정에 끌어들인다
 *    (실측: 위반 4건을 심자 레이어·presentation·수신자 홈·응답 DTO 가드가 그대로 red).
 *
 * CI 러너는 매번 단일 체크아웃이라 이 결함을 영영 못 본다. 그래서 가드를 두 겹으로 둔다.
 * - **구조** — 진입점이 하나이고([가드는 공유 진입점 하나만 쓴다]), 그 진입점이 루트를 훑는 생성기를
 *   쓰지 않으며([공유 진입점은 저장소 루트를 훑는 스코프 생성기를 쓰지 않는다]), 읽은 파일이 스캔 뿌리
 *   아래 전수와 정확히 일치한다([스캔 대상은 스캔 뿌리 아래 kt 파일 전수와 같다]). 환경·실행 순서와 무관하다.
 * - **실측** — 중첩 체크아웃을 실제로 심어 판정이 흔들리지 않음을 확인한다
 *   ([중첩 체크아웃을 심어도 가드가 보는 선언이 늘지 않는다]).
 */
class ScanScopeKonsistTest {
    /**
     * `Konsist` 를 아는 파일은 [AfternoteKonsistScope] 하나뿐이어야 한다.
     *
     * 가드마다 스코프를 따로 만들면 이 이슈가 조용히 되살아난다 — 실제로 여섯 가드가 제각기
     * `scopeFromProject()` 를 부르고 있었다.
     */
    @Test
    fun `가드는 공유 진입점 하나만 쓴다`() {
        val offenders =
            konsistModuleFiles()
                .filter { file -> file.imports.any { it.name == KONSIST_ENTRY_POINT } }
                .map { it.name }
                .filterNot { it == SHARED_SCOPE_FILE }

        check(offenders.isEmpty()) {
            buildString {
                appendLine("$KONSIST_ENTRY_POINT 를 직접 import 하는 가드가 있다 (${offenders.size}건).")
                appendLine("스코프는 $SHARED_SCOPE_FILE 하나에서만 만든다 — 진입점이 갈라지면 스캔 범위도 갈라진다 (#1659).")
                appendLine()
                offenders.sorted().forEach { appendLine("  $it") }
            }
        }
    }

    /**
     * `scopeFromProject`·`scopeFromProduction`·`scopeFromModule`·`scopeFromPackage`·`scopeFromSourceSet`·
     * `scopeFromFile` 계열은 전부 내부에서 `KoFileDeclarationProvider.getKoFileDeclarations()` 하나를 부른다.
     * 그 함수가 **저장소 루트를 통째로 walk 해 파싱한 뒤** 모듈·소스셋으로 거른다 — 모듈 이름을 넘겨도
     * 읽는 양은 줄지 않는다. 그래서 이름 자체를 금지한다.
     *
     * 주석은 지우고 본다. 「왜 안 쓰는가」를 설명하는 KDoc 이 스스로 걸리면 안 되기 때문이다.
     */
    @Test
    fun `공유 진입점은 저장소 루트를 훑는 스코프 생성기를 쓰지 않는다`() {
        val offenders =
            konsistModuleFiles().flatMap { file ->
                val code = file.text.withoutComments()
                ROOT_WALKING_CREATORS.filter { it in code }.map { "${file.name} — $it" }
            }

        check(offenders.isEmpty()) {
            buildString {
                appendLine("저장소 루트를 통째로 읽는 konsist 스코프 생성기를 쓴다 (${offenders.size}건).")
                appendLine("중첩 체크아웃(.claude/worktrees·.codex/worktrees)이 판정에 섞이고 로컬 실행이 OOM 으로 죽는다.")
                appendLine()
                offenders.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("스캔 뿌리를 명시하는 AfternoteKonsistScope.scanFresh() 를 쓴다 (#1659).")
            }
        }
    }

    /**
     * 읽은 파일이 스캔 뿌리 아래 `.kt` **전수와 정확히 같은지**. 한쪽으로 어긋나는 두 방향을 함께 막는다 —
     * 넓어지면 남의 체크아웃이 섞이고, 좁아지면 가드가 봐야 할 소스를 놓친 채 초록이 된다.
     */
    @Test
    fun `스캔 대상은 스캔 뿌리 아래 kt 파일 전수와 같다`() {
        val expected =
            AfternoteKonsistScope
                .scanRoots()
                .flatMap { root -> root.walkTopDown().filter { it.isFile && it.name.endsWith(KOTLIN_EXTENSION) } }
                .map { it.path }
                .toSet()
        val actual = AfternoteKonsistScope.files.map { it.path }.toSet()

        check(expected == actual) {
            buildString {
                appendLine("스캔 대상이 스캔 뿌리와 어긋난다.")
                appendLine("뿌리에는 있는데 안 읽은 파일 ${(expected - actual).size}건 / 뿌리 밖인데 읽은 파일 ${(actual - expected).size}건.")
                appendLine()
                (actual - expected).sorted().take(SAMPLE_SIZE).forEach { appendLine("  뿌리 밖: $it") }
                (expected - actual).sorted().take(SAMPLE_SIZE).forEach { appendLine("  안 읽음: $it") }
            }
        }
    }

    @Test
    fun `스캔 뿌리는 선언된 모듈의 소스 디렉터리뿐이다`() {
        val root = AfternoteKonsistScope.projectRoot.path
        val declared =
            (
                AfternoteKonsistScope.includedProjectPaths().map { it.removePrefix(":").replace(':', '/') } +
                    AfternoteKonsistScope.includedBuildPaths()
            ).map { "$root/$it/${AfternoteKonsistScope.SOURCE_DIR}" }
                .toSet()

        val unexpected = AfternoteKonsistScope.scanRoots().map { it.path } - declared

        check(unexpected.isEmpty()) {
            buildString {
                appendLine("선언되지 않은 디렉터리가 스캔 뿌리에 섞였다 (${unexpected.size}건).")
                appendLine("뿌리는 ${AfternoteKonsistScope.SETTINGS_FILE} 이 선언한 모듈·포함 빌드의 src 뿐이어야 한다.")
                appendLine()
                unexpected.sorted().forEach { appendLine("  $it") }
            }
        }
    }

    @Test
    fun `settings 의 모듈 선언이 하나도 빠지지 않는다`() {
        val settings = File(AfternoteKonsistScope.projectRoot, AfternoteKonsistScope.SETTINGS_FILE).readText()
        val declared = INCLUDE_CALL.findAll(settings).count()
        val parsed = AfternoteKonsistScope.includedProjectPaths().size

        check(declared == parsed) {
            buildString {
                appendLine("${AfternoteKonsistScope.SETTINGS_FILE} 의 include 선언 ${declared}건 중 ${parsed}건만 뿌리가 됐다.")
                appendLine("놓친 모듈은 가드가 통째로 못 본다 — 위반이 있어도 침묵하는 초록이 된다.")
                appendLine("include 표기를 바꿨다면 AfternoteKonsistScope.INCLUDE 도 함께 고친다.")
            }
        }
    }

    /**
     * 포함 빌드는 루트 `src` 하나만 뿌리로 삼는다. `build-logic` 이 서브모듈을 갖게 되면 그 소스가
     * 조용히 스캔에서 빠지므로, 가정이 깨지는 순간 여기서 멈춘다.
     */
    @Test
    fun `포함 빌드는 서브모듈을 두지 않는다`() {
        val withSubmodules =
            AfternoteKonsistScope
                .includedBuildPaths()
                .map { File(AfternoteKonsistScope.projectRoot, it) }
                .map { it to File(it, AfternoteKonsistScope.SETTINGS_FILE) }
                .filter { (_, settings) -> settings.exists() && INCLUDE_CALL.containsMatchIn(settings.readText()) }
                .map { (dir, _) -> dir.name }

        check(withSubmodules.isEmpty()) {
            "포함 빌드가 서브모듈을 선언했다: ${withSubmodules.joinToString()}. " +
                "AfternoteKonsistScope.scanRoots() 가 그 소스를 못 본다 — 뿌리 계산을 재귀로 바꾼다 (#1659)."
        }
    }

    /**
     * 재현 조건을 직접 만든다 — 저장소 루트에 「다른 브랜치 체크아웃」을 흉내 낸 디렉터리를 심고,
     * 패키지·어노테이션으로만 거르는 가드 4종이 보는 선언이 **한 건도 늘지 않는지** 본다.
     *
     * 심는 파일은 그 4종이 각각 잡아내야 할 위반이다. 소요를 초로 단언하면 기기 사정에 흔들리므로,
     * 소요를 좌우하는 **읽는 파일 수**를 고정한다.
     */
    @Test
    fun `중첩 체크아웃을 심어도 가드가 보는 선언이 늘지 않는다`() {
        val before = AfternoteKonsistScope.scope.snapshot()
        val probe = File(AfternoteKonsistScope.projectRoot, PROBE_DIR)
        probe.deleteRecursively()

        try {
            plantNestedCheckout(probe)
            check(AfternoteKonsistScope.scanRoots().none { it.path.startsWith(probe.path) }) {
                "중첩 체크아웃이 스캔 뿌리로 잡혔다: $probe"
            }

            val after = AfternoteKonsistScope.scanFresh()
            val leaked = after.files.count { it.path.startsWith(probe.path) }
            check(leaked == 0) { "중첩 체크아웃의 파일이 스캔 대상에 섞였다 (${leaked}건)." }
            check(after.snapshot() == before) {
                buildString {
                    appendLine("중첩 체크아웃을 심자 가드가 보는 선언이 달라졌다.")
                    appendLine("남의 브랜치 소스가 섞이면 이 브랜치의 판정이 작업 트리 바깥 상태에 좌우된다 (#1659).")
                    appendLine()
                    appendLine("  심기 전: $before")
                    appendLine("  심은 뒤: ${after.snapshot()}")
                }
            }
        } finally {
            probe.deleteRecursively()
        }
    }

    /** 패키지·어노테이션으로만 거르는 가드 4종이 실제로 보는 선언의 수. */
    private fun KoScope.snapshot(): Map<String, Int> =
        mapOf(
            "전체 파일" to files.size,
            "LayerDependency — domain 파일" to files.withPackage(DOMAIN_PACKAGE).size,
            "PresentationLayerDependency — presentation 파일" to
                AfternoteKonsistScope.productionFilesOf(this).withPackage(PRESENTATION_PACKAGE).size,
            "ReceiverHomeResource — 수신자 홈 파일" to files.withPackage(RECEIVER_HOME_PACKAGE).size,
            "ResponseDtoContract — 응답 DTO 클래스" to
                classes()
                    .withPackage(DTO_PACKAGE)
                    .count { dto -> dto.hasDataModifier && dto.annotations.any { it.name == SERIALIZABLE } },
        )

    private fun konsistModuleFiles(): List<KoFileDeclaration> =
        AfternoteKonsistScope.files.filter { file ->
            file.projectPath
                .replace('\\', '/')
                .trimStart('/')
                .startsWith(KONSIST_MODULE_PREFIX)
        }

    /** 블록·줄 주석 제거. 「무엇을 쓰지 않는가」를 적어 둔 KDoc 이 그 금지에 걸리지 않게 한다. */
    private fun String.withoutComments(): String = replace(BLOCK_COMMENT, " ").replace(LINE_COMMENT, " ")

    /**
     * 「다른 브랜치의 체크아웃」 흉내. 경로에 `/src/` 가 끼고, 가드 4종이 각각 잡아야 할 위반을 하나씩 담는다.
     * `.claude/worktrees/<브랜치>/…` 와 같은 자리 — 어느 모듈의 `src` 밑도 아닌 저장소 루트 밑이다.
     */
    private fun plantNestedCheckout(probe: File) {
        val checkout = File(probe, "worktrees/other-branch")
        plant(
            checkout,
            "feature/probe/domain/src/main/kotlin/com/afternote/feature/probe/domain/ProbeRepository.kt",
            """
            package com.afternote.feature.probe.domain

            import android.content.Context

            interface ProbeRepository {
                fun context(): Context
            }
            """.trimIndent(),
        )
        plant(
            checkout,
            "feature/probe/presentation/src/main/kotlin/com/afternote/feature/probe/presentation/ProbeViewModel.kt",
            """
            package com.afternote.feature.probe.presentation

            import com.afternote.core.datastore.ProbeDataSource

            class ProbeViewModel(
                private val dataSource: ProbeDataSource,
            )
            """.trimIndent(),
        )
        plant(
            checkout,
            "feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation/home/ProbeHome.kt",
            """
            package com.afternote.feature.home.presentation.receiver

            import com.afternote.feature.afternote.presentation.R

            val probeIcon: Int = R.drawable.probe
            """.trimIndent(),
        )
        plant(
            checkout,
            "feature/probe/data/src/main/kotlin/com/afternote/feature/probe/data/dto/ProbeResponseDto.kt",
            """
            package com.afternote.feature.probe.data.dto

            import kotlinx.serialization.Serializable

            @Serializable
            data class ProbeResponseDto(
                val items: List<String> = emptyList(),
            )
            """.trimIndent(),
        )
    }

    private fun plant(
        checkout: File,
        relativePath: String,
        content: String,
    ) {
        val file = File(checkout, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content + "\n")
    }

    private companion object {
        const val KONSIST_ENTRY_POINT = "com.lemonappdev.konsist.api.Konsist"
        const val SHARED_SCOPE_FILE = "AfternoteKonsistScope"
        const val KONSIST_MODULE_PREFIX = "konsist/src/"
        const val KOTLIN_EXTENSION = ".kt"
        const val SAMPLE_SIZE = 5

        /**
         * 금지할 생성기 이름을 조각으로 조립한다 — 완성된 이름을 이 파일에 적으면 스스로 걸린다.
         * 허용하는 `scopeFromDirectory`·`scopeFromExternalDirectories` 는 이 목록에 없다.
         */
        const val CREATOR_PREFIX = "scopeFrom"
        val ROOT_WALKING_CREATORS =
            listOf("Project", "Production", "Test", "Module", "Package", "SourceSet", "File")
                .map { CREATOR_PREFIX + it }

        val BLOCK_COMMENT = Regex("""/\*[\s\S]*?\*/""")
        val LINE_COMMENT = Regex("""//[^\n]*""")

        /** `include(":app")` — 세는 쪽은 느슨하게 잡아, 파싱이 놓친 선언을 드러낸다. */
        val INCLUDE_CALL = Regex("""^\s*include\(""", RegexOption.MULTILINE)

        const val PROBE_DIR = ".konsist-nested-checkout-probe"

        const val DOMAIN_PACKAGE = "com.afternote..domain.."
        const val PRESENTATION_PACKAGE = "com.afternote..presentation.."
        const val RECEIVER_HOME_PACKAGE = "com.afternote.feature.home.presentation.receiver.."
        const val DTO_PACKAGE = "com.afternote..dto.."
        const val SERIALIZABLE = "Serializable"
    }
}
