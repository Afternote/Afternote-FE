package com.afternote.konsist

import org.junit.Test
import java.io.File

/**
 * 정렬 아이콘(왼쪽·가운데·오른쪽) 사본 재분열 가드 (#1404 · #635).
 *
 * 마인드레코드와 타임레터는 같은 정렬 아이콘을 **각자의 사본**으로 들고 있었다. 사본이 갈리면
 * 눈에 안 보이는 방식으로 어긋난다 — 실제로 #1668 의 core 승격본은 처음에 `strokeAlpha="0.6"` 을
 * 빠뜨렸다. 두 경로 다 `tint` 를 얹으므로 한쪽만 alpha 가 더 곱해져, 같은 2.4 스트로크인데도
 * 화면에 찍히는 농도가 달랐다. 컴파일도 스크린샷도 이걸 잡지 못한다 — 각자 자기 사본을 보니까.
 *
 * 그래서 「같아야 한다」가 아니라 **「한 벌만 존재한다」** 를 지킨다. 벌이 하나면 어긋날 자리가 없다.
 */
class AlignIconSharedAssetKonsistTest {
    /**
     * 정렬 아이콘 벡터는 `core:ui` 밖에 있으면 안 된다.
     *
     * 이름 규칙이 아니라 **경로**로 판정한다 — 사본은 늘 새 이름을 달고 돌아오기 때문이다
     * (`ic_align_left` → `mindrecord_align_left`).
     */
    @Test
    fun `정렬 아이콘 벡터는 core ui 한 벌만 있다`() {
        val strays = alignIconVectors().filterNot { it.path.contains(CORE_UI_DRAWABLE) }

        check(strays.isEmpty()) {
            buildString {
                appendLine("`core:ui` 밖에 정렬 아이콘 사본이 있다 (${strays.size}건).")
                appendLine("$CORE_UI_RESOURCE_PREFIX 승격본을 참조한다 — 사본을 두면 alpha·스트로크가 조용히 갈린다 (#1404).")
                appendLine()
                strays.map { it.toRelativeString(AfternoteKonsistScope.projectRoot) }.sorted().forEach { appendLine("  $it") }
            }
        }
    }

    /**
     * 승격본이 실제로 있어야 한다.
     *
     * 위 검사는 「밖에 없다」만 보므로, 승격본까지 통째로 사라지면 둘 다 초록으로 통과한다.
     */
    @Test
    fun `core ui 승격본 세 벌이 실재한다`() {
        val promoted = alignIconVectors().map { it.nameWithoutExtension }.sorted()

        check(promoted == EXPECTED_PROMOTED) {
            "정렬 아이콘 승격본이 기대와 다르다. 기대=$EXPECTED_PROMOTED 실제=$promoted"
        }
    }

    /**
     * 호출부는 승격본 이름으로만 정렬 아이콘을 집는다.
     *
     * 파일만 지우고 참조가 남으면 빌드가 깨져 바로 드러나지만, **새 사본을 다른 모듈에 만들고**
     * 옛 이름으로 참조하는 경우는 위 두 검사가 잡은 뒤에도 이 검사가 이유를 짚어 준다.
     */
    @Test
    fun `호출부는 승격본 이름으로 정렬 아이콘을 참조한다`() {
        val offenders =
            AfternoteKonsistScope.files
                .filter { file -> LEGACY_REFERENCE.containsMatchIn(file.text) }
                .map { it.path.substringAfter(AfternoteKonsistScope.projectRoot.path + File.separator) }

        check(offenders.isEmpty()) {
            buildString {
                appendLine("승격 전 이름으로 정렬 아이콘을 참조하는 파일이 있다 (${offenders.size}건).")
                appendLine("$CORE_UI_RESOURCE_PREFIX 로 바꾼다 (#1404).")
                appendLine()
                offenders.sorted().forEach { appendLine("  $it") }
            }
        }
    }

    /** 모듈 `src` 아래 전수에서 정렬 아이콘 벡터를 찾는다. 스캔 뿌리는 [AfternoteKonsistScope] 와 같다. */
    private fun alignIconVectors(): List<File> =
        AfternoteKonsistScope
            .scanRoots()
            .flatMap { root -> root.walkTopDown().filter { it.isFile && it.isAlignIconVector() } }

    private fun File.isAlignIconVector(): Boolean =
        extension == VECTOR_EXTENSION && parentFile?.name?.startsWith(DRAWABLE_DIR) == true && ALIGN_ICON_NAME.matches(nameWithoutExtension)

    private companion object {
        /** `..._align_left` · `..._align_center` · `..._align_right` — 접두는 모듈마다 다르므로 열어 둔다. */
        val ALIGN_ICON_NAME = Regex("""^.*_align_(left|center|right)$""")

        /** 승격 전 이름으로 정렬 아이콘을 집는 호출부. `R.drawable.` 이 붙은 것만 본다. */
        val LEGACY_REFERENCE = Regex("""R\.drawable\.(?!core_ui_)\w*ic?_?align_(left|center|right)\b""")

        val EXPECTED_PROMOTED = listOf("core_ui_ic_align_center", "core_ui_ic_align_left", "core_ui_ic_align_right")

        val CORE_UI_DRAWABLE = "core${File.separator}ui${File.separator}src"
        const val CORE_UI_RESOURCE_PREFIX = "core_ui_ic_align_*"
        const val DRAWABLE_DIR = "drawable"
        const val VECTOR_EXTENSION = "xml"
    }
}
