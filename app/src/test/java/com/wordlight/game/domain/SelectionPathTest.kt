package com.wordlight.game.domain

import com.wordlight.game.domain.engine.SelectionPath
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionPathTest {

    @Test
    fun `first letter starts the path`() {
        assertEquals(listOf(2), SelectionPath.enter(emptyList(), 2))
    }

    @Test
    fun `new letters are appended`() {
        assertEquals(listOf(0, 1, 2), SelectionPath.enter(listOf(0, 1), 2))
    }

    @Test
    fun `re-entering the last letter is a no-op`() {
        assertEquals(listOf(0, 1), SelectionPath.enter(listOf(0, 1), 1))
    }

    @Test
    fun `re-entering the previous letter backtracks one step`() {
        assertEquals(listOf(0, 1), SelectionPath.enter(listOf(0, 1, 2), 1))
    }

    @Test
    fun `a letter used earlier in the path is ignored`() {
        assertEquals(listOf(0, 1, 2), SelectionPath.enter(listOf(0, 1, 2), 0))
    }
}
