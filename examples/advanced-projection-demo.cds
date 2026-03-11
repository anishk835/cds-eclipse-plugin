/**
 * Phase 22A: Advanced Projections - Built-in Functions and Column Aliases
 *
 * Demonstrates validation of:
 * - Built-in string functions (CONCAT, UPPER, LOWER, SUBSTRING, LENGTH, TRIM)
 * - Built-in numeric functions (ROUND, FLOOR, CEIL, ABS)
 * - Built-in date/time functions (CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP)
 * - Column alias uniqueness
 */

namespace bookshop;

// ── Base Entities ────────────────────────────────────────────────────────

entity Books {
  key ID: UUID;
  title: String(200);
  isbn: String(20);
  price: Decimal(10, 2);
  discount: Decimal(5, 2);
  author: Association to Authors;
}

entity Authors {
  key ID: UUID;
  firstName: String(100);
  middleName: String(100);
  lastName: String(100);
}

entity Orders {
  key ID: UUID;
  orderNumber: String(20);
  status: String(20);
  totalAmount: Decimal(15, 2);
  createdAt: DateTime;
}

// ── String Functions ─────────────────────────────────────────────────────

// ✅ Valid string function usage
entity BookTitles as SELECT from Books {
  // UPPER - converts to uppercase
  UPPER(title) as titleUpper,

  // LOWER - converts to lowercase
  LOWER(title) as titleLower,

  // CONCAT - concatenates strings
  CONCAT('ISBN: ', isbn) as isbnWithLabel,

  // SUBSTRING - extracts substring (1-based index)
  SUBSTRING(isbn, 1, 3) as isbnPrefix,

  // LENGTH - returns string length
  LENGTH(title) as titleLength,

  // TRIM - removes whitespace
  TRIM(title) as titleTrimmed
};

// ✅ Variadic CONCAT (multiple arguments)
entity AuthorNames as SELECT from Authors {
  CONCAT(firstName, ' ', middleName, ' ', lastName) as fullName,
  CONCAT(lastName, ', ', firstName) as displayName
};

// ── Numeric Functions ────────────────────────────────────────────────────

// ✅ Valid numeric function usage
entity BookPrices as SELECT from Books {
  // ROUND - rounds to specified decimals
  ROUND(price, 2) as roundedPrice,
  ROUND(price) as roundedPriceInt,

  // FLOOR - largest integer <= value
  FLOOR(price) as floorPrice,

  // CEIL/CEILING - smallest integer >= value
  CEIL(price) as ceilPrice,

  // ABS - absolute value
  ABS(discount) as absDiscount,

  // Nested functions
  ROUND(price * (1 - discount / 100), 2) as finalPrice
};

// ── Date/Time Functions ──────────────────────────────────────────────────

// ✅ Valid date/time function usage
entity OrderTimestamps as SELECT from Orders {
  orderNumber,

  // CURRENT_DATE - current date
  CURRENT_DATE() as today,

  // CURRENT_TIME - current time
  CURRENT_TIME() as now,

  // CURRENT_TIMESTAMP - current timestamp
  CURRENT_TIMESTAMP() as currentTimestamp,

  // NOW - alias for CURRENT_TIMESTAMP
  NOW() as nowAlias
};

// ── Column Alias Uniqueness ──────────────────────────────────────────────

// ✅ Valid: unique aliases
entity UniqueAliases as SELECT from Books {
  title as bookTitle,
  isbn as bookIsbn,
  price as bookPrice
};

// ── Invalid Examples (Commented Out) ─────────────────────────────────────

/*
// ❌ ERROR: Unknown function
entity InvalidFunction as SELECT from Books {
  UNKNOWN_FUNC(title) as result  // ERROR: Unknown built-in function
};

// ❌ ERROR: Wrong argument count
entity WrongArgCount as SELECT from Books {
  UPPER(title, isbn) as result   // ERROR: UPPER expects 1 argument, got 2
};

// ❌ WARNING: Wrong argument type
entity WrongArgType as SELECT from Books {
  UPPER(price) as result          // WARNING: UPPER expects string, got Decimal
};

// ❌ ERROR: Duplicate column alias
entity DuplicateAlias as SELECT from Books {
  title as name,
  isbn as name                    // ERROR: Duplicate column alias 'name'
};

// ❌ ERROR: Multiple duplicates
entity MultipleDuplicates as SELECT from Books {
  title as field,
  isbn as field,
  price as field                  // ERROR: All use same alias 'field'
};
*/

