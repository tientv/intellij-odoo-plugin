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
        // Check if this string is assigned to _inherit attribute
        val assignmentStatement = element.parent?.parent as? PyAssignmentStatement ?: return false
        val targets = assignmentStatement.targets
        
        // Check for _inherit = "model.name"
        for (target in targets) {
            if (target is PyTargetExpression && target.name == "_inherit") {
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