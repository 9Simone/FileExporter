package com.exporter.device

import com.exporter.model.FileEntry

object FileMapper {

    fun mapToFileEntry(output: String): List<FileEntry> {
        return output.lines().drop(1).mapNotNull { line ->
            val parts = line.trim().split("\\s+".toRegex(), 8)
            if (parts.size == 8) {
                FileEntry(
                    path = parts[7],
                    size = parts[4].toLongOrNull() ?: 0L,
                    lastModified = null
                )
            } else {
                null
            }
        }
    }
}
