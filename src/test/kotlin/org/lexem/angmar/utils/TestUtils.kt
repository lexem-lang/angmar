package org.lexem.angmar.utils

import org.junit.jupiter.api.*
import org.lexem.angmar.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.memory.*
import org.lexem.angmar.analyzer.stdlib.*
import org.lexem.angmar.commands.*
import org.lexem.angmar.compiler.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.*
import org.lexem.angmar.io.readers.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.parser.functional.expressions.*
import java.io.*
import java.nio.file.*

internal object TestUtils {
    /**
     * Freezes a copy in the memory.
     */
    fun freezeCopy(memory: LexemMemory) {
        memory.freezeCopy(
                LxmRollbackCodePoint(CompiledNode.Companion.EmptyCompiledNode, 0, IOStringReader.from("").saveCursor()))
    }

    /**
     * Calls Angmar with the given arguments and returns the result of the console.
     */
    fun callAngmar(args: Array<String>, block: (stdOut: String, errOut: String) -> Unit) {
        val oriOut = System.out
        val oriErrOut = System.err
        val myOut = ByteArrayOutputStream()
        val myErrorOut = ByteArrayOutputStream()

        System.setOut(PrintStream(myOut))
        System.setErr(PrintStream(myErrorOut))

        try {
            AngmarCommand().main(args)
        } catch (e: SecurityException) {
        }

        System.setOut(oriOut)
        System.setErr(oriErrOut)

        val standardOutput = myOut.toString().trim()
        val errorOutput = myErrorOut.toString().trim()

        block(standardOutput, errorOutput)
    }

    /**
     * Handles the logs generated by a block of code.
     */
    fun handleLogs(block: () -> Unit): Pair<String, String> {
        val oriOut = System.out
        val oriErrOut = System.err
        val myOut = ByteArrayOutputStream()
        val myErrorOut = ByteArrayOutputStream()

        System.setOut(PrintStream(myOut))
        System.setErr(PrintStream(myErrorOut))

        try {
            block()
        } catch (e: SecurityException) {
        }

        System.setOut(oriOut)
        System.setErr(oriErrOut)

        val standardOutput = myOut.toString().trim()
        val errorOutput = myErrorOut.toString().trim()

        return Pair(standardOutput, errorOutput)
    }

    /**
     * Ensures an [AngmarException] or throws an error.
     */
    internal inline fun assertAngmarException(print: Boolean = true, fn: () -> Unit) {
        try {
            fn()
            throw Exception("This method should throw an AngmarAnalyzerException")
        } catch (e: AngmarException) {
            if (print) {
                e.logMessage()
            }
        }
    }

    /**
     * Ensures an [AngmarParserException] or throws an error.
     */
    inline fun assertParserException(type: AngmarParserExceptionType?, fn: () -> Unit) {
        try {
            fn()
            throw Exception("This method should throw an AngmarParserException")
        } catch (e: AngmarParserException) {
            if (type != null && e.type != type) {
                throw AngmarException("The expected AngmarParserException is $type but actually it is ${e.type}", e)
            }

            e.logMessage()
        }
    }

    /**
     * Ensures an [AngmarAnalyzerException] or throws an error.
     */
    inline fun assertAnalyzerException(type: AngmarAnalyzerExceptionType?, print: Boolean = true, fn: () -> Unit) {
        try {
            fn()
            throw Exception("This method should throw an AngmarAnalyzerException")
        } catch (e: AngmarAnalyzerException) {
            if (type != null && e.type != type) {
                throw AngmarException("The expected AngmarAnalyzerException was $type but it is ${e.type}", e)
            }

            if (print) {
                e.logMessage()
            }
        }
    }

    /**
     * Ensures an [AngmarCompilerException] or throws an error.
     */
    inline fun assertCompilerException(type: AngmarCompilerExceptionType?, print: Boolean = true, fn: () -> Unit) {
        try {
            fn()
            throw Exception("This method should throw an assertCompilerException")
        } catch (e: AngmarCompilerException) {
            if (type != null && e.type != type) {
                throw AngmarException("The expected assertCompilerException was $type but it is ${e.type}", e)
            }

            if (print) {
                e.logMessage()
            }
        }
    }

