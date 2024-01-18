package io.github.deficuet.alpa.function

import io.github.deficuet.alpa.utils.configurations
import io.github.deficuet.alpa.utils.withDefaultPath
import tornadofx.chooseDirectory
import java.awt.Desktop
import java.io.File

abstract class BackendFunctions {
    abstract fun importFile(): File?

    abstract fun analyzeFile(importFile: File): Boolean

    abstract fun enableImport()

    open fun finishImport() {  }

    abstract fun importPainting(): File?

    abstract fun mergePainting()

    abstract fun saveMergedPainting()

    fun openFolder() {
        Desktop.getDesktop().open(File(configurations.painting.importPaintingPath))
    }

    companion object {
        fun importAssetSystemRoot(): File? {
            val folder = chooseDirectory(
                "选择文件夹",
                configurations.assetSystemRoot?.let {
                    File(it).withDefaultPath()
                } ?: File("C:/Users")
            ) ?: return null
            configurations.assetSystemRoot = folder.absolutePath
            return folder
        }

        fun importPaintingRoot(): File? {
            val folder = chooseDirectory(
                "选择文件夹",
                File(configurations.painting.importPaintingPath).withDefaultPath()
            ) ?: return null
            configurations.painting.importPaintingPath = folder.absolutePath
            return folder
        }
    }
}