// ── Complex Real-World Examples ──────────────────────────────────────────

// ✅ Combining multiple functions
entity BookCatalog as SELECT from Books {
  CONCAT(UPPER(SUBSTRING(title, 1, 1)), LOWER(SUBSTRING(title, 2, LENGTH(title) - 1)))
    as titleCase,
  CONCAT('$', ROUND(price * 0.9, 2)) as salePrice
};

// ✅ Author full names with proper formatting
entity AuthorDirectory as SELECT from Authors {
  CONCAT(UPPER(SUBSTRING(lastName, 1, 1)), '. ', firstName) as casualName,
  CONCAT(firstName, ' ', middleName, ' ', lastName) as formalName,
  LENGTH(CONCAT(firstName, lastName)) as nameLength
};

// ✅ Order summary with calculations
entity OrderSummary as SELECT from Orders {
  orderNumber,
  CONCAT('Order #', orderNumber, ' - ', status) as displayText,
  ROUND(totalAmount, 2) as roundedAmount,
  ABS(totalAmount - ROUND(totalAmount, 0)) as cents,
  CURRENT_TIMESTAMP() as reportTime
};

// ── All Supported Functions ──────────────────────────────────────────────

entity FunctionShowcase as SELECT from Books {
  // String functions (6)
  CONCAT('A', 'B', 'C') as concatDemo,
  UPPER('hello') as upperDemo,
  LOWER('WORLD') as lowerDemo,
  SUBSTRING('abcdef', 2, 3) as substringDemo,
  LENGTH('test') as lengthDemo,
  TRIM('  spaces  ') as trimDemo,

  // Numeric functions (5)
  ROUND(3.14159, 2) as roundDemo,
  FLOOR(3.9) as floorDemo,
  CEIL(3.1) as ceilDemo,
  CEILING(3.1) as ceilingDemo,
  ABS(-42) as absDemo,

  // Date/Time functions (4)
  CURRENT_DATE() as currentDateDemo,
  CURRENT_TIME() as currentTimeDemo,
  CURRENT_TIMESTAMP() as currentTimestampDemo,
  NOW() as nowDemo,

  // Conversion functions (1)
  STRING(123) as stringDemo
};

// ── Phase 22B Features (Not Yet Implemented) ─────────────────────────────

/*
// Future Phase 22B: CASE expressions
entity BookStatus as SELECT from Books {
  title,
  CASE
    WHEN price < 10 THEN 'Budget'
    WHEN price < 30 THEN 'Standard'
    ELSE 'Premium'
  END as priceCategory
};

// Future Phase 22B: CAST expressions
entity BookPricesCast as SELECT from Books {
  CAST(price AS Integer) as priceInt,
  CAST(discount AS String) as discountText
};

// Future Phase 22B: excluding clause
entity PublicBooks as SELECT from Books {
  * excluding { internalNotes, draft }
};

// Future Phase 22B: Subqueries
entity BookWithReviewCount as SELECT from Books {
  title,
  (SELECT COUNT(*) FROM Reviews WHERE bookID = Books.ID) as reviewCount
};
*/

// ── Summary ──────────────────────────────────────────────────────────────

/*
Phase 22A validates:
✅ String functions: CONCAT, UPPER, LOWER, SUBSTRING, LENGTH, TRIM
✅ Numeric functions: ROUND, FLOOR, CEIL, CEILING, ABS
✅ Date/Time functions: CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP, NOW
✅ Conversion functions: STRING
✅ Function argument count validation
✅ Function argument type validation (warnings)
✅ Column alias uniqueness in SELECT

18 built-in functions supported!

Phase 22B will add:
- CASE expressions
- CAST expressions
- excluding clause
- Subqueries in SELECT
*/
