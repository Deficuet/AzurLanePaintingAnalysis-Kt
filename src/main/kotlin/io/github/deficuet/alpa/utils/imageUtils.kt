package io.github.deficuet.alpa.utils

import io.github.deficuet.jimage.paste
import javafx.embed.swing.SwingFXUtils
import java.awt.*
import java.awt.image.BufferedImage
import kotlin.math.roundToInt
import javafx.scene.image.Image as ImageFX

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