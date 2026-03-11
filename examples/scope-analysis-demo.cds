/**
 * Phase 19: Scope Analysis Examples
 *
 * This file demonstrates the scope analysis and resolution validation
 * added in Phase 19.
 */

namespace bookshop;

// ── Valid Type References ──────────────────────────────────────────────────

// ✅ Built-in types are always available
entity Products {
  key ID: UUID;
  name: String(100);
  price: Decimal(10, 2);
  quantity: Integer;
  active: Boolean;
  createdAt: DateTime;
}

// ✅ Custom types defined in same file
type Currency : String(3);
type Money : Decimal(15, 2);
type EmailAddress : String(100);

entity Customers {
  key ID: UUID;
  name: String(100);
  email: EmailAddress;
  balance: Money;
  preferredCurrency: Currency;
}

// ── Valid Associations ─────────────────────────────────────────────────────

entity Authors {
  key ID: UUID;
  name: String(100);
  country: String(2);

  // ✅ Forward reference to Books (defined below)
  books: Association to many Books on books.author = $self;
}

entity Books {
  key ID: UUID;
  title: String(200);
  price: Money;

  // ✅ Backward reference to Authors (defined above)
  author: Association to Authors;

  // ✅ Forward reference to Publishers
  publisher: Association to Publishers;
}

entity Publishers {
  key ID: UUID;
  name: String(100);

  // ✅ Association to many
  books: Association to many Books on books.publisher = $self;
}

// ── Self-References ────────────────────────────────────────────────────────

// ✅ Hierarchical data with self-reference
entity Categories {
  key ID: UUID;
  name: String(100);
  parent: Association to Categories;  // Self-reference
}

// ✅ Tree structure
entity TreeNode {
  key ID: UUID;
  value: String;
  left: Association to TreeNode;
  right: Association to TreeNode;
}

// ── Circular Associations ──────────────────────────────────────────────────

// ✅ Circular references are valid in CDS
entity Employees {
  key ID: UUID;
  name: String(100);
  department: Association to Departments;
}

entity Departments {
  key ID: UUID;
  name: String(50);
  manager: Association to Employees;
  employees: Association to many Employees on employees.department = $self;
}

// ── Enum References ────────────────────────────────────────────────────────

type Priority : String enum {
  Low;
  Medium;
  High;
  Critical;
}

type Status : String enum {
  Draft;
  Published;
  Archived;
}

entity Tasks {
  key ID: UUID;
  title: String(200);
  priority: Priority;  // ✅ Enum reference
  status: Status;      // ✅ Enum reference
}

// ── Namespace Consistency ──────────────────────────────────────────────────

// ℹ️  INFO: These definitions use short names
// Fully qualified names would be: bookshop.Orders, bookshop.OrderItems, etc.

entity Orders {
  key ID: UUID;
  orderNumber: String(20);
  customer: Association to Customers;
  items: Composition of many OrderItems on items.order = $self;
}

entity OrderItems {
  key ID: UUID;
  order: Association to Orders;
  product: Association to Products;
  quantity: Integer;
  price: Money;
}

// ── Invalid References (Commented Out) ─────────────────────────────────────

/*
// Uncomment to see scope errors:

entity InvalidExamples {
  key ID: UUID;

  // ❌ ERROR: NonExistentType doesn't exist
  // field1: NonExistentType;

  // ❌ ERROR: MissingEntity doesn't exist
  // field2: Association to MissingEntity;

  // ❌ ERROR: UndefinedEnum doesn't exist
  // field3: UndefinedEnum;
}

// ⚠️  WARNING: Would need to import from external file
// using { Currency } from './nonexistent';  // File doesn't exist

// ⚠️  WARNING: Ambiguous if imported from multiple sources
// using { Status } from './file1';
// using { Status } from './file2';  // Ambiguous import
*/

// ── Cross-File References (Require File to Exist) ──────────────────────────

/*
// Examples of cross-file imports (would need actual files):

using { Country } from './common/types';
using { Currency } from './common/currencies';
using { User } from '../admin/users';

entity InternationalOrders {
  key ID: UUID;
  customer: Association to User;
  country: Country;
  currency: Currency;
}
*/

// ── Service Exposure ────────────────────────────────────────────────────────

// ✅ All entity references in service are resolved
service CatalogService {
  entity BooksView as projection on Books;
  entity AuthorsView as projection on Authors;
  entity PublishersView as projection on Publishers;

  // ✅ Computed elements can reference other entities
  entity OrderSummary as projection on Orders {
    *,
    customer.name as customerName,
    COUNT(items) as itemCount
  }
}

// ── Aspect Extensions ───────────────────────────────────────────────────────

// ✅ Aspects can reference entities
aspect Auditable {
  createdAt: DateTime;
  createdBy: Association to Employees;
  modifiedAt: DateTime;
  modifiedBy: Association to Employees;
}

entity AuditedBooks : Auditable {
  key ID: UUID;
  title: String(200);
  author: Association to Authors;
}
