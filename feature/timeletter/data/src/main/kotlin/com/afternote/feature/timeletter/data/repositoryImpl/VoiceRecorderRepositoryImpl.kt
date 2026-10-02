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

internal class VoiceRecorderRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : VoiceRecorderRepository {
        private val recorderLock = Any()
        private var recorder: MediaRecorder? = null
        private var outputFile: File? = null
        private val retainedFiles = linkedSetOf<File>()
        private var startedAtMillis: Long = 0L

        init {
            // 이전 프로세스가 등록/삭제 없이 죽으면 filesDir 에 원본이 남는다. 새 인스턴스가
            // 생성될 때(=새 프로세스) 그 시점까지 아무도 추적하지 않는 파일은 전부 고아다 (#440 리뷰).
            ioDispatcher.dispatch(EmptyCoroutineContext) {
                synchronized(recorderLock) { sweepOrphanedFilesLocked() }
            }
        }

        override suspend fun start(): Result<Unit> =
            withContext(ioDispatcher) {
                runCatching {
                    synchronized(recorderLock) {
                        discardInternal()
                        val directory = File(context.filesDir, AUDIO_DIRECTORY).apply { mkdirs() }
                        val file = File(directory, "${UUID.randomUUID()}.$AUDIO_EXTENSION")
                        val mediaRecorder = createMediaRecorder()
                        try {
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            mediaRecorder.setAudioEncodingBitRate(AUDIO_BIT_RATE)
                            mediaRecorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                            mediaRecorder.setOutputFile(file.absolutePath)
                            mediaRecorder.prepare()
                            mediaRecorder.start()
                        } catch (failure: Throwable) {
                            mediaRecorder.runCatching { release() }
                            file.delete()
                            throw failure
                        }
                        recorder = mediaRecorder
                        outputFile = file
                        startedAtMillis = SystemClock.elapsedRealtime()
                    }
                }
            }

        override suspend fun stop(): Result<RecordedAudio> =
            withContext(ioDispatcher) {
                runCatching {
                    synchronized(recorderLock) {
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
                    }
                }.onFailure {
                    synchronized(recorderLock) { discardInternal() }
                }
            }

        override suspend fun discard() {
            withContext(ioDispatcher) { synchronized(recorderLock) { discardInternal() } }
        }

        override fun retainRecordedFile() {
            synchronized(recorderLock) {
                outputFile?.let(retainedFiles::add)
                outputFile = null
            }
        }

        override suspend fun deleteRecordedFile(uriString: String) {
            withContext(ioDispatcher) {
                val uri = android.net.Uri.parse(uriString)
                if (uri.authority != "${context.packageName}.timeletter.fileprovider") return@withContext
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: return@withContext
                val file = File(File(context.filesDir, AUDIO_DIRECTORY), fileName)
                synchronized(recorderLock) { retainedFiles.remove(file) }
                file.delete()
            }
        }

        override fun release() {
            val filesToDelete =
                synchronized(recorderLock) {
                    buildList {
                        releaseRecorder()?.let(::add)
                        addAll(retainedFiles)
                        retainedFiles.clear()
                    }
                }
            if (filesToDelete.isNotEmpty()) {
                ioDispatcher.dispatch(EmptyCoroutineContext) {
                    filesToDelete.forEach(File::delete)
                }
            }
        }

        private fun discardInternal() {
            releaseRecorder()?.delete()
        }

        private fun sweepOrphanedFilesLocked() {
            val directory = File(context.filesDir, AUDIO_DIRECTORY)
            val currentFile = outputFile
            directory.listFiles()?.forEach { file ->
                if (file != currentFile) file.delete()
            }
        }

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
