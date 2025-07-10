# Changelog

All notable changes to the "Odoo Development Support" plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.2] - 2025-07-10

### Fixed
- **Runtime Error**: Fixed ClassNotFoundException by removing unimplemented extension declarations from plugin.xml
- **Plugin Loading**: Resolved "Cannot create extension" errors that prevented plugin from loading properly
- **Stability**: Plugin now loads without runtime errors in PyCharm

### Removed
- Temporarily disabled unimplemented extensions: framework detector, line markers, settings, facets, and actions
- These features will be restored in future versions once properly implemented

### Technical
- Commented out problematic extension declarations until implementation is complete
- Core functionality (completion contributors, reference resolution, inspections) remains active

## [1.0.1] - 2025-07-10

### Fixed
- **Compatibility**: Updated to support PyCharm builds up to 243.* (fixes compatibility with PyCharm 2024.2 and later)
- **Build Range**: Extended untilBuild from 241.* to 243.* to support latest PyCharm versions

### Technical
- Build compatibility: 233 (PyCharm 2023.3) to 243.* (PyCharm 2024.3)
- No functional changes - purely compatibility update

## [1.0.0] - 2025-07-10

### Added
- 🎯 **Smart Code Completion**
  - Model name autocompletion in string literals (e.g., `'res.partner'`, `'sale.order'`)
  - Field name completion with context awareness (e.g., `self.name`, `record.email`)
  - Method completion for Odoo framework methods (`create()`, `search()`, `write()`)
  - Field type completion with parameter hints (`Char()`, `Many2one()`, `Selection()`)

- 🔍 **Code Navigation**
  - Go-to-definition for Odoo model references (Ctrl+Click)
  - Find all references to models and fields across modules
  - Smart navigation between related models and inherited classes

- 📊 **Code Analysis & Inspections**
  - Model validation: detect missing `_name` and `_description` attributes
  - Field validation: validate field types and syntax
  - Inheritance checking: verify model inheritance patterns
  - Code quality inspections specific to Odoo development

- 🏗️ **Project Structure Support**
  - Automatic Odoo project detection via `__manifest__.py` files
  - Multi-module project support for complex Odoo installations
  - Manifest file parsing for dependencies and metadata
  - Project-wide model and field indexing

- 🎨 **User Interface**
  - Custom icons for different Odoo elements (models, fields, methods)
  - Context-aware completion with type information
  - Integration with PyCharm's existing Python support

### Supported Odoo Versions
- ✅ **Odoo 18.0** (Primary support with full feature set)
- ✅ **Odoo 17.0** (Partial support for core features)
- ✅ **Odoo 16.0** (Basic model and field completion)

### Supported IDEs
- **PyCharm Community Edition** 2023.3+
- **PyCharm Professional Edition** 2023.3+

### Technical Details
- Built with IntelliJ Platform SDK 2023.3.2
- Kotlin-based implementation for performance and maintainability
- Java 17+ compatibility
- Comprehensive PSI (Program Structure Interface) integration

### Installation
- Available through JetBrains Marketplace
- Manual installation from GitHub releases
- Development build support for contributors

[Unreleased]: https://github.com/tientv/intellij-odoo-plugin/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/tientv/intellij-odoo-plugin/releases/tag/v1.0.0