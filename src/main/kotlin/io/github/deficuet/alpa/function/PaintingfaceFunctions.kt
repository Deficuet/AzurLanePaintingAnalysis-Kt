package io.github.deficuet.alpa.function

import io.github.deficuet.alp.paintingface.PaintingfaceAnalyzeStatus
import io.github.deficuet.alp.paintingface.analyzePaintingface
import io.github.deficuet.alp.paintingface.scalePaintingface
import io.github.deficuet.alpa.gui.MainPanel
import io.github.deficuet.alpa.gui.PaintingfacePanel
import io.github.deficuet.alpa.utils.*
import io.github.deficuet.jimage.BufferedImage
import io.github.deficuet.jimage.flipY
import io.github.deficuet.jimageio.savePng
import io.github.deficuet.unitykt.classes.Sprite
import io.github.deficuet.unitykt.classes.SpriteCropStrategy
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import tornadofx.chooseFile
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.Path

class PaintingfaceFunctions(private val gui: PaintingfacePanel): BackendFunctions() {
    private lateinit var continuation: PaintingfaceTaskContinuation

    override fun importFile(): File? {
        val files = chooseFile(
            "选择文件", allTypeFilter,
            File(configurations.paintingface.importFilesPath).withDefaultPath()
        )
        if (files.isEmpty()) return null
        val file = files[0]
        configurations.paintingface.importFilesPath = file.parent
        return file
    }

    override fun analyzeFile(importFile: File): Boolean {
        if (::continuation.isInitialized) {
            continuation.close()
        }
        continuation = PaintingfaceTaskContinuation(importFile)
        runBlockingFX(gui) {
            currentTaskString.value = "当前任务：${continuation.taskName}"
            needMergeLabel.textFill = Color.BLACK
            needMergeString.value = "N/A"
            previewTabPane.selectionModel.select(0)
            previewMainImageView.image = initialPreview
            localPreviewImageView.image = initialPreview
            previewTabPane.isDisable = true
            spinnerX.isDisable = true
            spinnerX.valueFactory.value = 0
            spinnerY.isDisable = true
            spinnerY.valueFactory.value = 0
            reMergeButton.isDisable = true
            requiredImageMergeInfoList.clear()
            requiredImageListView.isDisable = true
            importFromFileButton.isDisable = true
            importFromImageButton.isDisable = true
            importImageTitledPane.isDisable = true
            saveButton.isDisable = true
            saveAllButton.isDisable = true
            paintingfaceFileErrorString.value = ""
        }
        val status = analyzePaintingface(importFile.toPath(), Path(configurations.assetSystemRoot!!))
        if (!status.succeed) {
            runBlockingFX(gui) {
                reportBundleError(status.message)
            }
            return false
        }
        status as PaintingfaceAnalyzeStatus
        runBlockingFX(gui) {
            if (status.requiresMerge) {
                needMergeLabel.textFill = errorTextFill
                needMergeString.value = "需要"
            } else {
                needMergeLabel.textFill = Color.web("#248C18")
                needMergeString.value = "不需要"
            }
        }
        with(continuation) {
            manager = status.manager
            baseMergeInfo = PaintingfaceMergeInfo(status.result.transforms[0], "")
            faceTransform = status.result.transforms[1]
            width = status.result.width
            height = status.result.height
        }
        enableImport()
        return true
    }

    override fun enableImport() {
        with(gui) {
            importImageTitledPane.isDisable = false
        }
    }

    override fun importPainting(): File? {
        val wc = configurations.painting.wildcards.replace("{name}", continuation.taskName)
        val files = chooseFile("导入立绘",
            arrayOf(
                FileChooser.ExtensionFilter("Required Files ($wc)", wc),
                FileChooser.ExtensionFilter("All Paintings (*.png)", "*.png")
            ),
            File(configurations.painting.importPaintingPath).withDefaultPath()
        )
        if (files.isEmpty()) return null
        val imageFile = files[0]
        configurations.painting.importPaintingPath = imageFile.parent
        MainPanel.Externals.paintingRootLabel.text = configurations.painting.importPaintingPath
        return imageFile
    }

