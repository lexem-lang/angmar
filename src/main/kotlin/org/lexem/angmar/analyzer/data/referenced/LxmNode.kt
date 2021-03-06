package org.lexem.angmar.analyzer.data.referenced

import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.memory.*
import org.lexem.angmar.analyzer.stdlib.types.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.io.*

/**
 * The Lexem values of the Node type.
 */
internal class LxmNode : LxmObject {
    val name: String

    // CONSTRUCTORS -----------------------------------------------------------

    // Only for the clone.
    private constructor(oldNode: LxmNode) : super(oldNode) {
        this.name = oldNode.name
    }

    constructor(name: String, from: IReaderCursor, parent: LxmReference?, memory: LexemMemory) {
        this.name = name

        if (parent != null) {
            setProperty(memory, AnalyzerCommons.Identifiers.Parent, parent, isConstant = true)
        }

        setProperty(memory, AnalyzerCommons.Identifiers.Name, LxmString.from(name), isConstant = true)
        setProperty(memory, AnalyzerCommons.Identifiers.From, LxmReaderCursor(from), isConstant = true)

        init(memory)
    }

    // METHODS ----------------------------------------------------------------

    /**
     * Adds the initial properties.
     */
    private fun init(memory: LexemMemory) {
        val children = LxmList()
        children.makeConstant(memory)
        setProperty(memory, AnalyzerCommons.Identifiers.Children, memory.add(children), isConstant = true)

        val properties = LxmObject()
        setProperty(memory, AnalyzerCommons.Identifiers.Properties, memory.add(properties), isConstant = true)
    }

    /**
     * Gets the parent node reference.
     */
    fun getParentReference(memory: LexemMemory) =
            getPropertyValue(memory, AnalyzerCommons.Identifiers.Parent) as? LxmReference

    /**
     * Gets the parent node.
     */
    fun getParent(memory: LexemMemory) = getDereferencedProperty<LxmNode>(memory, AnalyzerCommons.Identifiers.Parent)

    /**
     * Sets the parent node.
     */
    fun setParent(memory: LexemMemory, parent: LxmReference) =
            setProperty(memory, AnalyzerCommons.Identifiers.Parent, parent, isConstant = true, ignoringConstant = true)

    /**
     * Gets the children reference.
     */
    fun getChildrenReference(memory: LexemMemory) =
            getPropertyValue(memory, AnalyzerCommons.Identifiers.Children) as LxmReference

    /**
     * Gets the children.
     */
    fun getChildren(memory: LexemMemory) =
            getDereferencedProperty<LxmList>(memory, AnalyzerCommons.Identifiers.Children)!!

    /**
     * Gets the children property value as a list.
     */
    fun getChildrenAsList(memory: LexemMemory) = getChildren(memory).getAllCells()

    /**
     * Gets the content of the node.
     */
    fun getContent(memory: LexemMemory): LexemPrimitive? {
        val from = getFrom(memory).primitive
        val to = getTo(memory)?.primitive ?: return null
        val reader = to.getReader()

        return AnalyzerCommons.substringReader(reader, from, to)
    }

    /**
     * Gets the property object.
     */
    fun getProperties(memory: LexemMemory) =
            getDereferencedProperty<LxmObject>(memory, AnalyzerCommons.Identifiers.Properties)!!

    /**
     * Gets the initial position of the content of the node.
     */
    fun getFrom(memory: LexemMemory) =
            getDereferencedProperty<LxmReaderCursor>(memory, AnalyzerCommons.Identifiers.From)!!

    /**
     * Gets the final position of the content of the node.
     */
    fun getTo(memory: LexemMemory) = getDereferencedProperty<LxmReaderCursor>(memory, AnalyzerCommons.Identifiers.To)

    /**
     * Sets the value of the to property.
     */
    fun setTo(memory: LexemMemory, cursor: IReaderCursor) =
            setProperty(memory, AnalyzerCommons.Identifiers.To, LxmReaderCursor(cursor))

    /**
     * Applies the default properties for expressions.
     */
    fun applyDefaultPropertiesForExpression(memory: LexemMemory) {
        val props = getProperties(memory)
        props.setProperty(memory, AnalyzerCommons.Properties.Capture, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Children, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Consume, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Property, LxmLogic.False)
        props.setProperty(memory, AnalyzerCommons.Properties.Insensible, LxmLogic.False)
        props.setProperty(memory, AnalyzerCommons.Properties.Backtrack, LxmLogic.False)
        props.setProperty(memory, AnalyzerCommons.Properties.Reverse, LxmLogic.False)
    }

