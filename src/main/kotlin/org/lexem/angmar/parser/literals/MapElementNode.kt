package org.lexem.angmar.parser.literals

import com.google.gson.*
import org.lexem.angmar.*
import org.lexem.angmar.compiler.*
import org.lexem.angmar.compiler.literals.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*
import org.lexem.angmar.parser.functional.expressions.*


/**
 * Parser for key-value pairs of map literals.
 */
internal class MapElementNode private constructor(parser: LexemParser, parent: ParserNode) :
        ParserNode(parser, parent) {
    lateinit var key: ParserNode
    lateinit var value: ParserNode

    override fun toString() = StringBuilder().apply {
        append(key)
        append(keyValueSeparator)
        append(' ')
        append(value)
    }.toString()

    override fun toTree(): JsonObject {
        val result = super.toTree()

        result.add("key", key.toTree())
        result.add("value", value.toTree())

        return result
    }

    override fun compile(parent: CompiledNode, parentSignal: Int) =
            MapElementCompiled.compile(parent, parentSignal, this)

    companion object {
        const val keyValueSeparator = GlobalCommons.relationalToken


        // METHODS ------------------------------------------------------------

        /**
         * Parses a key-value pair of map literal.
         */
        fun parse(parser: LexemParser, parent: ParserNode): MapElementNode? {
            val initCursor = parser.reader.saveCursor()
            val result = MapElementNode(parser, parent)
            result.key = ExpressionsCommons.parseExpression(parser, result) ?: return null

            WhitespaceNode.parse(parser)

            if (!parser.readText(keyValueSeparator)) {
                throw AngmarParserException(AngmarParserExceptionType.MapElementWithoutRelationalSeparatorAfterKey,
                        "The relational separator '$keyValueSeparator' was expected after the key.") {
                    val fullText = parser.reader.readAllText()
                    addSourceCode(fullText, parser.reader.getSource()) {
                        title = Consts.Logger.codeTitle
                        highlightSection(initCursor.position(), parser.reader.currentPosition() - 1)
                    }
                    addSourceCode(fullText, null) {
                        title = Consts.Logger.hintTitle
                        highlightCursorAt(parser.reader.currentPosition())
                        message = "Try adding the relational separator '$keyValueSeparator' here"
                    }
                }
            }

            WhitespaceNode.parse(parser)

            result.value = ExpressionsCommons.parseExpression(parser, result) ?: throw AngmarParserException(
                    AngmarParserExceptionType.MapElementWithoutExpressionAfterRelationalSeparator,
                    "An expression acting as value was expected after the relational separator '$keyValueSeparator'.") {
                val fullText = parser.reader.readAllText()
                addSourceCode(fullText, parser.reader.getSource()) {
                    title = Consts.Logger.codeTitle
                    highlightSection(initCursor.position(), parser.reader.currentPosition() - 1)
                }
                addSourceCode(fullText, null) {
                    title = Consts.Logger.hintTitle
                    highlightCursorAt(parser.reader.currentPosition())
                    message = "Try adding an expression here"
                }
            }


            return parser.finalizeNode(result, initCursor)
        }
    }
}
