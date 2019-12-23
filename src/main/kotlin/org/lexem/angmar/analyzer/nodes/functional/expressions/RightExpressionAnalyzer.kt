package org.lexem.angmar.analyzer.nodes.functional.expressions

import org.lexem.angmar.*
import org.lexem.angmar.analyzer.data.*
import org.lexem.angmar.analyzer.nodes.*
import org.lexem.angmar.compiler.functional.expressions.*


/**
 * Analyzer for right expressions.
 */
internal object RightExpressionAnalyzer {
    const val signalEndExpression = AnalyzerNodesCommons.signalStart + 1

    // METHODS ----------------------------------------------------------------

    fun stateMachine(analyzer: LexemAnalyzer, signal: Int, node: RightExpressionCompiled) {
        when (signal) {
            AnalyzerNodesCommons.signalStart -> {
                return analyzer.nextNode(node.expression)
            }
            signalEndExpression -> {
                // Check is not a setter.
                val value = analyzer.memory.getLastFromStack()
                if (value is LexemSetter) {
                    analyzer.memory.replaceLastStackCell(value.getSetterPrimitive(analyzer.memory))
                }
            }
        }

        return analyzer.nextNode(node.parent, node.parentSignal)
    }
}
