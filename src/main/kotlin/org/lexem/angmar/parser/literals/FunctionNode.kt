package org.lexem.angmar.parser.literals

import com.google.gson.*
import org.lexem.angmar.*
import org.lexem.angmar.compiler.*
import org.lexem.angmar.compiler.literals.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*
import org.lexem.angmar.parser.functional.statements.*


/**
 * Parser for function literals.
 */
internal class FunctionNode private constructor(parser: LexemParser, parent: ParserNode) : ParserNode(parser, parent) {
    lateinit var block: ParserNode
    var parameterList: FunctionParameterListNode? = null

    override fun toString() = StringBuilder().apply {
        append(keyword)
        if (parameterList != null) {
            append(' ')
            append(parameterList)
        }

        append(' ')
        append(block)
    }.toString()

    override fun toTree(): JsonObject {
        val result = super.toTree()

        result.add("parameterList", parameterList?.toTree())
        result.add("block", block.toTree())

        return result
    }

    override fun compile(parent: CompiledNode, parentSignal: Int) = FunctionCompiled.compile(parent, parentSignal, this)

    companion object {
        const val keyword = "fun"


        // METHODS ------------------------------------------------------------

        /**
         * Parses a function literal.
         */
        fun parse(parser: LexemParser, parent: ParserNode): FunctionNode? {
            val initCursor = parser.reader.saveCursor()
            val result = FunctionNode(parser, parent)

            if (!Commons.parseKeyword(parser, keyword)) {
                return null
            }

            WhitespaceNode.parse(parser)

            result.parameterList = FunctionParameterListNode.parse(parser, result)
            if (result.parameterList != null) {
                WhitespaceNode.parse(parser)
            }

            val keepIsDescriptiveCode = parser.isDescriptiveCode
            parser.isDescriptiveCode = false
            result.block = BlockStmtNode.parse(parser, result) ?: LambdaStmtNode.parse(parser, result)
                    ?: throw AngmarParserException(AngmarParserExceptionType.FunctionWithoutBlock,
                    "A block of code was expected after the '$keyword' keyword.") {
                val fullText = parser.reader.readAllText()
                addSourceCode(fullText, parser.reader.getSource()) {
                    title = Consts.Logger.codeTitle
                    highlightSection(initCursor.position(), parser.reader.currentPosition() - 1)
                }
                addSourceCode(fullText, null) {
                    title = Consts.Logger.hintTitle
                    highlightCursorAt(parser.reader.currentPosition())
                    message = "Try adding an empty block '${BlockStmtNode.startToken}${BlockStmtNode.endToken}' here"
                }
            }
            parser.isDescriptiveCode = keepIsDescriptiveCode

            return parser.finalizeNode(result, initCursor)
        }
    }
}
