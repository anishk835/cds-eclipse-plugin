# Implementation Plan: Phase 13 - Actions and Functions

## Context

Phases 1-11 are complete (~49% coverage). Phase 12 (ON Conditions) encountered technical issues. Phase 13 adds actions and functions, which are in a separate grammar area and should be straightforward to implement.

**Current State:**
- ✅ Phases 1-11: Complete
- ⚠️ Phase 12: Deferred (grammar generator issues)
- ✅ Entity definitions work well
- ✅ Element definitions work well
- ❌ Missing: Actions and functions for business logic

**Why This Matters:**
- **CRITICAL for production** - Actions/functions are business logic entry points
- Required for CRUD operations beyond basic reads
- Essential for Fiori UI interactions
- Standard SAP CAP pattern for custom operations
- Cannot build real applications without them

**Target SAP CAP CDS Syntax:**
```cds
// Bound actions/functions (on entities)
entity Orders {
  key ID: UUID;
  status: String;

  // Bound action - modifies state
  action cancel(reason: String) returns {
    success: Boolean;
    message: String;
  };

  // Bound function - read-only query
  function getTotal() returns Decimal;
}

// Unbound actions/functions (in services)
service OrderService {
  entity Orders as projection on Orders;

  // Unbound action
  action processPayment(orderID: UUID, amount: Decimal);

  // Unbound function
  function getExchangeRate(from: String, to: String) returns Decimal;
}
```

## Recommended Approach: Add ActionDef and FunctionDef

### Architecture Decision

I recommend **adding separate ActionDef and FunctionDef rules** because:

1. **Clean Separation:**
   - Actions modify state, functions are read-only
   - Different semantics in SAP CAP
   - Actions use POST, functions use GET in OData

2. **Fits Grammar Structure:**
   - Can be elements in entities
   - Can be definitions in services
   - Similar to existing Element pattern

3. **Simple Implementation:**
   - No modification of complex expression hierarchy
   - Self-contained grammar rules
   - Clear AST structure

### Grammar Structure

```xtext
// In Entity (after Element rule)
Entity:
    annotations+=Annotation*
    'entity' name=ID
    (':' includes+=[AspectDef|QualifiedName]
         (',' includes+=[AspectDef|QualifiedName])*)?
    '{'
        (elements+=Element | actions+=ActionDef | functions+=FunctionDef)*
    '}';

// In Service (after ServiceEntity rule)
ServiceDef:
    annotations+=Annotation*
    'service' name=ID '{'
        (entities+=ServiceEntity | actions+=ActionDef | functions+=FunctionDef)*
    '}';

// New rules
ActionDef:
    annotations+=Annotation*
    'action' name=ID
    '(' (params+=Parameter (',' params+=Parameter)*)? ')'
    ('returns' returnType=ReturnType)?
    ';';

FunctionDef:
    annotations+=Annotation*
    'function' name=ID
    '(' (params+=Parameter (',' params+=Parameter)*)? ')'
    ('returns' returnType=ReturnType)?
    ';';

Parameter:
    name=ID ':' type=TypeRef;

ReturnType:
    TypeRef | ReturnTypeStruct;

ReturnTypeStruct:
    '{' elements+=ReturnElement (',' elements+=ReturnElement)* ','? '}';

ReturnElement:
    name=ID ':' type=TypeRef;
```

**Key Points:**
- Actions and functions can appear in entities and services
- Parameters have name and type
- Return type can be simple TypeRef or structured
- Structured return types allow multiple fields

## Implementation Steps

### Step 1: Grammar Extension

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext`

**Location 1:** Modify EntityDef (line 37)

```xtext
EntityDef:
    {EntityDef} annotations+=Annotation*
    'entity' name=ID
    (':' includes+=[AspectDef|QualifiedName]
         (',' includes+=[AspectDef|QualifiedName])*)?
    '{'
        (elements+=Element | actions+=ActionDef | functions+=FunctionDef)*
    '}';
```

**Location 2:** Modify ServiceDef (line 85)

```xtext
ServiceDef:
    {ServiceDef} annotations+=Annotation*
    'service' name=ID '{'
        (entities+=ServiceEntity | actions+=ActionDef | functions+=FunctionDef)*
    '}';
```

**Location 3:** Add new rules (after ServiceEntity, around line 98)

```xtext
// ─────────────────────────────────────────────────────────────────────────────
// Actions & Functions  (Phase 13)
// ─────────────────────────────────────────────────────────────────────────────

ActionDef:
    annotations+=Annotation*
    'action' name=ID
    '(' (params+=Parameter (',' params+=Parameter)*)? ')'
    ('returns' returnType=ReturnType)?
    ';';

