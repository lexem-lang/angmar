package org.lexem.angmar.analyzer.nodes.literals

import org.junit.jupiter.api.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.parser.literals.*
import org.lexem.angmar.utils.*

internal class MapElementAnalyzerTest {
    @Test
    fun `test normal`() {
        val value = 123
        val key = 456
        val text = "$key${MapElementNode.keyValueSeparator}$value"
        val analyzer = TestUtils.createAnalyzerFrom(text, parserFunction = MapElementNode.Companion::parse)

        // Prepare stack.
        val map = LxmMap(null)
        val mapRef = analyzer.memory.add(map)
        analyzer.memory.addToStack(AnalyzerCommons.Identifiers.Accumulator, mapRef)

        TestUtils.processAndCheckEmpty(analyzer)

        val resultRef =
                analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.Accumulator) as? LxmReference ?: throw Error(
                        "The result must be a LxmReference")
        val mapDeref = resultRef.dereferenceAs<LxmMap>(analyzer.memory) ?: throw Error("The result must be a LxmMap")
        val element =
                mapDeref.getDereferencedProperty<LxmInteger>(analyzer.memory, LxmInteger.from(key)) ?: throw Error(
                        "The element must be a LxmInteger")

        Assertions.assertEquals(value, element.primitive, "The primitive property is incorrect")

        // Remove Accumulator from the stack.
        analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.Accumulator)

        TestUtils.checkEmptyStackAndContext(analyzer)
    }
}