    /**
     * Ensures an [AngmarAnalyzerException] that raise the [AngmarAnalyzerExceptionType.TestControlSignalRaised] type.
     */
    inline fun assertControlSignalRaisedCheckingStack(analyzer: LexemAnalyzer, control: String, tagName: String?,
            value: LexemPrimitive?, fn: () -> Unit) {
        try {
            fn()
            throw Exception("This method should throw an AngmarAnalyzerException")
        } catch (e: AngmarAnalyzerException) {
            if (e.type != AngmarAnalyzerExceptionType.TestControlSignalRaised) {
                throw e
            }

            // Check stack.
            val controlValue = analyzer.memory.getFromStack(AnalyzerCommons.Identifiers.Control) as LxmControl
            Assertions.assertEquals(control, controlValue.type, "The type property is incorrect")
            Assertions.assertEquals(tagName, controlValue.tag, "The tag property is incorrect")
            Assertions.assertEquals(value, controlValue.value, "The value property is incorrect")

            analyzer.memory.removeFromStack(AnalyzerCommons.Identifiers.Control)
        }
    }

    /**
     * Creates a correct [LexemMemory].
     */
    fun generateTestMemory() = LexemMemory().apply { freezeCopy(this) }

    /**
     * Creates a correct [LexemMemory] from an analyzer.
     */
    fun generateTestMemoryFromAnalyzer() = LexemAnalyzer(CompiledNode.Companion.EmptyCompiledNode).memory

    /**
     * Creates an analyzer from the specified parameter.
     */
    fun createAnalyzerFrom(grammarText: String, isDescriptiveCode: Boolean = false, isFilterCode: Boolean = false,
            source: String = "", parserFunction: (LexemParser, ParserNode) -> ParserNode?): LexemAnalyzer {
        val parser = LexemParser(IOStringReader.from(grammarText, source))
        parser.isDescriptiveCode = isDescriptiveCode || isFilterCode
        parser.isFilterCode = isFilterCode
        val grammar = parserFunction(parser, ParserNode.Companion.EmptyParserNode)

        Assertions.assertNotNull(grammar, "The grammar cannot be null")

        val compiledGrammar = grammar!!.compile(CompiledNode.Companion.EmptyCompiledNode, 0)

        return LexemAnalyzer(compiledGrammar)
    }

    /**
     * Creates an analyzer from the specified parameter.
     */
    fun createAnalyzerFromWholeGrammar(grammarText: String, source: String = ""): LexemAnalyzer {
        val parser = LexemParser(IOStringReader.from(grammarText, source))
        val grammar = LexemFileNode.parse(parser)

        Assertions.assertNotNull(grammar, "The grammar cannot be null")

        val compiledGrammar = LexemFileCompiled.compile(grammar!!)

        return LexemAnalyzer(compiledGrammar)
    }

    /**
     * Creates an analyzer from the specified file.
     */
    fun createAnalyzerFromFile(file: File): LexemAnalyzer {
        val parser = LexemParser(IOStringReader.from(file))
        val grammar = LexemFileNode.parse(parser)

        Assertions.assertNotNull(grammar, "The grammar cannot be null")

        val compiledGrammar = LexemFileCompiled.compile(grammar!!)

        return LexemAnalyzer(compiledGrammar)
    }

    /**
     * Executes the analyzer and checks its results are empty.
     */
    fun processAndCheckEmpty(analyzer: LexemAnalyzer, text: IReader = IOStringReader.from(""),
            status: LexemAnalyzer.ProcessStatus = LexemAnalyzer.ProcessStatus.Forward,
            hasBacktrackingData: Boolean = false, bigNodeCount: Int = 1, entryPoint: String? = null) {
        val result = analyzer.start(text, entryPoint, timeoutInMilliseconds = 5 * 60 * 1000 /* 5 minutes */)

        // Assert status of the analyzer.
        Assertions.assertEquals(status, analyzer.processStatus, "The status is incorrect")
        Assertions.assertNull(analyzer.nextNode, "The next node must be null")
        Assertions.assertEquals(0, analyzer.signal, "The signal is incorrect")

        if (!hasBacktrackingData) {
            Assertions.assertNull(analyzer.backtrackingData, "The backtrackingData must be null")
        } else {
            Assertions.assertNotNull(analyzer.backtrackingData, "The backtrackingData cannot be null")
        }

        // Assert status of the result.
        Assertions.assertTrue(result, "The result must be true")

        // Check the memory has only n big nodes apart from the stdlib.
        var node = analyzer.memory.lastNode
        for (i in 0 until bigNodeCount) {
            Assertions.assertNotNull(node.previousNode, "The memory has lower than $bigNodeCount big nodes")
            node = node.previousNode!!
        }

        Assertions.assertNull(node.previousNode, "The memory has more than $bigNodeCount big nodes")
    }

    /**
     * Creates a temporary file.
     */
    fun createTempFile(fileName: String, text: String): File {
        val directory = System.getProperty("java.io.tmpdir")!!
        val file = Paths.get(directory, fileName).toAbsolutePath().toFile()

        file.writeText(text)

        return file
    }

