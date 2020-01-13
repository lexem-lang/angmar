package org.lexem.angmar

import es.jtp.kterm.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.analyzer.memory.*
import org.lexem.angmar.analyzer.nodes.*
import org.lexem.angmar.analyzer.stdlib.*
import org.lexem.angmar.analyzer.stdlib.globals.*
import org.lexem.angmar.compiler.*
import org.lexem.angmar.compiler.others.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.*
import org.lexem.angmar.io.readers.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.utils.*
import java.util.concurrent.*


/**
 * Analyzer for the Lexem language.
 */
class LexemAnalyzer internal constructor(internal val grammarRootNode: CompiledNode,
        grammars: Map<String, CompiledNode>? = null) {
    var text: IReader = IOStringReader.from("")
        internal set
    internal var rootFilePath = ""
    internal val memory = LexemMemory()
    internal var processStatus = ProcessStatus.Forward
    internal var nextNode: CompiledNode? = null
    internal var signal = 0
    internal var backtrackingData: LxmBacktrackingData? = null
    internal var initialCursor: IReaderCursor = text.saveCursor()
    internal val importMode = if (grammars == null) {
        ImportMode.Normal
    } else {
        ImportMode.AllIn
    }

    var status = Status.Ended
        internal set
    var entryPoint = Consts.defaultEntryPoint
        internal set
    internal var ticks = 0L
        private set

    init {
        val stdLibContext = LxmContext(memory, LxmContext.LxmContextType.StdLib)
        val hiddenContext = LxmContext(memory, LxmContext.LxmContextType.StdLib)
        val stdLibContextReference = stdLibContext.getPrimitive()
        val hiddenContextReference = hiddenContext.getPrimitive()

        if (stdLibContextReference.position != LxmReference.StdLibContext.position || hiddenContextReference.position != LxmReference.HiddenContext.position) {
            // This must never happen.
            throw AngmarUnreachableException()
        }

        hiddenContext.setProperty(memory, AnalyzerCommons.Identifiers.HiddenCurrentContext, stdLibContext)

        val fileMap = LxmObject(memory)
        hiddenContext.setProperty(memory, AnalyzerCommons.Identifiers.HiddenFileMap, fileMap)

        if (importMode == ImportMode.AllIn) {
            val parserMap = LxmObject(memory)

            for ((name, grammarRootNode) in grammars!!) {
                parserMap.setProperty(memory, name, LxmGrammarRootNode(grammarRootNode))
            }

            hiddenContext.setProperty(memory, AnalyzerCommons.Identifiers.HiddenParserMap, parserMap)
        }

        StdlibCommons.initTypesAndPrototypes(memory)
        StdlibCommons.initGlobals(memory)
    }

    // METHODS ----------------------------------------------------------------

    /**
     * Starts a new analysis.
     */
    fun start(text: IReader, entryPoint: String? = null,
            timeoutInMilliseconds: Long = Consts.Analyzer.defaultTimeoutInMilliseconds): Boolean {
        // Init state
        ticks = 0L
        this.text = text
        memory.clear()

        // Set the rollback code point.
        freezeMemoryCopy(grammarRootNode, signal)

        // Init the result node.
        initRootNode()

        processStatus = ProcessStatus.Forward

        nextNode = grammarRootNode
        signal = 0
        backtrackingData = null
        initialCursor = text.saveCursor()

        // Set the main file path.
        rootFilePath = grammarRootNode.parser.reader.getSource()

        // Set the entry point. It will be added to the context in AnalyzerCommons.createAndAssignNewModuleContext.
        this.entryPoint = entryPoint ?: Consts.defaultEntryPoint

        // Set the return code point.
        memory.addToStack(AnalyzerCommons.Identifiers.ReturnCodePoint, LxmNil)

        // Execute
        return resume(timeoutInMilliseconds)
    }

    /**
     * Resumes the analysis.
     */
    fun resume(timeoutInMilliseconds: Long = Consts.Analyzer.defaultTimeoutInMilliseconds): Boolean {
        status = Status.Executing
        val timeout = System.nanoTime() + timeoutInMilliseconds * 1000000
        var timeSpatialGC = 0.0

        val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        val gcChannel: Channel<Unit>? = Channel(Channel.CONFLATED)
        val analyzer = this

        try {
            runBlocking {
                val gcJob = if (gcChannel != null) {
                    memory.gcChannel = gcChannel
                    launch(dispatcher) {
                        try {
                            while (true) {
                                // Wait for a notification bigNode.
                                gcChannel.receive()

                                // Execute the remove.
                                memory.temporalGarbageCollector()
                            }
                        } catch (e: ClosedReceiveChannelException) {
                            if (Consts.verbose) {
                                Logger.debug("end garbage collector job") {
                                    showDate = true
                                }
                            }
                        }
                    }
                } else {
                    null
                }

                launch(dispatcher) {
                    loop@ while (true) {
                        // Check timeout.
                        val time = System.nanoTime()
                        if (!Consts.verbose && time >= timeout || status == Status.Paused) {
                            status = Status.Paused
                            return@launch
                        }

                        ticks += 1L

                        when (processStatus) {
                            ProcessStatus.Forward -> {
                                // No more nodes
                                if (nextNode == null) {
                                    break@loop
                                }

                                nextNode!!.analyze(analyzer, signal)

                                // Execute the garbage collector if the memory has asked for.
                                // Done here to prevent calling the garbage collector before
                                // set all the references.
                                if (memory.lastNode.startGarbageCollectorSync) {
                                    if (Consts.verbose) {
                                        Logger.debug(
                                                "init garbageCollect - heapSize: ${memory.lastNode.heapSize}, free(${memory.lastNode.heapFreedCells * 100.0 / memory.lastNode.heapSize}%): ${memory.lastNode.heapFreedCells}") {
                                            showDate = true
                                        }
                                        val gcTime = TimeUtils.measureTimeSeconds {
                                            memory.lastNode.garbageCollect()
                                        }
                                        timeSpatialGC += gcTime
                                        Logger.debug(
                                                "end  garbageCollect after ${gcTime}s - total: $timeSpatialGC - heapSize: ${memory.lastNode.heapSize}, free(${memory.lastNode.heapFreedCells * 100.0 / memory.lastNode.heapSize}%): ${memory.lastNode.heapFreedCells}") {
                                            showDate = true
                                        }
                                    } else {
                                        memory.lastNode.garbageCollect()
                                    }
                                }

                                if (Consts.verbose && ticks % 100000 == 0L) {
                                    val bigNodeCount = memory.lastNode.bigNodeSequence().count()
                                    Logger.debug(
                                            "tick $ticks - text at character ${text.currentPosition()} - heapSize: ${memory.lastNode.heapSize}, free(${memory.lastNode.heapFreedCells * 100.0 / memory.lastNode.heapSize}%): ${memory.lastNode.heapFreedCells}") {
                                        showDate = true
                                        addNote("BigNodes", bigNodeCount.toString())
                                    }
                                }
                            }
                            ProcessStatus.Backward -> {
                                val lastCodePoint = memory.rollbackCopy()
                                lastCodePoint.restore(analyzer)

                                // Handle the possibility of matching nothing.
                                if (memory.lastNode.previousNode == null) {
                                    signal = 0
                                    nextNode = null

                                    status = Status.Ended
                                    return@launch
                                }

                                processStatus = ProcessStatus.Forward
                            }
                        }
                    }
                }.join()

                // Close the channel.
                gcChannel?.close()

                // Wait for the garbage collector to end.
                gcJob?.join()
            }

            // Close the dispatcher.
            dispatcher.close()

            if (status == Status.Paused) {
                return false
            }

            if (status == Status.Ended) {
                return true
            }
        } catch (e: AngmarAnalyzerException) {
            // Close the channel.
            gcChannel?.close()

            // Close the dispatcher.
            dispatcher.close()

            // Print the stack traces
            val callHierarchy = AnalyzerCommons.getCallHierarchy(memory)

            if (callHierarchy.isNotEmpty()) {
                e.logger.apply {
                    setStack {
                        for (i in callHierarchy) {
                            if (i.callerNode is InternalFunctionCallCompiled) {
                                addStackTrace("<native code>") {
                                    methodName = i.callerContextName
                                }
                            } else {
                                val (line, column) = i.callerNode.from.lineColumn()
                                addStackTrace(i.callerNode.parser.reader.getSource(), line, column) {
                                    methodName = i.callerContextName
                                }
                            }
                        }
                    }

                    val txt = text
                    if (txt is ITextReader) {
                        addSourceCode(txt.readAllText(), txt.getSource()) {
                            highlightCursorAt(txt.currentPosition())
                            message = "Review at this point"
                        }
                    }
                }
            }

            if (Consts.verbose) {
                e.logger.cause = Logger("Debug error at $ticks ticks", e)
            }

            throw e
        } catch (e: Throwable) {
            // Close the channel.
            gcChannel?.close()

            // Close the dispatcher.
            dispatcher.close()

            if (Consts.verbose) {
                Logger.debug("Unexpected error at $ticks ticks", e)
            }

            throw e
        }

        // Finish the root node.
        val rootNode = getRootNode(toWrite = true)!!
        rootNode.setTo(memory, text.saveCursor())

        status = Status.Ended
        // TODO remove

        if (Consts.verbose) {
            Logger.debug("tick $ticks - text at character ${text.currentPosition()}") {
                showDate = true
                addNote("HeapSize", memory.lastNode.heapSize.toString())
                addNote("FreeCells", memory.lastNode.heapFreedCells.toString())
                addNote("MaxBigNodes", memory.nextId.toString())
            }

            memory.lastNode.garbageCollect()

            Logger.debug("after garbage collector") {
                showDate = true
                addNote("FreeCells", memory.lastNode.heapFreedCells.toString())
            }
        }
        return true
    }

    /**
     * Sets a different entry point.
     */
    fun setEntryPoint(entryPoint: String): LexemAnalyzer {
        if (status != Status.Ended) {
            throw AngmarException("Cannot modify a running Analyzer.")
        }

        val context = AnalyzerCommons.getCurrentContext(memory, toWrite = true)
        context.setProperty(memory, AnalyzerCommons.Identifiers.EntryPoint, LxmString.from(entryPoint))

        return this
    }

    /**
     * Gets the results of the analyzer.
     */
    fun getResult(): LexemMatch {
        if (status != Status.Ended) {
            throw AngmarException("Cannot get the results of a running Analyzer.")
        }

        // Return the correct result.
        val rootNode = getRootNode(toWrite = true) ?: LxmNode(memory, AnalyzerCommons.Identifiers.Root, initialCursor,
                LxmNode.LxmNodeType.Root)
        rootNode.setTo(memory, initialCursor)

        return LexemMatch(this, rootNode)
    }

    /**
     * Frees the resources of the analyzer.
     */
    fun freeResources() {
        if (status == Status.Ready) {
            return
        }

        // Init state
        memory.clear()

        // Set the status
        status = Status.Ready
    }

    /**
     * Sets the next node to execute.
     */
    internal fun nextNode(nextNode: CompiledNode?, signal: Int = AnalyzerNodesCommons.signalStart) {
        this.nextNode = nextNode
        this.signal = signal
    }

    /**
     * Sets the next node to execute.
     */
    internal fun nextNode(codePoint: LxmCodePoint) {
        nextNode = codePoint.node
        signal = codePoint.signal
    }

    /**
     * Inits the backtracking.
     */
    internal fun initBacktracking() {
        processStatus = ProcessStatus.Backward
    }

    /**
     * Freezes a new copy of the memory.
     */
    internal fun freezeMemoryCopy(node: CompiledNode, signal: Int) {
        val rollbackCodePoint = LxmRollbackCodePoint(node, signal, text.saveCursor())
        memory.freezeCopy(rollbackCodePoint)
    }

    /**
     * Restores a frozen memory copy.
     */
    internal fun restoreMemoryCopy(bigNode: BigNode) {
        val rollbackCodePoint = memory.restoreCopy(bigNode)
        rollbackCodePoint.restore(this)
    }

    /**
     * Gets the root [LxmNode] object.
     */
    private fun getRootNode(toWrite: Boolean): LxmNode? {
        val stdlibContext = AnalyzerCommons.getStdLibContext(memory, toWrite = false)
        val analyzerObject = stdlibContext.getDereferencedProperty<LxmObject>(memory, AnalyzerGlobalObject.ObjectName,
                toWrite = false)!!
        return analyzerObject.getDereferencedProperty(memory, AnalyzerGlobalObject.RootNode, toWrite)
    }

    /**
     * Creates the root node.
     */
    private fun initRootNode() {
        val nodeReference = createNewNode(AnalyzerCommons.Identifiers.Root, LxmNode.LxmNodeType.Root)
        val stdlibContext = AnalyzerCommons.getStdLibContext(memory, toWrite = false)
        val analyzerObject = stdlibContext.getDereferencedProperty<LxmObject>(memory, AnalyzerGlobalObject.ObjectName,
                toWrite = true)!!
        analyzerObject.setProperty(memory, AnalyzerGlobalObject.RootNode, nodeReference)
    }

    /**
     * Creates a new node.
     */
    internal fun createNewNode(name: String, type: LxmNode.LxmNodeType): LxmNode {
        val hiddenContext = AnalyzerCommons.getHiddenContext(memory, toWrite = true)
        val parent =
                hiddenContext.getDereferencedProperty<LxmNode>(memory, AnalyzerCommons.Identifiers.HiddenLastResultNode,
                        toWrite = true)

        val node = LxmNode(memory, name, text.saveCursor(), type)
        parent?.addChild(memory, node)

        hiddenContext.setProperty(memory, AnalyzerCommons.Identifiers.HiddenLastResultNode, node)

        return node
    }

    /**
     * Sets the upper node as current one.
     */
    internal fun setUpperNode() {
        val hiddenContext = AnalyzerCommons.getHiddenContext(memory, toWrite = true)
        val node =
                hiddenContext.getDereferencedProperty<LxmNode>(memory, AnalyzerCommons.Identifiers.HiddenLastResultNode,
                        toWrite = false)!!
        val parentRef = node.getParentReference(memory)!!

        hiddenContext.setProperty(memory, AnalyzerCommons.Identifiers.HiddenLastResultNode, parentRef)
    }

    /**
     * Gets whether the analyzer is running forward.
     */
    internal fun isForward(): Boolean {
        val props = AnalyzerCommons.getCurrentNodeProps(memory, toWrite = false)
        val reverse = props.getPropertyValue(memory, AnalyzerCommons.Properties.Reverse) ?: LxmNil
        val reverseLogic = RelationalFunctions.isTruthy(reverse)

        return !reverseLogic
    }

    // STATIC -----------------------------------------------------------------

    /**
     * Possible status of a [LexemAnalyzer].
     */
    enum class Status {
        Ready,
        Paused,
        Executing,
        Ended
    }

    /**
     * The status of the analysis.
     */
    internal enum class ProcessStatus {
        Forward,
        Backward
    }

    /**
     * The possible modes of import.
     */
    internal enum class ImportMode {
        /**
         * Imports everything.
         */
        Normal,

        /**
         * Imports only the stored [LexemParser]s.
         */
        AllIn
    }

    companion object {
        /**
         * Creates a new [LexemAnalyzer] parsing the specified input.
         */
        fun createParsing(input: ITextReader): LexemAnalyzer? {
            val parser = LexemParser(input)
            val parserNode = LexemFileNode.parse(parser) ?: return null
            val compiledNode = LexemFileCompiled.compile(parserNode)

            return LexemAnalyzer(compiledNode)
        }

        /**
         * Creates a new [LexemAnalyzer] using a set of named inputs.
         */
        fun createFrom(inputs: Map<String, ITextReader>, mainParserName: String): LexemAnalyzer? {
            if (mainParserName !in inputs) {
                throw AngmarException("Undefined parser called '$mainParserName'")
            }

            val grammars = inputs.mapValues { (key, input) ->
                val parser = LexemParser(input)
                val parserNode = LexemFileNode.parse(parser) ?: throw AngmarException(
                        "The '$key' input is not a correct Lexem file.")
                LexemFileCompiled.compile(parserNode)
            }

            val mainGrammarNode = grammars[mainParserName]!!
            return LexemAnalyzer(mainGrammarNode, grammars)
        }
    }
}
