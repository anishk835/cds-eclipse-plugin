/**
 * Phase 20: Foreign Key Validation Examples
 *
 * This file demonstrates the foreign key and association validation
 * added in Phase 20.
 */

namespace bookshop;

// ── Managed Associations (Automatic Foreign Keys) ─────────────────────────

// ✅ Managed associations: CDS generates foreign keys automatically
entity Authors {
  key ID: UUID;
  name: String(100);
  country: String(2);
}

entity Books {
  key ID: UUID;
  title: String(200);

  // ✅ Managed association: CDS generates author_ID foreign key field
  author: Association to Authors;

  // ℹ️  INFO: Target has key, managed association will work
}

// ── Unmanaged Associations (Explicit ON Conditions) ───────────────────────

entity Publishers {
  key ID: UUID;
  name: String(100);
}

entity BookPublications {
  key ID: UUID;
  bookID: UUID;
  publisherID: UUID;

  // ✅ Unmanaged association: explicit foreign key mapping
  book: Association to Books on book.ID = bookID;
  publisher: Association to Publishers on publisher.ID = publisherID;
}

// ── Type-Safe ON Conditions ────────────────────────────────────────────────

entity Categories {
  key code: String(10);
  name: String(100);
}

entity Products {
  key ID: UUID;
  name: String(100);
  categoryCode: String(10);

  // ✅ Type-safe: String = String in ON condition
  category: Association to Categories on category.code = categoryCode;
}

// Uncomment to see type error:
/*
entity InvalidProduct {
  key ID: UUID;
  categoryCode: Integer;  // Wrong type

  // ❌ ERROR: ON condition compares incompatible types (String with Integer)
  // category: Association to Categories on category.code = categoryCode;
}
*/

// ── Composite Keys ─────────────────────────────────────────────────────────

entity Countries {
  key code: String(2);      // Part 1 of composite key
  key region: String(10);   // Part 2 of composite key
  name: String(100);
}

entity Cities {
  key ID: UUID;
  name: String(100);

  // ℹ️  INFO: Association to entity with composite key (2 fields)
  // CDS will generate countryCode and countryRegion foreign key fields
  country: Association to Countries;
}

// ✅ Explicit composite key mapping
entity Addresses {
  key ID: UUID;
  street: String(200);
  countryCode: String(2);
  countryRegion: String(10);

  // ✅ Explicit ON condition for composite key
  country: Association to Countries
    on country.code = countryCode
    and country.region = countryRegion;
}

// ── Bidirectional Associations ─────────────────────────────────────────────

// ✅ Bidirectional managed associations (consistent)
entity Customers {
  key ID: UUID;
  name: String(100);

  // ✅ One-to-many: customer has many orders
  orders: Association to many Orders on orders.customer = $self;
}

entity Orders {
  key ID: UUID;
  orderNumber: String(20);

  // ✅ Many-to-one: order belongs to customer
  customer: Association to Customers;
}

// ℹ️  INFO: Both sides use consistent approach (managed/unmanaged)

// ── Association to Many ────────────────────────────────────────────────────

entity Departments {
  key ID: UUID;
  name: String(50);

  // ℹ️  INFO: Association to many without ON condition
  // CDS will generate foreign key automatically in Employees table
  employees: Association to many Employees on employees.department = $self;
}

entity Employees {
  key ID: UUID;
  name: String(100);

  // ✅ Backlink to department
  department: Association to Departments;
}

// ── Self-References (Hierarchical Data) ───────────────────────────────────

// ✅ Self-reference with managed association
entity Folder {
  key ID: UUID;
  name: String(100);

  // ✅ Hierarchical: folder can have parent folder
  parent: Association to Folder;

  // CDS generates parent_ID foreign key field
}

// ✅ Self-reference with explicit ON condition
entity OrgUnit {
  key ID: UUID;
  name: String(100);
  parentID: UUID;

  // ✅ Explicit parent relationship
  parent: Association to OrgUnit on parent.ID = parentID;
}

// ── Association Chains ─────────────────────────────────────────────────────

// ✅ Multi-level associations
entity OrderItems {
  key ID: UUID;
  order: Association to Orders;      // Level 1
  product: Association to Products;  // Level 1
  quantity: Integer;
  price: Decimal(15, 2);
}

// Can navigate: OrderItem → Order → Customer
//              OrderItem → Product → Category

// ── Custom Key Types ───────────────────────────────────────────────────────

// ✅ Using custom types for keys
type CustomerID : UUID;
type ProductID : UUID;
type OrderID : UUID;

entity TypedCustomers {
  key ID: CustomerID;
  name: String(100);
}

entity TypedOrders {
  key ID: OrderID;
  orderNumber: String(20);

  // ✅ Association using custom key type
  customer: Association to TypedCustomers;
}

// ── Invalid Associations (Commented Out) ──────────────────────────────────

/*
// Uncomment to see validation errors:

entity InvalidExamples {
  key ID: UUID;

  // ❌ ERROR: ON condition type mismatch
  // target has Integer key, but we compare with String
  // badAssoc1: Association to SomeEntity on target.keyField = "wrong";

  // ⚠️  WARNING: Target has no key defined
  // badAssoc2: Association to EntityWithoutKey;

  // ⚠️  WARNING: ON condition doesn't reference any fields
  // badAssoc3: Association to Authors on true;
}

entity EntityWithoutKey {
  name: String;  // No key field
}
*/

// ── Service Exposure with Associations ─────────────────────────────────────

service CatalogService {
  // ✅ All associations are preserved in projections
  entity BookView as projection on Books {
    *,
    author.name as authorName,
    author.country as authorCountry
  }

  entity OrderView as projection on Orders {
    *,
    customer.name as customerName,
    COUNT(items) as itemCount
  } excluding { customer }  // Hide customer association
}

// ── Composition (Contained Associations) ───────────────────────────────────

entity ShoppingCarts {
  key ID: UUID;
  customer: Association to Customers;

  // ✅ Composition: cart owns items (cascade delete)
  items: Composition of many CartItems on items.cart = $self;
}

entity CartItems {
  key ID: UUID;
  cart: Association to ShoppingCarts;  // Back-reference
  product: Association to Products;
  quantity: Integer;
}

// ── Association with Filters ───────────────────────────────────────────────

entity Reviews {
  key ID: UUID;
  product: Association to Products;
  rating: Integer;
  comment: String(500);
  approved: Boolean;
}

entity ProductsWithReviews {
  key ID: UUID;
  name: String(200);

  // ✅ All reviews
  allReviews: Association to many Reviews on allReviews.product = $self;

  // ✅ Filtered: only approved reviews
  approvedReviews: Association to many Reviews
    on approvedReviews.product = $self
    and approvedReviews.approved = true;
}

// ── Summary of Validations ─────────────────────────────────────────────────

/*
Phase 20 validates:

1. ✅ ON condition type compatibility
   - Left and right sides must have compatible types
   - Prevents UUID = Integer, String = Boolean, etc.

2. ✅ Managed association key requirements
   - Target entity must have a key defined
   - Warns if target has no key

3. ℹ️  Composite key information
   - Informs about composite keys
   - CDS generates multiple foreign key fields

4. ⚠️  Empty ON conditions
   - Warns if ON condition doesn't reference fields

5. ℹ️  Bidirectional consistency
   - Checks if both sides use consistent approach

6. ℹ️  Association to many guidance
   - Suggests explicit ON conditions for clarity
   - Checks for backlinks

All validations help ensure data integrity and prevent runtime errors!
*/
