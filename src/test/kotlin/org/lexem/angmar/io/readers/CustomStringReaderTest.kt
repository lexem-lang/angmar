package org.lexem.angmar.io.readers

import org.junit.jupiter.api.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.utils.*

internal class CustomStringReaderTest {
    @Test
    fun getSource() {
        var reader = CustomStringReader.from("xx")
        Assertions.assertEquals("", reader.getSource())

        TestUtils.handleTempFiles(mapOf("main" to "")) { files ->
            val mainFile = files["main"]!!
            reader = CustomStringReader.from(mainFile)
            Assertions.assertEquals(mainFile.canonicalPath, reader.getSource())
        }
    }

    @Test
    fun currentPosition() {
        val text = "This is a long test".repeat(10)
        val reader = CustomStringReader.from(text)

        var index = 0
        for (char in text) {
            Assertions.assertEquals(index, reader.currentPosition())
            reader.advance()
            index += 1
        }
        Assertions.assertEquals(index, reader.currentPosition())
    }

    @Test
    fun nextCharAt() {
        val text = "abc"
        val reader = CustomStringReader.from(text)

        Assertions.assertThrows(AngmarIOException::class.java) {
            reader.nextCharAt(-1)
        }
        Assertions.assertEquals(text[0], reader.nextCharAt(0))
        Assertions.assertEquals(text[1], reader.nextCharAt())
        Assertions.assertEquals(text[1], reader.nextCharAt(1))
        Assertions.assertEquals(text[2], reader.nextCharAt(2))
        Assertions.assertNull(reader.nextCharAt(3))
    }

    @Test
    fun prevCharAt() {
        val text = "abc"
        val reader = CustomStringReader.from(text)
        reader.advance(2)

        Assertions.assertThrows(AngmarIOException::class.java) {
            reader.prevCharAt(-1)
        }
        Assertions.assertEquals(text[2], reader.prevCharAt(0))
        Assertions.assertEquals(text[1], reader.prevCharAt())
        Assertions.assertEquals(text[1], reader.prevCharAt(1))
        Assertions.assertEquals(text[0], reader.prevCharAt(2))
        Assertions.assertNull(reader.prevCharAt(3))
    }

    @Test
    fun charAt() {
        val text = "abc"
        val reader = CustomStringReader.from(text)

        Assertions.assertThrows(AngmarIOException::class.java) {
            reader.charAt(-1)
        }

        Assertions.assertEquals(text[0], reader.charAt(0))
        Assertions.assertEquals(text[1], reader.charAt(1))
        Assertions.assertEquals(text[2], reader.charAt(2))
        Assertions.assertNull(reader.charAt(3))

        reader.advance(3)

        Assertions.assertThrows(AngmarIOException::class.java) {
            reader.charAt(-1)
        }

        Assertions.assertEquals(text[0], reader.charAt(0))
        Assertions.assertEquals(text[1], reader.charAt(1))
        Assertions.assertEquals(text[2], reader.charAt(2))
        Assertions.assertNull(reader.charAt(3))
    }

    @Test
    fun isEnd() {
        var text = ""
        var reader = CustomStringReader.from(text)
        Assertions.assertTrue(reader.isEnd())

        text = "a"
        reader = CustomStringReader.from(text)
        Assertions.assertFalse(reader.isEnd())
        reader.advance()
        Assertions.assertTrue(reader.isEnd())

        text = "ab"
        reader = CustomStringReader.from(text)
        Assertions.assertFalse(reader.isEnd())
        reader.advance()
        Assertions.assertFalse(reader.isEnd())
        reader.advance()
        Assertions.assertTrue(reader.isEnd())
    }

    @Test
    fun advance() {
        val text = "abc"
        val reader = CustomStringReader.from(text)

        Assertions.assertEquals(0, reader.currentPosition())
        Assertions.assertTrue(reader.advance())
        Assertions.assertEquals(1, reader.currentPosition())
        Assertions.assertTrue(reader.advance())
        Assertions.assertEquals(2, reader.currentPosition())
        Assertions.assertTrue(reader.advance())
        Assertions.assertEquals(3, reader.currentPosition())
        Assertions.assertFalse(reader.advance())

        reader.restart()
        Assertions.assertEquals(0, reader.currentPosition())
        Assertions.assertTrue(reader.advance(2))
        Assertions.assertEquals(2, reader.currentPosition())
    }

