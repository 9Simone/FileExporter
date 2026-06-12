# Interactive CLI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the plain-text CLI with an interactive wizard + live animated transfer UI using Mordant.

**Architecture:** `ExportCommand` delegates parameter collection to `WizardPrompt` (Mordant-based interactive prompts), then hands off to `TransferUI` (Mordant animation) which drives the transfer file-by-file and renders a live progress display. `TransferManager` gains an optional `onProgress` callback so the UI can react per-file without coupling the manager to the terminal.

**Tech Stack:** Kotlin, Mordant 2.7.2 (JVM artifact), picocli (unchanged), Maven

---

## File Map

| Action | File | Responsibility |
|---|---|---|
| Modify | `pom.xml` | Add Mordant dependency |
| Create | `src/main/kotlin/com/exporter/cli/ExportParams.kt` | Data class holding all validated export parameters |
| Modify | `src/main/kotlin/com/exporter/transfer/TransferManager.kt` | Add optional `onProgress` callback to `transferData` |
| Modify | `src/test/kotlin/com/exporter/transfer/TransferManagerTest.kt` | Add tests for callback behavior |
| Create | `src/main/kotlin/com/exporter/cli/WizardPrompt.kt` | Interactive wizard using Mordant Terminal prompts |
| Create | `src/main/kotlin/com/exporter/cli/TransferUI.kt` | Live animated transfer display using Mordant animation |
| Modify | `src/main/kotlin/com/exporter/cli/ExportCommand.kt` | Wire wizard + TransferUI, keep flags as pre-fill |

---

## Task 1: Add Mordant dependency

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Mordant to pom.xml**

Inside the `<dependencies>` block, add after the existing `logback-classic` dependency:

```xml
<dependency>
    <groupId>com.github.ajalt.mordant</groupId>
    <artifactId>mordant-jvm</artifactId>
    <version>2.7.2</version>
</dependency>
```

- [ ] **Step 2: Verify the project still compiles**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: add Mordant dependency for interactive CLI"
```

---

## Task 2: Create ExportParams data class

**Files:**
- Create: `src/main/kotlin/com/exporter/cli/ExportParams.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.exporter.cli

import com.exporter.device.connector.ConnectorType

data class ExportParams(
    val connectorType: ConnectorType,
    val sourcePath: String,
    val outputPath: String,
    val connectorPath: String = "connector/path",
    val timeoutSeconds: Long = 30L
)
```

- [ ] **Step 2: Verify compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/exporter/cli/ExportParams.kt
git commit -m "feat: add ExportParams data class"
```

---

## Task 3: Add onProgress callback to TransferManager (TDD)

**Files:**
- Modify: `src/main/kotlin/com/exporter/transfer/TransferManager.kt`
- Modify: `src/test/kotlin/com/exporter/transfer/TransferManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

Add these two tests to `TransferManagerTest.kt` inside the existing class:

```kotlin
@Test
fun `transferData - invoca onProgress per ogni file trasferito`() {
    whenever(connector.isConnected()).thenReturn(true)
    whenever(connector.listFiles(any())).thenReturn(listOf(fileA, fileB))
    whenever(connector.transferFile(any())).thenAnswer {
        TransferResult.Success(it.getArgument(0))
    }
    val reported = mutableListOf<TransferResult>()

    manager.transferData("/sdcard", onProgress = { reported.add(it) })

    assertEquals(2, reported.size)
}

