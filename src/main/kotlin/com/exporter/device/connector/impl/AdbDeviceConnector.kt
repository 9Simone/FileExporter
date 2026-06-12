package com.exporter.device.connector.impl

import com.exporter.device.connector.DeviceConnector
import com.exporter.model.FileEntry
import com.exporter.model.TransferResult
import com.exporter.device.utility.FileMapper
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class AdbDeviceConnector(
    private val adbPath: String,
    private val basePath: String,
    private val timeoutSeconds: Long
) : DeviceConnector {

    data class ProcessResult(
        val stdout: String,
        val exitCode: Int
    )

    override fun isConnected(): Boolean {
        val result = executeAdbCommand("get-state")
        return result.exitCode == 0 && result.stdout.trim() == "device"
    }

    override fun listFiles(path: String): List<FileEntry> {
        val result = executeAdbCommand("shell", "ls", "-la", path)
        return FileMapper.mapToFileEntry(result.stdout)
    }

    override fun transferFile(entry: FileEntry): TransferResult {
        val localDestination = Paths.get(basePath, entry.path)
        Files.createDirectories(localDestination.parent)

        val result = executeAdbCommand("pull", entry.path, localDestination.toString())
        if (result.exitCode != 0) {
            return TransferResult.Failure(fileEntry = entry, error = Exception("Failed to transfer file: ${result.stdout}"))
        }
        return TransferResult.Success(fileEntry = entry)
    }

    private fun executeAdbCommand(vararg command: String): ProcessResult {
        val processBuilder = ProcessBuilder()
            .command(listOf(adbPath) + command.toList())
            .redirectErrorStream(true)

        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

        return ProcessResult(stdout = stdout, exitCode = process.exitValue())
    }
}
