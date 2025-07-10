# GitHub Repository Setup Guide

This guide will help you set up the GitHub repository and push the plugin source code.

## 🚀 Quick Setup Commands

Run these commands in order to set up your GitHub repository:

### 1. **Initialize Git Repository**
```bash
cd /Users/tienable/Projects/TMC/plugins/intellij-odoo-plugin

# Initialize git repository
git init

# Add all files
git add .

# Create initial commit
git commit -m "Initial commit: Odoo Development Support plugin v1.0.0

✨ Features:
- Smart code completion for Odoo models, fields, and methods
- Code navigation and reference resolution
- Code inspections and validations
- Project structure detection and multi-module support
- PyCharm 2023.3+ compatibility

🎯 Ready for JetBrains Marketplace submission"
```

### 2. **Add GitHub Remote**
```bash
# Add the GitHub repository as remote origin
git remote add origin https://github.com/tientv/intellij-odoo-plugin.git

# Set up main branch
git branch -M main
```

### 3. **Push to GitHub**
```bash
# Push the code to GitHub
git push -u origin main
```

## 📁 Repository Structure

After pushing, your GitHub repository will contain:

```
intellij-odoo-plugin/
├── 📁 .github/
│   └── 📁 workflows/
│       ├── build.yml                    # CI build workflow
│       └── release.yml                  # Release workflow
├── 📁 gradle/
│   └── 📁 wrapper/
│       ├── gradle-wrapper.jar           # Gradle wrapper JAR
│       └── gradle-wrapper.properties    # Gradle wrapper config
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 kotlin/                   # Plugin source code
│   │   │   └── 📁 com/github/tientv/... # Package structure
│   │   └── 📁 resources/
│   │       ├── 📁 META-INF/
│   │       │   └── plugin.xml           # Plugin configuration
│   │       └── 📁 icons/
│   │           └── odoo.svg             # Plugin icon
│   └── 📁 test/                         # Test sources (future)
├── .gitignore                           # Git ignore rules
├── build.gradle.kts                     # Build configuration
├── CHANGELOG.md                         # Version history
├── DEVELOPMENT.md                       # Developer guide
├── gradle.properties                    # Gradle properties
├── gradlew                              # Gradle wrapper script
├── LICENSE                              # MIT license
├── MARKETPLACE.md                       # Marketplace submission guide
├── README.md                            # Main documentation
└── settings.gradle.kts                  # Gradle settings
```

## 🔧 GitHub Repository Settings

### **Repository Settings**
1. **Repository name**: `intellij-odoo-plugin`
2. **Description**: `Intelligent code completion and navigation for Odoo development in PyCharm`
3. **Topics/Tags**: 
   ```
   odoo, pycharm, intellij-plugin, code-completion, python, ide, jetbrains
   ```
4. **Website**: Will be set to JetBrains Marketplace URL after publication
5. **License**: MIT License

### **Branch Protection** (Recommended)
```bash
# After repository is created, set up branch protection for main branch:
# 1. Go to Settings > Branches
# 2. Add rule for main branch:
#    - Require pull request reviews
#    - Require status checks (build workflow)
#    - Dismiss stale reviews
```

### **GitHub Secrets** (For Automated Publishing)
Add these secrets in **Settings > Secrets and variables > Actions**:

1. **`PUBLISH_TOKEN`**: Your JetBrains Hub token for marketplace publishing
   - Get from: https://plugins.jetbrains.com/author/me/tokens
   - Scope: "Marketplace"

## 📋 Post-Setup Tasks

### **1. Enable GitHub Actions**
The workflows will automatically run on:
- **Push to main/develop**: Build and test plugin
- **Release published**: Build, test, and publish to marketplace

### **2. Create First Release**
```bash
# Tag the current version
git tag -a v1.0.0 -m "Release version 1.0.0

🎉 First release of Odoo Development Support plugin

Features:
- Smart autocompletion for Odoo models, fields, and methods
- Code navigation and reference resolution  
- Code inspections and validations
- Automatic Odoo project detection
- Multi-module support

Supported:
- PyCharm Community & Professional 2023.3+
- Odoo 18.0, 17.0, 16.0"

# Push the tag
git push origin v1.0.0
```

### **3. Create GitHub Release**
1. Go to **Releases** on GitHub
2. Click **"Create a new release"**
3. Select tag `v1.0.0`
4. Release title: `Odoo Development Support v1.0.0`
5. Description: Copy from CHANGELOG.md
6. Attach the built plugin ZIP (optional)
7. Publish release

## 🎯 Repository Features

### **Issues Templates**
Create `.github/ISSUE_TEMPLATE/` with:
- Bug report template
- Feature request template
- Question template

### **Pull Request Template**
Create `.github/pull_request_template.md`

### **Contributing Guidelines**
Create `CONTRIBUTING.md` with:
- Development setup
- Code style guidelines
- Contribution process

### **Code of Conduct**
Add `CODE_OF_CONDUCT.md` for community guidelines

## 🚀 Marketing Your Plugin

### **README Badges**
Update README.md plugin ID badges after marketplace approval:
```markdown
[![JetBrains Plugin](https://img.shields.io/jetbrains/plugin/v/YOUR_PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID)
[![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/YOUR_PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/YOUR_PLUGIN_ID)
```

### **Social Media**
- Share on Twitter with hashtags: #PyCharm #Odoo #Plugin
- Post in Odoo community forums
- Share in relevant developer communities

### **Documentation**
- Create wiki pages for advanced usage
- Add video demos (optional)
- Write blog posts about plugin features

## 📞 Support Setup

### **Issue Labels**
Create labels:
- `bug` - Something isn't working
- `enhancement` - New feature or request
- `documentation` - Improvements or additions to docs
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention is needed
- `question` - Further information is requested

### **Discussions**
Enable GitHub Discussions for:
- Q&A
- Ideas
- General discussions
- Show and tell

## ✅ Checklist

- [ ] Initialize git repository
- [ ] Add remote origin
- [ ] Push code to GitHub
- [ ] Set up repository description and topics
- [ ] Enable GitHub Actions
- [ ] Create first release tag
- [ ] Add GitHub secrets for publishing
- [ ] Set up issue templates
- [ ] Configure branch protection
- [ ] Update badges after marketplace approval

---

**Your plugin is now ready for GitHub and JetBrains Marketplace! 🎉**

Follow the marketplace submission guide in `MARKETPLACE.md` to publish your plugin.