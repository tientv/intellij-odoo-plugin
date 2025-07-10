package com.tmc.odoo.pycharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.tmc.odoo.pycharm.services.OdooProjectService

class OdooModelInspection : LocalInspectionTool() {
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val project = holder.project
        val odooService = OdooProjectService.getInstance(project)
        
        if (!odooService.isOdooProject()) {
            return PsiElementVisitor.EMPTY_VISITOR
        }
        
        return object : PyElementVisitor() {
            override fun visitPyClass(node: PyClass) {
                super.visitPyClass(node)
                
                // Check if this is an Odoo model class
                if (isOdooModelClass(node)) {
                    checkOdooModelStructure(node, holder)
                }
            }
        }
    }
    
    private fun isOdooModelClass(pyClass: PyClass): Boolean {
        val superClasses = pyClass.superClassExpressions
        return superClasses.any { expr ->
            expr.text.contains("Model") || expr.text.contains("models.")
        }
    }
    
    private fun checkOdooModelStructure(pyClass: PyClass, holder: ProblemsHolder) {
        // Check for _name attribute
        val nameAttribute = pyClass.classAttributes.find { it.name == "_name" }
        if (nameAttribute == null) {
            holder.registerProblem(
                pyClass.nameIdentifier ?: pyClass,
                "Odoo model should have a _name attribute"
            )
        } else {
            // Check if _name is a string literal
            val nameValue = nameAttribute.findAssignedValue()
            if (nameValue !is PyStringLiteralExpression) {
                holder.registerProblem(
                    nameAttribute,
                    "_name should be a string literal"
                )
            }
        }
        
        // Check for _description attribute
        val descriptionAttribute = pyClass.classAttributes.find { it.name == "_description" }
        if (descriptionAttribute == null) {
            holder.registerProblem(
                pyClass.nameIdentifier ?: pyClass,
                "Odoo model should have a _description attribute",
                com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
            )
        }
        
        // Check for proper inheritance
        val inheritAttribute = pyClass.classAttributes.find { it.name == "_inherit" }
        if (inheritAttribute != null) {
            val inheritValue = inheritAttribute.findAssignedValue()
            if (inheritValue !is PyStringLiteralExpression && 
                inheritValue !is com.jetbrains.python.psi.PyListLiteralExpression) {
                holder.registerProblem(
                    inheritAttribute,
                    "_inherit should be a string or list of strings"
                )
            }
        }
    }
}