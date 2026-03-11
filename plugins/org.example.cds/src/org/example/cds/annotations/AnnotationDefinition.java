package org.example.cds.annotations;

import java.util.Set;

/**
 * Defines an annotation with its expected value type and allowed targets.
 * Used by the annotation validator to check annotation correctness.
 */
public class AnnotationDefinition {

    /**
     * Expected value type for an annotation.
     */
    public enum ValueType {
        BOOLEAN,    // true/false
        STRING,     // "string literal"
        INTEGER,    // 123
        DECIMAL,    // 123.45
        ARRAY,      // [item1, item2, ...]
        OBJECT,     // { key: value, ... }
        ANY         // Any value type accepted
    }

    /**
     * Target types where annotations can be applied.
     */
    public enum TargetType {
        ENTITY,         // entity definitions
        ELEMENT,        // entity elements/fields
        SERVICE,        // service definitions
        TYPE,           // type definitions
        ENUM,           // enum definitions
        ASSOCIATION,    // associations
        ANY             // can be applied anywhere
    }

    private final String name;
    private final ValueType valueType;
    private final Set<TargetType> allowedTargets;
    private final String description;
    private final boolean deprecated;

    public AnnotationDefinition(String name, ValueType valueType,
                                Set<TargetType> allowedTargets, String description) {
        this(name, valueType, allowedTargets, description, false);
    }

    public AnnotationDefinition(String name, ValueType valueType,
                                Set<TargetType> allowedTargets,
                                String description, boolean deprecated) {
        this.name = name;
        this.valueType = valueType;
        this.allowedTargets = allowedTargets;
        this.description = description;
        this.deprecated = deprecated;
    }

    public String getName() {
        return name;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Set<TargetType> getAllowedTargets() {
        return allowedTargets;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * Checks if this annotation can be applied to the given target type.
     */
    public boolean canApplyTo(TargetType target) {
        return allowedTargets.contains(TargetType.ANY) ||
               allowedTargets.contains(target);
    }

    @Override
    public String toString() {
        return "@" + name + " (" + valueType + ")";
    }
}