@Test
fun `transferData - funziona senza callback (onProgress null)`() {
    whenever(connector.isConnected()).thenReturn(true)
    whenever(connector.listFiles(any())).thenReturn(listOf(fileA))
    whenever(connector.transferFile(any())).thenReturn(TransferResult.Success(fileA))

    assertDoesNotThrow { manager.transferData("/sdcard") }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
mvn test -pl . -Dtest=TransferManagerTest -q 2>&1 | tail -5
```

Expected: compilation error — `transferData` does not accept `onProgress`.

- [ ] **Step 3: Update TransferManager**

Replace the body of `transferData` in `src/main/kotlin/com/exporter/transfer/TransferManager.kt`:

```kotlin
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
```

- [ ] **Step 4: Run all tests**

```bash
mvn test -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/exporter/transfer/TransferManager.kt \
        src/test/kotlin/com/exporter/transfer/TransferManagerTest.kt
git commit -m "feat: add optional onProgress callback to TransferManager"
```

---

## Task 4: Create WizardPrompt

**Files:**
- Create: `src/main/kotlin/com/exporter/cli/WizardPrompt.kt`

The wizard collects parameters interactively. Any CLI flag already passed is shown as a pre-filled default — the user presses Enter to accept it or types a new value.

- [ ] **Step 1: Create the file**

```kotlin
package com.exporter.cli

import com.exporter.device.connector.ConnectorType
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
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
```

- [ ] **Step 2: Verify compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/exporter/cli/WizardPrompt.kt
git commit -m "feat: add interactive WizardPrompt using Mordant"
```

---

## Task 5: Create TransferUI

**Files:**
- Create: `src/main/kotlin/com/exporter/cli/TransferUI.kt`

`TransferUI` owns the connection setup, list-files call, and drives the per-file transfer loop while rendering a live animated display. It uses Mordant's `terminal.animation` widget system.

- [ ] **Step 1: Create the file**

```kotlin
package com.exporter.cli

import com.exporter.device.connector.DeviceConnectorFactory
import com.exporter.model.FileEntry
import com.exporter.model.TransferResult
import com.exporter.transfer.TransferManager
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
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

        for (file in files) {
            tick++
            anim.update(state(file))
            val result = connector.transferFile(file)
            results.add(result)
            anim.update(state(file))
        }

        anim.update(state(null))
        anim.stop()

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
                val spinner = spinnerChar(state.tick)
                appendLine("  Current  ${(yellow + bold)(state.currentFile.name)}")
                appendLine("           ${cyan(spinner.toString().repeat(20))}")
            }

            appendLine()
            appendLine(cyan("  ──────────────────────────────────────────────────────"))
            appendLine()

            // Result rows — show last 15 to avoid flooding
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
        val elapsed = formatElapsed(elapsedMs)
        terminal.println()
        terminal.println(cyan("  ══════════════════════════════════════════════════════"))
        terminal.println("  ${green("✔")}  ${green(bold("$successes files exported successfully"))}")
        if (failures > 0) terminal.println("  ${red("✘")}  ${red(bold("$failures files failed"))}")
        terminal.println("  ⏱   $elapsed elapsed")
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

private val white = com.github.ajalt.mordant.rendering.TextColors.white
```

- [ ] **Step 2: Verify compile**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/exporter/cli/TransferUI.kt
git commit -m "feat: add live animated TransferUI using Mordant"
```

---

## Task 6: Wire ExportCommand

**Files:**
- Modify: `src/main/kotlin/com/exporter/cli/ExportCommand.kt`

`ExportCommand` now creates a `Terminal`, builds a `ExportParams` from any CLI flags (as pre-fill), delegates to `WizardPrompt`, then to `TransferUI`. The flags remain optional — picocli `required = true` becomes `required = false` with sensible null defaults so pre-filling is optional.

- [ ] **Step 1: Rewrite ExportCommand.kt**

```kotlin
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
```

- [ ] **Step 2: Verify compile and all tests pass**

```bash
mvn test -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 3: Build the fat JAR**

```bash
mvn package -q
```

Expected: `BUILD SUCCESS`, `target/Exporter-0.0.1-SNAPSHOT.jar` updated.

- [ ] **Step 4: Smoke test — wizard with no flags**

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar
```

Expected: wizard appears with header, prompts for connector type, source, output. Type `LOCAL`, enter a valid source directory and output directory, confirm → animated transfer UI appears.

- [ ] **Step 5: Smoke test — wizard with pre-filled flags**

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar -d LOCAL -s /tmp -o /tmp/out
```

Expected: wizard shows with all fields pre-filled (in yellow). Pressing Enter on each accepts the defaults.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/exporter/cli/ExportCommand.kt
git commit -m "feat: wire ExportCommand to WizardPrompt and TransferUI"
```
