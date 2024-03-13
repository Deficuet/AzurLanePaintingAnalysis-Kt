package io.github.deficuet.alpa.utils

import io.github.deficuet.alp.TextureTransform
import io.github.deficuet.alp.painting.PaintingAnalyzeStatus
import io.github.deficuet.alp.painting.PaintingTransform
import io.github.deficuet.alp.paintingface.PaintingfaceAnalyzeStatus
import io.github.deficuet.jimage.copy
import io.github.deficuet.jimage.flipY
import io.github.deficuet.jimage.paste
import javafx.scene.control.Tab
import java.awt.image.BufferedImage
import java.io.Closeable
import java.io.File
import javafx.scene.image.Image as ImageFX

abstract class TaskContinuation(importFile: File): Closeable {
    val taskName: String = importFile.nameWithoutExtension
}

class PaintingTaskContinuation(importFile: File): TaskContinuation(importFile) {
    lateinit var mergedPainting: BufferedImage
    lateinit var status: PaintingAnalyzeStatus

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
    lateinit var status: PaintingfaceAnalyzeStatus
    var pasteX: Int = -1
    var pasteY: Int = -1

    inner class PaintingfaceMergeInfo(
        transform: TextureTransform,
        name: String
    ): MergeInfo(transform, name) {
        var changedFlag = 0b111

        private lateinit var mergedPainting: BufferedImage
        private lateinit var globalExhibit: ImageFX
        private lateinit var localExhibit: ImageFX

        @Synchronized
        fun getMergedPainting(): BufferedImage {
            if (changedFlag.and(0b001) == 1) {
                mergedPainting = baseMergeInfo.image.copy().paste(image, pasteX, pasteY)
                changedFlag = changedFlag.and(0b110)
            }
            return mergedPainting
        }

        @Synchronized
        fun getGlobalExhibit(): ImageFX {
            if (changedFlag.and(0b010) == 0b010) {
                globalExhibit = getMergedPainting().flipY().apply().createPreview()
                changedFlag = changedFlag.and(0b101)
            }
            return globalExhibit
        }

        @Synchronized
        fun getLocalExhibit(): ImageFX {
            if (changedFlag.and(0b100) == 0b100) {
                val p = getMergedPainting()
                localExhibit = p.getSubimage(
                    (pasteX - 32).coerceAtLeast(0),
                    (pasteY - 32).coerceAtLeast(0),
                    (image.width + 64).coerceAtMost(p.width - pasteX),
                    (image.height + 64).coerceAtMost(p.height - pasteY)
                ).flipY().apply(true).createPreview()
                changedFlag = changedFlag.and(0b011)
            }
            return localExhibit
        }
    }

    fun createMergeInfo(transform: TextureTransform, name: String): PaintingfaceMergeInfo {
        return PaintingfaceMergeInfo(transform, name)
    }

    override fun close() {
        if (::status.isInitialized) {
            status.manager.close()
        }
    }
}

