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

### 🔍 **Code Navigation**
- **Go to Definition**: Navigate directly to model definitions with Ctrl+Click
- **Find Usages**: Find all references to models and fields across your project
- **Cross-Module Navigation**: Jump between related models in different modules

### 📊 **Code Analysis**
- **Model Validation**: Detect missing `_name` and `_description` attributes
- **Field Validation**: Validate field definitions and types
- **Inheritance Checking**: Verify model inheritance patterns
- **Best Practices**: Odoo-specific code quality inspections

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

#### Model Completion
```python
# In any Python file, start typing model names in quotes
model_name = 'res.partner'  # ✨ Auto-completes with available models
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
| **18.0** | ✅ Full | All features, latest API support |
| **17.0** | ✅ Partial | Core completion and navigation |
| **16.0** | ⚠️ Basic | Model and field completion |

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
1. Increase PyCharm memory allocation in **Help > Change Memory Settings**
2. Exclude large directories from indexing in **Settings > Project Structure**

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