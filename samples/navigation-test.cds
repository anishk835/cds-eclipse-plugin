// Simple test for navigation
entity Book {
  key ID: Integer;
  title: String;
}

entity Author {
  key ID: Integer;
  name: String;
  favoriteBook: Association to Book;
}

// Test: Put cursor on "Book" in line 10 and press F3
// Should navigate to line 2
