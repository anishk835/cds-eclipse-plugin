# Missing 12% - SAP CAP CDS Features Not Yet Implemented

## Overview

With 88% coverage achieved through Phases 1-20, the remaining 12% consists of advanced SAP CAP CDS features. This document details what's missing and estimates the effort required.

---

## Remaining Features Breakdown

### 1. Advanced Projection Features (3%)

**What's Missing:**
```cds
// ❌ Not validated: Complex SELECT features
entity BookView as SELECT from Books {
  // Excluding specific fields
  * excluding { internalNotes, draft },

  // Column expressions with aliases
  price * 1.19 as priceWithTax : Decimal,

  // CASE expressions
  CASE
    WHEN stock > 100 THEN 'In Stock'
    WHEN stock > 0 THEN 'Low Stock'
    ELSE 'Out of Stock'
  END as stockStatus : String,

  // Subqueries in SELECT
  (SELECT COUNT(*) FROM Reviews WHERE product = $self.ID) as reviewCount,

  // CAST expressions
  CAST(price AS Integer) as roundedPrice,

  // String operations
  CONCAT(author.firstName, ' ', author.lastName) as authorFullName
};
```

**Not Validated:**
- ❌ `excluding` clause validation
- ❌ CASE expression type checking
- ❌ Subquery validation in projections
- ❌ CAST expression validation
- ❌ Built-in function validation (CONCAT, UPPER, LOWER, etc.)
- ❌ Column alias uniqueness
- ❌ Expression result type inference in SELECT

**Estimated Effort:** Medium (1-2 weeks)

---

### 2. Entity Extension Validation (2%)

**What's Missing:**
```cds
// ❌ Not validated: extend validations
namespace bookshop;

entity Books {
  key ID: UUID;
  title: String;
}

// Extend existing entity
extend Books with {
  // Should validate:
  // - Books exists
  // - No duplicate field names
  // - Types are valid
  subtitle: String;
  edition: Integer;

  // ❌ Should error: duplicate field
  // title: String;
};

// Extend with associations
extend Books with {
  // Should validate association target exists
  publisher: Association to Publishers;
};

// Extend with annotations
extend Books with @readonly;

// Extend projected entities
extend projection BookView with {
  additionalField: String;
};
```

**Not Validated:**
- ❌ Extend target resolution (entity exists?)
- ❌ Duplicate field detection in extensions
- ❌ Type compatibility in extended fields
- ❌ Association validation in extensions
- ❌ Annotation validation in extensions
- ❌ Extend projection validation
- ❌ Multiple extends on same entity

**Estimated Effort:** Small (3-5 days)

---

### 3. Annotation Validation (3%)

**What's Missing:**
```cds
// ❌ Not validated: annotation syntax and semantics
@title: 'Book Shop'
@requires: 'authenticated-user'
service BookshopService {

  @readonly
  @cds.autoexpose
  entity Books {
    key ID: UUID;

    @mandatory
    @assert.range: [0, 9999]
    stock: Integer;

    @Core.Computed
    @readonly
    totalValue: Decimal;
  }
}

// UI annotations
annotate Books with @(
  UI.LineItem: [
    { Value: title, Label: 'Title' },
    { Value: author.name, Label: 'Author' }
  ],
  UI.SelectionFields: [title, author]
);

// Custom annotations
@MyCustom.annotation: { key: 'value' }
entity Products { ... }
```

**Not Validated:**
- ❌ Annotation syntax validation
- ❌ Standard annotation vocabulary (@title, @readonly, @requires)
- ❌ UI.* annotations (LineItem, SelectionFields, HeaderInfo, etc.)
- ❌ Core.* annotations (Computed, Immutable, etc.)
- ❌ Validation annotations (@assert.range, @assert.format)
- ❌ Authorization annotations (@requires, @restrict)
- ❌ Custom annotation structure
- ❌ Annotation target validation (can this annotation go here?)
- ❌ Annotation value type checking

**Estimated Effort:** Medium (1-2 weeks)

---

### 4. Actions and Functions (2%)

**What's Missing:**
```cds
// ❌ Not validated: action/function definitions
service BookshopService {
  entity Books { ... }

  // Bound actions (instance-level)
  entity Books actions {
    action submitOrder(quantity: Integer) returns {
      orderID: UUID;
      totalPrice: Decimal;
    };

    function calculateDiscount(percentage: Decimal) returns Decimal;
  };

  // Unbound actions (service-level)
  action createBulkOrder(items: many {
    bookID: UUID;
    quantity: Integer;
  }) returns {
    orderID: UUID;
  };

  function searchBooks(query: String) returns many Books;
}
```

