package com.tmc.odoo.pycharm.references

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.*
import com.tmc.odoo.pycharm.services.OdooProjectService
import com.tmc.odoo.pycharm.services.OdooFieldCache
import com.tmc.odoo.pycharm.models.OdooField
import com.tmc.odoo.pycharm.icons.OdooIcons
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElement

class OdooReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Model name references in string literals
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java),
            OdooModelReferenceProvider()
        )
        
        // Compute/inverse/search method references
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java),
            OdooComputeReferenceProvider()
        )
        
        // Mixin class references
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyReferenceExpression::class.java),
            OdooMixinReferenceProvider()
        )
        
        // Comodel references in relational fields
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java),
            OdooComodelReferenceProvider()
        )
        
        // Related field path references
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java),
            OdooRelatedFieldReferenceProvider()
        )
        
        // Field references in domain/context expressions
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java),
            OdooFieldReferenceProvider()
        )
    }
}

class OdooModelReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PyStringLiteralExpression) return PsiReference.EMPTY_ARRAY
        
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        if (!odooService.isOdooProject()) return PsiReference.EMPTY_ARRAY
        
        val stringValue = element.stringValue
        if (stringValue.isBlank()) return PsiReference.EMPTY_ARRAY
        
        // Check if this string literal represents an Odoo model name
        if (isModelReference(element) || isInheritReference(element)) {
            return arrayOf(OdooModelReference(element, stringValue))
        }
        
        return PsiReference.EMPTY_ARRAY
    }
    
    private fun isModelReference(element: PyStringLiteralExpression): Boolean {
        val parent = element.parent
        
        // Check for patterns like self.env['model.name']
        if (parent is PySubscriptionExpression) {
            val operand = parent.operand
            if (operand is PyReferenceExpression && operand.text == "self.env") {
                return true
            }
        }
        
        // Check for patterns like env['model.name']
        if (parent is PySubscriptionExpression) {
            val operand = parent.operand
            if (operand is PyReferenceExpression && operand.name == "env") {
                return true
            }
        }
        
        return false
    }
    
    private fun isInheritReference(element: PyStringLiteralExpression): Boolean {
        // Check if this string is assigned to _inherit or _name attribute
        val assignmentStatement = element.parent?.parent as? PyAssignmentStatement ?: return false
        val targets = assignmentStatement.targets
        
        // Check for _inherit = "model.name" or _name = "model.name"
        for (target in targets) {
            if (target is PyTargetExpression && target.name in listOf("_inherit", "_name")) {
                return true
            }
        }
        
        // Check for _inherit list assignment: _inherit = ["model1", "model2"]
        val listExpression = element.parent as? PyListLiteralExpression
        if (listExpression != null) {
            val listAssignment = listExpression.parent?.parent as? PyAssignmentStatement
            val listTargets = listAssignment?.targets
            for (target in listTargets ?: emptyArray()) {
                if (target is PyTargetExpression && target.name == "_inherit") {
                    return true
                }
            }
        }
        
        return false
    }
}

class OdooComputeReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PyStringLiteralExpression) return PsiReference.EMPTY_ARRAY
        
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        if (!odooService.isOdooProject()) return PsiReference.EMPTY_ARRAY
        
        val stringValue = element.stringValue
        if (stringValue.isBlank()) return PsiReference.EMPTY_ARRAY
        
        // Check if this string literal is a compute function reference
        if (isComputeReference(element)) {
            return arrayOf(OdooComputeReference(element, stringValue))
        }
        
        return PsiReference.EMPTY_ARRAY
    }
    
    private fun isComputeReference(element: PyStringLiteralExpression): Boolean {
        // Check if this string is assigned to compute parameter in field definition
        val keywordArgument = element.parent as? PyKeywordArgument ?: return false
        
        // Check if the keyword is 'compute'
        if (keywordArgument.keyword != "compute") return false
        
        // Check if we're inside a field definition
        val callExpression = keywordArgument.parent as? PyArgumentList ?: return false
        val fieldCall = callExpression.parent as? PyCallExpression ?: return false
        val callee = fieldCall.callee as? PyQualifiedExpression ?: return false
        
        // Check if this is a fields.* call
        return callee.qualifier?.name == "fields" && isOdooFieldType(callee.name)
    }
    
    private fun isOdooFieldType(typeName: String?): Boolean {
        val odooFieldTypes = setOf(
            "Char", "Text", "Html", "Boolean", "Integer", "Float", "Monetary",
            "Date", "Datetime", "Selection", "Many2one", "One2many", "Many2many",
            "Binary", "Image", "Json", "Properties"
        )
        return typeName in odooFieldTypes
    }
}

