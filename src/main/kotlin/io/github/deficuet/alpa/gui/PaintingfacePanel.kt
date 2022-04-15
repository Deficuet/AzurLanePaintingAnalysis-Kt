package io.github.deficuet.alpa.gui

import io.github.deficuet.alpa.function.PaintingfaceFunctions
import io.github.deficuet.alpa.utils.*
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import tornadofx.*

class PaintingfacePanel: PanelTemplate("差分表情") {
    override val functions = PaintingfaceFunctions(this)
    var importFromFileButton: Button by singleAssign()
    var importFromImageButton: Button by singleAssign()
    val needMergeString = SimpleStringProperty("N/A")
    var needMergeLabel: Label by singleAssign()
    var spinnerX: Spinner<Int> by singleAssign()
    var spinnerY: Spinner<Int> by singleAssign()
    var reMergeButton: Button by singleAssign()
    val paintingfaceFileErrorString = SimpleStringProperty()
    var paintingfaceFileErrorLabel: Label by singleAssign()
    var localPreviewImageView: ImageView by singleAssign()

    init {
        with(importFileZone) {
            vbox {
                vboxConstraints {
                    marginTop = 16.0; marginLeft = 8.0
                    marginRight = 8.0; marginBottom = 8.0
                    minWidth = 358.0; minHeight = 149.0
                }
                hbox {
                    label("是否需要合并：")
                    needMergeLabel = label(needMergeString)
                }
                hbox {
                    vboxConstraints { marginTop = 24.0 }
                    alignment = Pos.CENTER_LEFT
                    label("横向偏移：")
                    spinnerX = spinner(initialValue = 0, editable = true) {
                        isDisable = true
                        tooltip("正方向为右方")
                    }
                }
                hbox {
                    vboxConstraints { marginTop = 16.0 }
                    alignment = Pos.CENTER_LEFT
                    label("纵向偏移：")
                    spinnerY = spinner(initialValue = 0, editable = true) {
                        isDisable = true
                        tooltip("正方向为上方")
                    }
                }
                hbox {
                    vboxConstraints { marginTop = 16.0 }
                    alignment = Pos.CENTER_LEFT
                    reMergeButton = button("重新计算") {
                        isDisable = true
                        minWidth = 80.0; minHeight = 30.0
                        action {
                            functions.mergePainting()
                        }
                    }
                    paintingfaceFileErrorLabel = label(paintingfaceFileErrorString) {
                        hboxConstraints { marginLeft = 16.0 }
                    }
                }
            }
        }
        importImageTitledPane.text = "导入差分表情"
        with(importImageButtonZone) {
            button("导入主立绘") {
                addClass(ImportButtonStyle.importButton)
                action {
                    isDisable = true
                    functions.importPainting()
                    isDisable = false
                }
            }
            importFromFileButton = button("导入差分 - 文件") {
                isDisable = true
                addClass(ImportButtonStyle.importButton)
                hboxConstraints { marginLeft = 16.0 }
                action {
                    isDisable = true
                    functions.importFaceFromFile()
                    isDisable = false
                }
            }
            importFromImageButton = button("导入差分 - 图片") {
                isDisable = true
                addClass(ImportButtonStyle.importButton)
                hboxConstraints { marginLeft = 16.0 }
                action {
                    isDisable = true
                    functions.importFaceFromImage()
                    isDisable = false
                }
            }
        }
        requiredImageListView.isDisable = true
        with(requiredImageListView) {
            onUserSelectModified {
                when (previewTabPane.selectionModel.selectedIndex) {
                    0 -> previewMainImageView.image = functions.continuation.childrenMergeInfoList[it].globalExhibit
                    1 -> localPreviewImageView.image = functions.continuation.childrenMergeInfoList[it].localExhibit
                }
            }
        }
        with(previewTabPane) {
            tab("局部预览") {
                localPreviewImageView = imageview(initialPreview)
            }
            isDisable = true
            selectionModel.selectedIndexProperty().addListener(
                ChangeListener { _, _, new ->
                    if (functions.continuation.childrenMergeInfoList.isNotEmpty()) {
                        when (new) {
                            0 -> previewMainImageView.image = functions.continuation.childrenMergeInfoList[
                                    requiredImageListView.selectionModel.selectedItem
                            ].globalExhibit
                            1 -> localPreviewImageView.image = functions.continuation.childrenMergeInfoList[
                                    requiredImageListView.selectionModel.selectedItem
                            ].localExhibit
                        }
                    }
                }
            )
        }
    }

    class ImportButtonStyle: Stylesheet() {
        init {
            importButton {
                minWidth = 109.px
                minHeight = 30.px
            }
        }

        companion object {
            val importButton by cssclass()
        }
    }
}