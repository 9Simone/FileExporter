package com.exporter.device.utility

import com.exporter.model.FileEntry

object FileMapper {

    fun mapToFileEntry(output: String): List<FileEntry> {
        return output.lines().drop(1).mapNotNull { line ->
            val parts = line.trim().split("\\s+".toRegex(), 8)
            if (parts.size == 8) {
                val permissions = parts[0]
                val name = parts[7]
                if (permissions.startsWith("d") || name == "." || name == "..") return@mapNotNull null
                FileEntry(
                    path = name,
                    size = parts[4].toLongOrNull() ?: 0L,
                    lastModified = null
                )
            } else {
                null
            }
        }
    }
}
