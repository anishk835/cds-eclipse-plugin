# Implementation Plan: Phase 12 - Association ON Conditions

## Context

Phase 11 (Localized Data) is complete and implemented ~49% of the SAP CAP CDS specification. Phase 12 adds association ON conditions to reach ~54% coverage.

**Current State:**
- ✅ Phase 11: Localized data fully implemented
- ✅ Basic associations work: `customer: Association to Customers;`
- ✅ Cardinality works: `Association to one` / `Composition of many`
- ❌ Missing: ON conditions for proper foreign key relationships

**Why This Matters:**
- **CRITICAL for production** - Without ON conditions, can't model proper foreign keys
- Essential for OData navigation properties
- Required for SQL foreign key generation
- Enables filtered associations
- Standard pattern in SAP CAP for relational integrity

**Target SAP CAP CDS Syntax:**
```cds
entity Orders {
  customer_ID: UUID;
  customer: Association to Customers
    on customer.ID = $self.customer_ID;
}

// Filtered associations
entity Products {
  reviews: Association to many Reviews
    on reviews.product = $self
    and reviews.status = 'approved';
}

// Self-reference
entity Employees {
  manager_ID: UUID;
  manager: Association to Employees
    on manager.ID = $self.manager_ID;
}
```

## Recommended Approach: Extend AssocDef with ON Condition

### Architecture Decision

After examining the codebase, I recommend **extending AssocDef with optional ON condition** because:

1. **Fits Existing Structure:**
   - AssocDef already exists in grammar
   - Natural place for association metadata
   - Already has kind, cardinality, target

2. **SAP CAP Semantics:**
   - ON condition is part of the association definition
   - Not a separate constraint
   - Modifies how association works

3. **Allows Complex Expressions:**
   - ON conditions use existing Expression AST
   - Can have comparisons, logical operators (and, or)
   - Supports $self references

### Grammar Structure

```xtext
// Modify AssocDef (currently line 130)
AssocDef:
    kind=AssocKind cardinality=Cardinality? 'to' target=[EntityDef|QualifiedName]
    ('on' onCondition=OnCondition)?;

// New ON condition rule
OnCondition:
    ComparisonExpr;

// Extend Expression hierarchy to support comparisons
ComparisonExpr returns Expression:
    LogicalOrExpr
    ({ComparisonExpr.left=current} op=ComparisonOp right=LogicalOrExpr)*;

enum ComparisonOp:
    EQ='=' | NEQ='!=' | LT='<' | LTE='<=' | GT='>' | GTE='>=';

LogicalOrExpr returns Expression:
    LogicalAndExpr
    ({BinaryExpr.left=current} op='or' right=LogicalAndExpr)*;

LogicalAndExpr returns Expression:
    AddExpr
    ({BinaryExpr.left=current} op='and' right=AddExpr)*;

// Existing AddExpr, MulExpr, etc. remain unchanged
```

**Key Points:**
- Add `('on' onCondition=OnCondition)?` to AssocDef
- Extend expression hierarchy to support comparisons and logical operators
- ON condition is an Expression (reuses existing AST)
- Support for `$self` reference (special keyword)

## Implementation Steps

### Step 1: Grammar Extension

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext`

**Location 1:** Modify AssocDef (line 130)

```xtext
AssocDef:
    kind=AssocKind cardinality=Cardinality? 'to' target=[EntityDef|QualifiedName]
    ('on' onCondition=Expression)?;
```

**Location 2:** Extend Expression hierarchy (after line 227)

Add comparison and logical operators to expression grammar:

```xtext
// ─────────────────────────────────────────────────────────────────────────────
// Expressions  (Phase 5 + Phase 12 enhancements)
// ─────────────────────────────────────────────────────────────────────────────

Expression:
    LogicalOrExpr;

LogicalOrExpr returns Expression:
    LogicalAndExpr
    ({BinaryExpr.left=current} op='or' right=LogicalAndExpr)*;

LogicalAndExpr returns Expression:
    LogicalAndExpr
    ({BinaryExpr.left=current} op='and' right=ComparisonExpr)*;

ComparisonExpr returns Expression:
    AddExpr
    ({BinaryExpr.left=current} op=ComparisonOp right=AddExpr)?;

