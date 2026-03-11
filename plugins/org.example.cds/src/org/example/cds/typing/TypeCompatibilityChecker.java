package org.example.cds.typing;

import org.example.cds.cDS.CDSFactory;
import org.example.cds.cDS.TypeDef;

/**
 * Determines if two types are compatible for operations.
 * Uses type families (numeric, string, temporal) for compatibility.
 */
public class TypeCompatibilityChecker {

    /**
     * Checks if two types are compatible for operations.
     * Returns true if types can be used together (e.g., in comparisons).
     */
    public boolean areCompatible(TypeInfo t1, TypeInfo t2) {
        if (t1 == null || t2 == null) return false;

        String name1 = t1.getTypeName();
        String name2 = t2.getTypeName();

        // Exact match
        if (name1.equals(name2)) return true;

        // Numeric compatibility (Integer, Integer64, Decimal, Double)
        if (isNumericFamily(name1) && isNumericFamily(name2)) return true;

        // String compatibility (String, LargeString)
        if (isStringFamily(name1) && isStringFamily(name2)) return true;

        // Temporal compatibility (Date, Time, DateTime, Timestamp)
        if (isTemporalFamily(name1) && isTemporalFamily(name2)) return true;

        return false;
    }

    private boolean isNumericFamily(String typeName) {
        return "Integer".equals(typeName) || "Integer64".equals(typeName) ||
               "Decimal".equals(typeName) || "Double".equals(typeName);
    }

    private boolean isStringFamily(String typeName) {
        return "String".equals(typeName) || "LargeString".equals(typeName);
    }

    private boolean isTemporalFamily(String typeName) {
        return "Date".equals(typeName) || "Time".equals(typeName) ||
               "DateTime".equals(typeName) || "Timestamp".equals(typeName);
    }

    /**
     * Finds the common type between two types (for CASE expressions, etc.).
     * Phase 22B: Returns the promoted type that can hold both types.
     */
    public TypeInfo findCommonType(TypeInfo t1, TypeInfo t2) {
        if (t1 == null) return t2;
        if (t2 == null) return t1;

        String name1 = t1.getTypeName();
        String name2 = t2.getTypeName();

        // Exact match
        if (name1.equals(name2)) return t1;

        // Numeric type promotion
        if (isNumericFamily(name1) && isNumericFamily(name2)) {
            return promoteNumericType(name1, name2);
        }

        // String type promotion (LargeString can hold String)
        if (isStringFamily(name1) && isStringFamily(name2)) {
            if ("LargeString".equals(name1) || "LargeString".equals(name2)) {
                return createTypeInfo("LargeString");
            }
            return createTypeInfo("String");
        }

        // Temporal type promotion (DateTime can hold Date and Time)
        if (isTemporalFamily(name1) && isTemporalFamily(name2)) {
            if ("DateTime".equals(name1) || "DateTime".equals(name2) ||
                "Timestamp".equals(name1) || "Timestamp".equals(name2)) {
                return createTypeInfo("DateTime");
            }
            return t1;  // Keep first type if both are Date or Time
        }

        // No common type found
        return null;
    }

    /**
     * Promotes numeric types to the most general type.
     * Order: Integer < Integer64 < Decimal < Double
     */
    private TypeInfo promoteNumericType(String type1, String type2) {
        // Double is the most general
        if ("Double".equals(type1) || "Double".equals(type2)) {
            return createTypeInfo("Double");
        }
        // Decimal is second most general
        if ("Decimal".equals(type1) || "Decimal".equals(type2)) {
            return createTypeInfo("Decimal");
        }
        // Integer64 can hold Integer
        if ("Integer64".equals(type1) || "Integer64".equals(type2)) {
            return createTypeInfo("Integer64");
        }
        // Both are Integer
        return createTypeInfo("Integer");
    }

    /**
     * Helper to create TypeInfo for a built-in type name.
     */
    private TypeInfo createTypeInfo(String typeName) {
        TypeDef typeDef = CDSFactory.eINSTANCE.createTypeDef();
        typeDef.setName(typeName);
        return new TypeInfo(typeDef, false);
    }
}