    /**
     * Handles the creation and removal of a temporary file.
     */
    fun handleTempFiles(content: Map<String, String>, function: (Map<String, File>) -> Unit) {
        val files = content.mapValues { createTempFile(it.key, it.value) }
        try {
            function(files)
            files.forEach { it.value.delete() }
        } catch (e: Throwable) {
            files.forEach { it.value.delete() }

            throw e
        }
    }

    /**
     * Checks that the stack is empty and the context is the standard one.
     */
    fun checkEmptyStackAndContext(analyzer: LexemAnalyzer, valuesToRemove: List<String>? = null) {
        val memory = analyzer.memory

        // Remove from the stack.
        try {
            memory.removeFromStack(AnalyzerCommons.Identifiers.ReturnCodePoint)
        } catch (e: AngmarAnalyzerException) {
        }

        // Check stack.
        if (memory.lastNode.stackSize != 0) {
            throw Exception("The stack must be empty. It contains ${memory.lastNode.stackSize} remaining elements.")
        }

        // Check context.
        val stdLibContext = AnalyzerCommons.getStdLibContext(memory, toWrite = false)
        val hiddenContext = AnalyzerCommons.getHiddenContext(memory, toWrite = false)
        val context = AnalyzerCommons.getCurrentContext(memory, toWrite = false)

        Assertions.assertEquals(stdLibContext, context, "The context must be the stdLib")

        // Remove elements from the context.
        StdlibCommons.GlobalNames.forEach {
            context.removeProperty(analyzer.memory, it, ignoreConstant = true)
        }

        valuesToRemove?.forEach {
            if (context.getPropertyValue(analyzer.memory, it) == null) {
                throw Exception("The context does not contains the property called '$it'")
            }

            context.removeProperty(analyzer.memory, it, ignoreConstant = true)
        }

        hiddenContext.removeProperty(analyzer.memory, AnalyzerCommons.Identifiers.HiddenFileMap, ignoreConstant = true)
        hiddenContext.removeProperty(analyzer.memory, AnalyzerCommons.Identifiers.HiddenCurrentContext,
                ignoreConstant = true)
        hiddenContext.removeProperty(analyzer.memory, AnalyzerCommons.Identifiers.HiddenLastResultNode,
                ignoreConstant = true)

        if (analyzer.importMode == LexemAnalyzer.ImportMode.AllIn) {
            hiddenContext.removeProperty(analyzer.memory, AnalyzerCommons.Identifiers.HiddenParserMap,
                    ignoreConstant = true)
        }

        // Check whether the context is empty.
        Assertions.assertEquals(0, stdLibContext.size, "The stdLibContext is not empty: $context")
        Assertions.assertEquals(0, hiddenContext.size, "The hiddenContext is not empty: $context")
        Assertions.assertEquals(0, context.size, "The context is not empty: $context")

        // Remove the stdlib and hidden context.
        LxmReference.StdLibContext.decreaseReferences(memory.lastNode)
        LxmReference.HiddenContext.decreaseReferences(memory.lastNode)

        // Check whether the memory is empty.
        Assertions.assertEquals(0, memory.lastNode.heapSize,
                "The memory must be completely cleared. Remaining cells with values: ${memory.lastNode.heapSize}")
    }

    /**
     * Checks that the stack is empty and the context is the standard one.
     */
    fun e2eTestExecutingExpression(functionCall: String, preFunctionCall: String = "", postFunctionCall: String = "",
            initialVars: Map<String, LexemPrimitive> = emptyMap(),
            checkFunction: (LexemAnalyzer, LexemPrimitive?) -> Unit) {
        val varName = "test"
        val grammar =
                "$preFunctionCall \n $varName ${AssignOperatorNode.assignOperator} $functionCall \n $postFunctionCall"
        val analyzer = createAnalyzerFromWholeGrammar(grammar)

        // Prepare context.
        var context = AnalyzerCommons.getCurrentContext(analyzer.memory, toWrite = true)
        context.setProperty(analyzer.memory, varName, LxmNil)

        for ((name, value) in initialVars) {
            context.setProperty(analyzer.memory, name, value)
        }

        processAndCheckEmpty(analyzer)

        context = AnalyzerCommons.getCurrentContext(analyzer.memory, toWrite = false)
        val result = context.getPropertyValue(analyzer.memory, varName)

        checkFunction(analyzer, result)

        // Remove the function cyclic reference.
        analyzer.memory.spatialGarbageCollect()

        checkEmptyStackAndContext(analyzer, listOf(varName) + initialVars.keys)
    }
}
