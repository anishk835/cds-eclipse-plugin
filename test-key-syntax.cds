// Test file for Phase 8: Key Constraints

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
