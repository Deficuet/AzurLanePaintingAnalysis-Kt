package io.github.deficuet.alpa.utils

import net.mamoe.yamlkt.Yaml
import io.github.deficuet.unitykt.data.RectTransform
import io.github.deficuet.unitykt.getObj
import io.github.deficuet.unitykt.math.Vector2
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.TableColumnHeader
import javafx.scene.input.*
import javafx.stage.FileChooser.ExtensionFilter
import javafx.scene.paint.Color as ColorFX
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.round

val initialPreview = BufferedImage(864, 540, BufferedImage.TYPE_INT_ARGB) {
    setRenderingHints(mapOf(
        RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
        RenderingHints.KEY_STROKE_CONTROL to RenderingHints.VALUE_STROKE_NORMALIZE,
        RenderingHints.KEY_FRACTIONALMETRICS to RenderingHints.VALUE_FRACTIONALMETRICS_ON
    ))
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

val RectTransform.size: Vector2
    get() = if (mFather.isNull) {
        mSizeDelta
    } else {
        mSizeDelta + (mAnchorMax - mAnchorMin) * (mFather.getObj()!! as RectTransform).size
    }

val allTypeFilter = arrayOf(
    ExtensionFilter("All types", "*.*")
)

val defaultConfigurations by lazy {
    Configurations(
        painting = Configurations.PaintingSetting(
            importFilesPath = "C:/Users",
            importPaintingPath = "C:/Users",
            wildcards = "*{name}.png;*{name}_group.png;*{name}_exp.png",
            autoImport = false,
        ),
        paintingface = Configurations.PaintingfaceSetting(
            importFilesPath = "C:/Users",
            importFace2DPath = "C:/Users",
            importFaceFilePath = "C:/Users",
            imageWildcards = "?.png;{name}_head.png",
            fileWildcards = "{name}",
        ),
        outputCompressionLevel = 7
    )
}

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

fun Vector2.round() = Vector2(round(x), round(y))

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
