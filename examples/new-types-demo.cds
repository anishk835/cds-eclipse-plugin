/**
 * New CDS Types Demo (CAP 2022+ features)
 *
 * This file demonstrates the new built-in types and type-as-projection
 * functionality added to SAP CAP.
 */

namespace examples.newtypes;

// ═══════════════════════════════════════════════════════════════════════════
// New Integer Types: UInt8 and Int16
// ═══════════════════════════════════════════════════════════════════════════

// UInt8: Unsigned 8-bit integer (0-255)
// Maps to: TINYINT (SQL) / Edm.Byte (OData)
type Rating : UInt8;
type AgeGroup : UInt8;
type Priority : UInt8;

// Int16: Signed 16-bit integer (-32768 to 32767)
// Maps to: SMALLINT (SQL) / Edm.Int16 (OData)
type Stock : Int16;
type YearOffset : Int16;
type SmallQuantity : Int16;

entity Products {
  key ID: UUID;
  name: String(100);

  // Using UInt8 for small numeric ranges
  rating: Rating default 0;          // 0-5 stars
  priority: Priority default 128;    // 0-255 priority levels

  // Using Int16 for inventory
  stock: Stock default 0;            // Can be negative for backorders
  reserved: SmallQuantity default 0;

  // Traditional types for comparison
  price: Decimal(10, 2);
  quantity: Integer;
}

// ✅ Valid: UInt8 and Int16 work in expressions
entity Calculations {
  key ID: UUID;
  rating: UInt8;
  stock: Int16;
  multiplier: Integer;

  // Numeric operations with new integer types
  adjustedRating: Integer = rating * 20;     // UInt8 → Integer
  stockValue: Integer = stock * multiplier;  // Int16 * Integer → Integer
  isLowStock: Boolean = stock < 10;          // Int16 comparison
  hasHighRating: Boolean = rating >= 4;      // UInt8 comparison
}

// ═══════════════════════════════════════════════════════════════════════════
// New Map Type (for arbitrary JSON-like objects)
// ═══════════════════════════════════════════════════════════════════════════

// Map type stores arbitrary JSON-like objects
// Maps to: NCLOB (HANA), JSON_TEXT (SQLite), JSON (H2), JSONB (Postgres)

entity Person {
  key ID: UUID;
  name: String;

  // Arbitrary metadata as JSON
  details: Map;

  // Can store:
  // - User preferences
  // - Dynamic attributes
  // - Configuration data
  // - Extensible properties
}

entity Configuration {
  key ID: UUID;
  name: String;

  // Store configuration as flexible JSON
  settings: Map;
  metadata: Map;
}

// Note: Current limitations on Map type:
// - Partial updates not yet fully supported
// - Filtering on Map elements has limited support
// - Deletes may have restrictions depending on runtime

// ═══════════════════════════════════════════════════════════════════════════
// Type-as-Projection (define types based on entity projections)
// ═══════════════════════════════════════════════════════════════════════════

// Base entity with many fields
entity FullName {
  firstName: String @label: 'First Name';
  middleName: String @label: 'Middle Name';
  lastName: String @label: 'Last Name';
  initials: String @label: 'Initials';
  title: String @label: 'Title';
  suffix: String @label: 'Suffix';
}

// Create a lightweight type by projecting only needed fields
type ShortName : projection on FullName {
  firstName,
  lastName
};

type FormalName : projection on FullName {
  title,
  firstName,
  lastName,
  suffix
};

// Use projection-based types in entities
entity Author {
  key ID: UUID;
  name: ShortName;         // Only firstName and lastName
  bio: String(1000);
}

entity Contact {
  key ID: UUID;
  displayName: FormalName; // title, firstName, lastName, suffix
  email: String;
}

// ═══════════════════════════════════════════════════════════════════════════
// Complex Examples Combining New Types
// ═══════════════════════════════════════════════════════════════════════════

entity ReviewSystem {
  key ID: UUID;
  productID: UUID;

  // Use UInt8 for ratings (0-5 scale)
  overallRating: UInt8;
  qualityRating: UInt8;
  valueRating: UInt8;
  serviceRating: UInt8;

  // Store reviewer details as Map for flexibility
  reviewer: Map;  // { name, location, verified, badges, etc. }

  // Store review metadata as Map
  metadata: Map;  // { helpful_count, report_count, featured, etc. }

  reviewText: String(5000);
  createdAt: Timestamp;
}

entity Inventory {
  key ID: UUID;
  productID: UUID;
  warehouseID: String;

  // Use Int16 for stock levels (can be negative)
  onHand: Int16 default 0;
  allocated: Int16 default 0;
  onOrder: Int16 default 0;

  // Calculated available quantity
  available: Int16 = onHand - allocated;

  // Store warehouse-specific settings as Map
  warehouseConfig: Map;

  lastUpdated: Timestamp;
}

// ═══════════════════════════════════════════════════════════════════════════
// Migration Notes
// ═══════════════════════════════════════════════════════════════════════════

/*
When to use UInt8:
- Small positive integers (0-255)
- Ratings, priorities, percentages, age groups
- Flags, status codes, enum-like values
- Reduces storage compared to Integer

When to use Int16:
- Medium-range integers (-32768 to 32767)
- Stock quantities, year offsets, small counts
- Values that can be negative
- Reduces storage compared to Integer

When to use Map:
- Flexible/extensible attributes
- User preferences or settings
- Dynamic metadata
- When structure varies per record
- Migration from schema-less systems

When to use type-as-projection:
- Create reusable structured types from existing entities
- Reduce boilerplate for common field combinations
- Maintain consistency across similar structures
- Inherit annotations from source entity fields

Prerequisites:
- CDS compiler with support for these types
- CAP runtime that supports the types
- Database that supports the mapped SQL types
*/
