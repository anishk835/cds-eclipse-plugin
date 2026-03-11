/**
 * Test file for enum value annotations.
 * Demonstrates @label, @description, and custom annotations on enum values.
 */

namespace test.annotations;

// ─── Basic Annotations ───────────────────────────────────────────────────────

// Good: Enum values with @label annotations for UI display
type Priority : Integer enum {
  @label: 'Low Priority'
  Low = 1;

  @label: 'Medium Priority'
  Medium = 5;

  @label: 'High Priority'
  High = 10;

  @label: 'Critical - Immediate Action Required'
  Critical = 99;
}

// Good: Enum with both @label and @description
type OrderStatus : String enum {
  @label: 'Draft'
  @description: 'Order is being prepared but not yet submitted'
  Draft;

  @label: 'Pending Payment'
  @description: 'Order submitted, waiting for payment confirmation'
  PendingPayment;

  @label: 'Processing'
  @description: 'Payment confirmed, order is being prepared for shipment'
  Processing;

  @label: 'Shipped'
  @description: 'Order has been shipped to customer'
  Shipped;

  @label: 'Delivered'
  @description: 'Order successfully delivered to customer'
  Delivered;

  @label: 'Cancelled'
  @description: 'Order was cancelled by customer or system'
  Cancelled;
}

// ─── Advanced Annotations ────────────────────────────────────────────────────

// Custom annotations for enum values
type TaskStatus : String enum {
  @label: 'To Do'
  @description: 'Task not yet started'
  @UI.color: 'gray'
  @icon: 'circle-outline'
  ToDo;

  @label: 'In Progress'
  @description: 'Task is actively being worked on'
  @UI.color: 'blue'
  @icon: 'progress-clock'
  InProgress;

  @label: 'In Review'
  @description: 'Task completed, awaiting review'
  @UI.color: 'yellow'
  @icon: 'eye'
  InReview;

  @label: 'Done'
  @description: 'Task completed and approved'
  @UI.color: 'green'
  @icon: 'check-circle'
  Done;

  @label: 'Blocked'
  @description: 'Task cannot proceed due to dependencies'
  @UI.color: 'red'
  @icon: 'alert-circle'
  Blocked;
}

// Enum with localization annotations
type PaymentMethod : String enum {
  @label: 'Credit Card'
  @label.de: 'Kreditkarte'
  @label.fr: 'Carte de crédit'
  @label.es: 'Tarjeta de crédito'
  @description: 'Pay with credit or debit card'
  CreditCard;

  @label: 'PayPal'
  @label.de: 'PayPal'
  @label.fr: 'PayPal'
  @label.es: 'PayPal'
  @description: 'Pay using PayPal account'
  PayPal;

  @label: 'Bank Transfer'
  @label.de: 'Banküberweisung'
  @label.fr: 'Virement bancaire'
  @label.es: 'Transferencia bancaria'
  @description: 'Direct bank transfer'
  BankTransfer;

  @label: 'Cash on Delivery'
  @label.de: 'Nachnahme'
  @label.fr: 'Paiement à la livraison'
  @label.es: 'Contra reembolso'
  @description: 'Pay when order is delivered'
  CashOnDelivery;
}

// ─── Annotations with Complex Values ─────────────────────────────────────────

// Annotations with record values
type TicketPriority : Integer enum {
  @label: 'Low'
  @UI.badge: { variant: 'info', icon: 'info-circle' }
  @SLA: { responseTime: 48, resolutionTime: 168 }
  Low = 1;

  @label: 'Normal'
  @UI.badge: { variant: 'default', icon: 'circle' }
  @SLA: { responseTime: 24, resolutionTime: 72 }
  Normal = 2;

  @label: 'High'
  @UI.badge: { variant: 'warning', icon: 'alert-triangle' }
  @SLA: { responseTime: 4, resolutionTime: 24 }
  High = 3;

  @label: 'Urgent'
  @UI.badge: { variant: 'danger', icon: 'alert-circle' }
  @SLA: { responseTime: 1, resolutionTime: 4 }
  Urgent = 4;
}

// Annotations with array values
type ShippingMethod : String enum {
  @label: 'Standard Shipping'
  @deliveryDays: [5, 7]
  @availableCountries: ['US', 'CA', 'MX']
  @cost: 5.99
  Standard;

  @label: 'Express Shipping'
  @deliveryDays: [2, 3]
  @availableCountries: ['US', 'CA']
  @cost: 19.99
  Express;

  @label: 'Overnight Shipping'
  @deliveryDays: [1, 1]
  @availableCountries: ['US']
  @cost: 49.99
  Overnight;
}

// ─── Inheritance with Annotations ────────────────────────────────────────────

// Base enum with annotations
type BaseStatus : String enum {
  @label: 'Active'
  @description: 'Currently active'
  @color: 'green'
  Active;

  @label: 'Inactive'
  @description: 'Currently inactive'
  @color: 'gray'
  Inactive;
}

// Extended enum adds more annotated values
type ExtendedStatus : BaseStatus enum {
  @label: 'Pending'
  @description: 'Waiting for activation'
  @color: 'yellow'
  Pending;

  @label: 'Suspended'
  @description: 'Temporarily suspended'
  @color: 'orange'
  Suspended;

  @label: 'Archived'
  @description: 'Permanently archived'
  @color: 'blue'
  Archived;
}

// ─── Usage in Entities ───────────────────────────────────────────────────────

entity Order {
  key ID          : UUID;
  orderNumber     : String(50);
  status          : OrderStatus = #Draft;
  priority        : Priority = #Medium;
  paymentMethod   : PaymentMethod;
  shippingMethod  : ShippingMethod;
  totalAmount     : Decimal(10,2);
}

entity Task {
  key ID          : UUID;
  title           : String(200);
  description     : String(1000);
  status          : TaskStatus = #ToDo;
  priority        : TicketPriority = #Normal;
  assignedTo      : String(100);
}

// ─── Without Annotations (for comparison) ────────────────────────────────────

// INFO: Consider adding @label annotations
type SimpleStatus : String enum {
  Open;
  Closed;
  Pending;
  Resolved;
}

// Single annotation example
type Gender : String enum {
  @label: 'Male'
  Male;

  @label: 'Female'
  Female;

  @label: 'Non-Binary'
  NonBinary;

  @label: 'Prefer not to say'
  PreferNotToSay;
}