FunctionDef:
    annotations+=Annotation*
    'function' name=ID
    '(' (params+=Parameter (',' params+=Parameter)*)? ')'
    ('returns' returnType=ReturnType)?
    ';';

Parameter:
    name=ID ':' type=TypeRef;

ReturnType:
    TypeRef | ReturnTypeStruct;

ReturnTypeStruct:
    '{' elements+=ReturnElement (';' elements+=ReturnElement)* ';'? '}';

ReturnElement:
    name=ID ':' type=TypeRef;
```

**Auto-Generated:**
- `ActionDef.java` interface
- `FunctionDef.java` interface
- `Parameter.java` interface
- `ReturnType.java` interface
- `ReturnTypeStruct.java` interface
- `ReturnElement.java` interface
- Updated `EntityDef.java` with `getActions()` and `getFunctions()`
- Updated `ServiceDef.java` with `getActions()` and `getFunctions()`

### Step 2: Validation Implementation

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add Diagnostic Codes** (after Phase 11 codes):
```java
// Phase 13: Actions and functions validation codes
public static final String CODE_ACTION_DUPLICATE_PARAM    = "cds.action.duplicate.param";
public static final String CODE_FUNCTION_DUPLICATE_PARAM  = "cds.function.duplicate.param";
public static final String CODE_ACTION_NO_RETURN_HINT     = "cds.action.no.return.hint";
public static final String CODE_FUNCTION_NO_RETURN        = "cds.function.no.return";
public static final String CODE_ACTION_FUNCTION_HINT      = "cds.action.function.hint";
```

**Add Validation Methods** (add new section after Phase 11):

```java
// ── Phase 13: Actions and functions validation ───────────────────────────────

/**
 * Validates action definitions.
 */
@Check(CheckType.FAST)
public void checkActionDef(ActionDef action) {
    if (action.getName() == null) return;

    // Check for duplicate parameters
    checkDuplicateParameters(action.getParams(),
        CdsPackage.Literals.ACTION_DEF__PARAMS,
        CODE_ACTION_DUPLICATE_PARAM);

    // Info: Actions typically modify state
    if (action.getReturnType() == null) {
        info("Action without return type - ensure this is intentional (actions typically return status)",
            action,
            CdsPackage.Literals.ACTION_DEF__NAME,
            CODE_ACTION_NO_RETURN_HINT);
    }
}

/**
 * Validates function definitions.
 */
@Check(CheckType.FAST)
public void checkFunctionDef(FunctionDef function) {
    if (function.getName() == null) return;

    // Check for duplicate parameters
    checkDuplicateParameters(function.getParams(),
        CdsPackage.Literals.FUNCTION_DEF__PARAMS,
        CODE_FUNCTION_DUPLICATE_PARAM);

    // Warning: Functions should have return type
    if (function.getReturnType() == null) {
        warning("Function should have a return type - functions are read-only queries that return data",
            function,
            CdsPackage.Literals.FUNCTION_DEF__NAME,
            CODE_FUNCTION_NO_RETURN);
    }
}

/**
 * Helper: Check for duplicate parameter names.
 */
private void checkDuplicateParameters(List<Parameter> params,
                                     org.eclipse.emf.ecore.EReference feature,
                                     String code) {
    Set<String> seen = new HashSet<>();
    for (Parameter param : params) {
        if (param.getName() == null) continue;
        if (!seen.add(param.getName())) {
            error("Duplicate parameter: '" + param.getName() + "'",
                param,
                CdsPackage.Literals.PARAMETER__NAME,
                code);
        }
    }
}
```

### Step 3: Testing

**File:** `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java`

**Add Imports:**
```java
import org.example.cds.cDS.ActionDef;
import org.example.cds.cDS.FunctionDef;
import org.example.cds.cDS.Parameter;
```

**Add Test Methods** (in new Phase 13 section):

```java
// ── Phase 13: Actions and Functions ──────────────────────────────────────────

