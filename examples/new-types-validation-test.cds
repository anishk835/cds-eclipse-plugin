/**
 * New CDS Types - Validation Test Cases
 *
 * Tests for UInt8, Int16, Map types and type-as-projection
 */

namespace test.newtypes;

// ── Test: UInt8 Type Definition ──────────────────────────────────────────

type StatusCode : UInt8;
type Percentage : UInt8;

entity UInt8Test {
  key ID: UUID;

  // ✅ Valid: Direct UInt8 usage
  rating: UInt8;
  score: UInt8 default 0;

  // ✅ Valid: Custom types based on UInt8
  status: StatusCode;
  progress: Percentage default 0;

  // ✅ Valid: UInt8 in expressions
  doubleRating: Integer = rating * 2;
  isHighScore: Boolean = score > 200;
}

// ── Test: Int16 Type Definition ──────────────────────────────────────────

type Quantity : Int16;
type Delta : Int16;

entity Int16Test {
  key ID: UUID;

  // ✅ Valid: Direct Int16 usage
  stock: Int16;
  reserved: Int16 default 0;

  // ✅ Valid: Custom types based on Int16
  quantity: Quantity;
  change: Delta;

  // ✅ Valid: Int16 in expressions (can be negative)
  available: Int16 = stock - reserved;
  isNegative: Boolean = change < 0;
  absoluteChange: Integer = change * (change >= 0 ? 1 : -1);
}

// ── Test: Map Type Definition ────────────────────────────────────────────

entity MapTest {
  key ID: UUID;
  name: String;

  // ✅ Valid: Map for flexible JSON data
  settings: Map;
  metadata: Map;
  preferences: Map;

  // ✅ Valid: Multiple Map fields in same entity
  config: Map;
  tags: Map;
}

// ── Test: Type-as-Projection ─────────────────────────────────────────────

entity Address {
  street: String;
  city: String;
  state: String;
  zipCode: String;
  country: String;
}

// ✅ Valid: Project subset of fields
type ShortAddress : projection on Address {
  city,
  country
};

type USAddress : projection on Address {
  street,
  city,
  state,
  zipCode
};

entity Customer {
  key ID: UUID;
  name: String;

  // ✅ Valid: Use projected types
  location: ShortAddress;
  billing: USAddress;
}

// ── Test: Mixed Type Usage ───────────────────────────────────────────────

entity MixedTypeTest {
  key ID: UUID;

  // Traditional types
  name: String(100);
  price: Decimal(10, 2);
  quantity: Integer;

  // New integer types
  rating: UInt8;
  stock: Int16;

  // Map type
  extra: Map;

  // ✅ Valid: Mix numeric types in expressions
  totalValue: Decimal = price * quantity;
  adjustedStock: Integer = stock + quantity;
  scoreMultiplier: Integer = rating * 10;

  // ✅ Valid: Comparisons across numeric types
  hasStock: Boolean = stock > 0;
  isTopRated: Boolean = rating >= 4;
  isExpensive: Boolean = price > 100.00;
}

// ── Test: Aggregations with New Types ────────────────────────────────────

entity Sales {
  key ID: UUID;
  productID: UUID;
  rating: UInt8;
  quantity: Int16;
  amount: Decimal;
}

entity SalesStats as SELECT from Sales {
  productID,

  // ✅ Valid: Aggregations on UInt8
  AVG(rating) as avgRating: Decimal,
  MIN(rating) as minRating: UInt8,
  MAX(rating) as maxRating: UInt8,

  // ✅ Valid: Aggregations on Int16
  SUM(quantity) as totalQuantity: Integer,
  MIN(quantity) as minQuantity: Int16,
  MAX(quantity) as maxQuantity: Int16,

  // ✅ Valid: Standard aggregations
  COUNT(*) as orderCount: Integer,
  SUM(amount) as totalRevenue: Decimal
}
group by productID;

// ── Test: Default Values with New Types ──────────────────────────────────

entity DefaultsTest {
  key ID: UUID;

  // ✅ Valid: UInt8 defaults
  priority: UInt8 default 128;
  level: UInt8 default 1;

  // ✅ Valid: Int16 defaults
  balance: Int16 default 0;
  offset: Int16 default -100;

  // Traditional defaults for comparison
  name: String default 'Unknown';
  active: Boolean default true;
  count: Integer default 0;
}

// ── Test: Type Compatibility ─────────────────────────────────────────────

entity TypeCompatibilityTest {
  key ID: UUID;

  // Numeric type hierarchy test
  tiny: UInt8;
  small: Int16;
  normal: Integer;
  large: Integer64;
  precise: Decimal;
  floating: Double;

  // ✅ Valid: Smaller types can be used with larger types
  sum1: Integer = tiny + normal;
  sum2: Integer = small + normal;
  sum3: Integer64 = normal + large;
  sum4: Decimal = small + precise;
  sum5: Double = precise + floating;

  // ✅ Valid: Comparisons across numeric types
  check1: Boolean = tiny < normal;
  check2: Boolean = small >= normal;
  check3: Boolean = precise > floating;
}
