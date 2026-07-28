package app.repeatless.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

object AudioTrimmer {
    private const val TAG = "AudioTrimmer"

    fun trim(
        inputFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ): Boolean {
        if (!inputFile.exists() || inputFile.length() == 0L) return false
        val totalMs = endMs - startMs
        if (startMs <= 0L && totalMs <= 0L) {
            return false
        }

        val startUs = (startMs * 1000L).coerceAtLeast(0L)
        val endUs = (endMs * 1000L).coerceAtLeast(startUs + 100000L)

        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var trackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (trackIndex < 0 || format == null) {
                Log.e(TAG, "No audio track found in input file")
                return false
            }

            extractor.selectTrack(trackIndex)
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val maxBufferSize = if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                128 * 1024
            }

            if (outputFile.exists()) {
                outputFile.delete()
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var ptsOffsetUs = -1L

            while (true) {
                bufferInfo.offset = 0
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    break
                }

                val sampleTime = extractor.sampleTime
                if (endUs > 0 && sampleTime > (endUs + 50000L)) {
                    break
                }

                if (ptsOffsetUs < 0) {
                    ptsOffsetUs = sampleTime.coerceAtMost(startUs)
                }

                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = (sampleTime - ptsOffsetUs).coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            extractor.release()
            extractor = null

            return outputFile.exists() && outputFile.length() > 0L

        } catch (e: Exception) {
            Log.e(TAG, "Error trimming audio file", e)
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            return false
        }
    }
}
