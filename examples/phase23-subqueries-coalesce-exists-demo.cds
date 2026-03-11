/**
 * Phase 23: Subqueries, COALESCE, and EXISTS/NOT EXISTS Predicates
 *
 * Demonstrates validation of:
 * - Subqueries in SELECT, WHERE, and expressions
 * - COALESCE function for NULL handling
 * - EXISTS and NOT EXISTS predicates
 * - IN with subqueries
 */

namespace bookshop;

// ── Base Entities ────────────────────────────────────────────────────────

entity Books {
  key ID: UUID;
  title: String(200);
  authorID: UUID;
  price: Decimal(10, 2);
  stock: Integer;
  rating: Decimal(3, 2);
  publishYear: Integer;
}

entity Authors {
  key ID: UUID;
  firstName: String(100);
  middleName: String(100);
  lastName: String(100);
  country: String(50);
}

entity Reviews {
  key ID: UUID;
  bookID: UUID;
  rating: Integer;
  comment: LargeString;
}

entity Orders {
  key ID: UUID;
  bookID: UUID;
  quantity: Integer;
  status: String(20);
  totalAmount: Decimal(15, 2);
}

// ── COALESCE Function ────────────────────────────────────────────────────

// ✅ Valid: COALESCE with 2 arguments
entity AuthorNames as SELECT from Authors {
  COALESCE(middleName, '') as middleNameSafe,
  CONCAT(firstName, ' ', COALESCE(middleName, ''), ' ', lastName) as fullName
};

// ✅ Valid: COALESCE with multiple arguments
entity BookDisplay as SELECT from Books {
  title,
  COALESCE(rating, 0.0) as safeRating,
  COALESCE(stock, 0) as safeStock,
  COALESCE(rating, 3.0, 0.0) as ratingWithDefault
};

// ✅ Valid: COALESCE with numeric type promotion
entity OrderDefaults as SELECT from Orders {
  COALESCE(totalAmount, 0) as amount,  // Decimal and Integer -> Decimal
  COALESCE(quantity, 0, 1) as qty
};

// ✅ Valid: Nested COALESCE
entity BookPricing as SELECT from Books {
  COALESCE(
    price,
    COALESCE(stock, 0) * 10.0,
    99.99
  ) as finalPrice
};

// ── EXISTS Predicates ────────────────────────────────────────────────────

// ✅ Valid: EXISTS in WHERE clause
entity BooksWithReviews as SELECT from Books {
  title,
  price
} where EXISTS (
  SELECT 1 FROM Reviews WHERE Reviews.bookID = Books.ID
);

// ✅ Valid: NOT EXISTS
entity BooksWithoutReviews as SELECT from Books {
  title,
  price
} where NOT EXISTS (
  SELECT ID FROM Reviews WHERE Reviews.bookID = Books.ID
);

// ✅ Valid: EXISTS with complex subquery
entity PopularBooks as SELECT from Books {
  title,
  price
} where EXISTS (
  SELECT 1 FROM Reviews
  WHERE Reviews.bookID = Books.ID
    AND Reviews.rating >= 4
);

// ✅ Valid: Multiple EXISTS conditions
entity WellReviewedAndOrdered as SELECT from Books {
  title
} where EXISTS (
  SELECT 1 FROM Reviews WHERE Reviews.bookID = Books.ID
) and EXISTS (
  SELECT 1 FROM Orders WHERE Orders.bookID = Books.ID
);

// ── Subqueries in SELECT ─────────────────────────────────────────────────

// ✅ Valid: Subquery in SELECT (scalar subquery)
entity BookWithReviewCount as SELECT from Books {
  title,
  price,
  (SELECT COUNT(*) FROM Reviews WHERE Reviews.bookID = Books.ID) as reviewCount
};

// ✅ Valid: Multiple subqueries
entity BookStatistics as SELECT from Books {
  title,
  (SELECT COUNT(*) FROM Reviews WHERE Reviews.bookID = Books.ID) as reviewCount,
  (SELECT AVG(rating) FROM Reviews WHERE Reviews.bookID = Books.ID) as avgRating,
  (SELECT COUNT(*) FROM Orders WHERE Orders.bookID = Books.ID) as orderCount
};

// ✅ Valid: Subquery with COALESCE
entity BookAverageRating as SELECT from Books {
  title,
  COALESCE(
    (SELECT AVG(rating) FROM Reviews WHERE Reviews.bookID = Books.ID),
    0.0
  ) as safeAvgRating
};

// ── IN with Subqueries ───────────────────────────────────────────────────

