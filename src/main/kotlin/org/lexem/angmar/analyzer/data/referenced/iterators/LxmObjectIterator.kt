package org.lexem.angmar.analyzer.data.referenced.iterators

import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.data.referenced.*
import org.lexem.angmar.analyzer.memory.*

/**
 * The Lexem value of an Object iterator.
 */
internal class LxmObjectIterator : LexemIterator {
    private val keys: List<String>

    // CONSTRUCTORS -----------------------------------------------------------

    constructor(memory: LexemMemory, obj: LxmObject) : super(memory) {
        setProperty(memory, AnalyzerCommons.Identifiers.Value, obj.getPrimitive())

        keys = obj.getAllIterableProperties().map { it.key }
        intervalSize = keys.size.toLong()
    }

    private constructor(memory: LexemMemory, oldVersion: LxmObjectIterator) : super(memory, oldVersion) {
        keys = oldVersion.keys
        intervalSize = oldVersion.intervalSize
    }

    // METHODS ----------------------------------------------------------------

    /**
     * Gets the iterator's object.
     */
    private fun getObject(memory: LexemMemory) =
            getDereferencedProperty<LxmObject>(memory, AnalyzerCommons.Identifiers.Value, toWrite = false)!!

    // OVERRIDE METHODS -------------------------------------------------------

    override val intervalSize: Long

    override fun getCurrent(memory: LexemMemory): Pair<LexemPrimitive?, LexemPrimitive>? {
        if (isEnded(memory)) {
            return null
        }

        val index = getIndex(memory)
        val value = getObject(memory)
        val currentKey = keys[index.primitive]
        val currentValue = value.getPropertyValue(memory, currentKey) ?: LxmNil
        return Pair(LxmString.from(currentKey), currentValue)
    }

    override fun memoryShift(memory: LexemMemory) = LxmObjectIterator(memory, oldVersion = this)

    override fun toString() = "[Iterator - Object] ${super.toString()}"
}
