import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class searchForTextOccurrencesTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `collects occurrences from files recursively`() = runBlocking {
        val topLevel = Files.writeString(
            tempDir.resolve("top.txt"),
            """
                no hits here
                match on this line
                another match match
            """.trimIndent()
        )

        val nestedDir = Files.createDirectories(tempDir.resolve("nested"))
        val nestedFile = Files.writeString(nestedDir.resolve("nested.txt"), "match inside nested")

        val comparator = compareBy<Triple<Path, Int, Int>>({ it.first.toString() }, { it.second }, { it.third })
        val actual = searchForTextOccurrences("match", tempDir)
            .toList()
            .map { Triple(it.file, it.line, it.offset) }
            .sortedWith(comparator)

        val expected = listOf(
            Triple(topLevel, 2, 1),
            Triple(topLevel, 3, 9),
            Triple(topLevel, 3, 15),
            Triple(nestedFile, 1, 1)
        ).sortedWith(comparator)

        assertEquals(expected, actual)
    }

    @Test
    fun `fails fast on blank search string`() {
        assertFailsWith<IllegalArgumentException> {
            searchForTextOccurrences("   ", tempDir)
        }
    }

    @Test
    fun `skips malformed files without aborting search`() = runBlocking {
        val goodFile = Files.writeString(tempDir.resolve("good.txt"), "binary safe line\nsingle word here")
        Files.newOutputStream(tempDir.resolve("broken.bin")).use { out ->
            out.write(byteArrayOf(0xC3.toByte(), 0x28))
        }

        val actual = searchForTextOccurrences("word", tempDir)
            .toList()
            .map { Triple(it.file, it.line, it.offset) }

        val expected = listOf(
            Triple(goodFile, 2, 8)
        )

        assertEquals(expected, actual)
    }
}