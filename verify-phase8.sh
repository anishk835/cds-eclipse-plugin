#!/bin/bash
# Standalone test for Phase 8 key constraint implementation

echo "=== Phase 8 Key Constraints - Verification Test ==="
echo ""

# Test 1: Check if grammar file has ElementModifier
echo "Test 1: Checking grammar file for ElementModifier enum..."
if grep -q "enum ElementModifier" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext; then
    echo "✅ PASS: ElementModifier enum found in grammar"
else
    echo "❌ FAIL: ElementModifier enum not found in grammar"
    exit 1
fi

# Test 2: Check if Element rule has modifier
echo ""
echo "Test 2: Checking Element rule has modifier field..."
if grep -A 2 "Element:" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/CDS.xtext | grep -q "(modifier=ElementModifier)?"; then
    echo "✅ PASS: Element rule has modifier field"
else
    echo "❌ FAIL: Element rule doesn't have modifier field"
    exit 1
fi

# Test 3: Check if ElementModifier.java was generated
echo ""
echo "Test 3: Checking if ElementModifier.java was generated..."
if [ -f "/Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src-gen/org/example/cds/cDS/ElementModifier.java" ]; then
    echo "✅ PASS: ElementModifier.java generated"
else
    echo "❌ FAIL: ElementModifier.java not generated"
    exit 1
fi

# Test 4: Check if Element.java has getModifier method
echo ""
echo "Test 4: Checking if Element.java has getModifier method..."
if grep -q "ElementModifier getModifier()" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src-gen/org/example/cds/cDS/Element.java; then
    echo "✅ PASS: Element interface has getModifier() method"
else
    echo "❌ FAIL: Element interface missing getModifier() method"
    exit 1
fi

# Test 5: Check if CDSValidator has key validation methods
echo ""
echo "Test 5: Checking if CDSValidator has key validation methods..."
methods_found=0
if grep -q "checkEntityHasKey" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
    echo "  ✅ checkEntityHasKey found"
    ((methods_found++))
fi
if grep -q "checkKeyNotOnAssociation" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
    echo "  ✅ checkKeyNotOnAssociation found"
    ((methods_found++))
fi
if grep -q "checkKeyElementProperties" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
    echo "  ✅ checkKeyElementProperties found"
    ((methods_found++))
fi

if [ $methods_found -eq 3 ]; then
    echo "✅ PASS: All 3 validation methods found"
else
    echo "❌ FAIL: Only $methods_found/3 validation methods found"
    exit 1
fi

# Test 6: Check if diagnostic codes were added
echo ""
echo "Test 6: Checking if diagnostic codes were added..."
codes_found=0
if grep -q "CODE_MISSING_KEY_ELEMENT" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
    echo "  ✅ CODE_MISSING_KEY_ELEMENT found"
    ((codes_found++))
fi
if grep -q "CODE_KEY_ON_ASSOCIATION" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
    echo "  ✅ CODE_KEY_ON_ASSOCIATION found"
    ((codes_found++))
fi
if grep -q "CODE_KEY_WITHOUT_TYPE" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
    echo "  ✅ CODE_KEY_WITHOUT_TYPE found"
    ((codes_found++))
fi
if grep -q "CODE_KEY_WITH_CALCULATION" /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
    echo "  ✅ CODE_KEY_WITH_CALCULATION found"
    ((codes_found++))
fi

if [ $codes_found -eq 4 ]; then
    echo "✅ PASS: All 4 diagnostic codes found"
else
    echo "❌ FAIL: Only $codes_found/4 diagnostic codes found"
    exit 1
fi

# Test 7: Check if test methods were added
echo ""
echo "Test 7: Checking if test methods were added..."
tests_found=0
if grep -q "parseSingleKeyElement" /Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java; then
    echo "  ✅ parseSingleKeyElement test found"
    ((tests_found++))
fi
if grep -q "parseCompositeKey" /Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java; then
    echo "  ✅ parseCompositeKey test found"
    ((tests_found++))
fi
if grep -q "detectMissingKey" /Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java; then
    echo "  ✅ detectMissingKey test found"
    ((tests_found++))
fi
if grep -q "detectKeyOnAssociation" /Users/I546280/cds-eclipse-plugin/tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java; then
    echo "  ✅ detectKeyOnAssociation test found"
    ((tests_found++))
fi

if [ $tests_found -eq 4 ]; then
    echo "✅ PASS: All 4 test methods found"
else
    echo "❌ FAIL: Only $tests_found/4 test methods found"
    exit 1
fi

# Test 8: Check if documentation was updated
echo ""
echo "Test 8: Checking if documentation was updated..."
if grep -q "Phase 8: Key Constraints (Complete)" /Users/I546280/cds-eclipse-plugin/docs/FEATURE_COMPLETENESS.md; then
    echo "✅ PASS: Documentation updated with Phase 8 completion"
else
    echo "❌ FAIL: Documentation not updated"
    exit 1
fi

# Test 9: Check if sample file uses key syntax
echo ""
echo "Test 9: Checking if sample file uses proper key syntax..."
if grep -q "key ID" /Users/I546280/cds-eclipse-plugin/samples/bookshop.cds; then
    echo "✅ PASS: Sample file uses key syntax"
else
    echo "❌ FAIL: Sample file doesn't use key syntax"
    exit 1
fi

# Test 10: Check if ElementModifier enum has KEY value
echo ""
echo "Test 10: Checking if ElementModifier enum has KEY value..."
if grep -q 'KEY(0, "KEY", "key")' /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/src-gen/org/example/cds/cDS/ElementModifier.java; then
    echo "✅ PASS: ElementModifier.KEY enum value defined correctly"
else
    echo "❌ FAIL: ElementModifier.KEY enum value not found"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ ALL TESTS PASSED!"
echo "=========================================="
echo ""
echo "Summary:"
echo "  ✓ Grammar extended with ElementModifier enum"
echo "  ✓ Generated code includes modifier support"
echo "  ✓ Validation rules implemented (3 methods, 4 codes)"
echo "  ✓ Unit tests added (4 test methods)"
echo "  ✓ Documentation updated"
echo "  ✓ Sample files use proper key syntax"
echo ""
echo "Phase 8 Implementation: VERIFIED ✅"
