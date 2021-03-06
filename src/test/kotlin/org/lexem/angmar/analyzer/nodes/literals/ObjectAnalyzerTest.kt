package org.lexem.angmar.analyzer.nodes.literals

import org.junit.jupiter.api.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.parser.literals.*
import org.lexem.angmar.utils.*

internal class ObjectAnalyzerTest {
    val values = mapOf("a" to 1, "b" to 2, "c" to 3, "d" to 4)
    val valuesText = values.asSequence().joinToString(ObjectNode.elementSeparator) {
        "${it.key}${ObjectElementNode.keyValueSeparator}${it.value}"
    }

    // TESTS ------------------------------------------------------------------

    @Test
    fun `test normal`() {
        val text = "${ObjectNode.startToken}$valuesText${ObjectNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(text, parserFunction = ObjectNode.Companion::parse)

        TestUtils.processAndCheckEmpty(analyzer)

        val resultRef =
                analyzer.memory.getLastFromStack() as? LxmReference ?: throw Error("The result must be a LxmReference")
        val obj = resultRef.dereferenceAs<LxmObject>(analyzer.memory) ?: throw Error("The result must be a LxmObject")
        val properties = obj.getAllIterableProperties()

        Assertions.assertEquals(values.size, properties.size, "The number of properties is incorrect")

        for (i in values) {
            val value = properties[i.key]?.value as? LxmInteger ?: throw Error("The property must be a LxmInteger")
            Assertions.assertEquals(i.value, value.primitive, "The primitive property is incorrect")
        }

        Assertions.assertFalse(obj.isImmutable, "The isImmutable property is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @Test
    fun `test constant`() {
        val text = "${ObjectNode.constantToken}${ObjectNode.startToken}$valuesText${ObjectNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(text, parserFunction = ObjectNode.Companion::parse)

        TestUtils.processAndCheckEmpty(analyzer)

        val resultRef =
                analyzer.memory.getLastFromStack() as? LxmReference ?: throw Error("The result must be a LxmReference")
        val obj = resultRef.dereferenceAs<LxmObject>(analyzer.memory) ?: throw Error("The result must be a LxmObject")
        val properties = obj.getAllIterableProperties()

        Assertions.assertEquals(values.size, properties.size, "The number of properties is incorrect")

        for (i in values) {
            val value = properties[i.key]?.value as? LxmInteger ?: throw Error("The property must be a LxmInteger")
            Assertions.assertEquals(i.value, value.primitive, "The primitive property is incorrect")
        }

        Assertions.assertTrue(obj.isImmutable, "The isImmutable property is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @Test
    fun `test empty normal`() {
        val text = "${ObjectNode.startToken}${ObjectNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(text, parserFunction = ObjectNode.Companion::parse)

        TestUtils.processAndCheckEmpty(analyzer)

        val resultRef =
                analyzer.memory.getLastFromStack() as? LxmReference ?: throw Error("The result must be a LxmReference")
        val obj = resultRef.dereferenceAs<LxmObject>(analyzer.memory) ?: throw Error("The result must be a LxmObject")
        val properties = obj.getAllIterableProperties()

        Assertions.assertEquals(0, properties.size, "The number of properties is incorrect")
        Assertions.assertFalse(obj.isImmutable, "The isImmutable property is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @Test
    fun `test empty constant`() {
        val text = "${ObjectNode.constantToken}${ObjectNode.startToken}${ObjectNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(text, parserFunction = ObjectNode.Companion::parse)

        TestUtils.processAndCheckEmpty(analyzer)

        val resultRef =
                analyzer.memory.getLastFromStack() as? LxmReference ?: throw Error("The result must be a LxmReference")
        val obj = resultRef.dereferenceAs<LxmObject>(analyzer.memory) ?: throw Error("The result must be a LxmObject")
        val properties = obj.getAllIterableProperties()

        Assertions.assertEquals(0, properties.size, "The number of properties is incorrect")
        Assertions.assertTrue(obj.isImmutable, "The isImmutable property is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }
}
