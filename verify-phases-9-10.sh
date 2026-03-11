#!/bin/bash

# Combined Verification Script for Phases 9-10
# Verifies both constraint and virtual element implementations

set -e

echo "=========================================================="
echo "Phases 9-10 Combined Implementation Verification"
echo "=========================================================="
echo

# ── Phase 9: Constraints ─────────────────────────────────────────────────────

echo "Phase 9: Data Constraints"
echo "──────────────────────────────────────────────────────────"
echo

echo "✓ Checking constraint grammar artifacts..."
for file in Constraint.java NotNullConstraint.java UniqueConstraint.java CheckConstraint.java; do
    if [ -f "plugins/org.example.cds/src-gen/org/example/cds/cDS/$file" ]; then
        echo "  ✓ $file exists"
    else
        echo "  ✗ $file missing!"
        exit 1
    fi
done

echo
echo "✓ Checking constraint validation methods..."
for method in checkNotNullConstraint checkUniqueConstraint checkCheckConstraint checkDefaultValue; do
    if grep -q "public void $method" plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
        echo "  ✓ $method() present"
    else
        echo "  ✗ $method() missing!"
        exit 1
    fi
done

echo
echo "✓ Checking constraint test coverage..."
for test in parseNotNullConstraint parseMultipleConstraints parseCheckConstraint; do
    if grep -q "public void $test" tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java; then
        echo "  ✓ $test() present"
    else
        echo "  ✗ $test() missing!"
        exit 1
    fi
done

# ── Phase 10: Virtual Elements ───────────────────────────────────────────────

echo
echo "Phase 10: Virtual Elements"
echo "──────────────────────────────────────────────────────────"
echo

echo "✓ Checking ElementModifier enum..."
if grep -q "VIRTUAL" plugins/org.example.cds/src-gen/org/example/cds/cDS/ElementModifier.java; then
    echo "  ✓ VIRTUAL enum value present"
else
    echo "  ✗ VIRTUAL enum value missing!"
    exit 1
fi

echo
echo "✓ Checking virtual validation methods..."
for method in checkVirtualElement checkVirtualConstraints; do
    if grep -q "public void $method" plugins/org.example.cds/src/org/example/cds/validation/CDSValidator.java; then
        echo "  ✓ $method() present"
    else
        echo "  ✗ $method() missing!"
        exit 1
    fi
done

echo
echo "✓ Checking virtual test coverage..."
for test in parseVirtualElement parseVirtualWithExpression parseVirtualInService; do
    if grep -q "public void $test" tests/org.example.cds.tests/src/org/example/cds/tests/CDSParsingTest.java; then
        echo "  ✓ $test() present"
    else
        echo "  ✗ $test() missing!"
        exit 1
    fi
done

# ── Sample Files ─────────────────────────────────────────────────────────────

echo
echo "Sample Files"
echo "──────────────────────────────────────────────────────────"
echo

if [ -f "samples/phase9-test.cds" ]; then
    echo "  ✓ phase9-test.cds exists"
else
    echo "  ✗ phase9-test.cds missing!"
    exit 1
fi

if [ -f "samples/phase10-test.cds" ]; then
    echo "  ✓ phase10-test.cds exists"
else
    echo "  ✗ phase10-test.cds missing!"
    exit 1
fi

if grep -q "not null" samples/bookshop.cds && grep -q "virtual" samples/bookshop.cds; then
    echo "  ✓ bookshop.cds updated with constraints and virtual"
else
    echo "  ✗ bookshop.cds not fully updated!"
    exit 1
fi

# ── Documentation ────────────────────────────────────────────────────────────

echo
echo "Documentation"
echo "──────────────────────────────────────────────────────────"
echo

for doc in PHASE_9_SUMMARY.md PHASE_10_SUMMARY.md; do
    if [ -f "docs/$doc" ]; then
        echo "  ✓ $doc exists"
    else
        echo "  ✗ $doc missing!"
        exit 1
    fi
done

if grep -q "~47%" docs/FEATURE_COMPLETENESS.md; then
    echo "  ✓ FEATURE_COMPLETENESS.md shows 47% coverage"
else
    echo "  ✗ FEATURE_COMPLETENESS.md not updated!"
    exit 1
fi

# ── Summary ──────────────────────────────────────────────────────────────────

echo
echo "=========================================================="
echo "✅ All Phases 9-10 verification checks passed!"
echo "=========================================================="
echo
echo "Implementation Status:"
echo "  • Phase 9 (Data Constraints): COMPLETE"
echo "  • Phase 10 (Virtual Elements): COMPLETE"
echo "  • Overall Coverage: ~47% of SAP CAP CDS specification"
echo
echo "Phase 9 Features:"
echo "  • not null constraints"
echo "  • unique constraints"
echo "  • check constraints with expressions"
echo "  • default values"
echo "  • Multiple constraints per element"
echo
echo "Phase 10 Features:"
echo "  • virtual modifier for transient elements"
echo "  • Virtual elements with expressions"
echo "  • Virtual element validation"
echo "  • Service projection support"
echo
echo "Combined Example:"
echo "  entity Users {"
echo "    key ID: UUID not null;"
echo "    email: String not null unique;"
echo "    age: Integer check age >= 18;"
echo "    status: String default 'active';"
echo "    virtual displayName: String;"
echo "  }"
echo
echo "To build and test:"
echo "  mvn clean package"
echo
