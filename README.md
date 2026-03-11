# CDS Editor for Eclipse

A native Eclipse plugin providing full IDE support for SAP CAP Core Data Services (`.cds`) files, built with Xtext and EMF.

## Features

| Feature | Status |
|---|---|
| Syntax highlighting | ✅ Phase 1 |
| Outline view | ✅ Phase 1-7 |
| Go-to-definition (Ctrl+Click) | ✅ Phase 1 |
| Cross-file references | ✅ Phase 1 |
| Associations & compositions | ✅ Phase 2 |
| Services & projections | ✅ Phase 3 |
| Annotations (@UI, @Core, ...) | ✅ Phase 4 |
| Calculated expressions | ✅ Phase 5 |
| extend / aspect / annotate | ✅ Phase 6 |
| **Enum types** | ✅ **Phase 7** |
| **Enum value references (#Value)** | ✅ **Phase 7** |
| **Enum inheritance** | ✅ **Phase 7** |
| **New integer types (UInt8, Int16)** | ✅ **Latest** |
| **Map type (JSON objects)** | ✅ **Latest** |
| **Type-as-projection** | ✅ **Latest** |
| Semantic validation | ✅ All phases |
| Content assist | ✅ All phases |
| Hover documentation | ✅ All phases |
| Auto-formatting | ✅ All phases |

## Project Structure

```
cds-eclipse-plugin/
├── plugins/
│   ├── org.example.cds/          # Grammar, AST, scoping, validation
│   ├── org.example.cds.ide/      # IDE services (Xtext-generated)
│   └── org.example.cds.ui/       # Eclipse editor UI
├── tests/
│   ├── org.example.cds.tests/    # Grammar & validation tests
│   └── org.example.cds.ui.tests/ # UI & navigation tests
├── releng/
│   ├── org.example.cds.target/   # Target platform
│   ├── org.example.cds.feature/  # Eclipse feature
│   └── org.example.cds.p2/       # P2 update site
└── samples/
    └── bookshop.cds              # Sample CDS model for smoke testing
```

## Getting Started

### Prerequisites

- Eclipse IDE for RCP and DSL Developers (2024-06+)
- JDK 17+
- Maven 3.9+ with Tycho

### Setup (Eclipse)

1. Set the active target platform: open `releng/org.example.cds.target/org.example.cds.target` and click **Set as Active Target Platform**.
2. Right-click `plugins/org.example.cds/src/org/example/cds/GenerateCDS.mwe2` → **Run As → MWE2 Workflow**. This generates the parser, lexer, and EMF model.
3. Refresh all projects (`F5`).
4. Run the Eclipse Application launch config (`Run → Run Configurations → Eclipse Application`).

### Build (Maven / Tycho)

```bash
# First build — or after changing the .target file — clear the Tycho cache first:
rm -rf ~/.m2/repository/.cache/tycho

# Full build including tests
mvn clean verify

# Skip tests for a faster iteration
mvn clean package -DskipTests

# Generate parser only (no full build)
mvn generate-sources -f plugins/org.example.cds/pom.xml
```

> **If you see "Could not find ... in the repositories" errors:** Tycho caches
> resolved target platforms aggressively. Delete `~/.m2/repository/.cache/tycho`
> and re-run. This is especially important after any edit to the `.target` file.

The P2 update site is produced at:
```
releng/org.example.cds.p2/target/repository/
```

### Install into Eclipse

`Help → Install New Software → Add → Local... → select the repository/ folder above`

## Running Tests

```bash
mvn test -pl tests/org.example.cds.tests
mvn test -pl tests/org.example.cds.ui.tests
```

## Enum Types (Phase 7)

The editor now supports SAP CAP enum types with full IDE integration, including inheritance:

```cds
// Define enums
type Color : String enum { Red; Green; Blue; }
type Status : Integer enum { Active = 1; Inactive = 0; }

// Enum inheritance
type BaseStatus : String enum { Active; Inactive; }
type ExtendedStatus : BaseStatus enum { Pending; Cancelled; }

// Use in entities with defaults
entity Products {
  color  : Color = #Red;
  status : ExtendedStatus = #Pending;
}
```

**Features:**
- ✅ String and Integer base types
- ✅ Explicit and implicit values
- ✅ Enum value references with `#` syntax
- ✅ Single and multi-level inheritance
- ✅ Circular inheritance detection
- ✅ Complete validation and IDE support

See [Enum Feature Documentation](docs/ENUM_FEATURE.md) for complete details.

## New CDS Types (Latest)

The editor now supports the latest SAP CAP built-in types:

```cds
// New integer types with smaller storage footprint
type Rating : UInt8;      // 0-255 (TINYINT / Edm.Byte)
type Stock : Int16;       // -32768 to 32767 (SMALLINT / Edm.Int16)

entity Products {
  rating: Rating default 5;
  stock: Stock default 0;
}

// Map type for flexible JSON-like objects
entity Person {
  key ID: UUID;
  name: String;
  details: Map;  // Arbitrary JSON data
}

// Type-as-projection: create types from entity projections
entity FullName {
  firstName: String;
  middleName: String;
  lastName: String;
}

type ShortName : projection on FullName {
  firstName,
  lastName
};

entity Author {
  name: ShortName;  // Only firstName and lastName
}
```

**Features:**
- ✅ UInt8 (unsigned 8-bit) and Int16 (signed 16-bit) integer types
- ✅ Map type for arbitrary JSON objects
- ✅ Type-as-projection syntax for reusable structured types
- ✅ Full type system integration with expressions and validation
- ✅ Complete IDE support (highlighting, completion, validation)

See [New Types Documentation](NEW_TYPES_UPDATE.md) and `examples/new-types-demo.cds` for complete details.

## Extending the Grammar

After editing `CDS.xtext`:

1. Re-run the MWE2 workflow.
2. Update `CDSScopeProvider` if new cross-references were added.
3. Add `@Check` methods to `CDSValidator` for new semantic constraints.
4. Add `_createChildren` / `_isLeaf` overrides to `CDSOutlineTreeProvider`.
5. Add tests.

## Architecture

```
CDS.xtext  ──MWE2──▶  Parser + Lexer (src-gen/)
                       EMF Model interfaces (cDS package)
                       AbstractCDSScopeProvider stub
                       AbstractCDSValidator stub
                       AbstractCDSProposalProvider stub

CDSScopeProvider         ← cross-reference resolution
CDSResourceDescriptionStrategy  ← workspace index contributions
CDSValidator             ← semantic checks (@Check methods)
CDSFormatter             ← auto-formatting rules
CDSOutlineTreeProvider   ← Outline view structure
CDSHoverProvider         ← hover documentation
CDSLabelProvider         ← Outline / search labels
CDSProposalProvider      ← content-assist contributions
```

## License

Eclipse Public License 2.0