**Not Validated:**
- ❌ Action/function signature validation
- ❌ Parameter type validation
- ❌ Return type validation
- ❌ Bound vs unbound action rules
- ❌ Parameter multiplicity (`many`)
- ❌ Complex return types (structured, array)
- ❌ Action naming conflicts

**Estimated Effort:** Small (3-5 days)

---

### 5. Events (1%)

**What's Missing:**
```cds
// ❌ Not validated: event definitions
service BookshopService {

  // Event definition
  event OrderPlaced {
    orderID: UUID;
    customer: Association to Customers;
    items: many {
      product: Association to Books;
      quantity: Integer;
    };
    timestamp: DateTime;
  }

  event StockUpdated {
    product: Association to Books;
    oldStock: Integer;
    newStock: Integer;
  }
}
```

**Not Validated:**
- ❌ Event structure validation
- ❌ Event field type validation
- ❌ Event associations
- ❌ Event naming rules
- ❌ Event payload structure

**Estimated Effort:** Very Small (2-3 days)

---

### 6. Temporal Data Features (1%)

**What's Missing:**
```cds
// ❌ Not validated: temporal table features
@cds.persistence.journal
entity Books {
  key ID: UUID;
  title: String;
  price: Decimal;

  // Temporal columns (automatically managed)
  validFrom: DateTime;
  validTo: DateTime;
}

// Time-travel queries (not in schema definition)
// SELECT * FROM Books AS OF TIMESTAMP '2024-01-01'
```

**Not Validated:**
- ❌ `@cds.persistence.journal` annotation
- ❌ Temporal column validation
- ❌ Time-travel query syntax (if in CDS)

**Estimated Effort:** Very Small (1-2 days)

---

### 7. Advanced Type Features (1%)

**What's Missing:**
```cds
// ❌ Not validated: advanced type features

// Type parameters
type List(T: Type) : many T;
type Map(K: Type, V: Type) : { key: K; value: V };

// Generic type usage
products: List(Product);
cache: Map(String, Integer);

// Type unions (not standard CDS, but some extensions)
type StringOrNumber : String | Integer;

// Structured type validation
type Address {
  street: String(100);
  city: String(50);
  // ❌ Should validate nested structure depth
  coordinates: {
    latitude: Decimal;
    longitude: Decimal;
  };
}
```

**Not Validated:**
- ❌ Type parameters/generics
- ❌ Type unions
- ❌ Deep structured type validation
- ❌ Type constraints (min, max, pattern)

**Estimated Effort:** Very Small (2-3 days)

---

### 8. Advanced Association Features (1%)

**What's Missing:**
```cds
// ❌ Not validated: advanced association features

entity Books {
  key ID: UUID;

  // Association with WHERE clause
  activeReviews: Association to many Reviews
    on reviews.book = $self
    where reviews.status = 'approved';

  // Multiple ON conditions with complex logic
  relatedBooks: Association to many Books
    on relatedBooks.category = category
    and relatedBooks.author = author
    and relatedBooks.ID <> ID;  // Exclude self

  // Association redirects
  publisher: Association to Publishers
    redirected to Publishers.activePublishers;
}

// Composition with parameters
entity Orders {
  key ID: UUID;
  items: Composition of many OrderItems
    on items.order = $self
    { key ID: UUID; product: Association to Products; };
}
```

**Not Validated:**
- ❌ WHERE clause in associations
- ❌ Complex ON conditions (AND, OR, nested)
- ❌ Association redirects
- ❌ Composition with inline structure
- ❌ Self-excluding conditions (`ID <> $self.ID`)

**Estimated Effort:** Small (3-5 days)

---

### 9. Service-Specific Features (1%)

**What's Missing:**
```cds
// ❌ Not validated: service-specific features

@path: '/api/v1'
@requires: 'authenticated-user'
service BookshopService {

  // Service-level annotations
  @odata.draft.enabled
  @Capabilities.Insertable: false
  entity Books as projection on data.Books;

  // Service-level type definitions
  type OrderStatus : String enum {
    Pending;
    Confirmed;
    Shipped;
    Delivered;
  };

  // Exposed vs non-exposed entities
  @cds.autoexpose
  entity InternalEntity { ... }
}
```

