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

// ─── Test 8: Virtual with enum default ──────────────────────────────────────

type Status : String enum {
  Active;
  Inactive;
  Pending;
}

entity VirtualWithEnum {
  key ID: UUID;
  virtual currentStatus: Status default #Active;
}

// ─── Test 9: Real-world example ─────────────────────────────────────────────

entity Books {
  key ID: UUID;
  title: String not null;
  price: Decimal not null;
  stock: Integer default 0;

  // Virtual computed fields
  virtual isInStock: Boolean = stock > 0;
  virtual displayTitle: String;
  virtual averageRating: Decimal;
  virtual reviewCount: Integer;
}