enum ComparisonOp:
    EQ='=' | NEQ='!=' | LT='<' | LTE='<=' | GT='>' | GTE='>=';

AddExpr returns Expression:
    MulExpr
    ({BinaryExpr.left=current} op=('+' | '-') right=MulExpr)*;

// MulExpr, UnaryExpr, PrimaryExpr remain unchanged
```

**Location 3:** Add $self keyword support (modify PrimaryExpr)

```xtext
PrimaryExpr returns Expression:
    '(' Expression ')'
    | {LiteralExpr} value=Literal
    | {FuncExpr} func=ID '(' (args+=Expression (',' args+=Expression)*)? ')'
    | {EnumRef} '#' value=[EnumValue|ID]
    | {SelfRef} '$self'
    | {RefExpr} ref=[Element|ID];
```

**Auto-Generated:**
- `AssocDef.java` updated with `getOnCondition()`
- `ComparisonOp.java` enum
- `SelfRef.java` interface
- Expression hierarchy updated

### Step 2: Validation Implementation

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add Diagnostic Codes** (after Phase 11 codes):
```java
// Phase 12: Association ON condition validation codes
public static final String CODE_ON_CONDITION_INVALID_REF     = "cds.on.condition.invalid.ref";
public static final String CODE_ON_CONDITION_NO_COMPARISON   = "cds.on.condition.no.comparison";
public static final String CODE_ON_CONDITION_COMPLEX_HINT    = "cds.on.condition.complex.hint";
public static final String CODE_SELF_REF_OUTSIDE_ON          = "cds.self.ref.outside.on";
```

**Add Validation Methods** (add new section after Phase 11):

```java
// ── Phase 12: Association ON condition validation ────────────────────────────

/**
 * Validates ON condition in associations.
 */
@Check(CheckType.FAST)
public void checkOnCondition(AssocDef assoc) {
    if (assoc.getOnCondition() == null) return;

    // Info: ON conditions are good practice
    info("ON condition defines the foreign key relationship - this enables proper OData navigation",
        assoc,
        CdsPackage.Literals.ASSOC_DEF__ON_CONDITION,
        CODE_ON_CONDITION_COMPLEX_HINT);

    // Check if condition contains comparison
    if (!containsComparison(assoc.getOnCondition())) {
        warning("ON condition should typically contain a comparison (=, !=, etc.)",
            assoc,
            CdsPackage.Literals.ASSOC_DEF__ON_CONDITION,
            CODE_ON_CONDITION_NO_COMPARISON);
    }
}

/**
 * Helper: Check if expression contains a comparison operator.
 */
private boolean containsComparison(Expression expr) {
    if (expr instanceof BinaryExpr) {
        BinaryExpr binary = (BinaryExpr) expr;
        String op = binary.getOp();
        if (op != null && (op.equals("=") || op.equals("!=") ||
            op.equals("<") || op.equals("<=") ||
            op.equals(">") || op.equals(">="))) {
            return true;
        }
        // Check left and right recursively
        return (binary.getLeft() != null && containsComparison(binary.getLeft())) ||
               (binary.getRight() != null && containsComparison(binary.getRight()));
    }
    return false;
}

/**
 * Validates $self references are only used in ON conditions.
 */
@Check(CheckType.FAST)
public void checkSelfReference(SelfRef selfRef) {
    // Check if $self is used inside an ON condition
    org.eclipse.emf.ecore.EObject container = selfRef.eContainer();
    boolean inOnCondition = false;

    while (container != null) {
        if (container instanceof AssocDef) {
            AssocDef assoc = (AssocDef) container;
            if (assoc.getOnCondition() != null &&
                isContainedIn(selfRef, assoc.getOnCondition())) {
                inOnCondition = true;
                break;
            }
        }
        container = container.eContainer();
    }

    if (!inOnCondition) {
        error("$self can only be used in association ON conditions",
            selfRef,
            null,
            CODE_SELF_REF_OUTSIDE_ON);
    }
}

/**
 * Helper: Check if node is contained in expression tree.
 */
