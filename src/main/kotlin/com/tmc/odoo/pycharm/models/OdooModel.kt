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

/**
 * Field attribute definitions with types and default values
 */
object OdooFieldAttributes {
    
    // Common attributes for all field types
    val COMMON_ATTRIBUTES = mapOf(
        "string" to FieldAttribute("string", "str", "Field label", "'Name'"),
        "help" to FieldAttribute("help", "str", "Help tooltip", "'Help text'"),
        "readonly" to FieldAttribute("readonly", "bool", "Read-only field", "True"),
        "required" to FieldAttribute("required", "bool", "Required field", "True"),
        "index" to FieldAttribute("index", "bool", "Database index", "True"),
        "copy" to FieldAttribute("copy", "bool", "Copy on duplicate", "False"),
        "store" to FieldAttribute("store", "bool", "Store in database", "True"),
        "compute" to FieldAttribute("compute", "str", "Compute method", "'_compute_field'"),
        "inverse" to FieldAttribute("inverse", "str", "Inverse method", "'_inverse_field'"),
        "search" to FieldAttribute("search", "str", "Search method", "'_search_field'"),
        "related" to FieldAttribute("related", "str", "Related field path", "'partner_id.name'"),
        "default" to FieldAttribute("default", "Any", "Default value", "lambda self: self._default_value()"),
        "groups" to FieldAttribute("groups", "str", "Security groups", "'base.group_user'"),
        "states" to FieldAttribute("states", "dict", "Field states", "{'draft': [('readonly', False)]}"),
        "depends" to FieldAttribute("depends", "list", "Computed field dependencies", "['partner_id', 'name']")
    )
    
    // Char and Text specific attributes
    val CHAR_TEXT_ATTRIBUTES = mapOf(
        "size" to FieldAttribute("size", "int", "Field size limit", "64"),
        "translate" to FieldAttribute("translate", "bool", "Translatable field", "True"),
        "trim" to FieldAttribute("trim", "bool", "Trim whitespace", "True")
    )
    
    // Selection field attributes
    val SELECTION_ATTRIBUTES = mapOf(
        "selection" to FieldAttribute("selection", "list", "Selection options", "[('draft', 'Draft'), ('done', 'Done')]"),
        "selection_add" to FieldAttribute("selection_add", "list", "Add selection options", "[('new', 'New')]")
    )
    
    // Relational field attributes
    val RELATIONAL_ATTRIBUTES = mapOf(
        "comodel_name" to FieldAttribute("comodel_name", "str", "Related model", "'res.partner'"),
        "domain" to FieldAttribute("domain", "list", "Domain filter", "[('active', '=', True)]"),
        "context" to FieldAttribute("context", "dict", "Context values", "{'default_type': 'customer'}"),
        "ondelete" to FieldAttribute("ondelete", "str", "Delete action", "'cascade'"),
        "auto_join" to FieldAttribute("auto_join", "bool", "Auto join in search", "True"),
        "delegate" to FieldAttribute("delegate", "bool", "Delegation inheritance", "True")
    )
    
    // Many2one specific
    val MANY2ONE_ATTRIBUTES = RELATIONAL_ATTRIBUTES + mapOf(
        "check_company" to FieldAttribute("check_company", "bool", "Check company", "True")
    )
    
    // One2many specific  
    val ONE2MANY_ATTRIBUTES = RELATIONAL_ATTRIBUTES + mapOf(
        "inverse_name" to FieldAttribute("inverse_name", "str", "Inverse field name", "'parent_id'"),
        "limit" to FieldAttribute("limit", "int", "Record limit", "100")
    )
    
    // Many2many specific
    val MANY2MANY_ATTRIBUTES = RELATIONAL_ATTRIBUTES + mapOf(
        "relation" to FieldAttribute("relation", "str", "Junction table", "'model_partner_rel'"),
        "column1" to FieldAttribute("column1", "str", "First column name", "'model_id'"),
        "column2" to FieldAttribute("column2", "str", "Second column name", "'partner_id'")
    )
    
    // Numeric field attributes
    val NUMERIC_ATTRIBUTES = mapOf(
        "digits" to FieldAttribute("digits", "tuple", "Decimal precision", "(16, 2)"),
        "group_operator" to FieldAttribute("group_operator", "str", "Group operation", "'sum'")
    )
    
    // Date/Datetime attributes
    val DATE_ATTRIBUTES = mapOf(
        "timezone" to FieldAttribute("timezone", "str", "Timezone", "'UTC'")
    )
    
    // Binary field attributes
    val BINARY_ATTRIBUTES = mapOf(
        "attachment" to FieldAttribute("attachment", "bool", "Store as attachment", "True"),
        "max_width" to FieldAttribute("max_width", "int", "Max image width", "1920"),
        "max_height" to FieldAttribute("max_height", "int", "Max image height", "1920")
    )
    
    /**
     * Get attributes for a specific field type
     */
    fun getAttributesForFieldType(fieldType: String): Map<String, FieldAttribute> {
        val attributes = mutableMapOf<String, FieldAttribute>()
        attributes.putAll(COMMON_ATTRIBUTES)
        
        when (fieldType) {
            "Char", "Text", "Html" -> attributes.putAll(CHAR_TEXT_ATTRIBUTES)
            "Selection" -> attributes.putAll(SELECTION_ATTRIBUTES)
            "Many2one" -> attributes.putAll(MANY2ONE_ATTRIBUTES)
            "One2many" -> attributes.putAll(ONE2MANY_ATTRIBUTES)
            "Many2many" -> attributes.putAll(MANY2MANY_ATTRIBUTES)
            "Float", "Integer", "Monetary" -> attributes.putAll(NUMERIC_ATTRIBUTES)
            "Date", "Datetime" -> attributes.putAll(DATE_ATTRIBUTES)
            "Binary", "Image" -> attributes.putAll(BINARY_ATTRIBUTES)
        }
        
        return attributes
    }
}

/**
 * Represents a field attribute with metadata
 */
data class FieldAttribute(
    val name: String,
    val type: String,
    val description: String,
    val example: String
)

/**
 * Extended field information with enhanced metadata
 */
data class OdooFieldInfo(
    val name: String,
    val type: String,
    val isRequired: Boolean = false,
    val isReadonly: Boolean = false,
    val isComputed: Boolean = false,
    val relatedModel: String? = null,
    val domain: String? = null,
    val selection: List<Pair<String, String>>? = null,
    val help: String? = null,
    val defaultValue: String? = null,
    val psiElement: PsiElement? = null
)
