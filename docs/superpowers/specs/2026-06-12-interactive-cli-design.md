# Interactive CLI Design

**Date:** 2026-06-12  
**Project:** Exporter  
**Status:** Approved

---

## Overview

Replace the current plain-text CLI with a fully interactive, visually rich terminal experience using the [Mordant](https://github.com/ajalt/mordant) library (Kotlin-first, JetBrains). The goal is a two-phase UX: an interactive wizard to collect parameters, followed by a live animated UI during the transfer.

---

## Phase 1 — Interactive Wizard

At startup, the tool enters a sequential prompt wizard. If CLI flags were passed, their values are pre-filled; the user can confirm with Enter or overwrite.

**Prompt sequence:**

1. `Connector type [ADB/LOCAL]:` — pre-filled from `-d`
2. `Source path:` — pre-filled from `-s`
3. `Output path:` — pre-filled from `-o`
4. `ADB binary path:` — shown only if connector = ADB; pre-filled from `-c`
5. `Timeout (seconds) [30]:` — shown only if connector = ADB; pre-filled from `-t`
6. Summary of chosen parameters + `Proceed? [Y/n]` confirmation prompt

Fields 4 and 5 are hidden entirely when connector = LOCAL, keeping the wizard clean.

---

## Phase 2 — Live Transfer UI

Once confirmed, the screen transitions to an animated transfer view built with Mordant widgets.

**Layout:**

```
╔═══════════════════════════════════════════════════════╗
║  📦 Exporter                                          ║
╚═══════════════════════════════════════════════════════╝

  Source   <source path>
  Output   <output path>
  Files    <N> found

──────────────────────────────────────────────────────

  Overall  ██████████░░░░░░░░░░  58%  24 / 42

  Current  <filename>
           ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  <size>

──────────────────────────────────────────────────────

  ✔  <filename>          <size>
  ✘  <filename>          <error reason>

──────────────────────────────────────────────────────
```

**Color scheme (ANSI via Mordant):**

| Element | Color |
|---|---|
| Header / borders | Cyan bold |
| ✔ success rows | Green |
| ✘ failure rows + error | Red |
| Current filename | Yellow bold |
| Overall progress bar | Green |
| Current file bar (LOCAL) | Cyan (determinate) |
| Current file bar (ADB) | Cyan (indeterminate animation) |
| Percentages / counters | White bold |

**Progress bar behavior:**
- Overall bar is always determinate: driven by `filesCompleted / filesTotal`.
- Current file bar: determinate for LOCAL (NIO copy with known size); indeterminate spinner-style animation for ADB (no byte-level progress from `adb pull`).

**Summary screen (shown after all transfers complete):**

```
  ✔  <N> files exported successfully
  ✘  <N> files failed
  ⏱   HH:MM:SS elapsed
```

---

## Architecture

### New components

| File | Responsibility |
|---|---|
| `cli/WizardPrompt.kt` | Collects and validates parameters interactively via Mordant Terminal input |
| `cli/TransferUI.kt` | Renders the live transfer screen using Mordant animation widgets |

### Modified components

| File | Change |
|---|---|
| `cli/ExportCommand.kt` | After parsing flags, delegates to `WizardPrompt` for interactive confirmation, then calls `TransferUI` to drive the transfer |
| `cli/Main.kt` | No change |
| `pom.xml` | Add Mordant dependency |

### Data flow

```
main()
  → ExportCommand.call()
      → WizardPrompt.collect(prefilled params)   // interactive phase
          → returns confirmed ExportParams
      → TransferUI.run(params)                   // visual phase
          → TransferManager.transferData()
              → connector.listFiles()
              → for each file: connector.transferFile()
                  → UI updates: overall bar, current bar, result row
          → prints summary
```

`TransferUI` wraps `TransferManager` calls, receiving callbacks/events per file to update the Mordant animation context. `TransferManager` itself is unchanged.

---

## Dependencies

Add to `pom.xml`:

```xml
<dependency>
    <groupId>com.github.ajalt.mordant</groupId>
    <artifactId>mordant-jvm</artifactId>
    <version>2.7.2</version>
</dependency>
```

---

## Out of scope

- Persistent config file (saving last-used parameters)
- Mouse input
- File filtering / include-exclude patterns
- Retry logic for failed files