private boolean isContainedIn(org.eclipse.emf.ecore.EObject node,
                              org.eclipse.emf.ecore.EObject root) {
    org.eclipse.emf.ecore.EObject current = node;
    while (current != null) {
        if (current == root) return true;
        current = current.eContainer();
    }
    return false;
}
```

### Step 3: Testing

**File:** `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java`

**Add Imports:**
```java
import org.example.cds.cDS.BinaryExpr;
import org.example.cds.cDS.SelfRef;
```

**Add Test Methods** (in new Phase 12 section):

```java
// ── Phase 12: Association ON Conditions ──────────────────────────────────────

@Test
public void parseSimpleOnCondition() throws Exception {
    CdsFile file = parse("""
        entity Customers { key ID: UUID; }
        entity Orders {
          key ID: UUID;
          customer_ID: UUID;
          customer: Association to Customers
            on customer.ID = customer_ID;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef orders = (EntityDef) file.getDefinitions().get(1);
    Element customerAssoc = orders.getElements().get(2);
    assertNotNull(customerAssoc.getAssoc());
    assertNotNull(customerAssoc.getAssoc().getOnCondition());
}

@Test
public void parseOnConditionWithSelfReference() throws Exception {
    CdsFile file = parse("""
        entity Customers { key ID: UUID; }
        entity Orders {
          key ID: UUID;
          customer_ID: UUID;
          customer: Association to Customers
            on customer.ID = $self.customer_ID;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef orders = (EntityDef) file.getDefinitions().get(1);
    Element customerAssoc = orders.getElements().get(2);
    assertNotNull(customerAssoc.getAssoc().getOnCondition());
    // Verify $self is used
    assertTrue(containsSelfRef(customerAssoc.getAssoc().getOnCondition()));
}

@Test
public void parseOnConditionWithMultipleConditions() throws Exception {
    CdsFile file = parse("""
        entity Products { key ID: UUID; }
        entity Reviews {
          key ID: UUID;
          product_ID: UUID;
          status: String;
          product: Association to Products
            on product.ID = $self.product_ID
            and product.status = 'active';
        }
        """);
    validationHelper.assertNoErrors(file);
}

@Test
public void parseSelfReference() throws Exception {
    CdsFile file = parse("""
        entity Employees {
          key ID: UUID;
          manager_ID: UUID;
          manager: Association to Employees
            on manager.ID = $self.manager_ID;
        }
        """);
    validationHelper.assertNoErrors(file);
}

@Test
public void parseFilteredAssociation() throws Exception {
    CdsFile file = parse("""
        entity Products { key ID: UUID; }
        entity Reviews {
          key ID: UUID;
          product_ID: UUID;
          status: String;
        }
        entity ProductView {
          key ID: UUID;
          approvedReviews: Association to many Reviews
            on approvedReviews.product_ID = $self.ID
            and approvedReviews.status = 'approved';
        }
        """);
    validationHelper.assertNoErrors(file);
}

@Test
public void parseOnConditionWithComparison() throws Exception {
    CdsFile file = parse("""
        entity Orders { key ID: UUID; orderDate: Date; }
        entity RecentOrders {
          key ID: UUID;
          orders: Association to many Orders
            on orders.orderDate >= $self.cutoffDate;
        }
        """);
    validationHelper.assertNoErrors(file);
}

@Test
public void detectSelfRefOutsideOnCondition() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          price: Decimal = $self.cost * 1.5;
        }
        """);
    validationHelper.assertError(file,
        null,  // SelfRef doesn't have a specific package literal
        CDSValidator.CODE_SELF_REF_OUTSIDE_ON);
}

@Test
public void parseToManyWithOnCondition() throws Exception {
    CdsFile file = parse("""
        entity Authors { key ID: UUID; }
        entity Books {
          key ID: UUID;
          author_ID: UUID;
        }
        entity AuthorView {
          key ID: UUID;
          books: Association to many Books
            on books.author_ID = $self.ID;
        }
        """);
    validationHelper.assertNoErrors(file);
}

@Test
public void parseComplexOnCondition() throws Exception {
    CdsFile file = parse("""
        entity Customers { key ID: UUID; }
        entity Orders {
          key ID: UUID;
          customer_ID: UUID;
          status: String;
          customer: Association to Customers
            on customer.ID = $self.customer_ID
            and $self.status != 'cancelled';
        }
        """);
    validationHelper.assertNoErrors(file);
}

@Test
public void parseOnConditionWithPathExpression() throws Exception {
    CdsFile file = parse("""
        entity Addresses { key ID: UUID; }
        entity Customers {
          key ID: UUID;
          address_ID: UUID;
          address: Association to Addresses
            on address.ID = $self.address_ID;
        }
        """);
    validationHelper.assertNoErrors(file);
}

// Helper method
private boolean containsSelfRef(Expression expr) {
    if (expr instanceof SelfRef) return true;
    if (expr instanceof BinaryExpr) {
        BinaryExpr binary = (BinaryExpr) expr;
        return (binary.getLeft() != null && containsSelfRef(binary.getLeft())) ||
               (binary.getRight() != null && containsSelfRef(binary.getRight()));
    }
    return false;
}
```

### Step 4: Update Samples and Documentation

**File:** `/Users/I546280/cds-eclipse-plugin/samples/bookshop.cds`

Add ON condition examples:

```cds
entity Orders {
  key ID: UUID;
  customer_ID: UUID;
  customer: Association to Customers
    on customer.ID = $self.customer_ID;

  items: Composition of many OrderItems
    on items.order_ID = $self.ID;
}

entity OrderItems {
  key order_ID: UUID;
  key lineNo: Integer;
  book_ID: UUID;

  order: Association to Orders
    on order.ID = $self.order_ID;

  book: Association to Books
    on book.ID = $self.book_ID;
}
```

**File:** Create `/Users/I546280/cds-eclipse-plugin/samples/phase12-test.cds`

```cds
/**
 * Phase 12 Test - Association ON Conditions
 * Tests ON conditions for proper foreign key relationships
 */

namespace test.phase12;

// ─── Test 1: Simple ON condition ─────────────────────────────────────────────

entity Customers {
  key ID: UUID;
  name: String;
}

entity Orders {
  key ID: UUID;
  customer_ID: UUID;
  customer: Association to Customers
    on customer.ID = $self.customer_ID;
}

// ─── Test 2: Self-reference ──────────────────────────────────────────────────

entity Employees {
  key ID: UUID;
  manager_ID: UUID;
  manager: Association to Employees
    on manager.ID = $self.manager_ID;
}

// ─── Test 3: Filtered association ────────────────────────────────────────────

entity Products {
  key ID: UUID;
  name: String;
}

entity Reviews {
  key ID: UUID;
  product_ID: UUID;
  status: String;
}

entity ProductsView {
  key ID: UUID;
  approvedReviews: Association to many Reviews
    on approvedReviews.product_ID = $self.ID
    and approvedReviews.status = 'approved';
}

// ─── Test 4: Complex conditions ──────────────────────────────────────────────

entity Orders2 {
  key ID: UUID;
  customer_ID: UUID;
  status: String;
  customer: Association to Customers
    on customer.ID = $self.customer_ID
    and $self.status != 'cancelled';
}

// ─── Test 5: To-many associations ────────────────────────────────────────────

entity Authors {
  key ID: UUID;
  books: Association to many Books
    on books.author_ID = $self.ID;
}

entity Books {
  key ID: UUID;
  author_ID: UUID;
}

// ─── Test 6: Composition with ON condition ───────────────────────────────────

entity Orders3 {
  key ID: UUID;
  items: Composition of many Items
    on items.order_ID = $self.ID;
}

entity Items {
  key ID: UUID;
  order_ID: UUID;
}

// ─── Test 7: Multiple foreign keys ───────────────────────────────────────────

entity OrderDetails {
  key ID: UUID;
  order_ID: UUID;
  product_ID: UUID;

  order: Association to Orders
    on order.ID = $self.order_ID;

  product: Association to Products
    on product.ID = $self.product_ID;
}
```

**File:** `/Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md`

Update Phase 12 section:
```markdown
#### Phase 12: Association ON Conditions (Complete)
- ✅ ON condition syntax
- ✅ Comparison operators (=, !=, <, <=, >, >=)
- ✅ Logical operators (and, or)
- ✅ $self reference
- ✅ Filtered associations
- ✅ Complex conditions
```

## Critical Files

### Files to Modify:
1. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext`
   - Add `('on' onCondition=Expression)?` to AssocDef
   - Extend Expression hierarchy with comparison/logical operators
   - Add $self keyword to PrimaryExpr

2. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`
   - Add 4 diagnostic codes
   - Add 3 validation/helper methods

3. `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java`
   - Add 10 test methods
   - Add 1 helper method

4. `/Users/I546280/cds-eclipse-plugin/samples/bookshop.cds`
   - Add ON condition examples

5. `/Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md`
   - Update Phase 12 to "Complete"

### Generated Files (Auto-updated by Xtext):
- `AssocDef.java` (updated with getOnCondition())
- `ComparisonOp.java` (enum)
- `SelfRef.java` (interface)
- `BinaryExpr.java` (updated for comparison ops)
- Expression hierarchy updated
- Parser and lexer artifacts

## Edge Cases and Considerations

### 1. Simple Foreign Key
```cds
customer: Association to Customers
  on customer.ID = $self.customer_ID;
```
**Handled:** Basic case

### 2. Self-Reference
```cds
manager: Association to Employees
  on manager.ID = $self.manager_ID;
```
**Handled:** $self resolves to current entity

### 3. Filtered Association
```cds
approvedReviews: Association to many Reviews
  on approvedReviews.product_ID = $self.ID
  and approvedReviews.status = 'approved';
```
**Handled:** Multiple conditions with and

### 4. Comparison Operators
```cds
recentOrders: Association to many Orders
  on recentOrders.date >= $self.cutoffDate;
```
**Handled:** Full set of comparison operators

### 5. $self Outside ON Condition
```cds
price: Decimal = $self.cost * 1.5;  // Error
```
**Handled:** Validation error

### 6. Complex Nested Conditions
```cds
on customer.ID = $self.customer_ID
and ($self.status = 'active' or $self.status = 'pending');
```
**Handled:** Expression hierarchy supports nesting

## Backward Compatibility

### Breaking Changes: MINIMAL

**Potential Breaking:**
- Expression grammar extends to add comparison/logical operators
- This could affect existing calculated fields using = (unlikely in Phase 5)

**Mitigation:**
- Existing expressions continue to work
- = in calculated fields would need disambiguation (rare)

**Non-Breaking:**
- ON condition is optional: `('on' ...)?`
- Existing associations without ON continue to work
- $self is a new keyword in limited context

## Verification Steps

### Build Verification:
1. Regenerate grammar: Run MWE2 workflow
2. Build succeeds: `mvn clean package -DskipTests`
3. Check AssocDef.getOnCondition() exists
4. Check ComparisonOp enum exists
5. Check SelfRef interface exists

### Parsing Verification:
1. Parse simple ON: `on customer.ID = customer_ID`
2. Parse with $self: `on customer.ID = $self.customer_ID`
3. Parse with and: `on ... and ...`
4. Parse with comparisons: `>=`, `!=`, etc.

### Validation Verification:
1. $self outside ON → error
2. ON condition without comparison → warning
3. Complex ON → info about OData navigation

## Success Criteria

- ✅ Parse ON condition syntax
- ✅ Parse $self reference
- ✅ Parse comparison operators (=, !=, <, <=, >, >=)
- ✅ Parse logical operators (and, or)
- ✅ Validate $self usage
- ✅ Support filtered associations
- ✅ All tests pass
- ✅ Documentation updated
- ✅ Minimal breaking changes

## Estimated Effort

**Implementation:** 3-4 hours
- Grammar changes: 60 minutes (expression hierarchy extension)
- Validation logic: 45 minutes
- Testing: 75 minutes
- Documentation: 30 minutes

**Testing & Verification:** 30 minutes
**Total:** ~4.5 hours

This completes Phase 12 (Association ON Conditions) and brings coverage to ~54%, with proper foreign key relationship support - a CRITICAL feature for production SAP CAP applications.

## Impact Assessment

**Before Phase 12:**
- ❌ Cannot model proper foreign keys
- ❌ No OData navigation support
- ❌ Cannot filter associations
- ❌ Relational integrity not enforceable

**After Phase 12:**
- ✅ Proper foreign key relationships
- ✅ OData navigation enabled
- ✅ Filtered associations supported
- ✅ Relational integrity enforceable
- ✅ Standard SAP CAP pattern supported

**Coverage Impact:** 49% → 54% (+5%)
**Production Readiness:** Significantly improved - removes a critical blocker
