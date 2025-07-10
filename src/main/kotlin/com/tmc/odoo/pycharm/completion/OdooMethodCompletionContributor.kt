package com.tmc.odoo.pycharm.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.*
import com.jetbrains.python.PythonLanguage
import com.tmc.odoo.pycharm.services.OdooProjectService
import com.tmc.odoo.pycharm.icons.OdooIcons
import com.tmc.odoo.pycharm.models.OdooConstants
import com.tmc.odoo.pycharm.models.OdooMethodType

class OdooMethodCompletionContributor : CompletionContributor() {
    
    init {
        // Complete method names in method calls
        extend(
            CompletionType.BASIC,
            methodCallPattern(),
            OdooMethodCompletionProvider()
        )
    }
    
    private fun methodCallPattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .afterLeaf(".")
            .withParent(PyReferenceExpression::class.java)
    }
}

class OdooMethodCompletionProvider : CompletionProvider<CompletionParameters>() {
    
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
        
        val element = parameters.position
        val refExpr = element.parent as? PyReferenceExpression ?: return
        val qualifier = refExpr.qualifier
        
        // Handle different types of method calls
        when {
            // Direct method call on self (e.g., self.method_name)
            isSelfMethodCall(qualifier) -> {
                val modelName = determineModelFromContext(element)
                if (modelName != null) {
                    addModelMethods(odooService, modelName, result)
                    addOdooFrameworkMethods(result)
                }
            }
            
            // Method call on recordset (e.g., records.method_name)
            isRecordsetMethodCall(qualifier) -> {
                addOdooRecordsetMethods(result)
                val modelName = inferModelFromVariable(qualifier as PyReferenceExpression, odooService)
                if (modelName != null) {
                    addModelMethods(odooService, modelName, result)
                }
            }
            
            // Method call on env model (e.g., self.env['res.partner'].method_name)
            isEnvModelMethodCall(qualifier) -> {
                val modelName = extractModelNameFromEnvCall(qualifier as PySubscriptionExpression)
                if (modelName != null) {
                    addModelMethods(odooService, modelName, result)
                    addOdooFrameworkMethods(result)
                }
            }
        }
    }
    
    private fun isSelfMethodCall(qualifier: PyExpression?): Boolean {
        return qualifier is PyReferenceExpression && qualifier.name == "self"
    }
    
    private fun isRecordsetMethodCall(qualifier: PyExpression?): Boolean {
        return qualifier is PyReferenceExpression && qualifier.name != "self"
    }
    
    private fun isEnvModelMethodCall(qualifier: PyExpression?): Boolean {
        return qualifier is PySubscriptionExpression &&
               qualifier.operand is PyQualifiedExpression &&
               qualifier.operand.text.contains("env")
    }
    
    private fun addModelMethods(odooService: OdooProjectService, modelName: String, result: CompletionResultSet) {
        // Get all methods including inherited ones
        val methods = odooService.getModelMethods(modelName)
        
        methods.forEach { method ->
            val lookupElement = LookupElementBuilder.create(method.name)
                .withIcon(getMethodIcon(method.type))
                .withTypeText(method.type.toString().lowercase().replace("_", " "))
                .withTailText("()", true)
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
                    insertMethodCall(context, emptyList()) // Could be enhanced to extract actual parameters
                }
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addOdooFrameworkMethods(result: CompletionResultSet) {
        // Add common Odoo framework methods
        OdooConstants.COMMON_METHODS.values.forEach { method ->
            val lookupElement = LookupElementBuilder.create(method.name)
                .withIcon(getMethodIcon(method.type))
                .withTypeText(method.type.name.lowercase().replace("_", " "))
                .withTailText(buildParameterText(method.parameters), true)
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
                    insertMethodCall(context, method.parameters)
                }
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addOdooRecordsetMethods(result: CompletionResultSet) {
        // Add methods available on recordsets
        val recordsetMethods = mapOf(
            "filtered" to listOf("func"),
            "mapped" to listOf("func"),
            "sorted" to listOf("key", "reverse"),
            "exists" to emptyList(),
            "ensure_one" to emptyList(),
            "sudo" to listOf("user"),
            "with_context" to listOf("**kwargs"),
            "with_user" to listOf("user"),
            "with_company" to listOf("company"),
            "browse" to listOf("ids"),
            "search" to listOf("domain", "offset", "limit", "order"),
            "search_count" to listOf("domain"),
            "create" to listOf("vals"),
            "write" to listOf("vals"),
            "unlink" to emptyList(),
            "copy" to listOf("default")
        )
        
        recordsetMethods.forEach { (methodName, parameters) ->
            val lookupElement = LookupElementBuilder.create(methodName)
                .withIcon(getMethodIcon(OdooMethodType.CRUD))
                .withTypeText("Recordset Method")
                .withTailText(buildParameterText(parameters), true)
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
                    insertMethodCall(context, parameters)
                }
            
            result.addElement(lookupElement)
        }
    }
    
    private fun determineModelFromContext(element: PsiElement): String? {
        // Find containing PyClass by traversing up the PSI tree
        var currentElement = element.parent
        while (currentElement != null && currentElement !is PyClass) {
            currentElement = currentElement.parent
        }
        
        val pyClass = currentElement as? PyClass ?: return null
        
        // Extract _name attribute from the class
        val nameAttribute = pyClass.classAttributes.find { it.name == "_name" }
        val nameValue = nameAttribute?.findAssignedValue()
        if (nameValue is PyStringLiteralExpression) {
            return nameValue.stringValue
        }
        
        return null
    }
    
    private fun inferModelFromVariable(qualifier: PyExpression?, odooService: OdooProjectService): String? {
        // This could be enhanced to perform more sophisticated type inference
        return null
    }
    
    private fun extractModelNameFromEnvCall(qualifier: PySubscriptionExpression): String? {
        val indexExpression = qualifier.indexExpression
        if (indexExpression is PyStringLiteralExpression) {
            return indexExpression.stringValue
        }
        return null
    }
    
    private fun getMethodIcon(methodType: OdooMethodType): javax.swing.Icon {
        return when (methodType) {
            OdooMethodType.COMPUTE -> OdooIcons.COMPUTE_METHOD
            OdooMethodType.API_ONCHANGE -> OdooIcons.ONCHANGE_METHOD
            OdooMethodType.CRUD -> OdooIcons.ORM_METHOD
            OdooMethodType.BUSINESS_LOGIC -> OdooIcons.METHOD
            else -> OdooIcons.METHOD
        }
    }
    
    private fun buildParameterText(parameters: List<String>): String {
        if (parameters.isEmpty()) return "()"
        return "(${parameters.joinToString(", ")})"
    }
    
    private fun insertMethodCall(context: InsertionContext, parameters: List<String>) {
        val editor = context.editor
        val caretOffset = editor.caretModel.offset
        
        if (parameters.isEmpty()) {
            editor.document.insertString(caretOffset, "()")
            editor.caretModel.moveToOffset(caretOffset + 2)
        } else {
            editor.document.insertString(caretOffset, "()")
            editor.caretModel.moveToOffset(caretOffset + 1)
        }
    }
}