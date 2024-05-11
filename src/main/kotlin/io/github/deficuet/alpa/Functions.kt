package io.github.deficuet.alpa

import io.github.deficuet.alp.*
import io.github.deficuet.jimage.copy
import io.github.deficuet.jimage.flipY
import io.github.deficuet.jimage.pasteExtension
import io.github.deficuet.jimageio.savePng
import io.github.deficuet.unitykt.cast
import io.github.deficuet.unitykt.classes.Sprite
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.control.Spinner
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import tornadofx.*
import java.awt.Desktop
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.pathString
import javafx.scene.image.Image as ImageFX

class Functions(private val gui: MainView): Closeable {
    lateinit var continuation: TaskContinuation

    fun isTaskInitialized() = ::continuation.isInitialized

    fun analyzeFile(importFile: File): Boolean {
        if (::continuation.isInitialized) {
            continuation.close()
        }
        continuation = TaskContinuation(importFile)
        runBlockingFX(gui) {
            currentTaskString.value = "当前任务：${continuation.taskName}"
            errorString.value = ""
            dependenciesList.clear()
            imageComponentsList.clear()
            importImageVBox.isDisable = true
            importFaceBundleButton.isDisable = true
            importButtonZone.isDisable = false
            savePaintingButton.isDisable = true
            saveAllFaceButton.isDisable = true
            with(previewTabPane.tabs) {
                if (size > 1) remove(1, size)
            }
            mainPreviewImageView.image = initialPreview
        }
        val status = analyzePainting(
            importFile.toPath(),
            Path(configurations.assetSystemRoot),
            dependencies
        )
        if (status is DependencyMissing) {
            runBlockingFX(gui) {
                dependenciesList.addAll(status.dependencies.keys)
                dependenciesColumn.cellFormat {
                    text = it
                    textFill = when (status.dependencies[it]) {
                        true -> Color.BLUE
                        else -> errorTextFill
                    }
                }
            }
        }
        if (!status.succeed) {
            gui.reportBundleError(status.message)
            return false
        }
        status as PaintingAnalyzeStatus
        with(continuation) {
            result = status.result
            manager = status.manager
            width = status.result.width
            height = status.result.height
            groupedPainting = BufferedImage(
                result.width, result.height,
                BufferedImage.TYPE_4BYTE_ABGR
            )
            runBlockingFX(gui) {
                for ((i, rect) in status.result.stack.withIndex()) {
                    imageComponentsList.add(
                        when (rect.type) {
                            TextureType.PAINTING -> PaintingComponent(rect as PaintingTransform, i)
                            TextureType.FACE -> {
                                FaceComponent(rect, i).also {
                                    this@with.faceComponent = it
                                    importFaceBundleButton.isDisable = false
                                }
                            }
                        }
                    )
                }
            }
        }
        return true
    }

    fun autoImport() {
        val doAutoImport = with(configurations) { painting.autoImport || face.autoImport }
        if (doAutoImport) {
            var anySucceed = false
            for (component in gui.imageComponentsList) {
                when (component.rect.type) {
                    TextureType.PAINTING -> {
                        if (!configurations.painting.autoImport) continue
                        component as PaintingComponent
                        gui.showDebugInfo("自动导入：${component.rect.name}")
                        val file = configurations.painting.imageNamePattern.replace(
                            "{name}", component.rect.name
                        ).split(";").firstNotNullOfOrNull {
                            File("${configurations.painting.importImagePath}/${it.trim('*')}")
                                .takeIf { f -> f.exists() }
                        }
                        if (file == null) {
                            component.hasImportError = true
                            runBlockingFX(gui) { imageComponentsListView.refresh() }
                            continue
                        }
                        if (processPainting(file, component, false)) {
                            updatePaintingTab(component)
                            anySucceed = true
                        } else {
                            component.hasImportError = true
                            runBlockingFX(gui) { imageComponentsListView.refresh() }
                            continue
                        }
                    }
                    TextureType.FACE -> {
                        if (!configurations.face.autoImport) continue
                        component as FaceComponent
                        gui.showDebugInfo("自动导入：差分表情文件")
                        val fileName = continuation.taskName
                            .split('_', limit = 3)
                            .let { it.slice(0..it.lastIndex.coerceAtMost(1)) }
                            .joinToString("_")
                            .replace("_n", "")
                        val file = configurations.face.bundleNamePattern.let { pattern ->
                            pattern.replace("{name}", continuation.taskName)
                                .split(";").firstNotNullOfOrNull {
                                    File("${configurations.face.importBundlePath}/${it.trim('*')}")
                                        .takeIf { f -> f.exists() }
                                } ?: pattern.replace("{name}", fileName)
                                .split(";").firstNotNullOfOrNull {
                                    File("${configurations.face.importBundlePath}/${it.trim('*')}")
                                        .takeIf { f -> f.exists() }
                                }
                        }
                        if (file == null) {
                            component.hasImportError = true
                            runBlockingFX(gui) { imageComponentsListView.refresh() }
                            continue
                        }
                        val groupList = processFaceBundle(file, component, false)
                        if (groupList.isEmpty()) {
                            component.hasImportError = true
                            runBlockingFX(gui) { imageComponentsListView.refresh() }
                            continue
                        } else {
                            initFaceTab(component)
                            updateFaceTab(groupList, component)
                            anySucceed = true
                        }
                    }
                }
            }
            if (anySucceed) {
                gui.showDebugInfo("合并立绘中")
                groupPainting(gui.imageComponentsList[0], true)
            }
        }
        runBlockingFX(gui) {
            errorString.value = ""
            imageComponentsListView.selectionModel.select(0)
            importImageVBox.isDisable = false
        }
    }

