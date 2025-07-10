package com.tmc.odoo.pycharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.psi.*
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
        val superClassExpressions = pyClass.superClassExpressions
        return superClassExpressions.any { expr ->
            when (expr) {
                is PyReferenceExpression -> {
                    val name = expr.name
                    name in listOf("Model", "TransientModel", "AbstractModel")
                }
                is PyCallExpression -> {
                    val callee = expr.callee
                    if (callee is PyQualifiedExpression) {
                        val qualifier = callee.qualifier?.name
                        val name = callee.name
                        qualifier == "models" && name in listOf("Model", "TransientModel", "AbstractModel")
                    } else false
                }
                else -> false
            }
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
        
        // Check for _description attribute (warning level)
        val descriptionAttribute = pyClass.classAttributes.find { it.name == "_description" }
        if (descriptionAttribute == null) {
            holder.registerProblem(
                pyClass.nameIdentifier ?: pyClass,
                "Odoo model should have a _description attribute",
                ProblemHighlightType.WEAK_WARNING
            )
        }
        
        // Check for proper inheritance
        val inheritAttribute = pyClass.classAttributes.find { it.name == "_inherit" }
        if (inheritAttribute != null) {
            val inheritValue = inheritAttribute.findAssignedValue()
            if (inheritValue !is PyStringLiteralExpression && 
                inheritValue !is PyListLiteralExpression) {
                holder.registerProblem(
                    inheritAttribute,
                    "_inherit should be a string or list of strings"
                )
            }
        }
    }
}