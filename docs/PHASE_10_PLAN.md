# Implementation Plan: Phase 10 - Virtual Elements

## Context

Phase 9 (Data Constraints) is complete and implemented ~45% of the SAP CAP CDS specification. Phase 10 adds virtual (transient) elements to reach ~47% coverage.

**Current State:**
- ✅ Phase 9: Data constraints (not null, unique, check, default) fully implemented
- ✅ ElementModifier enum exists with KEY modifier
- ✅ Grammar supports: `key ID: UUID;`
- ❌ Missing: `virtual` modifier for transient/computed fields

**Why This Matters:**
- Virtual elements are computed at runtime and not persisted to the database
- Essential for exposing calculated data without storing it
- Common in SAP CAP for UI annotations, computed values, and API responses
- Without them, developers cannot model transient fields properly

**Target SAP CAP CDS Syntax:**
```cds
entity Books {
  key ID: UUID;
  title: String;
  author: String;

  // Virtual fields - not persisted to database
  virtual fullName: String;
  virtual averageRating: Decimal;
}

entity Products {
  price: Decimal;
  tax: Decimal;

  // Virtual calculated field
  virtual totalPrice: Decimal;

  // Can also have default expressions
  virtual status: String = 'Available';
}
```

## Recommended Approach: Extend ElementModifier Enum

### Architecture Decision

After examining the codebase, I recommend **extending the ElementModifier enum** because:

1. **Consistent with Phase 8:**
   - `key` modifier already uses ElementModifier enum
   - Virtual is a modifier like key, not a constraint
   - Same parsing pattern: modifier before element name

2. **Simple Implementation:**
   - Single enum value addition
   - No new AST nodes needed
   - Minimal grammar changes

3. **Mutually Exclusive:**
   - An element cannot be both `key` and `virtual`
   - Enum enforces single modifier per element
   - Clean validation logic

4. **SAP CAP Semantics:**
   - Virtual is a storage modifier (like key defines identity)
   - Not a data constraint (like not null, unique)
   - Fits the ElementModifier abstraction

### Grammar Structure

```xtext
// Extend existing enum (line 135)
enum ElementModifier:
    KEY='key' | VIRTUAL='virtual';

// Element rule unchanged (line 117)
Element:
    annotations+=Annotation*
    (modifier=ElementModifier)?  // Can be 'key' or 'virtual'
    name=ID ':'
    (type=TypeRef | assoc=AssocDef)
    constraints+=Constraint*
    ('=' defaultValue=Expression)?
    ';';
```

**Key Points:**
- Only change: Add `| VIRTUAL='virtual'` to existing enum
- No changes to Element rule needed
- Element.getModifier() returns ElementModifier enum (KEY or VIRTUAL)
- Natural validation: cannot have both key and virtual

## Implementation Steps

### Step 1: Grammar Extension

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext`

**Location:** Line 135 (ElementModifier enum)

**Change:**
```xtext
enum ElementModifier:
    KEY='key' | VIRTUAL='virtual';
```

**Auto-Generated:**
- `ElementModifier.java` updated with VIRTUAL enum value
- No other AST changes needed (Element already has getModifier())

### Step 2: Validation Implementation

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add Diagnostic Codes** (after line 77):
```java
// Phase 10: Virtual element validation codes
public static final String CODE_VIRTUAL_ON_ASSOCIATION   = "cds.virtual.on.association";
public static final String CODE_VIRTUAL_WITHOUT_TYPE     = "cds.virtual.without.type";
public static final String CODE_VIRTUAL_WITH_KEY         = "cds.virtual.with.key";
public static final String CODE_VIRTUAL_PERSISTED_HINT   = "cds.virtual.persisted.hint";
```

**Add Validation Methods** (add new section after Phase 9):

```java
// ── Phase 10: Virtual element validation ─────────────────────────────────

/**
 * Validates virtual element properties.
 * Virtual elements are transient and not persisted to the database.
 */
