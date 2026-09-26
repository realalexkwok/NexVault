package com.nexvault.wallet.domain.model

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.flatMap
import com.nexvault.wallet.domain.model.common.getOrNull
import com.nexvault.wallet.domain.model.common.getOrThrow
import com.nexvault.wallet.domain.model.common.map
import com.nexvault.wallet.domain.model.common.toDataResult
import org.junit.Assert.*
import org.junit.Test

class DataResultTest {
    @Test
    fun testSuccessContainsData() {
        val result: DataResult<String> = DataResult.Success("test")
        assertTrue(result is DataResult.Success)
        assertEquals("test", (result as DataResult.Success).data)
    }

    @Test
    fun testErrorContainsExceptionAndMessage() {
        val exception = RuntimeException("test error")
        val result: DataResult<String> = DataResult.Error(exception, "custom message")
        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertEquals(exception, error.exception)
        assertEquals("custom message", error.message)
    }

    @Test
    fun testMapOnSuccessTransformsData() {
        val result: DataResult<Int> = DataResult.Success(5)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is DataResult.Success)
        assertEquals(10, (mapped as DataResult.Success).data)
    }

    @Test
    fun testMapOnErrorPassesThrough() {
        val exception = RuntimeException("error")
        val result: DataResult<Int> = DataResult.Error(exception)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is DataResult.Error)
    }

    @Test
    fun testFlatMapOnSuccessChainsCorrectly() {
        val result: DataResult<Int> = DataResult.Success(5)
        val flatMapped = result.flatMap { DataResult.Success(it * 2) }
        assertTrue(flatMapped is DataResult.Success)
        assertEquals(10, (flatMapped as DataResult.Success).data)
    }

    @Test
    fun testFlatMapOnErrorPassesThrough() {
        val exception = RuntimeException("error")
        val result: DataResult<Int> = DataResult.Error(exception)
        val flatMapped = result.flatMap { DataResult.Success(it * 2) }
        assertTrue(flatMapped is DataResult.Error)
    }

    @Test
    fun testGetOrNullReturnsDataForSuccess() {
        val result: DataResult<String> = DataResult.Success("test")
        assertEquals("test", result.getOrNull())
    }

    @Test
    fun testGetOrNullReturnsNullForError() {
        val result: DataResult<String> = DataResult.Error(RuntimeException("error"))
        assertNull(result.getOrNull())
    }

    @Test
    fun testGetOrThrowReturnsDataForSuccess() {
        val result: DataResult<String> = DataResult.Success("test")
        assertEquals("test", result.getOrThrow())
    }

    @Test
    fun testGetOrThrowThrowsForError() {
        val exception = RuntimeException("error")
        val result: DataResult<String> = DataResult.Error(exception)
        try {
            result.getOrThrow()
            fail("Expected exception")
        } catch (e: RuntimeException) {
            assertEquals(exception, e)
        }
    }

    @Test
    fun testToDataResultConvertsSuccess() {
        val kotlinResult: Result<String> = Result.success("test")
        val dataResult = kotlinResult.toDataResult()
        assertTrue(dataResult is DataResult.Success)
        assertEquals("test", (dataResult as DataResult.Success).data)
    }

    @Test
    fun testToDataResultConvertsFailure() {
        val exception = RuntimeException("error")
        val kotlinResult: Result<String> = Result.failure(exception)
        val dataResult = kotlinResult.toDataResult()
        assertTrue(dataResult is DataResult.Error)
        assertEquals(exception, (dataResult as DataResult.Error).exception)
    }
}
