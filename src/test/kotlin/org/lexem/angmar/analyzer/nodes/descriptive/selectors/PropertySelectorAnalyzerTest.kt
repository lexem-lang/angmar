package org.lexem.angmar.analyzer.nodes.descriptive.selectors

import org.junit.jupiter.api.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.parser.descriptive.selectors.*
import org.lexem.angmar.utils.*

internal class PropertySelectorAnalyzerTest {
    @Test
    fun `test addition`() {
        val propName = "propName"
        val grammar = "${PropertySelectorNode.token}$propName"
        val analyzer =
                TestUtils.createAnalyzerFrom(grammar, parserFunction = PropertySelectorNode.Companion::parseForAddition)

        // Prepare stack.
        val lxmNode = LxmNode("nodeName", analyzer.text.saveCursor(), null, analyzer.memory)
        lxmNode.setTo(analyzer.memory, analyzer.text.saveCursor())
        analyzer.memory.addToStack(AnalyzerCommons.Identifiers.Node, analyzer.memory.add(lxmNode))

        TestUtils.processAndCheckEmpty(analyzer)

        val result =
                analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.Node).dereference(analyzer.memory) as? LxmNode
                        ?: throw Error("The result must be a LxmNode")
        val props = result.getProperties(analyzer.memory)
        Assertions.assertEquals(LxmLogic.True, props.getPropertyValue(analyzer.memory, propName),
                "The property called $propName is incorrect")

        // Remove Node from the stack.
        analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.Node)

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `test filter - one`(isOk: Boolean) {
        val propName = "propName"
        val grammar = "${PropertySelectorNode.token}$propName"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = PropertySelectorNode.Companion::parse)

        // Prepare stack.
        val lxmNode = LxmNode("nodeName", analyzer.text.saveCursor(), null, analyzer.memory)
        if (isOk) {
            val props = lxmNode.getProperties(analyzer.memory)
            props.setProperty(analyzer.memory, propName, LxmLogic.True)
        }

        analyzer.memory.addToStack(AnalyzerCommons.Identifiers.Node, analyzer.memory.add(lxmNode))

        TestUtils.processAndCheckEmpty(analyzer)

        Assertions.assertEquals(LxmLogic.from(isOk), analyzer.memory.getLastFromStack(), "The result is incorrect")

        // Remove Node and Last from the stack.
        analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.Node)
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `test filter - many`(isOk: Boolean) {
        val propName = "propName"
        val grammar =
                "${PropertySelectorNode.token}${PropertySelectorNode.groupStartToken}${propName}ww${PropertySelectorNode.elementSeparator}${PropertyAbbreviationSelectorNode.notOperator}$propName${PropertySelectorNode.groupEndToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = PropertySelectorNode.Companion::parse)

        // Prepare stack.
        val lxmNode = LxmNode("nodeName", analyzer.text.saveCursor(), null, analyzer.memory)
        if (!isOk) {
            val props = lxmNode.getProperties(analyzer.memory)
            props.setProperty(analyzer.memory, propName, LxmLogic.True)
        }

        analyzer.memory.addToStack(AnalyzerCommons.Identifiers.Node, analyzer.memory.add(lxmNode))

        TestUtils.processAndCheckEmpty(analyzer)

        Assertions.assertEquals(LxmLogic.from(isOk), analyzer.memory.getLastFromStack(), "The result is incorrect")

        // Remove Node and Last from the stack.
        analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.Node)
        analyzer.memory.removeLastFromStack()

        TestUtils.checkEmptyStackAndContext(analyzer)
    }
}

