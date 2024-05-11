package io.github.deficuet.alpa

import io.github.deficuet.alp.AnalyzeResult
import io.github.deficuet.alp.PaintingTransform
import io.github.deficuet.alp.TextureTransform
import io.github.deficuet.unitykt.UnityAssetManager
import javafx.scene.control.Tab
import tornadofx.observableListOf
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.File
import javafx.scene.image.Image as ImageFX

class TaskContinuation(val importFile: File): Closeable {
    val taskName = importFile.nameWithoutExtension
    lateinit var groupedPainting: BufferedImage
    lateinit var result: AnalyzeResult
    lateinit var manager: UnityAssetManager

    lateinit var faceComponent: FaceComponent

    var width = -1
    var height = -1

    override fun close() {
        if (::manager.isInitialized) {
            manager.close()
        }
    }
}

abstract class Component(val index: Int) {
    abstract val rect: TextureTransform
    var isImported = false
    var hasImportError = false
    var offsetX = 0
    var offsetY = 0
}

class PaintingComponent(override val rect: PaintingTransform, index: Int): Component(index) {
    lateinit var previewTabContent: Functions.PaintingPreviewTab

    lateinit var image: BufferedImage
    lateinit var exhibit: ImageFX

    fun isPreviewTabInitialized() = ::previewTabContent.isInitialized
}

class FaceComponent(override val rect: TextureTransform, index: Int): Component(index) {
    lateinit var previewTabContent: Functions.FacePreviewTab
    lateinit var previewTab: Tab

    val faceImageGroupList = observableListOf<FaceImageGroup>()

    fun isPreviewTabInitialized() = ::previewTabContent.isInitialized
}

class FaceImageGroup(
    val name: String,
    val faceImage: BufferedImage
) {
    lateinit var groupedPainting: BufferedImage
    lateinit var globalExhibit: ImageFX
    lateinit var localExhibit: ImageFX

    var changedFlags = 0b111
}
