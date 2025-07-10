package com.tmc.odoo.pycharm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.python.psi.*
import com.tmc.odoo.pycharm.models.OdooModel
import com.tmc.odoo.pycharm.models.OdooField
import com.tmc.odoo.pycharm.models.OdooModule
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class OdooProjectService(private val project: Project) {
    
    private val cachedModels = ConcurrentHashMap<String, OdooModel>()
    private val cachedModules = ConcurrentHashMap<String, OdooModule>()
    private var lastScanTime = 0L
    private val CACHE_TIMEOUT = 10000L // 10 seconds
    
    companion object {
        fun getInstance(project: Project): OdooProjectService {
            return project.getService(OdooProjectService::class.java)
        }
    }
    
    /**
     * Check if this is an Odoo project by looking for __manifest__.py files
     */
    fun isOdooProject(): Boolean {
        return findOdooManifests().isNotEmpty()
    }
    
    /**
     * Find all __manifest__.py files in the project
     */
    fun findOdooManifests(): List<VirtualFile> {
        return FilenameIndex.getVirtualFilesByName(
            "__manifest__.py", 
            GlobalSearchScope.projectScope(project)
        ).toList()
    }
    
    /**
     * Get all Odoo models in the project
     */
    fun getAllModels(): List<OdooModel> {
        refreshCacheIfNeeded()
        return cachedModels.values.toList()
    }
    
    /**
     * Get all Odoo modules in the project
     */
    fun getAllModules(): List<OdooModule> {
        refreshCacheIfNeeded()
        return cachedModules.values.toList()
    }
    
    /**
     * Find a specific model by name
     */
    fun findModel(modelName: String): OdooModel? {
        refreshCacheIfNeeded()
        return cachedModels[modelName]
    }
    
    /**
     * Find models that inherit from a specific model
     */
    fun findModelsInheriting(parentModel: String): List<OdooModel> {
        refreshCacheIfNeeded()
        return cachedModels.values.filter { model ->
            model.inherits.contains(parentModel)
        }
    }
    
    /**
     * Get all fields for a specific model, including inherited fields
     */
    fun getModelFields(modelName: String): List<OdooField> {
        val model = findModel(modelName) ?: return emptyList()
        val fields = mutableListOf<OdooField>()
        
        // Add direct fields
        fields.addAll(model.fields)
        
        // Add inherited fields
        model.inherits.forEach { parentModelName ->
            val parentModel = findModel(parentModelName)
            if (parentModel != null) {
                fields.addAll(getModelFields(parentModelName))
            }
        }
        
        return fields.distinctBy { it.name }
    }
    
    /**
     * Refresh the cache if it's outdated
     */
    private fun refreshCacheIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime > CACHE_TIMEOUT) {
            scanProject()
            lastScanTime = currentTime
        }
    }
    
    /**
     * Scan the project for Odoo models and modules
     */
    private fun scanProject() {
        cachedModels.clear()
        cachedModules.clear()
        
        // Find all Python files in the project
        val pythonFiles = FilenameIndex.getVirtualFilesByName(
            "*.py", 
            GlobalSearchScope.projectScope(project)
        )
        
        pythonFiles.forEach { file ->
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile is PyFile) {
                scanPythonFile(psiFile)
            }
        }
        
        // Scan manifest files
        findOdooManifests().forEach { manifestFile ->
            val psiFile = PsiManager.getInstance(project).findFile(manifestFile)
            if (psiFile is PyFile) {
                scanManifestFile(psiFile)
            }
        }
    }
    
    /**
     * Scan a Python file for Odoo models
     */
    private fun scanPythonFile(psiFile: PyFile) {
        psiFile.topLevelClasses.forEach { pyClass ->
            if (isOdooModel(pyClass)) {
                val model = extractOdooModel(pyClass)
                if (model != null) {
                    cachedModels[model.name] = model
                }
            }
        }
    }
    
    /**
     * Scan a manifest file for module information
     */
    private fun scanManifestFile(psiFile: PyFile) {
        val module = extractOdooModule(psiFile)
        if (module != null) {
            cachedModules[module.name] = module
        }
    }
    
    /**
     * Check if a Python class is an Odoo model
     */
    private fun isOdooModel(pyClass: PyClass): Boolean {
        // Check for common Odoo model patterns
        val superClasses = pyClass.superClassExpressions
        return superClasses.any { expr ->
            when (expr) {
                is PyReferenceExpression -> {
                    val name = expr.name
                    name == "Model" || name == "TransientModel" || name == "AbstractModel"
                }
                is PyCallExpression -> {
                    val callee = expr.callee as? PyReferenceExpression
                    callee?.name == "models"
                }
                else -> false
            }
        }
    }
    
    /**
     * Extract Odoo model information from a Python class
     */
    private fun extractOdooModel(pyClass: PyClass): OdooModel? {
        val className = pyClass.name ?: return null
        
        // Find _name attribute
        val nameAttribute = pyClass.classAttributes.find { it.name == "_name" }
        val modelName = nameAttribute?.findAssignedValue()?.let { value ->
            if (value is PyStringLiteralExpression) {
                value.stringValue
            } else null
        } ?: return null
        
        // Find _description attribute
        val descriptionAttribute = pyClass.classAttributes.find { it.name == "_description" }
        val description = descriptionAttribute?.findAssignedValue()?.let { value ->
            if (value is PyStringLiteralExpression) {
                value.stringValue
            } else null
        } ?: ""
        
        // Find _inherit attribute
        val inheritAttribute = pyClass.classAttributes.find { it.name == "_inherit" }
        val inherits = mutableListOf<String>()
        inheritAttribute?.findAssignedValue()?.let { value ->
            when (value) {
                is PyStringLiteralExpression -> {
                    inherits.add(value.stringValue)
                }
                is PyListLiteralExpression -> {
                    value.elements.forEach { element ->
                        if (element is PyStringLiteralExpression) {
                            inherits.add(element.stringValue)
                        }
                    }
                }
                else -> {
                    // Handle other types if needed
                }
            }
        }
        
        // Extract fields
        val fields = extractFields(pyClass)
        
        return OdooModel(
            name = modelName,
            className = className,
            description = description,
            inherits = inherits,
            fields = fields,
            psiClass = pyClass
        )
    }
    
    /**
     * Extract fields from an Odoo model class
     */
    private fun extractFields(pyClass: PyClass): List<OdooField> {
        val fields = mutableListOf<OdooField>()
        
        pyClass.classAttributes.forEach { attribute ->
            val fieldName = attribute.name
            if (fieldName != null) {
                val assignedValue = attribute.findAssignedValue()
                
                if (assignedValue is PyCallExpression) {
                    val callee = assignedValue.callee as? PyReferenceExpression
                    val fieldType = callee?.name
                    
                    if (fieldType != null && isOdooFieldType(fieldType)) {
                        fields.add(OdooField(
                            name = fieldName,
                            type = fieldType,
                            psiElement = attribute
                        ))
                    }
                }
            }
        }
        
        return fields
    }
    
    /**
     * Check if a name represents an Odoo field type
     */
    private fun isOdooFieldType(name: String): Boolean {
        return name in setOf(
            "Char", "Text", "Html", "Boolean", "Integer", "Float", "Monetary",
            "Date", "Datetime", "Selection", "Many2one", "One2many", "Many2many",
            "Binary", "Image", "Json", "Properties"
        )
    }
    
    /**
     * Extract module information from a manifest file
     */
    private fun extractOdooModule(psiFile: PyFile): OdooModule? {
        // Look for dictionary definition in manifest
        val statements = psiFile.statements
        val dictExpr = statements.filterIsInstance<PyExpressionStatement>()
            .mapNotNull { it.expression as? PyDictLiteralExpression }
            .firstOrNull() ?: return null
        
        val name = dictExpr.elements.find { 
            it.key?.text?.contains("name") == true 
        }?.value?.let { (it as? PyStringLiteralExpression)?.stringValue }
        
        val version = dictExpr.elements.find { 
            it.key?.text?.contains("version") == true 
        }?.value?.let { (it as? PyStringLiteralExpression)?.stringValue }
        
        val depends = mutableListOf<String>()
        dictExpr.elements.find { 
            it.key?.text?.contains("depends") == true 
        }?.value?.let { value ->
            if (value is PyListLiteralExpression) {
                value.elements.forEach { element ->
                    if (element is PyStringLiteralExpression) {
                        depends.add(element.stringValue)
                    }
                }
            }
        }
        
        return if (name != null) {
            OdooModule(
                name = name,
                version = version ?: "1.0.0",
                depends = depends,
                manifestFile = psiFile
            )
        } else null
    }
}