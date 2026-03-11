# Xtext Bug Workaround: Manual Patching Guide

## Problem

Xtext 2.35.0's `AbstractAntlrGeneratorFragment2.improveLexerCodeQuality()` tries to read `InternalCDSLexer.java` before it exists, causing `FileNotFoundException` during grammar regeneration.

## Solution Options

### Option 1: Create Custom Generator Fragment (RECOMMENDED)

Create a custom Java class that extends Xtext's generator and skips the problematic code quality improvement.

**Step 1:** Create custom fragment class

File: `/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/generator/SafeAntlrGeneratorFragment.java`

```java
package org.example.cds.generator;

import org.eclipse.xtext.xtext.generator.model.IXtextGeneratorFileSystemAccess;
import org.eclipse.xtext.xtext.generator.model.TypeReference;
import org.eclipse.xtext.xtext.generator.parser.antlr.XtextAntlrGeneratorFragment2;

/**
 * Custom ANTLR generator fragment that safely handles lexer code quality improvements.
 * Workaround for Xtext 2.35.0 bug where improveLexerCodeQuality tries to read
 * a file that doesn't exist yet.
 */
public class SafeAntlrGeneratorFragment extends XtextAntlrGeneratorFragment2 {

    @Override
    protected void improveLexerCodeQuality(IXtextGeneratorFileSystemAccess fsa, TypeReference lexer) {
        try {
            // Check if file exists before trying to read it
            String path = lexer.getJavaPath();
            CharSequence content = fsa.readTextFile(path);

            if (content != null && content.length() > 0) {
                // File exists and has content, safe to improve
                super.improveLexerCodeQuality(fsa, lexer);
            } else {
                // File doesn't exist or is empty, skip improvement
                getLog().info("Skipping lexer code quality improvement - file not ready yet");
            }
        } catch (Exception e) {
            // If read fails for any reason, log and skip
            getLog().warn("Could not read lexer file for quality improvement: " + e.getMessage());
            getLog().info("Continuing without lexer code quality improvements");
        }
    }

    @Override
    protected void improveParserCodeQuality(IXtextGeneratorFileSystemAccess fsa, TypeReference parser) {
        try {
            String path = parser.getJavaPath();
            CharSequence content = fsa.readTextFile(path);

            if (content != null && content.length() > 0) {
                super.improveParserCodeQuality(fsa, parser);
            } else {
                getLog().info("Skipping parser code quality improvement - file not ready yet");
            }
        } catch (Exception e) {
            getLog().warn("Could not read parser file for quality improvement: " + e.getMessage());
            getLog().info("Continuing without parser code quality improvements");
        }
    }
}
```

**Step 2:** Update MWE2 workflow to use custom fragment

This approach won't work easily because `StandardLanguage` creates the fragment internally. We need a different approach.

---

### Option 2: Bytecode Patching (ADVANCED)

Directly patch the Xtext JAR to fix the bug.

**Requirements:**
- Java decompiler (e.g., JD-GUI, Fernflower)
- Java compiler
- JAR manipulation tools

**Steps:**

1. **Backup original JAR:**
```bash
cp /Users/I546280/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/2.35.0/org.eclipse.xtext.xtext.generator-2.35.0.jar \
   /Users/I546280/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/2.35.0/org.eclipse.xtext.xtext.generator-2.35.0.jar.backup
```

2. **Extract the JAR:**
```bash
cd /tmp/xtext-patch
jar -xf /Users/I546280/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/2.35.0/org.eclipse.xtext.xtext.generator-2.35.0.jar
```

3. **Decompile the problematic class:**
```bash
# Using fernflower or similar decompiler
java -jar fernflower.jar \
  org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class \
  /tmp/decompiled/
```

4. **Modify the source code** to add try-catch around `readTextFile`:

