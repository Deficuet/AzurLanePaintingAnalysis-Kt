package io.github.deficuet.alpa.function

import io.github.deficuet.alpa.gui.PaintingPanel
import io.github.deficuet.alpa.utils.*
import io.github.deficuet.unitykt.*
import io.github.deficuet.unitykt.data.*
import io.github.deficuet.unitykt.math.*
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import tornadofx.*
import javax.imageio.ImageIO
import kotlin.math.min

class PaintingFunctions(private val gui: PaintingPanel): BackendFunctions() {
    lateinit var continuation: PaintingTaskContinuation
    private lateinit var manager: AssetManager

    override fun importFile() {
        val files = chooseFile(
            "选择文件", allTypeFilter,
            File(configurations.painting.importFilesPath)
        )
        if (files.isEmpty()) return
        val file = files[0]
        if (::manager.isInitialized) manager.closeAll()
        if (::continuation.isInitialized) {
            continuation.childrenMergeInfoList.clear()
        }
        manager = AssetManager()
        continuation = PaintingTaskContinuation(file)
        configurations.painting.importFilesPath = continuation.importPath
        gui.currentTaskString.value = "当前任务：${continuation.taskName}"
        analyzeFile()
    }

    override fun analyzeFile() {
        with(gui) {
            requiredPaintingName.value = "目标名称：N/A"
            errorString.value = ""
            dependenciesList.clear()
            requiredImageNameList.clear()
            importImageTitledPane.isDisable = true
            saveButton.isDisable = true
            with(previewTabPane.tabs) {
                if (size > 1) remove(1, size)
            }
            previewMainImageView.image = initialPreview
        }
        val bundleContext = try {
            manager.loadFile(continuation.filePath)
        } catch (e: Exception) {
            return gui.reportBundleError()
        }
        if (bundleContext.objects.isEmpty()) {
            return gui.reportBundleError()
        }
        val bundle = bundleContext.objects.firstObjectOf<AssetBundle>()
        if (bundle.mDependencies.isNotEmpty()) {
            with(gui) {
                dependenciesList.addAll(bundle.mDependencies.map {
                    it.split('/').last()
                })
                dependenciesColumn.cellFormat {
                    text = it
                    textFill = if (Files.exists(Path.of("${continuation.importPath}/$it"))) {
                        Color.BLUE
                    } else {
                        errorTextFill
                    }
                }
            }
            if (gui.dependenciesList.any {
                !Files.exists(Path.of("${continuation.importPath}/$it"))
            }) {
                return gui.reportBundleError("依赖项缺失")
            }
        }
        val baseGameObject = bundle.mContainer[0].second.asset.getObj()
        if (baseGameObject == null || baseGameObject !is GameObject) {
            return gui.reportBundleError()
        }
        if (baseGameObject.mTransform.isEmpty()) {
            return gui.reportBundleError()
        }
        val rectList = mutableListOf<RectTransform>()
        val baseRect = baseGameObject.mTransform[0] as RectTransform
        rectList.add(baseRect)
        val baseChildren = baseRect.mChildren.getAllInstanceOf<RectTransform>()
        if (baseChildren.none { it.mGameObject.getObj()!!.mName in arrayOf("layers", "paint") }) {
            return gui.reportBundleError("无需合并")
        }
        continuation.baseMergeInfo = PaintingMergeInfo(baseRect)
        with(baseChildren) {
            filter {
                it.mGameObject.getObj()!!.mName == "layers"
            }.takeUnless {
                it.isEmpty()
            }?.forEach { layers ->
                val children = layers.mChildren.getAllInstanceOf<RectTransform>()
                rectList.addAll(children)
                val layersOrigin = (layers.mAnchorMax - layers.mAnchorMin) * baseRect.size * layers.mPivot +
                    layers.mAnchorMin * baseRect.size + layers.mAnchoredPosition -
                    layers.size * layers.mPivot * layers.mLocalScale.vector2
                children.forEach { child ->
                    val paste = ((child.mAnchorMax - child.mAnchorMin) * layers.size * child.mPivot +
                        child.mAnchorMin * layers.size + child.mAnchoredPosition + layersOrigin -
                        child.size * child.mPivot * child.mLocalScale.vector2).round() + Vector2(1.0, 1.0)
                    continuation.childrenMergeInfoList.add(
                        PaintingMergeInfo(child, child.mLocalScale.vector2, paste)
                    )
                }
            } ?: filter {
                it.mGameObject.getObj()!!.mName == "paint"
            }.forEach { layers ->
                rectList.add(layers)
                val paste = ((layers.mAnchorMax - layers.mAnchorMin) * baseRect.size * layers.mPivot +
                    layers.mAnchorMin * baseRect.size + layers.mAnchoredPosition -
                    layers.size * layers.mPivot * layers.mLocalScale.vector2).round() + Vector2(1.0, 1.0)
                continuation.childrenMergeInfoList.add(
                    PaintingMergeInfo(layers, layers.mLocalScale.vector2, paste)
                )
            }
        }
        val texManager = AssetManager().apply {
            loadFiles(
                *gui.dependenciesList
                    .filter { it.endsWith("_tex") }
                    .map { "${continuation.importPath}/$it" }
                    .toTypedArray()
            )
        }
        val requiredList = rectList.map { rect ->
            rect.mGameObject.getObj()!!.mComponents
                .map { manager.objects.objectFromPathID(it.mPathID)!! }
                .firstObjectOf<MonoBehaviour>()
                .json!!.getJSONObject("m_Sprite")
                .getLong("m_PathID")
                .let { pathID ->
                    texManager.objects.objectFromPathID(pathID)!!
                        .assetFile.root.name
                }.split("_tex")[0]
        }
        for ((i, mergeInfo) in continuation.mergeInfoList.withIndex()) {
            mergeInfo.name = requiredList[i]
        }
        gui.requiredImageNameList.addAll(requiredList)
        texManager.closeAll()
        with(continuation.extraPixel) {
            this[0] = continuation.mergeInfoList
                .minOf { it.pastePoint.x }
                .coerceAtMost(0.0)
                .absoluteValue.roundToInt()
            this[1] = continuation.mergeInfoList
                .minOf { it.pastePoint.y }
                .coerceAtMost(0.0)
                .absoluteValue.roundToInt()
            val v2 = continuation.childrenMergeInfoList.map { mi ->
                (mi.rect.size * mi.scale + mi.pastePoint - baseRect.size).round()
            }
            this[2] = v2.maxOf { it.x }.coerceAtLeast(0.0).roundToInt()
            this[3] = v2.maxOf { it.y }.coerceAtLeast(0.0).roundToInt()
        }
        for (childMI in continuation.childrenMergeInfoList) {
            childMI.pastePoint += with(continuation.extraPixel) {
                Vector2(this[0].toDouble(), this[1].toDouble())
            }
        }
        activeImport()
    }

