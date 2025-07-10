package com.tmc.odoo.pycharm.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.tmc.odoo.pycharm.services.OdooProjectService

class OdooProjectListener : ProjectManagerListener {
    
    override fun projectOpened(project: Project) {
        val odooService = OdooProjectService.getInstance(project)
        if (odooService.isOdooProject()) {
            // Initialize Odoo-specific features
            initializeOdooFeatures(project)
        }
    }
    
    private fun initializeOdooFeatures(project: Project) {
        // Perform initial project scan
        // This could include:
        // - Indexing models
        // - Setting up file watchers
        // - Configuring project structure
        
        // For now, just log that an Odoo project was detected
        // In a real implementation, you might want to show a notification
        println("Odoo project detected: ${project.name}")
    }
}