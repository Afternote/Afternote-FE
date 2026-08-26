import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildFingerprintTest {
    private val fullSha = "a3f91c2b8d4e5f60718293a4b5c6d7e8f9012345"

    @Test
    fun `커밋을 읽었고 워킹트리가 깨끗하면 short sha 만 붙인다`() {
        assertEquals("+a3f91c2", debugVersionNameSuffix(fullSha, hasUncommittedChanges = false))
    }

    @Test
    fun `커밋 안 된 변경이 있으면 dirty 를 함께 붙인다`() {
        assertEquals("+a3f91c2-dirty", debugVersionNameSuffix(fullSha, hasUncommittedChanges = true))
    }

    @Test
    fun `커밋을 특정할 수 없으면 접미사를 생략하지 않고 unknown 을 남긴다`() {
        assertEquals("+$UNKNOWN_COMMIT_MARKER", debugVersionNameSuffix(null, hasUncommittedChanges = false))
        // 커밋을 모르면 무엇 대비 dirty 인지도 말할 수 없다 — dirty 를 붙이지 않는다.
        assertEquals("+$UNKNOWN_COMMIT_MARKER", debugVersionNameSuffix(null, hasUncommittedChanges = true))
    }

    @Test
    fun `sha 는 앞 7자리로 정규화한다`() {
        assertEquals("a3f91c2", shortCommitSha(fullSha))
        assertEquals("a3f91c2", shortCommitSha("a3f91c2"))
        assertEquals("a3f91c2", shortCommitSha("  A3F91C2B8D  \n"))
    }

    @Test
    fun `16진수가 아니거나 7자리에 못 미치는 값은 커밋으로 받지 않는다`() {
        listOf("", "   ", "a3f91c", "z3f91c2", "a3f91c2-dirty", "HEAD", "$fullSha" + "0").forEach { invalid ->
            assertNull("커밋으로 받으면 안 된다: '$invalid'", shortCommitSha(invalid))
        }
    }

    @Test
    fun `정규화에 실패한 값은 커밋 없이 빌드한 것과 같게 다룬다`() {
        assertEquals("+$UNKNOWN_COMMIT_MARKER", debugVersionNameSuffix("not-a-sha", hasUncommittedChanges = false))
    }
}
