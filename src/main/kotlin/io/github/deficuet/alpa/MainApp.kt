package io.github.deficuet.alpa

import io.github.deficuet.alp.TextureType
import javafx.beans.property.SimpleObjectProperty
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
import javafx.util.Callback
import net.mamoe.yamlkt.Yaml
import tornadofx.*

fun main() {
    launch<MainApp>()
}

class MainApp: App(MainView::class) {
    override fun start(stage: Stage) {
        with(stage) {
            isResizable = false
        }
        super.start(stage)
    }

    override fun stop() {
        configFile.writeText(Yaml.encodeToString(Configurations.serializer(), configurations))
        with(find(MainView::class)) {
            functions.close()
        }
        super.stop()
    }
}

class MainView: View("ALPA") {
    private var compressionLevelSpinner: Spinner<Int> by singleAssign()
    private var autoImportPaintingCheckBox: CheckBox by singleAssign()
    private var autoImportFaceCheckBox: CheckBox by singleAssign()
    var paintingRootLabel: TextField by singleAssign()
    private var importPaintingRootButton: Button by singleAssign()
    var faceRootLabel: TextField by singleAssign()
    private var importFaceRootButton: Button by singleAssign()
    private var assetSystemRootLabel: TextField by singleAssign()
    private var importSysRootButton: Button by singleAssign()

    private var operationPanel: HBox by singleAssign()

    private var importFileButton: Button by singleAssign()
    val currentTaskString = SimpleStringProperty("当前任务：空闲中")
    private var errorLabel: Label by singleAssign()
    val errorString = SimpleStringProperty()
    val dependenciesList = observableListOf<String>()
    var dependenciesColumn: TableColumn<String, String> by singleAssign()

    var importImageVBox: VBox by singleAssign()
    var importButtonZone: HBox by singleAssign()
    var importFaceBundleButton: Button by singleAssign()
    val imageComponentsList = observableListOf<Component>()
    var imageComponentsListView: ListView<Component> by singleAssign()

    private var saveButtonZone: HBox by singleAssign()
    var savePaintingButton: Button by singleAssign()
    var saveAllFaceButton: Button by singleAssign()

    var previewTabPane: TabPane by singleAssign()
    var mainPreviewImageView: ImageView by singleAssign()

    val functions = Functions(this)
    var updateModeEnabled = false

