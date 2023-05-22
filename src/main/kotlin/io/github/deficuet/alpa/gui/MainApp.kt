package io.github.deficuet.alpa.gui

import net.mamoe.yamlkt.Yaml
import io.github.deficuet.alpa.function.BackendFunctions
import io.github.deficuet.alpa.utils.*
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.stage.Stage
import tornadofx.*

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
        super.stop()
    }
}

class MainPanel: View("ALPA") {
    override val root = vbox {
        titledpane("全局设置", collapsible = false) {
            vboxConstraints {
                marginTop = 16.0; marginLeft = 16.0; marginRight = 16.0
            }
            hbox {
                alignment = Pos.CENTER_LEFT
                label("保存图片压缩等级：")
                spinner(
                    min = 0, max = 9, initialValue = configurations.outputCompressionLevel,
                    editable = true
                ) {
                    tooltip(
                        "一般设置为7，8和9在只能进一步减少一点点体积的同时要花费数倍的时间。" +
                                "不在意占用空间的话可以调低使保存更快"
                    )
                    valueProperty().addListener(
                        ChangeListener { _, _, new ->
                            configurations.outputCompressionLevel = new
                        }
                    )
                }
                checkbox("自动寻找立绘") {
                    isSelected = configurations.painting.autoImport
                    hboxConstraints { marginLeft = 30.0 }
                    selectedProperty().addListener(
                        ChangeListener { _, _, new ->
                            configurations.painting.autoImport = new
                        }
                    )
                }
            }
        }
        tabpane {
            vboxConstraints { marginTop = 16.0 }
            tabMinWidth = 60.0
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tab<PaintingPanel>()
            tab<PaintingfacePanel>()
        }
    }
}

abstract class PanelTemplate(name: String): View(name) {
    abstract val functions: BackendFunctions

    val currentTaskString = SimpleStringProperty("当前任务：空闲中")
    var importFileButton: Button by singleAssign()
    val errorString = SimpleStringProperty()
    var importImageTitledPane: TitledPane by singleAssign()
    var importImageButtonZone: HBox by singleAssign()
    val requiredImageNameList = observableListOf<String>()
    var requiredImageListView: ListView<String> by singleAssign()
    var previewTabPane: TabPane by singleAssign()
    var previewMainImageView: ImageView by singleAssign()
    var saveButton: Button by singleAssign()
    private var errorLabel: Label by singleAssign()
    protected var importFileZone: VBox by singleAssign()

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
                                functions.importFile()
                                functions.finishImport()
                                isDisable = false
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
                    requiredImageListView = listview(requiredImageNameList) {
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
                saveButton = button("保存") {
                    minWidth = 192.0; minHeight = 30.0
                    isDisable = true
                    action {
                        isDisable = true
                        runAsync {
                            functions.saveMergedPainting()
                            isDisable = false
                        }
                    }
                }
                button("打开文件夹") {
                    tooltip("打开导出文件夹")
                    hboxConstraints { marginLeft  = 14.0 }
                    minWidth = 192.0; minHeight = 30.0
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
                marginLeft = 16.0; marginRight = 16.0
                marginTop = 16.0; marginBottom = 16.0
            }
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tab("总体预览") {
                previewMainImageView = imageview(initialPreview)
            }
        }
    }

    fun showDebugInfo(msg: String) = Platform.runLater {
        errorLabel.textFill = Color.BLUE
        errorString.value = msg
    }

    fun reportBundleError(msg: String = "AssetBundle不可用") = Platform.runLater {
        errorLabel.textFill = errorTextFill
        errorString.value = msg
    }
}