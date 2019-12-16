package org.lexem.angmar.analyzer.memory

import org.lexem.angmar.analyzer.data.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.config.*
import org.lexem.angmar.errors.*

/**
 * A big node that represents an differential view of the memory.
 */
internal class BigNode constructor(var previousNode: BigNode?, var nextNode: BigNode?) {
    var garbageThreshold = Consts.Memory.spatialGarbageCollectorInitialThreshold
        private set
    private val stackLevels = mutableMapOf<Int, BigNodeStackLevel>()
    private val heap = mutableMapOf<Int, BigNodeCell>()
    var garbageCollectorMark = false
        private set

    /**
     * The number of levels in the current [BigNode]'s stack.
     */
    val stackLevelSize get() = stackLevels.size

    /**
     * The number of elements in the current [BigNode]'s stack.
     */
    val stackSize get() = stackLevels.values.sumBy { it.cellCount }

    /**
     * The number of levels in the whole stack.
     */
    var actualStackLevelSize: Int = previousNode?.actualStackLevelSize ?: 0
        private set

    /**
     * The number of elements in the whole stack.
     */
    var actualStackSize: Int = previousNode?.actualStackSize ?: 0
        private set

    /**
     * The number of cells in the current [BigNode]'s heap.
     */
    val heapSize get() = heap.size

    /**
     * The number of cells in the whole heap.
     */
    var actualHeapSize: Int = previousNode?.actualHeapSize ?: 0
        private set

    /**
     * The number of stored elements in the whole heap.
     */
    var actualUsedCellCount: Int = previousNode?.actualUsedCellCount ?: 0
        private set

    /**
     * The position of the last empty cell that can be used to hold new information.
     * Used to avoid fragmentation.
     */
    var lastFreePosition: Int = previousNode?.lastFreePosition ?: actualHeapSize
        private set

    /**
     * Gets the free space percentage.
     */
    val freeSpacePercentage get() = 100 - (actualUsedCellCount * 100.0 / maxOf(actualHeapSize, garbageThreshold))

    // METHODS ----------------------------------------------------------------

    /**
     * Adds a new value into the stack by a name.
     */
    fun addToStack(name: String, value: LexemPrimitive, memory: LexemMemory) {
        // Increase the reference count of the incoming value.
        value.increaseReferences(memory)

        // Get the last stack level.
        var level = getStackLevelRecursively(actualStackLevelSize - 1) ?: let {
            val level = BigNodeStackLevel.new(0)
            stackLevels[0] = level
            actualStackLevelSize += 1
            level
        }

        // Increase a level if name is inside.
        level = if (level.hasCell(name)) {
            val nextLevel = BigNodeStackLevel.new(actualStackLevelSize)
            stackLevels[actualStackLevelSize] = nextLevel
            actualStackLevelSize += 1
            nextLevel
        } else {
            // Shifts the stack level.
            if (level.position !in stackLevels) {
                level = level.shiftLevel()
                stackLevels[level.position] = level
            }

            level
        }

        // Add the value to the stack.
        level.setCellValue(name, value)
        actualStackSize += 1
    }

    /**
     * Gets the specified value of the stack.
     */
    fun getFromStack(name: String): LexemPrimitive {
        for (i in actualStackLevelSize - 1 downTo 0) {
            val level = getStackLevelRecursively(i)!!
            val value = level.getCellValue(name)
            if (value != null) {
                return value
            }
        }

        return previousNode?.getFromStack(name) ?: throw AngmarAnalyzerException(
                AngmarAnalyzerExceptionType.StackNotFoundElement, "Not found element called '$name' in the stack.") {}
    }

    /**
     * Removes the specified value of the stack recursively.
     */
    fun removeFromStack(name: String, memory: LexemMemory) {
        // Get the stack level.
        var level: BigNodeStackLevel? = null
        var value: LexemPrimitive? = null
        for (i in actualStackLevelSize - 1 downTo 0) {
            level = getStackLevelRecursively(i)!!
            value = level.getCellValue(name)
            if (value != null) {
                // Shifts the level.
                level = level.shiftLevel()
                stackLevels[i] = level
                break
            }
        }

        if (level == null || value == null) {
            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.StackNotFoundElement,
                    "Not found element called '$name' in the stack.") {}
        }

