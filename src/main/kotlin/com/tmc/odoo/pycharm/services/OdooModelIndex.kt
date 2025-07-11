package com.tmc.odoo.pycharm.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.AppExecutorUtil
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.tmc.odoo.pycharm.models.OdooModel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance indexing service for Odoo models with incremental updates
 */
@Service(Service.Level.PROJECT)
class OdooModelIndex(private val project: Project) {
    
    private val modelsByName = ConcurrentHashMap<String, OdooModel>()
    private val modelsByFile = ConcurrentHashMap<VirtualFile, Set<String>>()
    private val inheritanceGraph = ConcurrentHashMap<String, MutableSet<String>>() // modelName -> set of children
    private val reverseInheritanceGraph = ConcurrentHashMap<String, MutableSet<String>>() // modelName -> set of parents
    
    private val isIndexing = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    
    private val executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("OdooModelIndex", 2)
    
    companion object {
        fun getInstance(project: Project): OdooModelIndex {
            return project.getService(OdooModelIndex::class.java)
        }
    }
    
    init {
        // Listen for file changes to update index incrementally
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val pythonFiles = events.mapNotNull { event ->
                    val file = event.file
                    if (file?.extension == "py" && file.isInLocalFileSystem) file else null
                }
                
                if (pythonFiles.isNotEmpty()) {
                    executor.submit {
                        updateIndexForFiles(pythonFiles)
                    }
                }
            }
        })
    }
    
    /**
     * Initialize the index asynchronously
     */
    fun initializeAsync() {
        if (isInitialized.get() || isIndexing.get()) return
        
        executor.submit {
            if (isIndexing.compareAndSet(false, true)) {
                try {
                    buildCompleteIndex()
                    isInitialized.set(true)
                } finally {
                    isIndexing.set(false)
                }
            }
        }
    }
    
    /**
     * Get model by name with fast lookup
     */
    fun getModel(modelName: String): OdooModel? {
        ensureInitialized()
        return modelsByName[modelName]
    }
    
    /**
     * Get all models
     */
    fun getAllModels(): Collection<OdooModel> {
        ensureInitialized()
        return modelsByName.values
    }
    
    /**
     * Get models that inherit from the given model
     */
    fun getChildModels(modelName: String): Set<String> {
        ensureInitialized()
        return inheritanceGraph[modelName]?.toSet() ?: emptySet()
    }
    
    /**
     * Get models that this model inherits from
     */
    fun getParentModels(modelName: String): Set<String> {
        ensureInitialized()
        return reverseInheritanceGraph[modelName]?.toSet() ?: emptySet()
    }
    
    /**
     * Check if index is ready for queries
     */
    fun isReady(): Boolean = isInitialized.get() && !isIndexing.get()
    
    /**
     * Ensure index is initialized (blocks if necessary)
     */
    private fun ensureInitialized() {
        if (!isInitialized.get() && !isIndexing.get()) {
            initializeAsync()
        }
        
        // Wait for initialization to complete (with timeout)
        var attempts = 0
        while (!isInitialized.get() && attempts < 50) { // Max 5 seconds
            Thread.sleep(100)
            attempts++
        }
    }
    
    /**
     * Build complete index from scratch
     */
    private fun buildCompleteIndex() {
        modelsByName.clear()
        modelsByFile.clear()
        inheritanceGraph.clear()
        reverseInheritanceGraph.clear()
        
        val pythonFiles = FileTypeIndex.getFiles(
            PythonFileType.INSTANCE,
            GlobalSearchScope.projectScope(project)
        )
        
        updateIndexForFiles(pythonFiles.toList())
    }
    
    /**
     * Update index for specific files
     */
    private fun updateIndexForFiles(files: List<VirtualFile>) {
        val odooService = OdooProjectService.getInstance(project)
        val psiManager = PsiManager.getInstance(project)
        
        for (file in files) {
            try {
                // Remove old models from this file
                modelsByFile[file]?.forEach { modelName ->
                    removeModelFromIndex(modelName)
                }
                modelsByFile.remove(file)
                
                // Scan file for new models
                val psiFile = psiManager.findFile(file) as? PyFile ?: continue
                val modelsInFile = mutableSetOf<String>()
                
                psiFile.topLevelClasses.forEach { pyClass ->
                    if (odooService.isOdooModel(pyClass)) {
                        val model = extractOdooModelFast(pyClass)
                        if (model != null) {
                            addModelToIndex(model)
                            modelsInFile.add(model.name)
                        }
                    }
                }
                
                if (modelsInFile.isNotEmpty()) {
                    modelsByFile[file] = modelsInFile
                }
                
            } catch (e: Exception) {
                // Log error but continue processing other files
                println("Error indexing file ${file.path}: ${e.message}")
            }
        }
    }
    
    /**
     * Fast model extraction without heavy PSI operations
     */
    private fun extractOdooModelFast(pyClass: PyClass): OdooModel? {
        val className = pyClass.name ?: return null
        
        // Find _name attribute
        val nameAttr = pyClass.classAttributes.find { it.name == "_name" }
        val modelName = nameAttr?.findAssignedValue()?.let { value ->
            (value as? com.jetbrains.python.psi.PyStringLiteralExpression)?.stringValue
        } ?: return null
        
        // Find _description attribute  
        val descAttr = pyClass.classAttributes.find { it.name == "_description" }
        val description = descAttr?.findAssignedValue()?.let { value ->
            (value as? com.jetbrains.python.psi.PyStringLiteralExpression)?.stringValue
        } ?: ""
        
        // Find _inherit attribute
        val inheritAttr = pyClass.classAttributes.find { it.name == "_inherit" }
        val inherits = mutableListOf<String>()
        inheritAttr?.findAssignedValue()?.let { value ->
            when (value) {
                is com.jetbrains.python.psi.PyStringLiteralExpression -> {
                    inherits.add(value.stringValue)
                }
                is com.jetbrains.python.psi.PyListLiteralExpression -> {
                    value.elements.forEach { element ->
                        if (element is com.jetbrains.python.psi.PyStringLiteralExpression) {
                            inherits.add(element.stringValue)
                        }
                    }
                }
                else -> {
                    // Handle other types if needed
                }
            }
        }
        
        return OdooModel(
            name = modelName,
            className = className,
            description = description,
            inherits = inherits,
            fields = emptyList(), // Fields extracted lazily when needed
            psiClass = pyClass
        )
    }
    
    /**
     * Add model to index and update inheritance graph
     */
    private fun addModelToIndex(model: OdooModel) {
        modelsByName[model.name] = model
        
        // Update inheritance graph
        model.inherits.forEach { parentModel ->
            inheritanceGraph.computeIfAbsent(parentModel) { mutableSetOf() }.add(model.name)
            reverseInheritanceGraph.computeIfAbsent(model.name) { mutableSetOf() }.add(parentModel)
        }
    }
    
    /**
     * Remove model from index and update inheritance graph
     */
    private fun removeModelFromIndex(modelName: String) {
        val model = modelsByName.remove(modelName) ?: return
        
        // Update inheritance graph
        model.inherits.forEach { parentModel ->
            inheritanceGraph[parentModel]?.remove(modelName)
            reverseInheritanceGraph[modelName]?.remove(parentModel)
        }
        
        // Remove as parent from other models
        inheritanceGraph.remove(modelName)
        reverseInheritanceGraph[modelName]?.clear()
    }
}

/**
 * Startup activity to initialize the index
 */
class OdooModelIndexStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val odooService = OdooProjectService.getInstance(project)
        if (odooService.isOdooProject()) {
            OdooModelIndex.getInstance(project).initializeAsync()
        }
    }
}