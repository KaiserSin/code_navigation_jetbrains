import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.nio.charset.MalformedInputException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

interface Occurrence {
    val file: Path
    val line: Int
    val offset: Int
}

private data class OccurrenceImpl(
    override val file: Path,
    override val line: Int,
    override val offset: Int
) : Occurrence


fun searchForTextOccurrences(
    stringToSearch: String,
    directory: Path,    
    numberOfThreads: Int = Runtime.getRuntime().availableProcessors()
): Flow<Occurrence>  {
    require(stringToSearch.isNotBlank()) { "Search string must not be blank" }
    val semaphore = Semaphore(numberOfThreads.coerceAtLeast(1))
    return channelFlow {
        Files.walk(directory).use { stream ->
            stream.filter(Files::isRegularFile).forEach { file ->
                ensureActive()
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        try {
                            searchInFile(stringToSearch, file).collect { (line, offset) ->
                                send(OccurrenceImpl(file, line, offset))
                            }
                        } catch (cause: MalformedInputException) {
                            // ignore files with invalid encoding
                        }
                    }
                }
            }
        }
    }
}

private fun searchInFile(
    stringToSearch: String,
    file: Path
): Flow<Pair<Int, Int>> {
    val queryLower = stringToSearch.lowercase(Locale.ROOT)
    return flow {
        val coroutineContext = currentCoroutineContext()
        Files.newBufferedReader(file).use { reader ->
            var lineNo = 0
            while (true) {
                coroutineContext.ensureActive()
                val line = reader.readLine() ?: break
                val lineLower = line.lowercase(Locale.ROOT)
                var fromIndex = 0
                while (true) {
                    coroutineContext.ensureActive()
                    val offset = lineLower.indexOf(queryLower, fromIndex)
                    if (offset == -1) break
                    emit(lineNo + 1 to offset + 1)
                    fromIndex = offset + 1
                }
                lineNo++
            }
        }
    }
}
