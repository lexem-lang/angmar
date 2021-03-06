package org.lexem.angmar.analyzer.nodes.descriptive.lexemes

import org.junit.jupiter.api.*
import org.lexem.angmar.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.readers.*
import org.lexem.angmar.parser.descriptive.*
import org.lexem.angmar.parser.descriptive.lexemes.*
import org.lexem.angmar.parser.descriptive.statements.*
import org.lexem.angmar.parser.functional.expressions.macros.*
import org.lexem.angmar.parser.functional.expressions.modifiers.*
import org.lexem.angmar.parser.functional.statements.*
import org.lexem.angmar.parser.functional.statements.controls.*
import org.lexem.angmar.parser.literals.*
import org.lexem.angmar.utils.*

internal class AccessLexemAnalyzerTest {
    @Test
    fun `test call internal function`() {
        val variableName = "test"
        val functionName = "fn"
        val returnValue = 3
        val grammar =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $functionName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = AnyLexemeNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        context.setProperty(analyzer.memory, functionName, LxmInternalFunction { analyzer, _, _ ->
            analyzer.memory.addToStackAsLast(LxmInteger.from(returnValue))
            return@LxmInternalFunction true
        })

        TestUtils.processAndCheckEmpty(analyzer)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val result = context.getPropertyValue(analyzer.memory, variableName) as? LxmInteger ?: throw Error(
                "The result must be a LxmInteger")

        Assertions.assertEquals(returnValue, result.primitive, "The result is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName, functionName))
    }

    @Test
    fun `test call function`() {
        val variableName = "test"
        val functionName = "fn"
        val returnValue = 3
        val function =
                "${FunctionNode.keyword} $functionName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} ${ControlWithExpressionStmtNode.returnKeyword} $returnValue${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $functionName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$function \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        context.setProperty(analyzer.memory, variableName, LxmNil)

        TestUtils.processAndCheckEmpty(analyzer, bigNodeCount = 2)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val result = context.getPropertyValue(analyzer.memory, variableName) as? LxmInteger ?: throw Error(
                "The result must be a LxmInteger")

        Assertions.assertEquals(returnValue, result.primitive, "The result is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName))
    }

    @Test
    fun `test call expression`() {
        val text = "this is a test"
        val variableName = "test"
        val expressionName = "expr"
        val pattern =
                "${LexemePatternNode.patternToken}${LexemePatternNode.staticTypeToken} ${StringNode.startToken}$text${StringNode.endToken}"
        val expression =
                "${ExpressionStmtNode.keyword} $expressionName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} $pattern ${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $expressionName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$expression \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)
        val textReader = IOStringReader.from(text)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        context.setProperty(analyzer.memory, variableName, LxmNil)

        TestUtils.processAndCheckEmpty(analyzer, textReader, bigNodeCount = 2)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val resultRef = context.getPropertyValue(analyzer.memory, variableName) as LxmReference
        val result = resultRef.dereference(analyzer.memory) as? LxmNode ?: throw Error("The result must be a LxmNode")
        val resultParent = result.getParent(analyzer.memory)!!
        val resultParentChildren = resultParent.getChildrenAsList(analyzer.memory)

        Assertions.assertEquals(expressionName, result.name, "The name property is incorrect")
        Assertions.assertEquals(0, result.getFrom(analyzer.memory).primitive.position(),
                "The from property is incorrect")
        Assertions.assertEquals(text.length, result.getTo(analyzer.memory)!!.primitive.position(),
                "The to property is incorrect")
        Assertions.assertEquals(AnalyzerCommons.Identifiers.Root, resultParent.name, "The parent is incorrect")
        Assertions.assertEquals(1, resultParentChildren.size, "The parent children count is incorrect")
        Assertions.assertEquals(resultRef.position, (resultParentChildren[0] as LxmReference).position,
                "The parent child[0] is incorrect")
        Assertions.assertEquals(textReader, analyzer.text, "The text has changed")
        Assertions.assertEquals(text.length, analyzer.text.currentPosition(),
                "The lexem has not consumed the characters")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        removeNode(analyzer)

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName))
    }

