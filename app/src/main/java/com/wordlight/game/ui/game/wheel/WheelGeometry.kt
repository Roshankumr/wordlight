package com.wordlight.game.ui.game.wheel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.lerp
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Pure geometry for the letter wheel. No Compose runtime, so it's unit-tested on the JVM.
 */
object WheelGeometry {

    /** Angle (degrees) of a slot; slot 0 sits at 12 o'clock, then clockwise. */
    fun slotAngle(slot: Int, count: Int): Float = -90f + 360f * slot / count

    /** Letter centres for the given angles inside a wheel of [size]. */
    fun letterCenters(size: Size, anglesDeg: List<Float>, ringFraction: Float = 0.64f): List<Offset> {
        if (size.minDimension <= 0f) return emptyList()
        val center = size.center
        val radius = size.minDimension / 2f * ringFraction
        return anglesDeg.map { angle ->
            val rad = Math.toRadians(angle.toDouble())
            Offset(
                x = center.x + radius * cos(rad).toFloat(),
                y = center.y + radius * sin(rad).toFloat(),
            )
        }
    }

    /** Index of the closest letter whose centre is within [hitRadius] of [point], or null. */
    fun hitTest(centers: List<Offset>, point: Offset, hitRadius: Float): Int? {
        var best = -1
        var bestDistance = Float.MAX_VALUE
        centers.forEachIndexed { index, c ->
            val d = (c - point).getDistance()
            if (d <= hitRadius && d < bestDistance) {
                best = index
                bestDistance = d
            }
        }
        return best.takeIf { it >= 0 }
    }

    /**
     * Calls [onSample] at points every <= [step] px along the segment (from, to], ending exactly at [to].
     * Move events can be 50+ px apart on a fast flick; sampling the segment means we never
     * jump over a letter the finger actually passed through.
     */
    inline fun sampleSegment(from: Offset, to: Offset, step: Float, onSample: (Offset) -> Unit) {
        val distance = (to - from).getDistance()
        val samples = max(1, ceil(distance / step).toInt())
        for (k in 1..samples) {
            onSample(lerp(from, to, k.toFloat() / samples))
        }
    }
}
