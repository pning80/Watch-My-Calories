package com.pning80.watchmycalories

import com.pning80.watchmycalories.ai.GeminiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * GeminiParser degradation contract (PORTING_CRITERIA.md T1.5).
 *
 * Both platforms must degrade identically on:
 *  - Missing optional macros (protein/carbs/fat → null, not 0)
 *  - Negative values (clamp to 0, preserve null)
 *  - Malformed JSON (throw)
 *  - Empty / missing required fields (throw or sensible default)
 *  - Markdown-wrapped JSON (strip wrapper, then parse)
 *
 * iOS implements the equivalent via `Codable` decoders + `max(0, ...)` /
 * `Optional.map { max(0, $0) }` in Services.swift. These tests pin the
 * Android side to the same observable behavior.
 */
class GeminiParserEdgeCasesTest {

    // ---- Negative clamping ----

    @Test
    fun `negative calories are clamped to zero, preserving sign-positive fields`() {
        val json = """
            {
              "items": [{
                "name": "Mystery Item",
                "quantity": "1 serving",
                "calories": -50.0,
                "protein": 10.0,
                "carbs": 20.0,
                "fat": 5.0,
                "confidence": 0.5
              }]
            }
        """.trimIndent()
        val result = GeminiParser.parseGeminiResponse(json)
        val item = result.items.single()
        assertEquals(0.0, item.calories, 0.0)
        assertEquals(10.0, item.protein!!, 0.0)
    }

    @Test
    fun `negative macros are clamped to zero (each independently)`() {
        val json = """
            {
              "items": [{
                "name": "Item",
                "quantity": "1",
                "calories": 100.0,
                "protein": -5.0,
                "carbs": -10.0,
                "fat": -1.5,
                "confidence": 0.9
              }]
            }
        """.trimIndent()
        val item = GeminiParser.parseGeminiResponse(json).items.single()
        assertEquals(0.0, item.protein!!, 0.0)
        assertEquals(0.0, item.carbs!!, 0.0)
        assertEquals(0.0, item.fat!!, 0.0)
    }

    // ---- Missing optional macros ----

    @Test
    fun `missing optional macros parse to null not zero (preserves unknown semantics)`() {
        val json = """
            {
              "items": [{
                "name": "Apple",
                "quantity": "1 medium",
                "calories": 95.0,
                "confidence": 0.99
              }]
            }
        """.trimIndent()
        val item = GeminiParser.parseGeminiResponse(json).items.single()
        assertNull("protein should be null when key is absent (matches iOS decodeIfPresent)", item.protein)
        assertNull(item.carbs)
        assertNull(item.fat)
    }

    @Test
    fun `partially-present macros parse correctly (some null, some not)`() {
        val json = """
            {
              "items": [{
                "name": "Item",
                "quantity": "1 serving",
                "calories": 200.0,
                "protein": 12.0,
                "confidence": 0.8
              }]
            }
        """.trimIndent()
        val item = GeminiParser.parseGeminiResponse(json).items.single()
        assertEquals(12.0, item.protein!!, 0.0)
        assertNull(item.carbs)
        assertNull(item.fat)
    }

    // ---- mealName handling ----

    @Test
    fun `mealName is preserved when present`() {
        val json = """
            {
              "mealName": "Breakfast",
              "items": [{ "name": "Toast", "quantity": "1 slice", "calories": 70.0, "confidence": 1.0 }]
            }
        """.trimIndent()
        val result = GeminiParser.parseGeminiResponse(json)
        assertEquals("Breakfast", result.mealName)
    }

    @Test
    fun `mealName is null when absent`() {
        val json = """
            {
              "items": [{ "name": "Toast", "quantity": "1 slice", "calories": 70.0, "confidence": 1.0 }]
            }
        """.trimIndent()
        val result = GeminiParser.parseGeminiResponse(json)
        assertNull(result.mealName)
    }

    // ---- Items array shapes ----

    @Test
    fun `empty items array parses cleanly (not an error)`() {
        val json = """{ "items": [] }"""
        val result = GeminiParser.parseGeminiResponse(json)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `multiple items round-trip with independent clamping`() {
        val json = """
            {
              "items": [
                { "name": "Egg",   "quantity": "1",      "calories": 70.0,  "confidence": 0.95 },
                { "name": "Toast", "quantity": "1 slice", "calories": -10.0, "confidence": 0.9 }
              ]
            }
        """.trimIndent()
        val items = GeminiParser.parseGeminiResponse(json).items
        assertEquals(2, items.size)
        assertEquals(70.0, items[0].calories, 0.0)
        assertEquals(0.0, items[1].calories, 0.0) // clamped
    }

    // ---- Malformed JSON ----

    @Test
    fun `malformed JSON throws`() {
        try {
            GeminiParser.parseGeminiResponse("not valid json at all {{{")
            fail("Expected parse to throw on malformed input")
        } catch (e: Exception) {
            // expected
        }
    }

    @Test
    fun `JSON with wrong root type throws`() {
        // An array at the root, not an object — should fail because EstimationResult expects an object
        try {
            GeminiParser.parseGeminiResponse("[1, 2, 3]")
            fail("Expected parse to throw when root is not an object matching EstimationResult")
        } catch (e: Exception) {
            // expected
        }
    }

    // ---- cleanMarkdownJson edge cases ----

    @Test
    fun `cleanMarkdownJson strips json fence prefix and backticks`() {
        val input = "```json\n{\"items\": []}\n```"
        assertEquals("{\"items\": []}", GeminiParser.cleanMarkdownJson(input))
    }

    @Test
    fun `cleanMarkdownJson strips plain backtick fence without language tag`() {
        val input = "```\n{\"items\": []}\n```"
        assertEquals("{\"items\": []}", GeminiParser.cleanMarkdownJson(input))
    }

    @Test
    fun `cleanMarkdownJson is null-safe and blank-safe`() {
        assertNull(GeminiParser.cleanMarkdownJson(null))
        assertNull(GeminiParser.cleanMarkdownJson(""))
        assertNull(GeminiParser.cleanMarkdownJson("   \n  \t  "))
    }

    @Test
    fun `cleanMarkdownJson preserves plain JSON without modification`() {
        val input = "{\"items\": [{\"name\": \"X\"}]}"
        assertEquals(input, GeminiParser.cleanMarkdownJson(input))
    }

    // ---- Full pipeline: markdown-wrapped + negative values + missing macros ----

    @Test
    fun `pipeline cleanMarkdownJson then parse handles markdown-wrapped negative-macros payload`() {
        val raw = """
            ```json
            {
              "items": [{
                "name": "Burrito",
                "quantity": "1 large",
                "calories": -700.0,
                "confidence": 0.4
              }]
            }
            ```
        """.trimIndent()
        val clean = GeminiParser.cleanMarkdownJson(raw)
        assertNotNull(clean)
        val item = GeminiParser.parseGeminiResponse(clean!!).items.single()
        assertEquals(0.0, item.calories, 0.0)
        assertNull(item.protein)
        assertFalse(item.name.isBlank())
    }
}
