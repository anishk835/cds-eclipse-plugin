package org.example.cds.typing;

import java.util.Set;

/**
 * Defines operator type requirements and result types.
 * Used to validate that operators are applied to compatible types.
 */
public class OperatorRegistry {

    // Operators requiring numeric operands
    private static final Set<String> NUMERIC_OPERATORS = Set.of("+", "-", "*", "/");

    // Operators requiring boolean operands
    private static final Set<String> LOGICAL_OPERATORS = Set.of("and", "or");

    // Comparison operators (require compatible types)
    private static final Set<String> COMPARISON_OPERATORS =
        Set.of("=", "!=", "<>", "<", "<=", ">", ">=");

    public boolean isNumericOperator(String op) {
        return NUMERIC_OPERATORS.contains(op);
    }

    public boolean isLogicalOperator(String op) {
        return LOGICAL_OPERATORS.contains(op);
    }

    public boolean isComparisonOperator(String op) {
        return COMPARISON_OPERATORS.contains(op);
    }

    /**
     * Returns the result type for binary operator application.
     * For numeric operators, performs type promotion.
     */
    public TypeInfo getResultType(String op, TypeInfo left, TypeInfo right) {
        if (isNumericOperator(op)) {
            // Numeric promotion: Integer → Integer64 → Decimal → Double
            return promoteNumericType(left, right);
        }
        if (isLogicalOperator(op)) {
            // Boolean operators return Boolean
            return left;  // Already validated as Boolean
        }
        if (isComparisonOperator(op)) {
            // Comparison operators return Boolean
            // Return null - handled by caller to create Boolean type
            return null;
        }
        return null;  // Unknown operator
    }

    /**
     * Performs numeric type promotion according to hierarchy:
     * Integer < Integer64 < Decimal < Double
     */
    private TypeInfo promoteNumericType(TypeInfo left, TypeInfo right) {
        String leftName = left.getTypeName();
        String rightName = right.getTypeName();

        // Promotion hierarchy: Integer < Integer64 < Decimal < Double
        if ("Double".equals(leftName) || "Double".equals(rightName)) {
            return "Double".equals(leftName) ? left : right;
        }
        if ("Decimal".equals(leftName) || "Decimal".equals(rightName)) {
            return "Decimal".equals(leftName) ? left : right;
        }
        if ("Integer64".equals(leftName) || "Integer64".equals(rightName)) {
            return "Integer64".equals(leftName) ? left : right;
        }
        return left;  // Both Integer
    }
}
