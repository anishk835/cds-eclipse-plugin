# Implementation Plan: Phase 11 - Localized Data

## Context

Phase 10 (Virtual Elements) is complete and implemented ~47% of the SAP CAP CDS specification. Phase 11 adds localized data support to reach ~49% coverage.

**Current State:**
- ✅ Phase 10: Virtual elements fully implemented
- ✅ ElementModifier enum exists with KEY and VIRTUAL
- ✅ Grammar supports: `key ID: UUID;` and `virtual rating: Decimal;`
- ❌ Missing: `localized` keyword for internationalization

**Why This Matters:**
- Localized data is essential for multilingual SAP CAP applications
- Standard pattern for global enterprise applications
- Required for proper i18n support in Fiori
- SAP CAP automatically generates text tables for localized fields
- Common requirement: product names, descriptions in multiple languages

**Target SAP CAP CDS Syntax:**
```cds
entity Products {
  key ID: UUID;
  name: localized String(100);
  description: localized String(1000);
  price: Decimal;
}

// SAP CAP generates:
entity Products {
  ID: UUID;
  price: Decimal;
}

entity Products.texts {
  ID_ID: UUID;
  locale: String(14);
  name: String(100);
  description: String(1000);
}
```

## Recommended Approach: Extend ElementModifier Enum

### Architecture Decision

After examining Phases 8 and 10, I recommend **extending the ElementModifier enum** because:

1. **Consistent with Phases 8 & 10:**
   - Phase 8: Added `KEY` modifier
   - Phase 10: Added `VIRTUAL` modifier
   - Phase 11: Add `LOCALIZED` modifier
   - Same pattern, proven approach

2. **Localized is a Type Modifier:**
   - Modifies how the field is stored (separate text table)
   - Similar to virtual (changes storage behavior)
   - Not a constraint (like not null, unique)
   - Not a cardinality (like to one, of many)

3. **Simple Implementation:**
   - Single enum value addition
   - No new AST nodes needed
   - Minimal grammar changes

4. **SAP CAP Semantics:**
   - `localized` is a storage/type modifier
   - Affects code generation (creates text tables)
   - Changes how data is queried (locale-aware)

### Grammar Structure

```xtext
// Extend existing enum (line 136)
enum ElementModifier:
    KEY='key' | VIRTUAL='virtual' | LOCALIZED='localized';

// Element rule unchanged (line 117)
Element:
    annotations+=Annotation*
    (modifier=ElementModifier)?
    name=ID ':'
    (type=TypeRef | assoc=AssocDef)
    constraints+=Constraint*
    ('=' defaultValue=Expression)?
    ';';
```

**Key Points:**
- Only change: Add `| LOCALIZED='localized'` to existing enum
- No changes to Element rule needed
- Element.getModifier() returns ElementModifier enum (KEY, VIRTUAL, or LOCALIZED)
- Natural validation: cannot have multiple modifiers

### Mutual Exclusivity

Elements can be:
- ✅ Regular: `name: String;`
- ✅ Key: `key ID: UUID;`
- ✅ Virtual: `virtual rating: Decimal;`
- ✅ Localized: `localized name: String;`
- ❌ Multiple: `key localized ID: String;` - PARSE ERROR (enum enforces)

## Implementation Steps

### Step 1: Grammar Extension

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext`

**Location:** Line 136 (ElementModifier enum)

**Change:**
```xtext
enum ElementModifier:
    KEY='key' | VIRTUAL='virtual' | LOCALIZED='localized';
```

**Auto-Generated:**
- `ElementModifier.java` updated with LOCALIZED enum value
- No other AST changes needed (Element already has getModifier())

### Step 2: Validation Implementation

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add Diagnostic Codes** (after Phase 10 codes):
```java
// Phase 11: Localized data validation codes
public static final String CODE_LOCALIZED_ON_ASSOCIATION   = "cds.localized.on.association";
public static final String CODE_LOCALIZED_ON_NON_STRING    = "cds.localized.on.non.string";
public static final String CODE_LOCALIZED_ON_KEY           = "cds.localized.on.key";
public static final String CODE_LOCALIZED_HINT             = "cds.localized.hint";
```

**Add Validation Methods** (add new section after Phase 10):

```java
// ── Phase 11: Localized data validation ──────────────────────────────────

/**
 * Validates localized element properties.
 * Localized elements are translated fields stored in separate text tables.
 */
