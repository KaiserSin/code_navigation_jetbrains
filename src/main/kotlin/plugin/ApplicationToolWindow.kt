package plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import searchForTextOccurrences
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.text.DefaultCaret

class ApplicationToolWindow : ToolWindowFactory {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var isRunning = false

    private fun validateDir(text: String): String? {
        if (text.isEmpty()) return "Enter path to directory"
        val path = Paths.get(text)
        if (!path.toFile().exists()) return "Directory not found"
        if (!Files.isDirectory(path)) return "Path must point to a directory"
        if (!Files.isReadable(path)) return "Cannot read directory"
        return null
    }
    private fun write(project: Project, stringToSearch: String, directory: Path) {
        if (job?.isActive == true) return
        ApplicationManager.getApplication().invokeLater { outputArea.text = "Search started\n" }
        job = scope.launch {
            try {
                searchForTextOccurrences(stringToSearch, directory, numberOfThreads = Runtime.getRuntime().availableProcessors())
                    .onEach { value ->
                        ensureActive()
                        val line = "${value.file.toAbsolutePath()}: ${value.line}:${value.offset}"
                        ApplicationManager.getApplication().invokeLater {
                            if (job?.isActive == true) {
                                outputArea.append("$line\n")
                            }
                        }
                    }
                    .onCompletion { cause ->
                        ApplicationManager.getApplication().invokeLater {
                            if (cause == null) {
                                outputArea.append("\nSearch completed")
                            }
                        }
                        isRunning = false
                    }
                    .collect()
            } catch (ex: Exception) {
                isRunning = false
                ApplicationManager.getApplication().invokeLater {
                    val message = ex.message ?: ex.javaClass.simpleName
                    outputArea.append("\nError: $message")
                    Messages.showErrorDialog(project, message, "Search error")
                }
            }
        }
    }

    private val outputArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        text = "Ready to search. Set directory and query then press Start search."
        (caret as? DefaultCaret)?.updatePolicy = DefaultCaret.ALWAYS_UPDATE
    }

    private val outputScrollPane = JBScrollPane(outputArea).apply {
        preferredSize = Dimension(800, 400)
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        lateinit var dirField: Cell<JTextField>
        lateinit var queryField: Cell<JTextField>
        val ui: JComponent = panel {
            
            group("Search settings") {
                row("Directory path") {
                    dirField = textField()
                        .comment("Absolute path to the directory for the search.")
                }
                row("String to search") {
                    queryField = textField()
                        .comment("Text that should be found in files.")
                }
                row {
                    button("Start search") {
                        if (isRunning) return@button
                        val dir = dirField.component.text.trim()
                        val query = queryField.component.text
                        val err = validateDir(dir)
                        if (err != null) {
                            Messages.showErrorDialog(project, err, "Invalid directory")
                            return@button
                        }
                        if (query.isBlank()) {
                            Messages.showErrorDialog(project, "Enter text to search", "Invalid query")
                            return@button
                        }
                        val directoryPath = Paths.get(dir)
                        isRunning = true
                        write(project, query, directoryPath)
                    }
                    button("Cancel search") {
                        job?.cancel()
                        isRunning = false
                        ApplicationManager.getApplication().invokeLater {
                            outputArea.append("\nSearch cancelled by user")
                        }
                    }
                }.comment("Start the search or cancel it at any time.")
                row("Output"){}
                row{
                    cell(outputScrollPane)
                        .align(Align.FILL)
                        .resizableColumn()
                        .comment("Search results appear here. New matches are appended automatically.")
                }.resizableRow()
            }
        }

        val content = ContentFactory.getInstance().createContent(ui, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
