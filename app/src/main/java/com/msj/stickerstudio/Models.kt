package com.msj.stickerstudio

import java.io.File

/** A WhatsApp sticker pack as reported by the content provider. */
data class StickerPack(
    val id: String,
    val name: String,
    val publisher: String,
    val iconUri: String
)

/** One sticker inside a pack. */
data class Sticker(
    val file: File,
    val emojis: List<String>
)

/** Output format for the converter. */
enum class OutputFormat(val label: String, val ext: String) {
    ANIMATED_WEBP("Animated WebP", ".webp"),
    GIF("GIF", ".gif"),
    MUSIC_STICKER("Music Sticker", ".webm"),
    RINGTONE("Ringtone (MP3)", ".mp3")
}

/** Conversion settings chosen in the editor. */
data class ConvertSettings(
    val format: OutputFormat = OutputFormat.ANIMATED_WEBP,
    val size: Int = 512,          // square output edge, px
    val fps: Int = 15,
    val quality: Int = 70,        // 1..100
    val withMusic: Boolean = true,
    val emojis: List<String> = listOf("\uD83D\uDE00"),
    val stickerTitle: String = "MSJ Sticker",
    val packName: String = "M.S.J Pack"
)
