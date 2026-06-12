package com.exporter.device.connector.impl

import com.exporter.device.connector.DeviceConnector
import com.exporter.model.FileEntry
import com.exporter.model.TransferResult
import java.nio.file.Files
import java.nio.file.Paths

class LocalDeviceConnector(
    private val outputPath: String,
) : DeviceConnector {

    override fun isConnected(): Boolean {
        return true
    }

    override fun listFiles(path: String): List<FileEntry> {
        
        val files = Files.walk(Paths.get(path))
        .filter { Files.isRegularFile(it) }
        .map {
            FileEntry(path = it.toString(), size = Files.size(it), lastModified = Files.getLastModifiedTime(it).toMillis())
         }
        return files.toList()
    }

    override fun transferFile(entry: FileEntry): TransferResult {
        val outputFilePath = Paths.get(outputPath, entry.name)
        try {
            if (Files.exists(outputFilePath)) {
                return TransferResult.Success(fileEntry = entry)
            }

            if (!Files.exists(outputFilePath.parent)) {
                Files.createDirectories(outputFilePath.parent)
            }

            Files.copy(Paths.get(entry.path), outputFilePath)
            return TransferResult.Success(fileEntry = entry)
        } catch (e: Exception) {
            return TransferResult.Failure(fileEntry = entry, error = e)
        }
    }
}