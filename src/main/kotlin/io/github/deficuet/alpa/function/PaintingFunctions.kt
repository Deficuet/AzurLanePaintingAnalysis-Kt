package io.github.deficuet.alpa.function

import io.github.deficuet.alp.painting.AnalyzeStatusDep
import io.github.deficuet.alp.painting.PaintingAnalyzeStatus
import io.github.deficuet.alp.painting.analyzePainting
import io.github.deficuet.alp.painting.decoratePainting
import io.github.deficuet.alpa.gui.MainPanel
import io.github.deficuet.alpa.gui.PaintingPanel
import io.github.deficuet.alpa.utils.*
import io.github.deficuet.jimage.BufferedImage
import io.github.deficuet.jimage.copy
import io.github.deficuet.jimage.flipY
import io.github.deficuet.jimage.paste
import io.github.deficuet.jimageio.savePng
import javafx.geometry.Pos
import javafx.scene.control.Tab
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import tornadofx.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.math.roundToInt

class PaintingFunctions(private val gui: PaintingPanel): BackendFunctions() {
    private lateinit var continuation: PaintingTaskContinuation

    override fun importFile(): File? {
        val files = chooseFile(
            "选择文件", allTypeFilter,
            File(configurations.painting.importFilesPath).withDefaultPath()
        )
        if (files.isEmpty()) return null
        val file = files[0]
        configurations.painting.importFilesPath = file.parent
        return file
    }

    override fun analyzeFile(importFile: File): Boolean {
        if (::continuation.isInitialized) {
            continuation.close()
        }
        continuation = PaintingTaskContinuation(importFile)
        runBlockingFX(gui) {
            currentTaskString.value = "当前任务：${continuation.taskName}"
            requiredPaintingName.value = "目标名称：N/A"
            errorString.value = ""
            dependenciesList.clear()
            requiredImageMergeInfoList.clear()
            importImageTitledPane.isDisable = true
            saveButton.isDisable = true
            with(previewTabPane.tabs) {
                if (size > 1) remove(1, size)
            }
            previewMainImageView.image = initialPreview
        }
        val status = analyzePainting(importFile.toPath(), Path(configurations.assetSystemRoot!!))
        if (status is AnalyzeStatusDep) {
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
            runBlockingFX(gui) {
                reportBundleError(status.message)
            }
            return false
        }
        status as PaintingAnalyzeStatus
        with(continuation) {
            width = status.result.width
            height = status.result.height
            runBlockingFX(gui) {
                for (tr in status.result.transforms) {
                    requiredImageMergeInfoList.add(PaintingMergeInfo(tr, tr.fileName))
                }
            }
        }
        status.manager.close()
        enableImport()
        return true
    }

    override fun enableImport() {
        runBlockingFX(gui) {
            importImageTitledPane.isDisable = configurations.painting.autoImport
            with(requiredImageListView.selectionModel) {
                select(0)
                requiredPaintingName.value = "目标名称：${selectedItem.name}"
            }
            MainPanel.Externals.importPaintingRootButton.isDisable = configurations.painting.autoImport
        }
    }

    override fun finishImport() {
        if (configurations.painting.autoImport) {
            var failed = false
            for (mergeInfo in gui.requiredImageMergeInfoList) {
                runBlockingFX(gui) { showDebugInfo("自动导入：${mergeInfo.name}") }
                val file = configurations.painting.wildcards.replace("{name}", mergeInfo.name)
                    .split(";").map {
                        "${configurations.painting.importPaintingPath}/${it.trim('*')}"
                    }.firstNotNullOfOrNull { val f = File(it); if (f.exists()) f else null }
                if (file == null) {
                    runBlockingFX(gui) { reportBundleError("自动导入中断：找不到 ${mergeInfo.name}") }
                    failed = true
                    break
                }
                processPainting(
                    file, gui.requiredImageListView.selectionModel.selectedIndex,
                    mergeInfo
                )
            }
            runBlockingFX(gui) {
                if (!failed) errorString.value = ""
                importImageTitledPane.isDisable = false
                MainPanel.Externals.importPaintingRootButton.isDisable = false
            }
        }
    }

