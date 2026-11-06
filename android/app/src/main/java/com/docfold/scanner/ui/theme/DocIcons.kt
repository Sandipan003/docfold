package com.docfold.scanner.ui.theme

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Docfold's icon set: one 1.8-unit stroke on a 24-unit grid, round caps — drawn to match the engraved
 * illustrations rather than borrowed from a library. Icons are tinted by `Icon`, so the stroke colour here is irrelevant.
 */
object DocIcons {
    private fun icon(name: String, vararg paths: String, stroke: Float = 1.8f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Scan by lazy { icon("scan", "M4 8V5.5A1.5 1.5 0 0 1 5.5 4H8M16 4h2.5A1.5 1.5 0 0 1 20 5.5V8M20 16v2.5a1.5 1.5 0 0 1-1.5 1.5H16M8 20H5.5A1.5 1.5 0 0 1 4 18.5V16M3.5 12h17") }
    val Add by lazy { icon("add", "M12 5v14M5 12h14") }
    val Folder by lazy { icon("folder", "M3.5 7.5A1.5 1.5 0 0 1 5 6h4.2l2 2.2H19a1.5 1.5 0 0 1 1.5 1.5v7.8A1.5 1.5 0 0 1 19 19H5a1.5 1.5 0 0 1-1.5-1.5Z") }
    val Print by lazy { icon("print", "M7 8V4h10v4M7 17H5.5A1.5 1.5 0 0 1 4 15.5v-5A2.5 2.5 0 0 1 6.5 8h11a2.5 2.5 0 0 1 2.5 2.5v5a1.5 1.5 0 0 1-1.5 1.5H17M7 14h10v6H7Z") }
    val Back by lazy { icon("back", "M19 12H5M11 6l-6 6 6 6") }
    val Close by lazy { icon("close", "M6 6l12 12M18 6L6 18") }
    val Check by lazy { icon("check", "M5 12.5l4.5 4.5L19 7") }
    val More by lazy { icon("more", "M12 5.5v.01M12 12v.01M12 18.5v.01", stroke = 2.8f) }
    val Search by lazy { icon("search", "M10.5 17a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13zM15.5 15.5L20 20") }
    val RotateRight by lazy { icon("rotate_right", "M19.5 12A7.5 7.5 0 1 1 17.3 6.7M19.5 3.5v4h-4") }
    val RotateLeft by lazy { icon("rotate_left", "M4.5 12A7.5 7.5 0 1 0 6.7 6.7M4.5 3.5v4h4") }
    val Crop by lazy { icon("crop", "M7 3v14h14M3 7h14v14") }
    val Filter by lazy { icon("filter", "M12 20a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM12 4v16M12 8h4.5M12 12h6M12 16h4.5") }
    val Sign by lazy { icon("sign", "M13.5 3.5l4 4-8 8h-4v-4zM11 6l4 4M4 20h16") }
    val Text by lazy { icon("text", "M5 7V4.5h14V7M12 4.5v15M9 19.5h6") }
    val Delete by lazy { icon("delete", "M4 7h16M9.5 7V4.5h5V7M6 7l1 13h10l1-13M10 11v5.5M14 11v5.5") }
    val Share by lazy { icon("share", "M12 15V3.5M7.5 8L12 3.5 16.5 8M5 12.5V19a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-6.5") }
    val Download by lazy { icon("download", "M12 3.5V15M7.5 10.5L12 15l4.5-4.5M5 15.5V19a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-3.5") }
    val Photo by lazy { icon("photo", "M4 5h16v14H4zM4 16l4.5-4.5L12 15l2.5-2.5L20 18M15.5 9v.01") }
    val Pdf by lazy { icon("pdf", "M6 3h8l4 4v14H6zM14 3v4h4M9 12.5h6M9 16.5h6") }
    val Copy by lazy { icon("copy", "M9 9h11v11H9zM5.5 15H4V4h11v1.5") }
    val Edit by lazy { icon("edit", "M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4") }
    val Duplicate by lazy { icon("duplicate", "M8 8h12v12H8zM4 16V4h12M14 11v6M11 14h6") }
    val MoveOut by lazy { icon("move_out", "M4 6h6.5l2 2H20v11H4zM9.5 13.5h6M13 11l2.5 2.5L13 16") }
    val Merge by lazy { icon("merge", "M7 4v4c0 2.5 5 3 5 6v6M17 4v4c0 2.5-5 3-5 6M9 17.5l3 3 3-3") }
    val Settings by lazy { icon("settings", "M4 7h9M17 7h3M4 17h3M11 17h9M15 4.5v5M9 14.5v5") }
    val Lock by lazy { icon("lock", "M6 11h12v9.5H6zM8.5 11V8a3.5 3.5 0 0 1 7 0v3") }
    val Shield by lazy { icon("shield", "M12 3l7 3v5c0 5-3.5 8.5-7 10-3.5-1.5-7-5-7-10V6zM9 12l2 2 4-4") }
    val Star by lazy { icon("star", "M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z") }
    val Select by lazy { icon("select", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM8 12l3 3 5-5.5") }
    val Images by lazy { icon("images", "M7.5 7.5h12.5v12.5H7.5zM4 16.5V4h12.5M7.5 17l3.5-3.5 3 3 2-2 4 4") }
    val Camera by lazy { icon("camera", "M4 8h3l2-3h6l2 3h3v12H4zM12 17a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z") }
    val Info by lazy { icon("info", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 11v6M12 7.5v.01") }
    val Contrast by lazy { icon("contrast", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 3v18M12 7h5M12 11h7M12 15h6.5") }
    val Reset by lazy { icon("reset", "M4 9h11a5 5 0 0 1 0 10H8M8 5L4 9l4 4") }
    val Expand by lazy { icon("expand", "M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5") }
    val ChevronDown by lazy { icon("chevron_down", "M6 9l6 6 6-6") }
    val ChevronRight by lazy { icon("chevron_right", "M9 6l6 6-6 6") }
    val Heart by lazy { icon("heart", "M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.4 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z") }
    val Open by lazy { icon("open", "M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5") }
    val Drag by lazy { icon("drag", "M9 6v.01M15 6v.01M9 12v.01M15 12v.01M9 18v.01M15 18v.01", stroke = 2.6f) }
}
