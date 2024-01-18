package io.github.deficuet.alpa.utils

import kotlinx.serialization.Serializable

@Serializable
data class Configurations(
    val painting: PaintingSetting = PaintingSetting(),
    val paintingface: PaintingfaceSetting = PaintingfaceSetting(),
    var outputCompressionLevel: Int = 7,
    var assetSystemRoot: String? = null
) {
    @Serializable
    data class PaintingSetting(
        var importFilesPath: String = "C:/Users",
        var importPaintingPath: String = "C:/Users",
        var wildcards: String = "*{name}.png;*{name}_group.png;*{name}_exp.png",
        var autoImport: Boolean = false
    )

    @Serializable
    data class PaintingfaceSetting(
        var importFilesPath: String = "C:/Users",
        var importFace2DPath: String = "C:/Users",
        var importFaceFilePath: String = "C:/Users",
        var imageWildcards: String = "?.png;{name}_head.png",
        var fileWildcards: String = "{name}"
    )
}