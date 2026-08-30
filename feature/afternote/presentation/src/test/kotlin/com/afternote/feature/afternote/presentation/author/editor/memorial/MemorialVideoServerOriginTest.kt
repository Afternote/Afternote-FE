package com.afternote.feature.afternote.presentation.author.editor.memorial

import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.author.editor.state.withMemorialThumbnail
import com.afternote.feature.afternote.presentation.author.editor.state.withMemorialVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 서버 영상을 로컬로 교체한 뒤 지우는 경로 (#1406).
 *
 * #1375 는 삭제 항목을 로컬 첨부에만 노출해 서버 미디어의 거짓 삭제를 막았다. 그런데 영상 축은 한
 * 칸이 로컬 픽과 원격 prefill 을 겸해, 서버 영상을 로컬 영상으로 **교체**하면 그 추론이 가드를
 * 통과시켰다 — 그걸 지우면 폼은 비지만 저장 뒤 서버 영상이 그대로 남는다(수정 계약이 삭제를
 * 표현하지 못한다).
 *
 * 지금은 두 칸이 갈려 있어(`pickedVideo`·`serverVideo`) 삭제가 고른 것만 걷어내고 표시는 서버
 * 원본으로 저절로 돌아간다. 지울 수 없는 것을 지운 척하지 않는 쪽이, 사후 전달이라 되돌릴 기회가
 * 제한적인 이 도메인에서 안전하다.
 */
class MemorialVideoServerOriginTest {
    private val serverVideo = "https://cdn.test/farewell.mp4"
    private val serverThumbnail = "https://cdn.test/farewell-thumb.jpg"
    private val localVideo = "content://videos/replacement"

    private fun prefilledEditForm(): EditorFormState =
        EditorFormState(
            typeForm =
                AfternoteTypeForm.fromPrefill(
                    EditorContentPrefill.Memorial(
                        videoUrl = serverVideo,
                        thumbnailUrl = serverThumbnail,
                        photoUrl = null,
                        playlistSongs = emptyList(),
                    ),
                ),
        )

    private fun EditorFormState.memorial(): AfternoteTypeForm.Memorial = typeForm as AfternoteTypeForm.Memorial

    @Test
    fun `수정 진입 prefill 은 서버 원본을 따로 기억한다`() {
        val form = prefilledEditForm().memorial()

        assertEquals(serverVideo, form.displayVideo()?.url)
        assertEquals(serverVideo, form.serverVideo?.url)
        assertEquals(serverThumbnail, form.serverVideo?.thumbnailUrl)
        assertNull("수정 진입만으로는 고른 영상이 없다", form.pickedVideo)
    }

    @Test
    fun `로컬 영상으로 교체해도 서버 원본은 남는다`() {
        val replaced = prefilledEditForm().withMemorialVideo(localVideo).memorial()

        assertEquals(localVideo, replaced.displayVideo()?.url)
        // 교체한 영상의 썸네일은 아직 없다 — 원본 썸네일을 물려주면 다른 영상의 그림이 붙는다.
        assertNull(replaced.displayVideo()?.thumbnailUrl)
        assertEquals(serverVideo, replaced.serverVideo?.url)
    }

    @Test
    fun `교체분을 지우면 빈 슬롯이 아니라 서버 영상으로 돌아간다`() {
        val removed =
            prefilledEditForm()
                .withMemorialVideo(localVideo)
                .withMemorialVideo(null)
                .memorial()

        // 종전에는 여기서 영상 칸이 null 이 되어 폼이 빈 슬롯을 보여줬다. 저장하면 서버 영상이 그대로
        // 남아 재진입 시 되살아났다 — 폼이 서버 상태를 두고 거짓말을 한 자리다.
        assertEquals(serverVideo, removed.displayVideo()?.url)
        assertEquals(serverThumbnail, removed.displayVideo()?.thumbnailUrl)
    }

    @Test
    fun `서버 영상으로 돌아가도 삭제 항목은 열려 있다`() {
        val removed =
            prefilledEditForm()
                .withMemorialVideo(localVideo)
                .withMemorialVideo(null)

        // #1561 은 여기서 삭제 항목이 «다시 감춰진다» 고 잠갔다. 그 근거는 계약 한계였다 —
        // PATCH 가 삭제를 표현하지 못하던 시절엔 서버 영상에 삭제를 열어 두면 폼만 비고 서버에는
        // 남는 거짓 삭제가 됐다. BE 가 «명시적 null = 삭제» 를 열면서(#1597) 그 근거가 사라졌으므로
        // 이 단언을 뒤집는다. 이제 서버로 돌아간 슬롯도 지울 수 있고, 지우면 실제로 지워진다.
        assertEquals(setOf(MemorialMediaTarget.VIDEO), removed.removableMemorialMediaTargets())
    }

    @Test
    fun `서버 원본이 없으면 삭제는 종전대로 슬롯을 비운다`() {
        // 생성 모드 — 되돌아갈 서버 상태가 없으므로 지운 그대로가 사실이다.
        val removed =
            EditorFormState(typeForm = AfternoteTypeForm.Memorial())
                .withMemorialVideo(localVideo)
                .withMemorialVideo(null)
                .memorial()

        assertNull(removed.displayVideo())
    }

    @Test
    fun `영상을 지운 뒤 도착한 썸네일은 버려진다`() {
        // 썸네일 업로드는 viewModelScope 라 삭제 뒤에 성공 콜백이 온다. 종전에는 두 칸이 따로 놀아
        // 「영상은 없는데 썸네일만 있는」 폼이 만들어졌다 — 그 상태는 이제 타입이 배제한다.
        val afterLateThumbnail =
            EditorFormState(typeForm = AfternoteTypeForm.Memorial())
                .withMemorialVideo(localVideo)
                .withMemorialVideo(null)
                .withMemorialThumbnail("https://cdn.test/late-thumb.jpg")
                .memorial()

        assertNull(afterLateThumbnail.pickedVideo)
        assertNull(afterLateThumbnail.displayVideo())
    }
}
