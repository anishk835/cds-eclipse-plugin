#!/bin/bash
# Verification script for CDS Eclipse Plugin

echo "=== CDS Plugin Build Verification ==="
echo ""

# Check if build artifacts exist
echo "1. Checking build artifacts..."
if [ -d "releng/org.example.cds.p2/target/repository" ]; then
    echo "   ✓ P2 repository exists"
else
    echo "   ✗ P2 repository not found - run ./build.sh first"
    exit 1
fi

# Check Java class version
echo ""
echo "2. Checking Java class file versions..."

CLASS_FILES=(
    "plugins/org.example.cds/target/classes/org/example/cds/scoping/CDSScopeProvider.class"
    "plugins/org.example.cds/target/classes/org/example/cds/scoping/CDSQualifiedNameProvider.class"
    "plugins/org.example.cds/target/classes/org/example/cds/projections/BuiltInFunctionRegistry.class"
)

ALL_CORRECT=true
for CLASS_FILE in "${CLASS_FILES[@]}"; do
    if [ -f "$CLASS_FILE" ]; then
        VERSION=$(javap -v "$CLASS_FILE" 2>/dev/null | grep "major version" | awk '{print $NF}')
        if [ "$VERSION" = "61" ]; then
            echo "   ✓ $(basename $CLASS_FILE): Java 17 (version $VERSION)"
        else
            echo "   ✗ $(basename $CLASS_FILE): Wrong version $VERSION (expected 61)"
            ALL_CORRECT=false
        fi
    else
        echo "   ✗ $(basename $CLASS_FILE): Not found"
        ALL_CORRECT=false
    fi
done

# Check MANIFEST.MF
echo ""
echo "3. Checking MANIFEST.MF configuration..."
if grep -q "Bundle-ActivationPolicy: lazy" "plugins/org.example.cds.ui/META-INF/MANIFEST.MF"; then
    echo "   ✓ Bundle-ActivationPolicy is configured"
else
    echo "   ✗ Bundle-ActivationPolicy is missing"
    ALL_CORRECT=false
fi

if grep -q "Bundle-Activator: org.example.cds.ui.internal.CdsActivator" "plugins/org.example.cds.ui/META-INF/MANIFEST.MF"; then
    echo "   ✓ Bundle-Activator is configured"
else
    echo "   ✗ Bundle-Activator is missing"
    ALL_CORRECT=false
fi

# Check source files
echo ""
echo "4. Checking source files..."
if [ -f "plugins/org.example.cds/src/org/example/cds/scoping/CDSQualifiedNameProvider.java" ]; then
    echo "   ✓ CDSQualifiedNameProvider.java exists"
else
    echo "   ✗ CDSQualifiedNameProvider.java not found"
    ALL_CORRECT=false
fi

# Summary
echo ""
echo "=== Summary ==="
if [ "$ALL_CORRECT" = true ]; then
    echo "✓ All checks passed!"
    echo ""
    echo "Next steps:"
    echo "1. Close Eclipse"
    echo "2. Install plugin: Help → Install New Software"
    echo "3. Location: file:$(pwd)/releng/org.example.cds.p2/target/repository"
    echo "4. Restart Eclipse"
    exit 0
else
    echo "✗ Some checks failed!"
    echo ""
    echo "Please run: ./build.sh clean install -DskipTests"
    exit 1
fi