@Check(CheckType.FAST)
public void checkVirtualElement(Element element) {
    if (element.getModifier() != ElementModifier.VIRTUAL) return;

    // Error: virtual on association
    if (element.getAssoc() != null) {
        error("virtual modifier cannot be used on associations - associations are inherently non-persisted references",
            element,
            CdsPackage.Literals.ELEMENT__MODIFIER,
            CODE_VIRTUAL_ON_ASSOCIATION);
        return;
    }

    // Error: virtual without type
    if (element.getType() == null) {
        error("virtual element must have a type",
            element,
            CdsPackage.Literals.ELEMENT__MODIFIER,
            CODE_VIRTUAL_WITHOUT_TYPE);
    }

    // Info: Suggest when to use virtual
    if (element.getDefaultValue() == null) {
        info("Virtual elements are typically computed at runtime - consider adding a default expression or computing in application logic",
            element,
            CdsPackage.Literals.ELEMENT__MODIFIER,
            CODE_VIRTUAL_PERSISTED_HINT);
    }
}

/**
 * Validates that virtual elements don't have not null constraints.
 * Virtual elements are computed, so not null doesn't make sense.
 */
@Check(CheckType.FAST)
public void checkVirtualConstraints(Element element) {
    if (element.getModifier() != ElementModifier.VIRTUAL) return;

    // Warning: not null on virtual element
    boolean hasNotNull = element.getConstraints().stream()
        .anyMatch(c -> c instanceof NotNullConstraint);

    if (hasNotNull) {
        warning("not null constraint on virtual elements may not be enforced - virtual elements are computed at runtime",
            element,
            CdsPackage.Literals.ELEMENT__NAME,
            CODE_VIRTUAL_PERSISTED_HINT);
    }

    // Info: unique on virtual element
    boolean hasUnique = element.getConstraints().stream()
        .anyMatch(c -> c instanceof UniqueConstraint);

    if (hasUnique) {
        info("unique constraint on virtual elements is unusual - virtual elements are not persisted",
            element,
            CdsPackage.Literals.ELEMENT__NAME,
            CODE_VIRTUAL_PERSISTED_HINT);
    }
}
```

### Step 3: Testing

**File:** `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java`

**Add Test Methods** (in new Phase 10 section):

```java
// ── Phase 10: Virtual Elements ───────────────────────────────────────────

@Test
public void parseVirtualElement() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          price: Decimal;
          virtual totalPrice: Decimal;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    Element virtualElement = entity.getElements().get(2);
    assertEquals(ElementModifier.VIRTUAL, virtualElement.getModifier());
    assertEquals("totalPrice", virtualElement.getName());
}

@Test
public void parseVirtualWithExpression() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          price: Decimal;
          tax: Decimal;
          virtual totalPrice: Decimal = price + tax;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    Element virtualElement = entity.getElements().get(3);
    assertEquals(ElementModifier.VIRTUAL, virtualElement.getModifier());
    assertNotNull(virtualElement.getDefaultValue());
}

@Test
public void parseMultipleVirtualElements() throws Exception {
    CdsFile file = parse("""
        entity Users {
          key ID: UUID;
          firstName: String;
          lastName: String;
          virtual fullName: String;
          virtual displayName: String;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    long virtualCount = entity.getElements().stream()
        .filter(e -> e.getModifier() == ElementModifier.VIRTUAL)
        .count();
    assertEquals(2, virtualCount);
}

@Test
public void detectVirtualOnAssociation() throws Exception {
    CdsFile file = parse("""
        entity Books { key ID: UUID; }
        entity Orders {
          key ID: UUID;
          virtual book: Association to Books;
        }
        """);
    validationHelper.assertError(file,
        CdsPackage.Literals.ELEMENT,
        CDSValidator.CODE_VIRTUAL_ON_ASSOCIATION);
}

@Test
public void detectVirtualWithoutType() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          virtual computed;
        }
        """);
    validationHelper.assertError(file,
        CdsPackage.Literals.ELEMENT,
        CDSValidator.CODE_VIRTUAL_WITHOUT_TYPE);
}

@Test
public void parseVirtualWithConstraints() throws Exception {
    CdsFile file = parse("""
        entity Users {
          key ID: UUID;
          virtual fullName: String not null;
        }
        """);
    // Should parse but produce warning
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    Element virtualElement = entity.getElements().get(1);
    assertEquals(ElementModifier.VIRTUAL, virtualElement.getModifier());
    assertTrue(virtualElement.getConstraints().stream()
        .anyMatch(c -> c instanceof NotNullConstraint));
}

