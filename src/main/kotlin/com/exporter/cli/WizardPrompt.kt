package com.exporter.cli

import com.exporter.device.connector.ConnectorType
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal

class ExportAbortedException : Exception("Export aborted by user")

class WizardPrompt(private val terminal: Terminal) {

    fun collect(prefilled: ExportParams?): ExportParams {
        printHeader()

        val connectorType = promptConnectorType(prefilled?.connectorType)
        val sourcePath = promptNonBlank("Source path", prefilled?.sourcePath)
        val outputPath = promptNonBlank("Output path", prefilled?.outputPath)

        val connectorPath: String
        val timeoutSeconds: Long
        if (connectorType == ConnectorType.ADB) {
            connectorPath = promptWithDefault("ADB binary path", prefilled?.connectorPath ?: "connector/path")
            timeoutSeconds = promptLong("Timeout (seconds)", prefilled?.timeoutSeconds ?: 30L)
        } else {
            connectorPath = "connector/path"
            timeoutSeconds = 30L
        }

        val params = ExportParams(connectorType, sourcePath, outputPath, connectorPath, timeoutSeconds)
        printSummary(params)

        if (!promptConfirm()) {
            terminal.println(yellow("Aborted."))
            throw ExportAbortedException()
        }
        return params
    }

    private fun printHeader() {
        terminal.println()
        terminal.println((cyan + bold)("╔═══════════════════════════════════════╗"))
        terminal.println((cyan + bold)("║  📦  Exporter                         ║"))
        terminal.println((cyan + bold)("╚═══════════════════════════════════════╝"))
        terminal.println()
    }

    private fun promptConnectorType(prefilled: ConnectorType?): ConnectorType {
        val label = buildLabel("Connector type [ADB/LOCAL]", prefilled?.name)
        while (true) {
            val raw = terminal.prompt(label)?.trim()?.uppercase()
            val input = if (raw.isNullOrBlank()) prefilled?.name else raw
            if (input == null) {
                terminal.println(red("This field is required."))
                continue
            }
            return try {
                ConnectorType.valueOf(input)
            } catch (_: IllegalArgumentException) {
                terminal.println(red("Invalid type. Choose ADB or LOCAL."))
                continue
            }
        }
    }

    private fun promptNonBlank(label: String, prefilled: String?): String {
        val prompt = buildLabel(label, prefilled)
        while (true) {
            val input = terminal.prompt(prompt)?.trim()
            if (!input.isNullOrBlank()) return input
            if (!prefilled.isNullOrBlank()) return prefilled
            terminal.println(red("This field is required."))
        }
    }

    private fun promptWithDefault(label: String, default: String): String {
        val input = terminal.prompt(buildLabel(label, default))?.trim()
        return if (input.isNullOrBlank()) default else input
    }

    private fun promptLong(label: String, default: Long): Long {
        while (true) {
            val input = terminal.prompt(buildLabel(label, default.toString()))?.trim()
            if (input.isNullOrBlank()) return default
            return input.toLongOrNull() ?: run {
                terminal.println(red("Must be a number."))
                return@run null
            } ?: continue
        }
    }

    private fun buildLabel(label: String, default: String?): String =
        if (!default.isNullOrBlank()) "$label (${yellow(default)})" else label

    private fun printSummary(params: ExportParams) {
        terminal.println()
        terminal.println(cyan("  ──────────────────────────────────────"))
        terminal.println("  Connector  ${bold(params.connectorType.name)}")
        terminal.println("  Source     ${bold(params.sourcePath)}")
        terminal.println("  Output     ${bold(params.outputPath)}")
        if (params.connectorType == ConnectorType.ADB) {
            terminal.println("  ADB        ${bold(params.connectorPath)}")
            terminal.println("  Timeout    ${bold(params.timeoutSeconds.toString())}s")
        }
        terminal.println(cyan("  ──────────────────────────────────────"))
        terminal.println()
    }

    private fun promptConfirm(): Boolean {
        val input = terminal.prompt("Proceed? [Y/n]")?.trim()?.lowercase()
        return input.isNullOrBlank() || input == "y" || input == "yes"
    }
}
