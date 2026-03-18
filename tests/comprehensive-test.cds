// ============================================================================
// COMPREHENSIVE CDS TEST FILE - ALL TEST SCENARIOS
// ============================================================================
// This file consolidates all test scenarios for the CDS Eclipse Plugin
// ============================================================================

namespace test.comprehensive;

// ============================================================================
// SECTION 1: Find Usages Tests
// ============================================================================

// Base entity - Target for find references
entity Product {
  key ID: UUID;
  name: String;
  description: String;
  price: Decimal;
}

// Reference 1: Association
entity Order {
  key ID: UUID;
  product: Association to Product;
  quantity: Integer;
}

// Reference 2: Projection
entity ProductView as projection on Product {
  ID, name, description
}

// Reference 3: SELECT FROM
entity ProductList as SELECT from Product {
  ID, name, price
}
where price > 0
order by name asc;

// Reference 4: Service Projection
service CatalogService {
  entity Products as projection on Product {
    ID, name, price
  };
}

// Reference 5: Extend
extend Product with {
  category: String;
  brand: String;
}

// Reference 6: Annotate
annotate Product with @readonly;

// ============================================================================
// SECTION 2: Type References Tests
// ============================================================================

type Currency = String(3);

entity PricedItem {
  price: Decimal;
  currency: Currency;  // Reference to Currency type
}

// ============================================================================
// SECTION 3: Aspect References Tests
// ============================================================================

aspect Managed {
  createdAt: DateTime;
  modifiedAt: DateTime;
}

entity ManagedProduct : Managed {
  key ID: UUID;
  name: String;
}

// ============================================================================
// SECTION 4: Enum References Tests
// ============================================================================

type Status : String enum {
  Active;
  Inactive;
  Deleted;
}

entity Item {
  key ID: UUID;
  status: Status;  // Reference to Status enum
}

// ============================================================================
// SECTION 5: Key Constraints Tests (Phase 8)
// ============================================================================

entity SingleKey {
  key ID: UUID;
  name: String;
}

entity CompositeKey {
  key orderID: UUID;
  key lineNo: Integer;
  quantity: Integer;
}

entity NoKey {
  name: String;
  description: String;
}

entity KeyOnAssoc {
  key partner: Association to SingleKey;
}

// ============================================================================
// SECTION 6: Navigation Tests - All Cases
// ============================================================================

entity Category {
  key ID: Integer;
  name: String;
}

// Test 1: Association Navigation
entity OrderWithNav {
  key ID: Integer;
  product: Association to Product;  // F3 on "Product" should navigate
}

// Test 2: Projection Navigation
entity ProductViewNav as projection on Product {
  ID, name
}

// Test 3: SELECT FROM Navigation (FIXED!)
entity ProductListNav as SELECT from Product {
  ID, name, description
}
where price > 0;

// Test 4: JOIN Navigation (FIXED!)
entity ProductsWithCategory as SELECT from Product {
  ID, name, price
}
inner join Category on Category.ID = Product.ID;

// Test 5: Type-as-Projection Navigation (FIXED!)
type ProductData : projection on Product {
  name, description
};

// Test 6: Extend Navigation
extend Product with {
  categoryNav: Association to Category;
}

// Test 7: Annotate Navigation
annotate Product with @readonly;
annotate Category with @readonly;

// ============================================================================
// SECTION 7: Cross-Reference Tests
// ============================================================================

// Using statement test (requires actual file, placeholder here)
// using CommonConfigurationService from './CommonConfigurationService';

// Annotation with qualified names
annotate Product with @Capabilities: {
  SortRestrictions: {
    $Type: 'Capabilities.SortRestrictionsType',
    NonSortableProperties: [ID, name]
  }
} {
  name @title: '{i18n>ProductName}';
  description @title: '{i18n>ProductDescription}';
};

// ============================================================================
// SECTION 8: Data Constraints Tests (Phase 9)
// ============================================================================

entity ConstrainedEntity {
  key ID: UUID not null;
  email: String not null unique;
  age: Integer check age >= 18;
  status: String default 'active';
  count: Integer check count >= 0 and count <= 100;
}

// ============================================================================
// SECTION 9: Virtual Elements Tests (Phase 10)
// ============================================================================

entity VirtualEntity {
  key ID: UUID;
  firstName: String;
  lastName: String;
  virtual fullName: String;
  virtual displayName: String;
}

// ============================================================================
// SECTION 10: Localized Elements Tests (Phase 11)
// ============================================================================

entity LocalizedEntity {
  key ID: UUID;
  code: String(10) not null;
  localized name: String(100);
  localized description: String(1000);
}

// ============================================================================
// SECTION 11: Actions & Functions Tests (Phase 13)
// ============================================================================

entity EntityWithActions {
  key ID: UUID;
  status: String;

  // Bound action
  action activate() returns Boolean;

  // Bound function
  function isActive() returns Boolean;

  // With parameters
  action updateStatus(newStatus: String) returns Boolean;

  // Structured return type
  function getInfo() returns {
    id: UUID;
    status: String;
    active: Boolean;
  };
}

