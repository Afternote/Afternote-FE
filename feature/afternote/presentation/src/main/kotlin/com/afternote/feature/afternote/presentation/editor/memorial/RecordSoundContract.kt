package com.afternote.feature.afternote.presentation.editor.memorial

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

/**
 * 녹음 앱을 띄워 결과 음성 URI 를 받는 계약 (#1118).
 *
 * `ActivityResultContracts` 에 대응물이 없어 직접 짠다. 사진·영상 촬영
 * (`TakePicture`·`CaptureVideo`) 은 **우리가 만든 출력 URI 를 넘기고** 성공 여부만 boolean 으로 받는데,
 * `MediaStore.Audio.Media.RECORD_SOUND_ACTION` 은 반대다 — 녹음 앱이 자기 저장소에 파일을 만들고
 * 그 URI 를 결과 `Intent.data` 로 돌려준다. 그래서 출력 URI 를 미리 만들 필요도, 취소 시 지울 필요도 없다.
 *
 * 결과 URI 의 실제 형식은 기기마다 다르다(m4a·3gp·amr…). 서버가 받는 형식인지는 호출부가
 * [com.afternote.feature.afternote.domain.repository.author.MemorialAudioFormats] 로 다시 가른다.
 */
internal class RecordSoundContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(
        context: Context,
        input: Unit,
    ): Intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Uri? = intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}
