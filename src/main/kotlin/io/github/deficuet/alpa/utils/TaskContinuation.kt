package io.github.deficuet.alpa.utils

import io.github.deficuet.alp.TextureTransform
import io.github.deficuet.alp.painting.PaintingTransform
import io.github.deficuet.jimage.copy
import io.github.deficuet.jimage.flipY
import io.github.deficuet.jimage.paste
import io.github.deficuet.unitykt.UnityAssetManager
import javafx.scene.control.Tab
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.File
import javafx.scene.image.Image as ImageFX

abstract class TaskContinuation(importFile: File): Closeable {
    val taskName: String = importFile.nameWithoutExtension
    var width = -1
    var height = -1
}

class PaintingTaskContinuation(importFile: File): TaskContinuation(importFile) {
    lateinit var mergedPainting: BufferedImage

    override fun close() {  }
}

abstract class MergeInfo(
    open val transform: TextureTransform,
    val name: String
) {
    var isImported = false
    lateinit var image: BufferedImage
    var offsetX = 0
    var offsetY = 0
}

class PaintingMergeInfo(
    override val transform: PaintingTransform,
    name: String
): MergeInfo(transform, name) {
    var displayTab: Tab? = null
    lateinit var exhibit: ImageFX
}

class PaintingfaceTaskContinuation(importFile: File): TaskContinuation(importFile) {
    lateinit var baseMergeInfo: PaintingfaceMergeInfo
    lateinit var faceTransform: TextureTransform
    lateinit var manager: UnityAssetManager
    var pasteX: Int = -1
    var pasteY: Int = -1

    inner class PaintingfaceMergeInfo(
        transform: TextureTransform,
        name: String
    ): MergeInfo(transform, name) {
        val changedFlag = BooleanArray(3) { true }

        private lateinit var mergedPainting: BufferedImage
        private lateinit var globalExhibit: ImageFX
        private lateinit var localExhibit: ImageFX

        @Synchronized
        fun getMergedPainting(): BufferedImage {
            if (changedFlag[0]) {
                mergedPainting = baseMergeInfo.image.copy().paste(image, pasteX, pasteY)
                changedFlag[0] = false
            }
            return mergedPainting
        }

        @Synchronized
        fun getGlobalExhibit(): ImageFX {
            if (changedFlag[1]) {
                globalExhibit = getMergedPainting().flipY().createPreview()
                changedFlag[1] = false
            }
            return globalExhibit
        }

        @Synchronized
        fun getLocalExhibit(): ImageFX {
            if (changedFlag[2]) {
                val p = getMergedPainting()
                localExhibit = p.getSubimage(
                    (pasteX - 32).coerceAtLeast(0),
                    (pasteY - 32).coerceAtLeast(0),
                    (image.width + 64).coerceAtMost(p.width - pasteX),
                    (image.height + 64).coerceAtMost(p.height - pasteY)
                ).flipY().createPreview()
                changedFlag[2] = false
            }
            return localExhibit
        }
    }

    fun createMergeInfo(transform: TextureTransform, name: String): PaintingfaceMergeInfo {
        return PaintingfaceMergeInfo(transform, name)
    }

    override fun close() {
        if (::manager.isInitialized) {
            manager.close()
        }
    }
}

