package com.exporter.cli

import com.exporter.device.connector.ConnectorType
import com.exporter.device.connector.DeviceConnectorFactory
import com.exporter.model.TransferResult
import com.exporter.transfer.TransferManager
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "export", description = ["Export data from the connected device"])
class ExportCommand : Callable<Int> {

    @Option(names = ["-c", "--connector-path"], description = ["Path to the connector binary"], defaultValue = "connector/path")
    private lateinit var connectorPath: String

    @Option(names = ["-d", "--connector-type"], description = ["Type of the connector (e.g., ADB)"], required = true)
    private lateinit var connectorType: ConnectorType

    @Option(names = ["-s", "--source"], description = ["Source path on the device to export from"], required = true)
    private lateinit var devicePath: String

    @Option(names = ["-o", "--output"], description = ["Local base path for exported files"], required = true)
    private lateinit var outputPath: String

    @Option(names = ["-t", "--timeout"], description = ["Timeout in seconds"], defaultValue = "30")
    private var timeoutSeconds: Long = 30

    override fun call(): Int {
        val connector = DeviceConnectorFactory.create(connectorType, connectorPath, outputPath, timeoutSeconds)
        val transferManager = TransferManager(connector)

        try {
            val results = transferManager.transferData(devicePath)
            val successResults = results.filterIsInstance<TransferResult.Success>()
            val failureResults = results.filterIsInstance<TransferResult.Failure>()
            successResults.forEach { println("Successfully exported: ${it.fileEntry.name}") }
            failureResults.forEach { println("Failed to export: ${it.fileEntry.name} - ${it.error.message}") }
        } catch (e: Exception) {
            e.printStackTrace()
            return 1
        }
        return 0
    }
}
