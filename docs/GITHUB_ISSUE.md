# FileNotFoundException in improveLexerCodeQuality during grammar regeneration (Xtext 2.35.0 - 2.42.0)

## Environment
- **Xtext Version:** 2.35.0 - 2.42.0 (bug confirmed in both)
- **Build Tool:** Maven with Tycho 4.0.4, exec-maven-plugin for MWE2
- **Java Version:** 21
- **OS:** macOS (Darwin), but likely affects all platforms

## Description

When modifying an existing Xtext grammar and regenerating sources, the build fails with `FileNotFoundException` when `AbstractAntlrGeneratorFragment2.improveLexerCodeQuality()` tries to read `InternalCDSLexer.java` before it exists.

**Initial generation succeeds, but ANY subsequent grammar modification fails.**

⚠️ **CONFIRMED:** This bug exists in **Xtext 2.35.0 AND 2.42.0** (latest as of March 2026)

## Steps to Reproduce

1. Create a working Xtext project with Xtext 2.35.0
2. Generate sources successfully once: `mvn generate-sources` ✅
3. Modify the grammar file (even minor changes)
4. Regenerate: `mvn generate-sources` or `mvn clean generate-sources` ❌

## Expected Behavior

Grammar regeneration should succeed, updating all generated artifacts.

## Actual Behavior

Build fails with:

```
java.io.FileNotFoundException: /path/to/src-gen/org/example/parser/antlr/internal/InternalLexer.java (No such file or directory)
	at org.eclipse.xtext.generator.JavaIoFileSystemAccess.readTextFile(JavaIoFileSystemAccess.java:295)
	at org.eclipse.xtext.generator.AbstractFileSystemAccess2.readTextFile(AbstractFileSystemAccess2.java:46)
	at org.eclipse.xtext.xtext.generator.parser.antlr.AbstractAntlrGeneratorFragment2.improveLexerCodeQuality(AbstractAntlrGeneratorFragment2.java:224)
	at org.eclipse.xtext.xtext.generator.parser.antlr.AbstractAntlrGeneratorFragment2.improveCodeQuality(AbstractAntlrGeneratorFragment2.java:211)
	at org.eclipse.xtext.xtext.generator.parser.antlr.AbstractAntlrGeneratorFragment2.splitParserAndLexerIfEnabled(AbstractAntlrGeneratorFragment2.java:203)
```

## Root Cause

The execution flow in `AbstractAntlrGeneratorFragment2`:

1. `generateProductionGrammar()` generates ANTLR grammar ✅
2. Calls `splitParserAndLexerIfEnabled()` (line 203)
3. Deletes existing lexer file for regeneration
4. Calls `improveCodeQuality()` (line 211)
5. Calls `improveLexerCodeQuality()` (line 224)
6. **Tries to read the file that was just deleted** ❌
7. FileNotFoundException

This is a timing/ordering issue where the code quality improvement runs before the new lexer file is generated.

## Minimal Reproducible Example

**Initial grammar (generates successfully):**
```xtext
grammar org.example.Test with org.eclipse.xtext.common.Terminals
generate test "http://example.org/test"

Model: elements+=Element*;
Element: 'element' name=ID ';';
```

**After modification (triggers bug):**
```xtext
grammar org.example.Test with org.eclipse.xtext.common.Terminals
generate test "http://example.org/test"

Model: elements+=Element*;
Element: 'element' name=ID ('=' value=STRING)? ';';  // Added optional value
```

Run `mvn generate-sources` → FileNotFoundException

## Impact

**This bug completely blocks iterative grammar development** with Xtext 2.35.0 through 2.42.0. Once a grammar generates successfully, no modifications can be made without hitting this error.

**Affects multiple Xtext versions:** Tested and confirmed in 2.35.0 and 2.42.0, likely affects all versions in between.

Particularly affects:
- Grammars with complex rule structures (expression hierarchies, precedence)
- Adding alternatives to existing rules
- Any change causing ANTLR grammar restructuring

## Proposed Fix

Add existence check in `improveLexerCodeQuality()`:

```java
// In AbstractAntlrGeneratorFragment2.java around line 224
protected void improveLexerCodeQuality(IXtextGeneratorFileSystemAccess fsa, TypeReference lexer) {
    try {
        CharSequence content = fsa.readTextFile(lexer.getJavaPath());
        if (content == null || content.length() == 0) {
            return; // Skip if file not generated yet
        }
        // ... existing code quality improvement logic
    } catch (Exception e) {
        // Log and skip if file doesn't exist
        getLog().debug("Skipping lexer code quality improvement: " + e.getMessage());
    }
}
```

Same fix needed for `improveParserCodeQuality()`.

## Workarounds Attempted

- ❌ MWE2 configuration changes (no suitable properties exist)
- ❌ Version downgrade (2.30.0, 2.34.0 unavailable)
- ❌ Manual file preservation (gets deleted by clean)
- ⚠️ Deep clean works once but fails on next modification

## Configuration

**Standard MWE2:**
```mwe2
Workflow {
    component = XtextGenerator {
        language = StandardLanguage {
            name = "org.example.CDS"
            fileExtensions = "cds"
        }
    }
}
```

**Maven exec plugin** with standard MWE2 launcher configuration.

## Questions

1. Is this a known issue in Xtext 2.35.0?
2. Is there a configuration option to disable code quality improvements?
3. Will this be fixed in Xtext 2.36+?
4. What's the recommended workaround for complex grammar development?

## Additional Context

This bug blocks development of a SAP CAP CDS Eclipse plugin that has successfully implemented 11 phases (47% of spec) but cannot proceed further due to this issue.

---

**Labels:** bug, xtext-generator, antlr
**Milestone:** 2.36.0 (suggested)
**Priority:** High - Blocks grammar development
