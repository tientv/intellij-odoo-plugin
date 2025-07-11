package com.tmc.odoo.pycharm.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.jetbrains.python.psi.*
import com.tmc.odoo.pycharm.services.OdooProjectService
import com.tmc.odoo.pycharm.models.OdooModel

/**
 * Provides hover documentation for Odoo model references, especially in _inherit contexts
 */
class OdooModelDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null || originalElement == null) return null
        
        // Check if this is a string literal that could be a model reference
        val stringLiteral = when {
            element is PyStringLiteralExpression -> element
            originalElement is PyStringLiteralExpression -> originalElement
            originalElement.parent is PyStringLiteralExpression -> originalElement.parent as PyStringLiteralExpression
            else -> null
        } ?: return null
        
        val modelName = stringLiteral.stringValue
        if (modelName.isBlank()) return null
        
        // Check if this string is in an _inherit context
        if (!isModelReference(stringLiteral)) return null
        
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        
        // Fast lookup from cache
        val model = odooService.findModel(modelName) ?: return null
        
        return buildModelDocumentation(model, odooService)
    }
    
    /**
     * Check if the string literal is a model reference (in _inherit, _name, or env[] context)
     */
    private fun isModelReference(stringLiteral: PyStringLiteralExpression): Boolean {
        val parent = stringLiteral.parent
        
        // Check for _inherit = "model.name" or _name = "model.name"
        if (parent is PyAssignmentStatement) {
            val targets = parent.targets
            for (target in targets) {
                if (target is PyTargetExpression && target.name in listOf("_inherit", "_name")) {
                    return true
                }
            }
        }
        
        // Check for _inherit = ["model1", "model2"]
        if (parent is PyListLiteralExpression) {
            val listParent = parent.parent
            if (listParent is PyAssignmentStatement) {
                val targets = listParent.targets
                for (target in targets) {
                    if (target is PyTargetExpression && target.name == "_inherit") {
                        return true
                    }
                }
            }
        }
        
        // Check for self.env['model.name'] pattern
        val callExpr = findParentOfType<PySubscriptionExpression>(stringLiteral)
        if (callExpr != null) {
            val operand = callExpr.operand
            if (operand is PyQualifiedExpression && operand.asQualifiedName()?.toString()?.endsWith(".env") == true) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Find parent element of specific type
     */
    private inline fun <reified T : PsiElement> findParentOfType(element: PsiElement): T? {
        var current: PsiElement? = element.parent
        while (current != null) {
            if (current is T) return current
            current = current.parent
        }
        return null
    }
    
    /**
     * Build rich documentation for an Odoo model
     */
    private fun buildModelDocumentation(model: OdooModel, odooService: OdooProjectService): String {
        val sections = mutableListOf<String>()
        
        // Header with model name
        sections.add(DocumentationMarkup.DEFINITION_START)
        sections.add("Odoo Model: <b>${model.name}</b>")
        sections.add(DocumentationMarkup.DEFINITION_END)
        
        // Description
        if (model.description.isNotEmpty()) {
            sections.add(DocumentationMarkup.CONTENT_START)
            sections.add("<p>${model.description}</p>")
            sections.add(DocumentationMarkup.CONTENT_END)
        }
        
        // Inheritance info
        if (model.inherits.isNotEmpty()) {
            sections.add(DocumentationMarkup.SECTIONS_START)
            sections.add("Inherits from:")
            sections.add("<ul>")
            model.inherits.forEach { inheritedModel ->
                sections.add("<li><code>$inheritedModel</code></li>")
            }
            sections.add("</ul>")
            sections.add(DocumentationMarkup.SECTIONS_END)
        }
        
        // Show fields (limited to avoid performance issues)
        val allFields = odooService.getModelFields(model.name)
        if (allFields.isNotEmpty()) {
            sections.add(DocumentationMarkup.SECTIONS_START)
            sections.add("Fields (${allFields.size}):")
            sections.add("<ul>")
            // Show only first 10 fields to avoid performance issues
            allFields.take(10).forEach { field ->
                sections.add("<li><code>${field.name}</code> (${field.type})</li>")
            }
            if (allFields.size > 10) {
                sections.add("<li><i>... and ${allFields.size - 10} more</i></li>")
            }
            sections.add("</ul>")
            sections.add(DocumentationMarkup.SECTIONS_END)
        }
        
        // Show child models (models that inherit from this one)
        val childModels = odooService.findModelsInheriting(model.name)
        if (childModels.isNotEmpty()) {
            sections.add(DocumentationMarkup.SECTIONS_START)
            sections.add("Extended by:")
            sections.add("<ul>")
            childModels.take(5).forEach { childModel ->
                sections.add("<li><code>${childModel.name}</code></li>")
            }
            if (childModels.size > 5) {
                sections.add("<li><i>... and ${childModels.size - 5} more</i></li>")
            }
            sections.add("</ul>")
            sections.add(DocumentationMarkup.SECTIONS_END)
        }
        
        return sections.joinToString("")
    }
    
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null || originalElement == null) return null
        
        val stringLiteral = when {
            element is PyStringLiteralExpression -> element
            originalElement is PyStringLiteralExpression -> originalElement
            originalElement.parent is PyStringLiteralExpression -> originalElement.parent as PyStringLiteralExpression
            else -> null
        } ?: return null
        
        val modelName = stringLiteral.stringValue
        if (modelName.isBlank() || !isModelReference(stringLiteral)) return null
        
        val project = element.project
        val odooService = OdooProjectService.getInstance(project)
        val model = odooService.findModel(modelName) ?: return null
        
        return "Odoo Model: ${model.name}${if (model.description.isNotEmpty()) " - ${model.description}" else ""}"
    }
}