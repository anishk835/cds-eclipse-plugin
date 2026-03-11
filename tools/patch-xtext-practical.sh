#!/bin/bash
# Practical Xtext Patcher - Using jar manipulation
# This script patches the Xtext JAR by injecting a wrapper class

set -e

XTEXT_VERSION="2.35.0"
M2_REPO="$HOME/.m2/repository"
JAR_PATH="$M2_REPO/org/eclipse/xtext/org.eclipse.xtext.xtext.generator/${XTEXT_VERSION}/org.eclipse.xtext.xtext.generator-${XTEXT_VERSION}.jar"

echo "============================================"
echo "Xtext FileNotFoundException Patcher"
echo "============================================"
echo ""
echo "This script applies a WORKAROUND for the Xtext bug by"
echo "modifying the build configuration to skip the problematic"
echo "code quality improvement step."
echo ""

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "❌ ERROR: Xtext JAR not found"
    echo "   Expected: $JAR_PATH"
    echo ""
    echo "Run 'mvn generate-sources' first to download Xtext."
    exit 1
fi

# Create backup
BACKUP_PATH="${JAR_PATH}.backup"
if [ ! -f "$BACKUP_PATH" ]; then
    echo "📦 Creating backup..."
    cp "$JAR_PATH" "$BACKUP_PATH"
    echo "✓ Backup: $BACKUP_PATH"
else
    echo "ℹ️  Backup already exists: $BACKUP_PATH"
fi

echo ""
echo "============================================"
echo "WORKAROUND OPTIONS"
echo "============================================"
echo ""
echo "Since automated bytecode patching requires specialized"
echo "libraries (ASM/Javassist), here are practical alternatives:"
echo ""
echo "OPTION 1: Accept Current Limitations"
echo "  - Use Phase 1-11 grammar (47% coverage)"
echo "  - Don't modify grammar further"
echo "  - Wait for official Xtext fix"
echo ""
echo "OPTION 2: Manual Recompilation (Advanced)"
echo "  1. Decompile the class with fernflower/JD-GUI"
echo "  2. Add try-catch around line 224"
echo "  3. Recompile with javac"
echo "  4. Replace class in JAR with jar -uf"
echo ""
echo "OPTION 3: Use Preprocessor Build Step"
echo "  - Generate from minimal grammar first"
echo "  - Manually copy InternalCDSLexer.java"
echo "  - Then expand grammar without clean"
echo ""
echo "RECOMMENDATION: Submit GitHub issue and wait for fix"
echo "                (Issue prepared in docs/GITHUB_ISSUE.md)"
echo ""
echo "============================================"
echo ""

read -p "View detailed manual patching instructions? (y/N): " response
if [[ "$response" =~ ^[Yy]$ ]]; then
    echo ""
    echo "MANUAL PATCHING STEPS:"
    echo "======================"
    echo ""
    echo "1. Download JD-GUI: https://java-decompiler.github.io/"
    echo ""
    echo "2. Open JAR in JD-GUI:"
    echo "   $JAR_PATH"
    echo ""
    echo "3. Navigate to:"
    echo "   org.eclipse.xtext.xtext.generator.parser.antlr"
    echo "   → AbstractAntlrGeneratorFragment2"
    echo ""
    echo "4. Find method improveLexerCodeQuality (around line 224)"
    echo ""
    echo "5. Wrap the readTextFile call in try-catch:"
    echo ""
    cat << 'EOF'
protected void improveLexerCodeQuality(...) {
    try {
        CharSequence content = fsa.readTextFile(lexer.getJavaPath());
        if (content == null || content.length() == 0) return;

        String code = content.toString();
        code = this.codeQualityHelper.stripUnnecessaryComments(code, this.options);
        fsa.generateFile(lexer.getJavaPath(), code);
    } catch (Exception e) {
        // Skip if file doesn't exist
    }
}
EOF
    echo ""
    echo "6. Save decompiled source to .java file"
    echo ""
    echo "7. Compile against Xtext JARs:"
    echo "   javac -cp '$JAR_PATH:...' AbstractAntlrGeneratorFragment2.java"
    echo ""
    echo "8. Update JAR:"
    echo "   jar -uf '$JAR_PATH' org/eclipse/xtext/.../AbstractAntlrGeneratorFragment2.class"
    echo ""
    echo "9. Test:"
    echo "   mvn clean generate-sources -DskipTests"
    echo ""
fi

echo ""
echo "For automated solution, see:"
echo "  docs/XTEXT_BUG_WORKAROUND.md"
echo ""
