/**
 * Test file for advanced enum validation features.
 * This demonstrates reserved keywords, similar names, and value ranges.
 */

namespace test.validation;

// ─── Reserved Keywords ───────────────────────────────────────────────────────

// WARNING: 'select' and 'from' are reserved keywords
type QueryStatus : String enum {
  select;    // ⚠️ Warning: reserved keyword
  from;      // ⚠️ Warning: reserved keyword
  active;
  pending;
}

// WARNING: 'type' and 'entity' are CDS keywords
type BadNames : String enum {
  type;      // ⚠️ Warning: reserved keyword
  entity;    // ⚠️ Warning: reserved keyword
  Normal;
}

// ─── Similar Names ───────────────────────────────────────────────────────────

// WARNING: Case-only differences
type StatusWithCaseIssues : String enum {
  Active;    // ⚠️ Warning: differs only in case
  active;    // ⚠️ Warning: differs only in case
  ACTIVE;    // ⚠️ Warning: differs only in case
  Pending;
}

// WARNING: Underscore variations
type StatusWithUnderscores : String enum {
  in_progress;   // ⚠️ Warning: confusingly similar
  InProgress;    // ⚠️ Warning: confusingly similar
  ON_HOLD;       // ⚠️ Warning: confusingly similar
  onhold;        // ⚠️ Warning: confusingly similar
  Completed;
}

// GOOD: Clear, distinct names
type GoodStatus : String enum {
  Draft;
  Review;
  Published;
  Archived;
}

// ─── Integer Value Ranges ────────────────────────────────────────────────────

// WARNING: Extremely large values
type HugeNumbers : Integer enum {
  Small = 1;
  Medium = 100;
  Huge = 99999999;   // ⚠️ Warning: unusually large value
}

// WARNING: Large gaps between values
type GappedValues : Integer enum {
  First = 1;
  Second = 2;
  Third = 10003;     // ⚠️ Warning: large gap (10001) from previous
}

// WARNING: Negative extremes
type NegativeExtremes : Integer enum {
  VeryNegative = -5000000;  // ⚠️ Warning: unusually large (negative)
  Zero = 0;
  Positive = 100;
}

// GOOD: Reasonable integer ranges
type GoodPriority : Integer enum {
  Low = 1;
  Medium = 5;
  High = 10;
  Critical = 99;
}

// GOOD: Sequential values
type HttpStatus : Integer enum {
  OK = 200;
  Created = 201;
  NoContent = 204;
  BadRequest = 400;
  Unauthorized = 401;
  Forbidden = 403;
  NotFound = 404;
  ServerError = 500;
}

// ─── Edge Cases ──────────────────────────────────────────────────────────────

// GOOD: Empty but inherits
type BaseStatus : String enum {
  Active;
  Inactive;
}

type ExtendedEmpty : BaseStatus enum {
  // No warnings - inherits from non-empty
}

// Multiple issues in one enum
type MultipleIssues : Integer enum {
  null = 1;        // ⚠️ Warning: reserved keyword
  NULL = 2;        // ⚠️ Warning: reserved keyword + case-only difference
  VeryLarge = 8888888;  // ⚠️ Warning: unusually large value
}
