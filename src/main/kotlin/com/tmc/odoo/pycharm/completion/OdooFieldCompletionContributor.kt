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

class OdooFieldCompletionContributor : CompletionContributor() {
    
    init {
        // Complete field names in self.field_name context
        extend(
            CompletionType.BASIC,
            selfFieldAccessPattern(),
            OdooFieldCompletionProvider()
        )
        
        // Complete field types in field assignments (e.g., fields.Char())
        extend(
            CompletionType.BASIC,
            fieldTypePattern(),
            OdooFieldTypeCompletionProvider()
        )
    }
    
    private fun selfFieldAccessPattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .afterLeaf(".")
            .withParent(
                PlatformPatterns.psiElement(PyReferenceExpression::class.java)
                    .withParent(
                        PlatformPatterns.psiElement(PyQualifiedExpression::class.java)
                            .withChild(
                                PlatformPatterns.psiElement(PyReferenceExpression::class.java)
                                    .withName("self")
                            )
                    )
            )
    }
    
    private fun fieldTypePattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(
                PlatformPatterns.psiElement(PyCallExpression::class.java)
                    .withChild(
                        PlatformPatterns.psiElement(PyReferenceExpression::class.java)
                            .withParent(
                                PlatformPatterns.psiElement(PyQualifiedExpression::class.java)
                                    .withChild(
                                        PlatformPatterns.psiElement(PyReferenceExpression::class.java)
                                            .withName("fields")
                                    )
                            )
                    )
            )
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
        
        // Handle different types of field access
        when {
            // Direct field access on self (e.g., self.field_name)
            isSelfFieldAccess(qualifier) -> {
                val modelName = determineModelFromContext(element) ?: return
                addModelFieldCompletions(odooService, modelName, result)
            }
            
            // Chained field access (e.g., self.partner_id.field_name)
            isChainedFieldAccess(qualifier) -> {
                handleChainedFieldAccess(qualifier as PyQualifiedExpression, element, odooService, result)
            }
            
            // Record set field access (e.g., records.field_name)
            isRecordSetAccess(qualifier) -> {
                handleRecordSetFieldAccess(qualifier as PyReferenceExpression, element, odooService, result)
            }
        }
    }
    
    private fun isSelfFieldAccess(qualifier: PyExpression?): Boolean {
        return qualifier is PyReferenceExpression && qualifier.name == "self"
    }
    
    private fun isChainedFieldAccess(qualifier: PyExpression?): Boolean {
        return qualifier is PyQualifiedExpression
    }
    
    private fun isRecordSetAccess(qualifier: PyExpression?): Boolean {
        // This could be enhanced to detect recordset variables
        return qualifier is PyReferenceExpression && qualifier.name != "self"
    }
    
    private fun addModelFieldCompletions(odooService: OdooProjectService, modelName: String, result: CompletionResultSet) {
        // Get all fields including inherited ones
        val fields = odooService.getModelFields(modelName)
        
        fields.forEach { field ->
            val lookupElement = LookupElementBuilder.create(field.name)
                .withIcon(getFieldIcon(field.type))
                .withTypeText(field.type)
                .withTailText(if (field.isRequired) " (required)" else "", true)
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
                    // Add smart completion for relational fields
                    if (field.type in listOf("Many2one", "One2many", "Many2many")) {
                        // Could add automatic ".browse()" or similar completions
                    }
                }
            
            result.addElement(lookupElement)
        }
        
        // Also add methods for this model
        val methods = odooService.getModelMethods(modelName)
        methods.forEach { method ->
            val lookupElement = LookupElementBuilder.create(method.name)
                .withIcon(getMethodIcon(method.type))
                .withTypeText(method.type.toString().lowercase())
                .withTailText("()", true)
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
                    // Add parentheses for method calls
                    val editor = context.editor
                    val offset = context.tailOffset
                    if (context.completionChar != '(') {
                        editor.document.insertString(offset, "()")
                        editor.caretModel.moveToOffset(offset + 1)
                    }
                }
            
            result.addElement(lookupElement)
        }
    }
    
    private fun handleChainedFieldAccess(qualifier: PyQualifiedExpression, element: PsiElement, 
                                       odooService: OdooProjectService, result: CompletionResultSet) {
        // Handle cases like self.partner_id.name
        val qualifierChain = buildQualifierChain(qualifier)
        if (qualifierChain.size < 2) return
        
        // Start from the base model (typically from self)
        val baseModelName = determineModelFromContext(element) ?: return
        var currentModelName = baseModelName
        
        // Navigate through the chain (excluding the last element which is what we're completing)
        for (i in 1 until qualifierChain.size) {
            val fieldName = qualifierChain[i]
            val relatedFields = odooService.getRelatedModelFields(fieldName, currentModelName)
            if (relatedFields.isNotEmpty()) {
                // Extract the model name for the next iteration
                val field = odooService.getModelFields(currentModelName).find { it.name == fieldName }
                if (field != null) {
                    currentModelName = odooService.extractRelatedModelFromField(field) ?: return
                } else {
                    return
                }
            } else {
                return // Chain broken, can't continue
            }
        }
        
        // Add completions for the final model in the chain
        addModelFieldCompletions(odooService, currentModelName, result)
    }
    
    private fun handleRecordSetFieldAccess(qualifier: PyReferenceExpression, element: PsiElement,
                                         odooService: OdooProjectService, result: CompletionResultSet) {
        // This could be enhanced to detect the model type of recordset variables
        // For now, we'll try to infer from context or variable assignments
        val modelName = inferModelFromVariable(qualifier, odooService) ?: return
        addModelFieldCompletions(odooService, modelName, result)
    }
    
    private fun buildQualifierChain(qualifier: PyQualifiedExpression): List<String> {
        val chain = mutableListOf<String>()
        var current: PyExpression? = qualifier
        
        while (current != null) {
            when (current) {
                is PyQualifiedExpression -> {
                    current.name?.let { chain.add(0, it) }
                    current = current.qualifier
                }
                is PyReferenceExpression -> {
                    current.name?.let { chain.add(0, it) }
                    break
                }
                else -> break
            }
        }
        
        return chain
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
    
    private fun inferModelFromVariable(variable: PyReferenceExpression, odooService: OdooProjectService): String? {
        // Try to find variable assignment and infer model type
        // This is a simplified implementation - could be enhanced
        val name = variable.name ?: return null
        
        // Look for common patterns like:
        // partner = self.env['res.partner']
        // records = self.search([])
        
        return null // Placeholder - would need more sophisticated analysis
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
    
    private fun getMethodIcon(methodType: OdooMethodType): javax.swing.Icon {
        return when (methodType) {
            OdooMethodType.COMPUTE -> OdooIcons.COMPUTE_METHOD
            OdooMethodType.API_ONCHANGE -> OdooIcons.ONCHANGE_METHOD
            OdooMethodType.CRUD -> OdooIcons.ORM_METHOD
            else -> OdooIcons.METHOD
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
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
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