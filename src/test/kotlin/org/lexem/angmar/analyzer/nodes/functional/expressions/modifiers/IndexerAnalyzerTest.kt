package org.lexem.angmar.analyzer.nodes.functional.expressions.modifiers

import org.junit.jupiter.api.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.primitives.setters.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.parser.functional.expressions.*
import org.lexem.angmar.parser.functional.expressions.modifiers.*
import org.lexem.angmar.utils.*

internal class IndexerAnalyzerTest {
    @Test
    fun `test getter`() {
        val varName = "test"
        val text = "$varName${IndexerNode.startToken}1${IndexerNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(text, parserFunction = AccessExpressionNode.Companion::parse)

        // Prepare context.
        val value = LxmInteger.from(5)
        val list = LxmList(analyzer.memory)
        list.addCell(analyzer.memory, LxmInteger.from(2))
        list.addCell(analyzer.memory, value)

        val context = AnalyzerCommons.getCurrentContext(analyzer.memory, toWrite = true)
        context.setProperty(analyzer.memory, varName, list)

        TestUtils.processAndCheckEmpty(analyzer)

        val result = analyzer.memory.getLastFromStack() as? LxmIndexerSetter ?: throw Error(
                "The result must be a LxmIndexerSetter")
        Assertions.assertEquals(list.getPrimitive(), result.element, "The element property is incorrect")
        Assertions.assertEquals(LxmInteger.from(1), result.index, "The index property is incorrect")
        Assertions.assertEquals(value, result.dereference(analyzer.memory, toWrite = false),
                "The value property is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(varName))
    }

    @Test
    fun `test setter`() {
        val varName = "test"
        val cellIndex = 1
        val right = 77
        val text =
                "$varName${IndexerNode.startToken}$cellIndex${IndexerNode.endToken} ${AssignOperatorNode.assignOperator} $right"
        val analyzer = TestUtils.createAnalyzerFrom(text, parserFunction = AssignExpressionNode.Companion::parse)

        // Prepare context.
        val value = LxmInteger.from(5)
        val list = LxmList(analyzer.memory)

        for (i in 0 until cellIndex) {
            list.addCell(analyzer.memory, LxmNil)
        }
        list.addCell(analyzer.memory, value)

        val context = AnalyzerCommons.getCurrentContext(analyzer.memory, toWrite = true)
        context.setProperty(analyzer.memory, varName, list)

        TestUtils.processAndCheckEmpty(analyzer)

        val result = analyzer.memory.getLastFromStack() as? LxmInteger ?: throw Error("The result must be a LxmInteger")
        Assertions.assertEquals(right, result.primitive, "The element property is incorrect")

        val finalList = list.getPrimitive().dereference(analyzer.memory, toWrite = false) as? LxmList ?: throw Error(
                "The result must be a LxmList")
        val cell =
                finalList.getDereferencedCell<LxmInteger>(analyzer.memory, cellIndex, toWrite = false) ?: throw Error(
                        "The result must be a LxmInteger")
        Assertions.assertEquals(right, cell.primitive, "The primitive property is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(varName))
    }
}
