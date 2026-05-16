package com.pning80.watchmycalories

import com.pning80.watchmycalories.ai.GeminiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiRepositoryTest {

    @Test
    fun `cleanMarkdownJson successfully strips markdown formatting`() {
        val dirtyJson = "```json\n{\"items\": []}\n```"
        val expected = "{\"items\": []}"

        val result = GeminiParser.cleanMarkdownJson(dirtyJson)
        assertEquals(expected, result)
    }

    @Test
    fun `cleanMarkdownJson handles plain JSON without markdown cleanly`() {
        val cleanJson = "{\"items\": []}"
        val result = GeminiParser.cleanMarkdownJson(cleanJson)
        assertEquals(cleanJson, result)
    }

    @Test
    fun `cleanMarkdownJson returns null on blank or null input`() {
        assertNull(GeminiParser.cleanMarkdownJson(null))
        assertNull(GeminiParser.cleanMarkdownJson("   "))
    }

    @Test
    fun `parseGeminiResponse unmarshals valid JSON into strong Entities`() {
        val validJson = """
            {
              "items": [
                {
                  "name": "Pizza",
                  "quantity": "2 slices",
                  "calories": 500.0,
                  "protein": 24.0,
                  "carbs": 60.0,
                  "fat": 18.0,
                  "confidence": 0.92
                }
              ]
            }
        """.trimIndent()

        val parsed = GeminiParser.parseGeminiResponse(validJson)
        assertNotNull(parsed.items)
        assertEquals(1, parsed.items.size)
        
        val firstItem = parsed.items.first()
        assertEquals("Pizza", firstItem.name)
        assertEquals(500.0, firstItem.calories, 0.0)
        assertEquals(0.92, firstItem.confidence, 0.0)
    }
}
