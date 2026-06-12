package com.exporter.transfer

import com.exporter.device.DeviceConnector
import com.exporter.model.TransferResult

class TransferManager(
    private val connector: DeviceConnector
) {

    fun transferData(devicePath: String): List<TransferResult> {
        if (!connector.isConnected()) {
            throw IllegalStateException("Error! Device not connected.")
        }

        val files = connector.listFiles(devicePath)
        if (files.isEmpty()) {
            throw IllegalStateException("Error! No files found at the specified path.")
        }

        return files.map { connector.transferFile(it) }
    }
}
