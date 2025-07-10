package com.tmc.odoo.pycharm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.FileTypeIndex
import com.jetbrains.python.psi.*
import com.jetbrains.python.PythonFileType
import com.tmc.odoo.pycharm.models.OdooModel
import com.tmc.odoo.pycharm.models.OdooField
import com.tmc.odoo.pycharm.models.OdooModule
import com.tmc.odoo.pycharm.models.OdooMethod
import com.tmc.odoo.pycharm.models.OdooMethodType
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class OdooProjectService(private val project: Project) {
    
    private val cachedModels = ConcurrentHashMap<String, OdooModel>()
    private val cachedModules = ConcurrentHashMap<String, OdooModule>()
    private var lastScanTime: Long = 0
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
    fun findModelsInheriting(modelName: String): List<OdooModel> {
        refreshCacheIfNeeded()
        return cachedModels.values.filter { model ->
            model.inherits.contains(modelName)
        }
    }

    /**
     * Get fields for a specific model, including inherited fields (recursive)
     */
    fun getModelFields(modelName: String): List<OdooField> {
        refreshCacheIfNeeded()
        return getModelFieldsRecursive(modelName, mutableSetOf())
    }

    /**
     * Get fields for a specific model recursively, including all inherited fields
     */
    private fun getModelFieldsRecursive(modelName: String, visitedModels: MutableSet<String>): List<OdooField> {
        if (modelName in visitedModels) return emptyList()
        visitedModels.add(modelName)
        
        val model = cachedModels[modelName] ?: return emptyList()
        val fields = mutableListOf<OdooField>()
        
        // Add model's own fields
        fields.addAll(model.fields)
        
        // Add inherited fields recursively
        model.inherits.forEach { inheritedModelName ->
            fields.addAll(getModelFieldsRecursive(inheritedModelName, visitedModels))
        }
        
        return fields.distinctBy { it.name }
    }

    /**
     * Get fields for a related model through a Many2one/One2many relationship
     */
    fun getRelatedModelFields(fieldName: String, currentModelName: String): List<OdooField> {
        refreshCacheIfNeeded()
        val currentModel = cachedModels[currentModelName] ?: return emptyList()
        
        // Find the field in current model (including inherited fields)
        val allFields = getModelFieldsRecursive(currentModelName, mutableSetOf())
        val field = allFields.find { it.name == fieldName } ?: return emptyList()
        
        // Extract related model name from field definition
        val relatedModelName = extractRelatedModelFromField(field) ?: return emptyList()
        
        return getModelFields(relatedModelName)
    }

    /**
     * Extract the related model name from a relational field
     */
    fun extractRelatedModelFromField(field: OdooField): String? {
        if (field.type !in listOf("Many2one", "One2many", "Many2many")) return null
        
        // Try to extract model name from field definition in PSI
        val fieldAssignment = field.psiElement as? PyTargetExpression
        val assignedValue = fieldAssignment?.findAssignedValue() as? PyCallExpression
        val arguments = assignedValue?.arguments
        
        // Look for string argument (first positional argument is usually the model name)
        arguments?.forEach { arg ->
            if (arg is PyStringLiteralExpression) {
                return arg.stringValue
            }
        }
        
        return null
    }

    /**
     * Get all methods for a specific model, including inherited methods
     */
    fun getModelMethods(modelName: String): List<OdooMethod> {
        refreshCacheIfNeeded()
        return getModelMethodsRecursive(modelName, mutableSetOf())
    }

    /**
     * Get methods for a specific model recursively, including all inherited methods
     */
    private fun getModelMethodsRecursive(modelName: String, visitedModels: MutableSet<String>): List<OdooMethod> {
        if (modelName in visitedModels) return emptyList()
        visitedModels.add(modelName)
        
        val model = cachedModels[modelName] ?: return emptyList()
        val methods = mutableListOf<OdooMethod>()
        
        // Extract methods from the model's PSI class
        model.psiClass.methods.forEach { pyFunction ->
            val methodName = pyFunction.name ?: return@forEach
            val methodType = when {
                methodName.startsWith("_compute_") -> OdooMethodType.COMPUTE
                methodName.startsWith("_onchange_") -> OdooMethodType.API_ONCHANGE
                methodName.startsWith("_inverse_") -> OdooMethodType.INVERSE
                methodName.startsWith("_search_") -> OdooMethodType.SEARCH
                methodName in listOf("create", "write", "unlink", "read") -> OdooMethodType.CRUD
                else -> OdooMethodType.BUSINESS_LOGIC
            }
            
            methods.add(OdooMethod(
                name = methodName,
                type = methodType,
                parameters = extractMethodParameters(pyFunction),
                description = pyFunction.docStringValue
            ))
        }
        
        // Add inherited methods recursively
        model.inherits.forEach { inheritedModelName ->
            methods.addAll(getModelMethodsRecursive(inheritedModelName, visitedModels))
        }
        
        return methods.distinctBy { it.name }
    }

    /**
     * Extract parameter names from a Python function
     */
    private fun extractMethodParameters(pyFunction: PyFunction): List<String> {
        val parameters = mutableListOf<String>()
        pyFunction.parameterList.parameters.forEach { param ->
            param.name?.let { name ->
                if (name != "self") {
                    parameters.add(name)
                }
            }
        }
        return parameters
    }

    /**
     * Refresh cache if timeout has passed
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
        
        // Find all Python files in the project using FileTypeIndex
        val pythonFiles = FileTypeIndex.getFiles(
            PythonFileType.INSTANCE,
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
    private fun scanPythonFile(pyFile: PyFile) {
        pyFile.topLevelClasses.forEach { pyClass ->
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
    private fun scanManifestFile(manifestFile: PyFile) {
        val module = extractOdooModule(manifestFile)
        if (module != null) {
            cachedModules[module.name] = module
        }
    }

    /**
     * Check if a Python class is an Odoo model
     */
    private fun isOdooModel(pyClass: PyClass): Boolean {
        val superClassExpressions = pyClass.superClassExpressions
        
        return superClassExpressions.any { expr ->
            when (expr) {
                is PyReferenceExpression -> {
                    val name = expr.name
                    name in listOf("Model", "TransientModel", "AbstractModel")
                }
                is PyCallExpression -> {
                    val callee = expr.callee
                    if (callee is PyQualifiedExpression) {
                        val qualifier = callee.qualifier?.name
                        val name = callee.name
                        qualifier == "models" && name in listOf("Model", "TransientModel", "AbstractModel")
                    } else false
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
     * Extract fields from a Python class
     */
    private fun extractFields(pyClass: PyClass): List<OdooField> {
        val fields = mutableListOf<OdooField>()
        
        pyClass.classAttributes.forEach { attribute ->
            val attributeName = attribute.name ?: return@forEach
            val assignedValue = attribute.findAssignedValue()
            
            if (assignedValue is PyCallExpression) {
                val callee = assignedValue.callee
                if (callee is PyQualifiedExpression) {
                    val qualifier = callee.qualifier?.name
                    val fieldType = callee.name
                    
                    if (qualifier == "fields" && isOdooFieldType(fieldType)) {
                        fields.add(OdooField(
                            name = attributeName,
                            type = fieldType ?: "Unknown",
                            isRequired = false, // TODO: Extract from field arguments
                            psiElement = attribute
                        ))
                    }
                }
            }
        }
        
        return fields
    }

    /**
     * Check if a type name is a valid Odoo field type
     */
    private fun isOdooFieldType(typeName: String?): Boolean {
        val odooFieldTypes = setOf(
            "Char", "Text", "Html", "Boolean", "Integer", "Float", "Monetary",
            "Date", "Datetime", "Selection", "Many2one", "One2many", "Many2many",
            "Binary", "Image", "Json", "Properties"
        )
        return typeName in odooFieldTypes
    }

    /**
     * Extract module information from a manifest file
     */
    private fun extractOdooModule(manifestFile: PyFile): OdooModule? {
        // TODO: Parse manifest file for module information
        val manifestPath = manifestFile.virtualFile?.path ?: return null
        val moduleName = manifestFile.virtualFile?.parent?.name ?: return null
        
        return OdooModule(
            name = moduleName,
            version = "Unknown",
            depends = emptyList(),
            manifestFile = manifestFile
        )
    }
}