@Check(CheckType.FAST)
public void checkLocalizedElement(Element element) {
    if (element.getModifier() != ElementModifier.LOCALIZED) return;

    // Error: localized on association
    if (element.getAssoc() != null) {
        error("localized modifier cannot be used on associations - only regular fields can be localized",
            element,
            CdsPackage.Literals.ELEMENT__MODIFIER,
            CODE_LOCALIZED_ON_ASSOCIATION);
        return;
    }

    // Error: localized without type
    if (element.getType() == null) {
        error("localized element must have a type",
            element,
            CdsPackage.Literals.ELEMENT__MODIFIER,
            CODE_LOCALIZED_ON_NON_STRING);
        return;
    }

    // Warning: localized on non-String types
    if (element.getType() != null && element.getType().getRef() != null) {
        String typeName = element.getType().getRef().getName();
        if (typeName != null &&
            !typeName.equals("String") &&
            !typeName.equals("LargeString")) {
            warning("localized is typically used with String or LargeString types - " +
                    "using it with '" + typeName + "' is unusual",
                element,
                CdsPackage.Literals.ELEMENT__MODIFIER,
                CODE_LOCALIZED_ON_NON_STRING);
        }
    }

    // Info: How localized works
    info("Localized fields are stored in separate text tables - " +
         "SAP CAP will generate a .texts entity for translations",
        element,
        CdsPackage.Literals.ELEMENT__MODIFIER,
        CODE_LOCALIZED_HINT);
}

/**
 * Validates that key elements are not localized.
 * Keys must have consistent values across locales.
 */
@Check(CheckType.FAST)
public void checkKeyNotLocalized(Element element) {
    if (element.getModifier() != ElementModifier.KEY) return;

    // This validation is implicitly handled by enum mutual exclusivity
    // An element cannot be both KEY and LOCALIZED because modifier is single-valued
    // But we keep this as documentation of the business rule
}
```

### Step 3: Testing

**File:** `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java`

**Add Test Methods** (in new Phase 11 section):

```java
// ── Phase 11: Localized Data ─────────────────────────────────────────────

@Test
public void parseLocalizedElement() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          localized name: String(100);
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    Element nameElement = entity.getElements().get(1);
    assertEquals(ElementModifier.LOCALIZED, nameElement.getModifier());
    assertEquals("name", nameElement.getName());
}

@Test
public void parseMultipleLocalizedElements() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          localized name: String(100);
          localized description: String(1000);
          price: Decimal;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    long localizedCount = entity.getElements().stream()
        .filter(e -> e.getModifier() == ElementModifier.LOCALIZED)
        .count();
    assertEquals(2, localizedCount);
}

@Test
public void parseLocalizedWithConstraints() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          localized name: String(100) not null;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    Element nameElement = entity.getElements().get(1);
    assertEquals(ElementModifier.LOCALIZED, nameElement.getModifier());
    assertTrue(nameElement.getConstraints().stream()
        .anyMatch(c -> c instanceof NotNullConstraint));
}

@Test
public void detectLocalizedOnAssociation() throws Exception {
    CdsFile file = parse("""
        entity Categories { key ID: UUID; }
        entity Products {
          key ID: UUID;
          localized category: Association to Categories;
        }
        """);
    validationHelper.assertError(file,
        CdsPackage.Literals.ELEMENT,
        CDSValidator.CODE_LOCALIZED_ON_ASSOCIATION);
}

@Test
public void warnLocalizedOnNonString() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          localized price: Decimal;
        }
        """);
    validationHelper.assertWarning(file,
        CdsPackage.Literals.ELEMENT,
        CDSValidator.CODE_LOCALIZED_ON_NON_STRING);
}

@Test
public void cannotBeKeyAndLocalized() throws Exception {
    // This should fail to parse because modifier can only be one value
    CdsFile file = parseHelper.parse("""
        entity Products {
          key localized name: String;
        }
        """);
    assertNotNull(file);
    assertFalse(file.eResource().getErrors().isEmpty(),
        "Should have parse errors");
}

@Test
public void cannotBeVirtualAndLocalized() throws Exception {
    // This should fail to parse because modifier can only be one value
    CdsFile file = parseHelper.parse("""
        entity Products {
          virtual localized name: String;
        }
        """);
    assertNotNull(file);
    assertFalse(file.eResource().getErrors().isEmpty(),
        "Should have parse errors");
}

@Test
public void parseLocalizedInService() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          localized name: String;
          price: Decimal;
        }

        service CatalogService {
          entity Products as projection on Products {
            ID, name, price
          }
        }
        """);
    validationHelper.assertNoErrors(file);
}

