# Eclipse Plugin Deployment Guide

## How to Use the CDS Plugin in Eclipse

This guide explains how to build, install, and use the SAP CAP CDS Eclipse plugin.

---

## Prerequisites

### Software Requirements
- **Eclipse IDE**: Eclipse 2024-06 or later (with Xtext support)
- **Java**: JDK 17 or later
- **Maven**: 3.8+ (for building)
- **Git**: For cloning (if not already cloned)

### Recommended Eclipse Package
- **Eclipse IDE for Java and DSL Developers** (includes Xtext)
- Download from: https://www.eclipse.org/downloads/packages/

---

## Option 1: Build and Install from Source (Recommended)

### Step 1: Build the Plugin

Open a terminal and navigate to the plugin directory:

```bash
cd /Users/I546280/cds-eclipse-plugin

# Clean build with P2 repository generation
mvn clean install
```

This will:
- Generate the parser from grammar
- Compile all Java code
- Run tests (if dependencies are resolved)
- Create an Eclipse update site in `releng/org.example.cds.p2/target/repository/`

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

### Step 2: Install in Eclipse

#### Method A: Install from Local Update Site

1. **Open Eclipse**

2. **Go to Install Dialog:**
   - Menu: `Help` → `Install New Software...`

3. **Add Local Repository:**
   - Click `Add...` button
   - Name: `CDS Plugin (Local)`
   - Location: Click `Local...` and browse to:
     ```
     /Users/I546280/cds-eclipse-plugin/releng/org.example.cds.p2/target/repository
     ```
   - Click `Add`

4. **Select Features:**
   - Check `org.example.cds.feature`
   - Uncheck "Contact all update sites..." (faster)
   - Click `Next`

5. **Review and Accept:**
   - Review items to be installed
   - Click `Next`
   - Accept license agreement
   - Click `Finish`

6. **Trust Certificate:**
   - When prompted about unsigned content, click `Trust Selected` or `Continue`

7. **Restart Eclipse:**
   - Click `Restart Now` when prompted

#### Method B: Install from Feature JAR (Alternative)

1. **Export Feature:**
   ```bash
   cd /Users/I546280/cds-eclipse-plugin
   # The feature is built at:
   # features/org.example.cds.feature/target/org.example.cds.feature-*.jar
   ```

2. **Install in Eclipse:**
   - Copy all plugin JARs from:
     - `plugins/org.example.cds/target/`
     - `plugins/org.example.cds.ui/target/`
     - `plugins/org.example.cds.ide/target/`
   - To Eclipse's `dropins` folder:
     ```
     /Applications/Eclipse.app/Contents/Eclipse/dropins/
     # or on your Eclipse installation directory
     ```
   - Restart Eclipse

---

## Option 2: Run from Development Workspace

For development and testing, you can run Eclipse with the plugin directly:

### Step 1: Import into Eclipse

1. **Open Eclipse** (a separate Eclipse instance for development)

2. **Import Projects:**
   - `File` → `Import...` → `Existing Maven Projects`
   - Browse to: `/Users/I546280/cds-eclipse-plugin`
   - Select all projects
   - Click `Finish`

3. **Wait for Build:**
   - Eclipse will automatically build the projects
   - Check `Progress` view for completion

### Step 2: Launch Runtime Eclipse

1. **Open Run Configuration:**
   - `Run` → `Run Configurations...`

2. **Create Eclipse Application:**
   - Right-click `Eclipse Application`
   - Select `New Configuration`
   - Name: `CDS Plugin Runtime`

3. **Configure:**
   - **Main Tab:**
     - Program to Run: `Run an application` → `org.eclipse.ui.ide.workbench`

   - **Arguments Tab:**
     - VM arguments:
       ```
       -Xmx2048m
       -Dfile.encoding=UTF-8
       ```

   - **Plug-ins Tab:**
     - Select: `plug-ins selected below only`
     - Click `Deselect All`
     - Check:
       - `org.example.cds`
       - `org.example.cds.ui`
       - `org.example.cds.ide`
     - Click `Add Required Plug-ins`

4. **Launch:**
   - Click `Run`
   - A new Eclipse instance will start with your plugin loaded

---

## Verify Installation

### Check Plugin is Installed

1. **Open About Dialog:**
   - Menu: `Help` → `About Eclipse IDE`
   - Click `Installation Details`

2. **Look for:**
   - `org.example.cds` - CDS Language Core
   - `org.example.cds.ui` - CDS Language UI
   - `org.example.cds.ide` - CDS Language IDE

### Check File Association

1. **Preferences:**
   - Menu: `Window` → `Preferences` (or `Eclipse` → `Preferences` on macOS)

2. **Navigate to:**
   - `General` → `Content Types`
   - Look for `Text` → `CDS`

3. **Check File Types:**
   - `General` → `Editors` → `File Associations`
   - Look for `*.cds` → should be associated with CDS Editor

---

## Using the Plugin

### Create a CDS Project

1. **Create New Project:**
   - `File` → `New` → `Project...`
   - Select `General` → `Project`
   - Name: `my-cap-project`
   - Click `Finish`

