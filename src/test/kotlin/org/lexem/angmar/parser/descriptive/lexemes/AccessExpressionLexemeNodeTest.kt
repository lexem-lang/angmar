package org.lexem.angmar.parser.descriptive.lexemes

import org.junit.jupiter.api.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import org.lexem.angmar.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.readers.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*
import org.lexem.angmar.parser.functional.expressions.modifiers.*
import java.util.stream.*
import kotlin.streams.*

internal class AccessExpressionLexemeNodeTest {
    // PARAMETERS -------------------------------------------------------------

    companion object {
        const val testExpression = "${IdentifierNodeTest.testExpression}${AccessExplicitMemberNodeTest.testExpression}"

        @JvmStatic
        private fun provideCorrectAccesses(): Stream<Arguments> {
            val elements = listOf(IdentifierNodeTest.testExpression)
            val modifiers = listOf("", AccessExplicitMemberNodeTest.testExpression, IndexerNodeTest.testExpression,
                    FunctionCallNodeTest.testExpression)
            val sequence = sequence {
                for (element in elements.withIndex()) {
                    for (modifier in modifiers.withIndex()) {
                        for (modifier2 in modifiers.withIndex()) {
                            val text = "${element.value}${modifier.value}${modifier2.value}"

                            yield(Arguments.of(text, element.index, modifier.index, modifier2.index))
                        }

                    }
                }
            }

            return sequence.asStream()
        }

        // AUX METHODS --------------------------------------------------------

        fun checkTestExpression(node: ParserNode) {
            Assertions.assertTrue(node is AccessExpressionLexemeNode, "The input has not been correctly parsed")
            node as AccessExpressionLexemeNode

            Assertions.assertNotNull(node.element, "The element property cannot be null")
            IdentifierNodeTest.checkTestExpression(node.element)
            Assertions.assertEquals(1, node.modifiers.size, "The number of modifiers is incorrect")
            AccessExplicitMemberNodeTest.checkTestExpression(node.modifiers.first())
        }
    }


    // TESTS ------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("provideCorrectAccesses")
    fun `parse correct accesses`(text: String, elementType: Int, modifierType: Int, modifier2Type: Int) {
        val parser = LexemParser(IOStringReader.from(text))
        val res = AccessExpressionLexemeNode.parse(parser, ParserNode.Companion.EmptyParserNode)

        Assertions.assertNotNull(res, "The input has not been correctly parsed")

        if (modifierType == 0 && modifier2Type == 0) {
            res as ParserNode

            when (elementType) {
                0 -> {
                    res
                    IdentifierNodeTest.checkTestExpression(res.element)
                    Assertions.assertEquals(0, res.modifiers.size, "The number of modifiers is incorrect")
                }
                else -> throw AngmarUnreachableException()
            }
        } else {
            res as AccessExpressionLexemeNode

            when (elementType) {
                0 -> IdentifierNodeTest.checkTestExpression(res.element)
                else -> throw AngmarUnreachableException()
            }

            val count = if (modifierType != 0 && modifier2Type != 0) {
                2
            } else {
                1
            }
            Assertions.assertEquals(count, res.modifiers.size, "The number of modifiers is incorrect")

            var index = 0
            when (modifierType) {
                0 -> index -= 1
                1 -> AccessExplicitMemberNodeTest.checkTestExpression(res.modifiers[index])
                2 -> IndexerNodeTest.checkTestExpression(res.modifiers[index])
                3 -> FunctionCallNodeTest.checkTestExpression(res.modifiers[index])
                else -> throw AngmarUnreachableException()
            }

            index += 1

            when (modifier2Type) {
                0 -> Unit
                1 -> AccessExplicitMemberNodeTest.checkTestExpression(res.modifiers[index])
                2 -> IndexerNodeTest.checkTestExpression(res.modifiers[index])
                3 -> FunctionCallNodeTest.checkTestExpression(res.modifiers[index])
                else -> throw AngmarUnreachableException()
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [""])
    fun `not parse the node`(text: String) {
        val parser = LexemParser(IOStringReader.from(text))
        val res = AccessExpressionLexemeNode.parse(parser, ParserNode.Companion.EmptyParserNode)

        Assertions.assertNull(res, "The input has incorrectly parsed anything")
        Assertions.assertEquals(0, parser.reader.currentPosition(), "The parser must not advance the cursor")
    }
}

