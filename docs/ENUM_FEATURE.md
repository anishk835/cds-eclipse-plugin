# Enum Type Support in CDS Eclipse Plugin

## Overview

The CDS Eclipse Plugin now provides full support for enum types according to the SAP CAP CDS specification. Enums allow you to define a fixed set of named values for string or integer types, providing better type safety and code clarity.

## Table of Contents

- [Syntax](#syntax)
- [Usage Examples](#usage-examples)
- [Features](#features)
- [Validation Rules](#validation-rules)
- [IDE Support](#ide-support)
- [Migration Guide](#migration-guide)
- [Troubleshooting](#troubleshooting)

---

## Syntax

### Basic Enum Definition

```cds
type <EnumName> : <BaseType> enum {
  <Value1>;
  <Value2>;
  <Value3>;
}
```

**Parameters:**
- `<EnumName>`: The name of the enum type (must be a valid identifier)
- `<BaseType>`: Must be either `String`, `Integer`, or another enum type (for inheritance)
- `<Value1>`, `<Value2>`, etc.: Enum value names (semicolon-separated)

### Enum with Explicit Values

```cds
type <EnumName> : <BaseType> enum {
  <Value1> = <literal1>;
  <Value2> = <literal2>;
}
```

**For String enums:**
```cds
type Gender : String enum {
  male;
  female;
  non_binary = 'non-binary';
}
```

**For Integer enums:**
```cds
type Status : Integer enum {
  Active = 1;
  Inactive = 0;
  Pending = 2;
}
```

### Enum Inheritance

Enums can inherit from other enums, extending them with additional values:

```cds
// Base enum
type BaseStatus : String enum {
  Active;
  Inactive;
}

// Extended enum inheriting from BaseStatus
type ExtendedStatus : BaseStatus enum {
  Pending;
  Cancelled;
}
```

In this example, `ExtendedStatus` includes all values from `BaseStatus` (Active, Inactive) plus its own values (Pending, Cancelled).

**Multi-level inheritance:**
```cds
type Level1 : String enum { Draft; }
type Level2 : Level1 enum { Review; }
type Level3 : Level2 enum { Published; }
// Level3 has: Draft, Review, Published
```

### Enum Value Annotations

Individual enum values can be annotated for documentation, UI display, and metadata:

```cds
type Priority : Integer enum {
  @label: 'Low Priority'
  @description: 'Non-urgent task'
  Low = 1;

  @label: 'High Priority'
  @description: 'Requires immediate attention'
  High = 10;
}
```

**Common annotations:**
- `@label`: Human-readable display name for UI
- `@description`: Detailed explanation of the value's meaning
- `@label.{lang}`: Localized labels (e.g., `@label.de`, `@label.fr`)
- `@UI.color`, `@icon`: UI-specific metadata
- Custom annotations for application-specific needs

**Complex annotation values:**
```cds
type Status : String enum {
  @label: 'Active'
  @UI.badge: { variant: 'success', icon: 'check' }
  @metadata: { sortOrder: 1, deprecated: false }
  Active;
}
```

### Enum Value References

Use the `#` prefix to reference enum values in element defaults:

```cds
entity Products {
  status : Status = #Active;
}
```

---

## Usage Examples

### Example 1: Simple String Enum

```cds
namespace my.bookshop;

// Define a color enum
type Color : String enum {
  Red;
  Green;
  Blue;
  Yellow;
}

// Use in entity
entity Products {
  key ID : UUID;
  name   : String(100);
  color  : Color;
}
```

### Example 2: Integer Enum with Explicit Values

```cds
namespace my.app;

type Priority : Integer enum {
  Low      = 1;
  Medium   = 2;
  High     = 3;
  Critical = 4;
}

entity Tasks {
  key ID       : UUID;
  title        : String(200);
  priority     : Priority = #Medium;  // Default to Medium priority
  status       : String(20);
}
```

### Example 3: Multiple Enums in a Model

```cds
namespace my.shop;

type OrderStatus : String enum {
  Pending;
  Processing;
  Shipped;
  Delivered;
  Cancelled;
}

type PaymentMethod : String enum {
  CreditCard;
  PayPal;
  BankTransfer;
  Cash;
}

entity Orders {
  key ID            : UUID;
  orderDate         : DateTime;
  status            : OrderStatus = #Pending;
  paymentMethod     : PaymentMethod;
  totalAmount       : Decimal(10,2);
}
```

### Example 4: Enum Inheritance

```cds
namespace my.app;

// Base status enum
type BaseStatus : String enum {
  Active;
  Inactive;
}

// Extended status with additional values
type TaskStatus : BaseStatus enum {
  Pending;
  InProgress;
  Completed;
  Cancelled;
}

// Multi-level inheritance
type DetailedStatus : TaskStatus enum {
  OnHold;
  Archived;
}

entity Tasks {
  key ID     : UUID;
  title      : String(200);
  status     : DetailedStatus = #Pending;
  // status can be: Active, Inactive, Pending, InProgress, Completed, Cancelled, OnHold, Archived
}
```

### Example 5: Enum Value Annotations

```cds
namespace my.shop;

type OrderStatus : String enum {
  @label: 'Draft'
  @description: 'Order is being prepared'
  Draft;

  @label: 'Pending Payment'
  @description: 'Waiting for payment confirmation'
  @UI.color: 'yellow'
  PendingPayment;

  @label: 'Processing'
  @description: 'Order is being prepared for shipment'
  @UI.color: 'blue'
  Processing;

  @label: 'Shipped'
  @description: 'Order has been shipped'
  @UI.color: 'green'
  Shipped;
}

entity Orders {
  key ID     : UUID;
  status     : OrderStatus = #Draft;
  // Hover over enum values to see their annotations
}
```

### Example 6: Localized Enum Labels

```cds
type PaymentMethod : String enum {
  @label: 'Credit Card'
  @label.de: 'Kreditkarte'
  @label.fr: 'Carte de crédit'
  CreditCard;

  @label: 'PayPal'
  @label.de: 'PayPal'
  @label.fr: 'PayPal'
  PayPal;
}
```

### Example 7: Enum with Mixed Default and Explicit Values

```cds
type BookStatus : String enum {
  Available;
  Reserved;
  CheckedOut;
  Lost;
  Damaged = 'damaged-beyond-repair';
}
```

---

## Features

### ✅ Supported Features

| Feature | Description | Status |
|---------|-------------|-----------|
| String Enums | Enums with String base type | ✅ Fully Supported |
| Integer Enums | Enums with Integer base type | ✅ Fully Supported |
| Implicit Values | Enum values without explicit assignment | ✅ Fully Supported |
| Explicit Values | Enum values with explicit assignment | ✅ Fully Supported |
| Enum References | `#EnumValue` syntax for defaults | ✅ Fully Supported |
| Enum Inheritance | Extend enums from other enums | ✅ Fully Supported |
| Multi-level Inheritance | Enums inheriting from inherited enums | ✅ Fully Supported |
| Enum Value Annotations | Annotate individual values with @label, @description, etc. | ✅ Fully Supported |
| Localized Labels | Multi-language support with @label.{lang} | ✅ Fully Supported |
| Complex Annotations | Record and array values in annotations | ✅ Fully Supported |
| Type Resolution | Cross-reference enum types in entities | ✅ Fully Supported |
| Validation | Comprehensive validation rules | ✅ Fully Supported |
| Reserved Keyword Detection | Warn about keywords in enum values | ✅ Fully Supported |
| Similar Name Detection | Warn about confusingly similar names | ✅ Fully Supported |
| Value Range Validation | Check integer value ranges and gaps | ✅ Fully Supported |
| Value Count Validation | Recommend reasonable enum sizes | ✅ Fully Supported |
| Value Ordering Hints | Suggest sorted integer enum values | ✅ Fully Supported |
| Auto-Documentation | Rich hover info with value counts and ranges | ✅ Fully Supported |
| Annotation Display | Show annotations in hover documentation | ✅ Fully Supported |
| Code Completion | IntelliSense for enum types and values | ✅ Fully Supported |
| Go-to-Definition | Navigate to enum definition | ✅ Fully Supported |
| Outline View | Display enums in outline | ✅ Fully Supported |
| Hover Documentation | Show enum details on hover | ✅ Fully Supported |

---

## Validation Rules

The CDS Eclipse Plugin enforces the following validation rules for enums:

### 1. Base Type Validation

**Rule:** Enum base type must be `String`, `Integer`, or another enum type (for inheritance).

❌ **Invalid:**
```cds
type MyEnum : Boolean enum { True; False; }
```

**Error:** `Enum base type must be String, Integer, or another enum, found: Boolean`

✅ **Valid:**
```cds
type MyEnum : String enum { Option1; Option2; }
type MyEnum : Integer enum { Value1 = 1; Value2 = 2; }

// Inheritance
type BaseEnum : String enum { Active; }
type ExtendedEnum : BaseEnum enum { Pending; }
```

### 2. Empty Enum Warning

**Rule:** Enums should have at least one value (unless inheriting from a non-empty enum).

⚠️ **Warning:**
```cds
type EmptyEnum : String enum { }
```

**Warning:** `Enum 'EmptyEnum' has no values`

✅ **Valid:**
```cds
type BaseEnum : String enum { Active; }
type ExtendedEnum : BaseEnum enum { }  // OK, inherits Active
```

### 3. Duplicate Enum Values

**Rule:** Enum values must be unique within an enum.

❌ **Invalid:**
```cds
type Status : String enum {
  Active;
  Active;  // Duplicate!
}
```

**Error:** `Duplicate enum value: 'Active'`

### 4. Value Type Matching

**Rule:** Explicit enum values must match the base type.

❌ **Invalid:**
```cds
type Status : Integer enum {
  Active = 'active';  // String value in Integer enum!
}
```

**Error:** `Integer enum 'Status' cannot have string value for 'Active'`

❌ **Invalid:**
```cds
type Color : String enum {
  Red = 1;  // Integer value in String enum!
}
```

**Error:** `String enum 'Color' cannot have integer value for 'Red'`

### 5. Enum Reference Resolution

**Rule:** Enum references (`#Value`) must resolve to existing enum values.

❌ **Invalid:**
```cds
type Color : String enum { Red; Green; }
entity Products {
  color : Color = #Blue;  // Blue doesn't exist!
}
```

**Error:** `Cannot resolve enum value reference`

### 6. Enum Reference Type Checking

**Rule:** Enum references can only be used with enum-typed fields.

❌ **Invalid:**
```cds
entity Products {
  name : String = #SomeValue;  // String is not an enum!
}
```

**Error:** `Enum reference can only be used with enum-typed fields, but field type is: String`

---

### 7. Circular Enum Inheritance

**Rule:** Enums cannot have circular inheritance chains.

❌ **Invalid:**
```cds
type Status1 : Status2 enum { Value1; }
type Status2 : Status1 enum { Value2; }  // Circular!
```

**Error:** `Circular enum inheritance detected`

### 8. Duplicate Inherited Values

**Rule:** An enum cannot redeclare values that are already defined in its parent enum.

❌ **Invalid:**
```cds
type BaseStatus : String enum {
  Active;
  Inactive;
}
type ExtendedStatus : BaseStatus enum {
  Active;  // Already defined in BaseStatus!
}
```

**Error:** `Enum value 'Active' is already defined in parent enum`

✅ **Valid:**
```cds
type BaseStatus : String enum {
  Active;
  Inactive;
}
type ExtendedStatus : BaseStatus enum {
  Pending;   // New value, not in parent
  Cancelled; // New value, not in parent
}
```

### 9. Reserved Keywords

**Rule:** Enum values should not use reserved keywords.

⚠️ **Warning:**
```cds
type QueryStatus : String enum {
  select;    // 'select' is a SQL keyword
  from;      // 'from' is a SQL keyword
  active;
}
```

**Warning:** `Enum value 'select' is a reserved keyword and may cause issues`

**Reserved keywords include:**
- CDS keywords: `entity`, `type`, `aspect`, `service`, `enum`, `namespace`, `using`, `extend`, `annotate`, `key`, etc.
- SQL keywords: `select`, `from`, `where`, `join`, `union`, `insert`, `update`, `delete`, etc.
- Common programming keywords: `if`, `else`, `for`, `while`, `return`, `function`, `class`, `null`, `true`, `false`, etc.

✅ **Better:**
```cds
type QueryStatus : String enum {
  SelectMode;    // Descriptive, not a keyword
  FromSource;
  Active;
}
```

### 10. Similar Names

**Rule:** Enum values should not have confusingly similar names.

⚠️ **Warning (case-only differences):**
```cds
type Status : String enum {
  Active;    // Differs only in case
  active;    // Differs only in case
  ACTIVE;    // Differs only in case
}
```

**Warning:** `Enum value 'active' differs only in case from other values, which may be confusing`

⚠️ **Warning (underscore/case variations):**
```cds
type Status : String enum {
  in_progress;   // Normalized: "inprogress"
  InProgress;    // Normalized: "inprogress" - confusingly similar!
  on_hold;       // Normalized: "onhold"
  onhold;        // Normalized: "onhold" - confusingly similar!
}
```

**Warning:** `Enum value 'InProgress' is confusingly similar to 'in_progress'`

✅ **Better:**
```cds
type Status : String enum {
  Draft;
  InProgress;
  Review;
  Published;
}
```

### 11. Integer Value Ranges

**Rule:** Integer enum values should be within reasonable ranges and not have large unexplained gaps.

⚠️ **Warning (unusually large value):**
```cds
type Numbers : Integer enum {
  Small = 1;
  Huge = 99999999;    // Too large!
}
```

**Warning:** `Integer enum value 99999999 is unusually large and may be unintentional`

Values outside the range [-1,000,000 to 1,000,000] trigger this warning.

⚠️ **Warning (large gap):**
```cds
type GappedValues : Integer enum {
  First = 1;
  Second = 2;
  Third = 10003;     // Gap of 10,001 from previous value
}
```

**Warning:** `Large gap (10001) between enum values may indicate an error`

Gaps larger than 1,000 between adjacent values trigger this warning.

✅ **Valid (HTTP status codes have expected gaps):**
```cds
type HttpStatus : Integer enum {
  OK = 200;
  BadRequest = 400;   // Gap of 200 is acceptable
  ServerError = 500;  // Gap of 100 is acceptable
}
```

### 12. Value Count Recommendations

**Rule:** Enums should have a reasonable number of values.

ℹ️ **Info (single value):**
```cds
type SingleValue : String enum {
  OnlyValue;    // Only 1 value
}
```

**Info:** `Enum 'SingleValue' has only 1 value - consider using a constant instead`

**Rationale:** A single-value enum doesn't provide the benefits of enums. Consider using:
```cds
// Better alternative for single values
const OnlyValue = 'OnlyValue';
```

⚠️ **Warning (too many values):**
```cds
type Countries : String enum {
  USA; Canada; Mexico; /* ... 150+ more values */
}
```

**Warning:** `Enum 'Countries' has 151 values - consider splitting into multiple enums`

**Rationale:** Enums with >100 values are hard to maintain and use. Consider:
- Splitting into regional enums (NorthAmericanCountries, EuropeanCountries, etc.)
- Using a database table instead of an enum
- Creating hierarchical enums with inheritance

✅ **Good practice:**
```cds
type Priority : Integer enum {
  Low = 1;
  Medium = 5;
  High = 10;
  Critical = 99;
}
// 4 values - clear, manageable, easy to understand
```

### 13. Value Ordering Suggestions

**Rule:** Integer enum values should be ordered consistently for clarity.

ℹ️ **Info (unsorted values):**
```cds
type Priority : Integer enum {
  High = 10;         // Not sorted
  Low = 1;
  Medium = 5;
  Critical = 99;
}
```

**Info:** `Integer enum values are not sorted - consider ordering values sequentially for clarity`

✅ **Better (sorted ascending):**
```cds
type Priority : Integer enum {
  Low = 1;
  Medium = 5;
  High = 10;
  Critical = 99;
}
```

✅ **Also acceptable (sorted descending):**
```cds
type Priority : Integer enum {
  Critical = 99;
  High = 10;
  Medium = 5;
  Low = 1;
}
```

**Note:** This is an informational suggestion, not an error. Some enums have natural groupings (like HTTP status codes) where strict sorting isn't necessary.

---

## IDE Support

### Code Completion

When typing in a CDS file, the editor provides intelligent code completion:

1. **Enum Type Suggestions**: When defining a field type, enum types are suggested alongside built-in types.

2. **Enum Value Suggestions**: When typing `#` after `=`, the editor suggests available enum values from the field's enum type.

### Go-to-Definition

- **Ctrl+Click** (or Cmd+Click on Mac) on an enum type reference to jump to its definition
- **Ctrl+Click** on an enum value reference (`#Value`) to jump to the value's definition

### Hover Documentation

Hover over enum types or enum values to see:
- Enum name and base type
- Total value count (including inherited values)
- List of all enum values (up to 10 shown inline)
- For integer enums: value range [min..max]
- For inherited enums: breakdown of own vs inherited values
- For enum values: explicit value assignment and parent enum info
- **Annotations**: All annotations on the enum value (e.g., @label, @description)

**Example hover content for enum:**
```
enum Priority : Integer
8 values

Values: Low, Medium, High, Critical, VeryLow, VeryHigh, Urgent, Emergency
Range: [1..100]
```

**Example hover content for enum value:**
```
Low = 1
in enum Priority : Integer

Annotations:
  @label: 'Low Priority'
  @description: 'Non-urgent task'
  @UI.color: 'gray'
```

### Outline View

The Outline view displays:
- Enum definitions at the top level
- Enum values nested under their enum
- Quick navigation to any enum or value

### Syntax Highlighting

Enums are highlighted with appropriate colors:
- `enum` keyword highlighted as a keyword
- Enum value names highlighted as identifiers
- `#` prefix in enum references highlighted specially

---

## Migration Guide

### Migrating from String Fields to Enums

**Before:**
```cds
entity Orders {
  key ID     : UUID;
  status     : String(20);  // Free-form string
}
```

**After:**
```cds
type OrderStatus : String enum {
  Pending;
  Processing;
  Shipped;
  Delivered;
}

entity Orders {
  key ID     : UUID;
  status     : OrderStatus = #Pending;  // Type-safe enum
}
```

**Benefits:**
- Type safety: Only valid enum values can be assigned
- Better tooling: Code completion for valid values
- Self-documenting: Valid values are defined in the model
- Validation: Catch invalid values at design time

### Migrating from Integer Codes to Enums

**Before:**
```cds
entity Tasks {
  priority : Integer;  // 1 = Low, 2 = Medium, 3 = High
}
```

**After:**
```cds
type Priority : Integer enum {
  Low    = 1;
  Medium = 2;
  High   = 3;
}

entity Tasks {
  priority : Priority = #Medium;
}
```

---

## Troubleshooting

### Issue: "Cannot resolve type" error for enum

**Symptom:** Enum type is not recognized in entity field.

**Cause:** Enum is defined in a different file or namespace.

**Solution:**
```cds
// Add using statement to import enum
using my.types.Color from './types';

entity Products {
  color : Color;  // Now resolves correctly
}
```

### Issue: Enum values not showing in code completion

**Symptom:** When typing `#`, no suggestions appear.

**Cause:** Field type is not resolved yet or is not an enum type.

**Solution:**
1. Ensure the field has an enum type
2. Wait for the workspace to build (check status bar)
3. Try closing and reopening the file

### Issue: Build errors after adding enums

**Symptom:** Build fails with "cannot resolve dependencies"

**Cause:** IDE workspace may need to be refreshed.

**Solution:**
1. Clean the workspace: `mvn clean`
2. Rebuild: `mvn verify -DskipTests`
3. Refresh Eclipse workspace (F5)

### Issue: Enum values with special characters

**Symptom:** Want to use enum values with spaces or special characters.

**Solution:** Use explicit string values:
```cds
type Status : String enum {
  in_progress = 'in progress';
  on_hold = 'on hold';
}
```

---

## Best Practices

### 1. Choose Appropriate Base Types

- Use **String enums** for human-readable values that might change
- Use **Integer enums** for numeric codes, especially when integrating with external systems

### 2. Provide Explicit Values When Needed

```cds
// Good: Explicit values for stability
type HttpStatus : Integer enum {
  OK = 200;
  NotFound = 404;
  ServerError = 500;
}

// Also Good: Implicit values for simple cases
type Color : String enum {
  Red;
  Green;
  Blue;
}
```

### 3. Use Descriptive Names

```cds
// Good: Clear, descriptive names
type OrderStatus : String enum {
  PendingPayment;
  PaymentConfirmed;
  Shipped;
  Delivered;
}

// Avoid: Abbreviations that aren't obvious
type OrderStatus : String enum {
  PP;
  PC;
  SH;
  DL;
}
```

### 4. Group Related Enums

```cds
// types.cds - Centralized enum definitions
namespace my.app.types;

type OrderStatus : String enum { /* ... */ }
type PaymentMethod : String enum { /* ... */ }
type ShippingMethod : String enum { /* ... */ }
```

### 5. Document Your Enums

```cds
/**
 * Order processing status
 */
type OrderStatus : String enum {
  Pending;      // Order placed, awaiting payment
  Processing;   // Payment confirmed, preparing shipment
  Shipped;      // Order shipped to customer
  Delivered;    // Order delivered successfully
}
```

---

## API Reference

### Grammar Rules

```ebnf
EnumDef ::= 'type' ID ':' ID 'enum' '{' EnumValue (';' EnumValue)* ';'? '}'

EnumValue ::= ID ('=' EnumValueLiteral)?

EnumValueLiteral ::= INT | STRING

EnumRef ::= '#' ID
```

### Validation Error Codes

| Code | Description |
|------|-------------|
| `cds.invalid.enum.base` | Enum base type is not String, Integer, or another enum |
| `cds.empty.enum` | Enum has no values (warning, unless inheriting) |
| `cds.duplicate.enum.value` | Duplicate enum value name |
| `cds.enum.value.type` | Enum value type doesn't match base type |
| `cds.enum.ref.unresolved` | Enum value reference cannot be resolved |
| `cds.enum.ref.wrong.type` | Enum reference used with non-enum type |
| `cds.enum.circular.inheritance` | Circular enum inheritance detected |
| `cds.enum.duplicate.inherited` | Enum value already defined in parent enum |
| `cds.enum.super.unresolved` | Enum super type cannot be resolved |
| `cds.enum.reserved.keyword` | Enum value uses a reserved keyword (warning) |
| `cds.enum.similar.names` | Enum values have confusingly similar names (warning) |
| `cds.enum.value.range` | Integer enum value is unusually large or has large gaps (warning) |
| `cds.enum.too.few.values` | Enum has only 1 value (info) |
| `cds.enum.too.many.values` | Enum has >100 values (warning) |
| `cds.enum.unsorted.values` | Integer enum values are not sorted (info) |
| `cds.enum.value.missing.label` | Enum values missing @label annotations (info) |

---

## See Also

- [SAP CAP CDS Documentation](https://cap.cloud.sap/docs/cds/cdl)
- [CDS Eclipse Plugin README](../README.md)
- [Grammar Reference](../plugins/org.example.cds/src/org/example/cds/CDS.xtext)

---

## Changelog

### Version 1.5.0 (Phase 7.4 - Enum Value Annotations)
- ✅ Support for annotations on individual enum values
- ✅ @label, @description, and custom annotations
- ✅ Localized labels with @label.{lang} syntax
- ✅ Complex annotation values (records, arrays)
- ✅ Enhanced hover documentation showing all annotations
- ✅ Info hint for enums missing @label annotations
- ✅ Full annotation display in IDE

### Version 1.4.0 (Phase 7.3 - Utility Features)
- ✅ Enum value count validation (1 value = info, >100 = warning)
- ✅ Integer enum value ordering suggestions
- ✅ Enhanced hover documentation with value counts and ranges
- ✅ Rich hover info for enum values showing parent and explicit assignments
- ✅ Informational hints for code quality improvements

### Version 1.3.0 (Phase 7.2 - Advanced Validation)
- ✅ Reserved keyword detection for enum values
- ✅ Similar name warnings (case-only and underscore variations)
- ✅ Integer value range validation
- ✅ Large gap detection between integer values
- ✅ Enhanced code quality checks

### Version 1.2.0 (Phase 7.1 - Enum Inheritance)
- ✅ Enum inheritance support (extend from other enums)
- ✅ Multi-level enum inheritance
- ✅ Circular inheritance detection
- ✅ Inherited value validation (prevent duplicates)
- ✅ Base type resolution through inheritance chains

### Version 1.1.0 (Phase 7)
- ✅ Initial enum type support
- ✅ String and Integer enum base types
- ✅ Explicit and implicit enum values
- ✅ Enum value references with `#` syntax
- ✅ Complete validation suite
- ✅ Full IDE integration

---

## License

Eclipse Public License 2.0
