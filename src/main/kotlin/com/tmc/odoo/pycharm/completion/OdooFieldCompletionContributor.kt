package com.tmc.odoo.pycharm.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.intellij.openapi.util.TextRange
import com.jetbrains.python.psi.*
import com.jetbrains.python.PythonLanguage
import com.tmc.odoo.pycharm.services.OdooProjectService
import com.tmc.odoo.pycharm.icons.OdooIcons
import com.tmc.odoo.pycharm.models.OdooConstants
import com.tmc.odoo.pycharm.models.OdooMethodType
import com.tmc.odoo.pycharm.models.OdooMethod
import com.tmc.odoo.pycharm.models.OdooFieldAttributes
import com.tmc.odoo.pycharm.models.OdooFieldInfo
import com.tmc.odoo.pycharm.services.OdooFieldCache
import com.tmc.odoo.pycharm.models.OdooField

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
        
        // Complete field attributes inside field constructors
        extend(
            CompletionType.BASIC,
            fieldAttributePattern(),
            OdooFieldAttributeCompletionProvider()
        )
        
        // Complete model names in relational field comodel_name
        extend(
            CompletionType.BASIC,
            comodelNamePattern(),
            OdooComodelCompletionProvider()
        )
        
        // Complete field names in related attribute
        extend(
            CompletionType.BASIC,
            relatedFieldPattern(),
            OdooRelatedFieldCompletionProvider()
        )
        
        // Complete compute method names
        extend(
            CompletionType.BASIC,
            computeMethodPattern(),
            OdooComputeMethodCompletionProvider()
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
    
    private fun fieldAttributePattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(
                PlatformPatterns.psiElement(PyArgumentList::class.java)
                    .withParent(
                        PlatformPatterns.psiElement(PyCallExpression::class.java)
                            .withChild(
                                PlatformPatterns.psiElement(PyQualifiedExpression::class.java)
                                    .withChild(
                                        PlatformPatterns.psiElement(PyReferenceExpression::class.java)
                                            .withName("fields")
                                    )
                            )
                    )
            )
    }
    
    private fun comodelNamePattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(
                PlatformPatterns.psiElement(PyStringLiteralExpression::class.java)
                    .withParent(
                        PlatformPatterns.psiElement(PyKeywordArgument::class.java)
                            .withName("comodel_name")
                    )
            )
    }
    
    private fun relatedFieldPattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(
                PlatformPatterns.psiElement(PyStringLiteralExpression::class.java)
                    .withParent(
                        PlatformPatterns.psiElement(PyKeywordArgument::class.java)
                            .withName("related")
                    )
            )
    }
    
    private fun computeMethodPattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(
                PlatformPatterns.psiElement(PyStringLiteralExpression::class.java)
                    .withParent(
                        PlatformPatterns.psiElement(PyKeywordArgument::class.java)
                            .withName(PlatformPatterns.string().oneOf("compute", "inverse", "search"))
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
        
        // Handle different types of field access with enhanced performance
        when {
            // Direct field access on self (e.g., self.field_name)
            isSelfFieldAccess(qualifier) -> {
                val modelName = determineModelFromContext(element) ?: return
                addModelFieldCompletions(odooService, modelName, result, element)
            }
            
            // Chained field access (e.g., self.partner_id.field_name)
            isChainedFieldAccess(qualifier) -> {
                handleChainedFieldAccess(qualifier as PyQualifiedExpression, element, odooService, result)
            }
            
            // Record set field access (e.g., records.field_name)
            isRecordSetAccess(qualifier) -> {
                handleRecordSetFieldAccess(qualifier as PyReferenceExpression, element, odooService, result)
            }
            
            // Environment access (e.g., self.env['model'].field_name)
            isEnvironmentAccess(qualifier) -> {
                handleEnvironmentFieldAccess(qualifier!!, element, odooService, result)
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
        return qualifier is PyReferenceExpression && qualifier.name != "self"
    }
    
    private fun isEnvironmentAccess(qualifier: PyExpression?): Boolean {
        // Check for patterns like self.env['model'] or self.env.ref('model')
        if (qualifier is PySubscriptionExpression) {
            val operand = qualifier.operand
            return operand is PyQualifiedExpression && 
                   operand.asQualifiedName()?.toString()?.endsWith(".env") == true
        }
        return false
    }
    
    private fun addModelFieldCompletions(
        odooService: OdooProjectService, 
        modelName: String, 
        result: CompletionResultSet,
        element: PsiElement
    ) {
        // Use enhanced field cache for performance
        val fieldCache = OdooFieldCache.getInstance(element.project)
        val fields = fieldCache.getModelFields(modelName)
        
        // Create enhanced field info with better metadata
        fields.forEach { field ->
            val fieldInfo = createEnhancedFieldInfo(field, odooService, modelName)
            val lookupElement = createFieldLookupElement(fieldInfo)
            result.addElement(lookupElement)
        }
        
        // Add methods for this model with enhanced metadata
        addModelMethodCompletions(odooService, modelName, result)
        
        // Add computed field suggestions based on context
        addComputedFieldSuggestions(odooService, modelName, result, element)
    }
    
    private fun createEnhancedFieldInfo(field: OdooField, odooService: OdooProjectService, modelName: String): OdooFieldInfo {
        val relatedModel = if (field.type in listOf("Many2one", "One2many", "Many2many")) {
            odooService.extractRelatedModelFromField(field)
        } else null
        
        // Extract additional metadata from PSI
        val isComputed = extractIsComputed(field.psiElement)
        val isReadonly = extractIsReadonly(field.psiElement)
        val help = extractHelpText(field.psiElement)
        val defaultValue = extractDefaultValue(field.psiElement)
        val domain = extractDomain(field.psiElement)
        val selection = extractSelection(field.psiElement)
        
        return OdooFieldInfo(
            name = field.name,
            type = field.type,
            isRequired = field.isRequired,
            isReadonly = isReadonly,
            isComputed = isComputed,
            relatedModel = relatedModel,
            domain = domain,
            selection = selection,
            help = help,
            defaultValue = defaultValue,
            psiElement = field.psiElement
        )
    }
    
    private fun createFieldLookupElement(fieldInfo: OdooFieldInfo): LookupElement {
        val builder = LookupElementBuilder.create(fieldInfo.name)
            .withIcon(getEnhancedFieldIcon(fieldInfo))
            .withTypeText(buildTypeText(fieldInfo))
            .withTailText(buildTailText(fieldInfo), true)
        
        // Add enhanced insert handler for relational fields
        if (fieldInfo.relatedModel != null) {
            builder.withInsertHandler { context: InsertionContext, item: LookupElement ->
                handleRelationalFieldInsertion(context, fieldInfo)
            }
        }
        
        return builder
    }
    
    private fun buildTypeText(fieldInfo: OdooFieldInfo): String {
        val typeText = mutableListOf<String>()
        typeText.add(fieldInfo.type)
        
        if (fieldInfo.relatedModel != null) {
            typeText.add("→ ${fieldInfo.relatedModel}")
        }
        
        return typeText.joinToString(" ")
    }
    
    private fun buildTailText(fieldInfo: OdooFieldInfo): String {
        val tailParts = mutableListOf<String>()
        
        if (fieldInfo.isRequired) tailParts.add("required")
        if (fieldInfo.isReadonly) tailParts.add("readonly") 
        if (fieldInfo.isComputed) tailParts.add("computed")
        
        return if (tailParts.isNotEmpty()) " (${tailParts.joinToString(", ")})" else ""
    }
    
    private fun handleRelationalFieldInsertion(context: InsertionContext, fieldInfo: OdooFieldInfo) {
        // For relational fields, we can add smart suggestions for chaining
        if (fieldInfo.type == "Many2one") {
            // Could potentially add auto-completion triggers for related fields
            val editor = context.editor
            val offset = context.tailOffset
            
            // Add a period and trigger completion for related fields
            editor.document.insertString(offset, ".")
            editor.caretModel.moveToOffset(offset + 1)
            
            // Trigger auto-completion for the related model
            // Auto-completion would be triggered here in a real implementation
        }
    }
    
    private fun handleChainedFieldAccess(
        qualifier: PyQualifiedExpression, 
        element: PsiElement, 
        odooService: OdooProjectService, 
        result: CompletionResultSet
    ) {
        val qualifierChain = buildQualifierChain(qualifier)
        if (qualifierChain.size < 2) return
        
        val baseModelName = determineModelFromContext(element) ?: return
        var currentModelName = baseModelName
        
        // Navigate through the chain with enhanced performance
        for (i in 1 until qualifierChain.size) {
            val fieldName = qualifierChain[i]
            
            // Use field cache for performance
            val fieldCache = OdooFieldCache.getInstance(element.project)
            val fields = fieldCache.getModelFields(currentModelName)
            val field = fields.find { it.name == fieldName }
            
            if (field?.type in listOf("Many2one", "One2many", "Many2many")) {
                currentModelName = odooService.extractRelatedModelFromField(field!!) ?: return
            } else {
                return // Chain broken
            }
        }
        
        // Add completions for the final model in the chain
        addModelFieldCompletions(odooService, currentModelName, result, element)
    }
    
    private fun handleEnvironmentFieldAccess(
        qualifier: PyExpression, 
        element: PsiElement,
        odooService: OdooProjectService,
        result: CompletionResultSet
    ) {
        if (qualifier is PySubscriptionExpression) {
            val modelNameExpr = qualifier.indexExpression as? PyStringLiteralExpression
            val modelName = modelNameExpr?.stringValue
            
            if (modelName != null) {
                addModelFieldCompletions(odooService, modelName, result, element)
            }
        }
    }
    
    private fun addModelMethodCompletions(odooService: OdooProjectService, modelName: String, result: CompletionResultSet) {
        val methods = odooService.getModelMethods(modelName)
        methods.forEach { method ->
            val lookupElement = LookupElementBuilder.create(method.name)
                .withIcon(getMethodIcon(method.type))
                .withTypeText("${method.type.toString().lowercase()} method")
                .withTailText("(${method.parameters.joinToString(", ")})", true)
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
                    insertMethodCall(context, method)
                }
            
            result.addElement(lookupElement)
        }
        
        // Add common Odoo methods
        OdooConstants.COMMON_METHODS.values.forEach { method ->
            val lookupElement = LookupElementBuilder.create(method.name)
                .withIcon(getMethodIcon(method.type))
                .withTypeText("odoo method")
                .withTailText("(${method.parameters.joinToString(", ")})", true)
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
                    insertMethodCall(context, method)
                }
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addComputedFieldSuggestions(
        odooService: OdooProjectService,
        modelName: String,
        result: CompletionResultSet,
        element: PsiElement
    ) {
        // Add suggestions for accessing computed fields that might exist
        val model = odooService.findModel(modelName)
        if (model != null) {
            // Find compute methods in the class and suggest related field names
            model.psiClass.methods.forEach { method ->
                val methodName = method.name
                if (methodName?.startsWith("_compute_") == true) {
                    val fieldName = methodName.removePrefix("_compute_")
                    
                    val lookupElement = LookupElementBuilder.create(fieldName)
                        .withIcon(OdooIcons.COMPUTE_METHOD)
                        .withTypeText("computed field")
                        .withTailText(" (computed)", true)
                    
                    result.addElement(lookupElement)
                }
            }
        }
    }
    
    private fun insertMethodCall(context: InsertionContext, method: OdooMethod) {
        val editor = context.editor
        val offset = context.tailOffset
        
        if (method.parameters.isEmpty()) {
            editor.document.insertString(offset, "()")
            editor.caretModel.moveToOffset(offset + 2)
        } else {
            val paramList = method.parameters.joinToString(", ") { "$it=" }
            editor.document.insertString(offset, "($paramList)")
            editor.caretModel.moveToOffset(offset + 1)
        }
    }
    
    // Enhanced utility methods
    private fun getEnhancedFieldIcon(fieldInfo: OdooFieldInfo): javax.swing.Icon {
        return when {
            fieldInfo.isComputed -> OdooIcons.COMPUTE_METHOD
            fieldInfo.type in listOf("Many2one", "One2many", "Many2many") -> OdooIcons.RELATION_FIELD
            fieldInfo.type == "Selection" -> OdooIcons.SELECTION_FIELD
            fieldInfo.type == "Boolean" -> OdooIcons.BOOLEAN_FIELD
            fieldInfo.type in listOf("Date", "Datetime") -> OdooIcons.DATE_FIELD
            fieldInfo.type in listOf("Float", "Integer", "Monetary") -> OdooIcons.NUMBER_FIELD
            fieldInfo.type in listOf("Binary", "Image") -> OdooIcons.BINARY_FIELD
            else -> OdooIcons.FIELD
        }
    }
    
    // Extraction methods for enhanced metadata
    private fun extractIsComputed(psiElement: PsiElement?): Boolean {
        if (psiElement is PyTargetExpression) {
            val assignedValue = psiElement.findAssignedValue() as? PyCallExpression
            return assignedValue?.arguments?.any { arg ->
                arg is PyKeywordArgument && arg.keyword == "compute"
            } == true
        }
        return false
    }
    
    private fun extractIsReadonly(psiElement: PsiElement?): Boolean {
        if (psiElement is PyTargetExpression) {
            val assignedValue = psiElement.findAssignedValue() as? PyCallExpression
            assignedValue?.arguments?.forEach { arg ->
                if (arg is PyKeywordArgument && arg.keyword == "readonly") {
                    val valueExpr = arg.valueExpression
                    if (valueExpr is PyBoolLiteralExpression) {
                        return valueExpr.value
                    }
                }
            }
        }
        return false
    }
    
    private fun extractHelpText(psiElement: PsiElement?): String? {
        if (psiElement is PyTargetExpression) {
            val assignedValue = psiElement.findAssignedValue() as? PyCallExpression
            assignedValue?.arguments?.forEach { arg ->
                if (arg is PyKeywordArgument && arg.keyword == "help") {
                    val valueExpr = arg.valueExpression
                    if (valueExpr is PyStringLiteralExpression) {
                        return valueExpr.stringValue
                    }
                }
            }
        }
        return null
    }
    
    private fun extractDefaultValue(psiElement: PsiElement?): String? {
        if (psiElement is PyTargetExpression) {
            val assignedValue = psiElement.findAssignedValue() as? PyCallExpression
            assignedValue?.arguments?.forEach { arg ->
                if (arg is PyKeywordArgument && arg.keyword == "default") {
                    return arg.valueExpression?.text
                }
            }
        }
        return null
    }
    
    private fun extractDomain(psiElement: PsiElement?): String? {
        if (psiElement is PyTargetExpression) {
            val assignedValue = psiElement.findAssignedValue() as? PyCallExpression
            assignedValue?.arguments?.forEach { arg ->
                if (arg is PyKeywordArgument && arg.keyword == "domain") {
                    return arg.valueExpression?.text
                }
            }
        }
        return null
    }
    
    private fun extractSelection(psiElement: PsiElement?): List<Pair<String, String>>? {
        if (psiElement is PyTargetExpression) {
            val assignedValue = psiElement.findAssignedValue() as? PyCallExpression
            assignedValue?.arguments?.forEach { arg ->
                if (arg is PyKeywordArgument && arg.keyword == "selection") {
                    val valueExpr = arg.valueExpression
                    if (valueExpr is PyListLiteralExpression) {
                        val selection = mutableListOf<Pair<String, String>>()
                        valueExpr.elements.forEach { element ->
                            if (element is PyTupleExpression && element.elements.size == 2) {
                                val key = (element.elements[0] as? PyStringLiteralExpression)?.stringValue
                                val value = (element.elements[1] as? PyStringLiteralExpression)?.stringValue
                                if (key != null && value != null) {
                                    selection.add(key to value)
                                }
                            }
                        }
                        return selection
                    }
                }
            }
        }
        return null
    }
    
    // Keep existing utility methods
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
        var currentElement = element.parent
        while (currentElement != null && currentElement !is PyClass) {
            currentElement = currentElement.parent
        }
        
        val pyClass = currentElement as? PyClass ?: return null
        
        val nameAttribute = pyClass.classAttributes.find { it.name == "_name" }
        val nameValue = nameAttribute?.findAssignedValue()
        if (nameValue is PyStringLiteralExpression) {
            return nameValue.stringValue
        }
        
        return null
    }
    
    private fun handleRecordSetFieldAccess(qualifier: PyReferenceExpression, element: PsiElement,
                                         odooService: OdooProjectService, result: CompletionResultSet) {
        val modelName = inferModelFromVariable(qualifier, odooService) ?: return
        addModelFieldCompletions(odooService, modelName, result, element)
    }
    
    private fun inferModelFromVariable(variable: PyReferenceExpression, odooService: OdooProjectService): String? {
        // Enhanced variable type inference
        val name = variable.name ?: return null
        
        // Look for assignment patterns
        val containingFile = variable.containingFile as? PyFile
        containingFile?.let { file ->
            // Search for variable assignments in the current scope
            val assignments = findVariableAssignments(file, name)
            assignments.forEach { assignment ->
                val modelName = extractModelFromAssignment(assignment)
                if (modelName != null) return modelName
            }
        }
        
        return null
    }
    
    private fun findVariableAssignments(file: PyFile, variableName: String): List<PyAssignmentStatement> {
        val assignments = mutableListOf<PyAssignmentStatement>()
        
        file.acceptChildren(object : PyElementVisitor() {
            override fun visitPyAssignmentStatement(node: PyAssignmentStatement) {
                    node.targets.forEach { target ->
                        if (target is PyTargetExpression && target.name == variableName) {
                            assignments.add(node)
                        }
                    }
                    super.visitPyAssignmentStatement(node)
                }
        })
        
        return assignments
    }
    
    private fun extractModelFromAssignment(assignment: PyAssignmentStatement): String? {
        val assignedValue = assignment.assignedValue
        
        // Handle self.env['model'] pattern
        if (assignedValue is PySubscriptionExpression) {
            val operand = assignedValue.operand
            if (operand is PyQualifiedExpression && operand.asQualifiedName()?.toString()?.endsWith(".env") == true) {
                val indexExpr = assignedValue.indexExpression as? PyStringLiteralExpression
                return indexExpr?.stringValue
            }
        }
        
        // Handle self.search([]) pattern
        if (assignedValue is PyCallExpression) {
            val callee = assignedValue.callee
            if (callee is PyQualifiedExpression && callee.name in listOf("search", "browse", "create")) {
                // Try to infer model from the calling object
                return determineModelFromContext(assignment)
            }
        }
        
        return null
    }
    
    private fun getMethodIcon(methodType: OdooMethodType): javax.swing.Icon {
        return when (methodType) {
            OdooMethodType.COMPUTE -> OdooIcons.COMPUTE_METHOD
            OdooMethodType.API_ONCHANGE -> OdooIcons.ONCHANGE_METHOD
            OdooMethodType.CRUD -> OdooIcons.ORM_METHOD
            OdooMethodType.SEARCH -> OdooIcons.SEARCH_METHOD
            else -> OdooIcons.METHOD
        }
    }
}

/**
 * Completion provider for field attributes inside field constructors
 */
class OdooFieldAttributeCompletionProvider : CompletionProvider<CompletionParameters>() {
    
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
        val fieldType = determineFieldType(element)
        
        if (fieldType != null) {
            val attributes = OdooFieldAttributes.getAttributesForFieldType(fieldType)
            
            attributes.values.forEach { attribute ->
                val lookupElement = LookupElementBuilder.create(attribute.name)
                    .withIcon(OdooIcons.FIELD)
                    .withTypeText(attribute.type)
                    .withTailText(" = ${attribute.example}", true)
                    .withInsertHandler { context: InsertionContext, item: LookupElement ->
                        val editor = context.editor
                        val offset = context.tailOffset
                        editor.document.insertString(offset, "=")
                        editor.caretModel.moveToOffset(offset + 1)
                        
                        // Trigger completion for attribute values
                        // Auto-completion would be triggered here in a real implementation
                    }
                
                result.addElement(lookupElement)
            }
        }
    }
    
    private fun determineFieldType(element: PsiElement): String? {
        // Navigate up to find the field call expression
        var current = element.parent
        while (current != null && current !is PyCallExpression) {
            current = current.parent
        }
        
        val callExpr = current as? PyCallExpression ?: return null
        val callee = callExpr.callee as? PyQualifiedExpression ?: return null
        
        return callee.name
    }
}