    private fun getTabInsertedIndex(componentIndex: Int): Int {
        var index = 1
        for (i in 0 until componentIndex) {
            if (gui.imageComponentsList[i].isImported) {
                index++
            }
        }
        return index
    }

    fun importPainting(paintingName: String): File? {
        val wc = configurations.painting.imageNamePattern.replace("{name}", paintingName)
        val files = chooseFile(
            "导入立绘",
            arrayOf(
                FileChooser.ExtensionFilter("Required Files ($wc)", wc),
                FileChooser.ExtensionFilter("All Paintings (*.png)", "*.png")
            ),
            File(configurations.painting.importImagePath).withDefaultPath()
        )
        if (files.isEmpty()) return null
        gui.errorString.value = ""
        val f = files[0]
        configurations.painting.importImagePath = f.parent
        gui.paintingRootLabel.text = configurations.painting.importImagePath
        return f
    }

    fun processPainting(
        imageFile: File,
        component: PaintingComponent,
        showError: Boolean = true
    ): Boolean {
        val image: BufferedImage
        try {
            image = ImageIO.read(imageFile)
        } catch (e: Exception) {
            if (showError) gui.reportBundleError("导入的图像文件不可用")
            return false
        }
        val painting = decoratePainting(image.flipY().apply(), component.rect)
        with(component) {
            exhibit = image.createPreview(height = PREVIEW_HEIGHT - 62)
            this.image = painting
            isImported = true
            hasImportError = false
        }
        return true
    }

    fun updatePaintingTab(component: PaintingComponent) {
        if (!component.isPreviewTabInitialized()) {
            component.previewTabContent = PaintingPreviewTab(component)
            runBlockingFX(gui) {
                with(previewTabPane) {
                    tab(
                        getTabInsertedIndex(component.index),
                        component.rect.name,
                        component.previewTabContent.root
                    )
                }
            }
        } else {
            component.previewTabContent.previewImageView.image = component.exhibit
        }
        runBlockingFX(gui) {
            savePaintingButton.isDisable = false
            imageComponentsListView.refresh()
        }
    }

    fun importFaceImage(): List<File> {
        val files = chooseFile(
            "导入差分表情图片",
            arrayOf(
                FileChooser.ExtensionFilter(
                    "Required Files(${configurations.face.imageNamePattern})"
                        .replace("{name}", continuation.taskName),
                    configurations.face.imageNamePattern
                        .replace("{name}", continuation.taskName)
                ),
                FileChooser.ExtensionFilter(
                    "All Images (*.png)", "*.png"
                )
            ),
            File(configurations.face.importImagePath).withDefaultPath(),
            FileChooserMode.Multi
        )
        if (files.isEmpty()) return emptyList()
        gui.errorString.value = ""
        configurations.face.importImagePath = files[0].parent
        return files
    }

    fun processFaceImage(imageFiles: List<File>, component: FaceComponent): List<FaceImageGroup> {
        val groupList = mutableListOf<FaceImageGroup>()
        for (file in imageFiles) {
            val img: BufferedImage
            try {
                img = ImageIO.read(file)
            } catch (e: IOException) {
                continue
            }
            groupList.add(
                FaceImageGroup(
                    file.nameWithoutExtension,
                    decoratePaintingface(
                        img.flipY().apply(true),
                        component.rect,
                        continuation.result.isStacked
                    )
                )
            )
        }
        return groupList
    }

