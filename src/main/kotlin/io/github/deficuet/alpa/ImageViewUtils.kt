package io.github.deficuet.alpa

import io.github.deficuet.jimage.fancyBufferedImage
import io.github.deficuet.jimage.paste
import io.github.deficuet.jimage.resize
import io.github.deficuet.unitykt.cast
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.IntBuffer
import kotlin.math.roundToInt
import javafx.scene.image.Image as ImageFX

fun BufferedImage.toFXImage(): ImageFX {
    return WritableImage(
        PixelBuffer(
            width, height, IntBuffer.wrap(
                raster.dataBuffer.cast<DataBufferInt>().data
            ), PixelFormat.getIntArgbPreInstance()
        )
    )
}

const val PREVIEW_WIDTH = 720
const val PREVIEW_HEIGHT = 540

fun BufferedImage.createPreview(width: Int = PREVIEW_WIDTH, height: Int = PREVIEW_HEIGHT): ImageFX {
    val pw: Int; val ph: Int
    (height.toDouble() / this.height * this.width).roundToInt().let {
        if (it > width) {
            pw = width; ph = (width.toDouble() / this.width * this.height).roundToInt()
        } else {
            pw = it; ph = height
        }
    }
    return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE).paste(
        resize(pw, ph), (width - pw) / 2, (height - ph) / 2
    ).toFXImage()
}

val initialPreview = fancyBufferedImage(
    PREVIEW_WIDTH,
    PREVIEW_HEIGHT,
    BufferedImage.TYPE_INT_ARGB_PRE
) {
    font = Font("aria", Font.BOLD, 54)
    color = Color(116, 116, 116)
    val p = fontMetrics.stringWidth("Preview")
    val n = fontMetrics.stringWidth("not")
    val a = fontMetrics.stringWidth("available")
    val y = 216
    drawString("Preview", (PREVIEW_WIDTH - p) / 2, y)
    drawString("not", (PREVIEW_WIDTH - n) / 2, y + fontMetrics.height)
    drawString("available", (PREVIEW_WIDTH - a) / 2, y + fontMetrics.height * 2)
}.toFXImage()

const val FACE_PREVIEW_HEIGHT = 398

val initialFacePreview = fancyBufferedImage(
    PREVIEW_WIDTH,
    FACE_PREVIEW_HEIGHT,
    BufferedImage.TYPE_INT_ARGB_PRE
) {
    font = Font("aria", Font.BOLD, 54)
    color = Color(116, 116, 116)
    val p = fontMetrics.stringWidth("Preview")
    val n = fontMetrics.stringWidth("not")
    val a = fontMetrics.stringWidth("available")
    val y = 160
    drawString("Preview", (PREVIEW_WIDTH - p) / 2, y)
    drawString("not", (PREVIEW_WIDTH - n) / 2, y + fontMetrics.height)
    drawString("available", (PREVIEW_WIDTH - a) / 2, y + fontMetrics.height * 2)
}.toFXImage()
