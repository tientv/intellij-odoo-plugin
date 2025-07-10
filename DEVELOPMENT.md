# Odoo PyCharm Plugin Development Guide

## Step-by-Step Development Instructions

### Prerequisites
- **Java 17** or higher
- **IntelliJ IDEA** (Community or Ultimate) or **PyCharm** (for testing)
- **Gradle** (included via wrapper)
- **Git** (for version control)

### 1. Project Setup

#### Clone and Navigate
```bash
cd /Users/tienable/Projects/TMC/odoo18-app/odoo-pycharm-plugin
```

#### Verify Project Structure
```
odoo-pycharm-plugin/
├── build.gradle.kts                 # Build configuration
├── gradle.properties                # Gradle properties
├── settings.gradle.kts              # Gradle settings
├── gradlew                          # Gradle wrapper (Unix)
├── gradlew.bat                      # Gradle wrapper (Windows)
├── src/
│   ├── main/
│   │   ├── kotlin/                  # Kotlin source files
│   │   │   └── com/tmc/odoo/pycharm/
│   │   │       ├── services/        # Project services
│   │   │       ├── completion/      # Code completion
│   │   │       ├── references/      # Reference resolution
│   │   │       ├── inspections/     # Code inspections
│   │   │       ├── models/          # Data models
│   │   │       ├── icons/           # Icon definitions
│   │   │       └── ...
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml       # Plugin configuration
│   │       └── icons/               # Icon files
│   └── test/                        # Test sources
├── README.md                        # User documentation
└── DEVELOPMENT.md                   # This file
```

### 2. Development Environment

#### Open in IntelliJ IDEA
1. Open IntelliJ IDEA
2. Choose **Open** and select the plugin directory
3. Wait for Gradle import to complete
4. Ensure Project SDK is set to Java 17+

#### Configure Run Configuration
1. Go to **Run > Edit Configurations**
2. Add new **Gradle** configuration
3. Set tasks to: `runIde`
4. Set working directory to plugin root

### 3. Building the Plugin

#### Build Plugin JAR
```bash
./gradlew buildPlugin
```

The built plugin will be in `build/distributions/odoo-pycharm-plugin-1.0.0.zip`

#### Verify Plugin
```bash
./gradlew verifyPlugin
```

### 4. Testing

#### Run Tests
```bash
./gradlew test
```

#### Run Plugin in Development IDE
```bash
./gradlew runIde
```

This will:
- Start a new PyCharm Community instance
- Load your plugin automatically
- Allow you to test functionality in real-time

### 5. Key Development Areas

#### Adding New Completion Types
1. Create new provider in `completion/` package
2. Extend `CompletionContributor`
3. Register in `plugin.xml`

**Example**: Adding XML completion
```kotlin
class OdooXmlCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            xmlElementPattern(),
            OdooXmlCompletionProvider()
        )
    }
}
```

#### Adding New Inspections
1. Create inspection class in `inspections/` package
2. Extend `LocalInspectionTool`
3. Register in `plugin.xml`

**Example**: Field validation inspection
```kotlin
class OdooFieldInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PyElementVisitor() {
            override fun visitPyCallExpression(node: PyCallExpression) {
                // Check if this is a field definition
                checkFieldDefinition(node, holder)
            }
        }
    }
}
```

#### Extending Model Analysis
1. Modify `OdooProjectService`
2. Add new extraction methods
3. Update caching logic

### 6. Plugin Configuration

#### Key Files
- **`plugin.xml`**: Main configuration, extensions, actions
- **`build.gradle.kts`**: Build configuration, dependencies
- **`OdooProjectService.kt`**: Core service for project analysis

#### Adding New Extensions
```xml
<extensions defaultExtensionNs="com.intellij">
    <completion.contributor 
        language="XML" 
        implementationClass="com.tmc.odoo.pycharm.completion.OdooXmlCompletionContributor"/>
</extensions>
```

### 7. Testing Your Plugin

#### Manual Testing
1. Run `./gradlew runIde`
2. Open an Odoo project
3. Test completion features:
   - Model name completion in strings
   - Field completion with `self.`
   - Method completion
   - Reference navigation

#### Automated Testing
```kotlin
class OdooCompletionTest : BasePlatformTestCase() {
    fun testModelCompletion() {
        myFixture.configureByText(
            "test.py",
            """
            class TestModel(models.Model):
                _name = 'test.model'
                
                def test_method(self):
                    model = self.env['<caret>']
            """.trimIndent()
        )
        
        val completions = myFixture.completeBasic()
        assertContainsElements(completions.map { it.lookupString }, "test.model")
    }
}
```

### 8. Debugging

#### Enable Debug Logging
Add to `idea.log`:
```
#com.tmc.odoo.pycharm
com.tmc.odoo.pycharm.services.OdooProjectService:DEBUG
```

#### Debug Plugin Code
1. Set breakpoints in your Kotlin code
2. Run in debug mode: `./gradlew runIde --debug-jvm`
3. Attach debugger to port 5005

### 9. Common Issues and Solutions

#### Plugin Not Loading
- Check `plugin.xml` syntax
- Verify all required dependencies are listed
- Check IntelliJ Platform version compatibility

#### Completion Not Working
- Verify PSI pattern matching
- Check if `OdooProjectService.isOdooProject()` returns true
- Debug completion provider logic

#### Performance Issues
- Implement proper caching in services
- Use lightweight PSI operations
- Avoid heavy computation in completion providers

### 10. Advanced Features

#### Adding XML Support
1. Create XML-specific completion providers
2. Add XML file type detection
3. Implement XML-to-Python navigation

#### Adding JavaScript Support
1. Extend to web assets
2. Add JavaScript completion for Odoo framework
3. Support for QWeb templates

### 11. Distribution

#### Publish to JetBrains Marketplace
1. Create account at https://plugins.jetbrains.com/
2. Configure `publishPlugin` task
3. Run `./gradlew publishPlugin`

#### Manual Distribution
1. Build plugin: `./gradlew buildPlugin`
2. Share the ZIP file from `build/distributions/`
3. Users install via **Settings > Plugins > Install from Disk**

### 12. Best Practices

#### Code Quality
- Follow Kotlin coding conventions
- Write comprehensive tests
- Use meaningful variable names
- Add proper documentation

#### Performance
- Cache expensive operations
- Use appropriate PSI traversal methods
- Profile plugin performance regularly

#### User Experience
- Provide clear error messages
- Add helpful tooltips and descriptions
- Ensure consistent behavior across features

### 13. Resources

#### Documentation
- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [PyCharm Plugin Development](https://plugins.jetbrains.com/docs/intellij/pycharm.html)
- [Kotlin Language Reference](https://kotlinlang.org/docs/reference/)

#### Community
- [Plugin Development Forum](https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development)
- [JetBrains Platform Slack](https://plugins.jetbrains.com/slack)

---

Happy coding! 🚀