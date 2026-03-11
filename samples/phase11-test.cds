/**
 * Phase 11 Test - Localized Data
 * Tests localized modifier for multilingual fields
 */

namespace test.phase11;

// ─── Test 1: Simple localized element ────────────────────────────────────────

entity SimpleLocalized {
  key ID: UUID;
  localized name: String(100);
}

// ─── Test 2: Multiple localized elements ─────────────────────────────────────

entity MultipleLocalized {
  key ID: UUID;
  localized name: String(100);
  localized description: String(1000);
  price: Decimal;
}

// ─── Test 3: Localized with constraints ──────────────────────────────────────

entity LocalizedWithConstraints {
  key ID: UUID;
  localized name: String(100) not null;
  localized description: String(1000);
  localized title: String(200) not null unique;
}

// ─── Test 4: Real-world product catalog ──────────────────────────────────────

entity Products {
  key ID: UUID not null;
  localized name: String(200) not null;
  localized description: LargeString;
  localized shortDesc: String(500);

  // Non-localized fields
  price: Decimal(9,2) not null;
  currency: String(3) default 'USD';
  sku: String(50) unique;
  stock: Integer default 0;

  virtual displayName: String;
}

// ─── Test 5: Books with localized content ────────────────────────────────────

entity Books {
  key ID: UUID;
  localized title: String(200) not null;
  localized description: LargeString;

  author: String(100);
  isbn: String(13) unique;
  price: Decimal(9,2);

  virtual isAvailable: Boolean;
}

// ─── Test 6: Categories with localized names ─────────────────────────────────

entity Categories {
  key ID: UUID;
  localized name: String(100) not null;
  localized description: String(500);

  parent: Association to Categories;
}

// ─── Test 7: Localized in service projection ─────────────────────────────────

service CatalogService {
  entity Products as projection on Products {
    ID, name, description, price
  }

  entity Books as projection on Books {
    ID, title, author, price
  }
}

// ─── Test 8: Complex multilingual application ────────────────────────────────

type Status : String enum {
  Active;
  Inactive;
}

entity Articles {
  key ID: UUID not null;

  // Localized content
  localized headline: String(200) not null;
  localized teaser: String(500);
  localized body: LargeString;

  // Metadata (not localized)
  author: String(100);
  publishedAt: DateTime;
  status: Status default #Active;

  // Virtual fields
  virtual wordCount: Integer;
  virtual readingTime: Integer;
}

// ─── Test 9: E-commerce example ──────────────────────────────────────────────

entity ProductCatalog {
  key ID: UUID;

  // Localized for international markets
  localized productName: String(200) not null;
  localized description: LargeString;
  localized specifications: String(2000);
  localized warranty: String(1000);

  // Not localized
  sku: String(50) not null unique;
  price: Decimal(10,2) not null check price > 0;
  weight: Decimal(8,2);
  dimensions: String(50);

  // Status
  status: Status default #Active;

  // Computed
  virtual displayPrice: String;
}
