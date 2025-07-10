package com.tmc.odoo.pycharm.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.PythonLanguage
import com.tmc.odoo.pycharm.services.OdooProjectService
import com.tmc.odoo.pycharm.icons.OdooIcons

class OdooModelCompletionContributor : CompletionContributor() {
    
    init {
        // Complete model names in string literals
        extend(
            CompletionType.BASIC,
            stringLiteralPattern(),
            OdooModelCompletionProvider()
        )
    }
    
    private fun stringLiteralPattern(): PsiElementPattern<PsiElement, *> {
        return PlatformPatterns.psiElement()
            .withLanguage(PythonLanguage.getInstance())
            .inside(PlatformPatterns.psiElement(PyStringLiteralExpression::class.java))
    }
}

class OdooModelCompletionProvider : CompletionProvider<CompletionParameters>() {
    
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val odooService = OdooProjectService.getInstance(project)
        
        // Check if this is an Odoo project
        if (!odooService.isOdooProject()) {
            return
        }
        
        // Get all Odoo models
        val models = odooService.getAllModels()
        
        models.forEach { model ->
            val lookupElement = LookupElementBuilder.create(model.name)
                .withIcon(OdooIcons.MODEL)
                .withTypeText(model.modulePath)
                .withTailText(" (${model.description})", true)
                .withInsertHandler { context, item ->
                    // Custom insert handler if needed
                }
            
            result.addElement(lookupElement)
        }
        
        // Add common Odoo model patterns
        addCommonModelPatterns(result)
    }
    
    private fun addCommonModelPatterns(result: CompletionResultSet) {
        val commonModels = listOf(
            "res.partner" to "Contact",
            "res.users" to "User",
            "res.company" to "Company", 
            "res.country" to "Country",
            "res.currency" to "Currency",
            "product.product" to "Product",
            "product.template" to "Product Template",
            "account.move" to "Journal Entry",
            "account.move.line" to "Journal Item",
            "sale.order" to "Sales Order",
            "sale.order.line" to "Sales Order Line",
            "purchase.order" to "Purchase Order",
            "purchase.order.line" to "Purchase Order Line",
            "stock.picking" to "Transfer",
            "stock.move" to "Stock Move",
            "hr.employee" to "Employee",
            "project.project" to "Project",
            "project.task" to "Task"
        )
        
        commonModels.forEach { (modelName, description) ->
            val lookupElement = LookupElementBuilder.create(modelName)
                .withIcon(OdooIcons.MODEL)
                .withTypeText("Built-in")
                .withTailText(" ($description)", true)
                .withBoldness(true)
            
            result.addElement(lookupElement)
        }
    }
}