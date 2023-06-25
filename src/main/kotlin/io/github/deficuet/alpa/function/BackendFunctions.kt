package io.github.deficuet.alpa.function

import io.github.deficuet.alpa.utils.configurations
import java.awt.Desktop
import java.io.File

abstract class BackendFunctions {
    abstract fun importFile(): File?

    abstract fun analyzeFile(importFile: File): Boolean

    abstract fun activeImport()

    open fun finishImport() {  }

    abstract fun importPainting(): File?

    abstract fun mergePainting()

    abstract fun saveMergedPainting()

    fun openFolder() {
        Desktop.getDesktop().open(File(configurations.painting.importPaintingPath))
    }
}