#!/bin/bash
# Xtext 2.35.0 Bytecode Patcher
# Patches AbstractAntlrGeneratorFragment2 to fix FileNotFoundException bug

set -e

echo "=========================================="
echo "Xtext 2.35.0 Bytecode Patcher"
echo "=========================================="
echo ""

XTEXT_VERSION="2.35.0"
JAR_PATH="$HOME/.m2/repository/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/${XTEXT_VERSION}/org.eclipse.xtext.xtext.generator-${XTEXT_VERSION}.jar"
BACKUP_PATH="${JAR_PATH}.backup"
WORK_DIR="/tmp/xtext-patch-$$"
CLASS_PATH="org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class"

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "❌ ERROR: Xtext JAR not found at:"
    echo "   $JAR_PATH"
    echo ""
    echo "Please run 'mvn generate-sources' first to download Xtext."
    exit 1
fi

# Check if already backed up
if [ -f "$BACKUP_PATH" ]; then
    echo "⚠️  Backup already exists at:"
    echo "   $BACKUP_PATH"
    echo ""
    read -p "Restore from backup and re-patch? (y/N): " response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        echo "📦 Restoring original JAR from backup..."
        cp "$BACKUP_PATH" "$JAR_PATH"
    else
        echo "❌ Aborted. Remove backup manually if needed."
        exit 1
    fi
fi

# Create backup
echo "💾 Creating backup..."
cp "$JAR_PATH" "$BACKUP_PATH"
echo "✓ Backup saved to: $BACKUP_PATH"
echo ""

# Extract JAR
echo "📦 Extracting JAR to $WORK_DIR..."
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"
jar -xf "$JAR_PATH"
echo "✓ JAR extracted"
echo ""

# Check if class file exists
if [ ! -f "$CLASS_PATH" ]; then
    echo "❌ ERROR: Class file not found in JAR:"
    echo "   $CLASS_PATH"
    exit 1
fi

# Attempt to use javassist for patching (if available)
echo "🔧 Attempting to patch class file..."
echo ""
echo "⚠️  WARNING: This requires Java bytecode manipulation."
echo "   We'll use a Java-based patcher script."
echo ""

# Create Java patcher on the fly
cat > /tmp/XtextPatcher.java << 'JAVA_EOF'
import java.io.*;
import java.nio.file.*;
import java.util.jar.*;

public class XtextPatcher {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: XtextPatcher <work-dir> <jar-path>");
            System.exit(1);
        }

        String workDir = args[0];
        String jarPath = args[1];
        String classPath = "org/eclipse/xtext/xtext/generator/parser/antlr/AbstractAntlrGeneratorFragment2.class";

        System.out.println("🔧 Patching strategy: Method wrapping with try-catch");
        System.out.println("");

        // Read the class bytes
        File classFile = new File(workDir, classPath);
        byte[] classBytes = Files.readAllBytes(classFile.toPath());

        // Simple bytecode patch: We'll add a try-catch wrapper
        // This is a simplified approach - in production you'd use ASM or Javassist

        System.out.println("⚠️  Simple patching not feasible without bytecode library.");
        System.out.println("   Recommended: Manual class override approach.");
        System.out.println("");
        System.out.println("Alternative solution:");
        System.out.println("   Create override class in project src/");
        System.exit(0);
    }
}
JAVA_EOF

# Compile and run patcher
javac /tmp/XtextPatcher.java 2>/dev/null || true
java -cp /tmp XtextPatcher "$WORK_DIR" "$JAR_PATH" || true

# Alternative: Create a patched source file for manual compilation
echo "📝 Creating patched source file for manual approach..."
mkdir -p patched-source/org/eclipse/xtext/xtext/generator/parser/antlr

cat > patched-source/AbstractAntlrGeneratorFragment2_PATCH.txt << 'PATCH_EOF'
// PATCHED VERSION - Add this to improveLexerCodeQuality method:

protected void improveLexerCodeQuality(IXtextGeneratorFileSystemAccess fsa, TypeReference lexer) {
    try {
        // PATCH: Wrap in try-catch to handle missing file
        CharSequence content = fsa.readTextFile(lexer.getJavaPath());
        if (content == null || content.length() == 0) {
            getLog().info("Skipping lexer code quality - file not ready");
            return;
        }

        // Original code:
        String code = content.toString();
        code = this.codeQualityHelper.stripUnnecessaryComments(code, this.options);
        fsa.generateFile(lexer.getJavaPath(), code);

    } catch (Exception e) {
        // PATCH: Catch and log instead of failing
        getLog().info("Skipping lexer code quality improvement: " + e.getMessage());
    }
}

// Apply same fix to improveParserCodeQuality!
PATCH_EOF

echo "✓ Patch instructions created"
echo ""

# Cleanup
cd /
rm -rf "$WORK_DIR"

echo "=========================================="
echo "PATCHING RESULT"
echo "=========================================="
echo ""
echo "❌ Automated bytecode patching requires ASM library."
echo ""
echo "✅ MANUAL SOLUTION AVAILABLE:"
echo ""
echo "   Use the Maven dependency override approach:"
echo ""
echo "   1. See: docs/XTEXT_BUG_WORKAROUND.md"
echo "   2. Option 4: Maven Dependency Override"
echo ""
echo "   This creates a project-level override class"
echo "   that Maven loads before the Xtext JAR."
echo ""
echo "OR restore backup and use current Phase 1-11 grammar:"
echo ""
echo "   cp $BACKUP_PATH $JAR_PATH"
echo ""
echo "=========================================="
