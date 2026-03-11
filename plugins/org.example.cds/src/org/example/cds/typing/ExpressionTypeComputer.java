package org.example.cds.typing;

import java.util.ArrayList;
import java.util.List;
import org.example.cds.cDS.*;
import org.example.cds.projections.BuiltInFunctionRegistry;
import org.example.cds.projections.FunctionDefinition;
import org.example.cds.projections.FunctionDefinition.ArgType;

/**
 * Core type inference engine for expressions.
 * Computes the result type of expressions for type checking validation.
 */
public class ExpressionTypeComputer {

    private final OperatorRegistry operatorRegistry = new OperatorRegistry();
    private final TypeCompatibilityChecker compatibilityChecker = new TypeCompatibilityChecker();
    private final BuiltInFunctionRegistry functionRegistry = new BuiltInFunctionRegistry();

    /**
     * Infers the type of an expression.
     * Returns null if type cannot be determined.
     */
    public TypeInfo inferType(Expression expr) {
        if (expr == null) return null;

        // BinaryExpr: arithmetic, logical, comparison
        if (expr instanceof BinaryExpr) {
            return inferBinaryExprType((BinaryExpr) expr);
        }

        // UnaryExpr: -, not
        if (expr instanceof UnaryExpr) {
            return inferUnaryExprType((UnaryExpr) expr);
        }

        // LiteralExpr: literals → known types
        if (expr instanceof LiteralExpr) {
            return inferLiteralType((LiteralExpr) expr);
        }

        // RefExpr: reference to element
        if (expr instanceof RefExpr) {
            return inferRefExprType((RefExpr) expr);
        }

        // AggregationExpr: COUNT, SUM, AVG, MIN, MAX
        if (expr instanceof AggregationExpr) {
            return inferAggregationType((AggregationExpr) expr);
        }

        // FuncExpr: built-in function calls
        if (expr instanceof FuncExpr) {
            return inferFuncExprType((FuncExpr) expr);
        }

        // CaseExpr: CASE expressions (Phase 22B)
        if (expr instanceof CaseExpr) {
            return inferCaseExprType((CaseExpr) expr);
        }

        // CastExpr: CAST expressions (Phase 22B)
        if (expr instanceof CastExpr) {
            return inferCastExprType((CastExpr) expr);
        }

        // CoalesceExpr: COALESCE expressions (Phase 23)
        if (expr instanceof CoalesceExpr) {
            return inferCoalesceExprType((CoalesceExpr) expr);
        }

        // ExistsExpr: EXISTS predicates (Phase 23)
        if (expr instanceof ExistsExpr) {
            return createBooleanType();  // EXISTS always returns boolean
        }

        // SubqueryExpr: Subquery expressions (Phase 23)
        if (expr instanceof SubqueryExpr) {
            return inferSubqueryExprType((SubqueryExpr) expr);
        }

        // Other expression types (InExpr, BetweenExpr, IsNullExpr)
        // These are boolean conditions
        if (expr instanceof InExpr || expr instanceof BetweenExpr ||
            expr instanceof IsNullExpr) {
            return createBooleanType();
        }

        return null;  // Unknown expression type
    }

    private TypeInfo inferBinaryExprType(BinaryExpr expr) {
        TypeInfo leftType = inferType(expr.getLeft());
        TypeInfo rightType = inferType(expr.getRight());

        if (leftType == null || rightType == null) return null;

        String op = expr.getOp();

        // Comparison operators → Boolean
        if (operatorRegistry.isComparisonOperator(op)) {
            return createBooleanType();
        }

        // Logical operators → Boolean (already validated)
        if (operatorRegistry.isLogicalOperator(op)) {
            return createBooleanType();
        }

        // Numeric operators → promoted type
        if (operatorRegistry.isNumericOperator(op)) {
            return operatorRegistry.getResultType(op, leftType, rightType);
        }

        return null;
    }

    private TypeInfo inferUnaryExprType(UnaryExpr expr) {
        TypeInfo operandType = inferType(expr.getOperand());
        if (operandType == null) return null;

        String op = expr.getOp();

        // 'not' → Boolean
        if ("not".equals(op)) {
            return createBooleanType();
        }

        // '-' (negation) → same as operand (must be numeric)
        if ("-".equals(op)) {
            return operandType;
        }

        return null;
    }

