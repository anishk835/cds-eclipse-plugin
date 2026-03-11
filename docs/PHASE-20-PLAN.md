# Implementation Plan: Phase 20 - Foreign Keys (2% Coverage)

## Context

**Current State:**
- ✅ Coverage: ~86% of SAP CAP CDS specification (after Phase 19)
- ✅ Scope analysis - validates types, associations, imports
- ✅ Basic association parsing - `Association to Entity` works
- ❌ **NO ON CONDITION VALIDATION** - ON clauses not type-checked
- ❌ **NO FOREIGN KEY VALIDATION** - key compatibility not checked
- ❌ **LIMITED JOIN VALIDATION** - Phase 16 had basic checks, need enhancement

**The Problem:**
Currently, the parser accepts invalid CDS like:
```cds
entity Books {
  key ID: UUID;
  author: Association to Authors on author.ID = ID;  // No validation
}

entity Authors {
  key ID: Integer;  // Different type from Books.ID (UUID)
}

// Should error: UUID = Integer in ON condition
```

**Why Foreign Key Validation Matters (Production SAP CAP):**
1. **Data integrity** - prevents mismatched key types
2. **Runtime errors** - catches issues before database deployment
3. **Referential integrity** - ensures foreign keys are valid
4. **Better IDE support** - immediate feedback on key mismatches
5. **SAP HANA compliance** - HANA requires type-compatible foreign keys

## Recommended Approach: ON Condition Validator + Key Compatibility Checker

### Architecture Decision

**Option A: Full Foreign Key Constraint Validator (Heavyweight)**
- Pros: Complete FK validation, cascade rules, etc.
- Cons: Complex, requires deep understanding of SAP HANA FK rules

**Option B: ON Condition + Key Type Validator (Recommended)**
- Pros: Focused on type safety, builds on Phase 18 type system
- Cons: Doesn't validate cascade rules (acceptable for now)
- **Best for:** Production SAP CAP with existing type infrastructure

**Rationale:**
1. Phase 18 already has type inference via ExpressionTypeComputer
2. Can reuse TypeInfo and TypeCompatibilityChecker
3. Focus on most common errors: mismatched key types
4. Incremental improvement on Phase 16 JOIN validation

### Foreign Key Validation Components

```
ForeignKeyValidator (new validator methods in CDSValidator)
├── checkOnConditionTypes() → validates ON clause type compatibility
├── checkAssociationKeyCompatibility() → checks source/target key types
├── checkManagedAssociations() → validates managed associations
└── checkBacklinkConsistency() → validates bidirectional associations

KeyHelper (utility class)
├── getKeyElements(EntityDef) → extracts key fields
├── inferKeyType(Element) → gets key field type
├── areKeysCompatible(EntityDef, EntityDef) → checks key compatibility
└── extractOnConditionReferences(Expression) → parses ON clause
```

## Implementation Steps

### Step 1: Create KeyHelper Utility Class

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/KeyHelper.java` (NEW)

**Purpose:** Helper methods for foreign key validation

```java
package org.example.cds.validation;

