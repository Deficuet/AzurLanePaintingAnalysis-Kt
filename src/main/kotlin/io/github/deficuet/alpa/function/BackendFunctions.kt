package io.github.deficuet.alpa.function

import io.github.deficuet.alpa.utils.configurations
import java.awt.Desktop
import java.io.File

abstract class BackendFunctions {
    abstract fun importFile()
    abstract fun analyzeFile()
    abstract fun activeImport()
    abstract fun importPainting()
    abstract fun mergePainting()
    abstract fun saveMergedPainting()
    fun openFolder() {
        Desktop.getDesktop().open(File(configurations.painting.importPaintingPath))
    }
}