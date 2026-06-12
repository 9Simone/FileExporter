object FileMapper {

    fun mapToFileEntry(output: String): List<FileEntry> {
        return output.lines().drop(1).mapNotNull { line ->
            val parts = line.split("\\s+".toRegex(), 9)
            if (parts.size == 9) {
                FileEntry(
                    name = parts[8],
                    size = parts[4].toLongOrNull() ?: 0L,
                    isDirectory = parts[0].startsWith("d")
                )
            } else {
                null
            }
        }
    }
}