    fun importFaceBundle(): File? {
        val fileName = continuation.taskName
            .split('_', limit = 3)
            .let { it.slice(0..it.lastIndex.coerceAtMost(1)) }
            .joinToString("_")
            .replace("_n", "")
        val wcf = configurations.face.bundleNamePattern.replace("{name}", fileName)
        val wct = configurations.face.bundleNamePattern.replace("{name}", continuation.taskName)
        val files = chooseFile(
            "导入差分表情文件",
            arrayOf(
                FileChooser.ExtensionFilter("Required Files ($wcf)", wcf),
                FileChooser.ExtensionFilter("Required Files ($wct)", wct),
                FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            ),
            File(configurations.face.importBundlePath).withDefaultPath(
                Path(configurations.assetSystemRoot).resolve("paintingface").pathString
            )
        )
        if (files.isEmpty()) return null
        gui.errorString.value = ""
        val file = files[0]
        configurations.face.importBundlePath = file.parent
        gui.faceRootLabel.text = configurations.face.importBundlePath
        return file
    }

    fun processFaceBundle(
        bundle: File,
        component: FaceComponent,
        showError: Boolean = true
    ): List<FaceImageGroup> {
        val faceContext = try {
            continuation.manager.loadFile(bundle)
        } catch (e: Exception) {
            if (showError) gui.reportBundleError("导入差分表情文件时出错")
            return emptyList()
        }
        if (faceContext.objectList.isEmpty()) {
            if (showError) gui.reportBundleError("差分表情文件不可用")
            return emptyList()
        }
        val sprites = faceContext.objectList.filterIsInstance<Sprite>()
        if (sprites.isEmpty()) {
            if (showError) gui.reportBundleError("差分表情文件不可用")
            return emptyList()
        }
        val groupList = mutableListOf<FaceImageGroup>()
        for (sprite in sprites) {
            groupList.add(
                FaceImageGroup(
                    sprite.mName,
                    decoratePaintingface(
                        getPaintingfaceImage(sprite),
                        component.rect, continuation.result.isStacked
                    )
                )
            )
        }
        return groupList
    }

    private fun resetFaceStatus() {
        for (group in continuation.faceComponent.faceImageGroupList) {
            group.changedFlags = 0b111
        }
    }

    fun initFaceTab(component: FaceComponent) {
        if (!component.isPreviewTabInitialized()) {
            component.previewTabContent = FacePreviewTab(component)
            runBlockingFX(gui) {
                with(previewTabPane) {
                    component.previewTab = tab(
                        getTabInsertedIndex(component.index),
                        component.rect.name,
                        component.previewTabContent.root
                    ).apply {
                        whenSelected {
                            with(component.previewTabContent) {
                                if (!updateModeEnabled) {
                                    previewImageView.image = getFaceLocalExhibit()
                                }
                            }
                        }
                    }
                }
            }
        } else {
            component.previewTabContent.previewImageView.image = initialFacePreview
        }
        runBlockingFX(gui) {
            component.faceImageGroupList.clear()
        }
    }

    fun updateFaceTab(groupList: List<FaceImageGroup>, component: FaceComponent) {
        with(component) {
            isImported = true
            hasImportError = false
        }
        runBlockingFX(gui) {
            with(component) {
                faceImageGroupList.addAll(groupList)
                previewTabContent.faceImageGroupListView.selectionModel.select(0)
                previewTabContent.previewImageView.image = getFaceLocalExhibit()
            }
            savePaintingButton.isDisable = false
            saveAllFaceButton.isDisable = false
            imageComponentsListView.refresh()
        }
    }

