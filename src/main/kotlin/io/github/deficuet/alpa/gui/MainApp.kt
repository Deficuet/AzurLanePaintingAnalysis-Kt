package io.github.deficuet.alpa.gui

import io.github.deficuet.alpa.function.BackendFunctions
import io.github.deficuet.alpa.utils.*
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import net.mamoe.yamlkt.Yaml
import tornadofx.*

fun main() {
    launch<MainApp>()
}

class MainApp: App(MainPanel::class, PaintingfacePanel.ImportButtonStyle::class) {
    init {
        reloadStylesheetsOnFocus()
    }

    override fun start(stage: Stage) {
        with(stage) {
            isResizable = false
        }
        super.start(stage)
    }

    override fun stop() {
        configFile.writeText(Yaml.encodeToString(Configurations.serializer(), configurations))
        with(find(PaintingfacePanel::class)) {
            functions.close()
        }
        super.stop()
    }
}

class MainPanel: View("ALPA") {
    object Externals {
        var compressionLevelSpinner: Spinner<Int> by singleAssign()
        var paintingRootLabel: TextField by singleAssign()
        var importPaintingRootButton: Button by singleAssign()
    }

    private var operationPanel: TabPane by singleAssign()
    private var assetSystemRootLabel: TextField by singleAssign()

    override val root = vbox {
        titledpane("设置", collapsible = false) {
            vboxConstraints {
                marginTop = 16.0; marginLeft = 16.0; marginRight = 16.0
            }
            hbox {
                alignment = Pos.CENTER_LEFT
                label("保存图片压缩等级 (0-9)：")
                Externals.compressionLevelSpinner = spinner(
                    min = 0, max = 9, initialValue = configurations.outputCompressionLevel,
                    editable = true
                ) {
                    maxWidth = 100.0; minHeight = 24.0
                    tooltip(
                        "0为无压缩，设置为1即有显著压缩效果，" +
                        "一般设置为5~7，8和9在只能进一步减少一点点体积的同时要花费数倍的时间。" +
                        "不在意占用空间的话可以调低使保存更快"
                    )
                    valueProperty().addListener(
                        ChangeListener { _, _, new ->
                            configurations.outputCompressionLevel = new
                        }
                    )
                }
                separator(Orientation.VERTICAL) {
                    hboxConstraints {
                        margin = Insets(0.0, 4.0, 0.0, 8.0)
                    }
                }
                label("立绘根目录：")
                Externals.paintingRootLabel = textfield(configurations.painting.importPaintingPath) {
                    maxWidth = 150.0; minHeight = 24.0
                    isEditable = false
                }
                Externals.importPaintingRootButton = button("浏览...") {
                    hboxConstraints { marginLeft = 8.0 }
                    minWidth = 64.0; minHeight = 24.0
                    action {
                        isDisable = true
                        val folder = BackendFunctions.importPaintingRoot()
                        if (folder != null) {
                            Externals.paintingRootLabel.text = configurations.painting.importPaintingPath
                        }
                        isDisable = false
                    }
                }
                checkbox("自动寻找立绘") {
                    isSelected = configurations.painting.autoImport
                    hboxConstraints { marginLeft = 8.0 }
                    selectedProperty().addListener(
                        ChangeListener { _, _, new ->
                            configurations.painting.autoImport = new
                        }
                    )
                }
                separator(Orientation.VERTICAL) {
                    hboxConstraints {
                        margin = Insets(0.0, 4.0, 0.0, 8.0)
                    }
                }
                label("素材文件根目录：")
                assetSystemRootLabel = textfield(configurations.assetSystemRoot ?: "无") {
                    maxWidth = 150.0; minHeight = 24.0
                    isEditable = false
                }
                button("浏览...") {
                    hboxConstraints { marginLeft = 8.0 }
                    minWidth = 64.0; minHeight = 24.0
                    action {
                        isDisable = true
                        val folder = BackendFunctions.importAssetSystemRoot()
                        if (folder != null) {
                            operationPanel.isDisable = false
                            assetSystemRootLabel.text = configurations.assetSystemRoot
                        }
                        isDisable = false
                    }
                }
            }
        }
        operationPanel = tabpane {
            vboxConstraints { marginTop = 16.0 }
            isDisable = configurations.assetSystemRoot == null
            tabMinWidth = 60.0
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tab<PaintingPanel>()
            tab<PaintingfacePanel>()
        }
    }
}

