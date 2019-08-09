package org.lexem.angmar.parser.functional.statements

import org.junit.jupiter.api.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import org.lexem.angmar.*
import org.lexem.angmar.io.readers.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*
import org.lexem.angmar.parser.functional.expressions.*
import org.lexem.angmar.parser.functional.statements.selective.*
import org.lexem.angmar.utils.*
import java.util.stream.*
import kotlin.streams.*

internal class SelectiveStmtNodeTest {
    // PARAMETERS -------------------------------------------------------------

    companion object {
        const val testExpression =
                "${SelectiveStmtNode.keyword} ${SelectiveStmtNode.startToken} ${SelectiveCaseStmtNodeTest.testExpression} ${SelectiveStmtNode.endToken}"

        @JvmStatic
        private fun provideCorrectSelectiveStatement(): Stream<Arguments> {
            val sequence = sequence {
                for (hasCondition in 0..1) {
                    val condition = if (hasCondition == 0) {
                        ""
                    } else {
                        ParenthesisExpressionNodeTest.testExpression
                    }

                    for (hasTag in 0..1) {
                        val tag = if (hasTag == 0) {
                            ""
                        } else {
                            "${SelectiveStmtNode.tagPrefix}${IdentifierNodeTest.testExpression}"
                        }

                        for (i in 1..3) {
                            val cases = List(i) { SelectiveCaseStmtNodeTest.testExpression }
                            var casesText = cases.joinToString(" ")

                            var text =
                                    "${SelectiveStmtNode.keyword} $condition ${SelectiveStmtNode.startToken}$tag $casesText ${SelectiveStmtNode.endToken}"

                            yield(Arguments.of(text, i, hasCondition == 1, hasTag == 1))

                            // without whitespaces
                            casesText = cases.joinToString("")

                            text =
                                    "${SelectiveStmtNode.keyword}$condition${SelectiveStmtNode.startToken}$tag $casesText${SelectiveStmtNode.endToken}"

                            yield(Arguments.of(text, i, hasCondition == 1, hasTag == 1))
                        }
                    }
                }
            }

            return sequence.asStream()
        }

        // AUX METHODS --------------------------------------------------------

        fun checkTestExpression(node: ParserNode) {
            Assertions.assertTrue(node is SelectiveStmtNode, "The node is not a SelectiveStmtNode")
            node as SelectiveStmtNode

            Assertions.assertEquals(1, node.cases.size, "The number of cases is incorrect")
            Assertions.assertNull(node.condition, "The condition property must be null")
            Assertions.assertNull(node.tag, "The tag property must be null")
            SelectiveCaseStmtNodeTest.checkTestExpression(node.cases.first())
        }
    }

    // TESTS ------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("provideCorrectSelectiveStatement")
    fun `parse correct selective statement`(text: String, numCases: Int, hasCondition: Boolean, hasTag: Boolean) {
        val parser = LexemParser(CustomStringReader.from(text))
        val res = SelectiveStmtNode.parse(parser)

        Assertions.assertNotNull(res, "The input has not been correctly parsed")
        res as SelectiveStmtNode

        Assertions.assertEquals(numCases, res.cases.size, "The number of cases is incorrect")

        if (hasCondition) {
            Assertions.assertNotNull(res.condition, "The condition property cannot be null")
            ParenthesisExpressionNodeTest.checkTestExpression(res.condition!!)
        } else {
            Assertions.assertNull(res.condition, "The condition property must be null")
        }

        if (hasTag) {
            Assertions.assertNotNull(res.tag, "The tag property cannot be null")
            IdentifierNodeTest.checkTestExpression(res.tag!!)
        } else {
            Assertions.assertNull(res.tag, "The tag property must be null")
        }

        for (case in res.cases) {
            SelectiveCaseStmtNodeTest.checkTestExpression(case)
        }

        Assertions.assertEquals(text.length, parser.reader.currentPosition(), "The parser did not advance the cursor")
    }

    @Test
    @Incorrect
    fun `parse incorrect selective statement without startToken`() {
        assertParserException {
            val text = SelectiveStmtNode.keyword
            val parser = LexemParser(CustomStringReader.from(text))
            SelectiveStmtNode.parse(parser)
        }
    }

    @Test
    @Incorrect
    fun `parse incorrect selective statement without any case`() {
        assertParserException {
            val text = "${SelectiveStmtNode.keyword} ${SelectiveStmtNode.startToken}"
            val parser = LexemParser(CustomStringReader.from(text))
            SelectiveStmtNode.parse(parser)
        }
    }

    @Test
    @Incorrect
    fun `parse incorrect selective statement without endToken`() {
        assertParserException {
            val text =
                    "${SelectiveStmtNode.keyword} ${SelectiveStmtNode.startToken} ${SelectiveCaseStmtNodeTest.testExpression}"
            val parser = LexemParser(CustomStringReader.from(text))
            SelectiveStmtNode.parse(parser)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [""])
    fun `not parse the node`(text: String) {
        val parser = LexemParser(CustomStringReader.from(text))
        val res = SelectiveStmtNode.parse(parser)

        Assertions.assertNull(res, "The input has incorrectly parsed anything")
        Assertions.assertEquals(0, parser.reader.currentPosition(), "The parser must not advance the cursor")
    }
}
