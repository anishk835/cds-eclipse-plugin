/**
 * Phase 18: Type System Validation Examples
 *
 * This file demonstrates the type checking capabilities added in Phase 18.
 * The type system catches type errors at compile time.
 */

namespace bookshop;

// ── Valid Type Operations ─────────────────────────────────────────────────

entity Products {
  key ID: UUID;
  name: String(100);
  price: Decimal(10, 2);
  quantity: Integer;

  // ✅ Valid: Decimal * Integer → Decimal
  totalValue: Decimal = price * quantity;

  // ✅ Valid: Integer + Integer → Integer
  doubleQuantity: Integer = quantity + quantity;

  // ✅ Valid: Boolean and Boolean → Boolean
  inStock: Boolean;
  available: Boolean;
  canOrder: Boolean = inStock and available;
}

// ── Numeric Type Promotion ────────────────────────────────────────────────

entity Calculations {
  key ID: UUID;

  // Integer → Integer64 → Decimal → Double (automatic promotion)
  smallNum: Integer;
  largeNum: Integer64;
  preciseNum: Decimal;

  // ✅ Valid: Integer + Decimal → Decimal
  mixedSum: Decimal = smallNum + preciseNum;

  // ✅ Valid: comparing numeric types
  isLarger: Boolean = smallNum > preciseNum;
}

// ── Invalid Type Operations (Would Error) ────────────────────────────────

// Uncomment to see type errors:

/*
entity TypeErrors {
  key ID: UUID;
  name: String;
  price: Decimal;
  active: Boolean;

  // ❌ ERROR: Can't add Decimal + String
  // wrong1: Decimal = price + name;

  // ❌ ERROR: Can't use 'and' with Integer
  // wrong2: Boolean = price and 10;

  // ❌ ERROR: Can't negate a String
  // wrong3: String = -name;

  // ❌ ERROR: 'not' requires Boolean
  // wrong4: Boolean = not price;

  // ⚠️  WARNING: Comparing incompatible types
  // suspicious: Boolean = name = price;
}
*/

// ── Aggregation Type Inference ────────────────────────────────────────────

entity Sales {
  key ID: UUID;
  productID: UUID;
  amount: Decimal;
  quantity: Integer;
}

entity SalesStats as SELECT from Sales {
  productID,

  // ✅ COUNT returns Integer
  COUNT(*) as totalOrders: Integer,

  // ✅ SUM returns same type as argument (Decimal)
  SUM(amount) as totalRevenue: Decimal,

  // ✅ AVG returns Decimal
  AVG(amount) as avgOrderValue: Decimal,

  // ✅ MIN/MAX return same type as argument
  MIN(quantity) as minQuantity: Integer,
  MAX(quantity) as maxQuantity: Integer
}
group by productID;

// ── Default Value Type Checking ───────────────────────────────────────────

entity Settings {
  key ID: UUID;

  // ✅ Valid: Integer default
  maxRetries: Integer default 3;

  // ✅ Valid: String default
  status: String default 'active';

  // ✅ Valid: Boolean default
  enabled: Boolean default true;

  // Uncomment to see type error:
  // ❌ ERROR: Can't assign String to Integer
  // wrongDefault: Integer default 'not a number';
}

// ── Complex Expressions ───────────────────────────────────────────────────

entity ComplexCalculations {
  key ID: UUID;
  basePrice: Decimal;
  taxRate: Decimal;
  discount: Decimal;
  quantity: Integer;

  // ✅ Valid: nested arithmetic operations
  subtotal: Decimal = basePrice * quantity;
  tax: Decimal = subtotal * taxRate;
  discountAmount: Decimal = subtotal * discount;
  finalPrice: Decimal = subtotal + tax - discountAmount;

  // ✅ Valid: comparison returns Boolean
  isFreeShipping: Boolean = finalPrice > 50.0;

  // ✅ Valid: logical operations
  hasDiscount: Boolean = discount > 0.0;
  hasLargeOrder: Boolean = quantity > 10;
  qualifiesForBonus: Boolean = hasDiscount and hasLargeOrder;
}
