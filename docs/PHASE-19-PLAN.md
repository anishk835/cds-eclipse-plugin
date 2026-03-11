# Implementation Plan: Phase 19 - Scope Analysis (3% Coverage)

## Context

**Current State:**
- ✅ Coverage: ~83% of SAP CAP CDS specification (after Phase 18)
- ✅ Type system implemented - catches type errors in expressions
- ✅ Basic scoping via CDSScopeProvider - handles single-file resolution
- ❌ **NO CROSS-FILE VALIDATION** - doesn't validate imported types exist
- ❌ **NO SCOPE DIAGNOSTICS** - unresolved references fail silently
- ❌ **LIMITED NAMESPACE SUPPORT** - namespace scoping not fully validated

**The Problem:**
Currently, the parser accepts invalid CDS like:
```cds
// File1.cds
namespace bookshop;
using { Currency } from './nonexistent';  // No error - file doesn't exist
using { Product } from './File2';         // No error - Product doesn't exist in File2

entity Books {
  price: Currency;  // No error - Currency unresolved
  item: Product;    // No error - Product unresolved
}
```

**Why Scope Analysis Matters (Production SAP CAP):**
1. **Catches import errors** - detects missing files/definitions early
2. **Better error messages** - "Currency not found in ./common" vs silent failure
3. **Namespace validation** - ensures proper namespace usage
4. **IDE navigation** - enables "Go to Definition" to work correctly
5. **Refactoring support** - safe renames across files

## Recommended Approach: Enhanced Scope Provider + Validators

### Architecture Decision

**Option A: Full Index-Based Resolution (Heavyweight)**
- Pros: Global workspace awareness, fast lookups
- Cons: Complex setup, requires workspace indexing, memory overhead

**Option B: Enhanced Scope Provider + Validation (Recommended)**
- Pros: Builds on existing infrastructure, incremental improvement
- Cons: Per-file validation (not global)
- **Best for:** Production SAP CAP with existing CDSScopeProvider

**Rationale:**
1. CDSScopeProvider already handles basic cross-file resolution via ImportedNamespaceAwareLocalScopeProvider
2. Need to add validation to detect when resolution fails
3. Add better error messages for common scope issues
4. Minimal changes, maximal impact

### Scope Analysis Components

```
ScopeValidator (new validator methods in CDSValidator)
├── checkUsingStatementResolution() → validates imported files exist
├── checkTypeReferenceResolution() → validates types are resolved
├── checkNamespaceConsistency() → validates namespace declarations
└── checkAmbiguousReferences() → detects conflicting imports

ScopeHelper (utility class)
├── isResolved(EObject) → checks if cross-reference is resolved
├── getImportSource(UsingDirective) → extracts file path from using
├── resolveFile(String path, Resource current) → finds imported resource
└── collectVisibleDefinitions(Scope) → gets all accessible definitions

Enhanced CDSScopeProvider (modifications)
├── Add better error handling for missing imports
├── Improve namespace resolution logic
└── Cache resolved scopes for performance
```

## Implementation Steps

### Step 1: Create ScopeHelper Utility Class

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/scoping/ScopeHelper.java` (NEW)

**Purpose:** Helper methods for scope validation

```java
package org.example.cds.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.example.cds.cDS.Definition;
import org.example.cds.cDS.UsingDirective;
import com.google.inject.Inject;
import java.util.Optional;

public class ScopeHelper {

    @Inject
    private IResourceDescriptions resourceDescriptions;

    /**
     * Checks if a cross-reference is resolved (not a proxy).
     */
    public boolean isResolved(EObject obj) {
        return obj != null && !obj.eIsProxy();
    }

    /**
     * Extracts the import source path from a using directive.
     * Returns empty if no 'from' clause.
     */
    public Optional<String> getImportSource(UsingDirective usingDir) {
        String from = usingDir.getFrom();
        if (from == null || from.isEmpty()) {
            return Optional.empty();
        }
        // Remove quotes: './common' -> ./common
        from = from.replaceAll("^['\"]|['\"]$", "");
        return Optional.of(from);
    }

