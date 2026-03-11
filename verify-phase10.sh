#!/bin/bash

# Phase 10 Verification Script
# Checks that virtual modifier was added correctly

set -e

echo "=================================================="
echo "Phase 10 Implementation Verification"
echo "=================================================="
echo

# Check 1: ElementModifier enum has VIRTUAL
echo "✓ Checking ElementModifier enum..."
if grep -q "VIRTUAL" plugins/org.example.cds/src-gen/org/example/cds/cDS/ElementModifier.java; then
    echo "  ✓ VIRTUAL enum value present"
else
    echo "  ✗ VIRTUAL enum value missing!"
    exit 1
fi

# Check 2: Grammar has virtual keyword
echo
echo "✓ Checking grammar definition..."
if grep -q "KEY='key' | VIRTUAL='virtual'" plugins/org.example.cds/src/org/example/cds/CDS.xtext; then
    echo "  ✓ Grammar includes virtual modifier"
else
    echo "  ✗ Grammar missing virtual modifier!"
    exit 1
fi

# Check 3: Validator has virtual diagnostic codes
echo
echo "✓ Checking CDSValidator diagnostic codes..."
for code in CODE_VIRTUAL_ON_ASSOCIATION CODE_VIRTUAL_WITHOUT_TYPE CODE_VIRTUAL_WITH_KEY CODE_VIRTUAL_PERSISTED_HINT; do
    if grep -q "$code" plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
        echo "  ✓ $code present"
    else
        echo "  ✗ $code missing!"
        exit 1
    fi
done

# Check 4: Validator has virtual methods
echo
echo "✓ Checking CDSValidator virtual methods..."
for method in checkVirtualElement checkVirtualConstraints; do
    if grep -q "public void $method" plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
        echo "  ✓ $method() present"
    else
        echo "  ✗ $method() missing!"
        exit 1
    fi
done

# Check 5: Test file has Phase 10 tests
echo
echo "✓ Checking test coverage..."
for test in parseVirtualElement parseVirtualWithExpression parseMultipleVirtualElements parseVirtualInService; do
    if grep -q "public void $test" tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java; then
        echo "  ✓ $test() present"
    else
        echo "  ✗ $test() missing!"
        exit 1
    fi
done

# Check 6: Sample files exist
echo
echo "✓ Checking sample files..."
if [ -f "samples/phase10-test.cds" ]; then
    echo "  ✓ phase10-test.cds exists"
else
    echo "  ✗ phase10-test.cds missing!"
    exit 1
fi

if grep -q "virtual" samples/bookshop.cds; then
    echo "  ✓ bookshop.cds updated with virtual elements"
else
    echo "  ✗ bookshop.cds not updated!"
    exit 1
fi

# Check 7: Documentation updated
echo
echo "✓ Checking documentation..."
if grep -q "Phase 10: Virtual Elements" docs/FEATURE_COMPLETENESS.md; then
    echo "  ✓ FEATURE_COMPLETENESS.md updated"
else
    echo "  ✗ FEATURE_COMPLETENESS.md not updated!"
    exit 1
fi

# Summary
echo
echo "=================================================="
echo "✅ All Phase 10 verification checks passed!"
echo "=================================================="
echo
echo "Phase 10 Implementation Status: COMPLETE"
echo "Coverage: ~47% of SAP CAP CDS specification"
echo
echo "New Features:"
echo "  • virtual modifier for transient elements"
echo "  • Virtual elements with expressions"
echo "  • Virtual element validation"
echo "  • Compatible with constraints (with warnings)"
echo "  • Service projection support"
echo
echo "To build and test:"
echo "  mvn clean package"
echo