    /**
     * Applies the default properties for filters.
     */
    fun applyDefaultPropertiesForFilter(memory: LexemMemory) {
        val props = getProperties(memory)
        props.setProperty(memory, AnalyzerCommons.Properties.Capture, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Children, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Backtrack, LxmLogic.False)
        props.setProperty(memory, AnalyzerCommons.Properties.Reverse, LxmLogic.False)
    }

    /**
     * Applies the default properties for groups.
     */
    fun applyDefaultPropertiesForGroup(memory: LexemMemory) {
        val props = getProperties(memory)
        props.setProperty(memory, AnalyzerCommons.Properties.Children, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Backtrack, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Consume, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Capture, LxmLogic.False)
        props.setProperty(memory, AnalyzerCommons.Properties.Property, LxmLogic.False)
        props.setProperty(memory, AnalyzerCommons.Properties.Insensible, LxmLogic.False)
        props.setProperty(memory, AnalyzerCommons.Properties.Reverse, LxmLogic.False)
    }

    /**
     * Applies the default properties for filter groups.
     */
    fun applyDefaultPropertiesForFilterGroup(memory: LexemMemory) {
        val props = getProperties(memory)
        props.setProperty(memory, AnalyzerCommons.Properties.Backtrack, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Consume, LxmLogic.True)
        props.setProperty(memory, AnalyzerCommons.Properties.Reverse, LxmLogic.False)
    }

    /**
     * Applies an offset to the current node and its children.
     */
    fun applyOffset(memory: LexemMemory, offset: IReaderCursor) {
        val newReader = offset.getReader()
        val currentCursor = newReader.saveCursor()
        var prevCursor = getFrom(memory).primitive

        // Update from.
        setProperty(memory, AnalyzerCommons.Identifiers.From, LxmReaderCursor(offset), isConstant = true,
                ignoringConstant = true)
        offset.restore()

        // Update children.
        for (child in getChildrenAsList(memory)) {
            val childNode = child.dereference(memory) as LxmNode

            // Calculate and set the offset.
            val difference = childNode.getFrom(memory).primitive.position() - prevCursor.position()
            newReader.advance(difference)

            // Save the end of the current child.
            prevCursor = childNode.getTo(memory)!!.primitive

            childNode.applyOffsetWithoutRestoring(memory, newReader)
        }

        // Update to.
        val difference = getTo(memory)!!.primitive.position() - prevCursor.position()
        newReader.advance(difference)
        setTo(memory, newReader.saveCursor())

        // Restore the position of the reader.
        currentCursor.restore()
    }

    private fun applyOffsetWithoutRestoring(memory: LexemMemory, reader: IReader) {
        var prevCursor = getFrom(memory).primitive

        // Update from.
        setProperty(memory, AnalyzerCommons.Identifiers.From, LxmReaderCursor(reader.saveCursor()), isConstant = true,
                ignoringConstant = true)

        // Update children.
        for (child in getChildrenAsList(memory)) {
            val childNode = child.dereference(memory) as LxmNode

            // Calculate and set the offset.
            val difference = childNode.getFrom(memory).primitive.position() - prevCursor.position()
            reader.advance(difference)

            // Save the end of the current child.
            prevCursor = childNode.getTo(memory)!!.primitive

            childNode.applyOffsetWithoutRestoring(memory, reader)
        }

        // Update to.
        val difference = getTo(memory)!!.primitive.position() - prevCursor.position()
        reader.advance(difference)
        setTo(memory, reader.saveCursor())
    }

    /**
     * De-allocates the current branch, removing the node from its parent.
     */
    fun memoryDeallocBranch(memory: LexemMemory) {
        // Remove children.
        for (child in getChildrenAsList(memory).map { it.dereference(memory) as LxmNode }) {
            child.memoryDeallocBranchRecursively(memory)
        }

        // Remove from parent.
        val parent = getParent(memory)
        if (parent != null) {
            val index = parent.getChildrenAsList(memory).map { it.dereference(memory) as LxmNode }.indexOf(this)
            if (index < 0) {
                throw AngmarUnreachableException()
            }

            parent.getChildren(memory).removeCell(memory, index)
        }

        // Dealloc the current one.
        memoryDealloc(memory)
    }

    private fun memoryDeallocBranchRecursively(memory: LexemMemory) {
        // Remove children.
        for (child in getChildrenAsList(memory).map { it.dereference(memory) as LxmNode }) {
            child.memoryDeallocBranchRecursively(memory)
        }

        // Dealloc the current one.
        memoryDealloc(memory)
    }

    // OVERRIDE METHODS -------------------------------------------------------

    override fun clone() = LxmNode(this)

    override fun getType(memory: LexemMemory) =
            AnalyzerCommons.getStdLibContextElement<LxmObject>(memory, NodeType.TypeName)

    override fun toString() = "[NODE] $name = ${super.toString()}"
}
