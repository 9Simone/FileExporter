# Exporter

A command-line tool written in Kotlin for exporting files from a device (via ADB) or from a local filesystem path to a local output directory.

## What it does

Exporter connects to a source — either an Android device over ADB or a local directory — lists all files at a given path, and copies them to a local output folder. It reports each file as successfully exported or failed, with the reason.

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

## Options

| Flag | Short | Required | Default | Description |
|------|-------|----------|---------|-------------|
| `--connector-type` | `-d` | Yes | — | Connector type: `ADB` or `LOCAL` |
| `--source` | `-s` | Yes | — | Source path to export from (device path for ADB, local path for LOCAL) |
| `--output` | `-o` | Yes | — | Local destination directory for exported files |
| `--connector-path` | `-c` | No | `connector/path` | Path to the ADB binary (ignored in LOCAL mode) |
| `--timeout` | `-t` | No | `30` | Command timeout in seconds (applies to ADB commands) |

## Examples

### Export files from an Android device via ADB

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar \
  -d ADB \
  -c /usr/local/bin/adb \
  -s /sdcard/DCIM/Camera \
  -o ~/Desktop/exported
```

This connects to the device using the ADB binary at `/usr/local/bin/adb`, lists all files under `/sdcard/DCIM/Camera` on the device, and pulls each one to `~/Desktop/exported`.

### Export files from a local directory

```bash
java -jar target/Exporter-0.0.1-SNAPSHOT.jar \
  -d LOCAL \
  -s /path/to/source/folder \
  -o /path/to/output/folder
```

This walks the source folder recursively and copies all regular files to the output folder. Files that already exist at the destination are skipped.

## Connector modes

### ADB

Wraps the `adb` binary. Runs `adb shell ls -la <source>` to list files, then `adb pull` for each file. Requires a device to be in `device` state (i.e., `adb get-state` returns `device`).

Use `--connector-path` to point to the ADB binary if it is not the system default.

### LOCAL

Uses the Java NIO filesystem API. Walks the source path recursively, finds all regular files, and copies them to the output directory. No external tools required. The `--connector-path` and `--timeout` flags have no effect in this mode.

## Output

For each file the tool prints one line:

```
Successfully exported: <filename>
Failed to export: <filename> - <error message>
```

The process exits with code `0` on success and `1` if an unrecoverable error occurs (device not connected, no files found, unexpected exception).

## Project structure

```
src/main/kotlin/com/exporter/
├── cli/
│   ├── Main.kt                   # Entry point
│   └── ExportCommand.kt          # CLI command definition (picocli)
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