2. **Create CDS File:**
   - Right-click project → `New` → `File`
   - Name: `schema.cds`
   - Click `Finish`

### Write CDS Code

Open `schema.cds` and try the example:

```cds
namespace bookshop;

entity Books {
  key ID: UUID;
  title: String(200);
  price: Decimal(10, 2);
  stock: Integer;
  author: Association to Authors;
}

entity Authors {
  key ID: UUID;
  firstName: String(100);
  lastName: String(100);
}

// Phase 22A: Built-in functions
entity BookView as SELECT from Books {
  UPPER(title) as titleUpper,
  CONCAT('Book: ', title) as displayTitle,
  ROUND(price, 2) as roundedPrice
};

// Phase 22B: CASE expression
entity BookCategory as SELECT from Books {
  title,
  CASE
    WHEN price < 10 THEN 'Budget'
    WHEN price < 30 THEN 'Standard'
    ELSE 'Premium'
  END as category
};

// Phase 23: Subqueries and COALESCE
entity BookStats as SELECT from Books {
  title,
  COALESCE(stock, 0) as safeStock,
  (SELECT COUNT(*) FROM Orders WHERE Orders.bookID = Books.ID) as orderCount
} where EXISTS (
  SELECT 1 FROM Authors WHERE Authors.ID = Books.author.ID
);
```

### Features You'll See

#### 1. Syntax Highlighting
- Keywords in **bold** or colored
- Strings in different color
- Comments highlighted

#### 2. Content Assist (Ctrl+Space)
- Entity names
- Element names
- Keywords (entity, type, SELECT, etc.)
- Built-in types (String, Integer, UUID, etc.)

#### 3. Validation Markers

**Errors (Red X):**
```cds
entity Invalid as SELECT from Books {
  UPPER(title, title) as wrong  // ❌ ERROR: Wrong arg count
};
```

**Warnings (Yellow ⚠):**
```cds
entity Warning as SELECT from Books {
  UPPER(price) as result  // ⚠️ WARNING: Wrong arg type
};
```

**Info (Blue ℹ):**
```cds
entity Info as SELECT from Books {
  UNKNOWN_FUNC(title) as result  // ℹ️ INFO: Unknown function
};
```

#### 4. Quick Fixes (Ctrl+1)
- Hover over error markers
- Press `Ctrl+1` for quick fixes (if available)

#### 5. Outline View
- `Window` → `Show View` → `Outline`
- Shows structure of your CDS file:
  - Entities
  - Types
  - Elements

#### 6. Problems View
- `Window` → `Show View` → `Problems`
- Lists all errors, warnings, and info messages

---

## Testing All Features

### Test Built-in Functions (Phase 22A)

```cds
entity FunctionTest as SELECT from Books {
  // String functions
  UPPER(title) as upper,
  LOWER(title) as lower,
  CONCAT('Book: ', title) as concat,
  SUBSTRING(title, 1, 10) as substr,
  LENGTH(title) as len,
  TRIM(title) as trim,

  // Numeric functions
  ROUND(price, 2) as round,
  FLOOR(price) as floor,
  CEIL(price) as ceil,
  ABS(price) as abs,

  // Date/time functions
  CURRENT_DATE() as today,
  CURRENT_TIME() as now,
  CURRENT_TIMESTAMP() as timestamp
};
```

**Expected:** No errors, all functions validated

### Test CASE Expressions (Phase 22B)

```cds
entity CaseTest as SELECT from Books {
  title,
  CASE
    WHEN stock = 0 THEN 'Out of Stock'
    WHEN stock < 10 THEN 'Low Stock'
    WHEN stock < 50 THEN 'Medium Stock'
    ELSE 'In Stock'
  END as availability
};
```

**Expected:** No errors, type checking works

### Test CAST Expressions (Phase 22B)

```cds
entity CastTest as SELECT from Books {
  CAST(price AS Integer) as priceInt,
  CAST(stock AS String) as stockStr
};
```

**Expected:** No errors, target type validated

### Test excluding Clause (Phase 22B)

```cds
entity ExcludeTest as SELECT from Books {
  * excluding { internalField }
};
```

**Expected:** Warning if internalField doesn't exist

### Test COALESCE (Phase 23)

```cds
entity CoalesceTest as SELECT from Books {
  COALESCE(price, 0.0) as safePrice,
  COALESCE(stock, 0) as safeStock
};
```

**Expected:** No errors, type checking works

### Test EXISTS (Phase 23)

```cds
entity ExistsTest as SELECT from Books {
  title
} where EXISTS (
  SELECT 1 FROM Authors WHERE Authors.ID = Books.author.ID
);
```

**Expected:** No errors

### Test Subqueries (Phase 23)

```cds
entity SubqueryTest as SELECT from Books {
  title,
  (SELECT COUNT(*) FROM Reviews WHERE Reviews.bookID = Books.ID) as reviewCount
};
```

**Expected:** No errors

### Test Error Cases

