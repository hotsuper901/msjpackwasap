package com.msj.stickerstudio.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import com.msj.stickerstudio.PackStore
import com.msj.stickerstudio.WhatsAppHandler
import java.io.File

/**
 * Serves sticker pack data to WhatsApp over the official third-party contract:
 *
 *  - /metadata                    all packs (id, name, publisher, icon, flags)
 *  - /metadata/{packId}           one pack — WhatsApp's ENABLE_STICKER_PACK entry point
 *  - /stickers/{packId}           sticker file names + emojis + accessibility labels
 *  - /stickers_asset/{packId}/{name}   binary sticker data (AssetFileDescriptor)
 *  - /sticker/{packId}/{name}     binary data via our own stored URIs
 *  - /sticker_pack_tray_icon/{packId}  tray icon URI stored in the pack row
 *  - /tray/{packId}               legacy tray icon route
 */
class StickerContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.msj.stickerstudio.stickercontentprovider"

        private const val MATCHER_METADATA = 0
        private const val MATCHER_METADATA_ID = 1
        private const val MATCHER_TRAY = 2
        private const val MATCHER_TRAY_LEGACY = 3
        private const val MATCHER_STICKERS = 4
        private const val MATCHER_STICKER_ASSET = 5
        private const val MATCHER_STICKER = 6

        private const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
        private const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
        private const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
        private const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"
        private const val ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link"
        private const val IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link"
        private const val PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        private const val PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        private const val PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website"
        private const val LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website"
        private const val IMAGE_DATA_VERSION = "image_data_version"
        private const val AVOID_CACHE = "whatsapp_will_not_cache_stickers"
        private const val ANIMATED_STICKER_PACK = "animated_sticker_pack"

        private const val STICKER_FILE_NAME = "sticker_file_name"
        private const val STICKER_FILE_EMOJI = "sticker_emoji"
        private const val STICKER_FILE_SIZE = "sticker_size"
        private const val STICKER_ACCESSIBILITY_TEXT = "sticker_accessibility_text"

        private val METADATA_COLUMNS = arrayOf(
            STICKER_PACK_IDENTIFIER_IN_QUERY,
            STICKER_PACK_NAME_IN_QUERY,
            STICKER_PACK_PUBLISHER_IN_QUERY,
            STICKER_PACK_ICON_IN_QUERY,
            ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
            IOS_APP_DOWNLOAD_LINK_IN_QUERY,
            PUBLISHER_EMAIL,
            PUBLISHER_WEBSITE,
            PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE,
            IMAGE_DATA_VERSION,
            AVOID_CACHE,
            ANIMATED_STICKER_PACK
        )

        private val STICKER_COLUMNS = arrayOf(
            STICKER_FILE_NAME,
            STICKER_FILE_EMOJI,
            STICKER_FILE_SIZE,
            STICKER_ACCESSIBILITY_TEXT
        )

        fun buildTrayUri(packId: String): Uri =
            Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath("sticker_pack_tray_icon").appendPath(packId).build()

        fun buildStickerUri(packId: String, fileName: String): Uri =
            Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath("stickers_asset").appendPath(packId).appendPath(fileName).build()
    }

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "metadata", MATCHER_METADATA)
        addURI(AUTHORITY, "metadata/*", MATCHER_METADATA_ID)
        addURI(AUTHORITY, "tray/*", MATCHER_TRAY)
        addURI(AUTHORITY, "sticker_pack_tray_icon/*", MATCHER_TRAY_LEGACY)
        addURI(AUTHORITY, "stickers/*", MATCHER_STICKERS)
        addURI(AUTHORITY, "stickers_asset/*/*", MATCHER_STICKER_ASSET)
        addURI(AUTHORITY, "sticker/*/*", MATCHER_STICKER)
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = when (uriMatcher.match(uri)) {
        MATCHER_METADATA, MATCHER_METADATA_ID, MATCHER_STICKERS -> "text/plain"
        else -> "image/webp"
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val file = resolveBinaryFile(uri) ?: return null
        return try {
            if (mode.contains("w")) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
            } else {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val file = resolveBinaryFile(uri) ?: return null
        return try {
            if (mode.contains("w")) {
                AssetFileDescriptor(
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE),
                    0,
                    file.length()
                )
            } else {
                AssetFileDescriptor(
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY),
                    0,
                    file.length()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
        MATCHER_METADATA -> {
            val ctx = context ?: return null
            val cursor = MatrixCursor(METADATA_COLUMNS)
            PackStore.load(ctx).forEach { addPackRow(cursor, it) }
            cursor
        }

        MATCHER_METADATA_ID -> {
            val ctx = context ?: return null
            val packId = uri.pathSegments.getOrNull(1) ?: return null
            val cursor = MatrixCursor(METADATA_COLUMNS)
            PackStore.find(ctx, packId)?.let { addPackRow(cursor, it) }
            cursor
        }

        MATCHER_STICKERS -> enumerateStickers(uri)

        MATCHER_TRAY, MATCHER_TRAY_LEGACY, MATCHER_STICKER_ASSET, MATCHER_STICKER -> {
            // WhatsApp filters by pack identifier when probing icons on startup.
            if (!TextUtils.isEmpty(selection)) {
                val wanted = selectionArgs?.firstOrNull() ?: ""
                val packId = uri.pathSegments.getOrNull(1) ?: ""
                if (wanted.isNotEmpty() && packId.isNotEmpty() && !wanted.contains(packId)) {
                    return MatrixCursor(arrayOf("file_descriptor"))
                }
            }
            val file = resolveBinaryFile(uri) ?: return null
            val pfd = try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (_: Exception) {
                return null
            }
            MatrixCursor(arrayOf("file_descriptor")).apply {
                addRow(arrayOf<Any>(pfd))
            }
        }

        else -> throw IllegalArgumentException("Unsupported URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    // ---------- helpers ----------

    private fun addPackRow(cursor: MatrixCursor, r: PackStore.PackRecord) {
        cursor.addRow(
            arrayOf<Any>(
                r.id,
                r.name,
                r.publisher,
                buildTrayUri(r.id).toString(),
                "",             // android_play_store_link
                "",             // ios_app_download_link
                "msj.stickerstudio@gmail.com",
                "",             // publisher website
                "",             // privacy policy
                "",             // license agreement
                "1",            // image_data_version
                true,           // whatsapp_will_not_cache_stickers
                r.animated      // animated_sticker_pack
            )
        )
    }

    private fun enumerateStickers(uri: Uri): Cursor {
        val ctx = context ?: return MatrixCursor(STICKER_COLUMNS)
        val packId = uri.pathSegments.getOrNull(1)
            ?: return MatrixCursor(STICKER_COLUMNS)
        val record = PackStore.find(ctx, packId)
        val byName = record?.stickers?.associateBy { it.fileName } ?: emptyMap()
        val dir = WhatsAppHandler.stickerDir(ctx, packId)
        val cursor = MatrixCursor(STICKER_COLUMNS)
        dir.listFiles()?.filter { it.isFile }?.forEach { file ->
            val meta = byName[file.name]
            cursor.addRow(
                arrayOf<Any>(
                    file.name,
                    meta?.emojis?.takeIf { it.isNotBlank() } ?: "\uD83D\uDE00",
                    file.length(),
                    meta?.title?.takeIf { it.isNotBlank() } ?: "MSJ Sticker"
                )
            )
        }
        return cursor
    }

    private fun resolveBinaryFile(uri: Uri): File? {
        val ctx = context ?: return null
        return when (uriMatcher.match(uri)) {
            MATCHER_TRAY, MATCHER_TRAY_LEGACY -> {
                val packId = uri.pathSegments.getOrNull(1) ?: return null
                File(WhatsAppHandler.trayDir(ctx), "$packId.webp")
            }
            MATCHER_STICKER_ASSET, MATCHER_STICKER -> {
                val packId = uri.pathSegments.getOrNull(1) ?: return null
                val name = uri.pathSegments.getOrNull(2) ?: return null
                if (name.contains("..") || name.contains('/')) return null
                File(WhatsAppHandler.stickerDir(ctx, packId), name)
            }
            else -> null
        }
    }
}
