package org.lexem.angmar.analyzer.stdlib.globals

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.lexem.angmar.Incorrect
import org.lexem.angmar.analyzer.AnalyzerCommons
import org.lexem.angmar.analyzer.data.primitives.LxmNil
import org.lexem.angmar.errors.AngmarAnalyzerExceptionType
import org.lexem.angmar.io.readers.IOStringReader
import org.lexem.angmar.parser.functional.expressions.AssignOperatorNode
import org.lexem.angmar.parser.functional.expressions.modifiers.AccessExplicitMemberNode
import org.lexem.angmar.parser.functional.expressions.modifiers.FunctionCallNode
import org.lexem.angmar.parser.literals.StringNode
import org.lexem.angmar.utils.TestUtils

internal class DebugGlobalObjectTest {
    @Test
    fun `test pause`() {
        val varName = "test"
        val varStmt = "$varName ${AssignOperatorNode.assignOperator} 5"
        val fnCall = "${DebugGlobalObject.ObjectName}${AccessExplicitMemberNode.accessToken}${DebugGlobalObject.Pause}"
        val grammar = "$fnCall${FunctionCallNode.startToken}${FunctionCallNode.endToken} \n $varStmt"
        val analyzer = TestUtils.createAnalyzerFromWholeGrammar(grammar)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory, toWrite = true)
        context.setProperty(analyzer.memory, varName, LxmNil)

        val hasFinished = analyzer.start(IOStringReader.from(""), timeoutInMilliseconds = Long.MAX_VALUE)
        Assertions.assertFalse(hasFinished, "The analyzer has finished")

        context = AnalyzerCommons.getCurrentContext(analyzer.memory, toWrite = false)
        val result = context.getPropertyValue(analyzer.memory, varName)
        Assertions.assertEquals(LxmNil, result, "The result is incorrect")
    }

    @Test
    fun `test log`() {
        val value = "this is a test"
        val (stdOut, errOut) = TestUtils.handleLogs {
            val fnCall =
                "${DebugGlobalObject.ObjectName}${AccessExplicitMemberNode.accessToken}${DebugGlobalObject.Log}"
            val fnArgs = "${StringNode.startToken}$value${StringNode.endToken}"
            val grammar = "$fnCall${FunctionCallNode.startToken}$fnArgs${FunctionCallNode.endToken}"
            val analyzer = TestUtils.createAnalyzerFromWholeGrammar(grammar)

            TestUtils.processAndCheckEmpty(analyzer)

            TestUtils.checkEmptyStackAndContext(analyzer)
        }

        Assertions.assertTrue(errOut.isEmpty(), "The error output must be empty")
        Assertions.assertTrue(stdOut.contains(value), "The value is not contained")
    }

    @Test
    fun `test log with tag`() {
        val tag = "tag"
        val value = "this is a test"
        val (stdOut, errOut) = TestUtils.handleLogs {
            val fnCall =
                "${DebugGlobalObject.ObjectName}${AccessExplicitMemberNode.accessToken}${DebugGlobalObject.Log}"
            val fnArgs =
                "${StringNode.startToken}$value${StringNode.endToken}${FunctionCallNode.argumentSeparator}${StringNode.startToken}$tag${StringNode.endToken}"
            val grammar = "$fnCall${FunctionCallNode.startToken}$fnArgs${FunctionCallNode.endToken}"
            val analyzer = TestUtils.createAnalyzerFromWholeGrammar(grammar)

            TestUtils.processAndCheckEmpty(analyzer)

            TestUtils.checkEmptyStackAndContext(analyzer)
        }

        Assertions.assertTrue(errOut.isEmpty(), "The error output must be empty")
        Assertions.assertTrue(stdOut.contains(value), "The value is not contained")
        Assertions.assertTrue(stdOut.contains(tag), "The tag is not contained")
    }

    @Test
    @Incorrect
    fun `test throw`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.CustomError) {
            val value = "this is a test"
            val fnCall =
                "${DebugGlobalObject.ObjectName}${AccessExplicitMemberNode.accessToken}${DebugGlobalObject.Throw}"
            val fnArgs = "${StringNode.startToken}$value${StringNode.endToken}"
            val grammar = "$fnCall${FunctionCallNode.startToken}$fnArgs${FunctionCallNode.endToken}"
            val analyzer = TestUtils.createAnalyzerFromWholeGrammar(grammar)

            TestUtils.processAndCheckEmpty(analyzer)

            TestUtils.checkEmptyStackAndContext(analyzer)
        }
    }

    @Test
    fun `test throw with tag`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.CustomError) {
            val value = "this is a test"
            val tag = "tag"
            val fnCall =
                "${DebugGlobalObject.ObjectName}${AccessExplicitMemberNode.accessToken}${DebugGlobalObject.Throw}"
            val fnArgs =
                "${StringNode.startToken}$value${StringNode.endToken}${FunctionCallNode.argumentSeparator}${StringNode.startToken}$tag${StringNode.endToken}"
            val grammar = "$fnCall${FunctionCallNode.startToken}$fnArgs${FunctionCallNode.endToken}"
            val analyzer = TestUtils.createAnalyzerFromWholeGrammar(grammar)

            TestUtils.processAndCheckEmpty(analyzer)

            TestUtils.checkEmptyStackAndContext(analyzer)
        }
    }

    @Test
    @Incorrect
    fun `test log with incorrect tag type`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.BadArgumentError) {
            val tag = 5
            val value = "this is a test"
            val fnCall =
                "${DebugGlobalObject.ObjectName}${AccessExplicitMemberNode.accessToken}${DebugGlobalObject.Log}"
            val fnArgs = "${StringNode.startToken}$value${StringNode.endToken}${FunctionCallNode.argumentSeparator}$tag"
            val grammar = "$fnCall${FunctionCallNode.startToken}$fnArgs${FunctionCallNode.endToken}"
            val analyzer = TestUtils.createAnalyzerFromWholeGrammar(grammar)

            TestUtils.processAndCheckEmpty(analyzer)

            TestUtils.checkEmptyStackAndContext(analyzer)
        }
    }
}
