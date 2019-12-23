package org.lexem.angmar.parser.descriptive

import com.google.gson.*
import org.lexem.angmar.*
import org.lexem.angmar.compiler.*
import org.lexem.angmar.compiler.descriptive.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.printer.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.commons.*
import org.lexem.angmar.parser.descriptive.lexemes.*


/**
 * Parser for lexeme pattern anonymous groups.
 */
internal class LexemePatternGroupNode private constructor(parser: LexemParser, parent: ParserNode) :
        ParserNode(parser, parent) {
    var type = LexemePatternNode.Companion.PatternType.Alternative
    var quantifier: ExplicitQuantifierLexemeNode? = null
    val patterns = mutableListOf<LexemePatternContentNode?>()

    override fun toString() = patterns.mapIndexed { i, patternContent ->
        StringBuilder().apply {
            append(LexemePatternNode.patternToken)
            append(type.token)

            if (quantifier != null) {
                append(quantifier)
            }

            if (patternContent != null) {
                append(" ")
                append(patternContent)
            }
        }.toString()
    }.joinToString("\n")

    override fun toTree(): JsonObject {
        val result = super.toTree()

        result.addProperty("type", type.toString())
        result.add("quantifier", quantifier?.toTree())
        result.add("patterns", SerializationUtils.nullableListToTest(patterns))

        return result
    }

    override fun compile(parent: CompiledNode, parentSignal: Int) =
            LexemePatternGroupCompiled.compile(parent, parentSignal, this)

    companion object {

        // METHODS ------------------------------------------------------------

        /**
         * Parses a lexeme pattern anonymous group.
         */
        fun parse(parser: LexemParser, parent: ParserNode): LexemePatternGroupNode? {
            val initCursor = parser.reader.saveCursor()
            val result = LexemePatternGroupNode(parser, parent)

            val pattern = LexemePatternNode.parse(parser, result) ?: return null

            // Only get anonymous of those type that accept surrogates.
            if (pattern.unionName != null || pattern.type in LexemePatternNode.singlePatterns) {
                initCursor.restore()
                return null
            }

            result.type = pattern.type
            result.patterns.add(pattern.patternContent)

            if (pattern.patternContent != null) {
                relinkNode(pattern.patternContent!!, result)
            }

            // Handle dangling quantified patterns.
            if (result.type == LexemePatternNode.Companion.PatternType.Quantified) {
                if (pattern.quantifier == null) {
                    throw AngmarParserException(AngmarParserExceptionType.SlaveQuantifiedPatternWithoutMaster,
                            "Anonymous slave quantified patterns has not a master.") {
                        val fullText = parser.reader.readAllText()
                        addSourceCode(fullText, parser.reader.getSource()) {
                            title = Consts.Logger.codeTitle
                            highlightSection(initCursor.position(), parser.reader.currentPosition() - 1)
                            message = "Review the union of this pattern"
                        }
                    }
                } else {
                    result.quantifier = pattern.quantifier
                    relinkNode(pattern.quantifier!!, result)
                }
            }

            // Capture surrogates.
            while (true) {
                val initLoopCursor = parser.reader.saveCursor()

                WhitespaceNode.parse(parser)

                val pattern = LexemePatternNode.parse(parser, result)
                if (pattern == null || result.type != pattern.type || pattern.unionName != null || pattern.quantifier != null) {
                    initLoopCursor.restore()
                    break
                }

                result.patterns.add(pattern.patternContent)

                if (pattern.patternContent != null) {
                    relinkNode(pattern.patternContent!!, result)
                }
            }

            return parser.finalizeNode(result, initCursor)
        }

        /**
         * Relinks a node to a new parent.
         */
        private fun relinkNode(node: ParserNode, newParent: LexemePatternGroupNode) {
            node.parent = newParent
        }
    }
}