    /**
     * Resolves a file path relative to the current resource.
     * Returns true if the resource can be loaded.
     */
    public boolean canResolveFile(String path, Resource currentResource) {
        if (path == null || currentResource == null) return false;

        try {
            // Handle relative paths
            String normalizedPath = normalizePath(path, currentResource);

            // Check if resource exists in workspace
            if (currentResource.getResourceSet() instanceof XtextResourceSet) {
                XtextResourceSet resourceSet = (XtextResourceSet) currentResource.getResourceSet();
                org.eclipse.emf.common.util.URI uri =
                    org.eclipse.emf.common.util.URI.createURI(normalizedPath);

                // Try to load the resource
                Resource targetResource = resourceSet.getResource(uri, true);
                return targetResource != null && !targetResource.getContents().isEmpty();
            }
        } catch (Exception e) {
            // Resource not found or cannot be loaded
            return false;
        }

        return false;
    }

    /**
     * Normalizes a relative path to absolute URI.
     */
    private String normalizePath(String path, Resource currentResource) {
        org.eclipse.emf.common.util.URI currentUri = currentResource.getURI();
        org.eclipse.emf.common.util.URI resolvedUri =
            org.eclipse.emf.common.util.URI.createURI(path).resolve(currentUri);
        return resolvedUri.toString();
    }

    /**
     * Checks if a definition is visible (not a proxy and has a name).
     */
    public boolean isVisible(Definition def) {
        return def != null && !def.eIsProxy() && def.getName() != null;
    }
}
```

**Lines:** ~80

---

### Step 2: Add Scope Validation to CDSValidator

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add after Phase 18 validation methods:**

```java
// ── Phase 19: Scope Analysis ──────────────────────────────────────────────

@Inject
private ScopeHelper scopeHelper;

/**
 * Validates that using directives can resolve their imports.
 */
@Check(CheckType.NORMAL)
public void checkUsingStatementResolution(UsingDirective usingDir) {
    Optional<String> importSource = scopeHelper.getImportSource(usingDir);

    if (importSource.isPresent()) {
        String path = importSource.get();
        Resource currentResource = usingDir.eResource();

        if (!scopeHelper.canResolveFile(path, currentResource)) {
            warning("Cannot resolve import: '" + path + "'",
                usingDir,
                CdsPackage.Literals.USING_DIRECTIVE__FROM,
                CODE_UNRESOLVED_IMPORT);
        }
    }

    // Check if imported definitions are resolved
    for (Definition imported : usingDir.getImports()) {
        if (!scopeHelper.isResolved(imported)) {
            error("Cannot resolve imported definition: '" +
                  getImportedName(usingDir, imported) + "'",
                usingDir,
                CdsPackage.Literals.USING_DIRECTIVE__IMPORTS,
                CODE_UNRESOLVED_IMPORT);
        }
    }
}

private String getImportedName(UsingDirective usingDir, Definition imported) {
    // Try to get the name from the using directive syntax
    String text = usingDir.toString();
    if (text.contains("{") && text.contains("}")) {
        // Extract name from "using { Name } from ..."
        return text.substring(text.indexOf("{") + 1, text.indexOf("}")).trim();
    }
    return imported != null ? imported.getName() : "unknown";
}

/**
 * Validates that type references are resolved.
 */
@Check(CheckType.FAST)
public void checkTypeReferenceResolution(SimpleTypeRef typeRef) {
    Definition typeDef = typeRef.getRef();

    if (!scopeHelper.isResolved(typeDef)) {
        error("Cannot resolve type: '" + getTypeRefName(typeRef) + "'",
            typeRef,
            CdsPackage.Literals.SIMPLE_TYPE_REF__REF,
            CODE_UNRESOLVED_TYPE);
    }
}

private String getTypeRefName(SimpleTypeRef typeRef) {
    // Try to get the name from the cross-reference
    if (typeRef.getRef() != null && typeRef.getRef().getName() != null) {
        return typeRef.getRef().getName();
    }
    // Fallback to text representation
    return typeRef.toString();
}

/**
 * Validates namespace consistency within a file.
 */