    override val root = vbox {
        titledpane("设置", collapsible = false) {
            vboxConstraints { margin = Insets(16.0, 17.0, 0.0, 16.0) }
            hbox {
                paddingAll = 16.0
                vbox {
                    hbox {
                        alignment = Pos.CENTER_LEFT
                        label("保存图片压缩等级 (0-9)：")
                        compressionLevelSpinner = spinner(
                            min = 0, max = 9, initialValue = configurations.pngCompressionLevel,
                            editable = true
                        ) {
                            maxWidth = 100.0; minHeight = 24.0
                            tooltip("0为无压缩，设置为1即有显著压缩效果，推荐设置为5~7")
                            valueProperty().addListener(
                                ChangeListener { _, _, new ->
                                    configurations.pngCompressionLevel = new
                                }
                            )
                        }
                    }
                    hbox {
                        alignment = Pos.CENTER_LEFT
                        vboxConstraints { marginTop = 16.0 }
                        label("自动导入：")
                        autoImportPaintingCheckBox = checkbox("立绘") {
                            isSelected = configurations.painting.autoImport
                            selectedProperty().addListener(
                                ChangeListener { _, _, new ->
                                    configurations.painting.autoImport = new
                                }
                            )
                        }
                        autoImportFaceCheckBox = checkbox("差分表情文件") {
                            hboxConstraints { marginLeft = 16.0 }
                            isSelected = configurations.face.autoImport
                            selectedProperty().addListener(
                                ChangeListener { _, _, new ->
                                    configurations.face.autoImport = new
                                }
                            )
                        }
                    }
                }
                separator(Orientation.VERTICAL) {
                    hboxConstraints {
                        margin = Insets(0.0, 4.0, 0.0, 8.0)
                    }
                }
                gridpane {
                    vgap = 12.0
                    row {
                        label("立绘根目录：")
                        paintingRootLabel = textfield(
                            configurations.painting.importImagePath.takeIf {
                                it.isNotBlank()
                            } ?: "无"
                        ) {
                            minWidth = 246.0; minHeight = 24.0
                            isEditable = false
                        }
                        importPaintingRootButton = button("浏览...") {
                            gridpaneConstraints { marginLeft = 8.0 }
                            minWidth = 64.0; minHeight = 24.0
                            action {
                                isDisable = true
                                importFileButton.isDisable = true
                                Functions.importPaintingRoot()?.let {
                                    paintingRootLabel.text = configurations.painting.importImagePath
                                }
                                importFileButton.isDisable = false
                                isDisable = false
                            }
                        }
                    }
                    row {
                        label("差分表情根目录：")
                        faceRootLabel = textfield(
                            configurations.face.importBundlePath.takeIf {
                                it.isNotBlank()
                            } ?: "无"
                        ) {
                            minWidth = 246.0; minHeight = 24.0
                            isEditable = false
                        }
                        importFaceRootButton = button("浏览...") {
                            gridpaneConstraints { marginLeft = 8.0 }
                            minWidth = 64.0; minHeight = 24.0
                            action {
                                isDisable = true
                                importFileButton.isDisable = true
                                Functions.importFaceBundleRoot()?.let {
                                    faceRootLabel.text = configurations.face.importBundlePath
                                }
                                importFileButton.isDisable = false
                                isDisable = false
                            }
                        }
                    }
                }
                separator(Orientation.VERTICAL) {
                    hboxConstraints {
                        margin = Insets(0.0, 4.0, 0.0, 8.0)
                    }
                }
                gridpane {
                    vgap = 12.0
                    row {
                        label("素材文件根目录：")
                        assetSystemRootLabel = textfield(
                            configurations.assetSystemRoot.takeIf {
                                it.isNotBlank()
                            } ?: "无"
                        ) {
                            minWidth = 245.0; minHeight = 24.0
                            isEditable = false
                        }
                        importSysRootButton = button("浏览...") {
                            gridpaneConstraints { marginLeft = 8.0 }
                            minWidth = 64.0; minHeight = 24.0
                            action {
                                isDisable = true
                                importFileButton.isDisable = true
                                Functions.importAssetSystemRoot()?.let {
                                    operationPanel.isDisable = false
                                    assetSystemRootLabel.text = configurations.assetSystemRoot
                                }
                                importFileButton.isDisable = false
                                isDisable = false
                            }
                        }
                    }
                }
            }
        }
        operationPanel = hbox {
            paddingAll = 16.0
            isDisable = configurations.assetSystemRoot.isEmpty()
            vbox {
                titledpane("分析文件") {
                    isCollapsible = false
                    vbox {
                        paddingAll = 16.0
                        hbox {
                            importFileButton = button("导入文件") {
                                minWidth = 80.0; minHeight = 30.0
                                tooltip("右键按钮重载当前文件")
                                action {
                                    setImportMainMode(true)
                                    val bundle = Functions.importMainFile()
                                    if (bundle != null) {
                                        runAsync {
                                            if (functions.analyzeFile(bundle)) {
                                                functions.autoImport()
                                            }
                                            setImportMainMode(false)
                                        }
                                    } else {
                                        setImportMainMode(false)
                                    }
                                }
                                onRightClick {
                                    if (functions.isTaskInitialized()) {
                                        setImportMainMode(true)
                                        runAsync {
                                            if (functions.analyzeFile(functions.continuation.importFile)) {
                                                functions.autoImport()
                                            }
                                            setImportMainMode(false)
                                        }
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
                        tableview(dependenciesList) {
                            vboxConstraints { marginTop = 16.0 }
                            maxHeight = 130.0
                            selectionModel = null
                            dependenciesColumn = column("依赖项", String::class) {
                                minWidth = 350.0; maxWidth = 350.0; isSortable = false
                                cellValueFactory = Callback { SimpleObjectProperty(it.value) }
                            }
                        }
                    }
                }
                titledpane("导入图像") {
                    isCollapsible = false
                    vboxConstraints { marginTop = 16.0 }
                    importImageVBox = vbox {
                        isDisable = true
                        paddingAll = 16.0
                        importButtonZone = hbox {
                            alignment = Pos.CENTER_LEFT
                            button("添加图像") {
                                minWidth = 175.0; minHeight = 30.0
                                action {
                                    setImportImageMode(true)
                                    val component = imageComponentsListView.selectionModel.selectedItem
                                    when (component.rect.type) {
                                        TextureType.PAINTING -> {
                                            component as PaintingComponent
                                            val paintingFile = functions.importPainting(component.rect.name)
                                            if (paintingFile != null) {
                                                runAsync {
                                                    if (functions.processPainting(paintingFile, component)) {
                                                        previewTabPane.selectionModel.select(0)
                                                        functions.updatePaintingTab(component)
                                                        functions.groupPainting(component, false)
                                                        with(imageComponentsListView.selectionModel) {
                                                            select(selectedIndex + 1)
                                                        }
                                                    }
                                                    setImportImageMode(false)
                                                }
                                                return@action
                                            }
                                        }
                                        TextureType.FACE -> {
                                            component as FaceComponent
                                            val faceFiles = functions.importFaceImage()
                                            if (faceFiles.isNotEmpty()) {
                                                runAsync {
                                                    val groupList = functions.processFaceImage(faceFiles, component)
                                                    if (groupList.isNotEmpty()) {
                                                        val isImportedBefore = component.isImported
                                                        functions.initFaceTab(component)
                                                        previewTabPane.selectionModel.select(component.previewTab)
                                                        functions.updateFaceTab(groupList, component)
                                                        functions.groupPainting(
                                                            component, false, !isImportedBefore
                                                        )
                                                        with(imageComponentsListView.selectionModel) {
                                                            select(selectedIndex + 1)
                                                        }
                                                    }
                                                    setImportImageMode(false)
                                                }
                                                return@action
                                            }
                                        }
                                    }
                                    setImportImageMode(false)
                                }
                            }
                            importFaceBundleButton = button("添加差分表情文件") {
                                isDisable = true
                                hboxConstraints { marginLeft = 16.0 }
                                minWidth = 175.0; minHeight = 30.0
                                action {
                                    setImportImageMode(true)
                                    val faceBundle = functions.importFaceBundle()
                                    if (faceBundle != null) {
                                        val component = functions.continuation.faceComponent
                                        runAsync {
                                            val groupList = functions.processFaceBundle(faceBundle, component)
                                            if (groupList.isNotEmpty()) {
                                                val isImportedBefore = component.isImported
                                                functions.initFaceTab(component)
                                                previewTabPane.selectionModel.select(component.previewTab)
                                                functions.updateFaceTab(groupList, component)
                                                functions.groupPainting(
                                                    component, false, !isImportedBefore
                                                )
                                                val selectedIndex = imageComponentsListView
                                                    .selectionModel.selectedIndex
                                                if (selectedIndex == component.index) {
                                                    with(imageComponentsListView.selectionModel) {
                                                        select(selectedIndex + 1)
                                                    }
                                                }
                                            }
                                            setImportImageMode(false)
                                        }
                                    } else {
                                        setImportImageMode(false)
                                    }
                                }
                            }
                        }
                        imageComponentsListView = listview(imageComponentsList) {
                            vboxConstraints { marginTop = 16.0 }
                            maxWidth = 366.0; maxHeight = 169.0
                            style {
                                borderColor += box(Color.grayRgb(200))
                            }
                            cellFormat {
                                text = it.rect.name
                                textFill = if (it.hasImportError) {
                                    errorTextFill
                                } else {
                                    when (it.isImported) {
                                        true -> Color.BLUE
                                        else -> Color.BLACK
                                    }
                                }
                            }
                            selectionModel.selectionMode = SelectionMode.SINGLE
                        }
                    }
                }
                hbox {
                    vboxConstraints { marginTop = 16.0 }
                    saveButtonZone = hbox {
                        savePaintingButton = button("保存") {
                            minWidth = 123.0; minHeight = 30.0
                            isDisable = true
                            action {
                                setUpdateMode(true)
                                importPaintingRootButton.isDisable = true
                                compressionLevelSpinner.isDisable = true
                                runAsync {
                                    functions.saveGroupedPainting()
                                    setUpdateMode(false)
                                    importPaintingRootButton.isDisable = false
                                    compressionLevelSpinner.isDisable = false
                                }
                            }
                        }
                        saveAllFaceButton = button("为所有表情保存") {
                            hboxConstraints { marginLeft = 16.0 }
                            minWidth = 122.0; minHeight = 30.0
                            isDisable = true
                            action {
                                setUpdateMode(true)
                                importPaintingRootButton.isDisable = true
                                compressionLevelSpinner.isDisable = true
                                runAsync {
                                    functions.saveAllFacePainting()
                                    setUpdateMode(false)
                                    importPaintingRootButton.isDisable = false
                                    compressionLevelSpinner.isDisable = false
                                }
                            }
                        }
                    }
                    button("打开保存文件夹") {
                        hboxConstraints { marginLeft = 16.0 }
                        minWidth = 123.0; minHeight = 30.0
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
                    borderColor += box(Color.grayRgb(200))
                }
                hboxConstraints { marginLeft = 16.0 }
                tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
                tab("总体预览") {
                    mainPreviewImageView = imageview(initialPreview)
                    whenSelected {
                        if (!updateModeEnabled && functions.continuation.faceComponent.isImported) {
                            mainPreviewImageView.image = functions.getFaceGlobalExhibit()
                        }
                    }
                }
            }
        }
    }

    private fun setImportImageMode(enable: Boolean) {
        importImageVBox.isDisable = enable
        saveButtonZone.isDisable = enable
        importFileButton.isDisable = enable
        importPaintingRootButton.isDisable = enable
        importFaceRootButton.isDisable = enable
        previewTabPane.isDisable = enable
        updateModeEnabled = enable
    }

    fun setUpdateMode(enable: Boolean) {
        importFileButton.isDisable = enable
        importButtonZone.isDisable = enable
        saveButtonZone.isDisable = enable
        previewTabPane.isDisable = enable
        updateModeEnabled = enable
    }

    private fun setImportMainMode(enable: Boolean) {
        setUpdateMode(enable)
        importPaintingRootButton.isDisable = enable
        importFaceRootButton.isDisable = enable
        importSysRootButton.isDisable = enable
        autoImportPaintingCheckBox.isDisable = enable
        autoImportFaceCheckBox.isDisable = enable
    }

    fun showDebugInfo(msg: String) {
        runBlockingFX(this) {
            errorLabel.textFill = Color.BLUE
            errorString.value = msg
        }
    }

    fun reportBundleError(msg: String = "文件不可用") {
        runBlockingFX(this) {
            errorLabel.textFill = errorTextFill
            errorString.value = msg
        }
    }
}
