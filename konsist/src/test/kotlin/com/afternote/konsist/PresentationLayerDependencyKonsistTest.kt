package com.afternote.konsist

import com.lemonappdev.konsist.api.ext.list.withPackage
import org.junit.Test

/**
 * presentation 이 data 계층 **구현**을 건너뛰고 잡지 않는지 (#1432).
 *
 * [LayerDependencyKonsistTest] 는 반대 방향만 본다 — domain 이 무엇을 보느냐. 이쪽 방향
 * (`presentation → core:datastore`)은 어느 가드의 사정거리에도 없었고, Gradle 도 사이클이
 * 아니라 조용하다. 금지 근거는 KDoc 문장 하나뿐이었다.
 *
 * 그 공백이 실제로 3개월을 놓쳤다. 2026-05-28 `ad328c64` 「UserProfileRepository 추가 및
 * 데이터 레이어 의존성 은닉」 이 afternote 를 DataSource → Repository 로 옮기고 그 모듈의
 * `core:datastore` 의존까지 지운 의도적 계층 이행이었는데 setting 만 빠졌다. 이후 아무
 * 검사도 이를 알리지 않았다.
 *
 * ### 검사 대상은 main 소스뿐이다
 * 테스트 소스는 `UserRepositoryImpl` 을 직접 조립하는 자리가 남아 있는데(#930 과 얽혀
 * `internal` 로 닫지 못하는 그 조립이다), 그건 프로덕션 계층 우회와 성격이 다르다.
 * 한 번에 막으면 #930 이 선행돼야 하므로 1차는 프로덕션 소스로 한정한다.
 *
 * ### [KNOWN_LAYER_BYPASSES]
 * 도입 시점에 남아 있던 잔여. 수복은 담당 이슈 몫이고 이 가드는 「다음에 또 생겨도 모른다」
 * 는 구조만 닫는다. 해소된 항목이 목록에 남으면 **경고만** 낸다 —
 * [ResponseDtoContractKonsistTest] 와 같은 이유로, 해소 PR 과 목록 정리 PR 이 서로를
 * 깨뜨리지 않게 한다.
 */
class PresentationLayerDependencyKonsistTest {
    @Test
    fun `presentation 은 datastore 와 data 구현을 직접 참조하지 않는다`() {
        val violations = layerBypasses() - KNOWN_LAYER_BYPASSES

        check(violations.isEmpty()) {
            buildString {
                appendLine("presentation 이 data 계층 구현을 직접 참조한다 (${violations.size}건).")
                appendLine("DataStore·Repository 구현은 domain 계약 뒤에 있어야 한다 —")
                appendLine("presentation 이 구현을 알면 저장 방식을 바꿀 때 화면이 함께 깨진다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("주입은 계약(`core:domain` 의 Repository 인터페이스)으로 받는다.")
            }
        }
    }

    @Test
    fun `해소된 항목은 경고로 알린다`() {
        val stale = KNOWN_LAYER_BYPASSES - layerBypasses()
        if (stale.isEmpty()) return

        println(
            buildString {
                appendLine("[경고] KNOWN_LAYER_BYPASSES 에 이미 해소된 항목이 남아 있다 (${stale.size}건).")
                appendLine("목록에서 지워야 다음 위반이 이 자리에 숨지 않는다.")
                appendLine()
                stale.sorted().forEach { appendLine("  $it") }
            },
        )
    }

    private fun layerBypasses(): Set<String> =
        presentationFiles()
            .flatMap { file ->
                file.imports
                    .map { it.name }
                    .filter { FORBIDDEN_DATA_IMPORT.matches(it) }
                    .map { "${file.name}: $it" }
            }.toSet()

    /**
     * 프로덕션 소스의 presentation 파일. [AfternoteKonsistScope.productionFiles] 가 test 소스셋을 빼,
     * 테스트 소스의 조립 코드가 섞이지 않는다.
     */
    private fun presentationFiles() =
        AfternoteKonsistScope
            .productionFiles
            .withPackage("com.afternote..presentation..")

    private companion object {
        /**
         * `core:datastore` 전부와 `core:data` 의 구현 패키지.
         *
         * `core:data` 를 통째로 막지 않는 것은 그 모듈이 구현만 담고 있지 않기 때문이다 —
         * 계약 옆에 놓인 타입까지 함께 막으면 가드가 실제 위반이 아닌 것을 잡는다.
         * 막아야 할 것은 「구현을 이름으로 아는 것」 이다.
         */
        val FORBIDDEN_DATA_IMPORT =
            Regex("""^com\.afternote\.core\.datastore\..*$|^com\.afternote\.core\.data\.repoimpl\..*$""")

        /**
         * #635 (setting 4건 중 3번) 이 수복한다. `PassKeyViewModel` 이 `UserProfileRepository`
         * 를 건너뛰고 `UserProfileDataSource` 를 직접 주입받는다. setting 은 다른 담당이라
         * 이 PR 에서 고치지 않는다.
         */
        val KNOWN_LAYER_BYPASSES =
            setOf("PassKeyViewModel: com.afternote.core.datastore.UserProfileDataSource")
    }
}
