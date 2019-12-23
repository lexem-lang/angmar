package org.lexem.angmar.parser.literals

import com.google.gson.*
import org.lexem.angmar.*
import org.lexem.angmar.compiler.*
import org.lexem.angmar.compiler.literals.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.printer.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*


/**
 * Parser for abbreviated unicode interval literals.
 */
internal class UnicodeIntervalAbbrNode private constructor(parser: LexemParser, parent: ParserNode) :
        ParserNode(parser, parent) {
    val elements = mutableListOf<ParserNode>()
    var reversed = false

    override fun toString() = StringBuilder().apply {
        append(startToken)
        if (reversed) {
            append(reversedToken)
        }
        append(elements.joinToString(" "))
        append(endToken)
    }.toString()

    override fun toTree(): JsonObject {
        val result = super.toTree()

        result.add("elements", SerializationUtils.listToTest(elements))
        result.addProperty("reversed", reversed)

        return result
    }

    override fun compile(parent: CompiledNode, parentSignal: Int) =
            UnicodeIntervalAbbrCompiled.compile(parent, parentSignal, this)

    companion object {
        const val startToken = "["
        const val reversedToken = GlobalCommons.notToken
        const val endToken = "]"


        // METHODS ------------------------------------------------------------

        /**
         * Parses an abbreviated unicode interval literal.
         */
        fun parse(parser: LexemParser, parent: ParserNode): UnicodeIntervalAbbrNode? {
            val initCursor = parser.reader.saveCursor()
            val result = UnicodeIntervalAbbrNode(parser, parent)

            if (!parser.readText(startToken)) {
                return null
            }

            result.reversed = parser.readText(reversedToken)

            while (true) {
                val initLoopCursor = parser.reader.saveCursor()

                WhitespaceNode.parseSimpleWhitespaces(parser)

                val node =
                        UnicodeIntervalSubIntervalNode.parse(parser, result) ?: UnicodeIntervalElementNode.parse(parser,
                                result)
                if (node == null) {
                    initLoopCursor.restore()
                    break
                }

                result.elements.add(node)
            }

            WhitespaceNode.parseSimpleWhitespaces(parser)

            if (!parser.readText(endToken)) {
                throw AngmarParserException(AngmarParserExceptionType.UnicodeIntervalAbbreviationWithoutEndToken,
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
