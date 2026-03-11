# Implementation Plan: Phase 21 - Annotations (3% Coverage)

## Context

**Current State:**
- ✅ Coverage: ~88% of SAP CAP CDS specification (after Phase 20)
- ✅ Type system, scope analysis, foreign key validation complete
- ✅ Basic annotation parsing - grammar supports `@annotationName: value`
- ❌ **NO ANNOTATION VALIDATION** - annotations not checked for correctness
- ❌ **NO VOCABULARY VALIDATION** - unknown annotations accepted
- ❌ **NO VALUE TYPE CHECKING** - annotation values not validated

**The Problem:**
Currently, the parser accepts invalid annotations like:
```cds
@readonly: 123  // Should be boolean, not integer
@title: { invalid: 'structure' }  // Should be string
@assert.range: 'not-an-array'  // Should be array [min, max]

entity Books {
  @mandatory: 'yes'  // Should be boolean
  @UnknownAnnotation.xyz: 'value'  // Unknown annotation, no warning
  title: String;
}
```

**Why Annotation Validation Matters (Production SAP CAP):**
1. **Fiori UI generation** - UI.* annotations control app behavior
2. **Authorization** - @requires, @restrict control access
3. **OData capabilities** - @Capabilities.* control service features
4. **Validation rules** - @assert.* validates data at runtime
5. **Developer experience** - catch typos early (@raedonly vs @readonly)

## Recommended Approach: Annotation Vocabulary + Value Validators

### Architecture Decision

**Option A: Full SAP Annotation Vocabulary (Heavyweight)**
- Pros: Complete SAP ecosystem support, all standard annotations
- Cons: Hundreds of annotations, complex structure validation

**Option B: Core Annotations + Extensible Framework (Recommended)**
- Pros: Common annotations validated, easy to extend, incremental
- Cons: Not all SAP annotations initially (can add later)
- **Best for:** Production SAP CAP with most common use cases

**Rationale:**
1. Focus on most frequently used annotations first
2. Build extensible framework for easy additions
3. Provide warnings for unknown annotations (not errors)
4. Validate annotation value types

### Annotation Validation Components

```
AnnotationRegistry (vocabulary definition)
├── Core annotations (@title, @readonly, @description)
├── Authorization (@requires, @restrict)
├── UI annotations (UI.LineItem, UI.SelectionFields, UI.HeaderInfo)
├── Validation (@assert.range, @assert.format, @mandatory)
├── OData (@Capabilities.*, @Core.*)
└── Custom annotation detection

AnnotationValidator (validation logic)
├── checkAnnotationExists() → known annotation?
├── checkAnnotationTarget() → can be applied here?
├── checkAnnotationValue() → correct value type?
├── checkAnnotationStructure() → valid structure for complex annotations
└── checkDeprecatedAnnotations() → warn about deprecated ones

AnnotationValueChecker (type checking)
├── validateBooleanValue() → true/false
├── validateStringValue() → string literal
├── validateNumberValue() → integer/decimal
├── validateArrayValue() → [item1, item2]
├── validateObjectValue() → { key: value }
└── validateEnumValue() → specific allowed values
```

## Implementation Steps

### Step 1: Create Annotation Model Classes

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/annotations/AnnotationDefinition.java` (NEW)

**Purpose:** Represents an annotation definition with expected value type

```java
package org.example.cds.annotations;

import java.util.Set;

/**
 * Defines an annotation with its expected value type and targets.
 */
public class AnnotationDefinition {

    public enum ValueType {
        BOOLEAN,    // true/false
        STRING,     // "string literal"
        INTEGER,    // 123
        DECIMAL,    // 123.45
        ARRAY,      // [item1, item2]
        OBJECT,     // { key: value }
        ANY         // Any value type accepted
    }

    public enum TargetType {
        ENTITY,
        ELEMENT,
        SERVICE,
        TYPE,
        ENUM,
        ASSOCIATION,
        ANY
    }

    private final String name;
    private final ValueType valueType;
    private final Set<TargetType> allowedTargets;
    private final String description;
    private final boolean deprecated;

    public AnnotationDefinition(String name, ValueType valueType,
                                Set<TargetType> allowedTargets, String description) {
        this(name, valueType, allowedTargets, description, false);
    }

