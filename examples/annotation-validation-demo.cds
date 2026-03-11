/**
 * Phase 21: Annotation Validation Examples
 *
 * This file demonstrates the annotation validation added in Phase 21.
 * The validator checks annotation vocabulary, value types, and targets.
 */

namespace bookshop;

// ── Core Annotations ────────────────────────────────────────────────────────

// ✅ Standard core annotations
@title: 'Book Catalog'
@description: 'Comprehensive catalog of available books'
@readonly: true
entity Books {
  key ID: UUID;

  @title: 'Book Title'
  @mandatory: true
  title: String(200);

  @description: 'Author full name'
  author: String(100);

  @title: 'Price'
  price: Decimal(10, 2);
}

// ✅ CDS persistence annotations
@cds.autoexpose: true
@cds.persistence.journal: true
entity AuditedProducts {
  key ID: UUID;
  name: String(100);
  price: Decimal(15, 2);
}

// ── Authorization Annotations ───────────────────────────────────────────────

// ✅ Service authorization
@title: 'Bookshop Service'
@requires: 'authenticated-user'
service CatalogService {

  // ✅ Entity-level restriction
  @restrict: [
    { grant: 'READ', to: 'Reader' },
    { grant: ['CREATE', 'UPDATE'], to: 'Admin' }
  ]
  entity SecureBooks {
    key ID: UUID;
    title: String(200);
  }
}

// ── UI Annotations (SAP Fiori) ──────────────────────────────────────────────

// ✅ List view configuration
@UI.LineItem: [
  { Value: title, Label: 'Title' },
  { Value: author, Label: 'Author' },
  { Value: price, Label: 'Price' }
]
@UI.SelectionFields: [title, author]
@UI.HeaderInfo: {
  TypeName: 'Book',
  TypeNamePlural: 'Books',
  Title: { Value: title }
}
entity FioriBooks {
  key ID: UUID;

  @UI.Hidden: false
  title: String(200);

  author: String(100);

  @Common.Label: 'Sale Price'
  price: Decimal(10, 2);
}

// ── Validation Annotations ──────────────────────────────────────────────────

// ✅ Data validation annotations
entity ValidatedProducts {
  key ID: UUID;

  @mandatory: true
  @assert.format: '^[A-Z]{3}-[0-9]{4}$'
  productCode: String(10);

  @assert.range: [0, 100]
  discount: Integer;

  @assert.notNull: true
  name: String(100);

  @assert.unique: true
  sku: String(50);

  @Core.Computed: true
  totalPrice: Decimal;

  @Core.Immutable: true
  createdAt: DateTime;
}

// ── OData Capability Annotations ────────────────────────────────────────────

// ✅ Control OData operations
@Capabilities.Insertable: true
@Capabilities.Updatable: false
@Capabilities.Deletable: false
@Capabilities.Readable: true
entity ReadOnlyOrders {
  key ID: UUID;
  orderNumber: String(20);
  status: String(20);
}

// ── Element-Level Annotations ───────────────────────────────────────────────

entity RichElements {
  key ID: UUID;

  // ✅ Multiple annotations on one field
  @title: 'Product Name'
  @description: 'Full product name including variant'
  @mandatory: true
  @Common.Label: 'Name'
  name: String(200);

  // ✅ UI annotations on element
  @UI.MultiLineText: true
  @UI.HiddenFilter: false
  description: LargeString;

  // ✅ Validation on element
  @assert.range: [0, 9999.99]
  @Common.Label: 'Unit Price'
  price: Decimal(15, 2);
}

// ── Invalid Annotations (Commented Out) ─────────────────────────────────────

/*
// Uncomment to see validation errors:

// ❌ ERROR: @readonly expects boolean, got number
@readonly: 123
entity WrongValueType1 {
  key ID: UUID;
}

// ❌ ERROR: @title expects string, got boolean
@title: true
entity WrongValueType2 {
  key ID: UUID;
}

// ❌ ERROR: @UI.LineItem expects array, got string
@UI.LineItem: "not-an-array"
entity WrongValueType3 {
  key ID: UUID;
}

// ❌ ERROR: @UI.LineItem can't be applied to element
entity WrongTarget1 {
  key ID: UUID;

  @UI.LineItem: []  // Should be on entity, not element
  title: String;
}

// ❌ ERROR: @mandatory can't be applied to entity
@mandatory: true  // Should be on element, not entity
entity WrongTarget2 {
  key ID: UUID;
}

// ℹ️  INFO: Unknown standard annotation (might be typo)
@UI.UnknownAnnotation: "value"
entity UnknownAnnotation {
  key ID: UUID;
}

// ℹ️  INFO: Looks like typo for @readonly
@raedonly: true
entity TypoAnnotation {
  key ID: UUID;
}
*/