class OdooComputeReference(
    element: PyStringLiteralExpression,
    private val computeMethodName: String
) : PsiReferenceBase<PyStringLiteralExpression>(element) {
    
    override fun resolve(): PsiElement? {
        // Find the compute method in the current class
        val pyClass = findContainingOdooClass() ?: return null
        
        // Look for the compute method in the class
        val computeMethod = pyClass.findMethodByName(computeMethodName, false, null)
        return computeMethod
    }
    
    override fun getVariants(): Array<Any> {
        val pyClass = findContainingOdooClass() ?: return emptyArray()
        
        // Return all compute methods (methods starting with _compute_)
        return pyClass.methods
            .filter { it.name?.startsWith("_compute_") == true }
            .mapNotNull { it.name }
            .toTypedArray()
    }
    
    private fun findContainingOdooClass(): PyClass? {
        var current = element.parent
        while (current != null) {
            if (current is PyClass) {
                // Check if this is an Odoo model class
                val project = element.project
                val odooService = OdooProjectService.getInstance(project)
                
                if (odooService.isOdooModel(current)) {
                    return current
                }
            }
            current = current.parent
        }
        return null
    }
}

class OdooMixinReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PyReferenceExpression) return PsiReference.EMPTY_ARRAY
        
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        if (!odooService.isOdooProject()) return PsiReference.EMPTY_ARRAY
        
        // Check if this reference is in a class inheritance context
        if (isMixinReference(element)) {
            val referenceName = element.name ?: return PsiReference.EMPTY_ARRAY
            return arrayOf(OdooMixinReference(element, referenceName))
        }
        
        return PsiReference.EMPTY_ARRAY
    }
    
    private fun isMixinReference(element: PyReferenceExpression): Boolean {
        // Check if this reference is in a class inheritance list
        val pyClass = element.parent?.parent as? PyClass ?: return false
        
        // Check if the reference is in the superclass expressions
        return pyClass.superClassExpressions.any { expr ->
            expr == element || 
            (expr is PyQualifiedExpression && (expr.qualifier == element || expr.reference == element))
        }
    }
}

class OdooMixinReference(
    element: PyReferenceExpression,
    private val mixinName: String
) : PsiReferenceBase<PyReferenceExpression>(element) {
    
    override fun resolve(): PsiElement? {
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        // First try to find as a model by class name
        val models = odooService.getAllModels()
        val mixinModel = models.find { model ->
            model.className == mixinName ||
            model.className.endsWith(".$mixinName") ||
            model.name == mixinName
        }
        
        if (mixinModel != null) {
            return mixinModel.psiClass
        }
        
        // Try to resolve as a regular Python class in the project
        // This would handle cases like models.Model, MailThread, etc.
        return null // Let PyCharm's default resolution handle this
    }
    
    override fun getVariants(): Array<Any> {
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        val variants = mutableListOf<String>()
        
        // Add abstract models/mixins
        val models = odooService.getAllModels()
        models.filter { model ->
            model.className.contains("Mixin") || 
            model.className.contains("Abstract") ||
            model.name.contains("abstract")
        }.forEach { model ->
            variants.add(model.className)
        }
        
        // Add common Odoo base classes and mixins
        variants.addAll(listOf(
            "models.Model", "models.TransientModel", "models.AbstractModel",
            "MailThread", "MailActivityMixin", "UtmMixin", "WebsiteMixin",
            "RatingMixin", "PortalMixin", "ImageMixin", "SequenceMixin"
        ))
        
        return variants.toTypedArray()
    }
}

