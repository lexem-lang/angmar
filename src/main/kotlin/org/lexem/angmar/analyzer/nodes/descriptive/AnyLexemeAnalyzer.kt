package org.lexem.angmar.analyzer.nodes.descriptive

import org.lexem.angmar.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.analyzer.nodes.*
import org.lexem.angmar.parser.descriptive.*


/**
 * Analyzer for lexemes.
 */
internal object AnyLexemeAnalyzer {
    const val signalEndDataCapturing = AnalyzerNodesCommons.signalStart + 1
    const val signalEndLexem = signalEndDataCapturing + 1
    const val signalEndQuantifier = signalEndLexem + 1
    const val signalStartLexemeForLazy = signalEndQuantifier + 1
    const val signalExitForGreedy = signalStartLexemeForLazy + 1

    // METHODS ----------------------------------------------------------------

    fun stateMachine(analyzer: LexemAnalyzer, signal: Int, node: AnyLexemeNode) {
        when (signal) {
            AnalyzerNodesCommons.signalStart -> {
                // Save the memory big node for atomic quantifiers
                val atomicFirstIndex = analyzer.memory.lastNode
                analyzer.memory.addToStack(AnalyzerCommons.Identifiers.AtomicFirstIndex, LxmBigNode(atomicFirstIndex))

                if (node.dataCapturing != null) {
                    return analyzer.nextNode(node.dataCapturing)
                }

                if (node.quantifier != null) {
                    return analyzer.nextNode(node.quantifier)
                }

                return analyzer.nextNode(node.lexeme)
            }
            signalEndDataCapturing -> {
                // Set the data capturing name.
                analyzer.memory.renameLastStackCell(AnalyzerCommons.Identifiers.LexemeDataCapturingName)

                val list = LxmList()
                analyzer.memory.addToStack(AnalyzerCommons.Identifiers.LexemeDataCapturingList,
                        analyzer.memory.add(list))

                if (node.quantifier != null) {
                    return analyzer.nextNode(node.quantifier)
                }

                return analyzer.nextNode(node.lexeme)
            }
            signalEndQuantifier -> {
                // Set the quantifier and the index.
                val quantifier = analyzer.memory.getLastFromStack() as LxmQuantifier
                val union = LxmPatternUnion(quantifier, LxmInteger.Num0, analyzer.memory)

                analyzer.memory.addToStack(AnalyzerCommons.Identifiers.LexemeUnion, analyzer.memory.add(union))

                // Remove Last from the stack.
                analyzer.memory.removeLastFromStack()

                return evaluateCondition(analyzer, node)
            }
            signalEndLexem -> {
                val result = analyzer.memory.getLastFromStack()

                // Add result to the list.
                if (node.dataCapturing != null) {
                    val list = analyzer.memory.getFromStack(
                            AnalyzerCommons.Identifiers.LexemeDataCapturingList).dereference(analyzer.memory) as LxmList
                    list.addCell(analyzer.memory, result)
                }

                // Remove Last from the stack.
                analyzer.memory.removeLastFromStack()

                if (node.quantifier != null) {
                    // Increase the index.
                    incrementIterationIndex(analyzer, node)

                    // Evaluate condition.
                    return evaluateCondition(analyzer, node)
                } else {
                    return finalization(analyzer, node, isAtomic = false)
                }
            }
            signalStartLexemeForLazy -> {
                // Evaluate the condition.
                val union = analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.LexemeUnion).dereference(
                        analyzer.memory) as LxmPatternUnion

                if (union.canHaveANextPattern(analyzer.memory)) {
                    // Execute the then block.
                    return analyzer.nextNode(node.lexeme)
                } else {
                    return analyzer.initBacktracking()
                }
            }
            signalExitForGreedy -> {
                val union = analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.LexemeUnion).dereference(
                        analyzer.memory) as LxmPatternUnion

                return finalization(analyzer, node, union.quantifier.isAtomic)
            }
            // Propagate the control signal.
            AnalyzerNodesCommons.signalExitControl, AnalyzerNodesCommons.signalNextControl, AnalyzerNodesCommons.signalRedoControl -> {
                finish(analyzer, node)

                return analyzer.nextNode(node.parent, signal)
            }
            AnalyzerNodesCommons.signalRestartControl, AnalyzerNodesCommons.signalReturnControl -> {
                finish(analyzer, node)

                return analyzer.nextNode(node.parent, signal)
            }
        }

        return analyzer.nextNode(node.parent, node.parentSignal)
    }

    /**
     * Performs the next iteration of a loop.
     */
    private fun evaluateCondition(analyzer: LexemAnalyzer, node: AnyLexemeNode) {
        // Evaluate the condition.
        val union = analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.LexemeUnion).dereference(
                analyzer.memory) as LxmPatternUnion
        val quantifier = union.quantifier
        val indexValue = union.getIndex(analyzer.memory)

        if (quantifier.isLazy) {
            when {
                quantifier.isFinished(indexValue.primitive) -> {
                    // Freezes a copy setting the next node to start the then block of the loop.
                    analyzer.freezeMemoryCopy(node, signalStartLexemeForLazy)

                    // Exit from the loop.
                    return finalization(analyzer, node, quantifier.isAtomic)
                }
                quantifier.canHaveANextIteration(indexValue.primitive) -> {
                    // Execute the lexeme.
                    return analyzer.nextNode(node.lexeme)
                }
                else -> {
                    return analyzer.initBacktracking()
                }
            }
        } else {
            when {
                quantifier.canHaveANextIteration(indexValue.primitive) -> {
                    // Freezes a copy setting the next node to start the then block of the loop
                    // only if the quantifier is satisfied.
                    if (quantifier.isFinished(indexValue.primitive)) {
                        analyzer.freezeMemoryCopy(node, signalExitForGreedy)
                    }

                    // Execute the lexeme.
                    return analyzer.nextNode(node.lexeme)
                }
                quantifier.isFinished(indexValue.primitive) -> {
                    // Exit from the loop.
                    return finalization(analyzer, node, quantifier.isAtomic)
                }
                else -> {
                    return analyzer.initBacktracking()
                }
            }
        }
    }

    /**
     * Evaluates the end of the lexeme.
     */
    private fun finalization(analyzer: LexemAnalyzer, node: AnyLexemeNode, isAtomic: Boolean) {
        if (isAtomic) {
            val atomicFirstIndex =
                    analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.AtomicFirstIndex) as LxmBigNode

            analyzer.memory.collapseTo(atomicFirstIndex.node)
        }

        // Sets the data capturing.
        setDataCapturing(analyzer, node)

        finish(analyzer, node)

        return analyzer.nextNode(node.parent, node.parentSignal)
    }

    /**
     * Sets the data captured in the specified variable.
     */
    private fun setDataCapturing(analyzer: LexemAnalyzer, node: AnyLexemeNode) {
        if (node.dataCapturing == null) {
            return
        }

        val setter = analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.LexemeDataCapturingName) as LexemSetter
        val listRef = analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.LexemeDataCapturingList)
        val list = listRef.dereference(analyzer.memory) as LxmList

        if (node.quantifier == null) {
            setter.setPrimitive(analyzer.memory, list.getCell(analyzer.memory, 0)!!)
        } else {
            setter.setPrimitive(analyzer.memory, listRef)
        }
    }

    /**
     * Increment the iteration index.
     */
    private fun incrementIterationIndex(analyzer: LexemAnalyzer, node: AnyLexemeNode, count: Int = 1) {
        val union = analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.LexemeUnion).dereference(
                analyzer.memory) as LxmPatternUnion

        union.increaseIndex(analyzer.memory)
    }

    /**
     * Process the finalization of the node.
     */
    private fun finish(analyzer: LexemAnalyzer, node: AnyLexemeNode) {
        // Remove AtomicFirstIndex, LexemeDataCapturingName, LexemeDataCapturingList, LexemeAlias and LexemeQuantifier from the stack.
        analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.AtomicFirstIndex)
        if (node.dataCapturing != null) {
            analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.LexemeDataCapturingName)
            analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.LexemeDataCapturingList)
        }

        if (node.quantifier != null) {
            analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.LexemeUnion)
        }
    }
}
