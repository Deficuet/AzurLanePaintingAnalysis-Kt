package io.github.deficuet.alpa.function

import io.github.deficuet.alpa.gui.PaintingfacePanel
import io.github.deficuet.alpa.utils.*
import io.github.deficuet.unitykt.*
import io.github.deficuet.unitykt.data.*
import io.github.deficuet.unitykt.math.Vector2
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt

class PaintingfaceFunctions(private val gui: PaintingfacePanel): BackendFunctions() {
    lateinit var continuation: PaintingfaceTaskContinuation
    private lateinit var manager: AssetManager

    override fun importFile() {
        val files = chooseFile(
            "选择文件", allTypeFilter,
            File(configurations.paintingface.importFilesPath)
        )
        if (files.isEmpty()) return
        val file = files[0]
        if (::manager.isInitialized) manager.closeAll()
        if (::continuation.isInitialized) {
            continuation.childrenMergeInfoList.clear()
        }
        manager = AssetManager()
        continuation = PaintingfaceTaskContinuation(file)
        configurations.paintingface.importFilesPath = continuation.importPath
        gui.currentTaskString.value = "当前任务：${continuation.taskName}"
        analyzeFile()
    }

    override fun analyzeFile() {
        with(gui) {
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
            requiredImageNameList.clear()
            requiredImageListView.isDisable = true
            importFromFileButton.isDisable = true
            importFromImageButton.isDisable = true
            importImageTitledPane.isDisable = true
            paintingfaceFileErrorString.value = ""
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
        val baseGameObject = bundle.mContainer[0].second.asset.getObj()
        if (baseGameObject == null || baseGameObject !is GameObject) {
            return gui.reportBundleError()
        }
        if (baseGameObject.mTransform.isEmpty()) {
            return gui.reportBundleError()
        }
        val baseRect = baseGameObject.mTransform[0] as RectTransform
        val baseChildren = baseRect.mChildren.getAllInstanceOf<RectTransform>()
        val face = baseChildren.find { it.mGameObject.getObj()!!.mName == "face" }
            ?: return gui.reportBundleError("没有可用的数据")
        val correction = with(gui) {
            if (baseChildren.any { it.mGameObject.getObj()!!.mName in arrayOf("layers", "paint") }) {
                needMergeLabel.textFill = errorTextFill
                needMergeString.value = "需要"
                Vector2(2.0, 1.0)
            } else {
                needMergeLabel.textFill = Color.web("#248C18")
                needMergeString.value = "不需要"
                val needDecode = if (bundle.mDependencies.isNotEmpty()) {
                    val d = bundle.mDependencies[0].split('/').last()
                    if (!Files.exists(Path.of("${continuation.importPath}/${d}"))) {
                        return gui.reportBundleError("依赖项缺失")
                    } else {
                        manager.let {
                            it.loadFile("${continuation.importPath}/${d}").objects
                                .any { obj -> obj is Mesh }
                        }
                    }
                } else {
                    bundleContext.objects.any { it is Mesh }
                }
                if (needDecode) {
                    Vector2(1.0, 0.0)
                } else {
                    Vector2.Zero
                }
            }
        }
        continuation.baseMergeInfo = PaintingfaceMergeInfo(baseRect)
        val paste = ((face.mAnchorMax - face.mAnchorMin) * baseRect.size * face.mPivot +
            face.mAnchorMin * baseRect.size + face.mAnchoredPosition -
            face.size * face.mPivot * face.mLocalScale.vector2).round() + correction
        continuation.faceMergeInfo = PaintingfaceMergeInfo(face, face.mLocalScale.vector2, paste)
        activeImport()
    }

    override fun activeImport() {
        with(gui) {
            importImageTitledPane.isDisable = false
        }
    }

    override fun importPainting() {
        val files = chooseFile("导入立绘",
            arrayOf(
                FileChooser.ExtensionFilter(
                    "Required Files (${configurations.painting.wildcards})"
                        .replace("{name}", continuation.taskName),
                    configurations.painting.wildcards
                        .replace("{name}", continuation.taskName)
                ),
                FileChooser.ExtensionFilter("All Paintings (*.png)", "*.png")
            ),
            File(configurations.painting.importPaintingPath)
        )
        if (files.isEmpty()) return
        val imageFile = files[0]
        configurations.painting.importPaintingPath = imageFile.parent
        val image = ImageIO.read(imageFile)
        val painting = with(continuation.baseMergeInfo.rect) {
            BufferedImage(
                maxOf(size.x.toInt(), image.width),
                maxOf(size.y.toInt(), image.height),
                BufferedImage.TYPE_4BYTE_ABGR
            ).paste(image.flipY(), 0, 0)
        }
        continuation.baseMergeInfo.image = painting
        mergePainting()
        with(gui) {
            importFromImageButton.isDisable = false
            importFromFileButton.isDisable = false
            requiredImageListView.isDisable = false
        }
    }

    fun importFaceFromFile() {
        val fileName = continuation.taskName
            .split('_', limit = 3)
            .slice(0..1)
            .joinToString("_")
        val files = chooseFile("导入差分表情文件",
            arrayOf(
                FileChooser.ExtensionFilter(
                    "Required Files (${configurations.paintingface.fileWildcards})"
                        .replace("{name}", fileName),
                    configurations.paintingface.fileWildcards
                        .replace("{name}", fileName)
                ),
                FileChooser.ExtensionFilter(
                    "Required Files (${configurations.paintingface.fileWildcards})"
                        .replace("{name}", continuation.taskName),
                    configurations.paintingface.fileWildcards
                        .replace("{name}", continuation.taskName)
                ),
                FileChooser.ExtensionFilter("All Paintings (*.png)", "*.png")
            ),
            File(configurations.paintingface.importFaceFilePath)
        )
        if (files.isEmpty()) return
        with(gui) {
            requiredImageNameList.clear()
        }
        continuation.childrenMergeInfoList.clear()
        val faceFile = files[0]
        configurations.paintingface.importFaceFilePath = faceFile.parent
        val faceContext = try {
            manager.loadFile(faceFile.absolutePath)
        } catch (e: Exception) {
            return gui.reportFaceBundleError("导入差分表情文件时出错")
        }
        if (faceContext.objects.isEmpty()) return gui.reportFaceBundleError()
        val sprites = faceContext.objects.allObjectsOf<Sprite>().toMutableList()
        if (sprites.isEmpty()) return gui.reportFaceBundleError()
        if (!sprites.all { sprite -> sprite.mName.all { it.isDigit() } }) {
            return gui.reportFaceBundleError()
        }
        sprites.sortBy { it.mName.toInt() }
        for (sprite in sprites) {
            continuation.childrenMergeInfoList.add(
                continuation.faceMergeInfo.emptyCopy().apply {
                    name = sprite.mName
                    val tex = sprite.mRD.texture.getObj()!!
                    image = if (
                        tex.mWidth == sprite.mRect.w.toInt() &&
                        tex.mHeight == sprite.mRect.h.toInt()
                    ) {
                        tex.image
                    } else {
                        tex.image.getSubimage(
                            sprite.mRD.textureRect.x.toInt(),
                            sprite.mRD.textureRect.y.toInt(),
                            sprite.mRect.w.toInt(),
                            sprite.mRect.h.toInt()
                        )
                    }.resize(
                        (sprite.mRect.w * scale.x).roundToInt(),
                        (sprite.mRect.h * scale.y).roundToInt()
                    )
                }
            )
        }
        with(gui) {
            requiredImageNameList.addAll(continuation.childrenMergeInfoList.map { it.name })
            requiredImageListView.selectionModel.select(0)
        }
        mergePainting()
    }

    fun importFaceFromImage() {
        val files = chooseFile("导入差分表情图片",
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
            File(configurations.paintingface.importFace2DPath)
        )
        if (files.isEmpty()) return
        val imageFile = files[0]
        configurations.paintingface.importFace2DPath = imageFile.parent
        val mergeInfo = continuation.faceMergeInfo.emptyCopy()
        val image = ImageIO.read(imageFile).let {
            it.flipY().resize(
                (it.width * mergeInfo.scale.x).roundToInt(),
                (it.height * mergeInfo.scale.y).roundToInt()
            )
        }
        continuation.childrenMergeInfoList.clear()
        continuation.childrenMergeInfoList.add(
            mergeInfo.apply {
                name = imageFile.nameWithoutExtension
                this.image = image
            }
        )
        with(gui) {
            requiredImageNameList.clear()
            requiredImageNameList.addAll(continuation.childrenMergeInfoList.map { it.name })
            requiredImageListView.selectionModel.select(0)
        }
        mergePainting()
    }

    override fun mergePainting() {
        if (continuation.childrenMergeInfoList.isEmpty()) {
            gui.previewMainImageView.image = continuation.baseMergeInfo.image.flipY().createPreview()
            return
        }
        val px = continuation.faceMergeInfo.pastePoint.x.toInt() + gui.spinnerX.value
        val py = continuation.faceMergeInfo.pastePoint.y.toInt() + gui.spinnerY.value
        for (face in continuation.childrenMergeInfoList) {
            face.mergePainting = {
                continuation.baseMergeInfo.image.copy().paste(face.image, px, py)
            }
            face.generateGlobalExhibit = { face.mergedPainting.flipY().createPreview() }
            face.generateLocalExhibit = {
                face.mergedPainting.getSubimage(
                    (px - 32).coerceAtLeast(0),
                    (py - 32).coerceAtLeast(0),
                    (face.image.width + 64).coerceAtMost(face.mergedPainting.width - px),
                    (face.image.height + 64).coerceAtMost(face.mergedPainting.height - py)
                ).flipY().createPreview()
            }
        }
        with(gui.spinnerX) {
            with(valueFactory as IntegerSpinnerValueFactory) {
                min = (
                        -continuation.faceMergeInfo.pastePoint.x -
                        continuation.childrenMergeInfoList[0].image.width
                    ).toInt()
                max = continuation.baseMergeInfo.image.width - continuation.faceMergeInfo.pastePoint.x.toInt()
            }
            isDisable = false
        }
        with(gui.spinnerY) {
            with(valueFactory as IntegerSpinnerValueFactory) {
                min = (
                        -continuation.faceMergeInfo.pastePoint.y -
                        continuation.childrenMergeInfoList[0].image.height
                    ).toInt()
                max = continuation.baseMergeInfo.image.height - continuation.faceMergeInfo.pastePoint.y.toInt()
            }
            isDisable = false
        }
        with(gui) {
            val selected = requiredImageListView.selectionModel.selectedItem
            previewMainImageView.image = continuation.childrenMergeInfoList[selected].globalExhibit
            localPreviewImageView.image = continuation.childrenMergeInfoList[selected].localExhibit
            previewTabPane.selectionModel.select(1)
            reMergeButton.isDisable = false
            saveButton.isDisable = false
            previewTabPane.isDisable = false
        }
    }

    override fun saveMergedPainting() {
        gui.importFileButton.isDisable = true
        gui.importImageButtonZone.isDisable = true
        gui.reMergeButton.isDisable = true
        continuation.childrenMergeInfoList[gui.requiredImageListView.selectionModel.selectedItem].mergedPainting
            .flipY().save(
                File("${configurations.painting.importPaintingPath}/${continuation.taskName}_exp.png")
            )
        gui.importFileButton.isDisable = false
        gui.importImageButtonZone.isDisable = false
        gui.reMergeButton.isDisable = false
    }

    private fun PaintingfacePanel.reportFaceBundleError(msg: String = "差分表情文件不可用") {
        paintingfaceFileErrorLabel.textFill = errorTextFill
        paintingfaceFileErrorString.value = msg
    }
}