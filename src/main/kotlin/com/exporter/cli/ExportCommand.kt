package com.exporter.cli

import com.exporter.device.connector.ConnectorType
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "export", description = ["Export data from the connected device"])
class ExportCommand : Callable<Int> {

    @Option(names = ["-c", "--connector-path"], description = ["Path to the connector binary"])
    private var connectorPath: String? = null

    @Option(names = ["-d", "--connector-type"], description = ["Type of the connector (ADB or LOCAL)"])
    private var connectorType: ConnectorType? = null

    @Option(names = ["-s", "--source"], description = ["Source path on the device to export from"])
    private var devicePath: String? = null

    @Option(names = ["-o", "--output"], description = ["Local base path for exported files"])
    private var outputPath: String? = null

    @Option(names = ["-t", "--timeout"], description = ["Timeout in seconds"])
    private var timeoutSeconds: Long? = null

    override fun call(): Int {
        val terminal = Terminal()
        val prefilled = buildPrefilled()
        return try {
            val params = WizardPrompt(terminal).collect(prefilled)
            TransferUI(terminal).run(params)
            0
        } catch (_: ExportAbortedException) {
            0
        } catch (e: Exception) {
            terminal.println(red("Error: ${e.message}"))
            1
        }
    }

    private fun buildPrefilled(): ExportParams? {
        val type = connectorType ?: return null
        val src = devicePath ?: return null
        val out = outputPath ?: return null
        return ExportParams(
            connectorType = type,
            sourcePath = src,
            outputPath = out,
            connectorPath = connectorPath ?: "connector/path",
            timeoutSeconds = timeoutSeconds ?: 30L
        )
    }
}
