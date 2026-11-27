package com.msj.stickerstudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.msj.stickerstudio.data.StickerContentProvider
import java.io.File
import java.io.FileOutputStream

/**
 * WhatsApp sticker integration.
 *
 * The officially supported flow (and the one current WhatsApp versions accept)
 * is: this app hosts the pack behind its own content provider, then fires
 * `com.whatsapp.intent.action.ENABLE_STICKER_PACK` with the provider authority,
 * pack id and pack name. WhatsApp shows its "Add pack?" dialog, then pulls the
 * pack metadata and sticker WebP assets from our provider.
 */
object WhatsAppHandler {

    const val CONTENT_PROVIDER_AUTHORITY = "com.whatsapp.stickerapp.provider"

    const val ACTION_ENABLE_STICKER_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    private const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
    private const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
    private const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"

    private const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
    private const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
    private const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
    private const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"

    private val stickerPackURI = Uri.parse("content://$CONTENT_PROVIDER_AUTHORITY/sticker_packs")

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

    fun whatsAppVersion(context: Context): String =
        try {
            context.packageManager.getPackageInfo("com.whatsapp", 0).versionName ?: "?"
        } catch (_: Exception) {
            "?"
        }

    /** Builds a plain-text diagnostic for the pack-add step. */
    fun diagnose(context: Context, identifier: String, trayFile: File, stickerFile: File): String {
        val registered = if (identifier.isNotEmpty()) {
            getStickerPacks(context).any { it.id == identifier }
        } else {
            false
        }
        val stored = if (identifier.isNotEmpty()) {
            PackStore.find(context, identifier) != null
        } else {
            false
        }
        return buildString {
            appendLine("Pack registered in WhatsApp: ${if (registered) "YES" else "NO"}")
            appendLine("Pack stored locally: ${if (stored) "YES" else "NO"}")
            appendLine("Tray icon: ${trayFile.length() / 1024} KB (${if (trayFile.exists()) "present" else "missing"})")
            appendLine("Sticker: ${stickerFile.length() / 1024} KB (${if (stickerFile.exists()) "present" else "missing"})")
            appendLine("WhatsApp v${whatsAppVersion(context)}")
        }
    }

    /** Reads the sticker packs already registered inside WhatsApp (best effort — may be empty on newer builds). */
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
     * Packs this app created: WhatsApp's own listing when readable, otherwise
     * the local registry (which is always current).
     */
    fun getRegisteredPacks(context: Context): List<StickerPack> {
        val fromWhatsApp = getStickerPacks(context)
        if (fromWhatsApp.isNotEmpty()) return fromWhatsApp
        return PackStore.load(context).map { r ->
            StickerPack(
                id = r.id,
                name = r.name,
                publisher = r.publisher,
                iconUri = StickerContentProvider.buildTrayUri(r.id).toString()
            )
        }
    }

    data class AddResult(
        val success: Boolean,
        val message: String,
        val packId: String? = null,
        val intent: Intent? = null
    )

    /**
     * Stages the pack behind our content provider, stores it in the local
     * registry and returns the ENABLE_STICKER_PACK intent. Launch the intent
     * to let WhatsApp show its confirmation dialog.
     *
     * trayIcon: 96x96 WebP file. stickers: WebP files (WhatsApp packs are WebP-only).
     */
    fun preparePack(
        context: Context,
        packName: String,
        trayIcon: File,
        stickers: List<Sticker>,
        animated: Boolean,
        stickerTitle: String,
        packId: String = java.util.UUID.randomUUID().toString()
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

            // Stage assets where our content provider serves them from.
            val targetTray = File(trayDir(context), "$packId.webp")
            trayIcon.copyTo(targetTray, overwrite = true)

            val records = mutableListOf<PackStore.StickerRecord>()
            stickers.forEach { s ->
                if (!s.file.exists() || s.file.length() == 0L) {
                    return AddResult(false, "A sticker file is empty")
                }
                val dir = stickerDir(context, packId)
                val target = File(dir, s.file.name)
                s.file.copyTo(target, overwrite = true)
                records += PackStore.StickerRecord(
                    fileName = s.file.name,
                    emojis = s.emojis.joinToString(","),
                    title = stickerTitle.take(250)
                )
            }

            PackStore.add(
                context,
                PackStore.PackRecord(
                    id = packId,
                    name = packName,
                    publisher = "M.S.J",
                    animated = animated,
                    stickers = records
                )
            )

            val intent = buildAddIntent(packId, packName)
            return AddResult(true, packId, packId, intent)
        } catch (e: Exception) {
            return AddResult(false, e.message ?: "Unknown error while preparing the pack")
        }
    }

    /** Intent that makes WhatsApp show its "Add pack" confirmation dialog. */
    fun buildAddIntent(packId: String, packName: String): Intent =
        Intent(ACTION_ENABLE_STICKER_PACK).apply {
            putExtra(EXTRA_STICKER_PACK_ID, packId)
            putExtra(EXTRA_STICKER_PACK_AUTHORITY, StickerContentProvider.AUTHORITY)
            putExtra(EXTRA_STICKER_PACK_NAME, packName)
            setPackage("com.whatsapp")
        }

    /** Re-add intent for a pack we staged earlier; null if it's no longer stored. */
    fun reAddIntent(context: Context, packId: String, packName: String): Intent? {
        if (PackStore.find(context, packId) == null) return null
        return buildAddIntent(packId, packName)
    }

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
