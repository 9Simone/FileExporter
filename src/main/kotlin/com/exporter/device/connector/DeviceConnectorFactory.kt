package com.exporter.device.connector

import com.exporter.device.connector.impl.AdbDeviceConnector
import com.exporter.device.connector.impl.LocalDeviceConnector

object DeviceConnectorFactory {

    fun create(deviceType: ConnectorType, connectorPath: String, basePath: String, timeout: Long): DeviceConnector = when (deviceType) {
        ConnectorType.ADB -> AdbDeviceConnector(connectorPath, basePath, timeout)
        ConnectorType.LOCAL -> LocalDeviceConnector(basePath)
    }
}
