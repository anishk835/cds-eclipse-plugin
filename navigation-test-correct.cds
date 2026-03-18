// Test file for navigation with correct syntax

// Base entity
entity Product {
  key ID: Integer;
  name: String;
  description: String;
}

// Test 1: Navigate from association target
entity Order {
  key ID: Integer;
  product: Association to Product;  // <-- F3 on "Product" should navigate to line 4
}

// Test 2: Navigate from projection source
entity ProductView as projection on Product {  // <-- F3 on "Product" should navigate to line 4
  ID,
  name
}

// Test 3: Navigate from view SELECT
entity ProductList as SELECT from Product {  // <-- F3 on "Product" should navigate to line 4
  ID,
  name,
  description
}
where name is not null;

// Test 4: Service with projection
service TestService {
  entity Products as projection on Product;  // <-- F3 on "Product" should navigate to line 4
}

// Test 5: Navigate in using statement
using Product from './navigation-test-correct.cds';  // <-- F3 on "Product" in using should work

// Test 6: Navigate in extend
extend Product with {
  price: Decimal;
}

// Test 7: Navigate in annotate
annotate Product with @readonly;