    @Test
    fun `test call filter`() {
        val node2ProcessVar = "node2ProcessVar"
        val variableName = "test"
        val filterName = "filter"
        val nodeName = "nodeName"
        val pattern =
                "${LexemePatternNode.patternToken}${LexemePatternNode.staticTypeToken} ${FilterLexemeNode.startToken}$nodeName${FilterLexemeNode.endToken}"
        val filter =
                "${FilterStmtNode.keyword} $filterName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} $pattern ${BlockStmtNode.endToken}"
        val filterArgs =
                "${AnalyzerCommons.Identifiers.Node2FilterParameter} ${FunctionCallNamedArgumentNode.relationalToken} $node2ProcessVar"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $filterName${FunctionCallNode.startToken}$filterArgs${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$filter \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val parent = LxmNode("processedNode", analyzer.text.saveCursor(), null, analyzer.memory)
        val parentRef = analyzer.memory.add(parent)
        val children = parent.getChildren(analyzer.memory)
        val childNode = LxmNode(nodeName, analyzer.text.saveCursor(), parentRef, analyzer.memory)
        val childNodeRef = analyzer.memory.add(childNode)
        children.addCell(analyzer.memory, childNodeRef, ignoreConstant = true)

        context.setProperty(analyzer.memory, variableName, LxmNil)
        context.setProperty(analyzer.memory, node2ProcessVar, parentRef)

        TestUtils.processAndCheckEmpty(analyzer, bigNodeCount = 2)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val resultRef = context.getPropertyValue(analyzer.memory, variableName) as LxmReference
        val result = resultRef.dereference(analyzer.memory) as? LxmNode ?: throw Error("The result must be a LxmNode")
        val resultParent = result.getParent(analyzer.memory)!!
        val resultParentChildren = resultParent.getChildrenAsList(analyzer.memory)

        Assertions.assertEquals(parent.name, result.name, "The name property is incorrect")
        Assertions.assertEquals(0, result.getFrom(analyzer.memory).primitive.position(),
                "The from property is incorrect")
        Assertions.assertEquals(0, result.getTo(analyzer.memory)!!.primitive.position(), "The to property is incorrect")
        Assertions.assertEquals(AnalyzerCommons.Identifiers.Root, resultParent.name, "The parent is incorrect")
        Assertions.assertEquals(1, resultParentChildren.size, "The parent children count is incorrect")
        Assertions.assertEquals(resultRef.position, (resultParentChildren[0] as LxmReference).position,
                "The parent child[0] is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        // Remove the circular references of the nodes.
        childNodeRef.dereferenceAs<LxmNode>(analyzer.memory)!!.setProperty(analyzer.memory,
                AnalyzerCommons.Identifiers.Parent, LxmNil, ignoringConstant = true)

        removeNode(analyzer)

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName, node2ProcessVar))
    }