```java
protected void improveLexerCodeQuality(IXtextGeneratorFileSystemAccess fsa, TypeReference lexer) {
    try {
        CharSequence content = fsa.readTextFile(lexer.getJavaPath());
        if (content == null || content.length() == 0) {
            return; // Skip if file doesn't exist or is empty
        }
        String code = content.toString();
        code = this.codeQualityHelper.stripUnnecessaryComments(code, this.options);
        fsa.generateFile(lexer.getJavaPath(), code);
    } catch (Exception e) {
        // File doesn't exist yet, skip code quality improvement
        System.err.println("Skipping lexer code quality: " + e.getMessage());
    }
}
```

5. **Recompile the class:**
```bash
javac -cp "original-jar-classpath/*" AbstractAntlrGeneratorFragment2.java
```

6. **Replace class in JAR:**
```bash
jar -uf /Users/I546280/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/2.35.0/org.eclipse.xtext.xtext.generator-2.35.0.jar \
  org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class
```

7. **Clear Maven cache and rebuild:**
```bash
rm -rf /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/target
mvn clean generate-sources -DskipTests
```

---

### Option 3: Gradle Shadow Plugin Approach

Use Gradle to create a shaded JAR with the patched class.

**Build file (if switching to Gradle):**
```gradle
shadowJar {
    relocate 'org.eclipse.xtext.xtext.generator.parser.antlr.AbstractAntlrGeneratorFragment2',
             'org.example.cds.patched.AbstractAntlrGeneratorFragment2'
}
```

Not recommended as it requires converting the entire build system.

---

### Option 4: Maven Dependency Override

Create a custom Maven module with the patched class that takes precedence.

**Structure:**
```
plugins/org.example.cds.xtext.patch/
├── pom.xml
└── src/main/java/org/eclipse/xtext/xtext/generator/parser/antlr/
    └── AbstractAntlrGeneratorFragment2.java (patched)
```

**POM dependency order:**
```xml
<dependencies>
    <!-- Patch MUST come before xtext-generator -->
    <dependency>
        <groupId>org.example.cds</groupId>
        <artifactId>org.example.cds.xtext.patch</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.eclipse.xtext</groupId>
        <artifactId>org.eclipse.xtext.xtext.generator</artifactId>
        <version>${xtext.version}</version>
    </dependency>
</dependencies>
```

---

### Option 5: Wait for Xtext 2.36+ (EASIEST)

Monitor Xtext releases for a fix. Check:
- https://github.com/eclipse/xtext-core/releases
- https://github.com/eclipse/xtext-core/issues

---

## Recommended Approach

**For immediate progress:** Option 2 (Bytecode Patching)

**Pros:**
- Direct fix to the root cause
- No architecture changes needed
- Can proceed with Phase 13+ immediately

**Cons:**
- Manual patching required
- Must redo if Xtext version changes
- Requires Java bytecode manipulation skills

**For long-term stability:** Option 5 (Wait for official fix) + Option 1 (Custom fragment as interim)

---

## Testing the Patch

After applying any patch:

1. **Test minimal grammar generation:**
```bash
mvn clean generate-sources -DskipTests -pl plugins/org.example.cds
```

2. **Test grammar modification:**
```bash
# Modify CDS.xtext (add a simple rule)
mvn generate-sources -DskipTests -pl plugins/org.example.cds
```

3. **Test Phase 13 implementation:**
```bash
# Add ActionDef/FunctionDef to grammar
mvn clean generate-sources -DskipTests
```

If all three succeed, the patch is working.

---

## Rollback Procedure

If patching fails:

```bash
# Restore original JAR
mv /Users/I546280/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/2.35.0/org.eclipse.xtext.xtext.generator-2.35.0.jar.backup \
   /Users/I546280/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/2.35.0/org.eclipse.xtext.xtext.generator-2.35.0.jar

# Restore grammar to Phase 1-11
cp /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext.phase11 \
   /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext

# Rebuild
mvn clean package -DskipTests
```

---

## Additional Resources

- **Xtext GitHub Issues:** https://github.com/eclipse/xtext-core/issues
- **Xtext Forum:** https://www.eclipse.org/forums/index.php?t=thread&frm_id=27
- **Stack Overflow xtext tag:** https://stackoverflow.com/questions/tagged/xtext

---

**Last Updated:** 2026-03-06
**Xtext Version:** 2.35.0
**Status:** Workaround required, official fix pending
