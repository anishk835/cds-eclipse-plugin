/**
 * Sample SAP CAP CDS model — covers Phases 1-17 constructs.
 * Use this file to smoke-test the editor after installation.
 *
 * Phase 16: Enhanced Validation
 * - JOIN validation (see BooksWithAuthors)
 * - Circular dependency detection (see Orders/OrderItems)
 * - Constraint conflict detection (see ValidationExamples)
 * - Aggregation validation (see BookStatsByGenre)
 *
 * Phase 17: Advanced Query Features
 * - JOINs (INNER, LEFT, RIGHT, FULL)
 * - Aggregation functions (COUNT, SUM, AVG, MIN, MAX)
 * - Advanced operators (IN, BETWEEN, IS NULL)
 * - UNION operations
 */

namespace my.bookshop;

using my.common.Types;

// ─── Aspects ─────────────────────────────────────────────────────────────────

aspect Timestamped {
  createdAt  : DateTime;
  modifiedAt : DateTime;
}

aspect Managed : Timestamped {
  createdBy  : String(100);
  modifiedBy : String(100);
}

// ─── Types ───────────────────────────────────────────────────────────────────

type Currency = String(3);
type Amount   = Decimal(15,2);

// Phase 15: Structured Types
type Address {
  street  : String(100);
  city    : String(50);
  zipCode : String(10);
  country : String(2);
}

// ─── Enums ───────────────────────────────────────────────────────────────────

type BookStatus : String enum {
  Available;
  Reserved;
  CheckedOut;
  Lost;
}

type Priority : Integer enum {
  Low = 1;
  Medium = 2;
  High = 3;
  Critical = 4;
}

// ─── Enum Inheritance (Phase 7) ──────────────────────────────────────────────

// Base status enum
type BaseStatus : String enum {
  Active;
  Inactive;
}

// Extended status inheriting from BaseStatus
type ExtendedStatus : BaseStatus enum {
  Pending;
  Cancelled;
}

// Multi-level inheritance
type Level1Status : String enum { Draft; }
type Level2Status : Level1Status enum { Review; }
type Level3Status : Level2Status enum { Published; }

// ─── Entities ────────────────────────────────────────────────────────────────

@UI.HeaderInfo: { TypeName: 'Author', TypeNamePlural: 'Authors' }
entity Authors : Managed {
  key ID   : Integer not null;
  name     : String(100) not null;
  dateOfBirth : Date;
  placeOfBirth: String(150);
  dateOfDeath : Date;
}

@UI.HeaderInfo: { TypeName: 'Book', TypeNamePlural: 'Books' }
entity Books : Managed {
  key ID    : UUID not null;
  localized title     : String(111) not null;
  localized descr     : String(1111);
  author    : Association to Authors;
  genre     : Association to Genres;
  stock     : Integer check stock >= 0;
  price     : Amount not null check price > 0;
  currency  : Currency default 'USD';
  grossPrice: Amount;
  tax       : Amount;
  netPrice  : Amount = grossPrice - tax;
  status    : BookStatus default #Available not null;

  // Phase 10: Virtual elements
  virtual averageRating: Decimal;
  virtual reviewCount: Integer;
  virtual isAvailable: Boolean = stock > 0;

  // Phase 13: Actions and Functions
  action reserve(userID: UUID) returns Boolean;
  function getAvailability() returns String;
  action checkout(userID: UUID, dueDate: Date) returns {
    success: Boolean;
    dueDate: Date;
    fine: Amount;
  };
}

entity Genres {
  key ID   : Integer not null;
  name     : String(100) not null unique;
  parent   : Association to Genres;
  children : Composition of many Genres;
}

// ─── Phase 9: Users entity with constraints ──────────────────────────────────

entity Users : Managed {
  key ID      : UUID not null;
  email       : String(100) not null unique;
  age         : Integer check age >= 18;
  status      : String default 'active' not null;
  country     : String(2);

  // Phase 15: Array and structured types
  phoneNumbers: array of String(20);
  address: Address;

  // Phase 13: User actions
  action activate() returns Boolean;
  action deactivate(reason: String) returns Boolean;
  function getFullProfile() returns {
    email: String;
    status: String;
    memberSince: DateTime;
  };
}

// ─── Services ────────────────────────────────────────────────────────────────

@Core.Description: 'Public catalog service'
service CatalogService {
  @readonly
  entity Books as projection on Books {
    ID, title, descr, price, currency, stock
  }

  @readonly
  entity Authors as projection on Authors {
    ID, name
  }

  // Phase 13: Service-level actions and functions
  function searchBooks(query: String) returns Integer;
  action resetInventory() returns Boolean;
}

