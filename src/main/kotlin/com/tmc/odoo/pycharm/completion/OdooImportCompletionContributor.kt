package com.tmc.odoo.pycharm.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.*
import com.jetbrains.python.PythonLanguage
import com.tmc.odoo.pycharm.services.OdooProjectService
import com.tmc.odoo.pycharm.icons.OdooIcons

class OdooImportCompletionContributor : CompletionContributor() {
    
    init {
        // Complete module names in import statements
        extend(
            CompletionType.BASIC,
            importModulePattern(),
            OdooImportModuleCompletionProvider()
        )
        
        // Complete model names in from-import statements
        extend(
            CompletionType.BASIC,
            fromImportPattern(),
            OdooFromImportCompletionProvider()
        )
    }
    
    private fun importModulePattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(PlatformPatterns.psiElement(PyImportStatement::class.java))
            .inside(PlatformPatterns.psiElement(PyReferenceExpression::class.java))
    }
    
    private fun fromImportPattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(PlatformPatterns.psiElement(PyFromImportStatement::class.java))
            .inside(PlatformPatterns.psiElement(PyReferenceExpression::class.java))
    }
}

class OdooImportModuleCompletionProvider : CompletionProvider<CompletionParameters>() {
    
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val odooService = OdooProjectService.getInstance(project)
        
        if (!odooService.isOdooProject()) {
            return
        }
        
        // Add common Odoo import patterns
        addOdooImportPatterns(result)
        
        // Add modules from current project
        val modules = odooService.getAllModules()
        modules.forEach { module ->
            val lookupElement = LookupElementBuilder.create(module.name)
                .withIcon(OdooIcons.MODULE)
                .withTypeText("Module")
                .withTailText(" (${module.version})", true)
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addOdooImportPatterns(result: CompletionResultSet) {
        val commonImports = listOf(
            "odoo" to "Odoo Framework",
            "odoo.models" to "Odoo Models",
            "odoo.fields" to "Odoo Fields", 
            "odoo.api" to "Odoo API",
            "odoo.exceptions" to "Odoo Exceptions",
            "odoo.tools" to "Odoo Tools",
            "odoo.addons" to "Odoo Addons",
            "odoo.http" to "Odoo HTTP",
            "odoo.release" to "Odoo Release Info",
            "odoo.service" to "Odoo Services",
            "odoo.sql_db" to "Odoo Database",
            "odoo.tests" to "Odoo Tests"
        )
        
        commonImports.forEach { (importPath, description) ->
            val lookupElement = LookupElementBuilder.create(importPath)
                .withIcon(OdooIcons.MODULE)
                .withTypeText("Odoo")
                .withTailText(" ($description)", true)
                .withBoldness(true)
            
            result.addElement(lookupElement)
        }
    }
}

class OdooFromImportCompletionProvider : CompletionProvider<CompletionParameters>() {
    
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val odooService = OdooProjectService.getInstance(project)
        
        if (!odooService.isOdooProject()) {
            return
        }
        
        // Get the from-import statement to understand context
        val fromImportStatement = parameters.position.parent?.parent as? PyFromImportStatement
        val importSource = fromImportStatement?.importSource?.text
        
        when {
            importSource?.contains("models") == true -> {
                addModelImports(result)
            }
            importSource?.contains("fields") == true -> {
                addFieldImports(result)
            }
            importSource?.contains("api") == true -> {
                addApiImports(result)
            }
            importSource?.contains("exceptions") == true -> {
                addExceptionImports(result)
            }
            importSource?.contains("tools") == true -> {
                addToolImports(result)
            }
            importSource?.contains("addons") == true -> {
                // Try to complete model names from addons
                addModuleModelImports(result, odooService)
            }
        }
    }
    
    private fun addModelImports(result: CompletionResultSet) {
        val modelClasses = listOf(
            "Model" to "Base Model",
            "TransientModel" to "Transient Model",
            "AbstractModel" to "Abstract Model"
        )
        
        modelClasses.forEach { (className, description) ->
            val lookupElement = LookupElementBuilder.create(className)
                .withIcon(OdooIcons.MODEL)
                .withTypeText("Model Class")
                .withTailText(" ($description)", true)
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addFieldImports(result: CompletionResultSet) {
        val fieldTypes = listOf(
            "Char", "Text", "Html", "Boolean", "Integer", "Float", "Monetary",
            "Date", "Datetime", "Selection", "Many2one", "One2many", "Many2many",
            "Binary", "Image", "Json", "Properties"
        )
        
        fieldTypes.forEach { fieldType ->
            val lookupElement = LookupElementBuilder.create(fieldType)
                .withIcon(OdooIcons.FIELD)
                .withTypeText("Field Type")
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addApiImports(result: CompletionResultSet) {
        val apiDecorators = listOf(
            "api.depends" to "Compute dependency",
            "api.onchange" to "Onchange trigger",
            "api.constrains" to "Constraint validator",
            "api.model" to "Model-level method",
            "api.model_create_multi" to "Multi-create method",
            "api.returns" to "Return type specification"
        )
        
        apiDecorators.forEach { (decorator, description) ->
            val lookupElement = LookupElementBuilder.create(decorator)
                .withIcon(OdooIcons.API)
                .withTypeText("API Decorator")
                .withTailText(" ($description)", true)
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addExceptionImports(result: CompletionResultSet) {
        val exceptions = listOf(
            "UserError" to "User error",
            "ValidationError" to "Validation error",
            "AccessError" to "Access error", 
            "RedirectWarning" to "Redirect warning",
            "Warning" to "Warning message"
        )
        
        exceptions.forEach { (exception, description) ->
            val lookupElement = LookupElementBuilder.create(exception)
                .withIcon(OdooIcons.EXCEPTION)
                .withTypeText("Exception")
                .withTailText(" ($description)", true)
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addToolImports(result: CompletionResultSet) {
        val tools = listOf(
            "safe_eval" to "Safe evaluation",
            "html_escape" to "HTML escaping",
            "plaintext2html" to "Plain text to HTML",
            "email_split" to "Email splitting",
            "formataddr" to "Format address",
            "config" to "Configuration",
            "misc" to "Miscellaneous tools"
        )
        
        tools.forEach { (tool, description) ->
            val lookupElement = LookupElementBuilder.create(tool)
                .withIcon(OdooIcons.TOOL)
                .withTypeText("Tool")
                .withTailText(" ($description)", true)
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addModuleModelImports(result: CompletionResultSet, odooService: OdooProjectService) {
        val models = odooService.getAllModels()
        
        models.forEach { model ->
            val lookupElement = LookupElementBuilder.create(model.className)
                .withIcon(OdooIcons.MODEL)
                .withTypeText("Model (${model.name})")
                .withTailText(" (${model.description})", true)
            
            result.addElement(lookupElement)
        }
    }
}