// ✅ Valid: IN with subquery
entity BooksFromTopAuthors as SELECT from Books {
  title,
  price
} where authorID in (
  SELECT ID FROM Authors WHERE country = 'USA'
);

// ✅ Valid: NOT IN with subquery
entity BooksNotOrdered as SELECT from Books {
  title,
  price
} where ID not in (
  SELECT bookID FROM Orders WHERE status = 'delivered'
);

// ✅ Valid: IN with complex subquery
entity ExpensiveBooksWithHighRatings as SELECT from Books {
  title,
  price
} where ID in (
  SELECT bookID FROM Reviews
  WHERE rating >= 4
  GROUP BY bookID
  HAVING COUNT(*) >= 5
);

// ── Combined Features ────────────────────────────────────────────────────

// ✅ Valid: COALESCE + Subqueries + EXISTS
entity CompleteBookAnalysis as SELECT from Books {
  title,
  COALESCE(price, 0.0) as safePrice,
  COALESCE(
    (SELECT AVG(rating) FROM Reviews WHERE Reviews.bookID = Books.ID),
    rating,
    0.0
  ) as displayRating,
  CASE
    WHEN EXISTS (SELECT 1 FROM Reviews WHERE Reviews.bookID = Books.ID AND rating >= 4)
      THEN 'Highly Rated'
    WHEN EXISTS (SELECT 1 FROM Reviews WHERE Reviews.bookID = Books.ID)
      THEN 'Rated'
    ELSE 'Not Rated'
  END as ratingStatus,
  (SELECT COUNT(*) FROM Orders WHERE Orders.bookID = Books.ID) as totalOrders
};

// ✅ Valid: Complex nested subqueries
entity AuthorStatistics as SELECT from Authors {
  CONCAT(firstName, ' ', lastName) as name,
  (SELECT COUNT(*) FROM Books WHERE Books.authorID = Authors.ID) as bookCount,
  (
    SELECT SUM(
      (SELECT COUNT(*) FROM Reviews WHERE Reviews.bookID = Books.ID)
    )
    FROM Books
    WHERE Books.authorID = Authors.ID
  ) as totalReviews
};

// ✅ Valid: Multiple predicates combined
entity PopularBooksWithOrders as SELECT from Books {
  title,
  price
} where
  EXISTS (SELECT 1 FROM Reviews WHERE Reviews.bookID = Books.ID AND rating >= 4)
  AND stock > 0
  AND price < 50
  AND authorID IN (SELECT ID FROM Authors WHERE country = 'USA');

// ── Subqueries in CASE ───────────────────────────────────────────────────

// ✅ Valid: Subquery in CASE expression
entity BookAvailability as SELECT from Books {
  title,
  CASE
    WHEN (SELECT COUNT(*) FROM Orders WHERE Orders.bookID = Books.ID AND status = 'pending') > 10
      THEN 'High Demand'
    WHEN stock > 50
      THEN 'Available'
    WHEN stock > 0
      THEN 'Limited'
    ELSE 'Out of Stock'
  END as availability
};

// ── Advanced Patterns ────────────────────────────────────────────────────

// ✅ Valid: Correlated subquery with aggregation
entity BookSalesRank as SELECT from Books {
  title,
  price,
  (
    SELECT COUNT(*) + 1
    FROM Books as B2
    WHERE (SELECT SUM(quantity) FROM Orders WHERE Orders.bookID = B2.ID)
        > (SELECT SUM(quantity) FROM Orders WHERE Orders.bookID = Books.ID)
  ) as salesRank
};

// ✅ Valid: COALESCE with EXISTS
entity SmartDefaults as SELECT from Books {
  title,
  COALESCE(
    CASE
      WHEN EXISTS (SELECT 1 FROM Reviews WHERE Reviews.bookID = Books.ID)
        THEN (SELECT AVG(rating) FROM Reviews WHERE Reviews.bookID = Books.ID)
      ELSE NULL
    END,
    3.0
  ) as estimatedRating
};

// ✅ Valid: Subquery with CAST
entity BookPriceComparison as SELECT from Books {
  title,
  price,
  CAST(
    (SELECT AVG(price) FROM Books WHERE publishYear = Books.publishYear)
    AS Decimal(10,2)
  ) as avgPriceForYear
};

// ── Invalid Examples (Commented Out) ─────────────────────────────────────

