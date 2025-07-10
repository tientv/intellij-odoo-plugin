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
import com.tmc.odoo.pycharm.models.OdooConstants

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
        
        // Check if this is a method call on a model
        if (isModelMethodCall(qualifier)) {
            addOdooModelMethods(result)
        }
        
        // Check if this is a method call on recordset
        if (isRecordsetMethodCall(qualifier)) {
            addOdooRecordsetMethods(result)
        }
    }
    
    private fun isModelMethodCall(qualifier: PyExpression?): Boolean {
        return when (qualifier) {
            is PyReferenceExpression -> {
                qualifier.name == "self" || 
                qualifier.name?.endsWith("_obj") == true ||
                qualifier.name?.contains("env") == true
            }
            is PySubscriptionExpression -> {
                // Check for self.env['model.name'] pattern
                val operand = qualifier.operand
                operand is PyReferenceExpression && operand.name == "env"
            }
            else -> false
        }
    }
    
    private fun isRecordsetMethodCall(qualifier: PyExpression?): Boolean {
        // This is a simplified check - in a real implementation,
        // you'd want to perform more sophisticated type inference
        return qualifier != null
    }
    
    private fun addOdooModelMethods(result: CompletionResultSet) {
        OdooConstants.COMMON_METHODS.values.forEach { method ->
            val lookupElement = LookupElementBuilder.create(method.name)
                .withIcon(getMethodIcon(method.type))
                .withTypeText(method.type.name.lowercase().replace("_", " "))
                .withTailText(buildParameterText(method.parameters), true)
                .withInsertHandler { context, item ->
                    insertMethodCall(context, method.parameters)
                }
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addOdooRecordsetMethods(result: CompletionResultSet) {
        // Add methods available on recordsets
        val recordsetMethods = listOf(
            "filtered", "mapped", "sorted", "exists", "ensure_one",
            "sudo", "with_context", "with_user", "with_company"
        )
        
        recordsetMethods.forEach { methodName ->
            val method = OdooConstants.COMMON_METHODS[methodName]
            if (method != null) {
                val lookupElement = LookupElementBuilder.create(methodName)
                    .withIcon(OdooIcons.METHOD)
                    .withTypeText("Recordset Method")
                    .withTailText(buildParameterText(method.parameters), true)
                    .withInsertHandler { context, item ->
                        insertMethodCall(context, method.parameters)
                    }
                
                result.addElement(lookupElement)
            }
        }
    }
    
    private fun getMethodIcon(methodType: com.tmc.odoo.pycharm.models.OdooMethodType): javax.swing.Icon {
        return when (methodType) {
            com.tmc.odoo.pycharm.models.OdooMethodType.CRUD -> OdooIcons.CRUD_METHOD
            com.tmc.odoo.pycharm.models.OdooMethodType.SEARCH -> OdooIcons.SEARCH_METHOD
            com.tmc.odoo.pycharm.models.OdooMethodType.API_MODEL -> OdooIcons.API_METHOD
            com.tmc.odoo.pycharm.models.OdooMethodType.COMPUTE -> OdooIcons.COMPUTE_METHOD
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
        } else {
            val paramText = parameters.joinToString(", ") { "$it=" }
            editor.document.insertString(caretOffset, "($paramText)")
            // Move cursor to first parameter
            editor.caretModel.moveToOffset(caretOffset + 1)
        }
    }
}