    override fun importPainting(): File? {
        val paintingName = gui.requiredImageListView.selectionModel.selectedItem.name
        val wc = configurations.painting.wildcards.replace("{name}", paintingName)
        val files = chooseFile(
            "导入立绘",
            arrayOf(
                FileChooser.ExtensionFilter("Required Files ($wc)", wc),
                FileChooser.ExtensionFilter("All Paintings (*.png)", "*.png")
            ),
            File(configurations.painting.importPaintingPath).withDefaultPath()
        )
        if (files.isEmpty()) return null
        gui.errorString.value = ""
        val f = files[0]
        configurations.painting.importPaintingPath = f.parent
        MainPanel.Externals.paintingRootLabel.text = configurations.painting.importPaintingPath
        return f
    }

    fun processPainting(imageFile: File, index: Int, mergeInfo: PaintingMergeInfo) {
        val image = ImageIO.read(imageFile)
        val painting = decoratePainting(image.flipY().apply(), mergeInfo.transform)
        with(mergeInfo) {
            exhibit = image.createPreview(height = 478)
            this.image = with(continuation) {
                if (index == 0 && (width > painting.width || height > painting.height)) {
                    BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR) {
                        drawImage(
                            painting,
                            mergeInfo.transform.pastePoint.x.toInt(),
                            mergeInfo.transform.pastePoint.y.toInt(),
                            null
                        )
                    }
                } else {
                    painting
                }
            }
            isImported = true
        }
        mergePainting()
        with(gui.previewTabPane) {
            val tab = Tab(mergeInfo.name).apply {
                vbox {
                    imageview(mergeInfo.exhibit)
                    hbox {
                        isDisable = index == 0
                        alignment = Pos.CENTER
                        vboxConstraints { marginTop = 16.0 }
                        with(mergeInfo) {
                            label("横向偏移：")
                            spinner(
                                -this.image.width - transform.pastePoint.x.toInt(),
                                with(gui.requiredImageMergeInfoList[0].transform) {
                                    maxOf(
                                        unscaledSize.x.roundToInt(),
                                        rawPaintingSize.x.toInt()
                                    ).times(overallScale.x).roundToInt()
                                } - transform.pastePoint.x.toInt(),
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
                                -this.image.height - transform.pastePoint.y.toInt(),
                                with(gui.requiredImageMergeInfoList[0].transform) {
                                    maxOf(
                                        unscaledSize.y.roundToInt(),
                                        rawPaintingSize.y.toInt()
                                    ).times(overallScale.y).roundToInt()
                                } - transform.pastePoint.y.toInt(),
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
                                with(gui) {
                                    importFileButton.isDisable = true
                                    importPaintingButton.isDisable = true
                                    saveButtonZone.isDisable = true
                                    previewTabPane.selectionModel.select(0)
                                }
                                runAsync {
                                    mergePainting()
                                    with(gui) {
                                        importFileButton.isDisable = false
                                        importPaintingButton.isDisable = false
                                        saveButtonZone.isDisable = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
            runBlockingFX(gui) {
                if (mergeInfo.displayTab != null) {
                    tabs.remove(mergeInfo.displayTab)
                }
                tabs.add(minOf(tabs.size, index + 1), tab)
            }
            mergeInfo.displayTab = tab
        }
        runBlockingFX(gui) {
            previewTabPane.selectionModel.select(0)
            with(requiredImageListView.selectionModel) {
                select(selectedIndex + 1)
                requiredPaintingName.value = "目标名称：${selectedItem.name}"
            }
        }
        if (gui.requiredImageMergeInfoList.all { it.isImported }) {
            gui.saveButton.isDisable = false
        }
    }

    override fun saveMergedPainting() {
        gui.importFileButton.isDisable = true
        gui.importImageButtonZone.isDisable = true
        continuation.mergedPainting.flipY().apply().savePng(
            File("${configurations.painting.importPaintingPath}/${continuation.taskName}_group.png"),
            configurations.outputCompressionLevel
        )
        gui.importFileButton.isDisable = false
        gui.importImageButtonZone.isDisable = false
    }

    override fun mergePainting() {
        if (!gui.requiredImageMergeInfoList[0].isImported) return
        continuation.mergedPainting = gui.requiredImageMergeInfoList[0].image.copy().apply {
            for (index in 1..gui.requiredImageMergeInfoList.lastIndex) {
                val mergeInfo = gui.requiredImageMergeInfoList[index]
                if (!mergeInfo.isImported) break
                paste(
                    mergeInfo.image,
                    mergeInfo.transform.pastePoint.x.toInt() + mergeInfo.offsetX,
                    mergeInfo.transform.pastePoint.y.toInt() + mergeInfo.offsetY
                )
            }
        }
        gui.previewMainImageView.image = continuation.mergedPainting.flipY().apply().createPreview()
    }
}