@Test
public void parseLocalizedWithDefault() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          localized name: String default 'Unknown';
        }
        """);
    // Default values on localized fields are allowed but unusual
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    Element nameElement = entity.getElements().get(1);
    assertEquals(ElementModifier.LOCALIZED, nameElement.getModifier());
    assertNotNull(nameElement.getDefaultValue());
}

@Test
public void parseRealWorldLocalizedExample() throws Exception {
    CdsFile file = parse("""
        entity Books {
          key ID: UUID;
          localized title: String(200) not null;
          localized description: LargeString;
          author: String(100);
          price: Decimal(9,2);
          isbn: String(13) unique;
        }

        service CatalogService {
          entity Books as projection on Books {
            ID, title, description, author, price
          }
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);

    // Verify title is localized
    Element titleElement = entity.getElements().get(1);
    assertEquals("title", titleElement.getName());
    assertEquals(ElementModifier.LOCALIZED, titleElement.getModifier());

    // Verify description is localized
    Element descElement = entity.getElements().get(2);
    assertEquals("description", descElement.getName());
    assertEquals(ElementModifier.LOCALIZED, descElement.getModifier());

    // Verify author is NOT localized
    Element authorElement = entity.getElements().get(3);
    assertEquals("author", authorElement.getName());
    assertNull(authorElement.getModifier());
}
```

### Step 4: Update Samples and Documentation

**File:** `/Users/I546280/cds-eclipse-plugin/samples/bookshop.cds`

Add localized element examples:
```cds
@UI.HeaderInfo: { TypeName: 'Book', TypeNamePlural: 'Books' }
entity Books : Managed {
  key ID    : UUID not null;
  localized title     : String(111) not null;
  localized descr     : String(1111);
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

**File:** Create `/Users/I546280/cds-eclipse-plugin/samples/phase11-test.cds`

```cds
/**
 * Phase 11 Test - Localized Data
 * Tests localized modifier for multilingual fields
 */

namespace test.phase11;

// ─── Test 1: Simple localized element ────────────────────────────────────────

entity SimpleLocalized {
  key ID: UUID;
  localized name: String(100);
}

// ─── Test 2: Multiple localized elements ─────────────────────────────────────

entity MultipleLocalized {
  key ID: UUID;
  localized name: String(100);
  localized description: String(1000);
  price: Decimal;
}

// ─── Test 3: Localized with constraints ──────────────────────────────────────

entity LocalizedWithConstraints {
  key ID: UUID;
  localized name: String(100) not null;
  localized description: String(1000);
  localized title: String(200) not null unique;
}

// ─── Test 4: Real-world product catalog ──────────────────────────────────────

entity Products {
  key ID: UUID not null;
  localized name: String(200) not null;
  localized description: LargeString;
  localized shortDesc: String(500);

  // Non-localized fields
  price: Decimal(9,2) not null;
  currency: String(3) default 'USD';
  sku: String(50) unique;
  stock: Integer default 0;

  virtual displayName: String;
}

// ─── Test 5: Books with localized content ────────────────────────────────────

entity Books {
  key ID: UUID;
  localized title: String(200) not null;
  localized description: LargeString;

  author: String(100);
  isbn: String(13) unique;
  price: Decimal(9,2);

  virtual isAvailable: Boolean;
}

// ─── Test 6: Categories with localized names ─────────────────────────────────

entity Categories {
  key ID: UUID;
  localized name: String(100) not null;
  localized description: String(500);

  parent: Association to Categories;
}

// ─── Test 7: Localized in service projection ─────────────────────────────────

service CatalogService {
  entity Products as projection on Products {
    ID, name, description, price
  }

  entity Books as projection on Books {
    ID, title, author, price
  }
}

// ─── Test 8: Complex multilingual application ────────────────────────────────

type Status : String enum {
  Active;
  Inactive;
}

entity Articles {
  key ID: UUID not null;

  // Localized content
  localized headline: String(200) not null;
  localized teaser: String(500);
  localized body: LargeString;

  // Metadata (not localized)
  author: String(100);
  publishedAt: DateTime;
  status: Status default #Active;

  // Virtual fields
  virtual wordCount: Integer;
  virtual readingTime: Integer;
}
```

**File:** `/Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md`

Update Phase 11 section:
```markdown
#### Phase 11: Localized Data (Complete)
- ✅ `localized` modifier syntax
- ✅ Localized String and LargeString fields
- ✅ Multiple localized elements per entity
- ✅ Localized element validation
- ✅ Compatible with constraints
- ✅ Service projection support
```

## Critical Files

### Files to Modify:
1. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext`
   - Add `| LOCALIZED='localized'` to ElementModifier enum (line 136)

2. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`
   - Add 4 diagnostic codes
   - Add 2 validation methods

3. `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java`
   - Add 10 test methods

4. `/Users/I546280/cds-eclipse-plugin/samples/bookshop.cds`
   - Add localized element examples

5. `/Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md`
   - Update Phase 11 to "Complete"

### Generated Files (Auto-updated by Xtext):
- `ElementModifier.java` (enum updated with LOCALIZED value)
- Parser and lexer artifacts

## Edge Cases and Considerations

### 1. Localized with Key (Mutually Exclusive)
```cds
key localized name: String;  // Parse error - modifier can only be one value
```
**Handled:** Enum enforces single modifier

### 2. Localized with Virtual (Mutually Exclusive)
```cds
virtual localized name: String;  // Parse error
```
**Handled:** Enum enforces single modifier

### 3. Localized with Constraints
```cds
localized name: String not null;  // Allowed
localized title: String unique;   // Allowed but unusual
```
**Handled:** Constraints work on localized fields

### 4. Localized on Non-String Types
```cds
localized price: Decimal;  // Warning: unusual
```
**Handled:** Validation warns about non-String types

### 5. Localized on Association
```cds
localized category: Association to Categories;  // Error
```
**Handled:** Validation error - associations can't be localized

### 6. Localized with Default Value
```cds
localized name: String default 'Unknown';  // Allowed but unusual
```
**Handled:** Works but may not make sense (different default per locale?)

### 7. Localized in Service Projections
```cds
service CatalogService {
  entity Products as projection on Products {
    name  // Localized field included
  }
}
```
**Handled:** Works naturally, no special handling needed

## SAP CAP Generated Structure

When you use `localized`, SAP CAP generates:

**Input:**
```cds
entity Products {
  key ID: UUID;
  localized name: String(100);
  price: Decimal;
}
```

**Generated by SAP CAP (conceptual):**
```cds
entity Products {
  key ID: UUID;
  price: Decimal;
}

entity Products.texts {
  key ID_ID: UUID;
  key locale: String(14);
  name: String(100);
}
```

**Note:** Our plugin doesn't generate the text tables - that's done by SAP CAP runtime. We only parse and validate the `localized` keyword.

## Backward Compatibility

### Breaking Changes: NONE

**Non-Breaking:**
- Adding LOCALIZED to ElementModifier enum is additive
- Existing code continues to work
- New keyword `localized` only in element position

### Migration Path:

No migration needed - this is purely additive functionality.

## Verification Steps

### Build Verification:
1. Regenerate grammar: Run MWE2 workflow
2. Build succeeds: `mvn clean package -DskipTests`
3. Check ElementModifier.LOCALIZED exists
4. Verify Element.getModifier() can return LOCALIZED

### Parsing Verification:
1. Parse `localized name: String;` - should have LOCALIZED modifier
2. Parse multiple localized fields
3. Parse `key localized name: String;` - should fail (parse error)
4. Parse `virtual localized name: String;` - should fail (parse error)

### Validation Verification:
1. localized on association → error
2. localized on Decimal → warning
3. localized on String → info about text tables

### Integration Verification:
1. Update bookshop.cds with localized elements
2. Verify no parse errors
3. Verify validation messages appear correctly

## Success Criteria

- ✅ Parse `localized` modifier
- ✅ Parse localized elements with constraints
- ✅ Parse multiple localized elements
- ✅ Validate localized element usage (errors on associations, warnings on non-String)
- ✅ Support localized in service projections
- ✅ All tests pass
- ✅ Documentation updated
- ✅ Zero breaking changes

## Estimated Effort

**Implementation:** 1.5-2 hours
- Grammar changes: 10 minutes (single line)
- Validation logic: 30 minutes
- Testing: 45 minutes
- Documentation: 15 minutes

**Testing & Verification:** 15 minutes
**Total:** ~2 hours

This completes Phase 11 (Localized Data) and brings coverage to ~49%, with full support for multilingual fields.

## Comparison with Phases 8 & 10

| Phase | Modifier | Purpose | Storage Impact |
|-------|----------|---------|----------------|
| 8 | KEY | Primary key identifier | Indexed, unique |
| 10 | VIRTUAL | Transient/computed | Not persisted |
| 11 | LOCALIZED | Multilingual field | Separate text table |

All three use the same pattern:
- Extend ElementModifier enum
- Add validation for proper usage
- No new AST nodes required
- Simple, clean, consistent implementation

This validates the ElementModifier design pattern works well for storage/type modifiers.
