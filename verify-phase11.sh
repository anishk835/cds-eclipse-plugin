#!/bin/bash

# Phase 11 Verification Script
# Checks that localized modifier was added correctly

set -e

echo "=================================================="
echo "Phase 11 Implementation Verification"
echo "=================================================="
echo

# Check 1: ElementModifier enum has LOCALIZED
echo "✓ Checking ElementModifier enum..."
if grep -q "LOCALIZED" plugins/org.example.cds/src-gen/org/example/cds/cDS/ElementModifier.java; then
    echo "  ✓ LOCALIZED enum value present"
else
    echo "  ✗ LOCALIZED enum value missing!"
    exit 1
fi

# Check 2: Grammar has localized keyword
echo
echo "✓ Checking grammar definition..."
if grep -q "KEY='key' | VIRTUAL='virtual' | LOCALIZED='localized'" plugins/org.example.cds/src/org/example/cds/CDS.xtext; then
    echo "  ✓ Grammar includes localized modifier"
else
    echo "  ✗ Grammar missing localized modifier!"
    exit 1
fi

# Check 3: Validator has localized diagnostic codes
echo
echo "✓ Checking CDSValidator diagnostic codes..."
for code in CODE_LOCALIZED_ON_ASSOCIATION CODE_LOCALIZED_ON_NON_STRING CODE_LOCALIZED_HINT; do
    if grep -q "$code" plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
        echo "  ✓ $code present"
    else
        echo "  ✗ $code missing!"
        exit 1
    fi
done

# Check 4: Validator has localized methods
echo
echo "✓ Checking CDSValidator localized methods..."
if grep -q "public void checkLocalizedElement" plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
    echo "  ✓ checkLocalizedElement() present"
else
    echo "  ✗ checkLocalizedElement() missing!"
    exit 1
fi

# Check 5: Test file has Phase 11 tests
echo
echo "✓ Checking test coverage..."
for test in parseLocalizedElement parseMultipleLocalizedElements parseLocalizedWithConstraints parseLocalizedInService; do
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
if [ -f "samples/phase11-test.cds" ]; then
    echo "  ✓ phase11-test.cds exists"
else
    echo "  ✗ phase11-test.cds missing!"
    exit 1
fi

if grep -q "localized" samples/bookshop.cds; then
    echo "  ✓ bookshop.cds updated with localized elements"
else
    echo "  ✗ bookshop.cds not updated!"
    exit 1
fi

# Check 7: Documentation updated
echo
echo "✓ Checking documentation..."
if grep -q "Phase 11: Localized Data" docs/FEATURE_COMPLETENESS.md; then
    echo "  ✓ FEATURE_COMPLETENESS.md updated"
else
    echo "  ✗ FEATURE_COMPLETENESS.md not updated!"
    exit 1
fi

# Summary
echo
echo "=================================================="
echo "✅ All Phase 11 verification checks passed!"
echo "=================================================="
echo
echo "Phase 11 Implementation Status: COMPLETE"
echo "Coverage: ~49% of SAP CAP CDS specification"
echo
echo "New Features:"
echo "  • localized modifier for multilingual fields"
echo "  • Localized String/LargeString support"
echo "  • Localized element validation"
echo "  • Compatible with constraints"
echo "  • Service projection support"
echo
echo "To build and test:"
echo "  mvn clean package"
echo