        // Shift the level to the current big node.
        if (level.position !in stackLevels) {
            level = level.shiftLevel()
            stackLevels[level.position] = level
        }

        // Remove cell.
        level.removeCell(name)
        actualStackSize -= 1

        // Remove the level if it is empty and it is the last one.
        if (level.cellCount == 0 && level.position == actualStackLevelSize - 1) {
            stackLevels.remove(level.position)
            actualStackLevelSize -= 1
            level.destroy()

            // Remove empty stack levels.
            for (i in actualStackLevelSize - 1 downTo 0) {
                val level = getStackLevelRecursively(i)!!
                if (level.cellCount != 0) {
                    break
                }

                if (i in stackLevels) {
                    stackLevels.remove(i)
                }

                actualStackLevelSize -= 1
                level.destroy()
            }
        }

        // Decrease reference count.
        value.decreaseReferences(memory)
    }

    /**
     * Replace the specified stack cell by another primitive.
     */
    fun replaceStackCell(name: String, newValue: LexemPrimitive, memory: LexemMemory) {
        // Get the stack level.
        var level: BigNodeStackLevel? = null
        var value: LexemPrimitive? = null
        for (i in actualStackLevelSize - 1 downTo 0) {
            level = getStackLevelRecursively(i)!!
            value = level.getCellValue(name)
            if (value != null) {
                break
            }
        }

        if (level == null || value == null) {
            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.StackNotFoundElement,
                    "Not found element called '$name' in the stack") {}
        }

        // Shift the level to the current big node.
        if (level.position !in stackLevels) {
            level = level.shiftLevel()
            stackLevels[level.position] = level
        }

        // Increase the reference count of the incoming value.
        if (newValue is LxmReference) {
            getCell(memory, newValue.position, forceShift = true).increaseReferences()
        }

        // Replace the cell.
        level.setCellValue(name, newValue)

        // Decrease reference count.
        value.decreaseReferences(memory)
    }

    /**
     * Gets the specified stack level.
     */
    private fun getStackLevelRecursively(position: Int): BigNodeStackLevel? =
            stackLevels[position] ?: previousNode?.getStackLevelRecursively(position)

    /**
     * Gets a cell recursively in the [BigNode]'s chain.
     */
    fun getCell(memory: LexemMemory, position: Int, forceShift: Boolean = false): BigNodeCell {
        if (position >= actualHeapSize) {
            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.HeapSegmentationFault,
                    "The analyzer is trying to access a forbidden memory position") {}
        }

        val res = heap[position]
        if (res != null) {
            return res
        }

        val (distance, cell) = previousNode?.getCellRecursive(position, 1) ?: throw AngmarUnreachableException()

        return if (forceShift || distance >= Consts.Memory.maxDistanceToShift) {
            val cell2 = cell.shiftCell(memory)
            heap[position] = cell2
            cell2
        } else {
            cell
        }
    }

    /**
     * Gets a value recursively without shifting the value in newer nodes.
     */
    private fun getCellRecursive(position: Int, distance: Int): Pair<Int, BigNodeCell>? {
        val value = heap[position] ?: return previousNode?.getCellRecursive(position, distance + 1)
        return Pair(distance, value)
    }

    /**
     * Sets a new value to the cell at the specified position.
     */
    fun setCell(memory: LexemMemory, position: Int, value: LexemReferenced) {
        if (position >= actualHeapSize) {
            throw AngmarAnalyzerException(AngmarAnalyzerExceptionType.HeapSegmentationFault,
                    "The analyzer is trying to access a forbidden memory position") {}
        }

        val cell = getCell(memory, position, forceShift = true)
        cell.setValue(value)
    }

    /**
     * Adds a new cell (or reuses a free one) to hold the specified value
     * returning the cell itself.
     */
    fun alloc(memory: LexemMemory, value: LexemReferenced): BigNodeCell {
        // No free cell.
        if (lastFreePosition == actualHeapSize) {
            // Execute the garbage collector to free space.
            if (actualHeapSize == garbageThreshold) {
                garbageCollectorMark = true
            }

            val cell = BigNodeCell.new(lastFreePosition, value)
            heap[lastFreePosition] = cell
            lastFreePosition += 1
            actualHeapSize += 1
            actualUsedCellCount += 1

            return cell
        }

        // Reuse a free cell.
        val cell = getCell(memory, lastFreePosition, forceShift = true)
        lastFreePosition = cell.referenceCount
        cell.reallocCell(memory, value)
        actualUsedCellCount += 1

        return cell
    }

    /**
     * Frees a memory cell to reuse it in the future.
     */
    fun free(memory: LexemMemory, position: Int) {
        var cell = getCell(memory, position)
        if (!cell.isFreed) {
            cell = getCell(memory, position, forceShift = true)
            cell.freeCell(memory)
            lastFreePosition = cell.position
            actualUsedCellCount -= 1
        }
    }

    /**
     * Collapses this [BigNode] to the specified one recursively
     */
    fun collapseTo(destination: BigNode) {
        if (destination == this) {
            return
        }

        // Sets the information.
        destination.actualStackLevelSize = actualStackLevelSize
        destination.actualStackSize = actualStackSize
        destination.actualHeapSize = actualHeapSize
        destination.actualUsedCellCount = actualUsedCellCount
        destination.lastFreePosition = lastFreePosition
        destination.garbageThreshold = garbageThreshold
        destination.garbageCollectorMark = garbageCollectorMark

        // Remove the excess levels in the stack.
        for (i in destination.stackLevels.keys) {
            if (i >= actualStackSize) {
                val level = destination.stackLevels[i]!!
                level.destroy()
                destination.stackLevels.remove(i)
            }
        }

        // Move stack elements backwards
        for (i in 0 until actualStackLevelSize) {
            var node: BigNode? = this
            while (node != null) {
                val level = node.stackLevels[i]
                if (level != null) {
                    destination.stackLevels[i] = level
                    node.stackLevels.remove(i)
                    break
                }

                node = node.previousNode
            }
        }

        collapseToRecursively(destination)
    }

    private fun collapseToRecursively(destination: BigNode) {
        // Move heap elements backwards.
        val iterator = heap.iterator()
        while (iterator.hasNext()) {
            val (i, cell) = iterator.next()
            var node = previousNode
            while (node != null) {
                if (i in node.heap) {
                    node.heap[i]!!.destroy()
                    node.heap.remove(i)
                }

                node = node.previousNode
            }

            destination.heap[i] = cell
            cell.value?.bigNode = destination
            iterator.remove()
        }

        if (previousNode == null) {
            destroy()
        } else {
            previousNode!!.collapseToRecursively(destination)
        }
    }

    /**
     * Clears the [BigNode] destroying its cells to reuse them.
     */
    fun destroy() {
        // Clears the stack.
        for ((_, level) in stackLevels) {
            level.destroy()
        }

        // Destroys all cells to reuse them.
        for ((_, cell) in heap) {
            cell.destroy()
        }

        actualStackSize = 0
        actualStackLevelSize = 0
        actualHeapSize = 0
        actualUsedCellCount = 0
        lastFreePosition = 0
        stackLevels.clear()
        heap.clear()
        previousNode = null
        nextNode?.destroy()
        nextNode = null
    }

    /**
     * Collects all the garbage of the current big node.
     */
    fun spatialGarbageCollect(memory: LexemMemory, forced: Boolean = false) {
        // Avoid to execute the garbage collector when there are enough free space.
        if (!forced && freeSpacePercentage >= Consts.Memory.minimumFreeSpace) {
            return
        }

        // Track from the main context.
        val stdLibCell = LxmReference.StdLibContext.getCell(memory)
        stdLibCell.spatialGarbageCollect(memory)

        // Track from stack.
        for (i in actualStackLevelSize - 1 downTo 0) {
            val level = getStackLevelRecursively(i)!!
            for ((_, cell) in level.cellValues) {
                cell.spatialGarbageCollect(memory)
            }
        }

        // Clean memory.
        for (i in 0 until actualHeapSize) {
            val cell = getCell(memory, i)

            if (!cell.isNotGarbage) {
                free(memory, i)
            } else {
                cell.clearGarbageFlag(memory)
            }
        }

        // Update the threshold only under the minimum quantity of free space.
        if (freeSpacePercentage < Consts.Memory.minimumFreeSpace) {
            garbageThreshold = (garbageThreshold * Consts.Memory.spatialGarbageCollectorThresholdIncrement).toInt()
        }

        garbageCollectorMark = false
    }
}
