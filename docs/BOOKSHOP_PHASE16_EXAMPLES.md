# Phase 16 Validation Examples - bookshop.cds

This document explains the Phase 16 validation examples added to `samples/bookshop.cds`.

## Overview

Phase 16 adds enhanced semantic validation that catches common issues and provides helpful warnings. The updated bookshop.cds demonstrates all Phase 16 validation features.

## Validation Examples

### 1. Constraint Conflicts (Lines 247-274)

```cds
entity ValidationExamples {
  key ID: UUID;

  // ℹ️ Info: not null with default is redundant (but valid)
  email: String(100) not null default 'unknown@example.com';

  // Valid: unique with default
  username: String(50) unique default 'anonymous';

  // ⚠️ Warning: virtual with not null may cause issues
  // virtual computed: Integer not null;  // Commented to avoid warning

  // Valid constraint combinations
  status: String(20) default 'active' not null;
  priority: Integer check priority >= 1 and priority <= 5;
}
```

**What Phase 16 Validates:**
- ℹ️ **Info:** `not null` with `default` value is redundant (the default ensures non-null)
- ⚠️ **Warning:** `virtual` with `not null` is problematic (virtual values computed at runtime)
- ✅ **Valid:** Multiple constraints are allowed when they don't conflict

**Validation Method:** `checkConflictingConstraints()`
**Check Type:** FAST (runs on keystroke)

---

### 2. Circular Dependencies (Lines 276-298)

```cds
entity Orders {
  key ID: UUID;
  customer: Association to Users;
  items: Composition of many OrderItems;  // ⚠️ Circular reference
}

entity OrderItems {
  key ID: UUID;
  order: Association to Orders;  // ⚠️ Circular reference detected
  book: Association to Books;
  quantity: Integer;
  price: Amount;
}
```

**What Phase 16 Validates:**
- ⚠️ **Warning:** Circular dependencies through associations are valid but may cause runtime issues
- Phase 16 uses depth-first search to detect cycles
- Self-references (like Categories.parent) are common patterns and allowed

**Validation Method:** `checkCircularDependency()`
**Check Type:** NORMAL (runs on save)
**Algorithm:** DFS with visited set and recursion stack

---

### 3. Self-Referencing (Lines 300-307)

```cds
entity Categories {
  key ID: Integer;
  name: String(100) not null;
  parent: Association to Categories;  // Self-reference (valid pattern)
  children: Composition of many Categories;
}
```

**What Phase 16 Validates:**
- ✅ Self-referencing is a valid pattern for hierarchies
- Phase 16 detects the cycle but doesn't warn (expected pattern)
- Common for category trees, org charts, etc.

---

### 4. JOIN Validation (Lines 196-204)

```cds
entity BooksWithAuthors as SELECT from Books {
  ID,
  title,
  price
}
inner join Authors as a on a.ID = author
where stock > 0
order by title asc;
```

**What Phase 16 Validates:**
- ✅ JOIN target (`Authors`) exists and is an entity
- ✅ ON condition is present
- ❌ Error if JOIN target is not an entity
- ❌ Error if ON condition is missing

**Validation Methods:**
- `checkJoinTarget()` - CheckType.NORMAL
- `checkJoinCondition()` - CheckType.FAST

---

### 5. Aggregation Validation (Lines 206-218)

```cds
entity BookStatsByGenre as SELECT from Books {
  genre,
  COUNT(ID) as bookCount,
  AVG(price) as avgPrice,
  SUM(stock) as totalStock
}
group by genre
having bookCount > 3
order by bookCount desc;
```

**What Phase 16 Validates:**
- ℹ️ **Info:** Aggregation functions without GROUP BY trigger a suggestion
- Phase 16 detects mixed aggregated/non-aggregated columns
- Helps catch common SQL errors

**Validation Method:** `checkAggregationUsage()`
**Check Type:** FAST (runs on keystroke)

---

## Complete Phase 16 Feature Matrix

