package com.afternote.feature.timeletter.data.repositoryImpl

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.core.content.FileProvider
import com.afternote.core.common.di.IoDispatcher
import com.afternote.feature.timeletter.domain.model.RecordedAudio
import com.afternote.feature.timeletter.domain.repository.VoiceRecorderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

class VoiceRecorderRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : VoiceRecorderRepository {
        private var recorder: MediaRecorder? = null
        private var outputFile: File? = null
        private var startedAtMillis: Long = 0L

        override suspend fun start(): Result<Unit> =
            withContext(ioDispatcher) {
                runCatching {
                    discardInternal()
                    val directory = File(context.filesDir, AUDIO_DIRECTORY).apply { mkdirs() }
                    val file = File(directory, "${UUID.randomUUID()}.$AUDIO_EXTENSION")
                    val mediaRecorder = createMediaRecorder()
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    mediaRecorder.setAudioEncodingBitRate(AUDIO_BIT_RATE)
                    mediaRecorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                    mediaRecorder.setOutputFile(file.absolutePath)
                    mediaRecorder.prepare()
                    mediaRecorder.start()
                    recorder = mediaRecorder
                    outputFile = file
                    startedAtMillis = SystemClock.elapsedRealtime()
                }
            }

        override suspend fun stop(): Result<RecordedAudio> =
            withContext(ioDispatcher) {
                runCatching {
                    val activeRecorder = checkNotNull(recorder) { "Voice recording has not started" }
                    val file = checkNotNull(outputFile) { "Voice recording file is missing" }
                    val durationMillis = SystemClock.elapsedRealtime() - startedAtMillis
                    try {
                        activeRecorder.stop()
                    } finally {
                        activeRecorder.release()
                        recorder = null
                    }
                    val uri =
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.timeletter.fileprovider",
                            file,
                        )
                    RecordedAudio(
                        uriString = uri.toString(),
                        fileName = file.name,
                        mimeType = AUDIO_MIME_TYPE,
                        durationMillis = durationMillis,
                    )
                }.onFailure {
                    discardInternal()
                }
            }

        override suspend fun discard() {
            withContext(ioDispatcher) { discardInternal() }
        }

        override fun retainRecordedFile() {
            outputFile = null
        }

        override suspend fun deleteRecordedFile(uriString: String) {
            withContext(ioDispatcher) {
                val uri = android.net.Uri.parse(uriString)
                if (uri.authority != "${context.packageName}.timeletter.fileprovider") return@withContext
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: return@withContext
                File(File(context.filesDir, AUDIO_DIRECTORY), fileName).delete()
            }
        }

        override fun release() {
            val fileToDelete = releaseRecorder()
            fileToDelete?.let { file ->
                ioDispatcher.dispatch(EmptyCoroutineContext) { file.delete() }
            }
        }

        @Synchronized
        private fun discardInternal() {
            releaseRecorder()?.delete()
        }

        @Synchronized
        private fun releaseRecorder(): File? {
            recorder?.runCatching { stop() }
            recorder?.release()
            recorder = null
            val file = outputFile
            outputFile = null
            startedAtMillis = 0L
            return file
        }

        @Suppress("DEPRECATION")
        private fun createMediaRecorder(): MediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

        private companion object {
            const val AUDIO_DIRECTORY = "timeletter_audio"
            const val AUDIO_EXTENSION = "m4a"
            const val AUDIO_MIME_TYPE = "audio/mp4"
            const val AUDIO_BIT_RATE = 128_000
            const val AUDIO_SAMPLE_RATE = 44_100
        }
    }
