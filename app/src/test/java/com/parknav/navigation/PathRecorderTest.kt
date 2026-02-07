package com.parknav.navigation

import com.parknav.data.Position
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PathRecorderTest {

    private lateinit var recorder: PathRecorder

    @Before
    fun setUp() {
        recorder = PathRecorder()
    }

    @Test
    fun `recordPoint does nothing before startRecording`() {
        recorder.recordPoint(Position(1f, 0f, 1f), 0f)
        assertTrue(recorder.points.isEmpty())
    }

    @Test
    fun `startRecording enables recording`() {
        recorder.startRecording()
        assertTrue(recorder.isRecording)
        recorder.recordPoint(Position(1f, 0f, 2f), 0.5f)
        assertEquals(1, recorder.points.size)
    }

    @Test
    fun `minInterval throttles recording`() {
        recorder.startRecording()
        recorder.recordPoint(Position(1f, 0f, 1f), 0f)
        // Second call within 200ms should be dropped
        recorder.recordPoint(Position(2f, 0f, 2f), 0f)
        // System.currentTimeMillis() is the same within a fast test
        // so second point should be throttled
        assertEquals(1, recorder.points.size)
    }

    @Test
    fun `getReversePath returns reversed list`() {
        recorder.startRecording()
        recorder.recordPoint(Position(1f, 0f, 1f), 0f)
        // Wait to bypass throttle
        Thread.sleep(210)
        recorder.recordPoint(Position(2f, 0f, 2f), 1f)

        val reversed = recorder.getReversePath()
        assertEquals(2, reversed.size)
        assertEquals(2f, reversed[0].position.x, 0.001f)
        assertEquals(1f, reversed[1].position.x, 0.001f)
    }

    @Test
    fun `stopRecording prevents further recording`() {
        recorder.startRecording()
        recorder.recordPoint(Position(1f, 0f, 1f), 0f)
        recorder.stopRecording()
        assertFalse(recorder.isRecording)
        Thread.sleep(210)
        recorder.recordPoint(Position(2f, 0f, 2f), 0f)
        assertEquals(1, recorder.points.size)
    }

    @Test
    fun `clear removes all points and stops recording`() {
        recorder.startRecording()
        recorder.recordPoint(Position(1f, 0f, 1f), 0f)
        recorder.clear()
        assertTrue(recorder.points.isEmpty())
        assertFalse(recorder.isRecording)
    }
}
