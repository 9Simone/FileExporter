package com.exporter.device.connector

import com.exporter.model.FileEntry
import com.exporter.model.TransferResult

interface DeviceConnector {
    fun isConnected(): Boolean
    fun listFiles(path: String): List<FileEntry>
    fun transferFile(entry: FileEntry): TransferResult
}
