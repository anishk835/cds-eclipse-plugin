package org.example.cds.annotations;

import org.eclipse.emf.ecore.EObject;
import org.example.cds.cDS.*;
import org.example.cds.annotations.AnnotationDefinition.TargetType;

/**
 * Helper methods for working with annotations in validation.
 */
public class AnnotationHelper {

    /**
     * Extracts annotation name from Annotation.
     * Handles both simple (@title) and path (@UI.LineItem) annotations.
     */
    public String getAnnotationName(Annotation annotation) {
        if (annotation == null || annotation.getName() == null) {
            return null;
        }
        return annotation.getName();
    }

    /**
     * Determines the target type where annotation is applied.
     * This is used to validate that annotations are applied to correct targets.
     */
    public TargetType getTargetType(EObject context) {
        if (context instanceof EntityDef || context instanceof ServiceEntity) {
            return TargetType.ENTITY;
        }
        if (context instanceof Element) {
            return TargetType.ELEMENT;
        }
        if (context instanceof ServiceDef) {
            return TargetType.SERVICE;
        }
        if (context instanceof TypeDef) {
            return TargetType.TYPE;
        }
        if (context instanceof EnumDef) {
            return TargetType.ENUM;
        }
        if (context instanceof AssocDef) {
            return TargetType.ASSOCIATION;
        }
        return TargetType.ANY;
    }

    /**
     * Checks if annotation value is a boolean literal (true/false).
     */
    public boolean isBooleanValue(AnnotationValue value) {
        return value instanceof BoolAnnotationValue;
    }

    /**
     * Checks if annotation value is a string literal.
     */
    public boolean isStringValue(AnnotationValue value) {
        return value instanceof StringAnnotationValue;
    }

    /**
     * Checks if annotation value is a number (integer or decimal).
     */
    public boolean isNumberValue(AnnotationValue value) {
        return value instanceof NumberAnnotationValue;
    }

    /**
     * Checks if annotation value is an array.
     * Arrays in CDS annotations: [item1, item2, ...]
     */
    public boolean isArrayValue(AnnotationValue value) {
        return value instanceof ArrayAnnotationValue;
    }

    /**
     * Checks if annotation value is an object/structure.
     * Objects in CDS annotations: { key: value, ... }
     */
    public boolean isObjectValue(AnnotationValue value) {
        return value instanceof RecordAnnotationValue;
    }

    /**
     * Gets human-readable value type name for error messages.
     */
    public String getValueTypeName(AnnotationValue value) {
        if (value == null) {
            return "null";
        }
        if (isBooleanValue(value)) {
            return "boolean";
        }
        if (isStringValue(value)) {
            return "string";
        }
        if (isNumberValue(value)) {
            return "number";
        }
        if (isArrayValue(value)) {
            return "array";
        }
        if (isObjectValue(value)) {
            return "object";
        }
        return "unknown";
    }

    /**
     * Checks if annotation has a value.
     * Some annotations don't require explicit values (e.g., @readonly is same as @readonly: true).
     */
    public boolean hasValue(Annotation annotation) {
        return annotation != null && annotation.getValue() != null;
    }

    /**
     * Gets the string value of a string annotation.
     * Returns null if not a string value.
     */
    public String getStringValue(AnnotationValue value) {
        if (value instanceof StringAnnotationValue) {
            return ((StringAnnotationValue) value).getValue();
        }
        return null;
    }

    /**
     * Gets the boolean value of a boolean annotation.
     * Returns null if not a boolean value.
     */
    public Boolean getBooleanValue(AnnotationValue value) {
        if (value instanceof BoolAnnotationValue) {
            String val = ((BoolAnnotationValue) value).getValue();
            return val != null && val.equals("true");
        }
        return null;
    }

    /**
     * Gets the integer value of a numeric annotation.
     * Returns null if not a number value.
     */
    public Integer getIntegerValue(AnnotationValue value) {
        if (value instanceof NumberAnnotationValue) {
            return ((NumberAnnotationValue) value).getValue();
        }
        return null;
    }
}
