package io.github.deficuet.alpa.gui

import io.github.deficuet.alpa.function.PaintingFunctions
import io.github.deficuet.alpa.utils.*
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.util.Callback
import tornadofx.*

class PaintingPanel: PanelTemplate("立绘合并") {
    override val functions = PaintingFunctions(this)
    val dependenciesList = observableListOf<String>()
    var dependenciesColumn: TableColumn<String, String> by singleAssign()
    val requiredPaintingName = SimpleStringProperty("目标名称：N/A")

    init {
        with(importFileZone) {
            tableview(dependenciesList) {
                vboxConstraints {
                    marginTop = 16.0; marginLeft = 8.0
                    marginRight = 8.0; marginBottom = 8.0
                    minWidth = 356.0; maxHeight = 150.0
                }
                selectionModel = null
                dependenciesColumn = column("依赖项", String::class) {
                    minWidth = 357.0; maxWidth = 357.0; isSortable = false
                    cellValueFactory = Callback { SimpleObjectProperty(it.value) }
                }
            }
        }
        importImageTitledPane.text = "导入立绘"
        with(importImageButtonZone) {
            alignment = Pos.CENTER_LEFT
            button("添加立绘") {
                minWidth = 80.0; minHeight = 30.0
                action {
                    isDisable = true
                    functions.importPainting()
                    isDisable = false
                }
            }
            label(requiredPaintingName) {
                hboxConstraints { marginLeft = 12.0 }
            }
        }
        with(requiredImageListView) {
            cellFormat {
                text = it
                textFill = if (functions.continuation.mergeInfoList[it].isImported) {
                    Color.BLUE
                } else Color.BLACK
            }
            onUserSelectModified {
                requiredPaintingName.value = "目标名称：$it"
            }
        }
    }
}