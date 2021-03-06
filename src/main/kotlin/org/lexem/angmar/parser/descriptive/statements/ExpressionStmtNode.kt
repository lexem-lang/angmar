package org.lexem.angmar.parser.descriptive.statements

import com.google.gson.*
import org.lexem.angmar.*
import org.lexem.angmar.analyzer.nodes.descriptive.statements.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*
import org.lexem.angmar.parser.functional.statements.*
import org.lexem.angmar.parser.literals.*


/**
 * Parser for expression statements.
 */
internal class ExpressionStmtNode private constructor(parser: LexemParser, parent: ParserNode, parentSignal: Int) :
        ParserNode(parser, parent, parentSignal) {
    lateinit var name: IdentifierNode
    lateinit var block: ParserNode
    var properties: PropertyStyleObjectBlockNode? = null
    var parameterList: FunctionParameterListNode? = null

    override fun toString() = StringBuilder().apply {
        append(keyword)
        append(' ')
        append(name)

        if (properties != null) {
            append(properties)
        }

        if (parameterList != null) {
            if (properties != null) {
                append(' ')
            }

            append(parameterList)
        }

        append(' ')
        append(block)
    }.toString()

    override fun toTree(): JsonObject {
        val result = super.toTree()

        result.add("properties", properties?.toTree())
        result.add("arguments", parameterList?.toTree())
        result.add("block", block.toTree())

        return result
    }

    override fun analyze(analyzer: LexemAnalyzer, signal: Int) =
            ExpressionStmtAnalyzer.stateMachine(analyzer, signal, this)

    companion object {
        const val keyword = "exp"


        // METHODS ------------------------------------------------------------

        /**
         * Parses a expression statement.
         */
        fun parse(parser: LexemParser, parent: ParserNode, parentSignal: Int): ExpressionStmtNode? {
            parser.fromBuffer(parser.reader.currentPosition(), ExpressionStmtNode::class.java)?.let {
                it.parent = parent
                it.parentSignal = parentSignal
                return@parse it
            }

            val initCursor = parser.reader.saveCursor()
            val result = ExpressionStmtNode(parser, parent, parentSignal)

            if (!Commons.parseKeyword(parser, keyword)) {
                return null
            }

            WhitespaceNode.parse(parser)

            result.name = IdentifierNode.parse(parser, result, ExpressionStmtAnalyzer.signalEndName) ?: let {
                initCursor.restore()
                return@parse null
            }

            WhitespaceNode.parse(parser)

            result.properties =
                    PropertyStyleObjectBlockNode.parse(parser, result, ExpressionStmtAnalyzer.signalEndProperties)
            if (result.properties != null) {
                WhitespaceNode.parse(parser)
            }

            result.parameterList =
                    FunctionParameterListNode.parse(parser, result, ExpressionStmtAnalyzer.signalEndParameterList)
            if (result.parameterList != null) {
                WhitespaceNode.parse(parser)
            }

            val keepIsDescriptiveCode = parser.isDescriptiveCode
            val keepIsFilterCode = parser.isFilterCode
            parser.isDescriptiveCode = true
            parser.isFilterCode = false

            result.block = BlockStmtNode.parse(parser, result, ExpressionStmtAnalyzer.signalEndBlock)
                    ?: throw AngmarParserException(AngmarParserExceptionType.ExpressionStatementWithoutBlock,
                            "Expressions require a block of code.") {
                        val fullText = parser.reader.readAllText()
                        addSourceCode(fullText, parser.reader.getSource()) {
                            title = Consts.Logger.codeTitle
                            highlightSection(initCursor.position(), parser.reader.currentPosition() - 1)
                        }
                        addSourceCode(fullText, null) {
                            title = Consts.Logger.hintTitle
                            highlightCursorAt(parser.reader.currentPosition())
                            message =
                                    "Try adding an empty block '${BlockStmtNode.startToken}${BlockStmtNode.endToken}' here"
                        }
                    }

            parser.isDescriptiveCode = keepIsDescriptiveCode
            parser.isFilterCode = keepIsFilterCode

            return parser.finalizeNode(result, initCursor)
        }
    }
}
