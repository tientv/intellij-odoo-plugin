package com.tmc.odoo.pycharm.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object OdooIcons {
    // Main icons
    @JvmField
    val ODOO = IconLoader.getIcon("/icons/odoo.svg", OdooIcons::class.java)
    
    @JvmField
    val MODEL = IconLoader.getIcon("/icons/model.svg", OdooIcons::class.java)
    
    @JvmField
    val FIELD = IconLoader.getIcon("/icons/field.svg", OdooIcons::class.java)
    
    @JvmField
    val METHOD = IconLoader.getIcon("/icons/method.svg", OdooIcons::class.java)
    
    @JvmField
    val MODULE = IconLoader.getIcon("/icons/module.svg", OdooIcons::class.java)
    
    // Field type icons
    @JvmField
    val RELATION_FIELD = IconLoader.getIcon("/icons/relation_field.svg", OdooIcons::class.java)
    
    @JvmField
    val SELECTION_FIELD = IconLoader.getIcon("/icons/selection_field.svg", OdooIcons::class.java)
    
    @JvmField
    val BOOLEAN_FIELD = IconLoader.getIcon("/icons/boolean_field.svg", OdooIcons::class.java)
    
    @JvmField
    val DATE_FIELD = IconLoader.getIcon("/icons/date_field.svg", OdooIcons::class.java)
    
    @JvmField
    val NUMBER_FIELD = IconLoader.getIcon("/icons/number_field.svg", OdooIcons::class.java)
    
    @JvmField
    val BINARY_FIELD = IconLoader.getIcon("/icons/binary_field.svg", OdooIcons::class.java)
    
    // Method type icons
    @JvmField
    val CRUD_METHOD = IconLoader.getIcon("/icons/crud_method.svg", OdooIcons::class.java)
    
    @JvmField
    val SEARCH_METHOD = IconLoader.getIcon("/icons/search_method.svg", OdooIcons::class.java)
    
    @JvmField
    val API_METHOD = IconLoader.getIcon("/icons/api_method.svg", OdooIcons::class.java)
    
    @JvmField
    val COMPUTE_METHOD = IconLoader.getIcon("/icons/compute_method.svg", OdooIcons::class.java)
    
    @JvmField
    val ONCHANGE_METHOD = IconLoader.getIcon("/icons/onchange_method.svg", OdooIcons::class.java)
    
    @JvmField
    val ORM_METHOD = IconLoader.getIcon("/icons/orm_method.svg", OdooIcons::class.java)
    
    // Fallback to default IntelliJ icons if custom icons are not available
    init {
        // This will use IntelliJ's default icons as fallback
        // In a real implementation, you would provide actual SVG icons
    }
}