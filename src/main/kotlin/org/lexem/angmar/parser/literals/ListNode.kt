package org.lexem.angmar.parser.literals

import com.google.gson.*
import org.lexem.angmar.*
import org.lexem.angmar.analyzer.nodes.literals.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.printer.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*
import org.lexem.angmar.parser.functional.expressions.*


/**
 * Parser for list literals.
 */
internal class ListNode private constructor(parser: LexemParser, parent: ParserNode, parentSignal: Int) :
        ParserNode(parser, parent, parentSignal) {
    val elements = mutableListOf<ParserNode>()
    var isConstant = false

    override fun toString() = StringBuilder().apply {
        if (isConstant) {
            append(constantToken)
        }
        append(startToken)
        append(elements.joinToString("$elementSeparator "))
        append(endToken)
    }.toString()

    override fun toTree(): JsonObject {
        val result = super.toTree()

        result.addProperty("isConstant", isConstant)
        result.add("elements", SerializationUtils.listToTest(elements))

        return result
    }

    override fun analyze(analyzer: LexemAnalyzer, signal: Int) = ListAnalyzer.stateMachine(analyzer, signal, this)

    companion object {
        const val startToken = "["
        const val constantToken = GlobalCommons.constantToken
        const val elementSeparator = GlobalCommons.elementSeparator
        const val endToken = "]"


        // METHODS ------------------------------------------------------------

        /**
         * Parses a list literal
         */
        fun parse(parser: LexemParser, parent: ParserNode, parentSignal: Int): ListNode? {
            parser.fromBuffer(parser.reader.currentPosition(), ListNode::class.java)?.let {
                it.parent = parent
                it.parentSignal = parentSignal
                return@parse it
            }

            val initCursor = parser.reader.saveCursor()
            val result = ListNode(parser, parent, parentSignal)

            result.isConstant = parser.readText(constantToken)

            if (!parser.readText(startToken)) {
                initCursor.restore()
                return null
            }

            WhitespaceNode.parse(parser)

            while (true) {
                val initLoopCursor = parser.reader.saveCursor()

                if (result.elements.isNotEmpty()) {
                    WhitespaceNode.parse(parser)

                    if (!parser.readText(elementSeparator)) {
                        initLoopCursor.restore()
                        break
                    }

                    WhitespaceNode.parse(parser)
                }

                val argument = ExpressionsCommons.parseExpression(parser, result,
                        result.elements.size + ListAnalyzer.signalEndFirstElement)
                if (argument == null) {
                    initLoopCursor.restore()
                    break
                }

                result.elements.add(argument)
            }

            // Trailing comma.
            let {
                val initTrailingCursor = parser.reader.saveCursor()

                WhitespaceNode.parse(parser)

                if (!parser.readText(elementSeparator)) {
                    initTrailingCursor.restore()
                    return@let
                }
            }

            WhitespaceNode.parse(parser)

            if (!parser.readText(endToken)) {
                throw AngmarParserException(AngmarParserExceptionType.ListWithoutEndToken,
                        "The close square bracket was expected '$endToken'.") {
                    val fullText = parser.reader.readAllText()
                    addSourceCode(fullText, parser.reader.getSource()) {
                        title = Consts.Logger.codeTitle
                        highlightSection(initCursor.position(), parser.reader.currentPosition() - 1)
                    }
                    addSourceCode(fullText, null) {
                        title = Consts.Logger.hintTitle
                        highlightCursorAt(parser.reader.currentPosition())
                        message = "Try adding the close square bracket '$endToken' here"
                    }
                }
            }

            return parser.finalizeNode(result, initCursor)
        }
    }
}
