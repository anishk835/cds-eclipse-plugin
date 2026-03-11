# Xtext Bug Report: InternalCDSLexer.java FileNotFoundException in improveLexerCodeQuality

## Environment
- **Xtext Version:** 2.35.0
- **Maven/Tycho Version:** Tycho 4.0.4, Maven 3.x
- **Java Version:** 21
- **OS:** macOS 25.3.0 (Darwin)
- **Build Tool:** Maven with exec-maven-plugin for MWE2 workflow

## Bug Description

When modifying an existing Xtext grammar and regenerating, the build fails with `FileNotFoundException` when trying to read `InternalCDSLexer.java` during the code quality improvement phase.

## Steps to Reproduce

1. Create a working Xtext grammar project with Xtext 2.35.0
2. Successfully generate sources once (this works)
3. Modify the grammar file (any change)
4. Run `mvn generate-sources` or `mvn clean generate-sources`
5. Build fails with FileNotFoundException

## Expected Behavior

Grammar regeneration should succeed, updating all generated artifacts including the lexer.

## Actual Behavior

Build fails with:

```
java.io.FileNotFoundException: /path/to/src-gen/org/example/cds/parser/antlr/internal/InternalCDSLexer.java (No such file or directory)
	at org.eclipse.xtext.generator.JavaIoFileSystemAccess.readTextFile(JavaIoFileSystemAccess.java:295)
	at org.eclipse.xtext.generator.AbstractFileSystemAccess2.readTextFile(AbstractFileSystemAccess2.java:46)
	at org.eclipse.xtext.xtext.generator.parser.antlr.AbstractAntlrGeneratorFragment2.improveLexerCodeQuality(AbstractAntlrGeneratorFragment2.java:224)
	at org.eclipse.xtext.xtext.generator.parser.antlr.AbstractAntlrGeneratorFragment2.improveCodeQuality(AbstractAntlrGeneratorFragment2.java:211)
	at org.eclipse.xtext.xtext.generator.parser.antlr.AbstractAntlrGeneratorFragment2.splitParserAndLexerIfEnabled(AbstractAntlrGeneratorFragment2.java:203)
```

## Root Cause Analysis

The execution flow in `AbstractAntlrGeneratorFragment2` is:

1. `generateProductionGrammar()` generates `InternalCDS.g` ✅
2. Calls `splitParserAndLexerIfEnabled()` at line 203
3. This deletes the existing `InternalCDSLexer.java` to regenerate it
4. Calls `improveCodeQuality()` at line 211
5. Calls `improveLexerCodeQuality()` at line 224
6. **Tries to READ the file that was just deleted** ❌
7. FileNotFoundException because file doesn't exist yet

The bug appears to be a timing/ordering issue where:
- The old lexer file is deleted during regeneration
- But `improveLexerCodeQuality()` expects the new file to already exist
- Creating a chicken-and-egg problem

## Specific Scenario

This bug is particularly triggered when:
- Modifying grammar with complex rules (expression hierarchies with precedence)
- Adding new rule alternatives to existing definitions
- Any change that causes ANTLR grammar restructuring

Initial generation works fine, but ANY subsequent grammar modification fails.

## Minimal Reproducible Example

**Initial working grammar:**
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

Running `mvn clean generate-sources` after the modification fails with the FileNotFoundException.

## Workarounds Attempted

1. **MWE2 Configuration Changes:** Tried disabling `classSplitting`, `splitParserAndLexer` - properties don't exist or don't prevent the bug
2. **Version Downgrade:** Attempted Xtext 2.30.0, 2.34.0 - versions not available in Maven Central
3. **Manual File Preservation:** Created placeholder `InternalCDSLexer.java` - gets deleted by clean phase
4. **Deep Clean:** Complete removal of `src-gen` works for initial generation but fails on subsequent modifications

## Impact

This bug **completely blocks iterative grammar development** with Xtext 2.35.0. Once a grammar is generated successfully, no modifications can be made without hitting this error.

## Proposed Fix

The `improveLexerCodeQuality()` method should:

1. Check if the file exists before trying to read it
2. Skip code quality improvement if file doesn't exist yet
3. Or, ensure the lexer is fully generated before attempting to improve it

Potential patch location:
```java
// In AbstractAntlrGeneratorFragment2.java around line 224
protected void improveLexerCodeQuality(IXtextGeneratorFileSystemAccess fsa) {
    // ADD: Check if file exists first
    if (!fileExists(fsa, lexerFile)) {
        return; // Skip if not generated yet
    }
    // ... existing code
}
```

## Configuration Details

**MWE2 Workflow:**
```mwe2
Workflow {
    component = XtextGenerator {
        language = StandardLanguage {
            name = "org.example.cds.CDS"
            fileExtensions = "cds"
            // Standard configuration
        }
    }
}
```

**Maven Exec Plugin Configuration:**
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>mwe2-generate</id>
            <phase>generate-sources</phase>
            <goals><goal>java</goal></goals>
            <configuration>
                <mainClass>org.eclipse.emf.mwe2.launch.runtime.Mwe2Launcher</mainClass>
                <arguments>
                    <argument>/${project.basedir}/src/org/example/cds/GenerateCDS.mwe2</argument>
                    <argument>-p</argument>
                    <argument>rootPath=/${project.basedir}/../..</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Additional Information

- This blocks development of a SAP CAP CDS Eclipse plugin
- Project has successfully implemented Phases 1-11 (47% of specification)
- Cannot proceed with Phases 12-13 due to this bug
- Affects any grammar with moderately complex rules

## Questions

1. Is this a known issue in Xtext 2.35.0?
2. Is there a configuration option to disable `improveLexerCodeQuality`?
3. Are there plans to fix this in Xtext 2.36+?
4. What's the recommended approach for complex grammar development with Xtext 2.35.0?

## Related Links

- GitHub Issue: [To be created]
- Project Repository: [Private development]
- Xtext Documentation: https://www.eclipse.org/Xtext/documentation/

---

**Reporter:** Claude Code Assistant (on behalf of developer)
**Date:** 2026-03-06
**Priority:** High - Blocks iterative development
