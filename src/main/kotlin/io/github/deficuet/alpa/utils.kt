package io.github.deficuet.alpa

import io.github.deficuet.unitykt.UnityAssetManager
import io.github.deficuet.unitykt.classes.AssetBundle
import io.github.deficuet.unitykt.classes.MonoBehaviour
import io.github.deficuet.unitykt.firstObjectOf
import io.github.deficuet.unitykt.pptr.getAs
import javafx.application.Platform
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.TableColumnHeader
import javafx.scene.input.InputEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.stage.FileChooser
import net.mamoe.yamlkt.Yaml
import tornadofx.View
import java.io.File
import java.util.concurrent.FutureTask
import kotlin.io.path.Path
import javafx.scene.paint.Color as ColorFX

fun File.withDefaultPath(defaultPath: String = "C:/Users"): File {
    return if (exists() && isDirectory) this else File(defaultPath)
}

fun generateFileName(raw: String): String {
    var i = 1
    var fileName: String
    do {
        fileName = "${raw}#${i++}"
    } while (File("${fileName}.png").exists())
    return fileName
}

val allTypeFilter = arrayOf(
    FileChooser.ExtensionFilter("All types", "*.*")
)

val errorTextFill: ColorFX = ColorFX.rgb(187, 0, 17)

val configFile = File("alpa.yml")

val configurations = if (configFile.exists()) {
    Yaml.decodeFromString(Configurations.serializer(), configFile.readText())
} else {
    val default = Configurations()
    configFile.writeText(
        Yaml.encodeToString(Configurations.serializer(), default)
    )
    default
}

val dependencies: Map<String, List<String>> by lazy {
    UnityAssetManager.new().use { manager ->
        val depContext = manager.loadFile(Path(configurations.assetSystemRoot).resolve("dependencies"))
        val bundle = depContext.objectList.firstObjectOf<AssetBundle>()
        val mono = bundle.mContainer.values.first()[0].asset.getAs<MonoBehaviour>()
        mono.toTypeTreeJson()!!.let { json ->
            val keys = json.getJSONArray("m_Keys")
            val values = json.getJSONArray("m_Values")
            val table = mutableMapOf<String, List<String>>()
            for (i in 0 until keys.length()) {
                val key = keys.getString(i)
                val value = values.getJSONObject(i).getJSONArray("m_Dependencies").map { it.toString() }
                table[key] = value
            }
            table
        }
    }
}

/**
 * Modified [tornadofx.onUserSelect]
 */
fun <T> ListView<T>.onUserSelectModified(clickCount: Int = 1, action: (T) -> Unit) {
    val isSelectable = { event: InputEvent ->
        event.target.isValidRowModified() && !selectionModel.isEmpty
    }
    addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
        if (event.clickCount == clickCount && isSelectable(event)) {
            action(selectionModel.selectedItem)
        }
    }
    addEventFilter(KeyEvent.KEY_PRESSED) { event ->
        if (
            (event.code == KeyCode.UP || event.code == KeyCode.DOWN) &&
            !event.isMetaDown && isSelectable(event)
        ) {
            when (event.code) {
                KeyCode.UP -> action(
                    items[(selectionModel.selectedIndex - 1).coerceAtLeast(0)]
                )
                KeyCode.DOWN -> action(
                    items[(selectionModel.selectedIndex + 1).coerceAtMost(items.lastIndex)]
                )
                else -> Unit
            }
        }
    }
}

/**
 * Modified [tornadofx.isInsideRow]
 */
fun EventTarget.isValidRowModified(): Boolean {
    return when {
        this !is Node -> false
        this is TableColumnHeader -> false
        this is TableRow<*> -> !this.isEmpty
        this is TableView<*> || this is TreeTableRow<*> || this is ListView<*>
                || this is TreeTableView<*> || this is ListCell<*> -> true
        this.parent != null -> this.parent.isValidRowModified()
        else -> false
    }
}

fun <P: View, T> runBlockingFX(gui: P, task: P.() -> T): T? {
    return try {
        if (Platform.isFxApplicationThread()) {
            gui.task()
        } else {
            val future = FutureTask { gui.task() }
            Platform.runLater(future)
            future.get()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
