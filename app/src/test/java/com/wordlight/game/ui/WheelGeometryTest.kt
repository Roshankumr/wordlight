package com.wordlight.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.wordlight.game.ui.game.wheel.WheelGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WheelGeometryTest {

    @Test
    fun `slot 0 is at the top of the wheel`() {
        val centers = WheelGeometry.letterCenters(Size(200f, 200f), listOf(WheelGeometry.slotAngle(0, 4)), 0.5f)
        assertEquals(100f, centers[0].x, 0.01f)
        assertEquals(50f, centers[0].y, 0.01f)
    }

    @Test
    fun `hit test picks the nearest letter inside the radius`() {
        val centers = listOf(Offset(0f, 0f), Offset(100f, 0f))
        assertEquals(1, WheelGeometry.hitTest(centers, Offset(90f, 5f), 34f))
        assertNull(WheelGeometry.hitTest(centers, Offset(50f, 0f), 34f))
    }

    @Test
    fun `fast swipe across a letter is still detected thanks to segment sampling`() {
        // Letter sits in the middle of a 300px jump between two move events.
        val centers = listOf(Offset(150f, 0f))
        val hits = mutableListOf<Int>()
        WheelGeometry.sampleSegment(Offset(0f, 0f), Offset(300f, 0f), step = 8f) { p ->
            WheelGeometry.hitTest(centers, p, 34f)?.let(hits::add)
        }
        assertEquals(0, hits.first())
    }
}
