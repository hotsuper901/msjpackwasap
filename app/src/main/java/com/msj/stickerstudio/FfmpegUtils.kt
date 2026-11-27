package com.msj.stickerstudio

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.StatisticsCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * FFmpeg-powered converters. All jobs run through ffmpeg-kit (audio build).
 * Progress callbacks receive [0..100].
 *
 * Quoting rule: ffmpeg-kit tokenizes the command on whitespace, so any value
 * that may contain spaces is wrapped in single quotes; filtergraphs below are
 * written without spaces so they stay intact as single tokens.
 */
object FfmpegUtils {

    data class JobResult(
        val success: Boolean,
        val message: String,
        val outputFile: File? = null
    )

    private fun fq(path: String): String = "'$path'"

    /**
     * Converts the video into a WhatsApp sticker asset (animated WebP or GIF).
     * Auto-shrinks (fps/quality) if the result exceeds the 1MB WhatsApp limit.
     */
    suspend fun convertToSticker(
        context: Context,
        inputUri: Uri,
        format: OutputFormat,
        size: Int,
        fps: Int,
        quality: Int,
        outFile: File,
        onProgress: (Int) -> Unit
    ): JobResult = withContext(Dispatchers.IO) {
        val input = resolveInput(context, inputUri)
            ?: return@withContext JobResult(false, "Could not read the selected video", null)

        var fpsNow = fps
        var qualityNow = quality
        var attempt = 0
        var result: JobResult
        do {
            val vf = buildStickerVf(size, fpsNow, format)
            val cmd = when (format) {
                OutputFormat.ANIMATED_WEBP ->
                    "-y -i $input -vf $vf -loop 0 -quality $qualityNow -preset default ${fq(outFile.absolutePath)}"
                OutputFormat.GIF ->
                    "-y -i $input -vf $vf -loop 0 ${fq(outFile.absolutePath)}"
                else -> return@withContext JobResult(false, "Use the music sticker converter for this format", null)
            }
            result = run(cmd, onProgress, outFile)
            attempt++
            if (result.success && outFile.length() > 900_000 && fpsNow > 5) {
                fpsNow = maxOf(5, fpsNow / 2)
                qualityNow = maxOf(30, qualityNow - 25)
            } else {
                break
            }
        } while (attempt < 3)
        result
    }

    /** Palette pipeline: side chain palettegen + paletteuse with Bayer dithering. */
    private fun buildStickerVf(size: Int, fps: Int, format: OutputFormat): String {
        val padColor = if (format == OutputFormat.GIF) "0x000000" else "0x00000000"
        return "fps=$fps," +
            "scale='if(gt(iw,ih),$size,-2)':'if(gt(iw,ih),-2,$size)':flags=lanczos," +
            "pad=$size:$size:(ow-iw)/2:(oh-ih)/2:$padColor," +
            "split[s0][s1];[s0]palettegen=stats_mode=diff[p];" +
            "[s1][p]paletteuse=dither=bayer:bayer_scale=3"
    }

    /**
     * Music sticker: WebM (VP8 video + Opus audio). Keeps the sound of the clip —
     * export/share it, or send it to WhatsApp as a video message.
     */
    suspend fun convertToMusicSticker(
        context: Context,
        inputUri: Uri,
        size: Int,
        fps: Int,
        withMusic: Boolean,
        outFile: File,
        onProgress: (Int) -> Unit
    ): JobResult = withContext(Dispatchers.IO) {
        val input = resolveInput(context, inputUri)
            ?: return@withContext JobResult(false, "Could not read the selected video", null)

        val vf = "fps=$fps,scale=$size:$size:force_original_aspect_ratio=increase,crop=$size:$size"
        val cmd = if (withMusic) {
            "-y -i $input -map 0:v:0 -map 0:a:0? -vf $vf " +
                "-c:v libvpx -b:v 1M -crf 10 -pix_fmt yuv420p -r $fps " +
                "-c:a libopus -b:a 96k -shortest " +
                "${fq(outFile.absolutePath)}"
        } else {
            "-y -i $input -map 0:v:0 -vf $vf " +
                "-c:v libvpx -b:v 1M -crf 10 -pix_fmt yuv420p -r $fps -an " +
                "${fq(outFile.absolutePath)}"
        }

        run(cmd, onProgress, outFile)
    }