// ── Custom Annotations (Always Valid) ───────────────────────────────────────

// ✅ Custom annotations are allowed (no validation)
@MyApp.customField: "value"
@CompanyX.internal: true
@ProjectY.metadata: { version: 1, author: 'Developer' }
entity CustomAnnotations {
  key ID: UUID;

  @MyApp.displayFormat: "bold"
  @MyApp.sortOrder: 10
  name: String(100);
}

// ── Type Annotations ────────────────────────────────────────────────────────

// ✅ Annotations on type definitions
@title: 'Email Address'
@description: 'Valid email address format'
@assert.format: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'
type EmailAddress : String(100);

@title: 'Currency Code'
@description: 'ISO 4217 currency code'
@assert.format: '^[A-Z]{3}$'
type CurrencyCode : String(3);

// ── Enum Annotations ────────────────────────────────────────────────────────

// ✅ Annotations on enums
@title: 'Order Status'
@description: 'Possible states of an order'
type OrderStatus : String enum {
  @title: 'Pending'
  Pending;

  @title: 'Confirmed'
  Confirmed;

  @title: 'Shipped'
  Shipped;

  @title: 'Delivered'
  Delivered;
}

// ── Service Entity Annotations ──────────────────────────────────────────────

service AdminService {
  // ✅ Annotations on service entities
  @readonly: true
  @Capabilities.Insertable: false
  @UI.LineItem: [
    { Value: ID },
    { Value: name }
  ]
  entity ReadOnlyView as projection on Books;
}

// ── Complex Annotation Structures ───────────────────────────────────────────

// ✅ Complex UI annotations
@UI.HeaderInfo: {
  TypeName: 'Product',
  TypeNamePlural: 'Products',
  Title: { Value: name },
  Description: { Value: description }
}
@UI.Identification: [
  { Value: productCode },
  { Value: name }
]
@UI.FieldGroup #Details: {
  Label: 'Product Details',
  Data: [
    { Value: productCode },
    { Value: name },
    { Value: price }
  ]
}
entity ComplexUIAnnotations {
  key ID: UUID;
  productCode: String(10);
  name: String(200);
  description: LargeString;
  price: Decimal(15, 2);
}

// ── Common Patterns ─────────────────────────────────────────────────────────

// ✅ Combination of annotations for production entities
@title: 'Customer Orders'
@description: 'Customer order management'
@cds.autoexpose: true
@requires: 'authenticated-user'
@Capabilities.Insertable: true
@Capabilities.Updatable: true
@Capabilities.Deletable: false
@UI.LineItem: [
  { Value: orderNumber, Label: 'Order #' },
  { Value: customer.name, Label: 'Customer' },
  { Value: totalAmount, Label: 'Total' },
  { Value: status, Label: 'Status' }
]
entity Orders {
  key ID: UUID;

  @mandatory: true
  @assert.format: '^ORD-[0-9]{6}$'
  orderNumber: String(20);

  @mandatory: true
  customer: Association to Customers;

  @Core.Computed: true
  totalAmount: Decimal(15, 2);

  @mandatory: true
  status: OrderStatus;

  @Core.Immutable: true
  createdAt: DateTime;

  @Common.Label: 'Created By'
  createdBy: String(100);
}

entity Customers {
  key ID: UUID;
  name: String(100);
}

// ── Summary ─────────────────────────────────────────────────────────────────

/*
Phase 21 validates:

✅ Known annotations (@title, @readonly, @UI.*, @assert.*, etc.)
✅ Value types (boolean for @readonly, string for @title, etc.)
✅ Target types (@UI.LineItem only on entities, @mandatory only on elements)
✅ Custom annotations (no errors for @MyApp.*)
ℹ️  Unknown standard annotations (info hint, might be typo)

30+ standard SAP annotations are registered and validated!
*/
