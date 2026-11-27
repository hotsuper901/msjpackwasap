package com.msj.stickerstudio

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.ExecuteCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
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
     * No audio in the image asset — the music lives in the paired track export.
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

        // Palette pipeline: side chain palettegen + paletteuse with Bayer dithering.
        val vf = "fps=$fps," +
            "scale='if(gt(iw,ih),$size,-2)':'if(gt(iw,ih),-2,$size)':flags=lanczos," +
            "pad=$size:$size:(ow-iw)/2:(oh-ih)/2:0x00000000," +
            "split[s0][s1];[s0]palettegen=stats_mode=diff[p];" +
            "[s1][p]paletteuse=dither=bayer:bayer_scale=3"

        val cmd = when (format) {
            OutputFormat.ANIMATED_WEBP ->
                "-y -i $input -vf $vf -loop 0 -quality $quality -preset default ${fq(outFile.absolutePath)}"
            OutputFormat.GIF -> {
                val vfGif = vf.replace("0x00000000", "0x000000")
                "-y -i $input -vf $vfGif -loop 0 ${fq(outFile.absolutePath)}"
            }
            else -> return@withContext JobResult(false, "Use the music sticker converter for this format", null)
        }

        run(cmd, onProgress, outFile)
    }

    /**
     * Music sticker: WebM (VP9 video + Opus audio). Keeps the sound of the clip —
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
            "-y -i $input -an -vf $vf -c:v libvpx-vp9 -b:v 900k -r $fps " +
                "-deadline good -cpu-used 4 -pix_fmt yuv420p " +
                "-i $input -map 0:v -map 1:a? -c:a libopus -b:a 96k -shortest " +
                "${fq(outFile.absolutePath)}"
        } else {
            "-y -i $input -an -vf $vf -c:v libvpx-vp9 -b:v 900k -r $fps " +
                "-deadline good -cpu-used 4 -pix_fmt yuv420p " +
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

    /** Maps a content:// URI to an ffmpeg-safe path, falling back to a cache copy. */
    private fun resolveInput(context: Context, inputUri: Uri): String? =
        FFmpegKitConfig.getSafParameterForRead(context, inputUri) ?: run {
            val tmp = File(context.cacheDir, "input_${System.currentTimeMillis()}.mp4")
            if (WhatsAppHandler.copyUriToFile(context, inputUri, tmp)) tmp.absolutePath else null
        }

    private suspend fun run(
        command: String,
        onProgress: ((Int) -> Unit)?,
        outFile: File
    ): JobResult {
        var lastPct = 0
        val session = FFmpegKit.executeAsync(
            command,
            StatisticsCallback { s ->
                val pct = s.time.toInt().coerceIn(0, 100)
                if (pct != lastPct) {
                    lastPct = pct
                    onProgress?.invoke(pct)
                }
            },
            LogCallback { },
            ExecuteCallback { }
        )
        val returnCode = session.returnCode
        return if (ReturnCode.isSuccess(returnCode) && outFile.exists() && outFile.length() > 0) {
            JobResult(true, "Done", outFile)
        } else {
            JobResult(false, "Conversion failed (code $returnCode). Try another format or quality.", null)
        }
    }
}
