package com.tmc.odoo.pycharm.references

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.*
import com.tmc.odoo.pycharm.services.OdooProjectService

class OdooReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java),
            OdooModelReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java),
            OdooComputeReferenceProvider()
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyReferenceExpression::class.java),
            OdooMixinReferenceProvider()
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