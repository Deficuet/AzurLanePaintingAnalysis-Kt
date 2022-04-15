package io.github.deficuet.alpa.utils

import io.github.deficuet.unitykt.*
import io.github.deficuet.unitykt.data.*
import io.github.deficuet.unitykt.math.*
import javafx.scene.image.Image as ImageFX
import java.awt.image.BufferedImage
import java.io.File
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

abstract class TaskContinuation(importFile: File) {
    val filePath: String = importFile.absolutePath
    val importPath: String = importFile.parent
    val taskName: String = importFile.nameWithoutExtension
}

class PaintingTaskContinuation(importFile: File): TaskContinuation(importFile) {
    lateinit var baseMergeInfo: PaintingMergeInfo
    val childrenMergeInfoList = mutableListOf<PaintingMergeInfo>()
    val mergeInfoList: List<PaintingMergeInfo>
        get() {
            return if (::baseMergeInfo.isInitialized) listOf(baseMergeInfo) + childrenMergeInfoList
            else childrenMergeInfoList
        }
    val extraPixel = intArrayOf(0, 0, 0, 0)
    lateinit var mergedPainting: BufferedImage
}

class PaintingfaceTaskContinuation(importFile: File): TaskContinuation(importFile) {
    lateinit var baseMergeInfo: PaintingfaceMergeInfo
    lateinit var faceMergeInfo: PaintingfaceMergeInfo
    val childrenMergeInfoList = mutableListOf<PaintingfaceMergeInfo>()
}

abstract class MergeInfo {
    abstract val rect: RectTransform
    abstract val scale: Vector2
    abstract var pastePoint: Vector2

    lateinit var name: String
    lateinit var image: BufferedImage
    val isImported get() = ::image.isInitialized
}

class PaintingMergeInfo(
    override val rect: RectTransform,
    override val scale: Vector2 = Vector2(1.0, 1.0),
    override var pastePoint: Vector2 = Vector2.Zero
): MergeInfo() {
    val rawSize = rect.mGameObject.getObj()!!.mComponents.mapNotNull { it.getObj() }
        .firstObjectOf<MonoBehaviour>().json!!.getJSONObject("mRawSpriteSize")
        .let { Vector2(it.getDouble("x"), it.getDouble("y")) }

    var offsetX = 0
    var offsetY = 0
    lateinit var exhibit: BufferedImage
}

class PaintingfaceMergeInfo(
    override val rect: RectTransform,
    override val scale: Vector2 = Vector2(1.0, 1.0),
    override var pastePoint: Vector2 = Vector2.Zero
): MergeInfo() {
    lateinit var mergePainting: () -> BufferedImage
    lateinit var generateGlobalExhibit: () -> ImageFX
    lateinit var generateLocalExhibit: () -> ImageFX

    val mergedPainting by LazyMutableImageLoader(::mergePainting)
    val globalExhibit by LazyMutableImageLoader(::generateGlobalExhibit)
    val localExhibit by LazyMutableImageLoader(::generateLocalExhibit)

    fun emptyCopy() = PaintingfaceMergeInfo(
        rect, scale, pastePoint
    )
}

class LazyMutableImageLoader<T: Any>(private val loaderProperty: KProperty0<() -> T>) {
    private lateinit var loader: () -> T
    private lateinit var value: T

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!::loader.isInitialized) {
            loader = loaderProperty()
            value = loader()
        } else {
            val l = loaderProperty()
            if (loader !== l) {
                loader = l
                value = loader()
            }
        }
        return value
    }
}

operator fun <M: MergeInfo> List<M>.get(name: String) = first { it.name.contentEquals(name) }