    private TypeInfo inferLiteralType(LiteralExpr expr) {
        Literal lit = expr.getValue();
        if (lit == null) return null;

        if (lit instanceof IntLiteral) {
            return createIntegerType();
        }
        if (lit instanceof DecimalLiteral) {
            return createDecimalType();
        }
        if (lit instanceof StringLiteral) {
            return createStringType();
        }
        if (lit instanceof BoolLiteral) {
            return createBooleanType();
        }
        if (lit instanceof NullLiteral) {
            return null;  // Null has no specific type
        }

        return null;
    }

    private TypeInfo inferRefExprType(RefExpr expr) {
        Element element = expr.getRef();
        if (element == null || element.eIsProxy()) return null;

        TypeRef typeRef = element.getType();
        if (typeRef == null) return null;

        return createTypeInfo(typeRef);
    }

    private TypeInfo inferAggregationType(AggregationExpr expr) {
        AggregationFunc func = expr.getFunc();

        switch (func) {
            case COUNT:
                return createIntegerType();
            case AVG:
                return createDecimalType();
            case SUM:
                // SUM returns same type as argument (must be numeric)
                TypeInfo argType = inferType(expr.getArg());
                return argType != null && argType.isNumeric() ? argType : null;
            case MIN:
            case MAX:
                // MIN/MAX return same type as argument
                return inferType(expr.getArg());
            default:
                return null;
        }
    }

    /**
     * Infers the type of a function call expression.
     * Phase 22A: Built-in function support.
     */
    private TypeInfo inferFuncExprType(FuncExpr expr) {
        String funcName = expr.getFunc();
        if (funcName == null) return null;

        // Get function definition
        FunctionDefinition funcDef = functionRegistry.getFunction(funcName);
        if (funcDef == null) {
            // Unknown function - let validator handle error
            return null;
        }

        // Infer argument types
        List<TypeInfo> argTypes = new ArrayList<>();
        for (Expression arg : expr.getArgs()) {
            TypeInfo argType = inferType(arg);
            if (argType == null) return null;  // Can't determine type
            argTypes.add(argType);
        }

        // Get return type based on function definition
        return getFunctionReturnType(funcDef, argTypes);
    }

    /**
     * Determines the return type of a function based on its definition and arguments.
     */
    private TypeInfo getFunctionReturnType(FunctionDefinition funcDef, List<TypeInfo> argTypes) {
        ArgType returnType = funcDef.getReturnType();

        // If function returns input type (e.g., UPPER, ABS)
        if (funcDef.returnsInputType() && !argTypes.isEmpty()) {
            return argTypes.get(0);
        }

        // Map ArgType to TypeInfo
        switch (returnType) {
            case STRING:
                return createStringType();
            case NUMERIC:
                return createDecimalType();  // Default numeric return
            case TEMPORAL:
                return createType("DateTime");  // Default temporal return
            case BOOLEAN:
                return createBooleanType();
            case ANY:
                return argTypes.isEmpty() ? null : argTypes.get(0);
            default:
                return null;
        }
    }

    /**
     * Helper to create TypeInfo for a built-in type name.
     */
    private TypeInfo createType(String typeName) {
        Definition typeDef = findBuiltInType(typeName);
        return new TypeInfo(typeDef, false);
    }

    /**
     * Infers the type of a CASE expression.
     * Phase 22B: Returns the common type of all THEN and ELSE branches.
     */
    private TypeInfo inferCaseExprType(CaseExpr expr) {
        if (expr.getWhenClauses() == null || expr.getWhenClauses().isEmpty()) {
            return null;
        }

        // Get type of first THEN expression as base type
        TypeInfo baseType = inferType(expr.getWhenClauses().get(0).getResult());
        if (baseType == null) return null;

        // Check all other THEN expressions are compatible
        for (WhenClause whenClause : expr.getWhenClauses()) {
            TypeInfo resultType = inferType(whenClause.getResult());
            if (resultType == null) continue;

            // Find common type (use type promotion)
            baseType = compatibilityChecker.findCommonType(baseType, resultType);
            if (baseType == null) return null;
        }

        // Check ELSE expression if present
        if (expr.getElseExpr() != null) {
            TypeInfo elseType = inferType(expr.getElseExpr());
            if (elseType != null) {
                baseType = compatibilityChecker.findCommonType(baseType, elseType);
            }
        }

        return baseType;
    }

