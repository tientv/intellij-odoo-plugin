package com.tmc.odoo.pycharm.references

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.PythonLanguage
import com.tmc.odoo.pycharm.services.OdooProjectService

class OdooReferenceContributor : PsiReferenceContributor() {
    
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register reference provider for string literals that might contain model names
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java)
                .withLanguage(PythonLanguage.getInstance()),
            OdooModelReferenceProvider()
        )
    }
}

class OdooModelReferenceProvider : PsiReferenceProvider() {
    
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val stringLiteral = element as? PyStringLiteralExpression ?: return emptyArray()
        val value = stringLiteral.stringValue
        
        // Check if this looks like a model name (contains dots)
        if (value.contains('.')) {
            val project = element.project
            val odooService = OdooProjectService.getInstance(project)
            
            if (odooService.isOdooProject()) {
                return arrayOf(OdooModelReference(element, value))
            }
        }
        
        return emptyArray()
    }
}

class OdooModelReference(
    element: PsiElement,
    private val modelName: String
) : PsiReferenceBase<PsiElement>(element) {
    
    override fun resolve(): PsiElement? {
        val project = element.project
        val service = OdooProjectService.getInstance(project)
        val model = service.findModel(modelName)
        return model?.psiClass
    }
    
    override fun getVariants(): Array<Any> {
        val project = element.project
        val service = OdooProjectService.getInstance(project)
        return service.getAllModels().map { model ->
            com.intellij.codeInsight.lookup.LookupElementBuilder.create(model.name)
                .withIcon(com.tmc.odoo.pycharm.icons.OdooIcons.MODEL)
                .withTypeText(model.modulePath)
        }.toTypedArray()
    }
}