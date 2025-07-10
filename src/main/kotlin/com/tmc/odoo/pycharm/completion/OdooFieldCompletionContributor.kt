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

class OdooFieldCompletionContributor : CompletionContributor() {
    
    init {
        // Complete field names in attribute access
        extend(
            CompletionType.BASIC,
            fieldAccessPattern(),
            OdooFieldCompletionProvider()
        )
        
        // Complete field types in assignments
        extend(
            CompletionType.BASIC,
            fieldTypePattern(),
            OdooFieldTypeCompletionProvider()
        )
    }
    
    private fun fieldAccessPattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .afterLeaf(".")
            .withParent(PyReferenceExpression::class.java)
    }
    
    private fun fieldTypePattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(PyCallExpression::class.java)
    }
}

class OdooFieldCompletionProvider : CompletionProvider<CompletionParameters>() {
    
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
        
        // Try to determine the model from the qualifier
        val modelName = determineModelFromQualifier(qualifier) ?: return
        
        // Get fields for this model
        val fields = odooService.getModelFields(modelName)
        
        fields.forEach { field ->
            val lookupElement = LookupElementBuilder.create(field.name)
                .withIcon(getFieldIcon(field.type))
                .withTypeText(field.type)
                .withTailText(if (field.isRequired) " (required)" else "", true)
            
            result.addElement(lookupElement)
        }
    }
    
    private fun determineModelFromQualifier(qualifier: PyExpression?): String? {
        return when (qualifier) {
            is PyReferenceExpression -> {
                // Check if it's 'self' and determine model from containing class
                if (qualifier.name == "self") {
                    // Find containing PyClass by traversing up the PSI tree
                    var element = qualifier.parent
                    while (element != null && element !is PyClass) {
                        element = element.parent
                    }
                    val pyClass = element as? PyClass
                    if (pyClass != null) {
                        val nameAttribute = pyClass.classAttributes.find { it.name == "_name" }
                        val nameValue = nameAttribute?.findAssignedValue()
                        if (nameValue is PyStringLiteralExpression) {
                            nameValue.stringValue
                        } else null
                    } else null
                } else null
            }
            else -> null
        }
    }
    
    private fun getFieldIcon(fieldType: String): javax.swing.Icon {
        return when (fieldType) {
            "Many2one", "One2many", "Many2many" -> OdooIcons.RELATION_FIELD
            "Selection" -> OdooIcons.SELECTION_FIELD
            "Boolean" -> OdooIcons.BOOLEAN_FIELD
            "Date", "Datetime" -> OdooIcons.DATE_FIELD
            "Float", "Integer", "Monetary" -> OdooIcons.NUMBER_FIELD
            "Binary", "Image" -> OdooIcons.BINARY_FIELD
            else -> OdooIcons.FIELD
        }
    }
}

class OdooFieldTypeCompletionProvider : CompletionProvider<CompletionParameters>() {
    
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
        
        // Add Odoo field types
        OdooConstants.FIELD_TYPES.forEach { fieldType ->
            val lookupElement = LookupElementBuilder.create(fieldType)
                .withIcon(getFieldTypeIcon(fieldType))
                .withTypeText("Odoo Field")
                .withInsertHandler { context, item ->
                    // Add parentheses for field constructor
                    val editor = context.editor
                    val caretOffset = editor.caretModel.offset
                    editor.document.insertString(caretOffset, "()")
                    editor.caretModel.moveToOffset(caretOffset + 1)
                }
            
            result.addElement(lookupElement)
        }
    }
    
    private fun getFieldTypeIcon(fieldType: String): javax.swing.Icon {
        return when (fieldType) {
            "Many2one", "One2many", "Many2many" -> OdooIcons.RELATION_FIELD
            "Selection" -> OdooIcons.SELECTION_FIELD
            "Boolean" -> OdooIcons.BOOLEAN_FIELD
            "Date", "Datetime" -> OdooIcons.DATE_FIELD
            "Float", "Integer", "Monetary" -> OdooIcons.NUMBER_FIELD
            "Binary", "Image" -> OdooIcons.BINARY_FIELD
            else -> OdooIcons.FIELD
        }
    }
}