    @Test
    fun `test call expression - expression`() {
        val leftText = "This "
        val middleText = "is a "
        val rightText = "test"
        val text = "$leftText$middleText$rightText"
        val variableName = "test"
        val expression2Name = "expr2"
        val expressionMainName = "exprMain"
        val pattern2 =
                "${LexemePatternNode.patternToken}${LexemePatternNode.staticTypeToken} ${StringNode.startToken}$middleText${StringNode.endToken}"
        val expression2 =
                "${ExpressionStmtNode.keyword} $expression2Name${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} $pattern2 ${BlockStmtNode.endToken}"
        val patternMain =
                "${LexemePatternNode.patternToken}${LexemePatternNode.staticTypeToken} ${StringNode.startToken}$middleText$rightText${StringNode.endToken}"
        val expressionMain =
                "${ExpressionStmtNode.keyword} $expressionMainName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} $patternMain ${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $expressionMainName${FunctionCallNode.startToken}${FunctionCallNode.endToken} ${AccessLexemeNode.nextAccessToken} $expression2Name${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$expressionMain \n $expression2 \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)
        val textReader = IOStringReader.from(text)
        textReader.setPosition(leftText.length)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        context.setProperty(analyzer.memory, variableName, LxmNil)

        TestUtils.processAndCheckEmpty(analyzer, textReader, bigNodeCount = 2)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val resultRef = context.getPropertyValue(analyzer.memory, variableName) as LxmReference
        val result = resultRef.dereference(analyzer.memory) as? LxmNode ?: throw Error("The result must be a LxmNode")
        val resultParent = result.getParent(analyzer.memory)!!
        val resultParentChildren = resultParent.getChildrenAsList(analyzer.memory)

        Assertions.assertEquals(expression2Name, result.name, "The name property is incorrect")
        Assertions.assertEquals(leftText.length, result.getFrom(analyzer.memory).primitive.position(),
                "The from property is incorrect")
        Assertions.assertEquals(leftText.length + middleText.length,
                result.getTo(analyzer.memory)!!.primitive.position(), "The to property is incorrect")
        Assertions.assertEquals(AnalyzerCommons.Identifiers.Root, resultParent.name, "The parent is incorrect")
        Assertions.assertEquals(1, resultParentChildren.size, "The parent children count is incorrect")
        Assertions.assertEquals(resultRef.position, (resultParentChildren[0] as LxmReference).position,
                "The parent child[0] is incorrect")
        Assertions.assertEquals(textReader, analyzer.text, "The text has changed")
        Assertions.assertEquals(text.length, analyzer.text.currentPosition(),
                "The lexem has not consumed the characters")

        val lxmNode = context.getDereferencedProperty<LxmNode>(analyzer.memory,
                AnalyzerCommons.Identifiers.HiddenLastResultNode) ?: throw Error("The node must be a LxmNode")
        Assertions.assertEquals(1, lxmNode.getChildrenAsList(analyzer.memory).size,
                "The number of results is incorrect")
        Assertions.assertEquals(result, lxmNode.getChildrenAsList(analyzer.memory).first().dereference(analyzer.memory),
                "The result is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        removeNode(analyzer)

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName))
    }

    @Test
    fun `test call negated function - matching`() {
        val variableName = "test"
        val functionName = "fn"
        val returnValue = 3
        val function =
                "${FunctionNode.keyword} $functionName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} ${ControlWithExpressionStmtNode.returnKeyword} $returnValue${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} ${AccessLexemeNode.notOperator}$functionName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$function \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        val context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        context.setProperty(analyzer.memory, variableName, LxmNil)

        TestUtils.processAndCheckEmpty(analyzer, status = LexemAnalyzer.ProcessStatus.Backward, bigNodeCount = 0)

        Assertions.assertEquals(LxmNil, context.getPropertyValue(analyzer.memory, variableName),
                "The result is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName))
    }

    @Test
    fun `test call negated expression - matching`() {
        val text = "this is a test"
        val variableName = "test"
        val expressionName = "expr"
        val pattern =
                "${LexemePatternNode.patternToken}${LexemePatternNode.staticTypeToken} ${StringNode.startToken}$text${StringNode.endToken}"
        val expression =
                "${ExpressionStmtNode.keyword} $expressionName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} $pattern ${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} ${AccessLexemeNode.notOperator}$expressionName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$expression \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)
        val textReader = IOStringReader.from(text)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        context.setProperty(analyzer.memory, variableName, LxmNil)

        TestUtils.processAndCheckEmpty(analyzer, textReader, status = LexemAnalyzer.ProcessStatus.Backward,
                bigNodeCount = 0)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        Assertions.assertEquals(LxmNil, context.getPropertyValue(analyzer.memory, variableName),
                "The result is incorrect")
        Assertions.assertEquals(0, analyzer.text.currentPosition(), "The lexem has consumed some characters")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName))
    }

    @Test
    fun `test call node - !filter - matching`() {
        val node2ProcessVar = "node2ProcessVar"
        val variableName = "test"
        val filterName = "filter"
        val nodeName = "nodeName"
        val pattern =
                "${LexemePatternNode.patternToken}${LexemePatternNode.staticTypeToken} ${FilterLexemeNode.startToken}$nodeName${FilterLexemeNode.endToken}"
        val filter =
                "${FilterStmtNode.keyword} $filterName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} $pattern ${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $node2ProcessVar ${AccessLexemeNode.nextAccessToken} ${AccessLexemeNode.notOperator}$filterName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$filter \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val parent = LxmNode("processedNode", analyzer.text.saveCursor(), null, analyzer.memory)
        val parentRef = analyzer.memory.add(parent)
        val children = parent.getChildren(analyzer.memory)
        val childNode = LxmNode(nodeName, analyzer.text.saveCursor(), parentRef, analyzer.memory)
        val childNodeRef = analyzer.memory.add(childNode)
        children.addCell(analyzer.memory, childNodeRef, ignoreConstant = true)

        context.setProperty(analyzer.memory, variableName, LxmNil)
        context.setProperty(analyzer.memory, node2ProcessVar, parentRef)

        TestUtils.processAndCheckEmpty(analyzer, status = LexemAnalyzer.ProcessStatus.Backward, bigNodeCount = 0)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)

        Assertions.assertEquals(LxmNil, context.getPropertyValue(analyzer.memory, variableName),
                "The result is incorrect")
        Assertions.assertEquals(0, analyzer.text.currentPosition(), "The lexem has consumed some characters")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        // Remove the circular references of the nodes.
        childNodeRef.dereferenceAs<LxmNode>(analyzer.memory)!!.setProperty(analyzer.memory,
                AnalyzerCommons.Identifiers.Parent, LxmNil, ignoringConstant = true)

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName, node2ProcessVar))
    }

    @Test
    fun `test call negated function - not matching`() {
        val variableName = "test"
        val functionName = "fn"
        val function =
                "${FunctionNode.keyword} $functionName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} ${MacroBacktrackNode.macroName}${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} ${AccessLexemeNode.notOperator}$functionName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$function \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        val context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        context.setProperty(analyzer.memory, variableName, LxmNil)

        TestUtils.processAndCheckEmpty(analyzer, bigNodeCount = 2)
        Assertions.assertEquals(LxmNil, context.getPropertyValue(analyzer.memory, variableName),
                "The result is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName))
    }

    @Test
    fun `test call negated expression - not matching`() {
        val variableName = "test"
        val expressionName = "expr"
        val expression =
                "${ExpressionStmtNode.keyword} $expressionName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} ${MacroBacktrackNode.macroName}${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} ${AccessLexemeNode.notOperator}$expressionName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$expression \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        val context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        context.setProperty(analyzer.memory, variableName, LxmNil)

        TestUtils.processAndCheckEmpty(analyzer, bigNodeCount = 2)
        Assertions.assertEquals(LxmNil, context.getPropertyValue(analyzer.memory, variableName),
                "The result is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName))
    }

    @Test
    fun `test call node - !filter - not matching`() {
        val node2ProcessVar = "node2ProcessVar"
        val variableName = "test"
        val filterName = "filter"
        val nodeName = "nodeName"
        val pattern =
                "${LexemePatternNode.patternToken}${LexemePatternNode.staticTypeToken} ${FilterLexemeNode.startToken}$nodeName${FilterLexemeNode.endToken}"
        val filter =
                "${FilterStmtNode.keyword} $filterName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} $pattern ${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $node2ProcessVar ${AccessLexemeNode.nextAccessToken} ${AccessLexemeNode.notOperator}$filterName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$filter \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val parent = LxmNode("processedNode", analyzer.text.saveCursor(), null, analyzer.memory)
        val parentRef = analyzer.memory.add(parent)
        val children = parent.getChildren(analyzer.memory)
        val childNode = LxmNode(nodeName + "x", analyzer.text.saveCursor(), parentRef, analyzer.memory)
        val childNodeRef = analyzer.memory.add(childNode)
        children.addCell(analyzer.memory, childNodeRef, ignoreConstant = true)

        context.setProperty(analyzer.memory, variableName, LxmNil)
        context.setProperty(analyzer.memory, node2ProcessVar, parentRef)

        TestUtils.processAndCheckEmpty(analyzer, bigNodeCount = 2)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)

        Assertions.assertEquals(LxmNil, context.getPropertyValue(analyzer.memory, variableName),
                "The result is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        // Remove the circular references of the nodes.
        childNodeRef.dereferenceAs<LxmNode>(analyzer.memory)!!.setProperty(analyzer.memory,
                AnalyzerCommons.Identifiers.Parent, LxmNil, ignoringConstant = true)

        removeNode(analyzer)

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName, node2ProcessVar))
    }

    @Test
    @Incorrect
    fun `test call with next not returning a node`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.AccessLexemWithNextRequiresANode) {
            val variableName = "test"
            val functionName = "fn"
            val returnValue = 3
            val function =
                    "${FunctionNode.keyword} $functionName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} ${ControlWithExpressionStmtNode.returnKeyword} $returnValue${BlockStmtNode.endToken}"
            val lexeme =
                    "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $functionName${FunctionCallNode.startToken}${FunctionCallNode.endToken} ${AccessLexemeNode.nextAccessToken} $functionName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
            val grammar =
                    "${BlockStmtNode.startToken}$function \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
            val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                    isDescriptiveCode = true)

            // Prepare context.
            val context = AnalyzerCommons.getCurrentContext(analyzer.memory)
            context.setProperty(analyzer.memory, variableName, LxmNil)

            TestUtils.processAndCheckEmpty(analyzer)
        }
    }

    @Test
    fun `test call node - filter`() {
        val node2ProcessVar = "node2ProcessVar"
        val variableName = "test"
        val filterName = "filter"
        val nodeName = "nodeName"
        val pattern =
                "${LexemePatternNode.patternToken}${LexemePatternNode.staticTypeToken} ${FilterLexemeNode.startToken}$nodeName${FilterLexemeNode.endToken}"
        val filter =
                "${FilterStmtNode.keyword} $filterName${FunctionParameterListNode.startToken}${FunctionParameterListNode.endToken}${BlockStmtNode.startToken} $pattern ${BlockStmtNode.endToken}"
        val lexeme =
                "$variableName ${AnyLexemeNode.dataCapturingRelationalToken} $node2ProcessVar ${AccessLexemeNode.nextAccessToken} $filterName${FunctionCallNode.startToken}${FunctionCallNode.endToken}"
        val grammar =
                "${BlockStmtNode.startToken}$filter \n ${LexemePatternNode.patternToken} $lexeme${BlockStmtNode.endToken}"
        val analyzer = TestUtils.createAnalyzerFrom(grammar, parserFunction = BlockStmtNode.Companion::parse,
                isDescriptiveCode = true)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val parent = LxmNode("processedNode", analyzer.text.saveCursor(), null, analyzer.memory)
        val parentRef = analyzer.memory.add(parent)
        val children = parent.getChildren(analyzer.memory)
        val childNode = LxmNode(nodeName, analyzer.text.saveCursor(), parentRef, analyzer.memory)
        val childNodeRef = analyzer.memory.add(childNode)
        children.addCell(analyzer.memory, childNodeRef, ignoreConstant = true)

        context.setProperty(analyzer.memory, variableName, LxmNil)
        context.setProperty(analyzer.memory, node2ProcessVar, parentRef)

        TestUtils.processAndCheckEmpty(analyzer, bigNodeCount = 2)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val resultRef = context.getPropertyValue(analyzer.memory, variableName) as LxmReference
        val result = resultRef.dereference(analyzer.memory) as? LxmNode ?: throw Error("The result must be a LxmNode")
        val resultParent = result.getParent(analyzer.memory)!!
        val resultParentChildren = resultParent.getChildrenAsList(analyzer.memory)

        Assertions.assertEquals(parent.name, result.name, "The name property is incorrect")
        Assertions.assertEquals(0, result.getFrom(analyzer.memory).primitive.position(),
                "The from property is incorrect")
        Assertions.assertEquals(0, result.getTo(analyzer.memory)!!.primitive.position(), "The to property is incorrect")
        Assertions.assertEquals(AnalyzerCommons.Identifiers.Root, resultParent.name, "The parent is incorrect")
        Assertions.assertEquals(1, resultParentChildren.size, "The parent children count is incorrect")
        Assertions.assertEquals(resultRef.position, (resultParentChildren[0] as LxmReference).position,
                "The parent child[0] is incorrect")

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        // Remove the circular references of the nodes.
        childNodeRef.dereferenceAs<LxmNode>(analyzer.memory)!!.setProperty(analyzer.memory,
                AnalyzerCommons.Identifiers.Parent, LxmNil, ignoringConstant = true)

        removeNode(analyzer)

        TestUtils.checkEmptyStackAndContext(analyzer, listOf(variableName, node2ProcessVar))
    }

    // AUXILIARY METHODS ------------------------------------------------------

    /**
     * Checks all the results of the header.
     */
    private fun removeNode(analyzer: LexemAnalyzer) {
        val context = AnalyzerCommons.getCurrentContext(analyzer.memory)
        val lxmNodeRef = context.getPropertyValue(analyzer.memory, AnalyzerCommons.Identifiers.HiddenLastResultNode)!!
        val lxmNode = lxmNodeRef.dereference(analyzer.memory) as LxmNode
        val children = lxmNode.getChildren(analyzer.memory)

        for (i in children.listSize - 1 downTo 0) {
            children.removeCell(analyzer.memory, i, ignoreConstant = true)
        }
    }
}
