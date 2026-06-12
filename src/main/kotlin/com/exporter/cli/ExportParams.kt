package com.exporter.cli

import com.exporter.device.connector.ConnectorType

data class ExportParams(
    val connectorType: ConnectorType,
    val sourcePath: String,
    val outputPath: String,
    val connectorPath: String = "connector/path",
    val timeoutSeconds: Long = 30L
)