    /** Extracts the soundtrack of the video as an MP3 ringtone. */
    suspend fun extractRingtone(
        context: Context,
        inputUri: Uri,
        outFile: File,
        onProgress: (Int) -> Unit
    ): JobResult = withContext(Dispatchers.IO) {
        val input = resolveInput(context, inputUri)
            ?: return@withContext JobResult(false, "Could not read the selected video", null)
        val cmd = "-y -i $input -vn -ac 2 -ar 44100 -b:a 192k -codec:a libmp3lame ${fq(outFile.absolutePath)}"
        run(cmd, onProgress, outFile)
    }

    /** Converts a single frame into the 96x96 WebP tray icon for a pack. */
    suspend fun makeTrayIcon(
        context: Context,
        inputUri: Uri,
        outFile: File,
        atSeconds: Double = 0.0
    ): JobResult = withContext(Dispatchers.IO) {
        val input = resolveInput(context, inputUri)
            ?: return@withContext JobResult(false, "Could not read the selected video", null)
        val cmd = "-y -ss $atSeconds -i $input -frames:v 1 " +
            "-vf scale=96:96:force_original_aspect_ratio=increase,crop=96:96 " +
            "-quality 95 ${fq(outFile.absolutePath)}"
        run(cmd, null, outFile)
    }

    /** Copies the picked video to a plain cache file so FFmpeg always gets a vanilla path. */
    private fun resolveInput(context: Context, inputUri: Uri): String? {
        val tmp = File(context.cacheDir, "in_${System.currentTimeMillis()}.mp4")
        return if (WhatsAppHandler.copyUriToFile(context, inputUri, tmp)) tmp.absolutePath else null
    }

    private suspend fun run(
        command: String,
        onProgress: ((Int) -> Unit)?,
        outFile: File
    ): JobResult {
        var lastPct = 0
        val latch = java.util.concurrent.CountDownLatch(1)
        var returnCode: ReturnCode? = null
        val logBuf = java.util.Collections.synchronizedList(mutableListOf<String>())

        // 6.0.LTS signature: (command, completeCallback, logCallback, statisticsCallback)
        FFmpegKit.executeAsync(
            command,
            FFmpegSessionCompleteCallback { session ->
                returnCode = session.returnCode
                latch.countDown()
            },
            LogCallback { log ->
                val line = log.message ?: return@LogCallback
                synchronized(logBuf) {
                    if (logBuf.size > 150) logBuf.removeAt(0)
                    logBuf.add(line)
                }
            },
            StatisticsCallback { s ->
                val pct = s.time.toInt().coerceIn(0, 100)
                if (pct != lastPct) {
                    lastPct = pct
                    onProgress?.invoke(pct)
                }
            }
        )
        latch.await()

        return if (returnCode != null && ReturnCode.isSuccess(returnCode!!) &&
            outFile.exists() && outFile.length() > 0
        ) {
            JobResult(true, "Done", outFile)
        } else {
            val tail = synchronized(logBuf) { logBuf.takeLast(6) }
                .joinToString("\n")
                .take(700)
            when {
                tail.isNotBlank() -> JobResult(
                    false,
                    "Conversion failed (code $returnCode):\n$tail",
                    null
                )
                else -> {
                    // No log output at all — check whether the engine itself runs.
                    val probe = FFmpegKit.execute("-hide_banner -version")
                    val probeCode = probe.returnCode
                    if (ReturnCode.isSuccess(probeCode)) {
                        JobResult(
                            false,
                            "Conversion failed (code $returnCode) with no engine output.\n" +
                                "The engine is fine — this video may be unreadable. Try another clip.",
                            null
                        )
                    } else {
                        JobResult(
                            false,
                            "FFmpeg engine check failed (code $probeCode). Reinstall the app and try again.",
                            null
                        )
                    }
                }
            }
        }
    }
}