    fun processPainting(importFile: File) {
        val image = ImageIO.read(importFile).flipY().apply(true)
        with(continuation) {
            baseMergeInfo.image = if (width > image.width || height > image.height) {
                BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR) {
                    drawImage(
                        image,
                        baseMergeInfo.transform.pastePoint.x.toInt(),
                        baseMergeInfo.transform.pastePoint.y.toInt(),
                        null
                    )
                }
            } else image
        }
        mergePainting()
        with(gui) {
            importFromImageButton.isDisable = false
            importFromFileButton.isDisable = false
            requiredImageListView.isDisable = false
        }
    }

    fun importFaceFromFile(): File? {
        val fileName = continuation.taskName
            .split('_', limit = 3)
            .let { it.slice(0..it.lastIndex.coerceAtMost(1)) }
            .joinToString("_")
            .replace("_n", "")
        val wcf = configurations.paintingface.fileWildcards.replace("{name}", fileName)
        val wct = configurations.paintingface.fileWildcards.replace("{name}", continuation.taskName)
        val files = chooseFile(
            "导入差分表情文件",
            arrayOf(
                FileChooser.ExtensionFilter("Required Files ($wcf)", wcf),
                FileChooser.ExtensionFilter("Required Files ($wct)", wct),
                FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            ),
            File(configurations.paintingface.importFaceFilePath).withDefaultPath()
        )
        if (files.isEmpty()) return null
        val faceFile = files[0]
        configurations.paintingface.importFaceFilePath = faceFile.parent
        return faceFile
    }

    fun processFaceFile(importFile: File) {
        runBlockingFX(gui) {
            requiredImageMergeInfoList.clear()
        }
        val faceContext = try {
            continuation.manager.loadFile(importFile.absolutePath)
        } catch (e: Exception) {
            return gui.reportFaceBundleError("导入差分表情文件时出错")
        }
        if (faceContext.objectList.isEmpty()) return gui.reportFaceBundleError()
        val sprites = faceContext.objectList.filterIsInstance<Sprite>()
        if (sprites.isEmpty()) return gui.reportFaceBundleError()
        for (sprite in sprites) {
            val mergeInfo = continuation.createMergeInfo(continuation.faceTransform, sprite.mName).apply {
                val spriteImage = sprite.getImage(SpriteCropStrategy.USE_RECT) ?: return gui.reportFaceBundleError()
                image = scalePaintingface(spriteImage, continuation.faceTransform)
            }
            runBlockingFX(gui) {
                requiredImageMergeInfoList.add(mergeInfo)
            }
        }
        gui.requiredImageListView.selectionModel.select(0)
        mergePainting()
    }

    fun importFaceFromImage(): File? {
        val files = chooseFile(
            "导入差分表情图片",
            arrayOf(
                FileChooser.ExtensionFilter(
                    "Required Files(${configurations.paintingface.imageWildcards})"
                        .replace("{name}", continuation.taskName),
                    configurations.paintingface.imageWildcards
                        .replace("{name}", continuation.taskName)
                ),
                FileChooser.ExtensionFilter(
                    "All Images (*.png)", "*.png"
                )
            ),
            File(configurations.paintingface.importFace2DPath).withDefaultPath()
        )
        if (files.isEmpty()) return null
        val imageFile = files[0]
        configurations.paintingface.importFace2DPath = imageFile.parent
        return imageFile
    }

    fun processFaceImage(importFile: File) {
        val mergeInfo = continuation.createMergeInfo(
            continuation.faceTransform,
            importFile.nameWithoutExtension
        ).apply {
            image = scalePaintingface(
                ImageIO.read(importFile).flipY().apply(true),
                continuation.faceTransform
            )
        }
        runBlockingFX(gui) {
            requiredImageMergeInfoList.clear()
            requiredImageMergeInfoList.add(mergeInfo)
            requiredImageListView.selectionModel.select(0)
        }
        mergePainting()
    }

    override fun mergePainting() {
        if (gui.requiredImageMergeInfoList.isEmpty()) {
            gui.previewMainImageView.image = continuation.baseMergeInfo.image.flipY().apply().createPreview()
            return
        }
        with(continuation) {
            pasteX = faceTransform.pastePoint.x.toInt() + gui.spinnerX.value
            pasteY = faceTransform.pastePoint.y.toInt() + gui.spinnerY.value
        }
        for (face in gui.requiredImageMergeInfoList) {
            face.changedFlag = 0b111
        }
        with(gui.spinnerX) {
            with(valueFactory as IntegerSpinnerValueFactory) {
                min = (-continuation.faceTransform.pastePoint.x -
                        gui.requiredImageMergeInfoList[0].image.width).toInt()
                max = continuation.baseMergeInfo.image.width -
                        continuation.faceTransform.pastePoint.x.toInt()
            }
            isDisable = false
        }
        with(gui.spinnerY) {
            with(valueFactory as IntegerSpinnerValueFactory) {
                min = (-continuation.faceTransform.pastePoint.y -
                        gui.requiredImageMergeInfoList[0].image.height).toInt()
                max = continuation.baseMergeInfo.image.height -
                        continuation.faceTransform.pastePoint.y.toInt()
            }
            isDisable = false
        }
        with(gui) {
            val mergeInfo = gui.requiredImageListView.selectionModel.selectedItem
            previewMainImageView.image = mergeInfo.getGlobalExhibit()
            localPreviewImageView.image = mergeInfo.getLocalExhibit()
            previewTabPane.selectionModel.select(1)
            reMergeButton.isDisable = false
            saveButton.isDisable = false
            saveAllButton.isDisable = false
            previewTabPane.isDisable = false
        }
    }

    override fun saveMergedPainting() = with(gui) {
        importFileButton.isDisable = true
        importImageButtonZone.isDisable = true
        reMergeButton.isDisable = true
        requiredImageListView.selectionModel.selectedItem.getMergedPainting()
            .flipY().apply().savePng(
                File("${configurations.painting.importPaintingPath}/${continuation.taskName}_exp.png"),
                configurations.outputCompressionLevel
            )
        importFileButton.isDisable = false
        importImageButtonZone.isDisable = false
        reMergeButton.isDisable = false
    }

    fun saveAllMergedPainting() {
        with(gui) {
            importFileButton.isDisable = true
            importImageButtonZone.isDisable = true
            reMergeButton.isDisable = true
            requiredImageListView.isDisable = true
            MainPanel.Externals.compressionLevelSpinner.isDisable = true
        }
        val size = gui.requiredImageMergeInfoList.size
        for ((i, mergeInfo) in gui.requiredImageMergeInfoList.withIndex()) {
            runBlockingFX(gui) { saveAllButton.text = "保存所有 ($i/$size)" }
            var fileName = configurations.painting.importPaintingPath +
                    "/${continuation.taskName}_exp_${mergeInfo.name}"
            if (Files.exists(Path.of("${fileName}.png"))) {
                fileName = generateFileName(fileName)
            }
            mergeInfo.getMergedPainting().flipY().apply().savePng(
                File("${fileName}.png"),
                configurations.outputCompressionLevel
            )
        }
        runBlockingFX(gui) {
            importFileButton.isDisable = false
            importImageButtonZone.isDisable = false
            reMergeButton.isDisable = false
            requiredImageListView.isDisable = false
            MainPanel.Externals.compressionLevelSpinner.isDisable = false
            saveAllButton.text = "保存所有"
        }
    }

    fun close() {
        if (::continuation.isInitialized) {
            continuation.close()
        }
    }
}