@Test
public void parseSimpleAction() throws Exception {
    CdsFile file = parse("""
        entity Orders {
          key ID: UUID;
          action cancel();
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    assertEquals(1, entity.getActions().size());
    ActionDef action = entity.getActions().get(0);
    assertEquals("cancel", action.getName());
}

@Test
public void parseActionWithParameters() throws Exception {
    CdsFile file = parse("""
        entity Orders {
          key ID: UUID;
          action cancel(reason: String);
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    ActionDef action = entity.getActions().get(0);
    assertEquals(1, action.getParams().size());
    assertEquals("reason", action.getParams().get(0).getName());
}

@Test
public void parseActionWithReturnType() throws Exception {
    CdsFile file = parse("""
        entity Orders {
          key ID: UUID;
          action cancel(reason: String) returns Boolean;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    ActionDef action = entity.getActions().get(0);
    assertNotNull(action.getReturnType());
}

@Test
public void parseActionWithStructuredReturn() throws Exception {
    CdsFile file = parse("""
        entity Orders {
          key ID: UUID;
          action cancel(reason: String) returns {
            success: Boolean;
            message: String;
          };
        }
        """);
    validationHelper.assertNoErrors(file);
}

@Test
public void parseSimpleFunction() throws Exception {
    CdsFile file = parse("""
        entity Orders {
          key ID: UUID;
          function getTotal() returns Decimal;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    assertEquals(1, entity.getFunctions().size());
    FunctionDef function = entity.getFunctions().get(0);
    assertEquals("getTotal", function.getName());
}

@Test
public void parseFunctionWithParameters() throws Exception {
    CdsFile file = parse("""
        entity Products {
          key ID: UUID;
          function calculatePrice(quantity: Integer, discount: Decimal) returns Decimal;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    FunctionDef function = entity.getFunctions().get(0);
    assertEquals(2, function.getParams().size());
}

@Test
public void parseUnboundActionInService() throws Exception {
    CdsFile file = parse("""
        entity Orders { key ID: UUID; }

        service OrderService {
          entity Orders as projection on Orders;
          action processPayment(orderID: UUID, amount: Decimal);
        }
        """);
    validationHelper.assertNoErrors(file);
    ServiceDef service = (ServiceDef) file.getDefinitions().stream()
        .filter(d -> d instanceof ServiceDef)
        .map(d -> (ServiceDef) d)
        .findFirst().get();
    assertEquals(1, service.getActions().size());
}

@Test
public void parseUnboundFunctionInService() throws Exception {
    CdsFile file = parse("""
        service UtilityService {
          function getExchangeRate(from: String, to: String) returns Decimal;
        }
        """);
    validationHelper.assertNoErrors(file);
    ServiceDef service = (ServiceDef) file.getDefinitions().get(0);
    assertEquals(1, service.getFunctions().size());
}

@Test
public void parseMultipleActionsAndFunctions() throws Exception {
    CdsFile file = parse("""
        entity Orders {
          key ID: UUID;
          action cancel(reason: String) returns Boolean;
          action approve() returns Boolean;
          function getTotal() returns Decimal;
          function getItemCount() returns Integer;
        }
        """);
    validationHelper.assertNoErrors(file);
    EntityDef entity = (EntityDef) file.getDefinitions().get(0);
    assertEquals(2, entity.getActions().size());
    assertEquals(2, entity.getFunctions().size());
}

@Test
public void detectDuplicateActionParameters() throws Exception {
    CdsFile file = parse("""
        entity Orders {
          key ID: UUID;
          action process(status: String, status: String);
        }
        """);
    validationHelper.assertError(file,
        CdsPackage.Literals.PARAMETER,
        CDSValidator.CODE_ACTION_DUPLICATE_PARAM);
}

@Test
public void warnFunctionWithoutReturn() throws Exception {
    CdsFile file = parse("""
        entity Orders {
          key ID: UUID;
          function calculate();
        }
        """);
    validationHelper.assertWarning(file,
        CdsPackage.Literals.FUNCTION_DEF,
        CDSValidator.CODE_FUNCTION_NO_RETURN);
}
```

### Step 4: Update Samples and Documentation

**File:** `/Users/I546280/cds-eclipse-plugin/samples/bookshop.cds`

Add action/function examples:

```cds
entity Orders {
  key ID: UUID;
  customer_ID: UUID;
  status: String;
  total: Decimal;

  // Actions
  action cancel(reason: String) returns {
    success: Boolean;
    message: String;
  };

  action approve() returns Boolean;

  // Functions
  function getTotal() returns Decimal;
}

service OrderService {
  entity Orders as projection on Orders;

  // Unbound action
  action processRefund(orderID: UUID, amount: Decimal);

  // Unbound function
  function getTaxRate(country: String) returns Decimal;
}
```

**File:** Create `/Users/I546280/cds-eclipse-plugin/samples/phase13-test.cds`

```cds
/**
 * Phase 13 Test - Actions and Functions
 * Tests action and function definitions for business logic
 */

namespace test.phase13;

// ─── Test 1: Simple actions ──────────────────────────────────────────────────

entity Orders {
  key ID: UUID;
  status: String;

  action cancel();
  action approve();
}

// ─── Test 2: Actions with parameters ─────────────────────────────────────────

entity Orders2 {
  key ID: UUID;

  action cancel(reason: String);
  action setStatus(newStatus: String, note: String);
}

// ─── Test 3: Actions with return types ───────────────────────────────────────

entity Orders3 {
  key ID: UUID;

  action cancel(reason: String) returns Boolean;
  action process() returns String;
}

// ─── Test 4: Actions with structured returns ─────────────────────────────────

entity Orders4 {
  key ID: UUID;

  action submit(items: String) returns {
    success: Boolean;
    orderID: UUID;
    message: String;
  };
}

// ─── Test 5: Simple functions ────────────────────────────────────────────────

entity Products {
  key ID: UUID;
  price: Decimal;

  function getPrice() returns Decimal;
  function isAvailable() returns Boolean;
}

// ─── Test 6: Functions with parameters ───────────────────────────────────────

entity Products2 {
  key ID: UUID;

  function calculatePrice(quantity: Integer, discount: Decimal) returns Decimal;
  function checkStock(warehouseID: UUID) returns Integer;
}

// ─── Test 7: Unbound actions in services ─────────────────────────────────────

service OrderService {
  action processPayment(orderID: UUID, amount: Decimal);
  action sendNotification(email: String, message: String);
}

// ─── Test 8: Unbound functions in services ───────────────────────────────────

service UtilityService {
  function getExchangeRate(from: String, to: String) returns Decimal;
  function calculateTax(amount: Decimal, country: String) returns Decimal;
}

// ─── Test 9: Mixed entities ──────────────────────────────────────────────────

entity FullExample {
  key ID: UUID;
  name: String not null;
  price: Decimal;

  // Regular elements
  virtual displayName: String;
  localized description: String;

  // Actions
  action activate() returns Boolean;
  action deactivate(reason: String) returns {
    success: Boolean;
    timestamp: DateTime;
  };

  // Functions
  function isValid() returns Boolean;
  function getDetails() returns {
    name: String;
    price: Decimal;
  };
}
```

**File:** `/Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md`

Update to add Phase 13:
```markdown
#### Phase 13: Actions and Functions (Complete)
- ✅ Bound actions (on entities)
- ✅ Bound functions (on entities)
- ✅ Unbound actions (in services)
- ✅ Unbound functions (in services)
- ✅ Action/function parameters
- ✅ Simple return types
- ✅ Structured return types
- ✅ Parameter validation
```

## Critical Files

### Files to Modify:
1. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext`
   - Modify EntityDef to include actions and functions
   - Modify ServiceDef to include actions and functions
   - Add ActionDef, FunctionDef, Parameter, ReturnType rules

2. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`
   - Add 5 diagnostic codes
   - Add 2 validation methods + 1 helper

3. `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java`
   - Add 11 test methods

4. `/Users/I546280/cds-eclipse-plugin/samples/bookshop.cds`
   - Add action/function examples

5. `/Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md`
   - Update with Phase 13

### Generated Files (Auto-updated by Xtext):
- `ActionDef.java` interface
- `FunctionDef.java` interface
- `Parameter.java` interface
- `ReturnType.java` interface
- `ReturnTypeStruct.java` interface
- `ReturnElement.java` interface
- Updated `EntityDef.java`
- Updated `ServiceDef.java`
- Parser and lexer artifacts

## Success Criteria

- ✅ Parse actions with no parameters
- ✅ Parse actions with parameters
- ✅ Parse actions with simple return types
- ✅ Parse actions with structured return types
- ✅ Parse functions with parameters
- ✅ Parse functions with return types
- ✅ Parse unbound actions/functions in services
- ✅ Validate duplicate parameters
- ✅ Validate function return types
- ✅ All tests pass
- ✅ Documentation updated

## Estimated Effort

**Implementation:** 2-3 hours
- Grammar changes: 45 minutes
- Validation logic: 30 minutes
- Testing: 60 minutes
- Documentation: 15 minutes

**Testing & Verification:** 20 minutes
**Total:** ~3 hours

This completes Phase 13 (Actions and Functions) and brings coverage to ~52%, adding CRITICAL business logic capabilities.

## Impact Assessment

**Before Phase 13:**
- ❌ Cannot model business logic entry points
- ❌ No way to define custom operations
- ❌ Cannot model CRUD beyond basic reads
- ❌ No Fiori action/function support

**After Phase 13:**
- ✅ Business logic entry points defined
- ✅ Custom operations modeled
- ✅ CRUD operations supported
- ✅ Fiori actions/functions enabled
- ✅ Standard SAP CAP pattern supported

**Coverage Impact:** 49% → 52% (+3%)
**Production Readiness:** Significantly improved - critical capability added
