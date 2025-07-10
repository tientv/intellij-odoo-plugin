#!/bin/bash

# GitHub Repository Setup Script for Odoo Development Support Plugin
# Run this script to initialize Git and push to GitHub

set -e

echo "🚀 Setting up GitHub repository for Odoo Development Support plugin..."

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    echo "❌ Error: Not in plugin directory. Please run this script from the plugin root."
    exit 1
fi

# Initialize git if not already done
if [ ! -d ".git" ]; then
    echo "📦 Initializing Git repository..."
    git init
else
    echo "✅ Git repository already initialized"
fi

# Add all files
echo "📁 Adding all files to Git..."
git add .

# Check if there are changes to commit
if git diff --staged --quiet; then
    echo "ℹ️  No changes to commit"
else
    # Create initial commit
    echo "💾 Creating initial commit..."
    git commit -m "Initial commit: Odoo Development Support plugin v1.0.1

✨ Features:
- Smart code completion for Odoo models, fields, and methods
- Code navigation and reference resolution
- Code inspections and validations
- Project structure detection and multi-module support
- PyCharm 2023.3+ compatibility

🎯 Ready for JetBrains Marketplace submission

📁 Repository includes:
- Complete Kotlin source code
- Build configuration with Gradle
- Comprehensive documentation (README, CHANGELOG, MARKETPLACE guide)
- GitHub Actions for CI/CD
- MIT License
- Plugin icon and assets

🔧 Technical details:
- IntelliJ Platform SDK 2023.3.2
- Java 17+ compatibility  
- Supports PyCharm Community & Professional
- Multi-module Odoo project support"
fi

# Check if remote origin exists
if git remote get-url origin >/dev/null 2>&1; then
    echo "✅ Remote origin already configured"
else
    # Add GitHub remote
    echo "🔗 Adding GitHub remote..."
    git remote add origin https://github.com/tientv/intellij-odoo-plugin.git
fi

# Set main branch
echo "🌿 Setting up main branch..."
git branch -M main

# Ask user before pushing
echo ""
echo "🚀 Ready to push to GitHub!"
echo "Repository: https://github.com/tientv/intellij-odoo-plugin"
echo ""
read -p "Do you want to push now? (y/N): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "⬆️  Pushing to GitHub..."
    git push -u origin main
    
    echo ""
    echo "🎉 Success! Plugin source code pushed to GitHub"
    echo ""
    echo "📋 Next steps:"
    echo "1. 🏷️  Create a release tag:"
    echo "   git tag -a v1.0.1 -m 'Release v1.0.1'"
    echo "   git push origin v1.0.1"
    echo ""
    echo "2. 🔧 Set up GitHub repository:"
    echo "   - Add repository description and topics"
    echo "   - Enable GitHub Actions"
    echo "   - Set up branch protection"
    echo ""
    echo "3. 🏪 Submit to JetBrains Marketplace:"
    echo "   - Follow instructions in MARKETPLACE.md"
    echo "   - Build plugin: ./gradlew buildPlugin"
    echo "   - Upload to: https://plugins.jetbrains.com/"
    echo ""
    echo "📖 Documentation:"
    echo "   - README.md - Main documentation"
    echo "   - MARKETPLACE.md - Marketplace submission guide"
    echo "   - GITHUB_SETUP.md - Detailed GitHub setup"
    echo "   - DEVELOPMENT.md - Development guide"
    echo ""
    echo "🔗 Repository: https://github.com/tientv/intellij-odoo-plugin"
else
    echo "ℹ️  Push cancelled. You can push later with:"
    echo "   git push -u origin main"
fi

echo ""
echo "✅ Setup complete!"