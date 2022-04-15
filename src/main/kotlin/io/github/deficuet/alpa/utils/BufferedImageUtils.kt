package io.github.deficuet.alpa.utils

import javafx.embed.swing.SwingFXUtils
import org.bytedeco.javacv.Java2DFrameUtils
import javafx.scene.image.Image as ImageFX
import java.awt.*
import java.awt.geom.AffineTransform
import java.io.File
import kotlin.math.roundToInt
import org.bytedeco.opencv.global.opencv_imgcodecs
import java.awt.image.*

inline fun BufferedImage(width: Int, height: Int, type: Int, initializer: Graphics2D.() -> Unit): BufferedImage {
    return BufferedImage(width, height, type).apply {
        with(createGraphics()) {
            setRenderingHints(mapOf(
                RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON,
                RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY,
                RenderingHints.KEY_COLOR_RENDERING to RenderingHints.VALUE_COLOR_RENDER_QUALITY,
                RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC,
                RenderingHints.KEY_ALPHA_INTERPOLATION to RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
            ))
            initializer()
            dispose()
        }
    }
}

inline fun BufferedImage.edit(process: Graphics2D.() -> Unit) = apply {
    with(createGraphics()) {
        setRenderingHints(mapOf(
            RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON,
            RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY,
            RenderingHints.KEY_COLOR_RENDERING to RenderingHints.VALUE_COLOR_RENDER_QUALITY,
            RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC,
            RenderingHints.KEY_ALPHA_INTERPOLATION to RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
        ))
        process()
        dispose()
    }
}

fun BufferedImage.resize(w: Int, h: Int) = BufferedImage(w, h, type) {
    drawImage(this@resize, 0, 0, w, h, null)
}

fun BufferedImage.copy() = BufferedImage(colorModel, copyData(null), isAlphaPremultiplied, null)

fun BufferedImage.paste(other: Image, x: Int, y: Int) = edit {
    drawImage(other, x, y, null)
}

fun BufferedImage.flipY(): BufferedImage {
    return AffineTransformOp(
        AffineTransform.getScaleInstance(1.0, -1.0).apply {
            translate(0.0, -height.toDouble())
        },
        AffineTransformOp.TYPE_BICUBIC
    ).filter(this, null)
}

fun BufferedImage.save(dst: File, compressionLevel: Int = 7) {
    assert(type == BufferedImage.TYPE_4BYTE_ABGR)
    opencv_imgcodecs.imwrite(
        dst.absolutePath,
        Java2DFrameUtils.toMat(
            BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR).apply {
                data = Raster.createRaster(
                    ComponentSampleModel(
                        this@save.raster.dataBuffer.dataType, this@save.width, this@save.height,
                        4, this@save.width * 4, intArrayOf(0, 3, 2, 1)
                    ), this@save.raster.dataBuffer, null
                )
            }
        ),
        intArrayOf(
            opencv_imgcodecs.IMWRITE_PNG_COMPRESSION, compressionLevel,
            opencv_imgcodecs.IMWRITE_PNG_STRATEGY, opencv_imgcodecs.IMWRITE_PNG_STRATEGY_FILTERED
        )
    )
}

fun BufferedImage.toFXImage(): ImageFX = SwingFXUtils.toFXImage(this, null)

fun BufferedImage.createPreview(width: Int = 864, height: Int = 540): ImageFX {
    val pw: Int; val ph: Int
    (height.toDouble() / this.height * this.width).roundToInt().let {
        if (it > width) {
            pw = width; ph = (width.toDouble() / this.width * this.height).roundToInt()
        } else {
            pw = it; ph = height
        }
    }
    return BufferedImage(width, height, type).paste(
        getScaledInstance(pw, ph, Image.SCALE_SMOOTH),
        (width - pw) / 2, (height - ph) / 2
    ).toFXImage()
}