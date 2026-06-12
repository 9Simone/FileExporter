package com.exporter.transfer

import com.exporter.device.connector.DeviceConnector
import com.exporter.model.TransferResult

class TransferManager(
    private val connector: DeviceConnector
) {

    fun transferData(
        devicePath: String,
        onProgress: ((TransferResult) -> Unit)? = null
    ): List<TransferResult> {
        if (!connector.isConnected()) {
            throw IllegalStateException("Error! Device not connected.")
        }

        val files = connector.listFiles(devicePath)
        if (files.isEmpty()) {
            throw IllegalStateException("Error! No files found at the specified path.")
        }

        return files.map { file ->
            connector.transferFile(file).also { result -> onProgress?.invoke(result) }
        }
    }
}