class OdooModelReference(
    element: PyStringLiteralExpression,
    private val modelName: String
) : PsiReferenceBase<PyStringLiteralExpression>(element) {
    
    override fun resolve(): PsiElement? {
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        val model = odooService.findModel(modelName)
        return model?.psiClass
    }
    
    override fun getVariants(): Array<Any> {
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        return odooService.getAllModels()
            .map { it.name }
            .toTypedArray()
    }
}

/**
 * Reference provider for comodel_name attributes in relational fields
 */
class OdooComodelReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PyStringLiteralExpression) {
            return PsiReference.EMPTY_ARRAY
        }
        
        if (!isComodelReference(element)) {
            return PsiReference.EMPTY_ARRAY
        }
        
        return arrayOf(OdooComodelReference(element))
    }
    
    private fun isComodelReference(element: PyStringLiteralExpression): Boolean {
        val parent = element.parent
        return parent is PyKeywordArgument && parent.keyword == "comodel_name"
    }
}

/**
 * Reference for comodel_name navigation
 */
class OdooComodelReference(element: PyStringLiteralExpression) : 
    PsiReferenceBase<PyStringLiteralExpression>(element) {
    
    override fun resolve(): PsiElement? {
        val modelName = element.stringValue
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        val model = odooService.findModel(modelName)
        return model?.psiClass
    }
    
    override fun getVariants(): Array<Any> {
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        val models = odooService.getAllModels()
        
        return models.map { model ->
            LookupElementBuilder.create(model.name)
                .withIcon(OdooIcons.MODEL)
                .withTypeText("Odoo Model")
                .withTailText(" - ${model.description}", true)
        }.toTypedArray()
    }
}

/**
 * Reference provider for related field paths
 */
class OdooRelatedFieldReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PyStringLiteralExpression) {
            return PsiReference.EMPTY_ARRAY
        }
        
        if (!isRelatedFieldReference(element)) {
            return PsiReference.EMPTY_ARRAY
        }
        
        return arrayOf(OdooRelatedFieldReference(element))
    }
    
    private fun isRelatedFieldReference(element: PyStringLiteralExpression): Boolean {
        val parent = element.parent
        return parent is PyKeywordArgument && parent.keyword == "related"
    }
}

/**
 * Reference for related field path navigation
 */