```cds
// Should show errors:
entity Errors as SELECT from Books {
  UPPER(title, title) as err1,        // ❌ Wrong arg count
  COALESCE(price) as err2,            // ❌ Need 2+ args
  title as name,
  price as name                       // ❌ Duplicate alias
};
```

**Expected:** 3 errors shown in Problems view

---

## Troubleshooting

### Problem: Plugin Not Showing in Eclipse

**Solution:**
1. Check Installation Details (`Help` → `About` → `Installation Details`)
2. If not listed, try:
   - Reinstall from update site
   - Check Eclipse log: `Window` → `Show View` → `Error Log`

### Problem: .cds Files Not Recognized

**Solution:**
1. Check file associations:
   - `Preferences` → `General` → `Editors` → `File Associations`
   - Add `*.cds` → CDS Editor
2. Right-click file → `Open With` → `CDS Editor`

### Problem: No Syntax Highlighting

**Solution:**
1. Check editor is correct: File should open with CDS Editor
2. Restart Eclipse
3. Check `Error Log` for exceptions

### Problem: No Content Assist

**Solution:**
1. Wait a few seconds after opening file (parser needs to run)
2. Try typing and pressing `Ctrl+Space`
3. Check no errors in `Error Log`

### Problem: Validation Not Working

**Solution:**
1. Check `Problems` view is open: `Window` → `Show View` → `Problems`
2. Save file (Ctrl+S) to trigger validation
3. Clean project: `Project` → `Clean...`
4. Check `Error Log` for validator exceptions

### Problem: Build Errors During Maven Install

**Solution:**
1. Check Java version: `java -version` (need JDK 17+)
2. Check Maven version: `mvn -version` (need 3.8+)
3. Clean and rebuild:
   ```bash
   mvn clean
   mvn install -U -DskipTests
   ```

### Problem: Can't Find Update Site

**Solution:**
1. Make sure you built with `mvn install`
2. Check the directory exists:
   ```bash
   ls -la releng/org.example.cds.p2/target/repository/
   ```
3. If missing, build again with:
   ```bash
   mvn clean install -DskipTests
   ```

---

## Advanced Configuration

### Enable/Disable Validators

In Eclipse preferences:
1. `Window` → `Preferences`
2. Navigate to: `CDS` → `Validation` (if available)
3. Toggle validators on/off

### Adjust Memory

If Eclipse is slow, increase memory:
1. Edit `eclipse.ini` in Eclipse installation directory
2. Increase `-Xmx` value:
   ```ini
   -Xmx2048m
   ```

### Export Configuration

To share your Eclipse setup:
1. `File` → `Export` → `General` → `Preferences`
2. Select preferences to export
3. Save `.epf` file

---

## Development Workflow

### Typical Usage

1. **Create CDS file** → Eclipse recognizes `.cds` extension
2. **Write code** → Syntax highlighting works
3. **Get validation** → Errors/warnings appear as you type
4. **Use content assist** → Ctrl+Space for completions
5. **Fix errors** → Hover for details, Ctrl+1 for quick fixes
6. **Save** → Final validation runs

### Working with Large Projects

For better performance:
1. Close unused projects
2. Increase Eclipse memory (see above)
3. Disable automatic build if not needed:
   - `Project` → Uncheck `Build Automatically`

---

## Example Projects

### Load Example Files

The plugin includes example files in the `examples/` directory:

```bash
/Users/I546280/cds-eclipse-plugin/examples/
├── advanced-projection-demo.cds
├── phase22b-case-cast-excluding-demo.cds
├── phase23-subqueries-coalesce-exists-demo.cds
├── annotation-validation-demo.cds
├── foreign-key-demo.cds
├── scope-analysis-demo.cds
└── type-system-demo.cds
```

**To use:**
1. Copy example files to your Eclipse project
2. Open in Eclipse
3. Explore features and validation

---

## Next Steps

### Learn More

1. **Review Documentation:**
   - `COMPLETE_IMPLEMENTATION_SUMMARY.md` - Full feature list
   - `PHASE_22_QUICK_START.md` - Quick reference

2. **Try Examples:**
   - Open example files from `examples/` directory
   - Experiment with features

3. **Build Your Schema:**
   - Create your own CDS files
   - Use all features: functions, CASE, subqueries, etc.

### Get Help

- **Check logs:** `Window` → `Show View` → `Error Log`
- **Review problems:** `Window` → `Show View` → `Problems`
- **Consult documentation:** See project README and docs

---

## Summary

**Installation:**
```bash
# 1. Build
cd /Users/I546280/cds-eclipse-plugin
mvn clean install

# 2. In Eclipse:
# Help → Install New Software → Add → Local
# Browse to: releng/org.example.cds.p2/target/repository/

# 3. Install, restart, and use!
```

**Features Available:**
- ✅ 19 built-in functions
- ✅ CASE expressions
- ✅ CAST expressions
- ✅ COALESCE function
- ✅ Subqueries
- ✅ EXISTS predicates
- ✅ excluding clause
- ✅ Full validation
- ✅ Content assist
- ✅ Syntax highlighting

**You're ready to use the CDS plugin!** 🚀
