package org.lexem.angmar.analyzer.nodes.functional.expressions

import org.junit.jupiter.api.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.parser.functional.expressions.*
import org.lexem.angmar.parser.literals.*
import org.lexem.angmar.utils.*

internal class PrefixExpressionAnalyzerTest {
    @Test
    fun test() {
        val text = "${PrefixOperatorNode.notOperator}${LogicNode.trueLiteral}"
        val analyzer = TestUtils.createAnalyzerFrom(text, parserFunction = PrefixExpressionNode.Companion::parse)
        TestUtils.processAndCheckEmpty(analyzer)

        Assertions.assertEquals(LxmLogic.False, analyzer.memory.getLastFromStack(),
                "The value inserted in the stack is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }
}