    public AnnotationDefinition(String name, ValueType valueType,
                                Set<TargetType> allowedTargets,
                                String description, boolean deprecated) {
        this.name = name;
        this.valueType = valueType;
        this.allowedTargets = allowedTargets;
        this.description = description;
        this.deprecated = deprecated;
    }

    public String getName() { return name; }
    public ValueType getValueType() { return valueType; }
    public Set<TargetType> getAllowedTargets() { return allowedTargets; }
    public String getDescription() { return description; }
    public boolean isDeprecated() { return deprecated; }

    public boolean canApplyTo(TargetType target) {
        return allowedTargets.contains(TargetType.ANY) ||
               allowedTargets.contains(target);
    }
}
```

**Lines:** ~70

---

### Step 2: Create Annotation Registry

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/annotations/AnnotationRegistry.java` (NEW)

**Purpose:** Central registry of known annotations

```java
package org.example.cds.annotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.example.cds.annotations.AnnotationDefinition.TargetType;
import org.example.cds.annotations.AnnotationDefinition.ValueType;

/**
 * Registry of standard SAP CAP CDS annotations.
 */
public class AnnotationRegistry {

    private static final Map<String, AnnotationDefinition> ANNOTATIONS = new HashMap<>();

    static {
        registerCoreAnnotations();
        registerAuthorizationAnnotations();
        registerUIAnnotations();
        registerValidationAnnotations();
        registerODataAnnotations();
    }

    private static void registerCoreAnnotations() {
        // Core CDS annotations
        register("title", ValueType.STRING, Set.of(TargetType.ANY),
            "Human-readable title for UI display");

        register("description", ValueType.STRING, Set.of(TargetType.ANY),
            "Detailed description text");

        register("readonly", ValueType.BOOLEAN, Set.of(TargetType.ENTITY, TargetType.ELEMENT),
            "Marks entity or field as read-only");

        register("cds.autoexpose", ValueType.BOOLEAN, Set.of(TargetType.ENTITY),
            "Auto-expose entity in parent service");

        register("cds.persistence.skip", ValueType.BOOLEAN, Set.of(TargetType.ENTITY),
            "Skip database table generation");

        register("cds.persistence.journal", ValueType.BOOLEAN, Set.of(TargetType.ENTITY),
            "Enable temporal/journal tables");
    }

    private static void registerAuthorizationAnnotations() {
        // Authorization annotations
        register("requires", ValueType.STRING, Set.of(TargetType.SERVICE, TargetType.ENTITY),
            "Required user role/scope");

        register("restrict", ValueType.ARRAY, Set.of(TargetType.ENTITY),
            "Access restriction rules");
    }

    private static void registerUIAnnotations() {
        // SAP Fiori UI annotations
        register("UI.LineItem", ValueType.ARRAY, Set.of(TargetType.ENTITY),
            "Columns for list view");

        register("UI.SelectionFields", ValueType.ARRAY, Set.of(TargetType.ENTITY),
            "Filter fields");

        register("UI.HeaderInfo", ValueType.OBJECT, Set.of(TargetType.ENTITY),
            "Object page header configuration");

        register("UI.Identification", ValueType.ARRAY, Set.of(TargetType.ENTITY),
            "Identification section fields");

        register("UI.FieldGroup", ValueType.OBJECT, Set.of(TargetType.ENTITY),
            "Field group configuration");

        register("UI.Hidden", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Hide field in UI");
    }

    private static void registerValidationAnnotations() {
        // Validation annotations
        register("mandatory", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Required field");

        register("assert.range", ValueType.ARRAY, Set.of(TargetType.ELEMENT),
            "Value range [min, max]");

        register("assert.format", ValueType.STRING, Set.of(TargetType.ELEMENT),
            "Regular expression pattern");

        register("assert.notNull", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field must not be null");

        register("assert.unique", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field value must be unique");
    }

    private static void registerODataAnnotations() {
        // OData capability annotations
        register("Capabilities.Insertable", ValueType.BOOLEAN,
            Set.of(TargetType.ENTITY),
            "Allow creating new records");

        register("Capabilities.Updatable", ValueType.BOOLEAN,
            Set.of(TargetType.ENTITY),
            "Allow updating records");

        register("Capabilities.Deletable", ValueType.BOOLEAN,
            Set.of(TargetType.ENTITY),
            "Allow deleting records");

        register("Core.Computed", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field value is computed");

        register("Core.Immutable", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field cannot be changed after creation");
    }

    private static void register(String name, ValueType valueType,
                                 Set<TargetType> targets, String description) {
        ANNOTATIONS.put(name, new AnnotationDefinition(name, valueType, targets, description));
    }

    /**
     * Looks up an annotation definition by name.
     */
    public static Optional<AnnotationDefinition> getAnnotation(String name) {
        return Optional.ofNullable(ANNOTATIONS.get(name));
    }

    /**
     * Checks if an annotation is known (standard).
     */
    public static boolean isKnownAnnotation(String name) {
        return ANNOTATIONS.containsKey(name);
    }

    /**
     * Checks if annotation name looks like a standard SAP annotation.
     * Standard patterns: UI.*, Core.*, Common.*, Capabilities.*
     */
    public static boolean looksLikeStandardAnnotation(String name) {
        return name.startsWith("UI.") ||
               name.startsWith("Core.") ||
               name.startsWith("Common.") ||
               name.startsWith("Capabilities.") ||
               name.startsWith("cds.") ||
               name.startsWith("odata.");
    }
}
```

