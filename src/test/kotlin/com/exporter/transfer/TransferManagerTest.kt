package com.exporter.transfer

import com.exporter.device.DeviceConnector
import com.exporter.model.FileEntry
import com.exporter.model.TransferResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class TransferManagerTest {

    private val connector: DeviceConnector = mock()
    private val manager = TransferManager(connector)

    private val fileA = FileEntry(path = "/sdcard/foto.jpg", size = 1024L, lastModified = null)
    private val fileB = FileEntry(path = "/sdcard/video.mp4", size = 2048L, lastModified = null)

    @Test
    fun `transferData - lancia IllegalStateException se device non connesso`() {
        whenever(connector.isConnected()).thenReturn(false)

        assertThrows<IllegalStateException> {
            manager.transferData("/sdcard")
        }
    }

    @Test
    fun `transferData - il messaggio dell'eccezione indica device non connesso`() {
        whenever(connector.isConnected()).thenReturn(false)

        val ex = assertThrows<IllegalStateException> {
            manager.transferData("/sdcard")
        }

        assertTrue(ex.message!!.contains("not connected", ignoreCase = true))
    }

    @Test
    fun `transferData - lancia IllegalStateException se nessun file trovato`() {
        whenever(connector.isConnected()).thenReturn(true)
        whenever(connector.listFiles(any())).thenReturn(emptyList())

        assertThrows<IllegalStateException> {
            manager.transferData("/sdcard/vuota")
        }
    }

    @Test
    fun `transferData - restituisce un risultato per ogni file`() {
        whenever(connector.isConnected()).thenReturn(true)
        whenever(connector.listFiles(any())).thenReturn(listOf(fileA, fileB))
        whenever(connector.transferFile(fileA)).thenReturn(TransferResult.Success(fileA))
        whenever(connector.transferFile(fileB)).thenReturn(TransferResult.Success(fileB))

        val results = manager.transferData("/sdcard")

        assertEquals(2, results.size)
    }

    @Test
    fun `transferData - tutti i file trasferiti con successo restituisce solo Success`() {
        whenever(connector.isConnected()).thenReturn(true)
        whenever(connector.listFiles(any())).thenReturn(listOf(fileA, fileB))
        whenever(connector.transferFile(any())).thenAnswer {
            TransferResult.Success(it.getArgument(0))
        }

        val results = manager.transferData("/sdcard")

        assertTrue(results.all { it is TransferResult.Success })
    }

    @Test
    fun `transferData - un file fallisce, gli altri vengono comunque trasferiti`() {
        val error = Exception("pull failed")
        whenever(connector.isConnected()).thenReturn(true)
        whenever(connector.listFiles(any())).thenReturn(listOf(fileA, fileB))
        whenever(connector.transferFile(fileA)).thenReturn(TransferResult.Failure(fileA, error))
        whenever(connector.transferFile(fileB)).thenReturn(TransferResult.Success(fileB))

        val results = manager.transferData("/sdcard")

        assertEquals(2, results.size)
        assertTrue(results[0] is TransferResult.Failure)
        assertTrue(results[1] is TransferResult.Success)
    }

    @Test
    fun `transferData - tutti i file falliscono restituisce solo Failure`() {
        val error = Exception("pull failed")
        whenever(connector.isConnected()).thenReturn(true)
        whenever(connector.listFiles(any())).thenReturn(listOf(fileA, fileB))
        whenever(connector.transferFile(any())).thenAnswer {
            TransferResult.Failure(it.getArgument(0), error)
        }

        val results = manager.transferData("/sdcard")

        assertTrue(results.all { it is TransferResult.Failure })
    }

    @Test
    fun `transferData - chiama transferFile esattamente una volta per file`() {
        whenever(connector.isConnected()).thenReturn(true)
        whenever(connector.listFiles(any())).thenReturn(listOf(fileA, fileB))
        whenever(connector.transferFile(any())).thenAnswer {
            TransferResult.Success(it.getArgument(0))
        }

        manager.transferData("/sdcard")

        verify(connector, times(1)).transferFile(fileA)
        verify(connector, times(1)).transferFile(fileB)
    }

    @Test
    fun `transferData - passa il path corretto a listFiles`() {
        whenever(connector.isConnected()).thenReturn(true)
        whenever(connector.listFiles("/sdcard/DCIM")).thenReturn(listOf(fileA))
        whenever(connector.transferFile(any())).thenReturn(TransferResult.Success(fileA))

        manager.transferData("/sdcard/DCIM")

        verify(connector).listFiles("/sdcard/DCIM")
    }
}