@Check(CheckType.FAST)
public void checkNamespaceConsistency(CdsFile file) {
    String declaredNamespace = file.getNamespace();

    if (declaredNamespace != null && !declaredNamespace.isEmpty()) {
        // Check if all top-level definitions use the namespace
        for (Definition def : file.getDefinitions()) {
            if (def.getName() != null && !def.getName().startsWith(declaredNamespace)) {
                // This is actually fine - CDS allows short names
                // But we can provide an info hint for consistency
                info("Definition '" + def.getName() + "' does not use namespace prefix",
                    def,
                    CdsPackage.Literals.DEFINITION__NAME,
                    CODE_NAMESPACE_HINT);
            }
        }
    }
}

/**
 * Detects ambiguous references (same name imported from multiple sources).
 */
@Check(CheckType.NORMAL)
public void checkAmbiguousImports(CdsFile file) {
    Map<String, List<UsingDirective>> importsByName = new HashMap<>();

    for (UsingDirective usingDir : file.getUsings()) {
        for (Definition imported : usingDir.getImports()) {
            if (scopeHelper.isResolved(imported)) {
                String name = imported.getName();
                importsByName.computeIfAbsent(name, k -> new ArrayList<>())
                    .add(usingDir);
            }
        }
    }

    // Check for duplicates
    for (Map.Entry<String, List<UsingDirective>> entry : importsByName.entrySet()) {
        if (entry.getValue().size() > 1) {
            String name = entry.getKey();
            for (UsingDirective usingDir : entry.getValue()) {
                warning("Ambiguous import: '" + name +
                        "' is imported from multiple sources",
                    usingDir,
                    CdsPackage.Literals.USING_DIRECTIVE__IMPORTS,
                    CODE_AMBIGUOUS_IMPORT);
            }
        }
    }
}

/**
 * Validates association target resolution.
 */
@Check(CheckType.FAST)
public void checkAssociationTargetResolution(AssocDef assoc) {
    Definition target = assoc.getTarget();

    if (!scopeHelper.isResolved(target)) {
        error("Cannot resolve association target: '" + getAssocTargetName(assoc) + "'",
            assoc,
            CdsPackage.Literals.ASSOC_DEF__TARGET,
            CODE_UNRESOLVED_ASSOC);
    }
}

private String getAssocTargetName(AssocDef assoc) {
    if (assoc.getTarget() != null && assoc.getTarget().getName() != null) {
        return assoc.getTarget().getName();
    }
    return "unknown";
}
```

**Add diagnostic codes** (after Phase 18 codes):

```java
// Phase 19: Scope analysis validation codes
public static final String CODE_UNRESOLVED_IMPORT     = "cds.unresolved.import";
public static final String CODE_AMBIGUOUS_IMPORT      = "cds.ambiguous.import";
public static final String CODE_NAMESPACE_HINT        = "cds.namespace.hint";
```

**Lines Added:** ~140

---

### Step 3: Add Import Statements

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`

**Add imports:**

```java
import org.example.cds.scoping.ScopeHelper;
import org.example.cds.cDS.UsingDirective;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
```

---

### Step 4: Enhance CDSScopeProvider

**File:** `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/scoping/CDSScopeProvider.java`

**Current implementation already handles cross-file resolution via ImportedNamespaceAwareLocalScopeProvider.**

**Add error logging for debugging:**

```java
// Add import
import org.apache.log4j.Logger;

public class CDSScopeProvider extends AbstractCDSScopeProvider {

    private static final Logger logger = Logger.getLogger(CDSScopeProvider.class);

    @Override
    public IScope getScope(EObject context, EReference reference) {
        IScope scope = super.getScope(context, reference);

        // Log unresolved references for debugging
        if (context instanceof SimpleTypeRef) {
            SimpleTypeRef typeRef = (SimpleTypeRef) context;
            if (typeRef.getRef() != null && typeRef.getRef().eIsProxy()) {
                logger.debug("Unresolved type reference: " + typeRef);
            }
        }

        return scope;
    }
}
```

**Lines Added:** ~15

