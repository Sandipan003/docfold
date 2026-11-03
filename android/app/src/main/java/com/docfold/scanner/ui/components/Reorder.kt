package com.docfold.scanner.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Long-press-and-drag reordering for a LazyVerticalGrid. Only items whose key passes [canMove] can be picked up
 * or swapped with. The grid must not use contentPadding at the top or sides (pad the grid itself instead) so
 * pointer and item coordinates line up.
 */
class GridReorder internal constructor(
    val grid: LazyGridState,
    private val scope: CoroutineScope,
    private val canMove: (Any) -> Boolean,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onEnd: () -> Unit,
    private val onStart: () -> Unit,
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    private var scrollJob: Job? = null
    private var pointer = Offset.Zero

    private fun LazyGridItemInfo.contains(p: Offset) =
        p.x >= offset.x && p.x <= offset.x + size.width && p.y >= offset.y && p.y <= offset.y + size.height

    internal fun start(p: Offset) {
        val item = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.contains(p) && canMove(it.key) } ?: return
        draggingKey = item.key
        offset = Offset.Zero
        pointer = p
        onStart()
    }

    internal fun drag(delta: Offset) {
        val key = draggingKey ?: return
        offset += delta
        pointer += delta
        swapIfNeeded(key)
        val h = grid.layoutInfo.viewportSize.height
        val edge = h * 0.12f
        val speed = when {
            pointer.y < edge -> -(edge - pointer.y) / edge * 28f
            pointer.y > h - edge -> (pointer.y - (h - edge)) / edge * 28f
            else -> 0f
        }
        scrollJob?.cancel()
        if (speed != 0f) scrollJob = scope.launch {
            while (isActive && draggingKey != null) {
                val used = grid.scrollBy(speed)
                offset += Offset(0f, used)
                swapIfNeeded(key)
                if (used == 0f) break
                kotlinx.coroutines.delay(16)
            }
        }
    }

    private fun swapIfNeeded(key: Any) {
        val items = grid.layoutInfo.visibleItemsInfo
        val cur = items.firstOrNull { it.key == key } ?: return
        val center = Offset(cur.offset.x + offset.x + cur.size.width / 2f, cur.offset.y + offset.y + cur.size.height / 2f)
        val target = items.firstOrNull { it.key != key && canMove(it.key) && it.contains(center) } ?: return
        onMove(cur.index, target.index)
        offset += Offset((cur.offset.x - target.offset.x).toFloat(), (cur.offset.y - target.offset.y).toFloat())
    }

    internal fun end() {
        scrollJob?.cancel()
        if (draggingKey != null) onEnd()
        draggingKey = null
        offset = Offset.Zero
    }
}

@Composable
fun rememberGridReorder(
    grid: LazyGridState,
    canMove: (Any) -> Boolean,
    onMove: (Int, Int) -> Unit,
    onEnd: () -> Unit,
): GridReorder {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    val can by rememberUpdatedState(canMove)
    return remember(grid) {
        GridReorder(grid, scope, { can(it) }, { a, b -> move(a, b) }, { end() }, { haptics.performHapticFeedback(HapticFeedbackType.LongPress) })
    }
}

fun Modifier.reorderable(state: GridReorder, enabled: Boolean): Modifier =
    if (!enabled) this else pointerInput(state) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.start(it) },
            onDrag = { change, amount -> if (state.draggingKey != null) { change.consume(); state.drag(amount) } },
            onDragEnd = { state.end() },
            onDragCancel = { state.end() },
        )
    }