| Feature | Validator Method | Check Type | Diagnostic Code |
|---------|------------------|------------|-----------------|
| JOIN target validation | `checkJoinTarget()` | NORMAL | `CODE_JOIN_UNRESOLVED_TARGET` |
| JOIN condition validation | `checkJoinCondition()` | FAST | `CODE_JOIN_INVALID_CONDITION` |
| Circular dependencies | `checkCircularDependency()` | NORMAL | `CODE_CIRCULAR_DEPENDENCY` |
| Constraint conflicts | `checkConflictingConstraints()` | FAST | `CODE_CONFLICTING_CONSTRAINTS` |
| Aggregation usage | `checkAggregationUsage()` | FAST | `CODE_AGGREGATE_WITHOUT_GROUP` |
| Enum circular inheritance | `checkEnumCircularInheritance()` | NORMAL | `CODE_CIRCULAR_INHERITANCE` |

## Check Types Explained

### FAST
- Runs on every keystroke
- No cross-file resolution
- Syntax-based validation
- Examples: Constraint conflicts, aggregation detection

### NORMAL
- Runs on save
- Can access Xtext index
- Cross-reference validation
- Examples: JOIN targets, circular dependencies

### EXPENSIVE
- Runs on explicit Build/Clean
- Can perform expensive operations
- Not used in Phase 16

## How to Test

1. **Open bookshop.cds in Eclipse**
   - Phase 16 validation runs automatically

2. **Look for Validation Markers**
   - ℹ️ Blue info icons for suggestions
   - ⚠️ Yellow warning icons for potential issues
   - ❌ Red error icons for problems

3. **Hover Over Markers**
   - See detailed validation messages
   - Get helpful suggestions

4. **Try Uncommenting**
   - Line 266: Uncomment `virtual computed: Integer not null;`
   - See warning appear immediately (FAST check)

5. **Test Circular Dependencies**
   - Lines 276-298: Orders ↔ OrderItems circular reference
   - Warning appears on save (NORMAL check)

## Common Validation Messages

### Info Messages (Blue)
```
"Element has both 'not null' and a default value - the default ensures
non-null so 'not null' is redundant"
```

### Warning Messages (Yellow)
```
"Entity 'Orders' may have circular dependency through associations -
this is valid but may cause runtime issues"
```

```
"Virtual elements with 'not null' constraint may cause issues - virtual
values are computed at runtime"
```

### Error Messages (Red)
```
"JOIN target must be an entity, but 'TypeName' is a TypeDef"
```

```
"JOIN must have an ON condition"
```

## Performance Notes

- Phase 16 validation is optimized for responsiveness
- FAST checks have minimal performance impact
- NORMAL checks use Xtext index for efficiency
- Circular dependency detection uses memoization (visited set)

## Integration with Other Phases

### Phase 9 (Constraints)
- Phase 9 parses constraints
- Phase 16 validates constraint combinations
- Example: `not null default 'x'` → Phase 9 parses, Phase 16 validates

### Phase 14 (Views)
- Phase 14 parses SELECT queries
- Phase 16 validates aggregation usage
- Example: `COUNT(ID)` without GROUP BY → Phase 16 suggests adding it

### Phase 17 (Advanced Queries)
- Phase 17 parses JOINs
- Phase 16 validates JOIN targets and conditions
- Example: `inner join Authors` → Phase 16 checks Authors is an entity

## Best Practices

1. **Pay Attention to Info Messages**
   - They suggest improvements, not errors
   - Help write cleaner CDS models

2. **Review Warnings**
   - Indicate potential issues
   - May work but could cause problems

3. **Fix Errors**
   - Must be resolved for valid models
   - Prevent compilation/runtime issues

4. **Use Validation as Learning Tool**
   - Read validation messages to understand CDS semantics
   - Learn best practices from suggestions

## Summary

The updated bookshop.cds demonstrates:
- ✅ 6 Phase 16 validation features
- ✅ Real-world validation scenarios
- ✅ Info, warning, and error examples
- ✅ Integration with Phases 9, 14, 17
- ✅ Best practices for constraint usage
- ✅ Performance-optimized validation

**File Size:** 339 lines (was 257)
**New Content:** ~82 lines of Phase 16 examples
**Coverage:** All 6 Phase 16 validation methods demonstrated
