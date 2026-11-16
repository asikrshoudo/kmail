package com.kmail.thekami.ui.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

enum class SwipeDirection { NEXT, PREVIOUS }

object SwipeDecision {
    /**
     * A swipe counts only if it is long enough and clearly more horizontal than vertical, so
     * normal scrolling never changes the message. Left means next, right means previous.
     */
    fun decide(dx: Float, dy: Float, threshold: Float): SwipeDirection? = when {
        abs(dx) < threshold || abs(dx) < abs(dy) * 2f -> null
        dx < 0 -> SwipeDirection.NEXT
        else -> SwipeDirection.PREVIOUS
    }
}

/** Watches touches before the children do and never consumes them, so web content still scrolls and taps. */
fun Modifier.horizontalSwipeNavigation(
    thresholdPx: Float,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier = pointerInput(thresholdPx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var dx = 0f
        var dy = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.size > 1) return@awaitEachGesture
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            dx += change.position.x - change.previousPosition.x
            dy += change.position.y - change.previousPosition.y
        }
        when (SwipeDecision.decide(dx, dy, thresholdPx)) {
            SwipeDirection.NEXT -> onNext()
            SwipeDirection.PREVIOUS -> onPrevious()
            null -> Unit
        }
    }
}
