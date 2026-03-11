/**
 * Phase 9 Test - Data Constraints
 * Tests not null, unique, check, and default constraints
 */

namespace test.phase9;

// ─── Test 1: Simple not null constraint ──────────────────────────────────────

entity SimpleNotNull {
  key ID: UUID;
  name: String not null;
}

// ─── Test 2: Unique constraint ───────────────────────────────────────────────

entity SimpleUnique {
  key ID: UUID;
  email: String unique;
}

// ─── Test 3: Multiple constraints ────────────────────────────────────────────

entity MultipleConstraints {
  key ID: UUID not null;
  email: String not null unique;
  username: String unique not null;
}

// ─── Test 4: Check constraints ───────────────────────────────────────────────

entity CheckConstraints {
  key ID: UUID;
  age: Integer check age >= 18;
  price: Decimal check price > 0;
  discount: Decimal check discount >= 0 and discount <= 100;
}

// ─── Test 5: Default values ──────────────────────────────────────────────────

entity DefaultValues {
  key ID: UUID;
  status: String default 'active';
  priority: Integer default 1;
  createdAt: DateTime default $now;
}

// ─── Test 6: Combined constraints and defaults ───────────────────────────────

entity Combined {
  key ID: UUID not null;
  email: String not null unique;
  status: String default 'pending' not null;
  age: Integer check age >= 18;
  level: Integer default 1 check level >= 1 and level <= 10;
}

// ─── Test 7: Key with constraints ────────────────────────────────────────────

entity KeyWithConstraints {
  key ID: UUID not null;
  key tenantID: String not null;
}

// ─── Test 8: Enum with default ───────────────────────────────────────────────

type Status : String enum {
  Active;
  Inactive;
  Pending;
}

entity EntityWithEnumDefault {
  key ID: UUID;
  status: Status default #Active not null;
}
