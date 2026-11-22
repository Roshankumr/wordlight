package com.wordlight.game.data

import com.wordlight.game.data.level.LevelParser
import com.wordlight.game.domain.engine.LevelValidator
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the shipped dataset: every level must be valid and playable. */
class LevelDataTest {

    // Unit tests run with the module directory as the working directory.
    private val levels = LevelParser(Json { ignoreUnknownKeys = true })
        .parse(File("src/main/assets/levels.json").readText())

    @Test
    fun `ships 20 levels with sequential ids`() {
        assertEquals((1..20).toList(), levels.map { it.id })
    }

    @Test
    fun `every level passes validation`() {
        levels.forEach { assertEquals(emptyList<String>(), LevelValidator.validate(it)) }
    }

    @Test
    fun `wheel has 3 to 5 letters and packs match the design`() {
        levels.forEach { level ->
            assertTrue(level.letters.size in 3..5)
            if (level.id <= 10) {
                assertEquals("Dawn", level.pack)
                assertTrue(level.letters.size in 3..4)
            } else {
                assertEquals("Dusk", level.pack)
                assertEquals(5, level.letters.size)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `parser rejects conflicting crossings`() {
        val bad = """
            {"version":1,"levels":[{"id":1,"pack":"X","letters":"CAT","rows":3,"cols":3,
              "words":[{"word":"CAT","row":0,"col":0,"dir":"H"},{"word":"TAC","row":0,"col":1,"dir":"V"}]}]}
        """.trimIndent()
        LevelParser(Json).parse(bad)
    }
}
