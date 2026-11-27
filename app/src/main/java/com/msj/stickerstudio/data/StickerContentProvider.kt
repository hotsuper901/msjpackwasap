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
import com.msj.stickerstudio.WhatsAppHandler
import java.io.File

/**
 * Serves sticker assets to WhatsApp across every route the client actually queries:
 *
 *  - /metadata                            provider capability probe
 *  - /tray/{packId}                       tray icon URI we stored in the pack row
 *  - /sticker_pack_tray_icon/{packId}     legacy tray icon route WhatsApp may normalize to
 *  - /stickers/{packId}                   enumerates sticker file names + emojis + sizes
 *  - /stickers_asset/{packId}/{name}      binary sticker data (WhatsApp's standard path)
 *  - /sticker/{packId}/{name}             binary data via our own stored URIs
 */
class StickerContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.msj.stickerstudio.stickercontentprovider"

        private const val MATCHER_METADATA = 0
        private const val MATCHER_TRAY = 1
        private const val MATCHER_TRAY_LEGACY = 2
        private const val MATCHER_STICKERS = 3
        private const val MATCHER_STICKER_ASSET = 4
        private const val MATCHER_STICKER = 5

        private const val STICKER_FILE_NAME = "sticker_file_name"
        private const val STICKER_FILE_EMOJI = "sticker_emoji"
        private const val STICKER_FILE_SIZE = "sticker_size"

        fun buildTrayUri(packId: String): Uri =
            Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath("tray").appendPath(packId).build()

        fun buildStickerUri(packId: String, fileName: String): Uri =
            Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath("sticker").appendPath(packId).appendPath(fileName).build()
    }

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "metadata", MATCHER_METADATA)
        addURI(AUTHORITY, "tray/*", MATCHER_TRAY)
        addURI(AUTHORITY, "sticker_pack_tray_icon/*", MATCHER_TRAY_LEGACY)
        addURI(AUTHORITY, "stickers/*", MATCHER_STICKERS)
        addURI(AUTHORITY, "stickers_asset/*/*", MATCHER_STICKER_ASSET)
        addURI(AUTHORITY, "sticker/*/*", MATCHER_STICKER)
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = when (uriMatcher.match(uri)) {
        MATCHER_METADATA -> "text/plain"
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
            val cursor = MatrixCursor(arrayOf("metadata"))
            cursor.addRow(arrayOf<Any>("metadata"))
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

    private fun enumerateStickers(uri: Uri): Cursor {
        val ctx = context ?: return MatrixCursor(arrayOf(STICKER_FILE_NAME, STICKER_FILE_EMOJI, STICKER_FILE_SIZE))
        val packId = uri.pathSegments.getOrNull(1)
            ?: return MatrixCursor(arrayOf(STICKER_FILE_NAME, STICKER_FILE_EMOJI, STICKER_FILE_SIZE))
        val dir = WhatsAppHandler.stickerDir(ctx, packId)
        val cursor = MatrixCursor(arrayOf(STICKER_FILE_NAME, STICKER_FILE_EMOJI, STICKER_FILE_SIZE))
        dir.listFiles()?.filter { it.isFile }?.forEach { file ->
            cursor.addRow(
                arrayOf<Any>(
                    file.name,
                    "\uD83D\uDE00",
                    file.length()
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
