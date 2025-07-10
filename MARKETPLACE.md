# JetBrains Marketplace Submission Guide

This document provides step-by-step instructions for publishing the Odoo Development Support plugin to the JetBrains Marketplace.

## 📋 Pre-Submission Checklist

### ✅ **Plugin Metadata**
- [x] **Plugin ID**: `com.github.tientv.intellij-odoo-plugin`
- [x] **Plugin Name**: "Odoo Development Support"
- [x] **Version**: 1.0.0
- [x] **Vendor**: Tien Tran (`tientran201yd@gmail.com`)
- [x] **Description**: Comprehensive HTML description with features
- [x] **License**: MIT License
- [x] **Compatibility**: PyCharm 2023.3+ (build 233-241.*)

### ✅ **Required Files**
- [x] `plugin.xml` - Plugin configuration
- [x] `README.md` - Detailed documentation
- [x] `CHANGELOG.md` - Version history
- [x] `LICENSE` - MIT license file
- [x] `build.gradle.kts` - Build configuration
- [x] Source code - Complete Kotlin implementation

### ✅ **Plugin Features**
- [x] Smart code completion for Odoo models, fields, and methods
- [x] Code navigation and reference resolution
- [x] Code inspections and validations
- [x] Project structure detection
- [x] Multi-module support

## 🚀 Submission Steps

### 1. **Build the Plugin**
```bash
cd /Users/tienable/Projects/TMC/plugins/intellij-odoo-plugin
./gradlew buildPlugin
```

The plugin ZIP will be generated at:
`build/distributions/intellij-odoo-plugin-1.0.0.zip`

### 2. **Create JetBrains Account**
1. Go to [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. Sign in with JetBrains account (or create one)
3. Navigate to **Developer Console**

### 3. **Submit New Plugin**
1. Click **"Upload plugin"**
2. Select the generated ZIP file
3. Fill in the required information:

#### **Basic Information**
- **Name**: Odoo Development Support
- **Categories**: 
  - Development Tools
  - Code Completion
  - IDE Integration
- **Tags**: odoo, python, pycharm, completion, navigation
- **Short Description**: 
  ```
  Intelligent code completion, navigation, and analysis for Odoo development in PyCharm
  ```

#### **Detailed Description**
Use the description from `plugin.xml` (already formatted with HTML):

```html
<h3>Odoo Development Support for PyCharm</h3>
<p>This plugin provides intelligent code completion, navigation, and analysis for Odoo development in PyCharm.</p>

<h4>Features:</h4>
<ul>
    <li><strong>Model Detection:</strong> Automatically detects Odoo models in your project</li>
    <li><strong>Field Completion:</strong> Smart autocompletion for model fields</li>
    <li><strong>Method Completion:</strong> Completion for Odoo framework methods</li>
    <li><strong>Reference Resolution:</strong> Navigate to model and field definitions</li>
    <li><strong>XML View Support:</strong> Completion for XML view elements</li>
    <li><strong>Manifest Support:</strong> Parse and validate __manifest__.py files</li>
</ul>

<h4>Supported Odoo Versions:</h4>
<ul>
    <li>Odoo 18.0</li>
    <li>Odoo 17.0 (partial support)</li>
</ul>

<p>This plugin enhances your Odoo development experience by providing PyCharm with deep understanding of the Odoo framework structure.</p>
```

#### **Vendor Information**
- **Vendor Name**: Tien Tran
- **Email**: tientran201yd@gmail.com
- **Website**: https://github.com/tientv
- **Support URL**: https://github.com/tientv/intellij-odoo-plugin/issues

### 4. **Upload Additional Assets**

#### **Plugin Icon** (Required)
- Size: 40x40 pixels for marketplace listing
- Size: 80x80 pixels for plugin manager
- Format: SVG or PNG
- Current icon: `src/main/resources/icons/odoo.svg`

#### **Screenshots** (Recommended)
Create screenshots showing:
1. **Model completion in action**
2. **Field access with context**
3. **Method completion with hints**
4. **Code navigation features**

#### **Plugin Logo** (Optional)
- Size: 200x200 pixels
- Use for prominent marketplace display

### 5. **Set Plugin Properties**

#### **Compatibility**
- **IDE**: PyCharm Community, PyCharm Professional
- **Version Range**: 2023.3 — 2024.1.*
- **Plugin Dependencies**: PythonCore

#### **Pricing**
- **Type**: Free
- **License**: MIT

#### **Support**
- **Issue Tracker**: https://github.com/tientv/intellij-odoo-plugin/issues
- **Documentation**: https://github.com/tientv/intellij-odoo-plugin
- **Source Code**: https://github.com/tientv/intellij-odoo-plugin

### 6. **Review and Submit**
1. **Preview** the plugin page
2. **Review** all information for accuracy
3. **Submit** for JetBrains review
4. **Wait** for approval (typically 1-3 business days)

## 📊 Post-Submission

### **Monitoring**
- Track downloads and ratings in Developer Console
- Monitor GitHub issues for user feedback
- Respond to user reviews and questions

### **Updates**
For future updates:
1. Increment version in `build.gradle.kts`
2. Update `CHANGELOG.md`
3. Build new plugin version
4. Upload updated ZIP to marketplace

### **Marketplace Optimization**

#### **SEO Keywords**
- odoo
- python
- pycharm
- code completion
- ide
- development tools
- framework support

#### **Tags for Better Discoverability**
- odoo-development
- python-tools
- pycharm-plugin
- code-completion
- ide-integration
- framework-support

## 🔧 Automated Publishing (Optional)

### **GitHub Actions Setup**
You can automate plugin publishing using GitHub Actions:

```yaml
name: Publish Plugin
on:
  release:
    types: [published]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Publish Plugin
        env:
          PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
        run: ./gradlew publishPlugin
```

### **Required Secrets**
Add to GitHub repository secrets:
- `PUBLISH_TOKEN`: Your JetBrains Hub token

## 📞 Support Information

### **For Users**
- **Documentation**: README.md in repository
- **Issues**: GitHub Issues tracker
- **Questions**: GitHub Discussions

### **For Marketplace Team**
- **Contact**: tientran201yd@gmail.com
- **Source Code**: https://github.com/tientv/intellij-odoo-plugin
- **License**: MIT (permissive, commercial-friendly)

## 📈 Success Metrics

### **Target Metrics**
- **Downloads**: 1,000+ in first 3 months
- **Rating**: 4.5+ stars
- **Reviews**: Positive feedback on Odoo development productivity

### **Feature Requests**
Based on user feedback, consider adding:
- XML view completion
- JavaScript/web asset support
- More Odoo versions support
- Advanced debugging features

---

**Ready for submission to JetBrains Marketplace! 🚀**

All files are prepared and the plugin is marketplace-ready. Follow the steps above to publish your plugin and share it with the Odoo development community.