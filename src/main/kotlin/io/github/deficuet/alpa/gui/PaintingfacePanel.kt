package io.github.deficuet.alpa.gui

import io.github.deficuet.alpa.function.PaintingfaceFunctions
import io.github.deficuet.alpa.utils.PaintingfaceTaskContinuation
import io.github.deficuet.alpa.utils.errorTextFill
import io.github.deficuet.alpa.utils.initialPreview
import io.github.deficuet.alpa.utils.onUserSelectModified
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Spinner
import javafx.scene.image.ImageView
import tornadofx.*

class PaintingfacePanel: PanelTemplate<PaintingfaceTaskContinuation.PaintingfaceMergeInfo>("差分表情") {
    override val functions = PaintingfaceFunctions(this)
    var importFromFileButton: Button by singleAssign()
    var importFromImageButton: Button by singleAssign()
    val needMergeString = SimpleStringProperty("N/A")
    var needMergeLabel: Label by singleAssign()
    var spinnerX: Spinner<Int> by singleAssign()
    var spinnerY: Spinner<Int> by singleAssign()
    var reMergeButton: Button by singleAssign()
    val paintingfaceFileErrorString = SimpleStringProperty()
    var localPreviewImageView: ImageView by singleAssign()
    var saveAllButton: Button by singleAssign()
    private var paintingfaceFileErrorLabel: Label by singleAssign()

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
                    reMergeButton = button("重新合并") {
                        isDisable = true
                        minWidth = 80.0; minHeight = 30.0
                        action {
                            importFileZone.isDisable = true
                            importImageTitledPane.isDisable = true
                            saveButtonZone.isDisable = true
                            runAsync {
                                functions.mergePainting()
                                importFileZone.isDisable = false
                                importImageTitledPane.isDisable = false
                                saveButtonZone.isDisable = false
                            }
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
                    importImageButtonZone.isDisable = true
                    importFileZone.isDisable = true
                    saveButtonZone.isDisable = true
                    val file = functions.importPainting()
                    if (file != null) {
                        runAsync {
                            functions.processPainting(file)
                            importImageButtonZone.isDisable = false
                            importFileZone.isDisable = false
                            saveButtonZone.isDisable = false
                        }
                    } else {
                        importImageButtonZone.isDisable = false
                        importFileZone.isDisable = false
                        saveButtonZone.isDisable = false
                    }
                }
            }
            importFromFileButton = button("导入差分 - 文件") {
                isDisable = true
                addClass(ImportButtonStyle.importButton)
                hboxConstraints { marginLeft = 16.0 }
                action {
                    importImageButtonZone.isDisable = true
                    importFileZone.isDisable = true
                    saveButtonZone.isDisable = true
                    val file = functions.importFaceFromFile()
                    if (file != null) {
                        runAsync {
                            requiredImageListView.isDisable = true
                            functions.processFaceFile(file)
                            importImageButtonZone.isDisable = false
                            importFileZone.isDisable = false
                            requiredImageListView.isDisable = false
                            saveButtonZone.isDisable = false
                        }
                    } else {
                        importImageButtonZone.isDisable = false
                        importFileZone.isDisable = false
                        saveButtonZone.isDisable = false
                    }
                }
            }
            importFromImageButton = button("导入差分 - 图片") {
                isDisable = true
                addClass(ImportButtonStyle.importButton)
                hboxConstraints { marginLeft = 16.0 }
                action {
                    importImageButtonZone.isDisable = true
                    importFileZone.isDisable = true
                    saveButtonZone.isDisable = true
                    val file = functions.importFaceFromImage()
                    if (file != null) {
                        runAsync {
                            requiredImageListView.isDisable = true
                            functions.processFaceImage(file)
                            importImageButtonZone.isDisable = false
                            importFileZone.isDisable = false
                            requiredImageListView.isDisable = false
                            saveButtonZone.isDisable = false
                        }
                    } else {
                        importImageButtonZone.isDisable = false
                        importFileZone.isDisable = false
                        saveButtonZone.isDisable = false
                    }
                }
            }
        }
        with(requiredImageListView) {
            isDisable = true
            cellFormat {
                text = it.name
            }
            onUserSelectModified {
                when (previewTabPane.selectionModel.selectedIndex) {
                    0 -> {
                        previewMainImageView.image = selectionModel.selectedItem.getGlobalExhibit()
                    }
                    1 -> {
                        localPreviewImageView.image = selectionModel.selectedItem.getLocalExhibit()
                    }
                }
            }
        }
        saveButton.minWidth = 122.0
        with(saveButtonZone) {
            saveAllButton = button("保存所有") {
                minWidth = 122.0; minHeight = 30.0
                hboxConstraints { marginLeft = 16.0 }
                isDisable = true
                action {
                    saveButtonZone.isDisable = true
                    runAsync {
                        functions.saveAllMergedPainting()
                        saveButtonZone.isDisable = false
                    }
                }
            }
        }
        openFolderButton.minWidth = 123.0
        with(previewTabPane) {
            tab("局部预览") {
                localPreviewImageView = imageview(initialPreview)
            }
            isDisable = true
            selectionModel.selectedIndexProperty().addListener(
                ChangeListener { _, _, new ->
                    if (requiredImageMergeInfoList.isNotEmpty()) {
                        when (new) {
                            0 -> previewMainImageView.image = requiredImageListView
                                .selectionModel.selectedItem.getGlobalExhibit()
                            1 -> localPreviewImageView.image = requiredImageListView
                                .selectionModel.selectedItem.getLocalExhibit()
                        }
                    }
                }
            )
        }
    }

    fun reportFaceBundleError(msg: String = "差分表情文件不可用") {
        paintingfaceFileErrorLabel.textFill = errorTextFill
        paintingfaceFileErrorString.value = msg
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