**Lines:** ~140

---

### Step 3: Create Annotation Helper

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/annotations/AnnotationHelper.java` (NEW)

**Purpose:** Helper methods for annotation validation

```java
package org.example.cds.annotations;

import org.example.cds.cDS.*;
import org.example.cds.annotations.AnnotationDefinition.TargetType;
import org.eclipse.emf.ecore.EObject;

/**
 * Helper methods for working with annotations.
 */
public class AnnotationHelper {

    /**
     * Extracts annotation name from AnnotationEntry.
     * Handles both simple (@title) and path (@UI.LineItem) annotations.
     */
    public String getAnnotationName(AnnotationEntry entry) {
        if (entry == null || entry.getName() == null) {
            return null;
        }
        return entry.getName();
    }

    /**
     * Determines the target type where annotation is applied.
     */
    public TargetType getTargetType(EObject context) {
        if (context instanceof EntityDef || context instanceof ServiceEntity) {
            return TargetType.ENTITY;
        }
        if (context instanceof Element) {
            return TargetType.ELEMENT;
        }
        if (context instanceof ServiceDef) {
            return TargetType.SERVICE;
        }
        if (context instanceof TypeDef) {
            return TargetType.TYPE;
        }
        if (context instanceof EnumDef) {
            return TargetType.ENUM;
        }
        if (context instanceof AssocDef) {
            return TargetType.ASSOCIATION;
        }
        return TargetType.ANY;
    }

    /**
     * Checks if annotation value is a boolean literal.
     */
    public boolean isBooleanValue(AnnotationValue value) {
        if (value == null) return false;
        if (value instanceof Literal) {
            return ((Literal) value) instanceof BoolLiteral;
        }
        return false;
    }

    /**
     * Checks if annotation value is a string literal.
     */
    public boolean isStringValue(AnnotationValue value) {
        if (value == null) return false;
        if (value instanceof Literal) {
            return ((Literal) value) instanceof StringLiteral;
        }
        return false;
    }

    /**
     * Checks if annotation value is a number (integer or decimal).
     */
    public boolean isNumberValue(AnnotationValue value) {
        if (value == null) return false;
        if (value instanceof Literal) {
            Literal lit = (Literal) value;
            return lit instanceof IntLiteral || lit instanceof DecimalLiteral;
        }
        return false;
    }

    /**
     * Checks if annotation value is an array.
     */
    public boolean isArrayValue(AnnotationValue value) {
        // In CDS, arrays are represented as [item1, item2]
        // This depends on your grammar - adjust as needed
        return value != null && value.toString().startsWith("[");
    }

    /**
     * Checks if annotation value is an object/structure.
     */
    public boolean isObjectValue(AnnotationValue value) {
        // In CDS, objects are represented as { key: value }
        // This depends on your grammar - adjust as needed
        return value != null && value.toString().startsWith("{");
    }