    @Test
    fun back() {
        val text = "abc"
        val reader = CustomStringReader.from(text)
        reader.advance(3)

        Assertions.assertEquals(3, reader.currentPosition())
        Assertions.assertTrue(reader.back())
        Assertions.assertEquals(2, reader.currentPosition())
        Assertions.assertTrue(reader.back())
        Assertions.assertEquals(1, reader.currentPosition())
        Assertions.assertTrue(reader.back())
        Assertions.assertEquals(0, reader.currentPosition())
        Assertions.assertFalse(reader.back())

        reader.restart()
        reader.advance(3)
        Assertions.assertEquals(3, reader.currentPosition())
        Assertions.assertTrue(reader.back(2))
        Assertions.assertEquals(1, reader.currentPosition())
    }

    @Test
    fun setPosition() {
        val text = "abc"
        val reader = CustomStringReader.from(text)

        Assertions.assertTrue(reader.setPosition(0))
        Assertions.assertEquals(0, reader.currentPosition())
        Assertions.assertTrue(reader.setPosition(1))
        Assertions.assertEquals(1, reader.currentPosition())
        Assertions.assertTrue(reader.setPosition(2))
        Assertions.assertEquals(2, reader.currentPosition())
        Assertions.assertTrue(reader.setPosition(3))
        Assertions.assertEquals(3, reader.currentPosition())
        Assertions.assertFalse(reader.setPosition(4))

        Assertions.assertTrue(reader.setPosition(3))
        Assertions.assertEquals(3, reader.currentPosition())
        Assertions.assertTrue(reader.setPosition(2))
        Assertions.assertEquals(2, reader.currentPosition())
        Assertions.assertTrue(reader.setPosition(1))
        Assertions.assertEquals(1, reader.currentPosition())
        Assertions.assertTrue(reader.setPosition(0))
        Assertions.assertEquals(0, reader.currentPosition())
    }

    @Test
    fun saveCursor() {
        val text = "This is a long test".repeat(10)
        val reader = CustomStringReader.from(text)

        var index = 0
        for (char in text) {
            val cursor = reader.saveCursor()
            Assertions.assertEquals(index, cursor.position())
            Assertions.assertEquals(char, cursor.char())
            Assertions.assertEquals(reader, cursor.getReader())
            reader.advance()
            index += 1
        }

        val cursor = reader.saveCursor()
        Assertions.assertEquals(index, cursor.position())
        Assertions.assertNull(cursor.char())
        Assertions.assertEquals(reader, cursor.getReader())
    }

    @Test
    fun restoreCursor() {
        val text = "This is a long test"
        val reader = CustomStringReader.from(text)

        val cursor = reader.saveCursor()
        repeat(5) {
            reader.advance()
        }

        Assertions.assertEquals(5, reader.currentPosition())
        Assertions.assertEquals(0, cursor.position())
        Assertions.assertEquals(text[0], cursor.char())
        Assertions.assertEquals(reader, cursor.getReader())

        cursor.restore()

        Assertions.assertEquals(0, reader.currentPosition())
    }

    @Test
    fun restart() {
        val text = "abc"
        val reader = CustomStringReader.from(text)

        Assertions.assertEquals(0, reader.currentPosition())
        reader.restart()
        Assertions.assertEquals(0, reader.currentPosition())
        reader.advance()
        Assertions.assertNotEquals(0, reader.currentPosition())
        reader.restart()
        Assertions.assertEquals(0, reader.currentPosition())
    }

    @Test
    fun readAllText() {
        val text = "test"
        var reader = CustomStringReader.from(text)
        Assertions.assertEquals(text, reader.readAllText())

        TestUtils.handleTempFiles(mapOf("main" to text)) { files ->
            val mainFile = files["main"]!!
            reader = CustomStringReader.from(mainFile)
            Assertions.assertEquals(text, reader.readAllText())
        }
    }

    @Test
    fun substring() {
        val text = "This is a text"
        val word = "This"

        val reader = CustomStringReader.from(text)

        val initCursor = reader.saveCursor()
        reader.advance(word.length)
        val endCursor = reader.saveCursor()
        val substr = reader.substring(initCursor, endCursor)

        Assertions.assertEquals(word.length, reader.currentPosition())
        Assertions.assertEquals(word, substr)
    }
}
