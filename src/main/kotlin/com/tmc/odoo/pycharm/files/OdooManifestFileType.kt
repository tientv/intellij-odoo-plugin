package com.tmc.odoo.pycharm.files

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.PythonFileType
import com.tmc.odoo.pycharm.icons.OdooIcons
import javax.swing.Icon

class OdooManifestFileType : FileType {
    
    companion object {
        @JvmField
        val INSTANCE = OdooManifestFileType()
    }
    
    override fun getName(): String = "Odoo Manifest"
    
    override fun getDescription(): String = "Odoo module manifest file"
    
    override fun getDefaultExtension(): String = "py"
    
    override fun getIcon(): Icon? = OdooIcons.MODULE
    
    override fun isBinary(): Boolean = false
    
    override fun isReadOnly(): Boolean = false
    
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
    
    // Check if this is actually a manifest file
    fun isManifestFile(file: VirtualFile): Boolean {
        return file.name == "__manifest__.py"
    }
}