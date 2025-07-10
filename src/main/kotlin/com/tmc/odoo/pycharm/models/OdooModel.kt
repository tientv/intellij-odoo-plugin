package com.tmc.odoo.pycharm.models

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile

/**
 * Represents an Odoo model
 */
data class OdooModel(
    val name: String,
    val className: String,
    val description: String,
    val inherits: List<String>,
    val fields: List<OdooField>,
    val psiClass: PyClass
) {
    val module: String get() = psiClass.containingFile?.name?.substringBefore(".py") ?: ""
    val modulePath: String get() = psiClass.containingFile?.virtualFile?.parent?.name ?: ""
}

/**
 * Represents an Odoo field
 */
data class OdooField(
    val name: String,
    val type: String,
    val psiElement: PsiElement,
    val isRequired: Boolean = false,
    val isReadonly: Boolean = false,
    val help: String? = null,
    val defaultValue: String? = null
)

/**
 * Represents an Odoo module
 */
data class OdooModule(
    val name: String,
    val version: String,
    val depends: List<String>,
    val manifestFile: PyFile
) {
    val path: String get() = manifestFile.virtualFile?.parent?.path ?: ""
}

/**
 * Represents an Odoo method
 */
data class OdooMethod(
    val name: String,
    val type: OdooMethodType,
    val parameters: List<String>,
    val description: String? = null
)

/**
 * Types of Odoo methods
 */
enum class OdooMethodType {
    API_MODEL,
    API_DEPENDS,
    API_CONSTRAINS,
    API_ONCHANGE,
    API_RETURNS,
    CRUD,
    BUSINESS_LOGIC,
    COMPUTE,
    INVERSE,
    SEARCH
}

/**
 * Odoo framework constants
 */
object OdooConstants {
    val FIELD_TYPES = setOf(
        "Char", "Text", "Html", "Boolean", "Integer", "Float", "Monetary",
        "Date", "Datetime", "Selection", "Many2one", "One2many", "Many2many",
        "Binary", "Image", "Json", "Properties"
    )
    
    val MODEL_TYPES = setOf(
        "Model", "TransientModel", "AbstractModel"
    )
    
    val API_DECORATORS = setOf(
        "api.model", "api.depends", "api.constrains", "api.onchange",
        "api.returns", "api.model_create_multi"
    )
    
    val COMMON_METHODS = mapOf(
        "create" to OdooMethod("create", OdooMethodType.CRUD, listOf("vals")),
        "write" to OdooMethod("write", OdooMethodType.CRUD, listOf("vals")),
        "unlink" to OdooMethod("unlink", OdooMethodType.CRUD, emptyList()),
        "read" to OdooMethod("read", OdooMethodType.CRUD, listOf("fields", "load")),
        "search" to OdooMethod("search", OdooMethodType.SEARCH, listOf("domain", "offset", "limit", "order")),
        "search_read" to OdooMethod("search_read", OdooMethodType.SEARCH, listOf("domain", "fields", "offset", "limit", "order")),
        "browse" to OdooMethod("browse", OdooMethodType.CRUD, listOf("ids")),
        "exists" to OdooMethod("exists", OdooMethodType.CRUD, emptyList()),
        "ensure_one" to OdooMethod("ensure_one", OdooMethodType.CRUD, emptyList()),
        "copy" to OdooMethod("copy", OdooMethodType.CRUD, listOf("default")),
        "name_get" to OdooMethod("name_get", OdooMethodType.BUSINESS_LOGIC, emptyList()),
        "name_search" to OdooMethod("name_search", OdooMethodType.SEARCH, listOf("name", "args", "operator", "limit")),
        "default_get" to OdooMethod("default_get", OdooMethodType.BUSINESS_LOGIC, listOf("fields")),
        "fields_get" to OdooMethod("fields_get", OdooMethodType.BUSINESS_LOGIC, listOf("allfields", "attributes")),
        "filtered" to OdooMethod("filtered", OdooMethodType.BUSINESS_LOGIC, listOf("func")),
        "mapped" to OdooMethod("mapped", OdooMethodType.BUSINESS_LOGIC, listOf("func")),
        "sorted" to OdooMethod("sorted", OdooMethodType.BUSINESS_LOGIC, listOf("key", "reverse")),
        "sudo" to OdooMethod("sudo", OdooMethodType.BUSINESS_LOGIC, listOf("user")),
        "with_context" to OdooMethod("with_context", OdooMethodType.BUSINESS_LOGIC, listOf("context")),
        "with_user" to OdooMethod("with_user", OdooMethodType.BUSINESS_LOGIC, listOf("user")),
        "with_company" to OdooMethod("with_company", OdooMethodType.BUSINESS_LOGIC, listOf("company")),
        "with_env" to OdooMethod("with_env", OdooMethodType.BUSINESS_LOGIC, listOf("env"))
    )
}