package org.example.cds.annotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.example.cds.annotations.AnnotationDefinition.TargetType;
import org.example.cds.annotations.AnnotationDefinition.ValueType;

/**
 * Registry of standard SAP CAP CDS annotations.
 * Contains definitions for common annotations used in SAP CAP applications.
 */
public class AnnotationRegistry {

    private static final Map<String, AnnotationDefinition> ANNOTATIONS = new HashMap<>();

    static {
        registerCoreAnnotations();
        registerAuthorizationAnnotations();
        registerUIAnnotations();
        registerValidationAnnotations();
        registerODataAnnotations();
    }

    /**
     * Core CDS annotations.
     */
    private static void registerCoreAnnotations() {
        register("title", ValueType.STRING, Set.of(TargetType.ANY),
            "Human-readable title for UI display");

        register("description", ValueType.STRING, Set.of(TargetType.ANY),
            "Detailed description text");

        register("readonly", ValueType.BOOLEAN, Set.of(TargetType.ENTITY, TargetType.ELEMENT),
            "Marks entity or field as read-only");

        register("cds.autoexpose", ValueType.BOOLEAN, Set.of(TargetType.ENTITY),
            "Auto-expose entity in parent service");

        register("cds.persistence.skip", ValueType.BOOLEAN, Set.of(TargetType.ENTITY),
            "Skip database table generation");

        register("cds.persistence.journal", ValueType.BOOLEAN, Set.of(TargetType.ENTITY),
            "Enable temporal/journal tables for change tracking");

        register("cds.persistence.table", ValueType.STRING, Set.of(TargetType.ENTITY),
            "Specify custom database table name");
    }

    /**
     * Authorization and access control annotations.
     */
    private static void registerAuthorizationAnnotations() {
        register("requires", ValueType.STRING, Set.of(TargetType.SERVICE, TargetType.ENTITY),
            "Required user role or scope for access");

        register("restrict", ValueType.ARRAY, Set.of(TargetType.ENTITY),
            "Access restriction rules based on grants");
    }

    /**
     * SAP Fiori UI annotations.
     */
    private static void registerUIAnnotations() {
        register("UI.LineItem", ValueType.ARRAY, Set.of(TargetType.ENTITY),
            "Defines columns for list view");

        register("UI.SelectionFields", ValueType.ARRAY, Set.of(TargetType.ENTITY),
            "Defines filter fields");

        register("UI.HeaderInfo", ValueType.OBJECT, Set.of(TargetType.ENTITY),
            "Object page header configuration");

        register("UI.Identification", ValueType.ARRAY, Set.of(TargetType.ENTITY),
            "Identification section fields");

        register("UI.FieldGroup", ValueType.OBJECT, Set.of(TargetType.ENTITY),
            "Field group configuration");

        register("UI.Hidden", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Hide field in UI");

        register("UI.HiddenFilter", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Hide field from filter bar");

        register("UI.MultiLineText", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Display as multi-line text area");
    }

    /**
     * Validation annotations for data integrity.
     */
    private static void registerValidationAnnotations() {
        register("mandatory", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field is required");

        register("assert.range", ValueType.ARRAY, Set.of(TargetType.ELEMENT),
            "Value must be in range [min, max]");

        register("assert.format", ValueType.STRING, Set.of(TargetType.ELEMENT),
            "Value must match regular expression pattern");

        register("assert.notNull", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field must not be null");

        register("assert.unique", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field value must be unique across all records");

        register("assert.target", ValueType.BOOLEAN, Set.of(TargetType.ASSOCIATION),
            "Validate association target exists");
    }

    /**
     * OData capability and behavior annotations.
     */
    private static void registerODataAnnotations() {
        // OData capabilities
        register("Capabilities.Insertable", ValueType.BOOLEAN,
            Set.of(TargetType.ENTITY),
            "Allow creating new records via OData");

        register("Capabilities.Updatable", ValueType.BOOLEAN,
            Set.of(TargetType.ENTITY),
            "Allow updating existing records via OData");

        register("Capabilities.Deletable", ValueType.BOOLEAN,
            Set.of(TargetType.ENTITY),
            "Allow deleting records via OData");

        register("Capabilities.Readable", ValueType.BOOLEAN,
            Set.of(TargetType.ENTITY),
            "Allow reading records via OData");

        // Core OData annotations
        register("Core.Computed", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field value is computed by backend");

        register("Core.Immutable", ValueType.BOOLEAN, Set.of(TargetType.ELEMENT),
            "Field cannot be changed after initial creation");

        register("Core.Description", ValueType.STRING, Set.of(TargetType.ANY),
            "Description text for OData metadata");

        // Common OData annotations
        register("Common.Label", ValueType.STRING, Set.of(TargetType.ANY),
            "Display label for field");

        register("Common.Text", ValueType.STRING, Set.of(TargetType.ELEMENT),
            "Text association for value help");

        register("Common.ValueList", ValueType.OBJECT, Set.of(TargetType.ELEMENT),
            "Value help configuration");
    }

    /**
     * Registers an annotation definition.
     */
    private static void register(String name, ValueType valueType,
                                 Set<TargetType> targets, String description) {
        ANNOTATIONS.put(name, new AnnotationDefinition(name, valueType, targets, description));
    }

    /**
     * Looks up an annotation definition by name.
     * Returns empty Optional if annotation is not registered.
     */
    public static Optional<AnnotationDefinition> getAnnotation(String name) {
        return Optional.ofNullable(ANNOTATIONS.get(name));
    }

    /**
     * Checks if an annotation is known (registered in this registry).
     */
    public static boolean isKnownAnnotation(String name) {
        return ANNOTATIONS.containsKey(name);
    }

    /**
     * Checks if annotation name looks like a standard SAP annotation.
     * Standard patterns: UI.*, Core.*, Common.*, Capabilities.*, cds.*, odata.*
     *
     * This is used to distinguish between likely typos and intentional custom annotations.
     */
    public static boolean looksLikeStandardAnnotation(String name) {
        return name.startsWith("UI.") ||
               name.startsWith("Core.") ||
               name.startsWith("Common.") ||
               name.startsWith("Capabilities.") ||
               name.startsWith("cds.") ||
               name.startsWith("odata.") ||
               name.startsWith("assert.");
    }

    /**
     * Gets all registered annotation names.
     * Useful for autocomplete/suggestions.
     */
    public static Set<String> getAllAnnotationNames() {
        return ANNOTATIONS.keySet();
    }
}
