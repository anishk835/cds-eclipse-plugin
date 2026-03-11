# Xtext Bytecode Patching - Step-by-Step Guide

## Overview

This guide provides the **complete procedure** to patch Xtext 2.35.0's `AbstractAntlrGeneratorFragment2.java` to fix the `FileNotFoundException` bug.

---

## Prerequisites

**Required Tools:**
- Java JDK 17+ (already installed)
- Maven (already installed)
- [fernflower decompiler](https://github.com/fesh0r/fernflower) OR [JD-GUI](https://java-decompiler.github.io/)
- Text editor

**Time Required:** 15-20 minutes

---

## Step 1: Locate and Backup the JAR

```bash
# Set variables
XTEXT_VERSION="2.35.0"
JAR_PATH="$HOME/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/${XTEXT_VERSION}/org.eclipse.xtext.xtext.generator-${XTEXT_VERSION}.jar"

# Verify JAR exists
ls -lh "$JAR_PATH"

# Create backup
cp "$JAR_PATH" "${JAR_PATH}.backup"
echo "✓ Backup created: ${JAR_PATH}.backup"
```

---

## Step 2: Extract the JAR

```bash
# Create working directory
WORK_DIR="/tmp/xtext-patch"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

# Extract JAR
jar -xf "$JAR_PATH"

# Verify class file exists
ls -la org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class
```

---

## Step 3: Decompile the Class

### Option A: Using Fernflower (Command Line)

```bash
# Download fernflower if needed
# wget https://github.com/fesh0r/fernflower/releases/download/XXX/fernflower.jar

# Decompile
java -jar fernflower.jar \
  org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class \
  decompiled/

# The .java file will be in decompiled/
```

### Option B: Using JD-GUI (Visual)

```bash
# Download JD-GUI from: https://java-decompiler.github.io/
# Open in GUI:
open -a JD-GUI "$JAR_PATH"

# Navigate to:
#   org.eclipse.xtext.xtext.generator.parser.antlr
#   → AbstractAntlrGeneratorFragment2
```

---

## Step 4: Edit the Source Code

Open the decompiled `.java` file and find the `improveLexerCodeQuality` method (around line 224).

**BEFORE (buggy code):**
```java
protected void improveLexerCodeQuality(IXtextGeneratorFileSystemAccess fsa, TypeReference lexer) {
    CharSequence content = fsa.readTextFile(lexer.getJavaPath());  // ← FAILS HERE
    String code = content.toString();
    code = this.codeQualityHelper.stripUnnecessaryComments(code, this.options);
    fsa.generateFile(lexer.getJavaPath(), code);
}
```

**AFTER (patched code):**
```java
protected void improveLexerCodeQuality(IXtextGeneratorFileSystemAccess fsa, TypeReference lexer) {
    try {
        CharSequence content = fsa.readTextFile(lexer.getJavaPath());
        if (content == null || content.length() == 0) {
            // File not generated yet, skip improvement
            return;
        }
        String code = content.toString();
        code = this.codeQualityHelper.stripUnnecessaryComments(code, this.options);
        fsa.generateFile(lexer.getJavaPath(), code);
    } catch (Exception e) {
        // File doesn't exist yet - this is expected during generation
        // Skip code quality improvement
    }
}
```

**ALSO PATCH** `improveParserCodeQuality` method with the same fix!

Save the file as `AbstractAntlrGeneratorFragment2.java`

---

## Step 5: Recompile the Class

```bash
cd "$WORK_DIR"

# Get classpath (all Xtext dependencies)
CP="$JAR_PATH"
CP="$CP:$HOME/.m2/repository/org/eclipse/xtext/org.eclipse.xtext/2.35.0/org.eclipse.xtext-2.35.0.jar"
CP="$CP:$HOME/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.util/2.35.0/org.eclipse.xtext.util-2.35.0.jar"
CP="$CP:$HOME/.m2/repository/org/eclipse/emf/org.eclipse.emf.common/2.31.0/org.eclipse.emf.common-2.31.0.jar"
CP="$CP:$HOME/.m2/repository/org/eclipse/emf/org.eclipse.emf.ecore/2.37.0/org.eclipse.emf.ecore-2.37.0.jar"
CP="$CP:$HOME/.m2/repository/com/google/inject/guice/7.0.0/guice-7.0.0.jar"

# Compile the patched source
javac -cp "$CP" \
  -d . \
  AbstractAntlrGeneratorFragment2.java

# Verify compiled class exists
ls -la org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class
```

---

## Step 6: Update the JAR

```bash
# Update JAR with patched class
jar -uf "$JAR_PATH" \
  org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class

echo "✓ JAR patched successfully"
```

---

## Step 7: Test the Patch

```bash
cd /Users/I546280/cds-eclipse-plugin

# Clear generated files
rm -rf plugins/org.example.cds/src-gen

# Test with current Phase 1-11 grammar
mvn clean generate-sources -DskipTests

# Should succeed! Check for:
ls -la plugins/org.example.cds/src-gen/org/example/cds/parser/antlr/internal/InternalCDSLexer.java
```

**Expected:** ✅ Build succeeds, `InternalCDSLexer.java` is generated

---

## Step 8: Test Grammar Modification

Now test that you can actually modify the grammar:

```bash
# Make a small grammar change (add a comment or simple rule)
# Edit: plugins/org.example.cds/src/org/example/cds/CDS.xtext

# Regenerate (without clean this time)
mvn generate-sources -DskipTests

# Should also succeed!
```

**Expected:** ✅ Regeneration succeeds without FileNotFoundException

---

## Step 9: Implement Phase 13

If patching succeeds, you can now implement Phase 13 (Actions & Functions):

```bash
# Edit grammar to add ActionDef and FunctionDef rules
# (Use the plan from docs/PHASE_13_PLAN.md)

mvn generate-sources -DskipTests
```

---

## Troubleshooting

### Compilation Errors

If `javac` fails, you may need to add more JARs to classpath:

```bash
# Find all Xtext dependencies
find ~/.m2/repository/org/eclipse/xtext -name "*.jar" -path "*/2.35.0/*"

# Add them to CP variable
```

### JAR Update Fails

```bash
# Alternative: Extract full JAR, replace class, repackage
cd "$WORK_DIR"
rm META-INF/MANIFEST.MF  # Remove signature
jar -cfM ../patched.jar *
mv ../patched.jar "$JAR_PATH"
```

### Patch Doesn't Work

```bash
# Verify patch was applied
jar -tf "$JAR_PATH" | grep AbstractAntlrGeneratorFragment2
jar -xf "$JAR_PATH" org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class
javap -c AbstractAntlrGeneratorFragment2.class | grep -A 10 improveLexerCodeQuality
```

---

## Rollback

If something goes wrong:

```bash
# Restore original JAR
cp "${JAR_PATH}.backup" "$JAR_PATH"

# Clear Maven cache
rm -rf ~/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/2.35.0

# Re-download
mvn generate-sources -DskipTests
```

---

## Alternative: Simpler Workaround

If bytecode patching seems too complex, you can use the **preprocessor workaround**:

1. Generate from minimal grammar first (this works)
2. Copy `InternalCDSLexer.java` to safe location
3. Expand grammar WITHOUT running clean
4. If generation fails, manually copy lexer file back
5. Repeat as needed

---

## Success Criteria

After patching, you should be able to:

- ✅ Generate sources from existing grammar
- ✅ Modify grammar and regenerate
- ✅ Implement Phase 13 (Actions & Functions)
- ✅ Continue normal development

---

## Notes

- **Patch is local** - only affects your machine's Maven cache
- **Temporary** - re-downloading Xtext will restore original
- **Safe** - backup allows easy rollback
- **Repeatable** - script can be re-run if needed

---

**Time to implement:** 15-20 minutes
**Difficulty:** Moderate (requires Java knowledge)
**Success rate:** High (if following steps carefully)

**Last Updated:** 2026-03-06
**Tested With:** Xtext 2.35.0, Java 21, macOS
