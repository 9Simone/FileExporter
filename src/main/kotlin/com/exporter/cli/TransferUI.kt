package com.exporter.cli

import com.exporter.device.connector.DeviceConnectorFactory
import com.exporter.model.FileEntry
import com.exporter.model.TransferResult
import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.widgets.Text
import com.github.ajalt.mordant.terminal.Terminal

class TransferUI(private val terminal: Terminal) {

    private data class UiState(
        val params: ExportParams,
        val totalFiles: Int,
        val completedFiles: Int,
        val currentFile: FileEntry?,
        val results: List<TransferResult>,
        val tick: Int,
        val elapsedMs: Long
    )

    fun run(params: ExportParams): List<TransferResult> {
        val connector = DeviceConnectorFactory.create(
            params.connectorType, params.connectorPath, params.outputPath, params.timeoutSeconds
        )

        if (!connector.isConnected()) throw IllegalStateException("Error! Device not connected.")
        val files = connector.listFiles(params.sourcePath)
        if (files.isEmpty()) throw IllegalStateException("Error! No files found at ${params.sourcePath}")

        val startTime = System.currentTimeMillis()
        val results = mutableListOf<TransferResult>()
        var tick = 0

        val anim = terminal.animation<UiState> { buildWidget(it) }

        fun state(current: FileEntry?) = UiState(
            params = params,
            totalFiles = files.size,
            completedFiles = results.size,
            currentFile = current,
            results = results.toList(),
            tick = tick,
            elapsedMs = System.currentTimeMillis() - startTime
        )

        anim.update(state(null))

        try {
            for (file in files) {
                tick++
                anim.update(state(file))
                val result = connector.transferFile(file)
                results.add(result)
                anim.update(state(file))
            }
            anim.update(state(null))
        } finally {
            anim.stop()
        }

        printSummary(results, System.currentTimeMillis() - startTime)
        return results
    }

    private fun buildWidget(state: UiState): Widget {
        val lines = buildString {
            appendLine()
            appendLine((cyan + bold)("╔═══════════════════════════════════════════════════════╗"))
            appendLine((cyan + bold)("║  📦  Exporter                                         ║"))
            appendLine((cyan + bold)("╚═══════════════════════════════════════════════════════╝"))
            appendLine()
            appendLine("  Source   ${bold(state.params.sourcePath)}")
            appendLine("  Output   ${bold(state.params.outputPath)}")
            appendLine("  Files    ${bold(state.totalFiles.toString())} found")
            appendLine()
            appendLine(cyan("  ──────────────────────────────────────────────────────"))
            appendLine()

            // Overall progress
            val pct = if (state.totalFiles > 0) state.completedFiles * 100 / state.totalFiles else 0
            val bar = progressBar(pct, 20, filled = '█', empty = '░', color = green)
            appendLine("  Overall  $bar  ${(white + bold)("$pct%")}  ${state.completedFiles} / ${state.totalFiles}")
            appendLine()

            // Current file
            if (state.currentFile != null) {
                appendLine("  Current  ${(yellow + bold)(state.currentFile.name)}")
                appendLine("           ${cyan(spinnerChar(state.tick).toString().repeat(20))}")
            }

            appendLine()
            appendLine(cyan("  ──────────────────────────────────────────────────────"))
            appendLine()

            // Result rows — show last 15
            state.results.takeLast(15).forEach { result ->
                when (result) {
                    is TransferResult.Success ->
                        appendLine("  ${green("✔")}  ${result.fileEntry.name.padEnd(40)} ${formatSize(result.fileEntry.size)}")
                    is TransferResult.Failure ->
                        appendLine("  ${red("✘")}  ${result.fileEntry.name.padEnd(40)} ${red(result.error.message ?: "error")}")
                }
            }

            appendLine(cyan("  ──────────────────────────────────────────────────────"))
        }
        return Text(lines)
    }

    private fun printSummary(results: List<TransferResult>, elapsedMs: Long) {
        val successes = results.count { it is TransferResult.Success }
        val failures = results.count { it is TransferResult.Failure }
        terminal.println()
        terminal.println(cyan("  ══════════════════════════════════════════════════════"))
        terminal.println("  ${green("✔")}  ${(green + bold)("$successes files exported successfully")}")
        if (failures > 0) terminal.println("  ${red("✘")}  ${(red + bold)("$failures files failed")}")
        terminal.println("  ⏱   ${formatElapsed(elapsedMs)} elapsed")
        terminal.println(cyan("  ══════════════════════════════════════════════════════"))
        terminal.println()
    }

    private fun progressBar(percent: Int, width: Int, filled: Char, empty: Char, color: com.github.ajalt.mordant.rendering.TextColors): String {
        val filledCount = (width * percent / 100).coerceIn(0, width)
        return color(filled.toString().repeat(filledCount)) + empty.toString().repeat(width - filledCount)
    }

    private val spinnerFrames = listOf('⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏')

    private fun spinnerChar(tick: Int): Char = spinnerFrames[tick % spinnerFrames.size]

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }

    private fun formatElapsed(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
