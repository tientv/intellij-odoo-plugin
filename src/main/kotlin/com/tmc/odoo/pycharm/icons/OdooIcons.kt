package com.tmc.odoo.pycharm.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object OdooIcons {
    // Main icons - using IntelliJ default for main icon since it's handled by META-INF/pluginIcon.svg
    
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
    
    @JvmField
    val API = IconLoader.getIcon("/icons/api.svg", OdooIcons::class.java)
    
    @JvmField
    val EXCEPTION = IconLoader.getIcon("/icons/exception.svg", OdooIcons::class.java)
    
    @JvmField
    val TOOL = IconLoader.getIcon("/icons/tool.svg", OdooIcons::class.java)
}