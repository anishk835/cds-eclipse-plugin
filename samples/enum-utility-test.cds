/**
 * Test file for enum utility features.
 * This demonstrates value count validation, ordering checks, and auto-documentation.
 */

namespace test.utility;

// ─── Value Count Validation ──────────────────────────────────────────────────

// INFO: Only 1 value - consider using a constant
type SingleValue : String enum {
  OnlyValue;    // ℹ️ Info: Consider using a constant instead
}

// GOOD: Reasonable number of values
type ReasonableSize : String enum {
  Draft;
  Review;
  Approved;
  Published;
  Archived;
}

// WARNING: Too many values (>100)
type TooManyValues : Integer enum {
  Value1 = 1;
  Value2 = 2;
  Value3 = 3;
  Value4 = 4;
  Value5 = 5;
  Value6 = 6;
  Value7 = 7;
  Value8 = 8;
  Value9 = 9;
  Value10 = 10;
  Value11 = 11;
  Value12 = 12;
  Value13 = 13;
  Value14 = 14;
  Value15 = 15;
  Value16 = 16;
  Value17 = 17;
  Value18 = 18;
  Value19 = 19;
  Value20 = 20;
  Value21 = 21;
  Value22 = 22;
  Value23 = 23;
  Value24 = 24;
  Value25 = 25;
  Value26 = 26;
  Value27 = 27;
  Value28 = 28;
  Value29 = 29;
  Value30 = 30;
  Value31 = 31;
  Value32 = 32;
  Value33 = 33;
  Value34 = 34;
  Value35 = 35;
  Value36 = 36;
  Value37 = 37;
  Value38 = 38;
  Value39 = 39;
  Value40 = 40;
  Value41 = 41;
  Value42 = 42;
  Value43 = 43;
  Value44 = 44;
  Value45 = 45;
  Value46 = 46;
  Value47 = 47;
  Value48 = 48;
  Value49 = 49;
  Value50 = 50;
  Value51 = 51;
  Value52 = 52;
  Value53 = 53;
  Value54 = 54;
  Value55 = 55;
  Value56 = 56;
  Value57 = 57;
  Value58 = 58;
  Value59 = 59;
  Value60 = 60;
  Value61 = 61;
  Value62 = 62;
  Value63 = 63;
  Value64 = 64;
  Value65 = 65;
  Value66 = 66;
  Value67 = 67;
  Value68 = 68;
  Value69 = 69;
  Value70 = 70;
  Value71 = 71;
  Value72 = 72;
  Value73 = 73;
  Value74 = 74;
  Value75 = 75;
  Value76 = 76;
  Value77 = 77;
  Value78 = 78;
  Value79 = 79;
  Value80 = 80;
  Value81 = 81;
  Value82 = 82;
  Value83 = 83;
  Value84 = 84;
  Value85 = 85;
  Value86 = 86;
  Value87 = 87;
  Value88 = 88;
  Value89 = 89;
  Value90 = 90;
  Value91 = 91;
  Value92 = 92;
  Value93 = 93;
  Value94 = 94;
  Value95 = 95;
  Value96 = 96;
  Value97 = 97;
  Value98 = 98;
  Value99 = 99;
  Value100 = 100;
  Value101 = 101;   // ⚠️ Warning: Too many values (>100)
}

// ─── Value Ordering ──────────────────────────────────────────────────────────

// INFO: Unordered values
type UnsortedPriority : Integer enum {
  High = 10;         // ℹ️ Info: Values are not sorted
  Low = 1;
  Medium = 5;
  Critical = 99;
}

// GOOD: Sorted ascending
type SortedAscending : Integer enum {
  Lowest = 1;
  Low = 10;
  Medium = 50;
  High = 100;
  Highest = 999;
}

// GOOD: Sorted descending
type SortedDescending : Integer enum {
  Critical = 100;
  High = 50;
  Medium = 25;
  Low = 10;
  Minimal = 1;
}

// GOOD: HTTP status codes (naturally grouped, not strictly sorted)
type HttpStatusCodes : Integer enum {
  OK = 200;
  Created = 201;
  NoContent = 204;
  BadRequest = 400;
  Unauthorized = 401;
  Forbidden = 403;
  NotFound = 404;
  ServerError = 500;
  ServiceUnavailable = 503;
}

// ─── Auto-Documentation Examples ─────────────────────────────────────────────

// Hover over these enum types to see rich documentation

type DocumentedString : String enum {
  Option1;
  Option2;
  Option3;
  Option4;
  Option5;
}

type DocumentedInteger : Integer enum {
  Min = 1;
  Default = 50;
  Max = 100;
}

type InheritedEnum : DocumentedString enum {
  ExtendedOption1;
  ExtendedOption2;
}

// ─── Usage in Entities ───────────────────────────────────────────────────────

entity TestEntity {
  key ID : Integer;

  // Hover over these field types to see enum documentation
  singleValue : SingleValue;
  priority    : UnsortedPriority;
  status      : DocumentedString = #Option1;
  inherited   : InheritedEnum = #ExtendedOption1;
}
