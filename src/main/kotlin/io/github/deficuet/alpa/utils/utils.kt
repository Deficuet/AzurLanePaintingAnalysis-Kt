package io.github.deficuet.alpa.utils

import io.github.deficuet.jimage.fancyBufferedImage
import javafx.application.Platform
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.TableColumnHeader
import javafx.scene.input.InputEvent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.stage.FileChooser.ExtensionFilter
import net.mamoe.yamlkt.Yaml
import tornadofx.View
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.FutureTask
import javafx.scene.paint.Color as ColorFX

val initialPreview = fancyBufferedImage(864, 540, BufferedImage.TYPE_INT_ARGB) {
    font = Font("aria", Font.BOLD, 54)
    color = Color(116, 116, 116)
    val p = fontMetrics.stringWidth("Preview")
    val n = fontMetrics.stringWidth("not")
    val a = fontMetrics.stringWidth("available")
    val y = 200
    drawString("Preview", (864 - p) / 2, y)
    drawString("not", (864 - n) / 2, y + fontMetrics.height)
    drawString("available", (864 - a) / 2, y + fontMetrics.height * 2)
}.toFXImage()

fun File.withDefaultPath(defaultPath: String = "C:/Users"): File {
    return if (exists()) this else File(defaultPath)
}

fun generateFileName(raw: String): String {
    var i = 1
    var fileName: String
    do {
        fileName = "$raw#$i"
        i++
    } while (Files.exists(Path.of("${fileName}.png")))
    return fileName
}

val allTypeFilter = arrayOf(
    ExtensionFilter("All types", "*.*")
)

val defaultConfigurations by lazy { Configurations() }

val configFile = File("alpa.yml")

val configurations = if (configFile.exists()) {
    Yaml.decodeFromString(Configurations.serializer(), configFile.readText())
} else {
    configFile.writeText(
        Yaml.encodeToString(Configurations.serializer(), defaultConfigurations)
    )
    defaultConfigurations
}

val errorTextFill: ColorFX = ColorFX.rgb(187, 0, 17)

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
