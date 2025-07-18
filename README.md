# Odoo Development Support for PyCharm

[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![GitHub Release](https://img.shields.io/github/release/tientv/intellij-odoo-plugin.svg)](https://github.com/tientv/intellij-odoo-plugin/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **Supercharge your Odoo development experience with intelligent code completion, navigation, and analysis in PyCharm.**

![Plugin Demo](https://raw.githubusercontent.com/tientv/intellij-odoo-plugin/main/docs/images/demo.gif)

## 🌟 Features

### 🎯 **Smart Code Completion**
- **Model Names**: Auto-complete Odoo model names in string literals
  ```python
  # Type quotes and get intelligent suggestions
  model = self.env['res.partner']  # ✨ Auto-completes with all available models
  model = self.env['sale.order']   # 🎯 Context-aware suggestions
  ```

- **Field Access**: Context-aware field completion
  ```python
  class MyModel(models.Model):
      _name = 'my.model'
      
      def my_method(self):
          self.name          # ✨ Shows available fields
          self.create_date   # 🎯 Inherited fields included
          partner.email      # 🔍 Related model fields
  ```

- **Method Completion**: Complete Odoo framework methods with parameter hints
  ```python
  records = self.search([])                    # ✨ Parameter hints
  new_record = self.create({'name': 'Test'})   # 🎯 Smart completion
  records.write({'state': 'done'})             # 🔍 Method suggestions
  ```

- **Field Types**: Auto-complete field types with constructor parameters
  ```python
  name = fields.Char()        # ✨ Field type completion
  partner_id = fields.Many2one()  # 🎯 With parameter hints
  ```

- **Field Attributes**: Context-aware field attribute completion ⭐ NEW in v1.4.0
  ```python
  name = fields.Char(
      string="Name",           # ✨ Auto-completes field attributes
      required=True,           # 🎯 Type-specific suggestions
      help="Enter the name"    # 🔍 Context-aware parameters
  )
  partner_id = fields.Many2one(
      comodel_name="res.partner",  # ✨ Model name completion
      string="Partner"             # 🎯 Relational field attributes
  )
  ```

- **Related Field Paths**: Multi-level field completion ⭐ NEW in v1.4.0
  ```python
  country_name = fields.Char(
      related="partner_id.country_id.name",  # ✨ Chained field completion
      readonly=True                          # 🎯 Smart path navigation
  )
  ```

### 🔍 **Code Navigation**
- **Go to Definition**: Navigate directly to model definitions with Ctrl+Click
- **_inherit Navigation**: Click on `_inherit` strings to jump to parent model definitions
- **Find Usages**: Find all references to models and fields across your project
- **Cross-Module Navigation**: Jump between related models in different modules
- **Comodel Navigation**: Ctrl+click on `comodel_name` to jump to related models ⭐ NEW in v1.4.0
- **Related Field Navigation**: Navigate through complex field relationships with ease ⭐ NEW in v1.4.0

### 🔧 **Advanced Navigation & References** ⭐ NEW in v1.1.0
- **Compute Function Navigation**: Ctrl+click on `compute="method_name"` to navigate directly to compute methods
  ```python
  name = fields.Char(compute='_compute_display_name')  # ✨ Click to jump to method
  
  @api.depends('first_name', 'last_name')
  def _compute_display_name(self):  # 🎯 Navigate here instantly
      for record in self:
          record.name = f"{record.first_name} {record.last_name}"
  ```

- **Smart Import Completion**: Intelligent auto-completion for Odoo imports
  ```python
  from odoo import models, fields, api  # ✨ Auto-completes common imports
  from odoo.exceptions import UserError  # 🎯 Context-aware suggestions
  from odoo.tools import safe_eval       # 🔍 Tool imports
  ```

- **_name Navigation**: Navigate from model name definitions
  ```python
  class MyModel(models.Model):
      _name = 'my.custom.model'  # ✨ Ctrl+click to find usages
      _inherit = 'res.partner'   # 🎯 Navigate to parent model
  ```

- **Mixin Intelligence**: Smart completion for common Odoo mixins
  ```python
  class MyModel(models.Model, MailThread):  # ✨ Suggests MailThread, UtmMixin, etc.
      _name = 'my.model'
      _inherit = ['mail.thread', 'portal.mixin']  # 🎯 Auto-complete mixins
  ```

### 🧬 **Inheritance Support** ⭐ NEW in v1.0.5
- **Inherited Field Completion**: Automatically shows fields from parent models
  ```python
  class SaleOrder(models.Model):
      _inherit = 'sale.order'
      
      def my_method(self):
          self.partner_id.email    # ✨ Shows res.partner fields
          self.state              # 🎯 Shows inherited sale.order fields
  ```

- **_inherit String Completion**: Auto-complete model names in inheritance
  ```python
  class MyModel(models.Model):
      _inherit = 'res.p'  # ✨ Auto-completes to 'res.partner'
  ```

- **Inherited Method Completion**: Access methods from parent models
  ```python
  class ProductTemplate(models.Model):
      _inherit = 'product.template'
      
      def my_method(self):
          self.write()   # ✨ Shows ORM methods from parent
          self._compute_display_name()  # 🎯 Parent model methods
  ```

- **Chained Field Navigation**: Navigate through relational fields
  ```python
  # Complete through Many2one relationships
  self.sale_order_id.partner_id.country_id.name  # ✨ Multi-level completion
  ```

### 📊 **Code Analysis**
- **Model Validation**: Detect missing `_name` and `_description` attributes
- **Field Validation**: Validate field definitions and types
- **Inheritance Checking**: Verify model inheritance patterns
- **Best Practices**: Odoo-specific code quality inspections

### ⚡ **High-Performance Hover & Indexing** ⭐ NEW in v1.3.0
- **Rich Hover Documentation**: Instant tooltips for `_inherit` model references with complete model information
- **Lightning-Fast Performance**: O(1) model lookups with async background indexing (no more UI lag!)
- **Smart Caching**: Intelligent field caching with inheritance-aware resolution
- **Real-time Updates**: Incremental index updates only for modified files

### 🎯 **Complete Field System** ⭐ NEW in v1.4.0
- **Comprehensive Field Attributes**: Auto-complete all field attributes with type-specific validation
- **Smart Constructor Generation**: Intelligent field constructor creation with proper parameter suggestions
- **Relational Field Intelligence**: Advanced completion for `Many2one`, `One2many`, `Many2many` relationships
- **Field Path Resolution**: Multi-level field path completion for complex related fields
- **Context-Aware Suggestions**: Different completions based on field type and usage context

### 🎨 **Professional Visual Experience** ⭐ NEW in v1.2.0
- **Custom Icon System**: 21 professionally designed SVG icons for enhanced visual distinction
- **Semantic Color Coding**: Color-coded icons for different element types (models, fields, methods)
- **Dark Theme Support**: Optimized icons for both light and dark IDE themes
- **Brand Consistency**: Odoo-branded visual identity with purple accent colors

### 🏗️ **Project Structure**
- **Auto-Detection**: Automatically recognizes Odoo projects
- **Multi-Module Support**: Works with complex Odoo project structures
- **Manifest Analysis**: Parses `__manifest__.py` files for dependencies

## 🚀 Installation

### From JetBrains Marketplace
1. Open PyCharm
2. Go to **File > Settings > Plugins** (or **PyCharm > Preferences > Plugins** on macOS)
3. Search for "Odoo Development Support"
4. Click **Install** and restart PyCharm

### Manual Installation
1. Download the latest release from [GitHub Releases](https://github.com/tientv/intellij-odoo-plugin/releases)
2. In PyCharm, go to **File > Settings > Plugins**
3. Click the gear icon and select **Install Plugin from Disk**
4. Select the downloaded `.zip` file

## 📋 Usage

### Getting Started
1. **Open your Odoo project** in PyCharm
2. **Plugin auto-detection**: The plugin automatically detects Odoo projects by scanning for `__manifest__.py` files
3. **Start coding**: Enjoy enhanced autocompletion and navigation!

### Key Features in Action

#### Rich Hover Documentation (NEW! v1.3.0)
```python
class MyModel(models.Model):
    _name = 'my.model'
    _inherit = 'res.partner'  # ✨ Hover to see rich model information!
    
    # Hover shows:
    # • Model description and technical details
    # • Complete inheritance hierarchy
    # • Available fields with types
    # • Models that extend this one
```

#### Model Completion
```python
# In any Python file, start typing model names in quotes
model_name = 'res.partner'  # ✨ Auto-completes with available models
```

#### Smart Import Assistance
```python
from odoo.  # ✨ Auto-completes: models, fields, api, exceptions, tools
from odoo.models import  # 🎯 Suggests: Model, TransientModel, AbstractModel
from odoo.exceptions import  # 🔍 Suggests: UserError, ValidationError, etc.
```

#### Compute Function Navigation
```python
class MyModel(models.Model):
    name = fields.Char(compute='_compute_name')  # ✨ Ctrl+click to navigate
    
    def _compute_name(self):  # 🎯 Jump here instantly
        pass
```

#### Field Access
```python
# In Odoo model methods
def my_method(self):
    self.  # ✨ Press Ctrl+Space to see all available fields
```

#### Method Calls
```python
# Odoo framework methods with parameter hints
records = self.search([('name', 'like', 'test')])  # ✨ Smart parameter completion
```

## 🎯 Supported Odoo Versions

| Odoo Version | Support Level | Features |
|--------------|---------------|----------|
| **18.0** | ✅ Full | All features, complete field system, latest API support |
| **17.0** | ✅ Enhanced | Core completion, navigation, and enhanced field features |
| **16.0** | ✅ Good | Model, field completion, and basic field attributes |

## 🔧 Supported IDEs

- **PyCharm Community Edition** 2023.3 or later
- **PyCharm Professional Edition** 2023.3 or later

## 📸 Screenshots

### Smart Model Completion
![Model Completion](https://raw.githubusercontent.com/tientv/intellij-odoo-plugin/main/docs/images/model-completion.png)

### Field Access with Context
![Field Completion](https://raw.githubusercontent.com/tientv/intellij-odoo-plugin/main/docs/images/field-completion.png)

### Method Completion with Hints
![Method Completion](https://raw.githubusercontent.com/tientv/intellij-odoo-plugin/main/docs/images/method-completion.png)

## 🔨 Development

### Building from Source
```bash
git clone https://github.com/tientv/intellij-odoo-plugin.git
cd intellij-odoo-plugin
./gradlew buildPlugin
```

### Running in Development Mode
```bash
./gradlew runIde
```

### Contributing
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## 📚 Documentation

- **[Development Guide](DEVELOPMENT.md)**: Detailed guide for plugin development
- **[Changelog](CHANGELOG.md)**: Version history and changes
- **[JetBrains Plugin Page](https://plugins.jetbrains.com/plugin/PLUGIN_ID)**: Official plugin listing

## 🐛 Troubleshooting

### Plugin Not Working?
1. Ensure your project contains `__manifest__.py` files
2. Check that the Python plugin is enabled in PyCharm
3. Verify PyCharm version compatibility (2023.3+)

### Completion Not Appearing?
1. Check if the plugin detected your project as an Odoo project
2. Try restarting PyCharm after installation
3. Clear PyCharm caches: **File > Invalidate Caches and Restart**

### Performance Issues?
**v1.3.0+ users:** Performance issues have been resolved with the new high-performance indexing system!
1. ~~Increase PyCharm memory allocation~~ → No longer needed with optimized indexing
2. ~~Exclude large directories~~ → Incremental updates handle large projects efficiently
3. **NEW**: Enjoy instant hover tooltips and O(1) model lookups!

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/tientv/intellij-odoo-plugin/issues)
- **Discussions**: [GitHub Discussions](https://github.com/tientv/intellij-odoo-plugin/discussions)
- **Email**: tientran201yd@gmail.com

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **JetBrains** for the excellent IntelliJ Platform
- **Odoo SA** for the amazing Odoo framework
- **PyCharm Community** for continuous feedback and support

---

**Made with ❤️ for the Odoo development community**

*Transform your Odoo development workflow with intelligent IDE support!*