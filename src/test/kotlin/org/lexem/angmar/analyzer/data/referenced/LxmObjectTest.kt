package org.lexem.angmar.analyzer.data.referenced

import org.junit.jupiter.api.*
import org.lexem.angmar.*
import org.lexem.angmar.analyzer.*
import org.lexem.angmar.analyzer.data.primitives.*
import org.lexem.angmar.analyzer.stdlib.types.*
import org.lexem.angmar.errors.*
import org.lexem.angmar.parser.*
import org.lexem.angmar.utils.*

internal class LxmObjectTest {
    @Test
    fun `test constructors`() {
        val memory = TestUtils.generateTestMemory()

        val old = LxmObject(memory)

        Assertions.assertNull(old.oldVersion, "The oldObject property is incorrect")
        Assertions.assertNull(old.prototypeReference, "The prototypeReference property is incorrect")

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!

        Assertions.assertEquals(old, new.oldVersion, "The oldObject property is incorrect")
        Assertions.assertNull(new.prototypeReference, "The prototypeReference property is incorrect")

        val obj = LxmObject(memory, new)

        Assertions.assertEquals(new.getPrimitive(), obj.prototypeReference,
                "The prototypeReference property is incorrect")
    }

    @Test
    fun `test constructors with prototype`() {
        val memory = TestUtils.generateTestMemory()

        val old = LxmObject(memory)
        val oldCell = memory.lastNode.getCell(memory, old.getPrimitive().position)

        Assertions.assertNull(old.oldVersion, "The oldObject property is incorrect")
        Assertions.assertNull(old.prototypeReference, "The prototypeReference property is incorrect")
        Assertions.assertEquals(0, oldCell.referenceCount, "The referenceCount property is incorrect")

        val new1 = LxmObject(memory, old)
        val new1Cell = memory.lastNode.getCell(memory, new1.getPrimitive().position)

        Assertions.assertNull(new1.oldVersion, "The oldObject property is incorrect")
        Assertions.assertEquals(old.getPrimitive(), new1.prototypeReference,
                "The prototypeReference property is incorrect")
        Assertions.assertEquals(0, new1Cell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(1, oldCell.referenceCount, "The referenceCount property is incorrect")
    }

    @Test
    fun `test get property`() {
        val memory = TestUtils.generateTestMemory()

        val prototype = LxmObject(memory)
        prototype.setProperty(memory, "prototype", LxmLogic.False)

        val old = LxmObject(memory, prototype)
        old.setProperty(memory, "old", LxmLogic.True)

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.setProperty(memory, "new", LxmInteger.Num1)

        Assertions.assertEquals(LxmLogic.True, new.getPropertyValue(memory, "old"), "The new property is incorrect")
        Assertions.assertEquals(LxmLogic.False, new.getPropertyValue(memory, "prototype"),
                "The prototype property is incorrect")
        Assertions.assertEquals(LxmInteger.Num1, new.getPropertyValue(memory, "new"), "The old property is incorrect")
    }

    @Test
    fun `test get removed property`() {
        val propName = "test"
        val memory = TestUtils.generateTestMemory()

        val old = LxmObject(memory)
        old.setProperty(memory, propName, LxmLogic.True)

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.removeProperty(memory, propName)

        val result = new.getPropertyValue(memory, propName)
        Assertions.assertNull(result, "The result is incorrect")
    }

    @Test
    fun `test get undefined property`() {
        val propName = "test"
        val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
        val memory = analyzer.memory

        val obj = LxmObject(memory)
        val result = obj.getPropertyValue(memory, propName)
        Assertions.assertNull(result, "The result is incorrect")
    }

    @Test
    fun `test get property descriptor`() {
        val memory = TestUtils.generateTestMemory()

        val prototype = LxmObject(memory)
        prototype.setProperty(memory, "prototype", LxmLogic.False)

        val old = LxmObject(memory, prototype)
        old.setProperty(memory, "old", LxmLogic.True)

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.setProperty(memory, "new", LxmInteger.Num1)

        val oldDescriptor = new.getOwnPropertyDescriptor(memory, "old") ?: throw Error("The old property is incorrect")
        val newDescriptor = new.getOwnPropertyDescriptor(memory, "new") ?: throw Error("The new property is incorrect")

        Assertions.assertEquals(LxmInteger.Num1, newDescriptor.value, "The new property is incorrect")
        Assertions.assertNull(new.getOwnPropertyDescriptor(memory, "prototype"), "The prototype property is incorrect")
        Assertions.assertEquals(LxmLogic.True, oldDescriptor.value, "The old property is incorrect")
    }

    @Test
    fun `test get dereferenced property`() {
        val memory = TestUtils.generateTestMemory()

        val other = LxmObject(memory)

        val obj = LxmObject(memory)
        obj.setProperty(memory, "a", LxmLogic.False)
        obj.setProperty(memory, "b", other)

        val derefPrimitive = obj.getDereferencedProperty<LxmLogic>(memory, "a", toWrite = false)
        val derefOther = obj.getDereferencedProperty<LxmObject>(memory, "b", toWrite = false)

        Assertions.assertEquals(LxmLogic.False, derefPrimitive, "The a property is incorrect")
        Assertions.assertEquals(other, derefOther, "The b property is incorrect")

        Assertions.assertNull(obj.getDereferencedProperty<LxmObject>(memory, "a", toWrite = false),
                "The a property is incorrect")
        Assertions.assertNull(obj.getDereferencedProperty<LxmLogic>(memory, "b", toWrite = false),
                "The b property is incorrect")
    }

    @Test
    fun `test set property`() {
        val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
        val memory = analyzer.memory

        val prototype = LxmObject(memory)
        prototype.setProperty(memory, "prototype", LxmLogic.False)

        val old = LxmObject(memory, prototype)
        old.setProperty(memory, "old", LxmLogic.True)
        Assertions.assertEquals(LxmLogic.True, old.getPropertyValue(memory, "old"), "The old property is incorrect")

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.setProperty(memory, "old", LxmInteger.Num1)

        Assertions.assertEquals(LxmLogic.True, old.getPropertyValue(memory, "old"), "The old property is incorrect")
        Assertions.assertEquals(LxmInteger.Num1, new.getPropertyValue(memory, "old"), "The old property is incorrect")
        Assertions.assertNull(prototype.getPropertyValue(memory, "old"), "The old property is incorrect")

        new.setProperty(memory, "old", LxmInteger.Num10)

        Assertions.assertEquals(LxmLogic.True, old.getPropertyValue(memory, "old"), "The old property is incorrect")
        Assertions.assertEquals(LxmInteger.Num10, new.getPropertyValue(memory, "old"), "The old property is incorrect")
        Assertions.assertNull(prototype.getPropertyValue(memory, "old"), "The old property is incorrect")

        new.setProperty(memory, "prototype", LxmInteger.Num1)

        Assertions.assertEquals(LxmInteger.Num1, new.getPropertyValue(memory, "prototype"),
                "The prototype property is incorrect")
        Assertions.assertEquals(LxmLogic.False, prototype.getPropertyValue(memory, "prototype"),
                "The prototype property is incorrect")
    }

    @Test
    fun `test set property updating references`() {
        val prop1Name = "test1"
        val prop2Name = "test2"
        val memory = TestUtils.generateTestMemory()

        val obj1 = LxmObject(memory)
        obj1.getPrimitive().increaseReferences(memory)
        val obj1Cell = memory.lastNode.getCell(memory, obj1.getPrimitive().position)

        val obj2 = LxmObject(memory)
        obj2.getPrimitive().increaseReferences(memory)
        val obj2Cell = memory.lastNode.getCell(memory, obj2.getPrimitive().position)

        val obj = LxmObject(memory)
        obj.getPrimitive().increaseReferences(memory)
        val objCell = memory.lastNode.getCell(memory, obj.getPrimitive().position)

        Assertions.assertEquals(1, obj1Cell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(1, obj2Cell.referenceCount, "The referenceCount property is incorrect")

        obj.setProperty(memory, prop1Name, LxmLogic.True)
        obj.setProperty(memory, prop2Name, obj1)

        Assertions.assertEquals(2, obj1Cell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(1, obj2Cell.referenceCount, "The referenceCount property is incorrect")

        obj.setProperty(memory, prop1Name, obj2)

        Assertions.assertEquals(2, obj1Cell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(2, obj2Cell.referenceCount, "The referenceCount property is incorrect")

        obj.setProperty(memory, prop2Name, LxmLogic.True)

        Assertions.assertEquals(1, obj1Cell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(2, obj2Cell.referenceCount, "The referenceCount property is incorrect")

        obj.setProperty(memory, prop1Name, obj1)

        Assertions.assertEquals(2, obj1Cell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(1, obj2Cell.referenceCount, "The referenceCount property is incorrect")

        obj.setProperty(memory, prop1Name, obj1)

        Assertions.assertEquals(2, obj1Cell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(1, obj2Cell.referenceCount, "The referenceCount property is incorrect")
    }

    @Test
    fun `test set property as context`() {
        val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
        val memory = analyzer.memory

        var prototype = LxmObject(memory)
        prototype.setPropertyAsContext(memory, "prototype", LxmLogic.False)

        val old = LxmObject(memory, prototype)
        old.setPropertyAsContext(memory, "old", LxmLogic.True)

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.setPropertyAsContext(memory, "old", LxmInteger.Num1)

        Assertions.assertEquals(LxmLogic.True, old.getPropertyValue(memory, "old"), "The old property is incorrect")
        Assertions.assertEquals(LxmInteger.Num1, new.getPropertyValue(memory, "old"), "The old property is incorrect")
        Assertions.assertNull(prototype.getPropertyValue(memory, "old"), "The old property is incorrect")

        new.setPropertyAsContext(memory, "old", LxmInteger.Num10)

        Assertions.assertEquals(LxmLogic.True, old.getPropertyValue(memory, "old"), "The old property is incorrect")
        Assertions.assertEquals(LxmInteger.Num10, new.getPropertyValue(memory, "old"), "The old property is incorrect")
        Assertions.assertNull(prototype.getPropertyValue(memory, "old"), "The old property is incorrect")

        new.setPropertyAsContext(memory, "prototype", LxmInteger.Num1)
        prototype = prototype.getPrimitive().dereferenceAs(memory, toWrite = false)!!

        Assertions.assertEquals(LxmInteger.Num1, new.getPropertyValue(memory, "prototype"),
                "The prototype property is incorrect")
        Assertions.assertEquals(LxmInteger.Num1, prototype.getPropertyValue(memory, "prototype"),
                "The prototype property is incorrect")
    }

    @Test
    fun `test set property ignoring constant`() {
        val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
        val memory = analyzer.memory
        val propName = "test"

        val obj = LxmObject(memory)
        obj.makeConstant(memory)

        obj.setProperty(memory, propName, LxmInteger.Num10, ignoringConstant = true)

        Assertions.assertEquals(LxmInteger.Num10, obj.getPropertyValue(memory, propName),
                "The $propName property is incorrect")
    }

    @Test
    fun `test set property changing the inner attributes`() {
        val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
        val memory = analyzer.memory
        val propName = "test"

        val obj = LxmObject(memory)
        obj.setProperty(memory, propName, LxmInteger.Num10)

        var cell = obj.getOwnPropertyDescriptor(memory, propName)!!

        Assertions.assertFalse(cell.isConstant, "The isConstant property is incorrect")
        Assertions.assertTrue(cell.isIterable, "The isIterable property is incorrect")
        Assertions.assertEquals(LxmInteger.Num10, obj.getPropertyValue(memory, propName),
                "The $propName property is incorrect")

        obj.setProperty(memory, propName, LxmLogic.True, isIterable = false, isConstant = true)

        cell = obj.getOwnPropertyDescriptor(memory, propName)!!

        Assertions.assertTrue(cell.isConstant, "The isConstant property is incorrect")
        Assertions.assertFalse(cell.isIterable, "The isIterable property is incorrect")
        Assertions.assertEquals(LxmLogic.True, obj.getPropertyValue(memory, propName),
                "The $propName property is incorrect")
    }

    @Test
    fun `test set property changing the inner attributes recursively`() {
        val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
        val memory = analyzer.memory
        val propName = "test"

        val old = LxmObject(memory)
        old.setProperty(memory, propName, LxmInteger.Num10)

        var cellOld = old.getOwnPropertyDescriptor(memory, propName)!!

        Assertions.assertFalse(cellOld.isConstant, "The isConstant property is incorrect")
        Assertions.assertTrue(cellOld.isIterable, "The isIterable property is incorrect")
        Assertions.assertEquals(LxmInteger.Num10, old.getPropertyValue(memory, propName),
                "The $propName property is incorrect")

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.setProperty(memory, propName, LxmLogic.True, isIterable = false, isConstant = true)

        cellOld = old.getOwnPropertyDescriptor(memory, propName)!!
        val cellNew = new.getOwnPropertyDescriptor(memory, propName)!!

        Assertions.assertFalse(cellOld.isConstant, "The isConstant property is incorrect")
        Assertions.assertTrue(cellOld.isIterable, "The isIterable property is incorrect")
        Assertions.assertEquals(LxmInteger.Num10, old.getPropertyValue(memory, propName),
                "The $propName property is incorrect")
        Assertions.assertTrue(cellNew.isConstant, "The isConstant property is incorrect")
        Assertions.assertFalse(cellNew.isIterable, "The isIterable property is incorrect")
        Assertions.assertEquals(LxmLogic.True, new.getPropertyValue(memory, propName),
                "The $propName property is incorrect")
    }

    @Test
    fun `test contains own property`() {
        val memory = TestUtils.generateTestMemory()

        val prototype = LxmObject(memory)
        prototype.setProperty(memory, "prototype", LxmLogic.False)

        val old = LxmObject(memory, prototype)
        old.setProperty(memory, "old", LxmLogic.True)

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.setProperty(memory, "new", LxmLogic.False)

        Assertions.assertTrue(new.containsOwnProperty(memory, "old"), "The old property is incorrect")
        Assertions.assertTrue(new.containsOwnProperty(memory, "new"), "The old property is incorrect")
        Assertions.assertFalse(new.containsOwnProperty(memory, "prototype"), "The old property is incorrect")
    }

    @Test
    fun `test contains own removed property`() {
        val memory = TestUtils.generateTestMemory()

        val prototype = LxmObject(memory)

        val old = LxmObject(memory, prototype)
        old.setProperty(memory, "old", LxmLogic.True)

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.removeProperty(memory, "old")

        Assertions.assertFalse(new.containsOwnProperty(memory, "old"), "The old property is incorrect")
    }

    @Test
    fun `test remove property from current`() {
        val propName = "property"
        val memory = TestUtils.generateTestMemory()

        val obj = LxmObject(memory)
        obj.setProperty(memory, propName, LxmLogic.True)

        obj.removeProperty(memory, propName)

        Assertions.assertNull(obj.getOwnPropertyDescriptor(memory, propName), "The $propName property mustn't be got")
    }

    @Test
    fun `test remove property existing in old`() {
        val propName = "property"
        val memory = TestUtils.generateTestMemory()

        val prototype = LxmObject(memory)
        prototype.setProperty(memory, propName, LxmLogic.False)

        val old = LxmObject(memory, prototype)
        old.setProperty(memory, propName, LxmLogic.True)

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.setProperty(memory, propName, LxmInteger.Num10)

        new.removeProperty(memory, propName)

        Assertions.assertEquals(LxmLogic.True, old.getOwnPropertyDescriptor(memory, propName)?.value,
                "The $propName property is incorrect")
        Assertions.assertEquals(LxmLogic.False, prototype.getOwnPropertyDescriptor(memory, propName)?.value,
                "The $propName property is incorrect")

        val descriptor =
                new.getOwnPropertyDescriptor(memory, propName) ?: throw Error("The $propName property is incorrect")
        Assertions.assertFalse(descriptor.isIterable, "The isIterable property is incorrect")
        Assertions.assertFalse(descriptor.isConstant, "The isConstant property is incorrect")
        Assertions.assertEquals(LxmNil, descriptor.value, "The value property is incorrect")
    }

    @Test
    fun `test make constant`() {
        val memory = TestUtils.generateTestMemory()

        val obj = LxmObject(memory)
        Assertions.assertFalse(obj.isConstant, "The isConstant property is incorrect")

        obj.makeConstant(memory)
        Assertions.assertTrue(obj.isConstant, "The isConstant property is incorrect")
    }

    @Test
    fun `test make property constant`() {
        val propName = "test"
        val memory = TestUtils.generateTestMemory()

        val obj = LxmObject(memory)
        obj.setProperty(memory, propName, LxmLogic.True)

        val descriptorPre = obj.getOwnPropertyDescriptor(memory, propName)!!
        Assertions.assertFalse(descriptorPre.isConstant, "The isConstant property is incorrect")
        Assertions.assertTrue(descriptorPre.isIterable, "The isIterable property is incorrect")
        Assertions.assertFalse(descriptorPre.isRemoved, "The isRemoved property is incorrect")
        Assertions.assertEquals(LxmLogic.True, descriptorPre.value, "The value property is incorrect")

        obj.makePropertyConstant(memory, propName)
        val descriptorPost = obj.getOwnPropertyDescriptor(memory, propName)!!
        Assertions.assertTrue(descriptorPost.isConstant, "The isConstant property is incorrect")
        Assertions.assertTrue(descriptorPost.isIterable, "The isIterable property is incorrect")
        Assertions.assertFalse(descriptorPost.isRemoved, "The isRemoved property is incorrect")
        Assertions.assertEquals(LxmLogic.True, descriptorPost.value, "The value property is incorrect")
    }

    @Test
    fun `test get all iterable properties`() {
        val oldPropsCount = 1
        val protoPropsCount = 3
        val newPropsCount = 6
        val memory = TestUtils.generateTestMemory()

        val prototype = LxmObject(memory)
        for (i in 0 until protoPropsCount) {
            prototype.setProperty(memory, "prototype$i", LxmLogic.False)
        }

        val old = LxmObject(memory, prototype)
        for (i in 0 until oldPropsCount) {
            old.setProperty(memory, "old$i", LxmLogic.True)
        }

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        for (i in 0 until newPropsCount) {
            new.setProperty(memory, "new$i", LxmInteger.Num1)
        }

        val properties = new.getAllIterableProperties()

        Assertions.assertEquals(newPropsCount + oldPropsCount, properties.size, "The number of properties is incorrect")

        for (i in properties) {
            when {
                i.key.startsWith("new") -> Assertions.assertEquals(LxmInteger.Num1, i.value.value,
                        "The new property is incorrect")
                i.key.startsWith("old") -> Assertions.assertEquals(LxmLogic.True, i.value.value,
                        "The old property is incorrect")
                else -> throw Error("The key property is incorrect")
            }
        }
    }

    @Test
    fun `test clone`() {
        val memory = TestUtils.generateTestMemory()

        val old = LxmObject(memory)
        val oldConst = LxmObject(memory)
        oldConst.makeConstant(memory)

        memory.freezeCopy()

        val cloned = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        val clonedConst = oldConst.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!

        Assertions.assertEquals(old, cloned.oldVersion, "The oldObject is incorrect")
        Assertions.assertTrue(clonedConst.isConstant, "The isConstant is incorrect")
        Assertions.assertNotEquals(oldConst, clonedConst, "The clonedConst is incorrect")
    }

    @Test
    fun `test clone - non-writable`() {
        val memory = TestUtils.generateTestMemory()

        val old = LxmObject(memory)
        old.makeConstantAndNotWritable(memory)

        memory.freezeCopy()

        val cloned = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!

        Assertions.assertEquals(old, cloned, "The cloned is incorrect")
    }

    @Test
    fun `test memory dealloc`() {
        val memory = TestUtils.generateTestMemory()

        val obj = LxmObject(memory)
        obj.getPrimitive().increaseReferences(memory)
        val objCell = memory.lastNode.getCell(memory, obj.getPrimitive().position)

        val prototype = LxmObject(memory)
        prototype.getPrimitive().increaseReferences(memory)
        val prototypeCell = memory.lastNode.getCell(memory, prototype.getPrimitive().position)

        val old = LxmObject(memory, prototype)
        old.setProperty(memory, "test-old", LxmLogic.True)
        old.setProperty(memory, "testObj-old", obj)
        old.getPrimitive().increaseReferences(memory)
        val oldCell = memory.lastNode.getCell(memory, old.getPrimitive().position)

        memory.freezeCopy()

        val new = old.getPrimitive().dereferenceAs<LxmObject>(memory, toWrite = true)!!
        new.setProperty(memory, "test-new", LxmLogic.False)
        new.setProperty(memory, "testObj-new", obj)
        val newCell = memory.lastNode.getCell(memory, new.getPrimitive().position)
        val objCellNew = memory.lastNode.getCell(memory, obj.getPrimitive().position)

        Assertions.assertEquals(oldCell.referenceCount, newCell.referenceCount,
                "The referenceCount property is incorrect")
        Assertions.assertEquals(2, objCell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(3, objCellNew.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(1, oldCell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(2, prototypeCell.referenceCount, "The referenceCount property is incorrect")

        new.memoryDealloc(memory)

        val prototypeCellNew = memory.lastNode.getCell(memory, prototype.getPrimitive().position)

        Assertions.assertEquals(1, newCell.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(1, objCellNew.referenceCount, "The referenceCount property is incorrect")
        Assertions.assertEquals(1, prototypeCellNew.referenceCount, "The referenceCount property is incorrect")
    }

    @Test
    fun `test memory dealloc - non-writable`() {
        val memory = TestUtils.generateTestMemory()

        val obj1 = LxmObject(memory)
        val obj2 = LxmObject(memory)

        obj1.setProperty(memory, "test", obj2)
        obj1.makeConstantAndNotWritable(memory)
        obj1.getPrimitive().increaseReferences(memory)

        obj1.memoryDealloc(memory)

        Assertions.assertTrue(obj2.getPrimitive().getCell(memory).isFreed, "The object has not been dealloc")
        Assertions.assertEquals(obj2.getPrimitive(), obj1.getPropertyValue(memory, "test"),
                "The reference property is incorrect")
    }

    @Test
    fun `test get type`() {
        val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
        val memory = analyzer.memory

        val obj = LxmObject(memory)
        val type = obj.getType(memory)
        val context = AnalyzerCommons.getCurrentContext(memory, toWrite = false)
        val objectType = context.getPropertyValue(memory, ObjectType.TypeName)!!
        Assertions.assertEquals(objectType, type, "The type is incorrect")
    }

    @Test
    fun `test get prototype`() {
        val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
        val memory = analyzer.memory

        val prototype1 = LxmObject(memory)

        val obj1 = LxmObject(memory, prototype1)
        val result1 = obj1.getPrototype(memory)
        Assertions.assertEquals(prototype1.getPrimitive(), result1, "The result is incorrect")

        val obj2 = LxmObject(memory)
        val result2 = obj2.getPrototype(memory)
        val context = AnalyzerCommons.getCurrentContext(memory, toWrite = false)
        val objectType = context.getDereferencedProperty<LxmObject>(memory, ObjectType.TypeName, toWrite = false)!!
        val prototype = objectType.getPropertyValue(memory, AnalyzerCommons.Identifiers.Prototype)!!
        Assertions.assertEquals(prototype, result2, "The result is incorrect")
    }

    @Test
    @Incorrect
    fun `set property in a constant object`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.CannotModifyAConstantObject) {
            val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
            val memory = analyzer.memory
            val obj = LxmObject(memory)

            obj.makeConstant(memory)

            val testName = "test"
            val testValue = LxmLogic.True

            obj.setProperty(memory, testName, testValue)
        }
    }

    @Test
    @Incorrect
    fun `set a constant property`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.CannotModifyAConstantObjectProperty) {
            val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
            val memory = analyzer.memory
            val obj = LxmObject(memory)

            val testName = "test"
            val testValue = LxmLogic.True

            obj.setProperty(memory, testName, testValue, isConstant = true)

            obj.setProperty(memory, testName, testValue)
        }
    }

    @Test
    @Incorrect
    fun `remove property in a constant object`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.CannotModifyAConstantObject) {
            val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
            val memory = analyzer.memory
            val obj = LxmObject(memory)

            obj.makeConstant(memory)

            val testName = "test"

            obj.removeProperty(memory, testName)
        }
    }

    @Test
    @Incorrect
    fun `remove a constant property`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.CannotModifyAConstantObjectProperty) {
            val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
            val memory = analyzer.memory
            val obj = LxmObject(memory)
            val testName = "test"
            val testValue = LxmLogic.True

            obj.setProperty(memory, testName, testValue, isConstant = true)

            obj.removeProperty(memory, testName)
        }
    }

    @Test
    @Incorrect
    fun `make an undefined property constant`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.UndefinedObjectProperty) {
            val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
            val memory = analyzer.memory
            val obj = LxmObject(memory)

            obj.makePropertyConstant(memory, "test")
        }
    }

    @Test
    @Incorrect
    fun `make a property constant in a constant object`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.CannotModifyAConstantObject) {
            val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
            val memory = analyzer.memory
            val obj = LxmObject(memory)

            obj.makeConstant(memory)
            obj.makePropertyConstant(memory, "test")
        }
    }

    @Test
    @Incorrect
    fun `set a property in a non-writable object`() {
        TestUtils.assertAnalyzerException(AngmarAnalyzerExceptionType.CannotModifyANonWritableObject) {
            val analyzer = LexemAnalyzer(ParserNode.Companion.EmptyParserNode)
            val memory = analyzer.memory
            val obj = LxmObject(memory)

            obj.makeConstantAndNotWritable(memory)
            obj.setProperty(memory, "test", LxmNil, ignoringConstant = true)
        }
    }
}