@Test
public void cannotBeKeyAndVirtual() throws Exception {
    // This should fail to parse because modifier can only be one value
    CdsFile file = parseHelper.parse("""
        entity Products {
          key virtual ID: UUID;
        }
        """);
    assertNotNull(file);
    assertFalse(file.eResource().getErrors().isEmpty(),
        "Should have parse errors");
}

@Test
public void parseVirtualWithDefault() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          virtual status: String default 'Available';
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    Element virtualElement = entity.getElements().get(1);
    assertEquals(ElementModifier.VIRTUAL, virtualElement.getModifier());
    assertNotNull(virtualElement.getDefaultValue());
}

@Test
public void parseVirtualInService() throws Exception {
    CdsFile file = parse("""
        entity Books {
          key ID: UUID;
          title: String;
          virtual rating: Decimal;
        }

        service CatalogService {
          entity Books as projection on Books {
            ID, title, rating
          }
        }
        """);
    validationHelper.assertNoErrors(file);
}
```

### Step 4: Update Samples and Documentation

**File:** `/Users/I546280/cds-eclipse-plugin/samples/bookshop.cds`

Add virtual element examples:
```cds
@UI.HeaderInfo: { TypeName: 'Book', TypeNamePlural: 'Books' }
entity Books : Managed {
  key ID    : UUID not null;
  title     : String(111) not null;
  descr     : String(1111);
  author    : Association to Authors;
  genre     : Association to Genres;
  stock     : Integer check stock >= 0;
  price     : Amount not null check price > 0;
  currency  : Currency default 'USD';
  grossPrice: Amount;
  tax       : Amount;
  netPrice  : Amount = grossPrice - tax;
  status    : BookStatus default #Available not null;

  // Phase 10: Virtual elements
  virtual averageRating: Decimal;
  virtual reviewCount: Integer;
  virtual isAvailable: Boolean = stock > 0;
}
```

**File:** Create `/Users/I546280/cds-eclipse-plugin/samples/phase10-test.cds`

```cds
/**
 * Phase 10 Test - Virtual Elements
 * Tests virtual modifier for transient/computed fields
 */

namespace test.phase10;

// ─── Test 1: Simple virtual element ──────────────────────────────────────────

entity SimpleVirtual {
  key ID: UUID;
  name: String;
  virtual displayName: String;
}

// ─── Test 2: Virtual with expression ─────────────────────────────────────────

entity VirtualWithExpression {
  key ID: UUID;
  price: Decimal;
  tax: Decimal;
  virtual totalPrice: Decimal = price + tax;
}

// ─── Test 3: Multiple virtual elements ──────────────────────────────────────

entity MultipleVirtual {
  key ID: UUID;
  firstName: String;
  lastName: String;
  email: String;

  virtual fullName: String;
  virtual displayName: String;
  virtual initials: String;
}

// ─── Test 4: Virtual with default value ─────────────────────────────────────

entity VirtualWithDefault {
  key ID: UUID;
  stock: Integer;
  virtual status: String default 'Available';
  virtual isAvailable: Boolean = stock > 0;
}

// ─── Test 5: Virtual in entity with constraints ─────────────────────────────

entity VirtualWithConstraints {
  key ID: UUID not null;
  name: String not null;
  price: Decimal check price > 0;

  virtual displayPrice: String;
  virtual isValid: Boolean;
}

// ─── Test 6: Virtual elements in service projection ─────────────────────────

entity Products {
  key ID: UUID;
  name: String;
  price: Decimal;
  virtual rating: Decimal;
}

service CatalogService {
  entity Products as projection on Products {
    ID, name, price, rating
  }
}

// ─── Test 7: Complex virtual calculations ───────────────────────────────────

entity Orders {
  key ID: UUID;
  subtotal: Decimal;
  taxRate: Decimal;
  discount: Decimal;

  virtual taxAmount: Decimal = subtotal * taxRate;
  virtual discountAmount: Decimal = subtotal * discount;
  virtual total: Decimal = subtotal + taxAmount - discountAmount;
}
```

**File:** `/Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md`

Update Phase 10 section:
```markdown
#### Phase 10: Virtual Elements (Complete)
- ✅ `virtual` modifier syntax
- ✅ Virtual elements with types
- ✅ Virtual elements with expressions
- ✅ Virtual element validation
- ✅ Compatible with constraints
- ✅ Service projection support
```

## Critical Files

### Files to Modify:
1. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext`
   - Add `| VIRTUAL='virtual'` to ElementModifier enum (line 135)

2. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`
   - Add 4 diagnostic codes
   - Add 2 validation methods

3. `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java`
   - Add 10 test methods

4. `/Users/I546280/cds-eclipse-plugin/samples/bookshop.cds`
   - Add virtual element examples

5. `/Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md`
   - Update Phase 10 to "Complete"

### Generated Files (Auto-updated by Xtext):
- `ElementModifier.java` (enum updated with VIRTUAL value)
- Parser and lexer artifacts

## Edge Cases and Considerations

### 1. Virtual with Key (Mutually Exclusive)
```cds
key virtual ID: UUID;  // Parse error - modifier can only be one value
```
**Handled:** Enum enforces single modifier

### 2. Virtual with Constraints
```cds
virtual fullName: String not null;  // Warning: not null may not be enforced
virtual email: String unique;        // Info: unique is unusual
```
**Handled:** Validation warns about constraint enforcement

### 3. Virtual with Expression
```cds
virtual totalPrice: Decimal = price + tax;  // Allowed
```
**Handled:** defaultValue expression is optional

### 4. Virtual without Expression
```cds
virtual rating: Decimal;  // Info: typically computed at runtime
```
**Handled:** Info message suggests adding expression

### 5. Virtual in Service Projections
```cds
entity Books {
  virtual rating: Decimal;
}

service CatalogService {
  entity Books as projection on Books { rating }  // Works
}
```
**Handled:** No special logic needed, works naturally

### 6. Virtual on Association
```cds
virtual customer: Association to Customers;  // Error: associations are already virtual
```
**Handled:** Validation error - associations don't need virtual

### 7. Virtual with Default Value
```cds
virtual status: String default 'Active';  // Allowed
```
**Handled:** Works like regular default values

## Backward Compatibility

### Breaking Changes: NONE

**Non-Breaking:**
- Adding VIRTUAL to ElementModifier enum is additive
- Existing code continues to work
- New keyword `virtual` only in element position

### Migration Path:

No migration needed - this is purely additive functionality.

## Verification Steps

### Build Verification:
1. Regenerate grammar: Run MWE2 workflow
2. Build succeeds: `mvn clean package -DskipTests`
3. Check ElementModifier.VIRTUAL exists
4. Verify Element.getModifier() can return VIRTUAL

### Parsing Verification:
1. Parse `virtual displayName: String;` - should have VIRTUAL modifier
2. Parse `virtual total: Decimal = price + tax;` - should have both modifier and expression
3. Parse `key virtual ID: UUID;` - should fail (parse error)

### Validation Verification:
1. virtual on association → error
2. virtual without type → error
3. virtual with not null → warning
4. virtual without expression → info

### Integration Verification:
1. Update bookshop.cds with virtual elements
2. Verify no parse errors
3. Verify validation messages appear correctly

## Success Criteria

- ✅ Parse `virtual` modifier
- ✅ Parse virtual elements with expressions
- ✅ Parse virtual elements without expressions
- ✅ Validate virtual element usage (errors on associations, etc.)
- ✅ Support virtual in service projections
- ✅ All tests pass
- ✅ Documentation updated
- ✅ Zero breaking changes

## Estimated Effort

**Implementation:** 2-3 hours
- Grammar changes: 15 minutes (single line)
- Validation logic: 45 minutes
- Testing: 60 minutes
- Documentation: 30 minutes

**Testing & Verification:** 30 minutes
**Total:** ~3 hours

This completes Phase 10 (Virtual Elements) and brings coverage to ~47%, with full support for transient/computed fields.

## Comparison with Phase 8 (Keys)

Phase 8 added KEY modifier:
```cds
key ID: UUID;
```

Phase 10 adds VIRTUAL modifier:
```cds
virtual rating: Decimal;
```

Both use the same pattern:
- Extend ElementModifier enum
- Add validation for proper usage
- No new AST nodes required
- Simple, clean implementation

This validates the ElementModifier design - it's extensible and maintainable.