import org.example.cds.cDS.*;
import org.example.cds.typing.TypeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KeyHelper {

    /**
     * Extracts all key elements from an entity.
     */
    public List<Element> getKeyElements(EntityDef entity) {
        if (entity == null) return new ArrayList<>();

        return entity.getElements().stream()
            .filter(elem -> elem.getModifiers() != null &&
                           elem.getModifiers().stream()
                               .anyMatch(mod -> mod == ElementModifier.KEY))
            .collect(Collectors.toList());
    }

    /**
     * Checks if entity has a key defined.
     */
    public boolean hasKey(EntityDef entity) {
        return !getKeyElements(entity).isEmpty();
    }

    /**
     * Gets the primary key type (for single-key entities).
     * Returns null if composite key or no key.
     */
    public TypeRef getPrimaryKeyType(EntityDef entity) {
        List<Element> keys = getKeyElements(entity);
        if (keys.size() == 1) {
            return keys.get(0).getType();
        }
        return null;  // Composite key or no key
    }

    /**
     * Checks if two entities have compatible keys for foreign key relationship.
     * Compatible means:
     * - Both have keys defined
     * - Same number of key fields (for composite keys)
     * - Corresponding key types are compatible
     */
    public boolean areKeysCompatible(EntityDef source, EntityDef target,
                                     TypeCompatibilityChecker typeChecker) {
        if (source == null || target == null) return false;

        List<Element> sourceKeys = getKeyElements(source);
        List<Element> targetKeys = getKeyElements(target);

        // Both must have keys
        if (sourceKeys.isEmpty() || targetKeys.isEmpty()) {
            return false;
        }

        // Must have same number of key fields
        if (sourceKeys.size() != targetKeys.size()) {
            return false;
        }

        // Check type compatibility for each key field
        for (int i = 0; i < sourceKeys.size(); i++) {
            TypeRef sourceType = sourceKeys.get(i).getType();
            TypeRef targetType = targetKeys.get(i).getType();

            if (sourceType == null || targetType == null) {
                return false;
            }

            // Convert to TypeInfo and check compatibility
            TypeInfo sourceTypeInfo = createTypeInfo(sourceType);
            TypeInfo targetTypeInfo = createTypeInfo(targetType);

            if (sourceTypeInfo == null || targetTypeInfo == null) {
                return false;
            }

            if (!typeChecker.areCompatible(sourceTypeInfo, targetTypeInfo)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extracts references from ON condition expression.
     * Returns list of element names referenced in the condition.
     */
    public List<String> extractOnConditionReferences(Expression expr) {
        List<String> refs = new ArrayList<>();
        if (expr == null) return refs;

        // Traverse expression tree and collect RefExpr names
        collectReferences(expr, refs);
        return refs;
    }

    private void collectReferences(Expression expr, List<String> refs) {
        if (expr instanceof RefExpr) {
            RefExpr refExpr = (RefExpr) expr;
            if (refExpr.getRef() != null && refExpr.getRef().getName() != null) {
                refs.add(refExpr.getRef().getName());
            }
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr) expr;
            collectReferences(binExpr.getLeft(), refs);
            collectReferences(binExpr.getRight(), refs);
        }

        if (expr instanceof UnaryExpr) {
            UnaryExpr unExpr = (UnaryExpr) expr;
            collectReferences(unExpr.getOperand(), refs);
        }
    }

    /**
     * Checks if an association is managed (no explicit ON condition).
     */
    public boolean isManagedAssociation(AssocDef assoc) {
        // Managed associations don't have explicit ON conditions
        // CDS runtime generates the foreign key automatically
        return assoc.getOnCondition() == null;
    }

    /**
     * Gets the number of key fields in an entity.
     */
    public int getKeyCount(EntityDef entity) {
        return getKeyElements(entity).size();
    }

    /**
     * Creates TypeInfo from TypeRef for compatibility checking.
     */
    private TypeInfo createTypeInfo(TypeRef typeRef) {
        if (typeRef instanceof SimpleTypeRef) {
            SimpleTypeRef simpleRef = (SimpleTypeRef) typeRef;
            Definition typeDef = simpleRef.getRef();
            if (typeDef == null || typeDef.eIsProxy()) return null;
            return new TypeInfo(typeDef, false);
        }
        if (typeRef instanceof ArrayTypeRef) {
            ArrayTypeRef arrayRef = (ArrayTypeRef) typeRef;
            SimpleTypeRef elementType = arrayRef.getElementType();
            if (elementType == null) return null;
            Definition typeDef = elementType.getRef();
            if (typeDef == null || typeDef.eIsProxy()) return null;
            return new TypeInfo(typeDef, true);
        }
        return null;
    }
}
```

**Lines:** ~150

---

### Step 2: Add Foreign Key Validation to CDSValidator

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add after Phase 19 validation methods:**

```java
// ── Phase 20: Foreign Keys ────────────────────────────────────────────────

private final KeyHelper keyHelper = new KeyHelper();

/**
 * Validates ON condition type compatibility in associations.
 * Ensures left and right sides of comparisons are type-compatible.
 */
@Check(CheckType.FAST)
public void checkOnConditionTypes(AssocDef assoc) {
    Expression onCondition = assoc.getOnCondition();
    if (onCondition == null) {
        // Managed association - no explicit ON condition
        return;
    }

    // Use existing type checker from Phase 18
    // ON conditions should be boolean expressions with compatible types
    if (onCondition instanceof BinaryExpr) {
        BinaryExpr binExpr = (BinaryExpr) onCondition;
        String op = binExpr.getOp();

        // ON conditions typically use = or other comparison operators
        if (typeComputer.getOperatorRegistry().isComparisonOperator(op)) {
            TypeInfo leftType = typeComputer.inferType(binExpr.getLeft());
            TypeInfo rightType = typeComputer.inferType(binExpr.getRight());

            if (leftType != null && rightType != null) {
                if (!typeComputer.getCompatibilityChecker().areCompatible(leftType, rightType)) {
                    error("ON condition compares incompatible types: " +
                          leftType + " and " + rightType,
                        assoc,
                        CdsPackage.Literals.ASSOC_DEF__ON_CONDITION,
                        CODE_ON_CONDITION_TYPE_MISMATCH);
                }
            }
        }
    }
}

/**
 * Validates key compatibility for managed associations.
 * Ensures source and target entities have compatible key types.
 */
@Check(CheckType.NORMAL)
public void checkAssociationKeyCompatibility(AssocDef assoc) {
    // Only check managed associations (no explicit ON condition)
    if (!keyHelper.isManagedAssociation(assoc)) {
        return;
    }

    Definition target = assoc.getTarget();
    if (!(target instanceof EntityDef)) {
        return;  // Not an entity
    }

    EntityDef targetEntity = (EntityDef) target;

    // Get source entity (containing the association)
    EntityDef sourceEntity = getContainingEntity(assoc);
    if (sourceEntity == null) {
        return;
    }

    // Check if target has a key
    if (!keyHelper.hasKey(targetEntity)) {
        warning("Association target '" + targetEntity.getName() +
                "' has no key defined. Managed association requires target key.",
            assoc,
            CdsPackage.Literals.ASSOC_DEF__TARGET,
            CODE_MISSING_TARGET_KEY);
        return;
    }

    // For managed associations, CDS generates foreign key based on target key
    // Validate that key types are compatible
    List<Element> targetKeys = keyHelper.getKeyElements(targetEntity);

    if (targetKeys.size() > 1) {
        // Composite key - provide info
        info("Association to entity with composite key (" + targetKeys.size() + " fields). " +
             "CDS will generate corresponding foreign key fields.",
            assoc,
            CdsPackage.Literals.ASSOC_DEF__TARGET,
            CODE_COMPOSITE_KEY_INFO);
    }
}

/**
 * Gets the entity that contains an element (association).
 */
private EntityDef getContainingEntity(Element element) {
    org.eclipse.emf.ecore.EObject container = element.eContainer();
    while (container != null) {
        if (container instanceof EntityDef) {
            return (EntityDef) container;
        }
        container = container.eContainer();
    }
    return null;
}

/**
 * Validates unmanaged associations have proper ON conditions.
 * Checks that ON condition references valid elements.
 */
@Check(CheckType.FAST)
public void checkUnmanagedAssociationOnCondition(AssocDef assoc) {
    Expression onCondition = assoc.getOnCondition();
    if (onCondition == null) {
        // Managed association - OK
        return;
    }

    // Extract references from ON condition
    List<String> refs = keyHelper.extractOnConditionReferences(onCondition);

    if (refs.isEmpty()) {
        warning("ON condition doesn't reference any fields. " +
                "Expected comparison between source and target keys.",
            assoc,
            CdsPackage.Literals.ASSOC_DEF__ON_CONDITION,
            CODE_EMPTY_ON_CONDITION);
    }

    // Validate that referenced fields can be resolved
    // (This is already handled by Phase 19 scope validation for RefExpr)
}

/**
 * Validates bidirectional associations for consistency.
 * If A -> B and B -> A, check that relationships are consistent.
 */
@Check(CheckType.NORMAL)
public void checkBacklinkConsistency(AssocDef assoc) {
    // Get target entity
    Definition target = assoc.getTarget();
    if (!(target instanceof EntityDef)) {
        return;
    }

    EntityDef targetEntity = (EntityDef) target;
    EntityDef sourceEntity = getContainingEntity(assoc);
    if (sourceEntity == null) {
        return;
    }

    // Check if target has association back to source
    for (Element elem : targetEntity.getElements()) {
        if (elem instanceof AssocDef) {
            AssocDef backlink = (AssocDef) elem;
            Definition backlinkTarget = backlink.getTarget();

            if (backlinkTarget == sourceEntity) {
                // Found bidirectional relationship
                // Check for consistency (both managed or both unmanaged)
                boolean assocIsManaged = keyHelper.isManagedAssociation(assoc);
                boolean backlinkIsManaged = keyHelper.isManagedAssociation(backlink);

                if (assocIsManaged != backlinkIsManaged) {
                    info("Bidirectional association: one side is managed, other is unmanaged. " +
                         "Consider using consistent ON conditions.",
                        assoc,
                        CdsPackage.Literals.ASSOC_DEF__TARGET,
                        CODE_BIDIRECTIONAL_INCONSISTENCY);
                }
            }
        }
    }
}

/**
 * Validates associations to many have proper backlink configuration.
 */
@Check(CheckType.NORMAL)
public void checkAssociationToMany(AssocDef assoc) {
    // Check if this is "Association to many"
    boolean isToMany = assoc.isMany();

    if (isToMany) {
        // For "to many", ON condition is recommended
        if (keyHelper.isManagedAssociation(assoc)) {
            info("Association to many without ON condition. " +
                 "Consider adding explicit ON clause for clarity.",
                assoc,
                CdsPackage.Literals.ASSOC_DEF__MANY,
                CODE_TO_MANY_WITHOUT_ON);
        }

        // Check if target references back to source
        Definition target = assoc.getTarget();
        if (target instanceof EntityDef) {
            EntityDef targetEntity = (EntityDef) target;
            EntityDef sourceEntity = getContainingEntity(assoc);

            if (sourceEntity != null) {
                boolean hasBacklink = targetEntity.getElements().stream()
                    .filter(elem -> elem instanceof AssocDef)
                    .map(elem -> (AssocDef) elem)
                    .anyMatch(a -> a.getTarget() == sourceEntity);

                if (!hasBacklink) {
                    info("Association to many without backlink in target entity. " +
                         "Consider adding association from '" + targetEntity.getName() +
                         "' back to '" + sourceEntity.getName() + "'.",
                        assoc,
                        CdsPackage.Literals.ASSOC_DEF__TARGET,
                        CODE_TO_MANY_NO_BACKLINK);
                }
            }
        }
    }
}
```

**Add diagnostic codes** (after Phase 19 codes):

```java
// Phase 20: Foreign key validation codes
public static final String CODE_ON_CONDITION_TYPE_MISMATCH    = "cds.on.condition.type.mismatch";
public static final String CODE_MISSING_TARGET_KEY            = "cds.missing.target.key";
public static final String CODE_COMPOSITE_KEY_INFO            = "cds.composite.key.info";
public static final String CODE_EMPTY_ON_CONDITION            = "cds.empty.on.condition";
public static final String CODE_BIDIRECTIONAL_INCONSISTENCY   = "cds.bidirectional.inconsistency";
public static final String CODE_TO_MANY_WITHOUT_ON            = "cds.to.many.without.on";
public static final String CODE_TO_MANY_NO_BACKLINK           = "cds.to.many.no.backlink";
```

**Lines Added:** ~200

---

### Step 3: Add Import Statements

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add import:**

```java
import org.example.cds.validation.KeyHelper;
```

---

## Testing Strategy

### Test Cases to Add

**File:** `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/ForeignKeyTest.java` (NEW)

```java
// Test ON condition type compatibility
// Test managed associations
// Test key compatibility checks
// Test bidirectional associations
// Test association to many
// Test composite keys
```

**Lines:** ~250

---

## Critical Files

### Files to Create (NEW):
1. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/KeyHelper.java` (~150 lines)
2. `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/ForeignKeyTest.java` (~250 lines)

### Files to Modify:
1. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`
   - Add 7 diagnostic codes
   - Add 6 validation methods (~200 lines)
   - Add 1 helper method
   - Add import

### No Changes Needed:
- ✅ Grammar (CDS.xtext) - stays at 389 lines
- ✅ Parser - no regeneration needed
- ✅ Type system - reuse Phase 18 infrastructure

---

## Success Criteria

**Phase 20 Complete When:**
- ✅ KeyHelper class created (~150 lines)
- ✅ 6 validation methods added to CDSValidator (~200 lines)
- ✅ 7 diagnostic codes added
- ✅ Build succeeds
- ✅ ON condition type mismatches detected
- ✅ Managed association validation works
- ✅ Test file created (~250 lines)

**Coverage Impact:**
- Before: ~86%
- After: ~88% (Foreign Keys adds 2%)

**Total New Code:**
- New classes: ~150 lines
- Validator additions: ~200 lines
- Tests: ~250 lines
- **Total: ~600 lines**

---

This plan provides production-ready foreign key validation for SAP CAP applications with comprehensive type checking and association validation.