    /**
     * Infers the type of a CAST expression.
     * Phase 22B: Returns the target type specified in the CAST.
     */
    private TypeInfo inferCastExprType(CastExpr expr) {
        TypeRef targetType = expr.getTargetType();
        if (targetType == null) return null;

        return createTypeInfo(targetType);
    }

    /**
     * Infers the type of a COALESCE expression.
     * Phase 23: Returns the common type of all arguments.
     */
    private TypeInfo inferCoalesceExprType(CoalesceExpr expr) {
        if (expr.getExpressions() == null || expr.getExpressions().isEmpty()) {
            return null;
        }

        // Get type of first expression as base type
        TypeInfo baseType = inferType(expr.getExpressions().get(0));
        if (baseType == null) return null;

        // Find common type across all expressions
        for (int i = 1; i < expr.getExpressions().size(); i++) {
            TypeInfo exprType = inferType(expr.getExpressions().get(i));
            if (exprType == null) continue;

            baseType = compatibilityChecker.findCommonType(baseType, exprType);
            if (baseType == null) return null;
        }

        return baseType;
    }

    /**
     * Infers the type of a subquery expression.
     * Phase 23: Returns the type of the first column in the SELECT.
     */
    private TypeInfo inferSubqueryExprType(SubqueryExpr expr) {
        SelectQuery subquery = expr.getSubquery();
        if (subquery == null || subquery.getColumns() == null ||
            subquery.getColumns().isEmpty()) {
            return null;
        }

        // Get the type of the first column's expression
        SelectColumn firstColumn = subquery.getColumns().get(0);
        if (firstColumn.getExpression() != null) {
            return inferType(firstColumn.getExpression());
        }

        // If no expression, try to resolve the alias reference
        // (This would need scope resolution, simplified for now)
        return null;
    }

    // Helper: Create TypeInfo from TypeRef
    private TypeInfo createTypeInfo(TypeRef typeRef) {
        if (typeRef instanceof SimpleTypeRef) {
            SimpleTypeRef simpleRef = (SimpleTypeRef) typeRef;
            Definition typeDef = simpleRef.getRef();
            if (typeDef == null || typeDef.eIsProxy()) return null;
            return new TypeInfo(typeDef, false);
        }

        if (typeRef instanceof ArrayTypeRef) {
            ArrayTypeRef arrayRef = (ArrayTypeRef) typeRef;
            SimpleTypeRef elementType = arrayRef.getElementType();
            if (elementType == null) return null;
            Definition typeDef = elementType.getRef();
            if (typeDef == null || typeDef.eIsProxy()) return null;
            return new TypeInfo(typeDef, true);
        }

        // StructuredTypeRef: not handled yet
        return null;
    }

    // Helper: Create built-in types
    private TypeInfo createIntegerType() {
        return new TypeInfo(findBuiltInType("Integer"), false);
    }

    private TypeInfo createDecimalType() {
        return new TypeInfo(findBuiltInType("Decimal"), false);
    }

    private TypeInfo createStringType() {
        return new TypeInfo(findBuiltInType("String"), false);
    }

    private TypeInfo createBooleanType() {
        return new TypeInfo(findBuiltInType("Boolean"), false);
    }

    private Definition findBuiltInType(String name) {
        // Use CDSBuiltInTypeProvider to get synthetic TypeDef
        // For now, create synthetic TypeDef (optimization: cache these)
        TypeDef typeDef = CDSFactory.eINSTANCE.createTypeDef();
        typeDef.setName(name);
        return typeDef;
    }

    public OperatorRegistry getOperatorRegistry() {
        return operatorRegistry;
    }

    public TypeCompatibilityChecker getCompatibilityChecker() {
        return compatibilityChecker;
    }
}
