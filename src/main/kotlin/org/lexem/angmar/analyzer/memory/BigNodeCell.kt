package org.lexem.angmar.analyzer.memory

import org.lexem.angmar.analyzer.data.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*
import java.util.*

/**
 * The representation of a memory cell.
 */
internal class BigNodeCell private constructor(position: Int, value: LexemReferenced?) {
    var position = position
        private set
    var referenceCount = 0
        private set
    var value: LexemReferenced? = value
        private set

    // PROPERTIES -------------------------------------------------------------

    val isFreed get() = value == null

    // METHODS ----------------------------------------------------------------

    /**
     * Destroys the cell to be reused.
     * MUST ONLY BE CALLED WHENEVER THE CELL IS REMOVED FROM MEMORY.
     */
    fun destroy() {
        position = -1
        referenceCount = 0
        value = null

        if (instances.size < Consts.Memory.maxPoolSize) {
            instances.add(this)
        }
    }

    /**
     * Shifts the memory cell to a forward [BigNode].
     */
    fun shiftCell(memory: LexemMemory): BigNodeCell {
        val newCell = new(position, value?.memoryShift(memory))
        newCell.referenceCount = referenceCount

        return newCell
    }

    /**
     * Undoes the free operation over the cell setting a new value. CAN ONLY BE USED IN [BigNode].
     */
    fun reallocCell(memory: LexemMemory, value: LexemReferenced) {
        referenceCount = 0
        this.value?.memoryDealloc(memory)
        this.value = value
    }

    /**
     * Frees the cell clearing all its fields but the reference which points to the next free cell.
     */
    fun freeCell(memory: LexemMemory) {
        if (isFreed) {
            // Avoid to throw errors when in garbage collection mode.
            if (memory.isInGarbageCollectionMode) {
                return
            }

            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.HeapSegmentationFault,
                    "Cannot access to a freed memory segment") {}
        }

        if (referenceCount > 0 && !memory.isInGarbageCollectionMode) {
            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.ReferencedHeapCellFreed,
                    "Cannot free a referenced memory cell") {}
        }

        val value = value!!
        this.value = null
        value.memoryDealloc(memory)
        referenceCount = memory.lastNode.lastFreePosition
    }

    /**
     * Increases the reference.
     */
    fun increaseReferences(count: Int = 1) {
        if (isFreed) {
            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.HeapSegmentationFault,
                    "Cannot access to a freed memory segment") {}
        }

        referenceCount += count
    }

    /**
     * Decreases the reference count freeing the cell if it reaches 0.
     */
    fun decreaseReferences(memory: LexemMemory, count: Int = 1) {
        if (isFreed) {
            // Avoid to free cells during garbage collector.
            if (memory.isInGarbageCollectionMode) {
                return
            }

            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.HeapSegmentationFault,
                    "Cannot access to a freed memory segment") {}
        }

        referenceCount -= count

        if (referenceCount < 0) {
            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.ReferenceCountUnderflow,
                    "A cell in the memory has an underflow reference count") {}
        }

        if (referenceCount == 0) {
            memory.lastNode.free(memory, position)
        }
    }

    // OVERRIDE METHODS -------------------------------------------------------

    override fun toString() = when {
        referenceCount < 0 -> "<REMOVED>"
        isFreed -> "[$position] Free -> $referenceCount"
        else -> "[$position] Refs<$referenceCount> = $value"
    }

    // STATIC -----------------------------------------------------------------

    companion object {
        private val instances = Stack<BigNodeCell>()

        /**
         * Creates a new [BigNodeCell] for a specified position with a default value value.
         */
        fun new(position: Int, value: LexemReferenced?): BigNodeCell {
            val instance = if (instances.size > 0) {
                instances.pop()!!
            } else {
                BigNodeCell(position, null)
            }

            instance.position = position
            instance.value = value
            instance.referenceCount = 0

            return instance
        }
    }
}

