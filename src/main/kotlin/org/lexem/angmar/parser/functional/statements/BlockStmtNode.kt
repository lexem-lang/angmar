package org.lexem.angmar.parser.functional.statements

import com.google.gson.*
import org.lexem.angmar.*
import org.lexem.angmar.analyzer.nodes.functional.statements.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.printer.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*


/**
 * Parser for block statements.
 */
internal class BlockStmtNode private constructor(parser: LexemParser, parent: ParserNode, parentSignal: Int) :
        ParserNode(parser, parent, parentSignal) {
    var tag: IdentifierNode? = null
    val statements = mutableListOf<ParserNode>()

    override fun toString() = StringBuilder().apply {
        append(startToken)
        if (tag != null) {
            append(tagPrefix)
            append(tag)
        }
        append('\n')
        append(statements.joinToString("\n    "))
        append('\n')
        append(endToken)
    }.toString()

    override fun toTree(): JsonObject {
        val result = super.toTree()

        result.add("tag", tag?.toTree())
        result.add("statements", SerializationUtils.listToTest(statements))

        return result
    }

    override fun analyze(analyzer: LexemAnalyzer, signal: Int) = BlockStmtAnalyzer.stateMachine(analyzer, signal, this)

    companion object {
        const val startToken = "{"
        const val endToken = "}"
        const val tagPrefix = GlobalCommons.tagPrefix

        // METHODS ------------------------------------------------------------

        /**
         * Parses a block statement.
         */
        fun parse(parser: LexemParser, parent: ParserNode, parentSignal: Int): BlockStmtNode? {
            parser.fromBuffer(parser.reader.currentPosition(), BlockStmtNode::class.java)?.let {
                it.parent = parent
                it.parentSignal = parentSignal
                return@parse it
            }

            val initCursor = parser.reader.saveCursor()

            if (!parser.readText(startToken)) {
                return null
            }

            val result = BlockStmtNode(parser, parent, parentSignal)

            // tag
            let {
                val initTagCursor = parser.reader.saveCursor()

                if (!parser.readText(tagPrefix)) {
                    return@let
                }

                result.tag = IdentifierNode.parse(parser, result, BlockStmtAnalyzer.signalEndTag)
                if (result.tag == null) {
                    initTagCursor.restore()
                }
            }

            while (true) {
                val initLoopCursor = parser.reader.saveCursor()

                WhitespaceNode.parse(parser)

                val statement = GlobalCommons.parseBlockStatement(parser, result,
                        result.statements.size + BlockStmtAnalyzer.signalEndFirstStatement)
                if (statement == null) {
                    initLoopCursor.restore()
                    break
                }

                result.statements.add(statement)
            }

            WhitespaceNode.parse(parser)

            if (!parser.readText(endToken)) {
                throw AngmarParserException(AngmarParserExceptionType.BlockStatementWithoutEndToken,
                        "The close bracket was expected '$endToken' to finish the block statement.") {
                    val fullText = parser.reader.readAllText()
                    addSourceCode(fullText, parser.reader.getSource()) {
                        title = Consts.Logger.codeTitle
                        highlightSection(initCursor.position(), parser.reader.currentPosition() - 1)
                    }
                    addSourceCode(fullText, null) {
                        title = Consts.Logger.hintTitle
                        highlightCursorAt(parser.reader.currentPosition())
                        message = "Try adding the close bracket '$endToken' here"
                    }
                }
            }

            return parser.finalizeNode(result, initCursor)
        }
    }
}