    override fun activeImport() {
        with(gui) {
            importImageTitledPane.isDisable = false
            with(requiredImageListView) {
                selectionModel.select(0)
                requiredPaintingName.value = "目标名称：$selectedItem"
            }
        }
    }

    override fun importPainting() {
        val paintingName = gui.requiredImageListView.selectionModel.selectedItem
        val paintingIndex = gui.requiredImageListView.selectionModel.selectedIndex
        val wc = configurations.painting.wildcards.replace("{name}", paintingName)
        val mergeInfo = continuation.mergeInfoList[paintingIndex]
        val files = chooseFile("导入立绘",
            arrayOf(
                FileChooser.ExtensionFilter("Required Files ($wc)", wc),
                FileChooser.ExtensionFilter("All Paintings (*.png)", "*.png")
            ),
            File(configurations.painting.importPaintingPath)
        )
        if (files.isEmpty()) return
        val imageFile = files[0]
        configurations.painting.importPaintingPath = imageFile.parent
        val image = ImageIO.read(imageFile)
        val pre = with(mergeInfo) {
            val w = maxOf(rawSize.x.toInt(), image.width)
            val h = maxOf(rawSize.y.toInt(), image.height)
            val x = (maxOf(rect.size.x.roundToInt(), w) * scale.x).roundToInt()
            val y = (maxOf(rect.size.y.roundToInt(), h) * scale.y).roundToInt()
            BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR).paste(image.flipY(), 0, 0).resize(x, y)
        }
        val painting = if (paintingIndex == 0) {
            with(continuation.extraPixel) {
                val w = pre.width + this[0] + this[2]
                val h = pre.height + this[1] + this[3]
                val x = this[0]; val y = this[1]
                BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR).paste(pre, x, y)
            }
        } else pre
        with(mergeInfo) {
            exhibit = image
            this.image = painting
        }
        mergePainting()
        with(gui.previewTabPane) {
            val tab = Tab(paintingName).apply {
                vbox {
                    imageview(
                        mergeInfo.exhibit.createPreview(height = 478)
                    )
                    hbox {
                        isDisable = paintingIndex == 0
                        alignment = Pos.CENTER
                        vboxConstraints { marginTop = 16.0 }
                        label("横向偏移：")
                        with(mergeInfo) {
                            spinner(
                                -this.image.width - pastePoint.x.toInt(),
                                continuation.baseMergeInfo.image.width - pastePoint.x.toInt(),
                                offsetX, editable = true
                            ) {
                                tooltip("正方向为右方")
                                valueProperty().addListener(
                                    ChangeListener { _, _, newValue ->
                                        offsetX = newValue
                                    }
                                )
                            }
                            label("纵向偏移：") {
                                hboxConstraints { marginLeft = 16.0 }
                            }
                            spinner(
                                -this.image.height - pastePoint.y.toInt(),
                                continuation.baseMergeInfo.image.height - pastePoint.y.toInt(),
                                offsetY, editable = true
                            ) {
                                tooltip("正方向为上方")
                                valueProperty().addListener(
                                    ChangeListener { _, _, newValue ->
                                        offsetY = newValue
                                    }
                                )
                            }
                        }
                        button("重新合并") {
                            minWidth = 80.0; minHeight = 30.0
                            tooltip("将重新计算坐标并制图")
                            hboxConstraints { marginLeft = 16.0 }
                            action {
                                with(gui.previewTabPane) {
                                    selectionModel.select(0)
                                }
                                mergePainting()
                            }
                        }
                    }
                }
            }
            if (mergeInfo.displayTab != null) {
                tabs.remove(mergeInfo.displayTab)
            }
            tabs.add(min(tabs.size, paintingIndex + 1), tab)
            mergeInfo.displayTab = tab
        }
        with(gui) {
            previewTabPane.selectionModel.select(0)
            with(requiredImageListView.selectionModel) {
                select(selectedIndex + 1)
                requiredPaintingName.value = "目标名称：$selectedItem"
            }
        }
        if (continuation.mergeInfoList.all { it.isImported }) {
            gui.saveButton.isDisable = false
        }
    }

    override fun saveMergedPainting() {
        gui.importFileButton.isDisable = true
        gui.importImageButtonZone.isDisable = true
        continuation.mergedPainting.flipY().save(
            File("${configurations.painting.importPaintingPath}/${continuation.taskName}_group.png"),
            configurations.outputCompressionLevel
        )
        gui.importFileButton.isDisable = false
        gui.importImageButtonZone.isDisable = false
    }

    override fun mergePainting() {
        if (!continuation.baseMergeInfo.isImported) return
        continuation.mergedPainting = continuation.baseMergeInfo.image.copy().apply {
            if (continuation.childrenMergeInfoList[0].isImported) {
                for (mi in continuation.childrenMergeInfoList) {
                    if (!mi.isImported) break
                    paste(
                        mi.image,
                        mi.pastePoint.x.toInt() + mi.offsetX,
                        mi.pastePoint.y.toInt() + mi.offsetY
                    )
                }
            }
        }
        gui.previewMainImageView.image = continuation.mergedPainting.flipY().createPreview()
    }
}