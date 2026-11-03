package com.docfold.scanner.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

enum class PageFilter(val key: String, val label: String) {
    Original("original", "Original"),
    Magic("magic", "Magic"),
    Gray("gray", "Grayscale"),
    Ink("ink", "B&W");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: Original
    }
}

/**
 * One page. The rendered image lives at `pages/<id>.jpg`. When the page has been cropped or filtered the
 * untouched capture is kept at `pages/<id>.src.jpg`, so every edit can be redone from the original.
 * Rotation is metadata only: it is applied when drawing and when writing the PDF, so it never costs quality.
 */
@Immutable
data class Page(
    val id: String,
    val rotation: Int = 0,
    val filter: PageFilter = PageFilter.Original,
    /** Crop quad in source-image fractions: x0,y0 (top-left), x1,y1, x2,y2, x3,y3 clockwise. */
    val crop: List<Float>? = null,
    /** Bumped on every pixel edit so image caches know to reload. */
    val rev: Int = 0,
) {
    fun toJson() = JSONObject().apply {
        put("id", id)
        put("rot", rotation)
        put("filter", filter.key)
        put("rev", rev)
        crop?.let { c -> put("crop", JSONArray().apply { c.forEach { put(it.toDouble()) } }) }
    }

    val cacheKey get() = "$id#$rev"

    companion object {
        fun fromJson(o: JSONObject) = Page(
            id = o.getString("id"),
            rotation = o.optInt("rot", 0),
            filter = PageFilter.of(o.optString("filter")),
            crop = o.optJSONArray("crop")?.let { a -> List(a.length()) { a.getDouble(it).toFloat() } },
            rev = o.optInt("rev", 0),
        )
    }
}

@Immutable
data class Doc(
    val id: String,
    val name: String,
    val created: Long,
    val updated: Long,
    /** Index into the stamp palette: the document's own colour everywhere it appears. */
    val color: Int,
    val pages: List<Page>,
    /** When the document was last saved or shared as a PDF; it then wears a postmark. */
    val sentAt: Long = 0,
    /** Bytes on disk, filled in when listing. */
    val sizeBytes: Long = 0,
) {
    fun toJson() = JSONObject().apply {
        put("v", 1)
        put("id", id)
        put("name", name)
        put("created", created)
        put("updated", updated)
        put("color", color)
        put("sentAt", sentAt)
        put("pages", JSONArray().apply { pages.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(o: JSONObject) = Doc(
            id = o.getString("id"),
            name = o.optString("name", "Untitled"),
            created = o.optLong("created"),
            updated = o.optLong("updated"),
            color = o.optInt("color", 0),
            sentAt = o.optLong("sentAt", 0),
            pages = o.optJSONArray("pages")?.let { a -> List(a.length()) { Page.fromJson(a.getJSONObject(it)) } } ?: emptyList(),
        )
    }
}
