package com.parknav.data

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class PositionTest {

    @Test
    fun `distanceTo computes 3D distance`() {
        val a = Position(0f, 0f, 0f)
        val b = Position(3f, 4f, 0f)
        assertEquals(5f, a.distanceTo(b), 0.001f)
    }

    @Test
    fun `distanceTo with all axes`() {
        val a = Position(1f, 2f, 3f)
        val b = Position(4f, 6f, 3f)
        // dx=3, dy=4, dz=0 → 5
        assertEquals(5f, a.distanceTo(b), 0.001f)
    }

    @Test
    fun `distanceTo2D ignores Y axis`() {
        val a = Position(0f, 0f, 0f)
        val b = Position(3f, 100f, 4f)
        // 2D uses X and Z only: sqrt(9+16)=5
        assertEquals(5f, a.distanceTo2D(b), 0.001f)
    }

    @Test
    fun `distance to same point is zero`() {
        val p = Position(5f, 3f, 7f)
        assertEquals(0f, p.distanceTo(p), 0.001f)
        assertEquals(0f, p.distanceTo2D(p), 0.001f)
    }
}
