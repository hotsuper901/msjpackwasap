package com.msj.stickerstudio

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.msj.stickerstudio.data.StickerContentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Wraps the WhatsApp sticker content-provider API.
 * Callbacks: onAlreadyExists -> close, onSuccess -> success, onFailure(reason) -> fail.
 */
object WhatsAppHandler {

    const val CONTENT_PROVIDER_AUTHORITY = "com.whatsapp.stickerapp.provider"
    private const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
    private const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
    private const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
    private const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"
    private const val STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji"
    private const val STICKER_FILE_NAME_IN_QUERY = "sticker_file_name"

    private val stickerPackURI = Uri.parse("content://$CONTENT_PROVIDER_AUTHORITY/sticker_packs")
    private val stickerDetailsURI = Uri.parse("content://$CONTENT_PROVIDER_AUTHORITY/sticker_pack_details")

    /** WhatsApp sticker packs support up to 30 stickers per pack. */
    private const val ADD_STICKER_MAX = 30

    /** Working directories used by the converter + content provider. */
    fun workDir(context: Context): File =
        File(context.filesDir, "msj_studio").apply { mkdirs() }

    fun trayDir(context: Context): File =
        File(workDir(context), "tray").apply { mkdirs() }

    fun stickerDir(context: Context, packId: String): File =
        File(workDir(context), "stickers/$packId").apply { mkdirs() }

    fun exportsDir(context: Context): File =
        File(workDir(context), "exports").apply { mkdirs() }

    fun isWhatsAppInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (_: Exception) {
            false
        }

    /** Reads the sticker packs already registered inside WhatsApp. */
    fun getStickerPacks(context: Context): List<StickerPack> {
        val packs = mutableListOf<StickerPack>()
        try {
            context.contentResolver.query(
                stickerPackURI, null, null, null, null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(STICKER_PACK_IDENTIFIER_IN_QUERY)
                val nameIdx = cursor.getColumnIndexOrThrow(STICKER_PACK_NAME_IN_QUERY)
                val pubIdx = cursor.getColumnIndexOrThrow(STICKER_PACK_PUBLISHER_IN_QUERY)
                val iconIdx = cursor.getColumnIndexOrThrow(STICKER_PACK_ICON_IN_QUERY)
                while (cursor.moveToNext()) {
                    packs += StickerPack(
                        id = cursor.getString(idIdx),
                        name = cursor.getString(nameIdx),
                        publisher = cursor.getString(pubIdx),
                        iconUri = cursor.getString(iconIdx)
                    )
                }
            }
        } catch (_: Exception) { }
        return packs
    }

    /**
     * Adds a pack to WhatsApp and inserts every sticker.
     * trayIcon: 96x96 WebP file. stickers: WebP files 512x512.
     */
    fun addStickerPack(
        context: Context,
        packName: String,
        trayIcon: File,
        stickers: List<Sticker>
    ): AddResult {
        try {
            if (!isWhatsAppInstalled(context)) {
                return AddResult(false, "WhatsApp is not installed on this device")
            }
            if (stickers.isEmpty() || stickers.size > ADD_STICKER_MAX) {
                return AddResult(false, "A pack needs 1 to $ADD_STICKER_MAX stickers")
            }
            if (!trayIcon.exists() || trayIcon.length() == 0L) {
                return AddResult(false, "Tray icon is missing")
            }

            val identifier = java.util.UUID.randomUUID().toString()
            val trayUri = StickerContentProvider.buildTrayUri(identifier)

            // Place assets where our content provider serves them from.
            val targetTray = File(trayDir(context), "$identifier.webp")
            trayIcon.copyTo(targetTray, overwrite = true)
            val stickerTargets = stickers.map { s ->
                val dir = stickerDir(context, identifier)
                val target = File(dir, s.file.name)
                s.file.copyTo(target, overwrite = true)
                Sticker(target, s.emojis)
            }

            // 1) Register the pack
            val packValues = android.content.ContentValues().apply {
                put(STICKER_PACK_IDENTIFIER_IN_QUERY, identifier)
                put(STICKER_PACK_NAME_IN_QUERY, packName)
                put(STICKER_PACK_PUBLISHER_IN_QUERY, "M.S.J")
                put(STICKER_PACK_ICON_IN_QUERY, trayUri.toString())
            }
            val packRes = context.contentResolver.insert(stickerPackURI, packValues)
                ?: return AddResult(false, "WhatsApp rejected the pack")

            // 2) Register each sticker
            for ((index, sticker) in stickerTargets.withIndex()) {
                if (!sticker.file.exists() || sticker.file.length() == 0L) {
                    return AddResult(false, "Sticker ${index + 1} is empty")
                }
                val stickerUri = StickerContentProvider.buildStickerUri(identifier, sticker.file.name)
                val values = android.content.ContentValues().apply {
                    put(STICKER_PACK_IDENTIFIER_IN_QUERY, identifier)
                    put(STICKER_FILE_NAME_IN_QUERY, stickerUri.lastPathSegment)
                    put(STICKER_FILE_EMOJI_IN_QUERY, sticker.emojis.joinToString(","))
                }
                val res = context.contentResolver.insert(stickerDetailsURI, values)
                if (res == null) {
                    return AddResult(false, "WhatsApp rejected sticker ${index + 1}")
                }
            }

            // 3) Trigger the "added" broadcast so WhatsApp shows it immediately
            val intent = android.content.Intent("com.whatsapp.intent.action.STICKER_PACK_ADDED")
            intent.setPackage("com.whatsapp")
            context.sendBroadcast(intent)

            return AddResult(true, identifier)
        } catch (e: Exception) {
            return AddResult(false, e.message ?: "Unknown error while adding to WhatsApp")
        }
    }

    data class AddResult(val success: Boolean, val message: String)

    /** Copies the content behind a URI into a local cache file. */
    fun copyUriToFile(context: Context, uri: Uri, target: File): Boolean =
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            target.exists() && target.length() > 0
        } catch (_: Exception) {
            false
        }

    fun queryDisplayName(context: Context, uri: Uri): String {
        var name = "video_${System.currentTimeMillis()}"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx) ?: name
                }
            }
        } catch (_: Exception) { }
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
