package com.msj.stickerstudio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local registry of every pack this app registered with WhatsApp. The content
 * provider reads from here, so packs keep serving metadata + sticker assets
 * even after the app process is killed (WhatsApp can query at any time).
 */
object PackStore {

    data class StickerRecord(
        val fileName: String,
        val emojis: String,
        val title: String
    )

    data class PackRecord(
        val id: String,
        val name: String,
        val publisher: String,
        val animated: Boolean,
        val stickers: List<StickerRecord>
    )

    private fun file(context: Context): File =
        File(WhatsAppHandler.workDir(context), "packs.json")

    @Synchronized
    fun load(context: Context): List<PackRecord> {
        return try {
            val f = file(context)
            if (!f.exists()) return emptyList()
            val root = JSONObject(f.readText())
            val arr = root.optJSONArray("packs") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val stickersArr = o.optJSONArray("stickers") ?: JSONArray()
                val stickers = (0 until stickersArr.length()).mapNotNull { j ->
                    val so = stickersArr.optJSONObject(j) ?: return@mapNotNull null
                    StickerRecord(
                        fileName = so.optString("file"),
                        emojis = so.optString("emojis"),
                        title = so.optString("title")
                    )
                }
                PackRecord(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    publisher = o.optString("publisher"),
                    animated = o.optBoolean("animated"),
                    stickers = stickers
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun add(context: Context, record: PackRecord) {
        val existing = load(context).filter { it.id != record.id }
        save(context, existing + record)
    }

    @Synchronized
    fun find(context: Context, id: String): PackRecord? =
        load(context).firstOrNull { it.id == id }

    @Synchronized
    private fun save(context: Context, records: List<PackRecord>) {
        val arr = JSONArray()
        records.forEach { r ->
            val o = JSONObject()
            o.put("id", r.id)
            o.put("name", r.name)
            o.put("publisher", r.publisher)
            o.put("animated", r.animated)
            val stickersArr = JSONArray()
            r.stickers.forEach { s ->
                val so = JSONObject()
                so.put("file", s.fileName)
                so.put("emojis", s.emojis)
                so.put("title", s.title)
                stickersArr.put(so)
            }
            o.put("stickers", stickersArr)
            arr.put(o)
        }
        val root = JSONObject().put("packs", arr)

        val f = file(context)
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, "packs.json.tmp")
        tmp.writeText(root.toString())
        if (f.exists()) f.delete()
        tmp.renameTo(f)
    }
}
