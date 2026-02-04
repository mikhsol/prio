package com.prio.app.feature.tasks.reorder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Drag-and-drop state for managing reorderable lists.
 * 
 * Implements 3.1.6 from ACTION_PLAN.md per 1.1.1 Task List Screen Specification:
 * - Long press to reorder (300ms)
 * - Haptic feedback on drag start
 * - Persist order after drop
 * - Visual elevation increase during drag
 */
class ReorderableState<T>(
    val listState: LazyListState,
    private val items: List<T>,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    private val onDragEnd: () -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set
    
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set
    
    var dragOffset by mutableFloatStateOf(0f)
        private set
    
    var isDragging by mutableStateOf(false)
        private set
    
    fun onDragStart(index: Int, key: Any) {
        draggingItemIndex = index
        draggingItemKey = key
        isDragging = true
        dragOffset = 0f
    }
    
    fun onDrag(offset: Float) {
        dragOffset += offset
        
        val currentIndex = draggingItemIndex ?: return
        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == currentIndex }
        
        if (itemInfo != null) {
            val currentCenter = itemInfo.offset + itemInfo.size / 2 + dragOffset
            
            // Find target position
            val targetIndex = listState.layoutInfo.visibleItemsInfo.find { info ->
                info.index != currentIndex &&
                currentCenter > info.offset && currentCenter < info.offset + info.size
            }?.index
            
            if (targetIndex != null && targetIndex != currentIndex) {
                onMove(currentIndex, targetIndex)
                draggingItemIndex = targetIndex
                // Adjust offset for the swap
                val sizeDiff = (itemInfo.size) * (if (targetIndex > currentIndex) 1 else -1)
                dragOffset -= sizeDiff
            }
        }
    }
    
    fun onDragEnd() {
        draggingItemIndex = null
        draggingItemKey = null
        isDragging = false
        dragOffset = 0f
        onDragEnd()
    }
}

/**
 * Remember a ReorderableState for drag-and-drop functionality.
 */
@Composable
fun <T> rememberReorderableState(
    items: List<T>,
    listState: LazyListState = rememberLazyListState(),
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit = {}
): ReorderableState<T> {
    return remember(items) {
        ReorderableState(
            listState = listState,
            items = items,
            onMove = onMove,
            onDragEnd = onDragEnd
        )
    }
}

/**
 * Modifier extension for making items reorderable.
 * 
 * Per 1.1.1 spec:
 * - Long press (300ms) triggers drag
 * - Elevation increases to 8dp during drag
 * - Haptic feedback on start
 * - Drop saves position immediately
 */
@Composable
fun <T> Modifier.reorderable(
    state: ReorderableState<T>,
    key: Any,
    index: Int,
    enabled: Boolean = true
): Modifier {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val isDragging = state.draggingItemKey == key
    val elevation = remember { Animatable(1f) }
    
    LaunchedEffect(isDragging) {
        elevation.animateTo(
            targetValue = if (isDragging) 8f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        )
    }
    
    return this
        .zIndex(if (isDragging) 1f else 0f)
        .graphicsLayer {
            this.shadowElevation = elevation.value
            if (isDragging) {
                scaleX = 1.02f
                scaleY = 1.02f
            }
        }
        .offset { IntOffset(0, if (isDragging) state.dragOffset.roundToInt() else 0) }
        .then(
            if (enabled) {
                Modifier.pointerInput(key) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { _ ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            state.onDragStart(index, key)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            state.onDrag(dragAmount.y)
                        },
                        onDragEnd = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            state.onDragEnd()
                        },
                        onDragCancel = {
                            state.onDragEnd()
                        }
                    )
                }
            } else {
                Modifier
            }
        )
}

/**
 * Drag handle icon that becomes visible during edit mode.
 * 
 * Per 1.1.1 spec:
 * - Shows ⋮⋮ drag handle on long-press
 * - Indicates reorder capability
 */
@Composable
fun DragHandle(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { 
                contentDescription = "Drag to reorder" 
            }
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * First-time reorder hint per 1.1.1 spec.
 * Shows once to teach users about long-press reorder.
 */
@Composable
fun ReorderHint(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = "💡",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text(
                    text = "Long-press and drag to reorder tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    androidx.compose.material3.Text("Got it")
                }
            }
        }
    }
}

/**
 * Extension function to move items in a list.
 */
fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    val item = removeAt(fromIndex)
    add(toIndex, item)
}
