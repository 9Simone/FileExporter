package com.exporter.model

data class FileEntry(
    val path: String,
    val size: Long,
    val lastModified: Long?
) {
    val name: String get() = path.substringAfterLast('/')
}
