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
        if (isModelReference(element)) {
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