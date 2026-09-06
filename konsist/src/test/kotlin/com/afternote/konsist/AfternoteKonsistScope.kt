package com.afternote.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import java.io.File

/**
 * 가드 6종이 공유하는 **단 하나의** konsist 진입점 (#1659).
 *
 * ### 왜 `Konsist.scopeFromProject()` 를 쓰지 않는가
 * konsist 의 `scopeFromProject()`·`scopeFromModule()`·`scopeFromProduction()`·`scopeFromPackage()` 는
 * 전부 내부에서 `KoFileDeclarationProvider.getKoFileDeclarations()` 하나를 부른다. 그 함수는
 * **저장소 루트를 통째로 walk 해 `.kt` 를 전부 파싱한 뒤** 모듈·소스셋으로 걸러낸다. 즉 모듈 이름을
 * 넘겨도 읽는 양은 줄지 않는다 — 걸러지는 건 파싱이 끝난 뒤다.
 *
 * 이 저장소는 루트 밑에 다른 브랜치의 체크아웃을 둔다(`.claude/worktrees/`·`.codex/worktrees/`).
 * konsist 가 걸러 주는 건 `build`/`target`/`.gradle` 뿐이라(`KoScopeCreatorCore.isBuildToolPath`)
 * 중첩 체크아웃은 그대로 끌려 들어온다. 그 결과는 둘이다.
 * 1. 로컬 `:konsist:test` 가 OOM 으로 죽는다 — 실측에서 심은 19,228개로 재현된다.
 * 2. **남의 브랜치의 아직 머지되지 않은 위반이 지금 브랜치의 가드를 빨갛게 만든다** —
 *    패키지·어노테이션으로만 거르는 가드(레이어·DTO·수신자 홈·presentation)는 경로를 보지 않는다.
 *
 * ### 대신 무엇을 쓰는가
 * `scopeFromExternalDirectories()` 만이 루트 walk 를 타지 않고 **넘긴 디렉터리만** walk 한다.
 * 그래서 스캔 뿌리를 [scanRoots] — `settings.gradle.kts` 가 선언한 모듈의 `src` 디렉터리 — 로
 * 명시한다. 중첩 체크아웃은 어느 모듈의 `src` 밑에도 없으므로 구조적으로 섞일 수 없고,
 * `<모듈>/build` 도 `src` 의 형제라 함께 빠진다(`scopeFromExternalDirectories` 는 빌드 산출물을
 * 걸러 주지 않으므로 이 점이 중요하다).
 *
 * 뿌리를 `settings.gradle.kts` 에서 뽑는 덕에 모듈이 늘어도 이 파일을 고칠 일이 없다. 선언이 빠짐없이
 * 뿌리가 되는지는 [ScanScopeKonsistTest] 가 지킨다.
 */
internal object AfternoteKonsistScope {
    /** konsist 가 `gradlew` 를 찾아 정한 저장소 루트. `projectPath` 도 이 값 기준이라 같은 것을 쓴다. */
    val projectRoot: File = File(Konsist.projectRootPath)

    /** 이 빌드가 선언한 Gradle 프로젝트 경로 — `:core:model` 형태. */
    fun includedProjectPaths(): List<String> = INCLUDE.findAll(settingsText()).map { it.groupValues[1] }.toList()

    /** 이 빌드가 끌어들이는 포함 빌드의 디렉터리 — `build-logic`. */
    fun includedBuildPaths(): List<String> = INCLUDE_BUILD.findAll(settingsText()).map { it.groupValues[1] }.toList()

    /**
     * 실제로 walk 할 디렉터리. 선언된 모듈과 포함 빌드의 `src` 중 **실재하는 것만** 담는다
     * (`:feature:onboarding:domain` 처럼 아직 소스가 없는 모듈이 있다 — 없는 경로를 넘기면
     * konsist 가 `Directory does not exist` 로 죽는다).
     */
    fun scanRoots(): List<File> {
        val moduleRoots = includedProjectPaths().map { File(projectRoot, it.removePrefix(":").replace(':', '/')) }
        val buildRoots = includedBuildPaths().map { File(projectRoot, it) }
        val roots = (moduleRoots + buildRoots).map { File(it, SOURCE_DIR) }.filter { it.isDirectory }

        check(roots.isNotEmpty()) {
            "스캔할 소스 디렉터리를 하나도 찾지 못했다 — $projectRoot 의 $SETTINGS_FILE 을 읽지 못했을 수 있다."
        }
        return roots
    }

    /**
     * 전 소스. 가드 6종이 같은 인스턴스를 나눠 쓴다 — `scopeFromExternalDirectories` 는
     * `scopeFromProject` 와 달리 파싱 결과를 캐시하지 않아, 부를 때마다 다시 읽는다.
     */
    val scope: KoScope by lazy { scanFresh() }

    /** [scope] 의 파일. */
    val files: List<KoFileDeclaration> get() = scope.files

    /**
     * 프로덕션(비-test) 소스셋의 파일. konsist 의 `scopeFromProduction()` 과 같은 기준
     * (소스셋 이름에 `test` 가 들어가면 테스트)으로 거른다.
     */
    val productionFiles: List<KoFileDeclaration> get() = productionFilesOf(scope)

    /** [productionFiles] 와 같은 기준을 임의의 스코프에 적용한다. 재발 가드가 새로 읽은 스코프에 쓴다. */
    fun productionFilesOf(target: KoScope): List<KoFileDeclaration> = target.files.filterNot { it.sourceSetName.isTestSourceSet() }

    /** 테스트 소스셋(test·androidTest·testFixtures·screenshotTest…)의 파일 — [productionFiles] 의 여집합. */
    val testFiles: List<KoFileDeclaration> get() = scope.files.filter { it.sourceSetName.isTestSourceSet() }

    /** 캐시를 타지 않고 지금의 [scanRoots] 를 다시 읽는다. 재발 가드([ScanScopeKonsistTest])가 쓴다. */
    fun scanFresh(): KoScope = Konsist.scopeFromExternalDirectories(scanRoots().map { it.path })

    /** 실제 Kotlin 파서로 작은 회귀 fixture를 읽는다. Konsist 진입점은 이 객체 하나로 유지한다. */
    fun scanExternalDirectories(directories: List<File>): KoScope {
        check(directories.isNotEmpty() && directories.all(File::isDirectory)) {
            "Konsist fixture 디렉터리가 없거나 비어 있다: ${directories.joinToString()}"
        }
        return Konsist.scopeFromExternalDirectories(directories.map { it.path })
    }

    private fun settingsText(): String = File(projectRoot, SETTINGS_FILE).readText()

    private fun String.isTestSourceSet(): Boolean = substringAfter(':').lowercase().contains(TEST_NAME_IN_PATH)

    const val SETTINGS_FILE = "settings.gradle.kts"
    const val SOURCE_DIR = "src"

    /** `include(":core:model")` — 이 빌드의 모듈 선언. */
    private val INCLUDE = Regex("""^\s*include\("(:[^"]+)"\)""", RegexOption.MULTILINE)

    /** `includeBuild("build-logic")` — 포함 빌드 선언. */
    private val INCLUDE_BUILD = Regex("""^\s*includeBuild\("([^"]+)"\)""", RegexOption.MULTILINE)

    private const val TEST_NAME_IN_PATH = "test"
}
