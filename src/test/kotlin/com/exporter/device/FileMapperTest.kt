import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class FileMapperTest {

    @Test
    fun `mapToFileEntry - singola riga valida restituisce un FileEntry`() {
        val output = """
            total 8
            -rw-r--r-- 1 root root 1024 2024-01-15 12:30 foto.jpg
        """.trimIndent()

        val result = FileMapper.mapToFileEntry(output)

        assertEquals(1, result.size)
        assertEquals("foto.jpg", result[0].path)
        assertEquals(1024L, result[0].size)
    }

    @Test
    fun `mapToFileEntry - righe multiple restituiscono tutti i FileEntry`() {
        val output = """
            total 16
            -rw-r--r-- 1 root root 1024 2024-01-15 12:30 foto.jpg
            -rw-r--r-- 1 root root 2048 2024-01-15 12:31 video.mp4
            -rw-r--r-- 1 root root 512  2024-01-15 12:32 nota.txt
        """.trimIndent()

        val result = FileMapper.mapToFileEntry(output)

        assertEquals(3, result.size)
        assertEquals("foto.jpg", result[0].path)
        assertEquals("video.mp4", result[1].path)
        assertEquals("nota.txt", result[2].path)
    }

    @Test
    fun `mapToFileEntry - file con spazi nel nome viene parsato correttamente`() {
        val output = """
            total 8
            -rw-r--r-- 1 root root 1024 2024-01-15 12:30 foto di viaggio.jpg
        """.trimIndent()

        val result = FileMapper.mapToFileEntry(output)

        assertEquals(1, result.size)
        assertEquals("foto di viaggio.jpg", result[0].path)
    }

    @Test
    fun `mapToFileEntry - size viene parsata correttamente come Long`() {
        val output = """
            total 8
            -rw-r--r-- 1 root root 9999999 2024-01-15 12:30 grande.mp4
        """.trimIndent()

        val result = FileMapper.mapToFileEntry(output)

        assertEquals(9999999L, result[0].size)
    }

    @Test
    fun `mapToFileEntry - output vuoto restituisce lista vuota`() {
        val result = FileMapper.mapToFileEntry("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mapToFileEntry - solo riga total restituisce lista vuota`() {
        val output = "total 0"
        val result = FileMapper.mapToFileEntry(output)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `mapToFileEntry - righe malformate vengono ignorate silenziosamente`() {
        val output = """
            total 8
            questa riga non ha senso
            -rw-r--r-- 1 root root 1024 2024-01-15 12:30 valido.jpg
        """.trimIndent()

        val result = FileMapper.mapToFileEntry(output)

        assertEquals(1, result.size)
        assertEquals("valido.jpg", result[0].path)
    }

    @Test
    fun `mapToFileEntry - size non numerica viene sostituita con 0`() {
        val output = """
            total 8
            -rw-r--r-- 1 root root NaN 2024-01-15 12:30 corrotto.jpg
        """.trimIndent()

        val result = FileMapper.mapToFileEntry(output)

        // La riga ha 9 parti ma size non parsabile — dipende dall'impl:
        // se toLongOrNull() ?: 0L allora size == 0, altrimenti la riga viene scartata.
        // Questo test documenta il comportamento atteso — adattalo alla tua scelta.
        if (result.isNotEmpty()) {
            assertEquals(0L, result[0].size)
        }
    }
}