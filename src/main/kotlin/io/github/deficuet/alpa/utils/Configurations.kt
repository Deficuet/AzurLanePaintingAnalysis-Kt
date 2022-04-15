package io.github.deficuet.alpa.utils

import kotlinx.serialization.Serializable

@Serializable
data class Configurations(
    val painting: PaintingSetting,
    val paintingface: PaintingfaceSetting,
    var outputCompressionLevel: Int
) {
    @Serializable
    data class PaintingSetting(
        var importFilesPath: String,
        var importPaintingPath: String,
        var wildcards: String
    )

    @Serializable
    data class PaintingfaceSetting(
        var importFilesPath: String,
        var importFace2DPath: String,
        var importFaceFilePath: String,
        var imageWildcards: String,
        var fileWildcards: String
    )
}