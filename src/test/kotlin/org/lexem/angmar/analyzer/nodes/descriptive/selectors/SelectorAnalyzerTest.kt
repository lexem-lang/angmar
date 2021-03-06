package org.lexem.angmar.analyzer.nodes.descriptive.selectors

import org.junit.jupiter.api.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.parser.descriptive.selectors.*
import org.lexem.angmar.utils.*
import java.util.stream.*
import kotlin.streams.*

internal class SelectorAnalyzerTest {
    // PARAMETERS -------------------------------------------------------------

    companion object {
        @JvmStatic
        private fun provideFilterOptions(): Stream<Arguments> {
            val sequence = sequence {
                for (isOk in listOf(false, true)) {
                    for (hasName in listOf(false, true)) {
                        for (hasProperty in listOf(false, true)) {
                            for (hasMethod in listOf(false, true)) {
                                if (!(hasName || hasProperty || hasMethod)) {
                                    continue
                                }

                                yield(Arguments.of(isOk, hasName, hasProperty, hasMethod))
                            }
                        }
                    }
                }
            }

            return sequence.asStream()
        }
    }


    // TESTS ------------------------------------------------------------------

    @Test
    fun `test addition - only name`() {
        val nodeName = "nodeName"
        val grammar = nodeName
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = SelectorNode.Companion::parseForAddition)

        TestUtils.processAndCheckEmpty(analyzer)

        analyzer.memory.getLastFromStack().dereference(analyzer.memory) as? LxmNode ?: throw Error(
                "The result must be a LxmNode")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @Test
    fun `test addition - with properties`() {
        val nodeName = "nodeName"
        val propName1 = "propName1"
        val propName2 = "propName2"
        val grammar =
                "$nodeName${PropertySelectorNode.token}$propName1${PropertySelectorNode.token}${PropertyAbbreviationSelectorNode.notOperator}$propName2"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = SelectorNode.Companion::parseForAddition)

        TestUtils.processAndCheckEmpty(analyzer)

        val result = analyzer.memory.getLastFromStack().dereference(analyzer.memory) as? LxmNode ?: throw Error(
                "The result must be a LxmNode")
        val props = result.getProperties(analyzer.memory)
        Assertions.assertEquals(LxmLogic.True, props.getPropertyValue(analyzer.memory, propName1),
                "The property called $propName1 is incorrect")
        Assertions.assertEquals(LxmLogic.False, props.getPropertyValue(analyzer.memory, propName2),
                "The property called $propName2 is incorrect")

        // Remove Last from the stack.
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @ParameterizedTest
    @MethodSource("provideFilterOptions")
    fun `test filter`(isOk: Boolean, hasName: Boolean, hasProperty: Boolean, hasMethod: Boolean) {
        val nodeName = "nodeName"
        val propertyName = "propertyName"
        var grammar = if (hasName) {
            nodeName
        } else {
            ""
        }

        grammar += if (hasProperty) {
            "${PropertySelectorNode.token}$propertyName"
        } else {
            ""
        }

        grammar += if (hasMethod) {
            "${MethodSelectorNode.relationalToken}${AnalyzerCommons.SelectorMethods.Empty}"
        } else {
            ""
        }

        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = SelectorNode.Companion::parse)

        // Prepare stack.
        if (isOk) {
            val lxmNode = LxmNode(nodeName, analyzer.text.saveCursor(), null, analyzer.memory)
            val lxmNodeRef = analyzer.memory.add(lxmNode)

            lxmNode.getProperties(analyzer.memory).setProperty(analyzer.memory, propertyName, LxmLogic.True)
            analyzer.memory.addToStack(AnalyzerCommons.Identifiers.Node, lxmNodeRef)
        } else {
            when {
                hasMethod -> {
                    val lxmNode = LxmNode(nodeName, analyzer.text.saveCursor(), null, analyzer.memory)
                    val lxmNodeRef = analyzer.memory.add(lxmNode)
                    val lxmNodeAux = LxmNode("aux", analyzer.text.saveCursor(), null, analyzer.memory)

                    lxmNode.getChildren(analyzer.memory)
                            .addCell(analyzer.memory, analyzer.memory.add(lxmNodeAux), ignoreConstant = true)
                    lxmNode.getProperties(analyzer.memory).setProperty(analyzer.memory, propertyName, LxmLogic.True)
                    analyzer.memory.addToStack(AnalyzerCommons.Identifiers.Node, lxmNodeRef)
                }
                hasProperty -> {
                    val lxmNode = LxmNode(nodeName, analyzer.text.saveCursor(), null, analyzer.memory)
                    val lxmNodeRef = analyzer.memory.add(lxmNode)

                    analyzer.memory.addToStack(AnalyzerCommons.Identifiers.Node, lxmNodeRef)
                }
                else -> {
                    val lxmNode = LxmNode(nodeName + "x", analyzer.text.saveCursor(), null, analyzer.memory)
                    val lxmNodeRef = analyzer.memory.add(lxmNode)

                    analyzer.memory.addToStack(AnalyzerCommons.Identifiers.Node, lxmNodeRef)
                }
            }
        }

        TestUtils.processAndCheckEmpty(analyzer)

        Assertions.assertEquals(LxmLogic.from(isOk), analyzer.memory.getLastFromStack(), "The result is incorrect")

        // Remove Node and Last from the stack.
        analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.Node)
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }
}