---

## Testing Strategy

### Test Cases to Add

**File:** `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/ScopeAnalysisTest.java` (NEW)

```java
package org.example.cds.tests;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.CdsPackage;
import org.example.cds.validation.CDSValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.google.inject.Inject;

/**
 * Phase 19: Scope Analysis Tests
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(CDSInjectorProvider.class)
public class ScopeAnalysisTest {

    @Inject
    private ParseHelper<CdsFile> parseHelper;

    @Inject
    private ValidationTestHelper validationHelper;

    private CdsFile parse(String input) throws Exception {
        return parseHelper.parse(input);
    }

    @Test
    public void testUnresolvedTypeReference() throws Exception {
        CdsFile file = parse("""
            namespace test;

            entity Books {
              key ID: UUID;
              title: String;
              author: NonExistentType;
            }
            """);

        validationHelper.assertError(file,
            CdsPackage.Literals.SIMPLE_TYPE_REF,
            CDSValidator.CODE_UNRESOLVED_TYPE);
    }

    @Test
    public void testResolvedBuiltInType() throws Exception {
        CdsFile file = parse("""
            namespace test;

            entity Books {
              key ID: UUID;
              title: String;
              price: Decimal;
            }
            """);

        validationHelper.assertNoErrors(file);
    }

    @Test
    public void testUnresolvedAssociationTarget() throws Exception {
        CdsFile file = parse("""
            namespace test;

            entity Books {
              key ID: UUID;
              author: Association to NonExistentEntity;
            }
            """);

        validationHelper.assertError(file,
            CdsPackage.Literals.ASSOC_DEF,
            CDSValidator.CODE_UNRESOLVED_ASSOC);
    }

    @Test
    public void testValidAssociationTarget() throws Exception {
        CdsFile file = parse("""
            namespace test;

            entity Authors {
              key ID: UUID;
            }

            entity Books {
              key ID: UUID;
              author: Association to Authors;
            }
            """);

        validationHelper.assertNoErrors(file);
    }

    @Test
    public void testNamespaceConsistency() throws Exception {
        CdsFile file = parse("""
            namespace bookshop;

            entity Books {
              key ID: UUID;
            }
            """);

        // Should have info hint about namespace prefix
        validationHelper.assertInfo(file,
            CdsPackage.Literals.DEFINITION,
            CDSValidator.CODE_NAMESPACE_HINT);
    }
}
```

**Lines:** ~120

---

## Critical Files

### Files to Create (NEW):
1. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/scoping/ScopeHelper.java` (~80 lines)
2. `/Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/ScopeAnalysisTest.java` (~120 lines)

### Files to Modify:
1. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java`
   - Add 3 diagnostic codes
   - Add 5 validation methods (~140 lines)
   - Add imports

2. `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/scoping/CDSScopeProvider.java`
   - Add logging (~15 lines)

### No Changes Needed:
- ✅ Grammar (CDS.xtext) - stays at 389 lines
- ✅ Parser - no regeneration needed
- ✅ Build configuration - scoping package already exported

---

## Success Criteria

**Phase 19 Complete When:**
- ✅ ScopeHelper class created (~80 lines)
- ✅ 5 validation methods added to CDSValidator (~140 lines)
- ✅ 3 diagnostic codes added
- ✅ CDSScopeProvider enhanced (~15 lines)
- ✅ Build succeeds
- ✅ Unresolved type references show errors
- ✅ Unresolved imports show warnings
- ✅ Test file created (~120 lines)

**Coverage Impact:**
- Before: ~83%
- After: ~86% (Scope Analysis adds 3%)

**Total New Code:**
- New classes: ~80 lines
- Validator additions: ~140 lines
- Scope provider: ~15 lines
- Tests: ~120 lines
- **Total: ~355 lines** (excluding comments/blank lines)

---

## Next Steps After Phase 19

**Phase 20: Foreign Keys (2%)** - Enhanced ON condition validation
**Total Coverage After Phases 19-20: ~88%**

This plan provides production-ready scope analysis for SAP CAP applications with cross-file validation and better error messages.
