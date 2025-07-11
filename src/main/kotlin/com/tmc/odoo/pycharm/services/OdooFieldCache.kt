package com.tmc.odoo.pycharm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import com.jetbrains.python.psi.*
import com.tmc.odoo.pycharm.models.OdooField
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

/**
 * High-performance field cache with lazy loading
 */
@Service(Service.Level.PROJECT)
class OdooFieldCache(private val project: Project) {
    
    private val fieldCache = ConcurrentHashMap<String, List<OdooField>>()
    private val loadingFutures = ConcurrentHashMap<String, CompletableFuture<List<OdooField>>>()
    private val executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("OdooFieldCache", 2)
    
    companion object {
        fun getInstance(project: Project): OdooFieldCache {
            return project.getService(OdooFieldCache::class.java)
        }
    }
    
    /**
     * Get fields for a model with inheritance, using cache and async loading
     */
    fun getModelFields(modelName: String): List<OdooField> {
        // Return cached fields if available
        fieldCache[modelName]?.let { return it }
        
        // If already loading, wait for result
        loadingFutures[modelName]?.let { future ->
            return try {
                future.get()
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        // Start loading fields asynchronously
        val future = CompletableFuture.supplyAsync({
            computeModelFields(modelName)
        }, executor)
        
        loadingFutures[modelName] = future
        
        // Try to return synchronously if computation is quick
        return try {
            val fields = future.get(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            fieldCache[modelName] = fields
            loadingFutures.remove(modelName)
            fields
        } catch (e: java.util.concurrent.TimeoutException) {
            // Return empty list for now, cache will be populated asynchronously
            future.thenAccept { fields ->
                fieldCache[modelName] = fields
                loadingFutures.remove(modelName)
            }
            emptyList()
        } catch (e: Exception) {
            loadingFutures.remove(modelName)
            emptyList()
        }
    }
    
    /**
     * Get fields synchronously (blocks until computed)
     */
    fun getModelFieldsSync(modelName: String): List<OdooField> {
        fieldCache[modelName]?.let { return it }
        
        val fields = computeModelFields(modelName)
        fieldCache[modelName] = fields
        return fields
    }
    
    /**
     * Compute fields for a model with inheritance
     */
    private fun computeModelFields(modelName: String): List<OdooField> {
        val index = OdooModelIndex.getInstance(project)
        val visitedModels = mutableSetOf<String>()
        
        return computeModelFieldsRecursive(modelName, visitedModels, index)
    }
    
    /**
     * Recursively compute fields including inheritance
     */
    private fun computeModelFieldsRecursive(
        modelName: String, 
        visitedModels: MutableSet<String>,
        index: OdooModelIndex
    ): List<OdooField> {
        if (modelName in visitedModels) return emptyList()
        visitedModels.add(modelName)
        
        val model = index.getModel(modelName) ?: return emptyList()
        val fields = mutableListOf<OdooField>()
        
        // Extract fields from this model
        fields.addAll(extractFieldsFromClass(model.psiClass))
        
        // Add inherited fields recursively
        model.inherits.forEach { inheritedModelName ->
            fields.addAll(computeModelFieldsRecursive(inheritedModelName, visitedModels, index))
        }
        
        return fields.distinctBy { it.name }
    }
    
    /**
     * Fast field extraction from PSI class
     */
    private fun extractFieldsFromClass(pyClass: PyClass): List<OdooField> {
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
                            isRequired = extractIsRequired(assignedValue),
                            psiElement = attribute
                        ))
                    }
                }
            }
        }
        
        return fields
    }
    
    /**
     * Extract isRequired from field definition
     */
    private fun extractIsRequired(callExpression: PyCallExpression): Boolean {
        callExpression.arguments.forEach { arg ->
            if (arg is PyKeywordArgument && arg.keyword == "required") {
                val valueExpr = arg.valueExpression
                if (valueExpr is PyBoolLiteralExpression) {
                    return valueExpr.value
                }
            }
        }
        return false
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
     * Clear cache for a specific model
     */
    fun invalidateModel(modelName: String) {
        fieldCache.remove(modelName)
        loadingFutures.remove(modelName)
    }
    
    /**
     * Clear entire cache
     */
    fun clear() {
        fieldCache.clear()
        loadingFutures.clear()
    }
}