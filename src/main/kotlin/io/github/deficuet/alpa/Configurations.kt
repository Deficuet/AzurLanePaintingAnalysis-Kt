package io.github.deficuet.alpa

import kotlinx.serialization.Serializable

@Serializable
data class Configurations(
    var pngCompressionLevel: Int = 7,
    var assetSystemRoot: String = "",
    var importMainBundlePath: String = "",
    val painting: PaintingSetting = PaintingSetting(),
    val face: FaceSetting = FaceSetting()
) {
    @Serializable
    data class PaintingSetting(
        var autoImport: Boolean = false,
        var importImagePath: String = "C:/Users",
        var imageNamePattern: String = "*{name}.png;*{name}_dec.png;*{name}_group.png;*{name}_exp.png"
    )

    @Serializable
    data class FaceSetting(
        var autoImport: Boolean = false,
        var importBundlePath: String = "",
        var bundleNamePattern: String = "{name}",
        var importImagePath: String = "C:/Users",
        var imageNamePattern: String = "?.png"
    )
}