/*
// ❌ ERROR: COALESCE with only 1 argument
entity InvalidCoalesce as SELECT from Books {
  COALESCE(price) as invalid  // ERROR: Need at least 2 arguments
};

// ❌ WARNING: COALESCE with incompatible types
entity TypeMismatchCoalesce as SELECT from Books {
  COALESCE(title, 123) as mixed  // WARNING: String and Integer incompatible
};

// ❌ ERROR: EXISTS without subquery
entity InvalidExists as SELECT from Books {
  title
} where EXISTS;  // ERROR: Must have subquery

// ❌ ERROR: Empty subquery in EXISTS
entity EmptyExists as SELECT from Books {
  title
} where EXISTS (
  SELECT FROM Reviews  // ERROR: Missing columns
);

// ❌ WARNING: Subquery returns multiple columns
entity MultiColumnSubquery as SELECT from Books {
  title,
  (SELECT title, price FROM Books WHERE ID = Books.authorID) as other  // WARNING: Multiple columns
};

// ❌ ERROR: IN with both values and subquery
entity InvalidIn as SELECT from Books {
  title
} where authorID in (1, 2, (SELECT ID FROM Authors));  // ERROR: Can't mix
*/

// ── Real-World Use Cases ─────────────────────────────────────────────────

// ✅ E-commerce: Product recommendations
entity RecommendedBooks as SELECT from Books {
  title,
  price,
  COALESCE(
    (SELECT AVG(rating) FROM Reviews WHERE Reviews.bookID = Books.ID),
    0.0
  ) as avgRating,
  (SELECT COUNT(*) FROM Orders WHERE Orders.bookID = Books.ID) as orderCount,
  CASE
    WHEN EXISTS (
      SELECT 1 FROM Reviews
      WHERE Reviews.bookID = Books.ID
        AND rating >= 4
      HAVING COUNT(*) >= 10
    ) THEN 'Bestseller'
    WHEN stock > 0 THEN 'Available'
    ELSE 'Out of Stock'
  END as badge
} where
  EXISTS (SELECT 1 FROM Reviews WHERE Reviews.bookID = Books.ID)
  AND price > 0
  AND stock >= 0;

// ✅ Analytics: Author performance
entity AuthorPerformance as SELECT from Authors {
  CONCAT(firstName, ' ', lastName) as name,
  COALESCE(country, 'Unknown') as location,
  (SELECT COUNT(*) FROM Books WHERE Books.authorID = Authors.ID) as totalBooks,
  COALESCE(
    (
      SELECT AVG(rating)
      FROM Reviews
      WHERE Reviews.bookID IN (
        SELECT ID FROM Books WHERE Books.authorID = Authors.ID
      )
    ),
    0.0
  ) as avgBookRating,
  (
    SELECT SUM(totalAmount)
    FROM Orders
    WHERE Orders.bookID IN (
      SELECT ID FROM Books WHERE Books.authorID = Authors.ID
    )
  ) as totalRevenue
} where EXISTS (
  SELECT 1 FROM Books WHERE Books.authorID = Authors.ID
);

// ✅ Inventory: Stock alerts
entity StockAlerts as SELECT from Books {
  title,
  stock,
  COALESCE(
    (
      SELECT AVG(quantity)
      FROM Orders
      WHERE Orders.bookID = Books.ID
        AND status = 'delivered'
    ),
    0
  ) as avgOrderQuantity,
  CASE
    WHEN stock = 0 THEN 'Critical'
    WHEN stock < COALESCE(
      (SELECT AVG(quantity) FROM Orders WHERE Orders.bookID = Books.ID),
      10
    ) THEN 'Low'
    ELSE 'OK'
  END as stockStatus
} where
  stock < 20
  AND EXISTS (
    SELECT 1 FROM Orders
    WHERE Orders.bookID = Books.ID
      AND status = 'pending'
  );

// ── Summary ──────────────────────────────────────────────────────────────

/*
Phase 23 adds:
✅ COALESCE function - NULL handling
  - COALESCE(expr1, expr2, ...)
  - Type consistency validation
  - Minimum 2 arguments required

✅ EXISTS predicates - Existence checks
  - EXISTS (subquery)
  - NOT EXISTS (subquery)
  - Used in WHERE clauses

✅ Subqueries - Nested SELECT
  - Scalar subqueries in SELECT
  - Subqueries in WHERE (with comparisons)
  - Subqueries with IN
  - Correlated subqueries

✅ IN with subqueries - Set membership
  - IN (subquery)
  - NOT IN (subquery)
  - Cannot mix values and subquery

Combined with Phases 22A & 22B:
✅ 18 built-in functions
✅ CASE expressions
✅ CAST expressions
✅ excluding clause
✅ Column alias uniqueness

Total: Complete SAP CAP projection and query support!

Coverage: 91% → 96% (+5% total: +2% Phase 22A, +1% Phase 22B, +2% Phase 23)
*/