    /**
     * Gets human-readable value type name.
     */
    public String getValueTypeName(AnnotationValue value) {
        if (isBooleanValue(value)) return "boolean";
        if (isStringValue(value)) return "string";
        if (isNumberValue(value)) return "number";
        if (isArrayValue(value)) return "array";
        if (isObjectValue(value)) return "object";
        return "unknown";
    }
}
```

**Lines:** ~110

---

### Step 4: Add Annotation Validation to CDSValidator

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add after Phase 20 validation methods:**

```java
// ── Phase 21: Annotations ─────────────────────────────────────────────────

private final AnnotationHelper annotationHelper = new AnnotationHelper();

/**
 * Validates that annotations are known (standard or custom).
 * Warns about unknown annotations that might be typos.
 */
@Check(CheckType.FAST)
public void checkAnnotationKnown(AnnotationEntry annotation) {
    String name = annotationHelper.getAnnotationName(annotation);
    if (name == null || name.isEmpty()) {
        return;
    }

    // Check if it's a known standard annotation
    if (AnnotationRegistry.isKnownAnnotation(name)) {
        return;  // ✅ Known annotation
    }

    // Check if it looks like a standard annotation (UI.*, Core.*, etc.)
    if (AnnotationRegistry.looksLikeStandardAnnotation(name)) {
        // Looks standard but not in our registry - might be typo or newer annotation
        info("Unknown standard annotation: '@" + name + "'. " +
             "This might be a typo or an annotation not yet supported.",
            annotation,
            CdsPackage.Literals.ANNOTATION_ENTRY__NAME,
            CODE_UNKNOWN_ANNOTATION);
        return;
    }

    // Custom annotation (not starting with standard prefix) - just info, not warning
    // This is OK - users can define custom annotations
}

/**
 * Validates annotation value types match expected types.
 */
@Check(CheckType.FAST)
public void checkAnnotationValueType(AnnotationEntry annotation) {
    String name = annotationHelper.getAnnotationName(annotation);
    if (name == null) return;

    // Only validate known annotations
    Optional<AnnotationDefinition> def = AnnotationRegistry.getAnnotation(name);
    if (!def.isPresent()) {
        return;  // Unknown annotation, skip value validation
    }

    AnnotationDefinition annotationDef = def.get();
    AnnotationValue value = annotation.getValue();

    if (value == null) {
        // Some annotations don't require values (e.g., @readonly is same as @readonly: true)
        return;
    }

    // Check value type matches expected
    AnnotationDefinition.ValueType expectedType = annotationDef.getValueType();

    switch (expectedType) {
        case BOOLEAN:
            if (!annotationHelper.isBooleanValue(value)) {
                error("Annotation '@" + name + "' expects boolean value (true/false), " +
                      "but got " + annotationHelper.getValueTypeName(value),
                    annotation,
                    CdsPackage.Literals.ANNOTATION_ENTRY__VALUE,
                    CODE_ANNOTATION_VALUE_TYPE);
            }
            break;

        case STRING:
            if (!annotationHelper.isStringValue(value)) {
                error("Annotation '@" + name + "' expects string value, " +
                      "but got " + annotationHelper.getValueTypeName(value),
                    annotation,
                    CdsPackage.Literals.ANNOTATION_ENTRY__VALUE,
                    CODE_ANNOTATION_VALUE_TYPE);
            }
            break;

        case INTEGER:
        case DECIMAL:
            if (!annotationHelper.isNumberValue(value)) {
                error("Annotation '@" + name + "' expects numeric value, " +
                      "but got " + annotationHelper.getValueTypeName(value),
                    annotation,
                    CdsPackage.Literals.ANNOTATION_ENTRY__VALUE,
                    CODE_ANNOTATION_VALUE_TYPE);
            }
            break;

        case ARRAY:
            if (!annotationHelper.isArrayValue(value)) {
                error("Annotation '@" + name + "' expects array value [...], " +
                      "but got " + annotationHelper.getValueTypeName(value),
                    annotation,
                    CdsPackage.Literals.ANNOTATION_ENTRY__VALUE,
                    CODE_ANNOTATION_VALUE_TYPE);
            }
            break;

        case OBJECT:
            if (!annotationHelper.isObjectValue(value)) {
                error("Annotation '@" + name + "' expects object value {...}, " +
                      "but got " + annotationHelper.getValueTypeName(value),
                    annotation,
                    CdsPackage.Literals.ANNOTATION_ENTRY__VALUE,
                    CODE_ANNOTATION_VALUE_TYPE);
            }
            break;

        case ANY:
            // Any value type is OK
            break;
    }
}