service AdminService {
  entity Books   as projection on Books;
  entity Authors as projection on Authors;
  entity Genres  as projection on Genres;
}

// ─── Phase 14: Views and SELECT Queries ──────────────────────────────────────

// View: Books in stock
entity BooksInStock as SELECT from Books {
  ID,
  title,
  price,
  stock
}
where stock > 0
order by title asc;

// View: Author statistics
entity AuthorStats as SELECT from Books {
  author,
  stock as totalBooks
}
group by author;

// View: Recent books
entity RecentBooks as SELECT from Books {
  ID,
  title,
  createdAt
}
order by createdAt desc;

// ─── Phase 17: Advanced Query Features ───────────────────────────────────────

// View with JOIN (Phase 17 + Phase 16 validation)
// Phase 16: Validates JOIN target is an entity, validates ON condition exists
entity BooksWithAuthors as SELECT from Books {
  ID,
  title,
  price
}
inner join Authors as a on a.ID = author
where stock > 0
order by title asc;

// View with aggregation (Phase 17 + Phase 16 validation)
// Phase 16: Validates aggregation usage, suggests GROUP BY when needed
entity BookStatsByGenre as SELECT from Books {
  genre,
  COUNT(ID) as bookCount,
  AVG(price) as avgPrice,
  SUM(stock) as totalStock
}
group by genre
having bookCount > 3
order by bookCount desc;

// View with IN operator (Phase 17)
entity PremiumBooks as SELECT from Books {
  ID,
  title,
  price
}
where status in (#Available, #Reserved)
  and price > 50
order by price desc;

// View with BETWEEN operator (Phase 17)
entity MidPriceBooks as SELECT from Books {
  ID,
  title,
  price
}
where price between 10 and 50
order by price asc;

// View with IS NULL (Phase 17)
entity BooksWithoutAuthors as SELECT from Books {
  ID,
  title
}
where author is null;

// View with aggregation and HAVING (Phase 17)
entity PopularAuthors as SELECT from Books {
  author,
  COUNT(ID) as bookCount
}
group by author
having bookCount > 3
order by bookCount desc;

// View with COUNT DISTINCT (Phase 17)
entity GenreStatistics as SELECT from Books {
  COUNT(distinct author) as uniqueAuthors,
  COUNT(ID) as totalBooks
};

// ─── Extensions ──────────────────────────────────────────────────────────────

extend Books with {
  reviews : Composition of many Reviews;
}

entity Reviews {
  key ID     : UUID;
  book       : Association to Books;
  rating     : Integer;
  comment    : String(1000);
}

// ─── Phase 16: Validation Examples ───────────────────────────────────────────

// Example 1: Constraint conflicts (Phase 16 validation)
entity ValidationExamples {
  key ID: UUID;

  // ℹ️ Info: not null with default is redundant (but valid)
  // Phase 16 validator will provide an info message
  email: String(100) not null default 'unknown@example.com';

  // Valid: unique with default
  username: String(50) unique default 'anonymous';

  // ⚠️ Warning: virtual with not null may cause issues
  // Phase 16 validator will warn about this combination
  // virtual computed: Integer not null;  // Uncomment to see warning

  // Valid constraint combinations
  status: String(20) default 'active' not null;
  priority: Integer check priority >= 1 and priority <= 5;
}

// Example 2: Circular dependencies (Phase 16 validation)
entity Orders {
  key ID: UUID;
  customer: Association to Users;
  items: Composition of many OrderItems;  // ⚠️ Circular: Orders → OrderItems → Orders
}

entity OrderItems {
  key ID: UUID;
  order: Association to Orders;  // ⚠️ Circular reference detected
  book: Association to Books;
  quantity: Integer;
  price: Amount;
}

// Example 3: Self-referencing (valid circular pattern)
// Phase 16 detects but allows (common pattern for hierarchies)
entity Categories {
  key ID: Integer;
  name: String(100) not null;
  parent: Association to Categories;  // Self-reference
  children: Composition of many Categories;
}

// ─── Annotations ─────────────────────────────────────────────────────────────

annotate Books with @readonly;

annotate CatalogService.Books with @UI.LineItem: [
  { Value: title,    Label: 'Title'  },
  { Value: price,    Label: 'Price'  },
  { Value: currency, Label: 'Curr.'  },
  { Value: stock,    Label: 'Stock'  }
];