/**
 * Completion provider for model names in comodel_name attribute
 */
class OdooComodelCompletionProvider : CompletionProvider<CompletionParameters>() {
    
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
        
        val allModels = odooService.getAllModels()
        
        allModels.forEach { model ->
            val lookupElement = LookupElementBuilder.create(model.name)
                .withIcon(OdooIcons.MODEL)
                .withTypeText("Odoo Model")
                .withTailText(" - ${model.description}", true)
            
            result.addElement(lookupElement)
        }
    }
}

/**
 * Completion provider for field names in related attribute
 */
class OdooRelatedFieldCompletionProvider : CompletionProvider<CompletionParameters>() {
    
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
        val currentText = getCurrentRelatedPath(element)
        val modelName = determineModelFromContext(element) ?: return
        
        if (currentText.isEmpty()) {
            // Suggest top-level fields from current model
            addFieldSuggestions(odooService, modelName, result, "", project)
        } else {
            // Handle chained field paths like "partner_id.name"
            handleChainedRelatedPath(odooService, modelName, currentText, result, project)
        }
    }
    
    private fun getCurrentRelatedPath(element: PsiElement): String {
        val stringLiteral = element.parent as? PyStringLiteralExpression
        return stringLiteral?.stringValue?.substringBeforeLast('.', "") ?: ""
    }
    
    private fun addFieldSuggestions(
        odooService: OdooProjectService,
        modelName: String,
        result: CompletionResultSet,
        prefix: String,
        project: com.intellij.openapi.project.Project
    ) {
        val fieldCache = OdooFieldCache.getInstance(project)
        val fields = fieldCache.getModelFields(modelName)
        
        fields.forEach { field ->
            val fullPath = if (prefix.isEmpty()) field.name else "$prefix.${field.name}"
            
            val lookupElement = LookupElementBuilder.create(fullPath)
                .withIcon(getFieldIcon(field.type))
                .withTypeText(field.type)
                .withTailText(if (field.type in listOf("Many2one", "One2many", "Many2many")) " (relational)" else "", true)
            
            result.addElement(lookupElement)
        }
    }
    
    private fun handleChainedRelatedPath(
        odooService: OdooProjectService,
        baseModelName: String,
        currentPath: String,
        result: CompletionResultSet,
        project: com.intellij.openapi.project.Project
    ) {
        val pathParts = currentPath.split(".")
        var currentModelName = baseModelName
        
        // Navigate through the path to find the current model
        for (fieldName in pathParts) {
            val fieldCache = OdooFieldCache.getInstance(project)
            val fields = fieldCache.getModelFields(currentModelName)
            val field = fields.find { it.name == fieldName }
            
            if (field?.type in listOf("Many2one", "One2many", "Many2many")) {
                currentModelName = odooService.extractRelatedModelFromField(field!!) ?: return
            } else {
                return // Path is broken or ends with non-relational field
            }
        }
        
        // Add field suggestions for the current model
        addFieldSuggestions(odooService, currentModelName, result, currentPath, project)
    }
    
    private fun determineModelFromContext(element: PsiElement): String? {
        var current = element.parent
        while (current != null && current !is PyClass) {
            current = current.parent
        }
        
        val pyClass = current as? PyClass ?: return null
        val nameAttribute = pyClass.classAttributes.find { it.name == "_name" }
        val nameValue = nameAttribute?.findAssignedValue()
        
        if (nameValue is PyStringLiteralExpression) {
            return nameValue.stringValue
        }
        
        return null
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

/**
 * Completion provider for compute method names
 */
class OdooComputeMethodCompletionProvider : CompletionProvider<CompletionParameters>() {
    
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
        val modelName = determineModelFromContext(element) ?: return
        val attributeName = determineAttributeName(element) ?: return
        
        // Find methods that match the attribute type
        val model = odooService.findModel(modelName)
        if (model != null) {
            model.psiClass.methods.forEach { method ->
                val methodName = method.name ?: return@forEach
                
                val isRelevant = when (attributeName) {
                    "compute" -> methodName.startsWith("_compute_")
                    "inverse" -> methodName.startsWith("_inverse_")
                    "search" -> methodName.startsWith("_search_")
                    else -> false
                }
                
                if (isRelevant) {
                    val lookupElement = LookupElementBuilder.create(methodName)
                        .withIcon(OdooIcons.COMPUTE_METHOD)
                        .withTypeText("$attributeName method")
                        .withTailText(buildMethodSignature(method), true)
                    
                    result.addElement(lookupElement)
                }
            }
            
            // Suggest method name patterns for new methods
            if (attributeName == "compute") {
                val fieldName = determineCurrentFieldName(element)
                if (fieldName != null) {
                    val suggestedMethodName = "_compute_$fieldName"
                    
                    val lookupElement = LookupElementBuilder.create(suggestedMethodName)
                        .withIcon(OdooIcons.COMPUTE_METHOD)
                        .withTypeText("new compute method")
                        .withTailText(" (create new)", true)
                        .withInsertHandler { context: InsertionContext, item: LookupElement ->
                            // Could potentially create the method stub
                        }
                    
                    result.addElement(lookupElement)
                }
            }
        }
    }
    
    private fun determineAttributeName(element: PsiElement): String? {
        var current = element.parent
        while (current != null && current !is PyKeywordArgument) {
            current = current.parent
        }
        
        val keywordArg = current as? PyKeywordArgument
        return keywordArg?.keyword
    }
    
    private fun determineCurrentFieldName(element: PsiElement): String? {
        var current = element.parent
        while (current != null && current !is PyAssignmentStatement) {
            current = current.parent
        }
        
        val assignment = current as? PyAssignmentStatement
        val target = assignment?.targets?.firstOrNull() as? PyTargetExpression
        return target?.name
    }
    
    private fun buildMethodSignature(method: PyFunction): String {
        val params = method.parameterList.parameters
            .filter { it.name != "self" }
            .mapNotNull { it.name }
            .joinToString(", ")
        
        return "($params)"
    }
    
    private fun determineModelFromContext(element: PsiElement): String? {
        var current = element.parent
        while (current != null && current !is PyClass) {
            current = current.parent
        }
        
        val pyClass = current as? PyClass ?: return null
        val nameAttribute = pyClass.classAttributes.find { it.name == "_name" }
        val nameValue = nameAttribute?.findAssignedValue()
        
        if (nameValue is PyStringLiteralExpression) {
            return nameValue.stringValue
        }
        
        return null
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
        
        // Add enhanced Odoo field types with smart insertion
        OdooConstants.FIELD_TYPES.forEach { fieldType ->
            val lookupElement = LookupElementBuilder.create(fieldType)
                .withIcon(getFieldTypeIcon(fieldType))
                .withTypeText("Odoo Field")
                .withTailText(getFieldDescription(fieldType), true)
                .withInsertHandler { context: InsertionContext, item: LookupElement ->
                    insertFieldWithSmartAttributes(context, fieldType, odooService)
                }
            
            result.addElement(lookupElement)
        }
    }
    
    private fun insertFieldWithSmartAttributes(
        context: InsertionContext,
        fieldType: String,
        odooService: OdooProjectService
    ) {
        val editor = context.editor
        val caretOffset = editor.caretModel.offset
        
        // Generate smart field constructor based on field type
        val constructor = generateSmartConstructor(fieldType, context, odooService)
        
        editor.document.insertString(caretOffset, constructor)
        
        // Position cursor for optimal editing
        val cursorPosition = findOptimalCursorPosition(constructor, caretOffset)
        editor.caretModel.moveToOffset(cursorPosition)
        
        // Trigger completion for attributes if appropriate
        if (constructor.contains("=")) {
            // Auto-completion would be triggered here in a real implementation
        }
    }
    
    private fun generateSmartConstructor(
        fieldType: String,
        context: InsertionContext,
        odooService: OdooProjectService
    ): String {
        val attributes = mutableListOf<String>()
        
        when (fieldType) {
            "Char" -> {
                attributes.add("string=''")
                // Could add size based on field name analysis
            }
            "Text" -> {
                attributes.add("string=''")
            }
            "Boolean" -> {
                attributes.add("string=''")
                attributes.add("default=False")
            }
            "Selection" -> {
                attributes.add("selection=[]")
                attributes.add("string=''")
            }
            "Many2one" -> {
                // Try to infer comodel from field name
                val fieldName = getCurrentFieldName(context)
                val inferredModel = inferComodelFromFieldName(fieldName, odooService)
                
                if (inferredModel != null) {
                    attributes.add("comodel_name='$inferredModel'")
                } else {
                    attributes.add("comodel_name=''")
                }
                attributes.add("string=''")
            }
            "One2many" -> {
                attributes.add("comodel_name=''")
                attributes.add("inverse_name=''")
                attributes.add("string=''")
            }
            "Many2many" -> {
                attributes.add("comodel_name=''")
                attributes.add("string=''")
            }
            "Date" -> {
                attributes.add("string=''")
                // Add common date field patterns
                val fieldName = getCurrentFieldName(context)
                if (fieldName?.contains("date") == true) {
                    if (fieldName.contains("birth") || fieldName.contains("dob")) {
                        // Birth date typically doesn't need default
                    } else {
                        attributes.add("default=fields.Date.today")
                    }
                }
            }
            "Datetime" -> {
                attributes.add("string=''")
                val fieldName = getCurrentFieldName(context)
                if (fieldName?.contains("create") == true || fieldName?.contains("write") == true) {
                    attributes.add("default=fields.Datetime.now")
                }
            }
            "Float", "Integer" -> {
                attributes.add("string=''")
                attributes.add("default=0")
            }
            "Monetary" -> {
                attributes.add("string=''")
                attributes.add("currency_field='currency_id'")
            }
            "Binary", "Image" -> {
                attributes.add("string=''")
                attributes.add("attachment=True")
            }
        }
        
        return if (attributes.isNotEmpty()) {
            "(${attributes.joinToString(", ")})"
        } else {
            "()"
        }
    }
    
    private fun getCurrentFieldName(context: InsertionContext): String? {
        val document = context.document
        val lineStart = document.getLineStartOffset(document.getLineNumber(context.startOffset))
        val lineText = document.getText(TextRange(lineStart, context.startOffset))
        
        // Extract field name from line like "field_name = fields."
        val regex = Regex("""(\w+)\s*=\s*fields\.$""")
        val match = regex.find(lineText)
        return match?.groupValues?.get(1)
    }
    
    private fun inferComodelFromFieldName(fieldName: String?, odooService: OdooProjectService): String? {
        if (fieldName == null) return null
        
        // Common field name patterns
        val patterns = mapOf(
            "partner_id" to "res.partner",
            "user_id" to "res.users",
            "company_id" to "res.company",
            "currency_id" to "res.currency",
            "country_id" to "res.country",
            "state_id" to "res.country.state",
            "category_id" to "product.category",
            "product_id" to "product.product",
            "sale_order_id" to "sale.order",
            "purchase_order_id" to "purchase.order",
            "invoice_id" to "account.move",
            "move_id" to "account.move",
            "move_line_id" to "account.move.line",
            "account_id" to "account.account",
            "journal_id" to "account.journal"
        )
        
        // Direct match
        patterns[fieldName]?.let { return it }
        
        // Pattern matching for common suffixes
        when {
            fieldName.endsWith("_partner_id") -> return "res.partner"
            fieldName.endsWith("_user_id") -> return "res.users"
            fieldName.endsWith("_company_id") -> return "res.company"
            fieldName.endsWith("_product_id") -> return "product.product"
            fieldName.endsWith("_category_id") -> return "product.category"
            fieldName.endsWith("_currency_id") -> return "res.currency"
            fieldName.endsWith("_country_id") -> return "res.country"
            fieldName.endsWith("_state_id") -> return "res.country.state"
        }
        
        // Try to find models that match the field name pattern
        val modelPrefix = fieldName.removeSuffix("_id").replace("_", ".")
        val allModels = odooService.getAllModels()
        val matchingModel = allModels.find { it.name == modelPrefix }
        
        return matchingModel?.name
    }
    
    private fun findOptimalCursorPosition(constructor: String, baseOffset: Int): Int {
        // Find first empty string or value that needs to be filled
        val emptyStringIndex = constructor.indexOf("''")
        if (emptyStringIndex != -1) {
            return baseOffset + emptyStringIndex + 1
        }
        
        val emptyListIndex = constructor.indexOf("[]")
        if (emptyListIndex != -1) {
            return baseOffset + emptyListIndex + 1
        }
        
        // Default to inside parentheses
        return baseOffset + 1
    }
    
    private fun getFieldDescription(fieldType: String): String {
        return when (fieldType) {
            "Char" -> " - Text field with size limit"
            "Text" -> " - Large text field"
            "Html" -> " - HTML formatted text"
            "Boolean" -> " - True/False checkbox"
            "Integer" -> " - Whole number"
            "Float" -> " - Decimal number"
            "Monetary" -> " - Currency amount"
            "Date" -> " - Date only"
            "Datetime" -> " - Date and time"
            "Selection" -> " - Dropdown selection"
            "Many2one" -> " - Link to one record"
            "One2many" -> " - List of related records"
            "Many2many" -> " - Multiple selections"
            "Binary" -> " - File attachment"
            "Image" -> " - Image file"
            "Json" -> " - JSON data"
            "Properties" -> " - Dynamic properties"
            else -> ""
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