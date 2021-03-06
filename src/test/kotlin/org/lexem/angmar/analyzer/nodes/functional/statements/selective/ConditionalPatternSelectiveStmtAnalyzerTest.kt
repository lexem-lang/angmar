package org.lexem.angmar.analyzer.nodes.functional.statements.selective

import org.junit.jupiter.api.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.parser.functional.statements.selective.*
import org.lexem.angmar.parser.literals.*
import org.lexem.angmar.utils.*

internal class ConditionalPatternSelectiveStmtAnalyzerTest {
    @Test
    fun `test if true`() {
        val text = "${ConditionalPatternSelectiveStmtNode.ifKeyword} ${LogicNode.trueLiteral}"
        val analyzer = TestUtils.createAnalyzerFrom(text,
                parserFunction = ConditionalPatternSelectiveStmtNode.Companion::parse)

        TestUtils.processAndCheckEmpty(analyzer)

        val result = analyzer.memory.getLastFromStack()

        Assertions.assertEquals(LxmLogic.True, result, "The result is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @Test
    fun `test if false`() {
        val text = "${ConditionalPatternSelectiveStmtNode.ifKeyword} ${LogicNode.falseLiteral}"
        val analyzer = TestUtils.createAnalyzerFrom(text,
                parserFunction = ConditionalPatternSelectiveStmtNode.Companion::parse)

        TestUtils.processAndCheckEmpty(analyzer)

        val result = analyzer.memory.getLastFromStack()

        Assertions.assertEquals(LxmLogic.False, result, "The result is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @Test
    fun `test unless true`() {
        val text = "${ConditionalPatternSelectiveStmtNode.unlessKeyword} ${LogicNode.trueLiteral}"
        val analyzer = TestUtils.createAnalyzerFrom(text,
                parserFunction = ConditionalPatternSelectiveStmtNode.Companion::parse)

        TestUtils.processAndCheckEmpty(analyzer)

        val result = analyzer.memory.getLastFromStack()

        Assertions.assertEquals(LxmLogic.False, result, "The result is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @Test
    fun `test unless false`() {
        val text = "${ConditionalPatternSelectiveStmtNode.unlessKeyword} ${LogicNode.falseLiteral}"
        val analyzer = TestUtils.createAnalyzerFrom(text,
                parserFunction = ConditionalPatternSelectiveStmtNode.Companion::parse)

        TestUtils.processAndCheckEmpty(analyzer)

        val result = analyzer.memory.getLastFromStack()

        Assertions.assertEquals(LxmLogic.True, result, "The result is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }
}
