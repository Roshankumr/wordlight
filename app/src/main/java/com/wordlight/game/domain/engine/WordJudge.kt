package com.wordlight.game.domain.engine

import com.wordlight.game.domain.model.Level
import com.wordlight.game.domain.model.WordPlacement
import javax.inject.Inject

sealed interface GuessResult {
    data class GridWord(val placement: WordPlacement) : GuessResult
    data class BonusWord(val word: String) : GuessResult
    data class AlreadyFound(val word: String) : GuessResult
    data class Invalid(val word: String) : GuessResult
}

/** Pure rules: decides what a submitted word means for the current level. */
class WordJudge @Inject constructor() {

    fun judge(
        guess: String,
        level: Level,
        foundWords: Collection<String>,
        foundBonusWords: Collection<String>,
    ): GuessResult {
        val word = guess.uppercase()
        return when {
            word in foundWords || word in foundBonusWords -> GuessResult.AlreadyFound(word)
            else -> level.placementOf(word)?.let { GuessResult.GridWord(it) }
                ?: if (word in level.bonusWords) GuessResult.BonusWord(word) else GuessResult.Invalid(word)
        }
    }
}