    private fun updateSpinners() {
        for (component in gui.imageComponentsList) {
            if (!component.isImported) continue
            when (component.rect.type) {
                TextureType.PAINTING -> {
                    component as PaintingComponent
                    with(component.previewTabContent.spinnerX.valueFactory.cast<IntegerSpinnerValueFactory>()) {
                        min = -component.rect.pastePoint.x.toInt() - component.image.width
                        max = continuation.width - component.rect.pastePoint.x.toInt()
                    }
                    with(component.previewTabContent.spinnerY.valueFactory.cast<IntegerSpinnerValueFactory>()) {
                        min = -component.rect.pastePoint.y.toInt() - component.image.height
                        max = continuation.height - component.rect.pastePoint.y.toInt()
                    }
                }
                TextureType.FACE -> {
                    component as FaceComponent
                    val faceImage = component.faceImageGroupList[0].faceImage
                    with(component.previewTabContent.spinnerX.valueFactory.cast<IntegerSpinnerValueFactory>()) {
                        min = -component.rect.pastePoint.x.toInt() - faceImage.width
                        max = continuation.width - component.rect.pastePoint.x.toInt()
                    }
                    with(component.previewTabContent.spinnerY.valueFactory.cast<IntegerSpinnerValueFactory>()) {
                        min = -component.rect.pastePoint.y.toInt() - faceImage.height
                        max = continuation.height - component.rect.pastePoint.y.toInt()
                    }
                }
            }
        }
    }

    fun groupPainting(
        changedComponent: Component,
        previewEachStep: Boolean,
        forcedInit: Boolean = false
    ) {
        val faceComponent = continuation.faceComponent
        val isFaceImported = faceComponent.isImported
        if (forcedInit || !isFaceImported || changedComponent.index < continuation.faceComponent.index) {
            continuation.groupedPainting = BufferedImage(
                continuation.result.width,
                continuation.result.height,
                BufferedImage.TYPE_4BYTE_ABGR
            )
            for (component in gui.imageComponentsList) {
                if (component.rect.type == TextureType.FACE) {
                    when (component.isImported) {
                        true -> break
                        else -> continue
                    }
                }
                component as PaintingComponent
                if (!component.isImported) {
                    continue
                }
                continuation.groupedPainting = continuation.groupedPainting.pasteExtension(
                    component.image,
                    component.rect.pastePoint.x.toInt() + component.offsetX,
                    component.rect.pastePoint.y.toInt() + component.offsetY
                )
                if (previewEachStep) {
                    gui.mainPreviewImageView.image = continuation.groupedPainting.flipY().apply().createPreview()
                }
            }
            if (!isFaceImported && !previewEachStep) {
                gui.mainPreviewImageView.image = continuation.groupedPainting.flipY().apply().createPreview()
            }
        }
        if (isFaceImported) {
            resetFaceStatus()
            when (changedComponent.rect.type) {
                TextureType.PAINTING -> {
                    when (previewEachStep) {
                        true -> getFaceGroupedPainting(true)
                        else -> gui.mainPreviewImageView.image = getFaceGlobalExhibit()
                    }
                }
                TextureType.FACE -> {
                    continuation.faceComponent.previewTabContent.previewImageView.image = getFaceLocalExhibit()
                }
            }
        } else {
            with(continuation) {
                width = groupedPainting.width
                height = groupedPainting.height
            }
            updateSpinners()
        }
    }

    @Synchronized
    fun getFaceGroupedPainting(
        previewEachStep: Boolean,
        groupParam: FaceImageGroup? = null
    ): BufferedImage {
        val faceComponent = continuation.faceComponent
        val group = groupParam ?: faceComponent.previewTabContent.faceImageGroupListView.selectionModel.selectedItem
        if (group.changedFlags.and(0b001) > 0) {
            group.groupedPainting = with(faceComponent) {
                continuation.groupedPainting.copy().pasteExtension(
                    group.faceImage,
                    rect.pastePoint.x.toInt() + offsetX,
                    rect.pastePoint.y.toInt() + offsetY
                )
            }
            if (previewEachStep) {
                gui.mainPreviewImageView.image = group.groupedPainting.flipY().apply().createPreview()
            }
            for (i in (faceComponent.index + 1) until gui.imageComponentsList.size) {
                val component = gui.imageComponentsList[i] as PaintingComponent
                if (!component.isImported) {
                    continue
                }
                with(component) {
                    group.groupedPainting = group.groupedPainting.pasteExtension(
                        image,
                        rect.pastePoint.x.toInt() + offsetX,
                        rect.pastePoint.y.toInt() + offsetY
                    )
                }
                if (previewEachStep) {
                    gui.mainPreviewImageView.image = group.groupedPainting.flipY().apply().createPreview()
                }
            }
            group.changedFlags = group.changedFlags.and(0b110)
            if (previewEachStep) {
                group.globalExhibit = gui.mainPreviewImageView.image
                group.changedFlags = group.changedFlags.and(0b101)
            }
            with(continuation) {
                width = group.groupedPainting.width
                height = group.groupedPainting.height
                updateSpinners()
            }
        }
        return group.groupedPainting
    }