/**
 * Validates annotations are applied to correct targets.
 */
@Check(CheckType.FAST)
public void checkAnnotationTarget(AnnotationEntry annotation) {
    String name = annotationHelper.getAnnotationName(annotation);
    if (name == null) return;

    // Only validate known annotations
    Optional<AnnotationDefinition> def = AnnotationRegistry.getAnnotation(name);
    if (!def.isPresent()) {
        return;
    }

    AnnotationDefinition annotationDef = def.get();

    // Get the context where annotation is applied
    EObject context = annotation.eContainer();
    AnnotationDefinition.TargetType actualTarget = annotationHelper.getTargetType(context);

    // Check if annotation can be applied to this target
    if (!annotationDef.canApplyTo(actualTarget)) {
        error("Annotation '@" + name + "' cannot be applied to " +
              actualTarget.toString().toLowerCase() + ". " +
              "Allowed targets: " + annotationDef.getAllowedTargets(),
            annotation,
            CdsPackage.Literals.ANNOTATION_ENTRY__NAME,
            CODE_ANNOTATION_INVALID_TARGET);
    }
}

/**
 * Validates deprecated annotations and suggests alternatives.
 */
@Check(CheckType.NORMAL)
public void checkDeprecatedAnnotation(AnnotationEntry annotation) {
    String name = annotationHelper.getAnnotationName(annotation);
    if (name == null) return;

    Optional<AnnotationDefinition> def = AnnotationRegistry.getAnnotation(name);
    if (def.isPresent() && def.get().isDeprecated()) {
        warning("Annotation '@" + name + "' is deprecated. " +
                def.get().getDescription(),
            annotation,
            CdsPackage.Literals.ANNOTATION_ENTRY__NAME,
            CODE_ANNOTATION_DEPRECATED);
    }
}
```

**Add diagnostic codes** (after Phase 20 codes):

```java
// Phase 21: Annotation validation codes
public static final String CODE_UNKNOWN_ANNOTATION          = "cds.annotation.unknown";
public static final String CODE_ANNOTATION_VALUE_TYPE       = "cds.annotation.value.type";
public static final String CODE_ANNOTATION_INVALID_TARGET   = "cds.annotation.invalid.target";
public static final String CODE_ANNOTATION_DEPRECATED       = "cds.annotation.deprecated";
```

**Lines Added:** ~160

---

## Critical Files

### Files to Create (NEW):
1. `/plugins/org.example.cds/src/org/example/cds/annotations/AnnotationDefinition.java` (~70 lines)
2. `/plugins/org.example.cds/src/org/example/cds/annotations/AnnotationRegistry.java` (~140 lines)
3. `/plugins/org.example.cds/src/org/example/cds/annotations/AnnotationHelper.java` (~110 lines)

### Files to Modify:
1. `/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`
   - Add 4 diagnostic codes
   - Add 4 validation methods (~160 lines)
   - Add imports

2. `/plugins/org.example.cds/META-INF/MANIFEST.MF`
   - Export annotations package

---

## Success Criteria

**Phase 21 Complete When:**
- ✅ 3 annotation classes created (~320 lines)
- ✅ 4 validation methods added (~160 lines)
- ✅ 4 diagnostic codes added
- ✅ Build succeeds
- ✅ Unknown annotations detected
- ✅ Value type mismatches detected
- ✅ Invalid targets detected
- ✅ Test file created

**Coverage Impact:**
- Before: ~88%
- After: ~91% (Annotations add 3%)

**Total New Code:**
- Annotation classes: ~320 lines
- Validator additions: ~160 lines
- Tests: ~300 lines
- **Total: ~780 lines**

---

This plan provides production-ready annotation validation for SAP CAP applications with the most commonly used annotations.
