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

class OdooMixinCompletionContributor : CompletionContributor() {
    
    init {
        // Complete mixin class names in inheritance
        extend(
            CompletionType.BASIC,
            mixinInheritancePattern(),
            OdooMixinCompletionProvider()
        )
    }
    
    private fun mixinInheritancePattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(PlatformPatterns.psiElement(PyReferenceExpression::class.java))
            .inside(PlatformPatterns.psiElement(PyArgumentList::class.java))
            .inside(PlatformPatterns.psiElement(PyClass::class.java))
    }
}

class OdooMixinCompletionProvider : CompletionProvider<CompletionParameters>() {
    
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
        
        // Add common Odoo mixin patterns
        addOdooMixinPatterns(result)
        
        // Add abstract models (mixins) from current project
        val models = odooService.getAllModels()
        val abstractModels = models.filter { model ->
            model.className.contains("Mixin") || 
            model.className.contains("Abstract") ||
            model.name.contains("abstract")
        }
        
        abstractModels.forEach { model ->
            val lookupElement = LookupElementBuilder.create(model.className)
                .withIcon(OdooIcons.MODEL)
                .withTypeText("Mixin (${model.name})")
                .withTailText(" (${model.description})", true)
                .withInsertHandler { context, item ->
                    // Auto-import the mixin if needed
                    // TODO: Implement auto-import logic
                }
            
            result.addElement(lookupElement)
        }
        
        // Add models.* base classes
        addOdooBaseClasses(result)
    }
    
    private fun addOdooMixinPatterns(result: CompletionResultSet) {
        val commonMixins = listOf(
            "MailThread" to "Mail & Activity tracking",
            "MailActivityMixin" to "Activity tracking",
            "UtmMixin" to "UTM tracking",
            "WebsiteMixin" to "Website features",
            "RatingMixin" to "Rating system",
            "PortalMixin" to "Portal access",
            "ImageMixin" to "Image handling",
            "SequenceMixin" to "Sequence handling",
            "SaleOrderTemplateMixin" to "Sale order templates",
            "PurchaseOrderTemplateMixin" to "Purchase order templates"
        )
        
        commonMixins.forEach { (mixinName, description) ->
            val lookupElement = LookupElementBuilder.create(mixinName)
                .withIcon(OdooIcons.MODEL)
                .withTypeText("Built-in Mixin")
                .withTailText(" ($description)", true)
                .withBoldness(true)
            
            result.addElement(lookupElement)
        }
    }
    
    private fun addOdooBaseClasses(result: CompletionResultSet) {
        val baseClasses = listOf(
            "models.Model" to "Persistent Model",
            "models.TransientModel" to "Transient Model",
            "models.AbstractModel" to "Abstract Model"
        )
        
        baseClasses.forEach { (className, description) ->
            val lookupElement = LookupElementBuilder.create(className)
                .withIcon(OdooIcons.MODEL)
                .withTypeText("Base Class")
                .withTailText(" ($description)", true)
                .withBoldness(true)
            
            result.addElement(lookupElement)
        }
    }
}