class OdooRelatedFieldReference(element: PyStringLiteralExpression) : 
    PsiReferenceBase<PyStringLiteralExpression>(element) {
    
    override fun resolve(): PsiElement? {
        val relatedPath = element.stringValue
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        val fieldPath = relatedPath.split(".")
        if (fieldPath.isEmpty()) return null
        
        val baseModelName = determineModelFromContext(element) ?: return null
        var currentModelName = baseModelName
        
        // Navigate through the field path
        for (i in 0 until fieldPath.size - 1) {
            val fieldName = fieldPath[i]
            val fieldCache = OdooFieldCache.getInstance(project)
            val fields = fieldCache.getModelFields(currentModelName)
            val field = fields.find { it.name == fieldName }
            
            if (field?.type in listOf("Many2one", "One2many", "Many2many")) {
                currentModelName = odooService.extractRelatedModelFromField(field!!) ?: return null
            } else {
                return null
            }
        }
        
        // Find the final field
        val finalFieldName = fieldPath.last()
        val fieldCache = OdooFieldCache.getInstance(project)
        val fields = fieldCache.getModelFields(currentModelName)
        val finalField = fields.find { it.name == finalFieldName }
        
        return finalField?.psiElement
    }
    
    override fun getVariants(): Array<Any> {
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        val baseModelName = determineModelFromContext(element) ?: return emptyArray()
        
        return buildRelatedFieldVariants(odooService, baseModelName, "", 3).map { it as Any }.toTypedArray()
    }
    
    private fun buildRelatedFieldVariants(
        odooService: OdooProjectService,
        modelName: String,
        prefix: String,
        maxDepth: Int
    ): Array<LookupElement> {
        if (maxDepth <= 0) return emptyArray()
        
        val variants = mutableListOf<LookupElement>()
        val project = element.project
        val fieldCache = OdooFieldCache.getInstance(project)
        val fields = fieldCache.getModelFields(modelName)
        
        fields.forEach { field ->
            val fullPath = if (prefix.isEmpty()) field.name else "$prefix.${field.name}"
            
            val lookupElement = LookupElementBuilder.create(fullPath)
                .withIcon(getFieldIcon(field.type))
                .withTypeText(field.type)
                .withTailText(buildFieldTail(field), true)
            
            variants.add(lookupElement)
            
            // Add chained fields for relational fields
            if (field.type in listOf("Many2one", "One2many", "Many2many") && maxDepth > 1) {
                val relatedModelName = odooService.extractRelatedModelFromField(field)
                if (relatedModelName != null) {
                    val chainedVariants = buildRelatedFieldVariants(
                        odooService, relatedModelName, fullPath, maxDepth - 1
                    )
                    variants.addAll(chainedVariants)
                }
            }
        }
        
        return variants.toTypedArray()
    }
    
    private fun buildFieldTail(field: OdooField): String {
        val parts = mutableListOf<String>()
        if (field.isRequired) parts.add("required")
        if (field.type in listOf("Many2one", "One2many", "Many2many")) parts.add("relational")
        return if (parts.isNotEmpty()) " (${parts.joinToString(", ")})" else ""
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

/**
 * Reference provider for field names in domain/context expressions
 */
class OdooFieldReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PyStringLiteralExpression) {
            return PsiReference.EMPTY_ARRAY
        }
        
        if (!isFieldInDomainOrContext(element)) {
            return PsiReference.EMPTY_ARRAY
        }
        
        return arrayOf(OdooFieldReference(element))
    }
    
    private fun isFieldInDomainOrContext(element: PyStringLiteralExpression): Boolean {
        // Check if this string is inside a domain or context expression
        var parent = element.parent
        while (parent != null) {
            if (parent is PyKeywordArgument) {
                val keyword = parent.keyword
                if (keyword in listOf("domain", "context")) {
                    return true
                }
            }
            if (parent is PyListLiteralExpression) {
                // Check if this list is a domain
                val listParent = parent.parent
                if (listParent is PyKeywordArgument && listParent.keyword == "domain") {
                    return true
                }
            }
            parent = parent.parent
        }
        return false
    }
}

/**
 * Reference for field names in domain/context expressions
 */
class OdooFieldReference(element: PyStringLiteralExpression) : 
    PsiReferenceBase<PyStringLiteralExpression>(element) {
    
    override fun resolve(): PsiElement? {
        val fieldName = element.stringValue
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        val modelName = determineModelFromContext(element) ?: return null
        
        val fieldCache = OdooFieldCache.getInstance(project)
        val fields = fieldCache.getModelFields(modelName)
        val field = fields.find { it.name == fieldName }
        
        return field?.psiElement
    }
    
    override fun getVariants(): Array<Any> {
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        val modelName = determineModelFromContext(element) ?: return emptyArray()
        
        val fieldCache = OdooFieldCache.getInstance(project)
        val fields = fieldCache.getModelFields(modelName)
        
        return fields.map { field ->
            LookupElementBuilder.create(field.name)
                .withIcon(getFieldIcon(field.type))
                .withTypeText(field.type)
                .withTailText(if (field.isRequired) " (required)" else "", true)
        }.toTypedArray()
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