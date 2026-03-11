package org.example.cds.typing;

import org.example.cds.cDS.Definition;

/**
 * Represents inferred type information for expressions.
 * Used by the type checker to validate expression type compatibility.
 */
public class TypeInfo {
    private final Definition typeDefinition;  // Entity, TypeDef, EnumDef
    private final boolean isArray;

    public TypeInfo(Definition typeDef, boolean isArray) {
        this.typeDefinition = typeDef;
        this.isArray = isArray;
    }

    public Definition getTypeDefinition() {
        return typeDefinition;
    }

    public boolean isArray() {
        return isArray;
    }

    public String getTypeName() {
        if (typeDefinition == null) return "Unknown";

        // Handle concrete Definition subtypes
        if (typeDefinition instanceof org.example.cds.cDS.EntityDef entity) return entity.getName();
        if (typeDefinition instanceof org.example.cds.cDS.TypeDef type) return type.getName();
        if (typeDefinition instanceof org.example.cds.cDS.EnumDef enumDef) return enumDef.getName();
        if (typeDefinition instanceof org.example.cds.cDS.ViewDef view) return view.getName();
        if (typeDefinition instanceof org.example.cds.cDS.ServiceDef service) return service.getName();
        if (typeDefinition instanceof org.example.cds.cDS.AspectDef aspect) return aspect.getName();

        return "Unknown";
    }

    // Type category helpers

    public boolean isNumeric() {
        String name = getTypeName();
        return "Integer".equals(name) || "Integer64".equals(name) ||
               "UInt8".equals(name) || "Int16".equals(name) ||
               "Decimal".equals(name) || "Double".equals(name);
    }

    public boolean isString() {
        String name = getTypeName();
        return "String".equals(name) || "LargeString".equals(name);
    }

    public boolean isBoolean() {
        return "Boolean".equals(getTypeName());
    }

    public boolean isTemporal() {
        String name = getTypeName();
        return "Date".equals(name) || "Time".equals(name) ||
               "DateTime".equals(name) || "Timestamp".equals(name);
    }

    @Override
    public String toString() {
        return isArray ? "array of " + getTypeName() : getTypeName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TypeInfo)) return false;
        TypeInfo other = (TypeInfo) obj;
        return isArray == other.isArray &&
               getTypeName().equals(other.getTypeName());
    }

    @Override
    public int hashCode() {
        return getTypeName().hashCode() * 31 + (isArray ? 1 : 0);
    }
}
