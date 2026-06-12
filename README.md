# File Exporter

A command-line tool written in Kotlin for exporting files from an Android device (via ADB) or from a local filesystem path to a local output directory. Features an interactive wizard UI and a live animated transfer progress display powered by [Mordant](https://github.com/ajalt/mordant).

## What it does

Exporter connects to a source — either an Android device over ADB or a local directory — lists all files at a given path, and copies them to a local output folder. It reports each file as successfully exported or failed, with the reason.

When launched without all required flags, an **interactive wizard** guides you through the configuration step by step, showing a summary and asking for confirmation before the transfer begins. Once confirmed, a **live animated UI** tracks overall progress, the current file being transferred, and a scrolling log of results.

## Requirements

- Java 17+
- Maven 3.x
- ADB installed and on your `PATH` (only required for ADB mode)
- An Android device connected and authorized (only required for ADB mode)

## Build

```bash
mvn package
```

This produces a fat JAR at `target/Exporter-0.0.1-SNAPSHOT.jar` with all dependencies bundled.

## Run

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar [options]
```

All flags are optional. Any flag you omit will be prompted interactively at startup.

## Options

| Flag | Short | Required | Default | Description |
|------|-------|----------|---------|-------------|
| `--connector-type` | `-d` | No | prompted | Connector type: `ADB` or `LOCAL` |
| `--source` | `-s` | No | prompted | Source path to export from (device path for ADB, local path for LOCAL) |
| `--output` | `-o` | No | prompted | Local destination directory for exported files |
| `--connector-path` | `-c` | No | `connector/path` | Path to the ADB binary (ADB mode only) |
| `--timeout` | `-t` | No | `30` | Command timeout in seconds (ADB mode only) |

## Interactive wizard

If you run the tool without flags (or omit some), it launches a step-by-step wizard:

```
╔═══════════════════════════════════════╗
║  📦  Exporter                         ║
╚═══════════════════════════════════════╝

Connector type [ADB/LOCAL]: ADB
Source path: /sdcard/DCIM/Camera
Output path: ~/Desktop/exported
ADB binary path (connector/path): /usr/local/bin/adb
Timeout (seconds) (30):

  ──────────────────────────────────────
  Connector  ADB
  Source     /sdcard/DCIM/Camera
  Output     ~/Desktop/exported
  ADB        /usr/local/bin/adb
  Timeout    30s
  ──────────────────────────────────────

Proceed? [Y/n]:
```

Pre-filled values (from flags) are shown in yellow next to each prompt. Press Enter to accept a pre-filled value.

## Live transfer UI

Once confirmed, a live animated progress display runs in the terminal:

```
╔═══════════════════════════════════════════════════════╗
║  📦  Exporter                                         ║
╚═══════════════════════════════════════════════════════╝

  Source   /sdcard/DCIM/Camera
  Output   ~/Desktop/exported
  Files    42 found

  ──────────────────────────────────────────────────────

  Overall  ████████████░░░░░░░░  60%  25 / 42

  Current  IMG_20240315_143022.jpg
           ⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙⠙

  ──────────────────────────────────────────────────────

  ✔  IMG_20240315_143001.jpg              2.3 MB
  ✔  IMG_20240315_143010.jpg              1.8 MB
  ✘  VID_20240315_140000.mp4              Failed to transfer file: ...
  ...
  ──────────────────────────────────────────────────────
```

At the end a summary shows total successes, failures, and elapsed time.

## Examples

### Export files from an Android device via ADB (fully non-interactive)

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar \
  -d ADB \
  -c /usr/local/bin/adb \
  -s /sdcard/DCIM/Camera \
  -o ~/Desktop/exported
```

### Export files from a local directory (fully non-interactive)

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar \
  -d LOCAL \
  -s /path/to/source/folder \
  -o /path/to/output/folder
```

### Launch the interactive wizard

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar
```

## ADB mode — step by step

### 1. Install ADB

**macOS (Homebrew):**
```bash
brew install android-platform-tools
```

**Linux (apt):**
```bash
sudo apt install adb
```

**Windows:** Download the [Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools) and add the folder to your `PATH`.

Verify the installation:
```bash
adb version
# Android Debug Bridge version 1.0.41
```

### 2. Enable USB debugging on the device

1. Open **Settings → About phone**.
2. Tap **Build number** seven times to unlock developer options.
3. Go to **Settings → Developer options** and enable **USB debugging**.

### 3. Connect the device

Plug the device into your computer via USB. The first time you connect, accept the **"Allow USB debugging?"** dialog on the device.

Verify the device is recognized:
```bash
adb devices
# List of devices attached
# R58M123ABCD    device
```

The state must be `device`. If it shows `unauthorized`, re-accept the dialog on the phone. If it shows `offline`, try unplugging and reconnecting.

### 4. Find the ADB binary path

```bash
which adb
# /usr/local/bin/adb   (macOS/Linux)
# C:\platform-tools\adb.exe   (Windows)
```

Pass this path to `--connector-path` (or enter it in the wizard).

### 5. Run the export

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar \
  -d ADB \
  -c /usr/local/bin/adb \
  -s /sdcard/DCIM/Camera \
  -o ~/Desktop/exported \
  -t 60
```

Common source paths on Android:

| Content | Path |
|---------|------|
| Camera photos/videos | `/sdcard/DCIM/Camera` |
| Screenshots | `/sdcard/Pictures/Screenshots` |
| Downloads | `/sdcard/Download` |
| WhatsApp media | `/sdcard/Android/media/com.whatsapp/WhatsApp/Media` |

### Troubleshooting ADB

| Symptom | Fix |
|---------|-----|
| `Error! Device not connected.` | Run `adb devices` — device must show as `device`, not `unauthorized` or `offline` |
| `adb: not found` | Install ADB or pass the full binary path via `-c` |
| Timeout errors | Increase `-t` (default 30 s); large files may need 120 s or more |
| Permission denied on device path | The path may require root or a different app's storage permission |

## Connector modes

### ADB

Wraps the `adb` binary. Runs `adb shell ls -la <source>` to list files, then `adb pull` for each file. Requires the device to be in `device` state (`adb get-state` returns `device`).

Use `--connector-path` to point to the ADB binary if it is not the system default.

### LOCAL

Uses the Java NIO filesystem API. Walks the source path recursively, finds all regular files, and copies them to the output directory. No external tools required. The `--connector-path` and `--timeout` flags have no effect in this mode.

## Project structure

```
src/main/kotlin/com/exporter/
├── cli/
│   ├── Main.kt                   # Entry point
│   ├── ExportCommand.kt          # CLI command (picocli) — wires wizard + UI
│   ├── ExportParams.kt           # Data class holding all transfer parameters
│   ├── WizardPrompt.kt           # Interactive step-by-step configuration wizard
│   └── TransferUI.kt             # Live animated transfer progress display
├── device/
│   ├── connector/
│   │   ├── ConnectorType.kt      # ADB / LOCAL enum
│   │   ├── DeviceConnector.kt    # Interface: isConnected, listFiles, transferFile
│   │   ├── DeviceConnectorFactory.kt
│   │   └── impl/
│   │       ├── AdbDeviceConnector.kt
│   │       └── LocalDeviceConnector.kt
│   └── utility/
│       └── FileMapper.kt         # Parses `ls -la` output into FileEntry list
├── model/
│   ├── FileEntry.kt              # path, size, lastModified, name
│   └── TransferResult.kt         # Sealed class: Success | Failure
└── transfer/
    └── TransferManager.kt        # Orchestrates listFiles + transferFile
```

## Tests

```bash
mvn test
```

Tests cover `FileMapper` (parsing ADB `ls -la` output) and `TransferManager` (connection check, empty path, transfer orchestration).

## Adding a new connector

1. Add a value to `ConnectorType`.
2. Implement `DeviceConnector` in `device/connector/impl/`.
3. Add the mapping in `DeviceConnectorFactory.create()`.
