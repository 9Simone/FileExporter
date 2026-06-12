package com.exporter.model

sealed class TransferResult {
    data class Success(val fileEntry: FileEntry) : TransferResult()
    data class Failure(val fileEntry: FileEntry, val error: Throwable) : TransferResult()
}
