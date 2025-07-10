# Testing Guide for Odoo Plugin v1.0.5

This guide provides comprehensive testing instructions for the new inheritance features added in v1.0.5.

## 🚀 Quick Start Testing

### Prerequisites
1. Install PyCharm Community/Professional 2023.3+
2. Install the plugin from `build/distributions/odoo-pycharm-plugin-1.0.5.zip`
3. Open an Odoo project containing `__manifest__.py` files

### Basic Plugin Detection Test
```python
# 1. Open any Odoo project
# 2. Check if plugin detected the project (should see model completion working)
model = self.env['res.partner']  # Should auto-complete model names
```

## 🧬 Inheritance Features Testing

### Test 1: _inherit Navigation
```python
class SaleOrder(models.Model):
    _inherit = 'sale.order'  # Cmd+click here should navigate to sale.order model
    _inherit = ['sale.order', 'mail.thread']  # Both should be clickable
```

**Expected Result**: Clicking on `'sale.order'` navigates to the sale.order model definition.

### Test 2: _inherit String Completion
```python
class MyModel(models.Model):
    _inherit = 'res.p'  # Type this and press Ctrl+Space
```

**Expected Result**: Should auto-complete to `'res.partner'`, `'res.product'`, etc.

### Test 3: Inherited Field Completion
```python
class SaleOrder(models.Model):
    _inherit = 'sale.order'
    
    def test_method(self):
        self.partner_id.  # Should show res.partner fields (name, email, country_id, etc.)
        self.state        # Should show sale.order fields including inherited ones
```

**Expected Result**: 
- `self.partner_id.` shows res.partner fields
- `self.state` shows as available field from parent sale.order model

### Test 4: Inherited Method Completion  
```python
class ProductTemplate(models.Model):
    _inherit = 'product.template'
    
    def test_method(self):
        self.write()      # Should show ORM methods
        self.create()     # Should show inherited methods
        self._compute_display_name()  # Should show parent model methods
```

**Expected Result**: Methods from parent models appear in completion with proper icons.

### Test 5: Chained Field Navigation
```python
class SaleOrder(models.Model):
    _inherit = 'sale.order'
    
    def test_method(self):
        self.partner_id.country_id.name    # Multi-level completion
        self.order_line.product_id.name    # Through One2many -> Many2one
```

**Expected Result**: Each level should complete with fields from the appropriate model.

## 🔍 Advanced Testing Scenarios

### Test 6: Multiple Inheritance Levels
```python
# Test deep inheritance chains
class BaseModel(models.Model):
    _name = 'base.model'
    base_field = fields.Char()

class MiddleModel(models.Model):
    _inherit = 'base.model'
    middle_field = fields.Char()

class FinalModel(models.Model):
    _inherit = 'middle.model'
    
    def test_method(self):
        self.base_field    # Should show field from 2 levels up
        self.middle_field  # Should show field from 1 level up
```

### Test 7: Cross-Module Navigation
```python
# In different modules
class ResPartner(models.Model):
    _inherit = 'res.partner'
    
    def test_method(self):
        self.name         # Should work across modules
        self.email        # Core model fields should be available
```

### Test 8: Complex Relational Chains
```python
class SaleOrder(models.Model):
    _inherit = 'sale.order'
    
    def test_method(self):
        # Complex navigation through multiple models
        self.partner_id.commercial_partner_id.country_id.code
        self.order_line.product_id.categ_id.name
```

## 🎯 Context-Specific Testing

### Test 9: Different Completion Contexts
```python
class TestModel(models.Model):
    _name = 'test.model'
    
    def test_contexts(self):
        # Direct field access
        self.name
        
        # In expressions  
        if self.active:
            pass
        
        # In assignments
        partner_name = self.partner_id.name
        
        # In method calls
        self.write({'name': self.partner_id.name})
```

### Test 10: Method Type Classification
```python
class TestModel(models.Model):
    _name = 'test.model'
    
    @api.depends('name')
    def _compute_display_name(self):  # Should show as COMPUTE method
        pass
    
    @api.onchange('partner_id')  
    def _onchange_partner(self):     # Should show as ONCHANGE method
        pass
    
    def create(self, vals):          # Should show as CRUD method
        pass
    
    def custom_business_logic(self): # Should show as BUSINESS_LOGIC method
        pass
```

## 🐛 Error Scenarios Testing

### Test 11: Circular Inheritance Protection
```python
# This should not cause infinite loops
class ModelA(models.Model):
    _inherit = 'model.b'

class ModelB(models.Model):  
    _inherit = 'model.a'
```

### Test 12: Missing Model References
```python
class TestModel(models.Model):
    _inherit = 'non.existent.model'  # Should not crash plugin
    
    def test_method(self):
        self.some_field  # Should gracefully handle missing inheritance
```

## 📊 Performance Testing

### Test 13: Large Project Performance
1. Open a large Odoo project (100+ modules)
2. Test completion response time (should be < 500ms)
3. Monitor memory usage during completion
4. Test with multiple inheritance levels

### Test 14: Cache Effectiveness
1. Use completion multiple times in same session
2. Verify caching is working (subsequent calls should be faster)
3. Test cache timeout (wait 10+ seconds, should refresh)

## ✅ Success Criteria

### Must Work:
- [ ] _inherit navigation (Cmd+click)
- [ ] _inherit string completion
- [ ] Inherited field completion  
- [ ] Inherited method completion
- [ ] Chained field navigation (self.partner_id.name)
- [ ] Cross-module inheritance
- [ ] Method type classification
- [ ] Performance under 500ms for typical projects

### Should Work:
- [ ] Deep inheritance chains (3+ levels)
- [ ] Complex relational navigation
- [ ] Large project performance
- [ ] Error handling for missing models
- [ ] Cache efficiency

### Nice to Have:
- [ ] Real-time cache updates
- [ ] Advanced type inference
- [ ] XML view integration
- [ ] Documentation links

## 🔧 Troubleshooting

### Common Issues:
1. **No completion appearing**: Check if project contains `__manifest__.py` files
2. **Slow performance**: Check project size and consider excluding large directories
3. **Missing inherited fields**: Verify parent models are properly detected
4. **Navigation not working**: Ensure plugin is properly installed and enabled

### Debug Steps:
1. Check PyCharm logs for errors
2. Verify plugin is enabled in Settings → Plugins
3. Test with a simple Odoo project first
4. Clear PyCharm caches and restart

---

**Testing Checklist**: Complete all tests above before considering the release ready for production use.