abstract class PanelTemplate<M: MergeInfo>(name: String): View(name) {
    abstract val functions: BackendFunctions

    var importFileZone: VBox by singleAssign()
    val currentTaskString = SimpleStringProperty("当前任务：空闲中")
    var importFileButton: Button by singleAssign()
    val errorString = SimpleStringProperty()
    var importImageTitledPane: TitledPane by singleAssign()
    var importImageButtonZone: HBox by singleAssign()
    val requiredImageMergeInfoList = observableListOf<M>()
    var requiredImageListView: ListView<M> by singleAssign()
    var previewTabPane: TabPane by singleAssign()
    var previewMainImageView: ImageView by singleAssign()
    var saveButtonZone: HBox by singleAssign()
    var saveButton: Button by singleAssign()
    var openFolderButton: Button by singleAssign()
    private var errorLabel: Label by singleAssign()

    override val root = hbox {
        vbox {
            hboxConstraints {
                marginLeft = 16.0; marginTop = 16.0; marginBottom = 16.0
            }
            titledpane("分析文件") {
                isCollapsible = false
                importFileZone = vbox {
                    hbox {
                        vboxConstraints {
                            marginLeft = 8.0; marginTop = 8.0
                        }
                        importFileButton = button("导入文件") {
                            minWidth = 80.0; minHeight = 30.0
                            action {
                                isDisable = true
                                importImageButtonZone.isDisable = true
                                val file = functions.importFile()
                                if (file != null) {
                                    runAsync {
                                        if (functions.analyzeFile(file)) {
                                            functions.finishImport()
                                        }
                                        isDisable = false
                                        importImageButtonZone.isDisable = false
                                    }
                                } else {
                                    isDisable = false
                                    importImageButtonZone.isDisable = false
                                }
                            }
                        }
                        vbox {
                            hboxConstraints { marginLeft = 12.0 }
                            label(currentTaskString) {
                                vboxConstraints { marginTop = -2.0 }
                            }
                            errorLabel = label(errorString) {
                                vboxConstraints { marginTop = 4.0 }
                            }
                        }
                    }
                }
            }
            importImageTitledPane = titledpane {
                isCollapsible = false; isDisable = true
                vboxConstraints { marginTop = 16.0 }
                vbox {
                    importImageButtonZone = hbox {
                        vboxConstraints {
                            marginLeft = 8.0; marginTop = 8.0
                        }
                    }
                    requiredImageListView = listview(requiredImageMergeInfoList) {
                        vboxConstraints {
                            marginTop = 16.0; marginLeft = 8.0
                            marginRight = 8.0; marginBottom = 8.0
                            minWidth = 356.0; maxHeight = 148.0
                        }
                        style {
                            borderColor += box(
                                Color.rgb(200, 200, 200)
                            )
                        }
                    }
                }
            }
            hbox {
                vboxConstraints {
                    marginTop = 16.0
                }
                saveButtonZone = hbox {
                    saveButton = button("保存") {
                        minHeight = 30.0
                        isDisable = true
                        action {
                            saveButtonZone.isDisable = true
                            runAsync {
                                functions.saveMergedPainting()
                                saveButtonZone.isDisable = false
                            }
                        }
                    }
                }
                openFolderButton = button("打开保存文件夹") {
                    minHeight = 30.0
                    hboxConstraints { marginLeft = 16.0 }
                    action {
                        isDisable = true
                        functions.openFolder()
                        isDisable = false
                    }
                }
            }
        }
        previewTabPane = tabpane {
            style {
                borderColor += box(
                    Color.rgb(200, 200, 200)
                )
            }
            hboxConstraints {
                margin = Insets(16.0)
            }
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tab("总体预览") {
                previewMainImageView = imageview(initialPreview)
            }
        }
    }

    fun showDebugInfo(msg: String) {
        errorLabel.textFill = Color.BLUE
        errorString.value = msg
    }

    fun reportBundleError(msg: String = "AssetBundle不可用") {
        errorLabel.textFill = errorTextFill
        errorString.value = msg
    }
}