    @Synchronized
    fun getFaceGlobalExhibit(): ImageFX {
        val group = continuation.faceComponent.previewTabContent
            .faceImageGroupListView.selectionModel.selectedItem
        if (group.changedFlags.and(0b010) > 0) {
            group.globalExhibit = getFaceGroupedPainting(false)
                .flipY().apply().createPreview()
            group.changedFlags = group.changedFlags.and(0b101)
        }
        return group.globalExhibit
    }

    @Synchronized
    fun getFaceLocalExhibit(): ImageFX {
        val faceComponent = continuation.faceComponent
        val group = faceComponent.previewTabContent
            .faceImageGroupListView.selectionModel.selectedItem
        if (group.changedFlags.and(0b100) > 0) {
            val p = getFaceGroupedPainting(false)
            group.localExhibit = with(faceComponent) {
                p.getSubimage(
                    (rect.pastePoint.x.toInt() + offsetX - 32).coerceAtLeast(0),
                    (rect.pastePoint.y.toInt() + offsetY - 32).coerceAtLeast(0),
                    (group.faceImage.width + 64)
                        .coerceAtMost(continuation.width - rect.pastePoint.x.toInt() - offsetX),
                    (group.faceImage.height + 64)
                        .coerceAtMost(continuation.height - rect.pastePoint.y.toInt() - offsetY)
                )
            }.flipY().apply().createPreview(height = FACE_PREVIEW_HEIGHT)
            group.changedFlags = group.changedFlags.and(0b011)
        }
        return group.localExhibit
    }

    inner class PaintingPreviewTab(private val component: PaintingComponent): View(component.rect.name) {
        var previewImageView: ImageView by singleAssign()
        var spinnerX: Spinner<Int> by singleAssign()
        var spinnerY: Spinner<Int> by singleAssign()

        override val root = vbox {
            previewImageView = imageview(component.exhibit)
            hbox {
                alignment = Pos.CENTER
                vboxConstraints { marginTop = 16.0 }
                label("横向偏移：")
                with(component) {
                    spinnerX = spinner(initialValue = offsetX, editable = true) {
                        tooltip("正方向为右方")
                        valueProperty().addListener(
                            ChangeListener { _, _, newValue ->
                                if (newValue == null) {
                                    valueFactory.value = 0
                                }
                            }
                        )
                    }
                }
                label("纵向偏移：") {
                    hboxConstraints { marginLeft = 16.0 }
                }
                with(component) {
                    spinnerY = spinner(initialValue = offsetY, editable = true) {
                        tooltip("正方向为上方")
                        valueProperty().addListener(
                            ChangeListener { _, _, newValue ->
                                if (newValue == null) {
                                    valueFactory.value = 0
                                }
                            }
                        )
                    }
                }
                button("重新合并") {
                    minWidth = 80.0; minHeight = 30.0
                    tooltip("将重新计算坐标并制图")
                    hboxConstraints { marginLeft = 16.0 }
                    action {
                        gui.setUpdateMode(true)
                        runAsync {
                            with(component) {
                                offsetX = spinnerX.value
                                offsetY = spinnerY.value
                            }
                            groupPainting(component, false)
                            gui.previewTabPane.selectionModel.select(0)
                            gui.setUpdateMode(false)
                        }
                    }
                }
            }
        }
    }