service ActionService {
  entity Items as projection on EntityWithActions;

  // Unbound action
  action refreshAll() returns Integer;

  // Unbound function
  function countActive() returns Integer;
}

// ============================================================================
// SECTION 12: Advanced Queries Tests (Phases 14-17)
// ============================================================================

// Views with SELECT
entity BooksInStock as SELECT from Product {
  ID, name, price
}
where price > 0
order by name asc;

// GROUP BY
entity ProductStats as SELECT from Product {
  category,
  COUNT(ID) as total
}
group by category;

// JOINs
entity ProductsWithDetails as SELECT from Product {
  ID, name, price
}
inner join Category as c on c.ID = Product.ID
where Product.price > 10;

// Aggregations
entity CategoryStats as SELECT from Product {
  category,
  COUNT(ID) as bookCount,
  AVG(price) as avgPrice,
  SUM(price) as totalPrice
}
group by category
having bookCount > 3;

// IN operator
entity ActiveProducts as SELECT from Product {
  ID, name
}
where status in (#Active, #Available);

// BETWEEN operator
entity MidPriceProducts as SELECT from Product {
  ID, name, price
}
where price between 10 and 50;

// IS NULL
entity ProductsWithCategory as SELECT from Product {
  ID, name
}
where category is not null;

// ============================================================================
// SECTION 13: Array & Structured Types Tests (Phase 15)
// ============================================================================

type Address {
  street: String(100);
  city: String(50);
  zipCode: String(10);
  country: String(2);
}

entity Customer {
  key ID: UUID;
  name: String(100);

  // Array of simple type
  emails: array of String(100);

  // Array of structured type
  addresses: array of Address;

  // Inline structured type
  dimensions: {
    width: Decimal(10,2);
    height: Decimal(10,2);
    depth: Decimal(10,2);
  };
}

// ============================================================================
// SECTION 14: Built-in Functions Tests (Phase 22A)
// ============================================================================

entity FunctionTests as SELECT from Product {
  // String functions
  UPPER(name) as upperName,
  LOWER(name) as lowerName,
  CONCAT('Product: ', name) as displayName,
  SUBSTRING(name, 1, 10) as shortName,
  LENGTH(name) as nameLength,
  TRIM(description) as cleanDesc,

  // Numeric functions
  ROUND(price, 2) as roundedPrice,
  FLOOR(price) as floorPrice,
  CEIL(price) as ceilPrice,
  ABS(price) as absPrice,

  // Date/Time functions
  CURRENT_DATE() as today,
  CURRENT_TIMESTAMP() as now
};

// ============================================================================
// SECTION 15: CASE/CAST/excluding Tests (Phase 22B)
// ============================================================================

// CASE expressions
entity ProductCategories as SELECT from Product {
  ID,
  name,
  CASE
    WHEN price < 10 THEN 'Budget'
    WHEN price < 30 THEN 'Standard'
    ELSE 'Premium'
  END as priceCategory
};

// CAST expressions
entity ProductDisplay as SELECT from Product {
  CAST(price AS Integer) as priceInt,
  CONCAT('$', CAST(price AS String)) as priceDisplay
};

// excluding clause
entity PublicProducts as SELECT from Product {
  * excluding { description }
};

// ============================================================================
// SECTION 16: Complex Combined Tests
// ============================================================================

entity ComplexView as SELECT from Product {
  * excluding { description },
  UPPER(name) as displayName,
  CASE
    WHEN price < 10 THEN 'Budget'
    WHEN price < 30 THEN 'Standard'
    ELSE 'Premium'
  END as category,
  ROUND(price * 0.9, 2) as salePrice,
  CONCAT('Product #', CAST(ID AS String)) as productCode,
  CURRENT_TIMESTAMP() as queryTime
}
where price > 0 and price between 5 and 100
order by price desc;

// ============================================================================
// TEST INSTRUCTIONS
// ============================================================================
//
// HOW TO TEST:
//
// 1. FIND USAGES (Ctrl+Shift+G):
//    - Place cursor on "Product" at line 10
//    - Press Ctrl+Shift+G
//    - Expected: 6+ references shown in Search view
//
// 2. NAVIGATION (F3):
//    - Place cursor on any entity/type reference
//    - Press F3
//    - Expected: Navigate to definition
//
// 3. KEY CONSTRAINTS:
//    - Check that "key ID" is recognized
//    - Check composite keys in CompositeKey
//    - Check validation warnings on NoKey entity
//
// 4. VALIDATION:
//    - Check constraints are validated (not null, unique, check)
//    - Check virtual elements work without errors
//    - Check localized elements are recognized
//
// 5. QUERIES:
//    - Verify SELECT, JOIN, GROUP BY, ORDER BY parse correctly
//    - Check built-in functions are recognized
//    - Check CASE and CAST expressions work
//
// 6. BUILD:
//    - Run: mvn clean compile
//    - Expected: BUILD SUCCESS
//
// ============================================================================
// END OF TEST FILE
// ============================================================================
