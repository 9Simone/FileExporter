interface DeviceConnector {
    fun isConnected(): Boolean
    fun listFiles(path: String): List<FileEntry>
    fun transferFile(entry: FileEntry): TransferResult
}