    inner class FacePreviewTab(private val component: FaceComponent): View("\$face\$") {
        var previewImageView: ImageView by singleAssign()
        var faceImageGroupListView: ListView<FaceImageGroup> by singleAssign()
        var spinnerX: Spinner<Int> by singleAssign()
        var spinnerY: Spinner<Int> by singleAssign()

        override val root = vbox {
            previewImageView = imageview(initialFacePreview)
            hbox {
                alignment = Pos.CENTER
                vboxConstraints { marginTop = 16.0 }
                faceImageGroupListView = listview(component.faceImageGroupList) {
                    maxHeight = 109.0
                    style {
                        borderColor += box(Color.grayRgb(200))
                    }
                    cellFormat {
                        text = it.name
                    }
                    onUserSelectModified {
                        previewImageView.image = getFaceLocalExhibit()
                    }
                    selectionModel.selectionMode = SelectionMode.SINGLE
                }
                separator(Orientation.VERTICAL) {
                    hboxConstraints { margin = Insets(0.0, 4.0, 0.0, 8.0) }
                }
                vbox {
                    hbox {
                        alignment = Pos.CENTER_LEFT
                        label("横向偏移：")
                        with(component) {
                            spinnerX = spinner(initialValue = offsetX, editable = true) {
                                tooltip("正方向为右方")
                                valueProperty().addListener(
                                    ChangeListener { _, _, newValue ->
                                        if (newValue == null) {
                                            valueFactory.value = 0
                                        }
                                    }
                                )
                            }
                        }
                    }
                    hbox {
                        alignment = Pos.CENTER_LEFT
                        vboxConstraints { marginTop = 16.0 }
                        label("纵向偏移：")
                        with(component) {
                            spinnerY = spinner(initialValue = offsetY, editable = true) {
                                tooltip("正方向为上方")
                                valueProperty().addListener(
                                    ChangeListener { _, _, newValue ->
                                        if (newValue == null) {
                                            valueFactory.value = 0
                                        }
                                    }
                                )
                            }
                        }
                    }
                    button("重新合并") {
                        minWidth = 80.0; minHeight = 30.0
                        vboxConstraints {
                            marginTop = 16.0
                        }
                        action {
                            gui.setUpdateMode(true)
                            runAsync {
                                resetFaceStatus()
                                with(component) {
                                    offsetX = spinnerX.value
                                    offsetY = spinnerY.value
                                }
                                previewImageView.image = getFaceLocalExhibit()
                                gui.setUpdateMode(false)
                            }
                        }
                    }
                }
            }
        }
    }

    fun saveGroupedPainting() {
        if (continuation.faceComponent.isImported) {
            getFaceGroupedPainting(false).flipY().apply().savePng(
                File(configurations.painting.importImagePath)
                    .resolve("${continuation.taskName}_exp.png"),
                configurations.pngCompressionLevel
            )
        } else {
            continuation.groupedPainting.flipY().apply().savePng(
                File(configurations.painting.importImagePath)
                    .resolve("${continuation.taskName}_group.png"),
                configurations.pngCompressionLevel
            )
        }
    }

    fun saveAllFacePainting() {
        val groupList = continuation.faceComponent.faceImageGroupList
        for ((i, group) in groupList.withIndex()) {
            gui.showDebugInfo("保存进度：${i+1} / ${groupList.size}")
            var fileName = configurations.painting.importImagePath +
                    "/${continuation.taskName}_exp_${group.name}"
            if (File("${fileName}.png").exists()) {
                fileName = generateFileName(fileName)
            }
            getFaceGroupedPainting(false, group).flipY().apply().savePng(
                File("${fileName}.png"),
                configurations.pngCompressionLevel
            )
        }
        runBlockingFX(gui) { errorString.value = "" }
    }

    fun openFolder() {
        val dest = File(configurations.painting.importImagePath)
        if (dest.exists() && dest.isDirectory) {
            Desktop.getDesktop().open(dest)
        } else {
            gui.reportBundleError("目标文件夹不存在")
        }
    }

    override fun close() {
        if (::continuation.isInitialized) {
            continuation.close()
        }
    }

    companion object {
        fun importPaintingRoot(): File? {
            val folder = chooseDirectory(
                "选择文件夹",
                File(configurations.painting.importImagePath).withDefaultPath()
            ) ?: return null
            configurations.painting.importImagePath = folder.absolutePath
            return folder
        }

        fun importFaceBundleRoot(): File? {
            val folder = chooseDirectory(
                "选择文件夹",
                File(configurations.face.importBundlePath).withDefaultPath()
            ) ?: return null
            configurations.face.importBundlePath = folder.absolutePath
            return folder
        }

        fun importAssetSystemRoot(): File? {
            val folder = chooseDirectory(
                "选择文件夹",
                configurations.assetSystemRoot.let {
                    File(it).withDefaultPath()
                }
            ) ?: return null
            configurations.assetSystemRoot = folder.absolutePath
            return folder
        }

        fun importMainFile(): File? {
            val files = chooseFile(
                "选择文件", allTypeFilter,
                File(configurations.importMainBundlePath).withDefaultPath(
                    Path(configurations.assetSystemRoot).resolve("painting").pathString
                )
            )
            if (files.isEmpty()) return null
            val file = files[0]
            configurations.importMainBundlePath = file.parent
            return file
        }
    }
}