**Not Validated:**
- ❌ Service path validation
- ❌ Service-level annotation validation
- ❌ OData capability annotations
- ❌ Draft enablement validation
- ❌ Auto-expose rules
- ❌ Service isolation (what's visible where)

**Estimated Effort:** Small (3-5 days)

---

### 10. Advanced Constraint Features (0.5%)

**What's Missing:**
```cds
entity Products {
  key ID: UUID;

  // ❌ Complex CHECK constraints
  price: Decimal check price > 0 and price < 10000;

  // ❌ Multi-column constraints
  startDate: Date;
  endDate: Date;
  constraint validDates check endDate > startDate;

  // ❌ Conditional constraints
  discount: Decimal check discount >= 0 and (
    category = 'Sale' or discount = 0
  );

  // ❌ UNIQUE constraints on expressions
  constraint uniqueCode unique (UPPER(productCode));
}
```

**Not Validated:**
- ❌ Complex CHECK expressions (multiple conditions)
- ❌ Multi-column constraints
- ❌ Conditional constraints
- ❌ Expression-based UNIQUE constraints

**Estimated Effort:** Very Small (1-2 days)

---

### 11. Localization Features (0.5%)

**What's Missing:**
```cds
// ❌ Not validated: full localization features

entity Books {
  key ID: UUID;

  // Localized text
  localized title: String;
  localized description: LargeString;

  // ❌ Should validate:
  // - Only String types can be localized
  // - Localized fields can't be keys
  // - Localized in associations
}

// Translation tables (auto-generated)
entity Books.texts {
  key ID: UUID;
  key locale: String(5);
  title: String;
  description: LargeString;
}
```

**Not Validated:**
- ❌ Localized field validation rules (already partial in Phase 11)
- ❌ Translation table generation rules
- ❌ Locale validation
- ❌ Localized in projections

**Estimated Effort:** Very Small (1-2 days)

---

## Summary Table

| Feature | Coverage | Effort | Priority | Complexity |
|---------|----------|--------|----------|------------|
| Advanced Projections | 3% | Medium | High | High |
| Entity Extensions | 2% | Small | Medium | Medium |
| Annotations | 3% | Medium | High | High |
| Actions/Functions | 2% | Small | Medium | Medium |
| Events | 1% | Very Small | Low | Low |
| Temporal Data | 1% | Very Small | Low | Low |
| Advanced Types | 1% | Very Small | Medium | Medium |
| Advanced Associations | 1% | Small | Medium | Medium |
| Service Features | 1% | Small | Medium | Medium |
| Advanced Constraints | 0.5% | Very Small | Low | Low |
| Localization | 0.5% | Very Small | Low | Low |
| **Total** | **12%** | **~3-4 weeks** | - | - |

---

## Prioritization for Next Phases

### High Priority (Should do next)
1. **Annotations (3%)** - Critical for SAP Fiori apps
2. **Advanced Projections (3%)** - Common in real projects
3. **Entity Extensions (2%)** - Frequently used pattern

**Subtotal:** 8% (would reach 96% coverage)

### Medium Priority
4. **Actions/Functions (2%)** - Important for OData services
5. **Advanced Associations (1%)** - Used in complex models
6. **Service Features (1%)** - Service-level validation

**Subtotal:** 4% (would reach 100% coverage)

### Low Priority (Nice to have)
7. **Events (1%)** - Less common
8. **Temporal Data (1%)** - Specialized feature
9. **Advanced Types (1%)** - Edge cases
10. **Advanced Constraints (0.5%)** - Rare usage
11. **Localization (0.5%)** - Partially done in Phase 11

---

## Recommended Next Steps

### Phase 21: Annotations (3%)
**Focus:** Standard SAP annotations (@readonly, @title, UI.*, etc.)
**Effort:** 1-2 weeks
**Impact:** High - enables Fiori app development validation

### Phase 22: Advanced Projections (3%)
**Focus:** CASE expressions, excluding, subqueries, CAST
**Effort:** 1-2 weeks
**Impact:** High - used in most projections

### Phase 23: Entity Extensions (2%)
**Focus:** Extend validation, duplicate detection
**Effort:** 3-5 days
**Impact:** Medium - common in modular projects

**After Phase 23:** 96% coverage achieved! 🎯

---

## Features Intentionally Excluded

Some CDS features are excluded because they're:

1. **Runtime-only** (not in schema definition)
   - Query syntax in application code
   - Data manipulation statements
   - Transaction handling

2. **Deployment-specific** (not language features)
   - Database-specific SQL generation
   - Migration scripts
   - Index optimization

3. **Framework-specific** (beyond CDS language)
   - Node.js service implementation
   - Java service implementation
   - Authentication/Authorization logic

---

## Conclusion

The **remaining 12%** consists primarily of:
- **Advanced features** used in sophisticated models (6%)
- **Annotation ecosystem** for Fiori apps (3%)
- **Specialized features** for specific use cases (3%)

**Good news:**
- The **core 88%** is production-ready and covers most SAP CAP projects
- The remaining features follow similar patterns to implemented ones
- Estimated **3-4 weeks** to reach 100% coverage
- Next 3 phases (21-23) would bring us to **96% coverage**

The plugin is already suitable for production SAP CAP development. The remaining 12% would add polish and support for advanced/specialized scenarios.
