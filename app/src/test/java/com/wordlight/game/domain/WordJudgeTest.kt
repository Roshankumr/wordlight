package com.wordlight.game.domain

import com.wordlight.game.domain.engine.GuessResult
import com.wordlight.game.domain.engine.WordJudge
import com.wordlight.game.domain.model.Cell
import com.wordlight.game.domain.model.Direction
import com.wordlight.game.domain.model.Level
import com.wordlight.game.domain.model.WordPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordJudgeTest {

    private val judge = WordJudge()
    private val level = Level(
        id = 4,
        pack = "Dawn",
        letters = "TEAR".toList(),
        rows = 4,
        cols = 6,
        placements = listOf(
            WordPlacement("TEAR", Cell(0, 0), Direction.ACROSS),
            WordPlacement("EAT", Cell(0, 1), Direction.DOWN),
        ),
        bonusWords = listOf("TEA", "ART"),
    )

    @Test
    fun `grid word is recognised`() {
        val result = judge.judge("tear", level, emptyList(), emptyList())
        assertTrue(result is GuessResult.GridWord)
        assertEquals("TEAR", (result as GuessResult.GridWord).placement.word)
    }

    @Test
    fun `bonus word is recognised`() {
        assertEquals(GuessResult.BonusWord("ART"), judge.judge("ART", level, emptyList(), emptyList()))
    }

    @Test
    fun `repeat grid word is already found`() {
        assertEquals(GuessResult.AlreadyFound("EAT"), judge.judge("EAT", level, listOf("EAT"), emptyList()))
    }

    @Test
    fun `repeat bonus word is already found`() {
        assertEquals(GuessResult.AlreadyFound("TEA"), judge.judge("TEA", level, emptyList(), listOf("TEA")))
    }

    @Test
    fun `unknown word is invalid`() {
        assertEquals(GuessResult.Invalid("TRE"), judge.judge("TRE", level, emptyList(), emptyList()))
    }
}
