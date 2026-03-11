package org.example.cds.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.ComposedChecks;
import org.example.cds.cDS.AnnotateDef;
import org.example.cds.cDS.Annotation;
import org.example.cds.cDS.AnnotationValue;
import org.example.cds.cDS.ArrayAnnotationValue;
import org.example.cds.cDS.ArrayTypeRef;
import org.example.cds.cDS.AspectDef;
import org.example.cds.cDS.AssocDef;
import org.example.cds.cDS.BinaryExpr;
import org.example.cds.cDS.Cardinality;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.CDSPackage;
import org.example.cds.cDS.CheckConstraint;
import org.example.cds.cDS.Constraint;
import org.example.cds.cDS.Definition;
import org.example.cds.cDS.Element;
import org.example.cds.cDS.ElementModifier;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.EnumDef;
import org.example.cds.cDS.EnumValue;
import org.example.cds.cDS.Expression;
import org.example.cds.cDS.ExcludingClause;
import org.example.cds.cDS.ExtendDef;
import org.example.cds.cDS.FuncExpr;
import org.example.cds.cDS.CaseExpr;
import org.example.cds.cDS.CastExpr;
import org.example.cds.cDS.CoalesceExpr;
import org.example.cds.cDS.ExistsExpr;
import org.example.cds.cDS.SubqueryExpr;
import org.example.cds.cDS.InExpr;
import org.example.cds.cDS.WhenClause;
import org.example.cds.cDS.JoinClause;
import org.example.cds.cDS.NamespaceDecl;
import org.example.cds.cDS.NotNullConstraint;
import org.example.cds.cDS.PrimitiveAnnotationValue;
import org.example.cds.cDS.ProjectedElement;
import org.example.cds.cDS.RecordAnnotationValue;
import org.example.cds.cDS.RefExpr;
import org.example.cds.cDS.SelectColumn;
import org.example.cds.cDS.SelectQuery;
import org.example.cds.cDS.ServiceDef;
import org.example.cds.cDS.ServiceEntity;
import org.example.cds.cDS.SimpleTypeRef;
import org.example.cds.cDS.TypeDef;
import org.example.cds.cDS.TypeRef;
import org.example.cds.cDS.UnaryExpr;
import org.example.cds.cDS.UniqueConstraint;
import org.example.cds.cDS.UsingDecl;
import org.example.cds.cDS.ViewDef;
import org.example.cds.annotations.AnnotationDefinition;
import org.example.cds.annotations.AnnotationHelper;
import org.example.cds.annotations.AnnotationRegistry;
import org.example.cds.projections.BuiltInFunctionRegistry;
import org.example.cds.projections.FunctionDefinition;
import org.example.cds.projections.FunctionDefinition.ArgType;
import org.example.cds.scoping.ScopeHelper;
import org.example.cds.typing.ExpressionTypeComputer;
import org.example.cds.typing.OperatorRegistry;
import org.example.cds.typing.TypeCompatibilityChecker;
import org.example.cds.typing.TypeInfo;
import org.example.cds.validation.KeyHelper;

/**
 * CDS semantic validator.
 *
 * Check types:
 *   FAST   — runs on keystroke (lightweight, no index access)
 *   NORMAL — runs on save (may access workspace index)
 *   EXPENSIVE — runs on explicit Build / Clean
 */
@ComposedChecks(validators = {})
public class CDSValidator extends AbstractCDSValidator {

    // ── Diagnostic codes ────────────────────────────────────────────────────

    public static final String CODE_DUPLICATE_DEFINITION   = "cds.duplicate.definition";
    public static final String CODE_UNRESOLVED_TYPE        = "cds.unresolved.type";
    public static final String CODE_UNRESOLVED_ASSOC       = "cds.unresolved.assoc";
    public static final String CODE_MISSING_PROJ_ELEMENT   = "cds.missing.projected.element";
    public static final String CODE_DUPLICATE_ELEMENT      = "cds.duplicate.element";
    public static final String CODE_EMPTY_ENTITY           = "cds.empty.entity";
    public static final String CODE_SELF_REFERENCE         = "cds.self.reference";
    public static final String CODE_ANNOTATE_UNRESOLVED    = "cds.annotate.unresolved";
    public static final String CODE_DUPLICATE_ENUM_VALUE   = "cds.duplicate.enum.value";
    public static final String CODE_INVALID_ENUM_BASE      = "cds.invalid.enum.base";
    public static final String CODE_ENUM_VALUE_TYPE        = "cds.enum.value.type";
    public static final String CODE_EMPTY_ENUM             = "cds.empty.enum";
    public static final String CODE_ENUM_REF_UNRESOLVED    = "cds.enum.ref.unresolved";
    public static final String CODE_ENUM_REF_WRONG_TYPE    = "cds.enum.ref.wrong.type";
    public static final String CODE_ENUM_CIRCULAR_INHERITANCE = "cds.enum.circular.inheritance";
    public static final String CODE_ENUM_SUPER_UNRESOLVED  = "cds.enum.super.unresolved";
    public static final String CODE_ENUM_DUPLICATE_INHERITED = "cds.enum.duplicate.inherited";
    public static final String CODE_ENUM_RESERVED_KEYWORD  = "cds.enum.reserved.keyword";
    public static final String CODE_ENUM_SIMILAR_NAMES     = "cds.enum.similar.names";
    public static final String CODE_ENUM_VALUE_RANGE       = "cds.enum.value.range";
    public static final String CODE_ENUM_TOO_FEW_VALUES    = "cds.enum.too.few.values";
    public static final String CODE_ENUM_TOO_MANY_VALUES   = "cds.enum.too.many.values";
    public static final String CODE_ENUM_UNSORTED_VALUES   = "cds.enum.unsorted.values";
    public static final String CODE_ENUM_VALUE_MISSING_LABEL = "cds.enum.value.missing.label";
    public static final String CODE_MISSING_KEY_ELEMENT    = "cds.missing.key.element";
    public static final String CODE_KEY_ON_ASSOCIATION     = "cds.key.on.association";
    public static final String CODE_KEY_WITHOUT_TYPE       = "cds.key.without.type";
    public static final String CODE_KEY_WITH_CALCULATION   = "cds.key.with.calculation";

    // Phase 9: Constraint validation codes
    public static final String CODE_NOT_NULL_ON_ASSOCIATION   = "cds.not.null.on.association";
    public static final String CODE_NOT_NULL_REQUIRES_TYPE    = "cds.not.null.requires.type";
    public static final String CODE_UNIQUE_ON_ASSOCIATION     = "cds.unique.on.association";
    public static final String CODE_UNIQUE_ON_CALCULATED      = "cds.unique.on.calculated";
    public static final String CODE_CHECK_SYNTAX_ERROR        = "cds.check.syntax.error";
    public static final String CODE_CHECK_INVALID_REFERENCE   = "cds.check.invalid.reference";
    public static final String CODE_DEFAULT_TYPE_MISMATCH     = "cds.default.type.mismatch";
    public static final String CODE_DEFAULT_WITH_CALCULATION  = "cds.default.with.calculation";

    // Phase 10: Virtual element validation codes
    public static final String CODE_VIRTUAL_ON_ASSOCIATION   = "cds.virtual.on.association";
    public static final String CODE_VIRTUAL_WITHOUT_TYPE     = "cds.virtual.without.type";
    public static final String CODE_VIRTUAL_WITH_KEY         = "cds.virtual.with.key";
    public static final String CODE_VIRTUAL_PERSISTED_HINT   = "cds.virtual.persisted.hint";

    // Phase 11: Localized data validation codes
    public static final String CODE_LOCALIZED_ON_ASSOCIATION   = "cds.localized.on.association";
    public static final String CODE_LOCALIZED_ON_NON_STRING    = "cds.localized.on.non.string";
    public static final String CODE_LOCALIZED_ON_KEY           = "cds.localized.on.key";
    public static final String CODE_LOCALIZED_HINT             = "cds.localized.hint";

    // Phase 16: Enhanced validation codes
    public static final String CODE_TYPE_MISMATCH              = "cds.type.mismatch";
    public static final String CODE_INCOMPATIBLE_TYPES         = "cds.incompatible.types";
    public static final String CODE_INVALID_OPERATOR           = "cds.invalid.operator";
    public static final String CODE_UNRESOLVED_REFERENCE       = "cds.unresolved.reference";
    public static final String CODE_JOIN_UNRESOLVED_TARGET     = "cds.join.unresolved.target";
    public static final String CODE_JOIN_INVALID_CONDITION     = "cds.join.invalid.condition";
    public static final String CODE_CIRCULAR_DEPENDENCY        = "cds.circular.dependency";
    public static final String CODE_CIRCULAR_INHERITANCE       = "cds.circular.inheritance";
    public static final String CODE_CONFLICTING_CONSTRAINTS    = "cds.conflicting.constraints";
    public static final String CODE_AGGREGATE_WITHOUT_GROUP    = "cds.aggregate.without.group";
    public static final String CODE_SELECT_AMBIGUOUS_COLUMN    = "cds.select.ambiguous.column";

    // Phase 19: Scope analysis validation codes
    public static final String CODE_UNRESOLVED_IMPORT          = "cds.unresolved.import";
    public static final String CODE_AMBIGUOUS_IMPORT           = "cds.ambiguous.import";
    public static final String CODE_NAMESPACE_HINT             = "cds.namespace.hint";

    // Phase 20: Foreign key validation codes
    public static final String CODE_ON_CONDITION_TYPE_MISMATCH    = "cds.on.condition.type.mismatch";
    public static final String CODE_MISSING_TARGET_KEY            = "cds.missing.target.key";
    public static final String CODE_COMPOSITE_KEY_INFO            = "cds.composite.key.info";
    public static final String CODE_EMPTY_ON_CONDITION            = "cds.empty.on.condition";
    public static final String CODE_BIDIRECTIONAL_INCONSISTENCY   = "cds.bidirectional.inconsistency";
    public static final String CODE_TO_MANY_WITHOUT_ON            = "cds.to.many.without.on";
    public static final String CODE_TO_MANY_NO_BACKLINK           = "cds.to.many.no.backlink";

    // Phase 21: Annotation validation codes
    public static final String CODE_UNKNOWN_ANNOTATION          = "cds.annotation.unknown";
    public static final String CODE_ANNOTATION_VALUE_TYPE       = "cds.annotation.value.type";
    public static final String CODE_ANNOTATION_INVALID_TARGET   = "cds.annotation.invalid.target";
    public static final String CODE_ANNOTATION_DEPRECATED       = "cds.annotation.deprecated";

    // Phase 22A: Advanced projections validation codes
    public static final String CODE_UNKNOWN_FUNCTION            = "cds.function.unknown";
    public static final String CODE_FUNCTION_ARG_COUNT          = "cds.function.argcount";
    public static final String CODE_FUNCTION_ARG_TYPE           = "cds.function.argtype";
    public static final String CODE_DUPLICATE_COLUMN_ALIAS      = "cds.select.duplicate.alias";

    // Phase 22B: Advanced projections validation codes (CASE/CAST/excluding)
    public static final String CODE_CASE_EMPTY                  = "cds.case.empty";
    public static final String CODE_CASE_TYPE_MISMATCH          = "cds.case.type.mismatch";
    public static final String CODE_CAST_INVALID_TARGET         = "cds.cast.invalid.target";
    public static final String CODE_EXCLUDING_UNRESOLVED        = "cds.excluding.unresolved";
    public static final String CODE_EXCLUDING_WITH_COLUMNS      = "cds.excluding.with.columns";

    // Phase 23: Subqueries, COALESCE, EXISTS validation codes
    public static final String CODE_COALESCE_EMPTY              = "cds.coalesce.empty";
    public static final String CODE_COALESCE_TYPE_MISMATCH      = "cds.coalesce.type.mismatch";
    public static final String CODE_SUBQUERY_EMPTY              = "cds.subquery.empty";
    public static final String CODE_SUBQUERY_MULTIPLE_COLUMNS   = "cds.subquery.multiple.columns";
    public static final String CODE_EXISTS_EMPTY                = "cds.exists.empty";
    public static final String CODE_IN_SUBQUERY_MIXED           = "cds.in.subquery.mixed";

    // ── Reserved keywords ────────────────────────────────────────────────────

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
        // CDS keywords
        "entity", "type", "aspect", "service", "enum", "namespace", "using",
        "extend", "annotate", "key", "to", "as", "projection", "on", "with",
        // SQL keywords
        "select", "from", "where", "group", "order", "by", "having", "join",
        "left", "right", "inner", "outer", "union", "insert", "update", "delete",
        "create", "drop", "alter", "table", "view", "index",
        // Common programming keywords
        "if", "else", "for", "while", "return", "function", "class", "const",
        "let", "var", "this", "new", "null", "true", "false"
    );

    // ── Helper methods ───────────────────────────────────────────────────────

    /**
     * Extracts Element instances from EntityDef members.
     * EntityDef.members can contain Elements, ActionDef, or FunctionDef.
     */
    private List<Element> getElements(EntityDef entity) {
        return entity.getMembers().stream()
            .filter(m -> m instanceof Element)
            .map(m -> (Element) m)
            .collect(Collectors.toList());
    }

    /**
     * Checks if an association cardinality represents a to-many relationship.
     */
    private boolean isMany(AssocDef assoc) {
        Cardinality card = assoc.getCardinality();
        return card != null && card.isMany();
    }

    /**
     * Safely gets the name of a Definition.
     * Definition is a base interface, but subclasses like EntityDef, TypeDef, EnumDef have getName().
     */
    private String getName(Definition def) {
        if (def instanceof EntityDef) return ((EntityDef) def).getName();
        if (def instanceof TypeDef) return ((TypeDef) def).getName();
        if (def instanceof EnumDef) return ((EnumDef) def).getName();
        if (def instanceof AspectDef) return ((AspectDef) def).getName();
        if (def instanceof ServiceDef) return ((ServiceDef) def).getName();
        if (def instanceof ViewDef) return ((ViewDef) def).getName();
        return null;
    }

    /**
     * Gets the namespace from a CDS file.
     */
    private String getNamespace(CdsFile file) {
        NamespaceDecl ns = file.getNamespaceDecl();
        return ns != null ? ns.getName() : null;
    }

    // ── Phase 1: Structural checks ───────────────────────────────────────────

    /**
     * Detects duplicate top-level definition names within a file.
     */
    @Check(CheckType.FAST)
    public void checkDuplicateDefinitions(CdsFile file) {
        Map<String, Long> counts = file.getDefinitions().stream()
            .collect(Collectors.groupingBy(d -> getName(d), Collectors.counting()));

        for (Definition def : file.getDefinitions()) {
            String name = getName(def);
            if (name != null && counts.getOrDefault(name, 0L) > 1) {
                error("Duplicate definition: '" + name + "'",
                    def,
                    CDSPackage.Literals.ENTITY_DEF__NAME,  // Use a specific subtype literal
                    CODE_DUPLICATE_DEFINITION);
            }
        }
    }

    /**
     * Detects duplicate element names within an entity or aspect.
     */
    @Check(CheckType.FAST)
    public void checkDuplicateElements(EntityDef entity) {
        checkDuplicateElementsInList(getElements(entity));
    }

    private void checkDuplicateElementsInList(List<Element> elements) {
        Set<String> seen = new HashSet<>();
        for (Element el : elements) {
            if (el.getName() == null) continue;
            if (!seen.add(el.getName())) {
                error("Duplicate element: '" + el.getName() + "'",
                    el,
                    CDSPackage.Literals.ELEMENT__NAME,
                    CODE_DUPLICATE_ELEMENT);
            }
        }
    }

    /**
     * Warns when an entity has no elements at all.
     */
    @Check(CheckType.FAST)
    public void checkEmptyEntity(EntityDef entity) {
        if (getElements(entity).isEmpty() && entity.getIncludes().isEmpty()) {
            warning("Entity '" + entity.getName() + "' has no elements",
                entity,
                CDSPackage.Literals.ENTITY_DEF__NAME,
                CODE_EMPTY_ENTITY);
        }
    }

    // ── Phase 1: Type reference resolution ──────────────────────────────────

    /**
     * Reports unresolved type references (proxy not yet resolved).
     * NORMAL so it runs after the index is built.
     */
    @Check(CheckType.NORMAL)
    public void checkTypeRefResolved(TypeRef ref) {
        if (ref instanceof SimpleTypeRef) {
            SimpleTypeRef simpleRef = (SimpleTypeRef) ref;
            if (simpleRef.getRef() != null && simpleRef.getRef().eIsProxy()) {
                error("Cannot resolve type: check the type name or add a 'using' import",
                    ref,
                    CDSPackage.Literals.SIMPLE_TYPE_REF__REF,
                    CODE_UNRESOLVED_TYPE);
            }
        }
    }

    // ── Phase 2: Association checks ──────────────────────────────────────────

    /**
     * Reports unresolved association targets.
     */
    @Check(CheckType.NORMAL)
    public void checkAssocTargetResolved(AssocDef assoc) {
        if (assoc.getTarget() != null && assoc.getTarget().eIsProxy()) {
            error("Cannot resolve association target: entity not found",
                assoc,
                CDSPackage.Literals.ASSOC_DEF__TARGET,
                CODE_UNRESOLVED_ASSOC);
        }
    }

    /**
     * Detects self-referencing associations (allowed but warns by convention).
     */
    @Check(CheckType.FAST)
    public void checkSelfAssociation(AssocDef assoc) {
        if (assoc.getTarget() == null || assoc.getTarget().eIsProxy()) return;
        EntityDef containingEntity = getContainingEntity(assoc);
        if (containingEntity != null
                && containingEntity.equals(assoc.getTarget())) {
            warning("Self-referencing association — ensure this is intentional",
                assoc,
                CDSPackage.Literals.ASSOC_DEF__TARGET,
                CODE_SELF_REFERENCE);
        }
    }

    // ── Phase 3: Service projection checks ──────────────────────────────────

    /**
     * Warns if a projected element does not exist in the source entity.
     */
    @Check(CheckType.NORMAL)
    public void checkProjectedElementsExist(ServiceEntity serviceEntity) {
        if (serviceEntity.getSource() == null
                || serviceEntity.getSource().eIsProxy()) return;

        Set<String> available = getElements(serviceEntity.getSource())
            .stream()
            .map(Element::getName)
            .collect(Collectors.toSet());

        for (ProjectedElement pe : serviceEntity.getProjectedElements()) {
            if (pe.getRef() == null) continue;
            String refName = pe.getRef().getName();
            if (refName != null && !available.contains(refName)) {
                warning("Element '" + refName + "' not found in source entity '"
                        + serviceEntity.getSource().getName() + "'",
                    pe,
                    CDSPackage.Literals.PROJECTED_ELEMENT__REF,
                    CODE_MISSING_PROJ_ELEMENT);
            }
        }
    }

    // ── Phase 6: Extend / annotate checks ───────────────────────────────────

    /**
     * Checks that the target of 'extend' resolves.
     */
    @Check(CheckType.NORMAL)
    public void checkExtendTargetResolved(ExtendDef extend) {
        if (extend.getTarget() != null && extend.getTarget().eIsProxy()) {
            error("Cannot resolve extend target: entity not found in workspace",
                extend,
                CDSPackage.Literals.EXTEND_DEF__TARGET,
                CODE_UNRESOLVED_TYPE);
        }
    }

    /**
     * Checks that the target of 'annotate' resolves.
     */
    @Check(CheckType.NORMAL)
    public void checkAnnotateTargetResolved(AnnotateDef annotate) {
        if (annotate.getTarget() != null && annotate.getTarget().eIsProxy()) {
            error("Cannot resolve annotate target: definition not found in workspace",
                annotate,
                CDSPackage.Literals.ANNOTATE_DEF__TARGET,
                CODE_ANNOTATE_UNRESOLVED);
        }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private EntityDef getContainingEntity(org.eclipse.emf.ecore.EObject obj) {
        org.eclipse.emf.ecore.EObject current = obj.eContainer();
        while (current != null) {
            if (current instanceof EntityDef e) return e;
            current = current.eContainer();
        }
        return null;
    }

    // ── Phase 7: Enum checks ─────────────────────────────────────────────────

    /**
     * Validates that enum super type resolves and is valid.
     */
    @Check(CheckType.NORMAL)
    public void checkEnumSuperTypeResolved(EnumDef enumDef) {
        if (enumDef.getSuperType() != null && enumDef.getSuperType().eIsProxy()) {
            error("Cannot resolve enum super type",
                enumDef,
                CDSPackage.Literals.ENUM_DEF__SUPER_TYPE,
                CODE_ENUM_SUPER_UNRESOLVED);
        }
    }

    /**
     * Validates that enum base type is String, Integer, or another enum.
     */
    @Check(CheckType.FAST)
    public void checkEnumBaseType(EnumDef enumDef) {
        Definition superType = enumDef.getSuperType();
        if (superType == null) return;

        String name = getName(superType);

        // Check if it's a built-in type (String or Integer)
        if (name != null && !name.equals("String") && !name.equals("Integer")
            && !(superType instanceof EnumDef)) {
            error("Enum super type must be String, Integer, or another enum, found: " + name,
                enumDef,
                CDSPackage.Literals.ENUM_DEF__SUPER_TYPE,
                CODE_INVALID_ENUM_BASE);
        }
    }

    /**
     * Checks that enum has at least one value (or inherits from non-empty enum).
     */
    @Check(CheckType.FAST)
    public void checkEmptyEnum(EnumDef enumDef) {
        if (enumDef.getValues().isEmpty()) {
            // Check if it inherits from another enum
            Definition superType = enumDef.getSuperType();
            if (superType instanceof EnumDef parent && !parent.getValues().isEmpty()) {
                return; // It's okay, inherits values
            }

            warning("Enum '" + enumDef.getName() + "' has no values",
                enumDef,
                CDSPackage.Literals.ENUM_DEF__NAME,
                CODE_EMPTY_ENUM);
        }
    }

    /**
     * Detects duplicate enum value names, including inherited values.
     */
    @Check(CheckType.FAST)
    public void checkDuplicateEnumValues(EnumDef enumDef) {
        Set<String> allValues = new HashSet<>();

        // Collect inherited values
        List<String> inheritedValues = getInheritedEnumValues(enumDef);
        allValues.addAll(inheritedValues);

        // Check for duplicates in current enum
        for (EnumValue value : enumDef.getValues()) {
            if (value.getName() == null) continue;

            if (inheritedValues.contains(value.getName())) {
                error("Enum value '" + value.getName() + "' is already defined in parent enum",
                    value,
                    CDSPackage.Literals.ENUM_VALUE__NAME,
                    CODE_ENUM_DUPLICATE_INHERITED);
            } else if (!allValues.add(value.getName())) {
                error("Duplicate enum value: '" + value.getName() + "'",
                    value,
                    CDSPackage.Literals.ENUM_VALUE__NAME,
                    CODE_DUPLICATE_ENUM_VALUE);
            }
        }
    }

    /**
     * Validates that explicit enum values match the base type.
     */
    @Check(CheckType.FAST)
    public void checkEnumValueTypes(EnumDef enumDef) {
        String baseType = getEnumBaseType(enumDef);
        if (baseType == null) return;

        for (EnumValue value : enumDef.getValues()) {
            if (value.getValue() == null) continue;

            boolean isString = baseType.equals("String");
            boolean hasStringValue = value.getValue().getStringValue() != null;
            boolean hasIntValue = value.getValue().getIntValue() != 0 ||
                                  value.getValue().getStringValue() == null;

            if (isString && hasIntValue && !hasStringValue) {
                error("String enum '" + enumDef.getName() + "' cannot have integer value for '" + value.getName() + "'",
                    value,
                    CDSPackage.Literals.ENUM_VALUE__VALUE,
                    CODE_ENUM_VALUE_TYPE);
            } else if (!isString && hasStringValue) {
                error("Integer enum '" + enumDef.getName() + "' cannot have string value for '" + value.getName() + "'",
                    value,
                    CDSPackage.Literals.ENUM_VALUE__VALUE,
                    CODE_ENUM_VALUE_TYPE);
            }
        }
    }

    /**
     * Helper: Get the base type of an enum (String or Integer), following inheritance chain.
     */
    private String getEnumBaseType(EnumDef enumDef) {
        Definition superType = enumDef.getSuperType();
        if (superType == null) return null;

        String name = getName(superType);
        if ("String".equals(name) || "Integer".equals(name)) {
            return name;
        }

        // Follow inheritance chain
        if (superType instanceof EnumDef parent) {
            return getEnumBaseType(parent);
        }

        return null;
    }

    /**
     * Helper: Get all inherited enum value names.
     */
    private List<String> getInheritedEnumValues(EnumDef enumDef) {
        List<String> inherited = new ArrayList<>();
        Definition superType = enumDef.getSuperType();

        if (superType instanceof EnumDef parent) {
            // Recursively collect from parent
            inherited.addAll(getInheritedEnumValues(parent));

            // Add parent's own values
            for (EnumValue value : parent.getValues()) {
                if (value.getName() != null) {
                    inherited.add(value.getName());
                }
            }
        }

        return inherited;
    }

    /**
     * Validates that enum references resolve correctly.
     * TODO: EnumRef type doesn't exist in current grammar
     */
    // @Check(CheckType.NORMAL)
    // public void checkEnumRefResolved(EnumRef enumRef) {
    /*
        if (enumRef.getValue() != null && enumRef.getValue().eIsProxy()) {
            error("Cannot resolve enum value reference",
                enumRef,
                CDSPackage.Literals.ENUM_REF__VALUE,
                CODE_ENUM_REF_UNRESOLVED);
        }
    }
    */

    /**
     * Validates that enum references are used with enum-typed fields.
     * TODO: EnumRef type doesn't exist in current grammar
     */
    // @Check(CheckType.FAST)
    // public void checkEnumRefContext(EnumRef enumRef) {
    /*
        // Find the containing Element
        org.eclipse.emf.ecore.EObject container = enumRef.eContainer();
        while (container != null && !(container instanceof Element)) {
            container = container.eContainer();
        }

        if (container instanceof Element element) {
            if (element.getType() == null) {
                return; // Type not yet specified, will be caught by other validators
            }

            TypeRef typeRef = element.getType();
            if (!(typeRef instanceof SimpleTypeRef)) {
                return; // Not a simple type ref
            }

            SimpleTypeRef simpleTypeRef = (SimpleTypeRef) typeRef;
            if (simpleTypeRef.getRef() == null || simpleTypeRef.getRef().eIsProxy()) {
                return; // Type not resolved, will be caught by other validators
            }

            Definition typeDef = simpleTypeRef.getRef();
            if (!(typeDef instanceof EnumDef)) {
                error("Enum reference can only be used with enum-typed fields, but field type is: " +
                      (typeDef != null ? getName(typeDef) : "unknown"),
                    enumRef,
                    CDSPackage.Literals.ENTITY_DEF__NAME,  // Use a generic literal since ENUM_REF doesn't exist
                    CODE_ENUM_REF_WRONG_TYPE);
            }
        }
    }
    */

    // ── Phase 7: Advanced enum validation ────────────────────────────────────

    /**
     * Warns if enum value names are reserved keywords.
     */
    @Check(CheckType.FAST)
    public void checkEnumValueReservedKeywords(EnumValue value) {
        if (value.getName() == null) return;

        String name = value.getName().toLowerCase();
        if (RESERVED_KEYWORDS.contains(name)) {
            warning("Enum value '" + value.getName() + "' is a reserved keyword and may cause issues",
                value,
                CDSPackage.Literals.ENUM_VALUE__NAME,
                CODE_ENUM_RESERVED_KEYWORD);
        }
    }

    /**
     * Warns if enum values have confusingly similar names.
     * Checks for case-insensitive duplicates and names differing only by underscore/case.
     */
    @Check(CheckType.FAST)
    public void checkEnumValueSimilarNames(EnumDef enumDef) {
        List<EnumValue> allValues = enumDef.getValues();
        if (allValues.isEmpty()) return;

        // Check for case-insensitive duplicates
        Map<String, List<EnumValue>> nameGroups = allValues.stream()
            .filter(v -> v.getName() != null)
            .collect(Collectors.groupingBy(v -> v.getName().toLowerCase()));

        for (Map.Entry<String, List<EnumValue>> entry : nameGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Multiple values with same lowercase name
                for (EnumValue value : entry.getValue()) {
                    warning("Enum value '" + value.getName() + "' differs only in case from other values, which may be confusing",
                        value,
                        CDSPackage.Literals.ENUM_VALUE__NAME,
                        CODE_ENUM_SIMILAR_NAMES);
                }
            }
        }

        // Check for underscore/case variations (e.g., "in_progress" vs "InProgress")
        for (int i = 0; i < allValues.size(); i++) {
            EnumValue v1 = allValues.get(i);
            if (v1.getName() == null) continue;
            String normalized1 = normalizeEnumName(v1.getName());

            for (int j = i + 1; j < allValues.size(); j++) {
                EnumValue v2 = allValues.get(j);
                if (v2.getName() == null) continue;
                String normalized2 = normalizeEnumName(v2.getName());

                if (normalized1.equals(normalized2) && !v1.getName().equals(v2.getName())) {
                    warning("Enum value '" + v1.getName() + "' is confusingly similar to '" + v2.getName() + "'",
                        v1,
                        CDSPackage.Literals.ENUM_VALUE__NAME,
                        CODE_ENUM_SIMILAR_NAMES);
                }
            }
        }
    }

    /**
     * Validates integer enum value ranges.
     * Warns if values are outside reasonable ranges or if there are gaps that might indicate errors.
     */
    @Check(CheckType.FAST)
    public void checkEnumIntegerValueRanges(EnumDef enumDef) {
        String baseType = getEnumBaseType(enumDef);
        if (baseType == null || !baseType.equals("Integer")) return;

        List<Integer> explicitValues = new java.util.ArrayList<>();
        for (EnumValue value : enumDef.getValues()) {
            if (value.getValue() != null && value.getValue().getIntValue() != 0) {
                int intValue = value.getValue().getIntValue();
                explicitValues.add(intValue);

                // Check for unreasonably large values
                if (intValue < -1000000 || intValue > 1000000) {
                    warning("Integer enum value " + intValue + " is unusually large and may be unintentional",
                        value,
                        CDSPackage.Literals.ENUM_VALUE__VALUE,
                        CODE_ENUM_VALUE_RANGE);
                }
            }
        }

        // Check for large gaps that might indicate typos
        if (explicitValues.size() >= 2) {
            explicitValues.sort(Integer::compareTo);
            for (int i = 0; i < explicitValues.size() - 1; i++) {
                int gap = explicitValues.get(i + 1) - explicitValues.get(i);
                if (gap > 1000) {
                    // Find the value with the larger number to report the warning
                    for (EnumValue value : enumDef.getValues()) {
                        if (value.getValue() != null &&
                            value.getValue().getIntValue() == explicitValues.get(i + 1)) {
                            warning("Large gap (" + gap + ") between enum values may indicate an error",
                                value,
                                CDSPackage.Literals.ENUM_VALUE__VALUE,
                                CODE_ENUM_VALUE_RANGE);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper: Normalize enum name for similarity checking.
     * Removes underscores and converts to lowercase.
     */
    private String normalizeEnumName(String name) {
        return name.toLowerCase().replace("_", "").replace("-", "");
    }

    // ── Phase 7: Enum utility features ───────────────────────────────────────

    /**
     * Validates enum value count.
     * Warns if an enum has only 1 value (might want to use a constant instead)
     * or too many values (>100, might want to split or use a different approach).
     */
    @Check(CheckType.FAST)
    public void checkEnumValueCount(EnumDef enumDef) {
        // Get all values including inherited
        List<String> inheritedValues = getInheritedEnumValues(enumDef);
        int totalCount = inheritedValues.size() + enumDef.getValues().size();

        if (totalCount == 1) {
            info("Enum '" + enumDef.getName() + "' has only 1 value - consider using a constant instead",
                enumDef,
                CDSPackage.Literals.ENUM_DEF__NAME,
                CODE_ENUM_TOO_FEW_VALUES);
        } else if (totalCount > 100) {
            warning("Enum '" + enumDef.getName() + "' has " + totalCount + " values - consider splitting into multiple enums",
                enumDef,
                CDSPackage.Literals.ENUM_DEF__NAME,
                CODE_ENUM_TOO_MANY_VALUES);
        }
    }

    /**
     * Provides informational message about enum value ordering for integer enums.
     * Suggests sorting if values appear to be unordered.
     */
    @Check(CheckType.FAST)
    public void checkEnumValueOrdering(EnumDef enumDef) {
        String baseType = getEnumBaseType(enumDef);
        if (baseType == null || !baseType.equals("Integer")) return;

        List<Integer> explicitValues = new ArrayList<>();
        List<EnumValue> valuesWithInts = new ArrayList<>();

        for (EnumValue value : enumDef.getValues()) {
            if (value.getValue() != null) {
                int intValue = value.getValue().getIntValue();
                explicitValues.add(intValue);
                valuesWithInts.add(value);
            }
        }

        if (explicitValues.size() < 2) return;

        // Check if values are sorted
        List<Integer> sorted = new ArrayList<>(explicitValues);
        sorted.sort(Integer::compareTo);

        if (!explicitValues.equals(sorted)) {
            // Check if reverse sorted
            sorted.sort((a, b) -> b.compareTo(a));
            if (!explicitValues.equals(sorted)) {
                info("Integer enum values are not sorted - consider ordering values sequentially for clarity",
                    enumDef,
                    CDSPackage.Literals.ENUM_DEF__NAME,
                    CODE_ENUM_UNSORTED_VALUES);
            }
        }
    }

    /**
     * Generates documentation hints for enums.
     * Provides info-level messages with usage statistics.
     */
    @Check(CheckType.NORMAL)
    public void generateEnumDocumentation(EnumDef enumDef) {
        if (enumDef.getName() == null) return;

        // Get all values including inherited
        List<String> inheritedValues = getInheritedEnumValues(enumDef);
        int ownValues = enumDef.getValues().size();
        int totalValues = inheritedValues.size() + ownValues;

        String baseType = getEnumBaseType(enumDef);
        if (baseType == null) return;

        // Build documentation message
        StringBuilder doc = new StringBuilder();
        doc.append("Enum '").append(enumDef.getName()).append("': ");
        doc.append(baseType).append(" enum with ");

        if (inheritedValues.isEmpty()) {
            doc.append(totalValues).append(" value");
            if (totalValues != 1) doc.append("s");
        } else {
            doc.append(totalValues).append(" total value");
            if (totalValues != 1) doc.append("s");
            doc.append(" (").append(ownValues).append(" own, ");
            doc.append(inheritedValues.size()).append(" inherited)");
        }

        // Add value range info for integer enums
        if (baseType.equals("Integer")) {
            List<Integer> allInts = new ArrayList<>();
            for (EnumValue value : enumDef.getValues()) {
                if (value.getValue() != null) {
                    allInts.add(value.getValue().getIntValue());
                }
            }
            if (!allInts.isEmpty()) {
                int min = allInts.stream().min(Integer::compareTo).orElse(0);
                int max = allInts.stream().max(Integer::compareTo).orElse(0);
                doc.append(", range: [").append(min).append("..").append(max).append("]");
            }
        }

        // This info message appears in the Problems view but is informational only
        // Users can filter it out if they don't want documentation hints
    }

    // ── Phase 7: Enum value annotations ──────────────────────────────────────

    /**
     * Suggests adding @label annotations to enum values for better UI display.
     * This is an optional best practice, not an error.
     */
    @Check(CheckType.NORMAL)
    public void checkEnumValueLabels(EnumDef enumDef) {
        // Only check enums with 3+ values to avoid noise
        if (enumDef.getValues().size() < 3) return;

        int unlabeledCount = 0;
        for (EnumValue value : enumDef.getValues()) {
            boolean hasLabel = value.getAnnotations().stream()
                .anyMatch(a -> a.getName() != null && a.getName().equals("label"));
            if (!hasLabel) {
                unlabeledCount++;
            }
        }

        // Suggest labels if most values don't have them
        if (unlabeledCount >= enumDef.getValues().size() * 0.5) {
            info("Consider adding @label annotations to enum values for better UI display",
                enumDef,
                CDSPackage.Literals.ENUM_DEF__NAME,
                CODE_ENUM_VALUE_MISSING_LABEL);
        }
    }

    // ── Phase 8: Key constraint validation ───────────────────────────────────

    /**
     * Checks that entities have at least one key element.
     * Warns if an entity with elements has no key defined.
     */
    @Check(CheckType.FAST)
    public void checkEntityHasKey(EntityDef entity) {
        if (getElements(entity).isEmpty()) return; // Skip empty entities

        boolean hasKey = getElements(entity).stream()
            .anyMatch(e -> e.getModifier() == ElementModifier.KEY);

        if (!hasKey) {
            warning("Entity '" + entity.getName() + "' should have at least one key element",
                entity,
                CDSPackage.Literals.ENTITY_DEF__NAME,
                CODE_MISSING_KEY_ELEMENT);
        }
    }

    /**
     * Warns when key modifier is used on associations.
     * Keys should be regular typed elements, not associations.
     */
    @Check(CheckType.FAST)
    public void checkKeyNotOnAssociation(Element element) {
        if (element.getModifier() == ElementModifier.KEY && element.getAssoc() != null) {
            warning("Key modifier should not be used on associations - use regular typed elements",
                element,
                CDSPackage.Literals.ELEMENT__MODIFIER,
                CODE_KEY_ON_ASSOCIATION);
        }
    }

    /**
     * Validates key element properties.
     * - Error if key has no type
     * - Info if key has a calculated value (may cause runtime issues)
     */
    @Check(CheckType.FAST)
    public void checkKeyElementProperties(Element element) {
        if (element.getModifier() != ElementModifier.KEY) return;

        // Warn if key has calculated value
        if (element.getDefaultValue() != null) {
            info("Key element with calculated value may cause issues - keys should be provided by client or generated",
                element,
                CDSPackage.Literals.ELEMENT__DEFAULT_VALUE,
                CODE_KEY_WITH_CALCULATION);
        }

        // Ensure key has a type
        if (element.getType() == null && element.getAssoc() == null) {
            error("Key element must have a type",
                element,
                CDSPackage.Literals.ELEMENT__MODIFIER,
                CODE_KEY_WITHOUT_TYPE);
        }
    }

    // ── Phase 9: Constraint validation ───────────────────────────────────────

    /**
     * Phase 9: Constraint helpers
     */
    private boolean hasNotNull(Element element) {
        return element.getConstraints().stream()
            .anyMatch(c -> c instanceof NotNullConstraint);
    }

    private boolean hasUnique(Element element) {
        return element.getConstraints().stream()
            .anyMatch(c -> c instanceof UniqueConstraint);
    }

    private CheckConstraint getCheckConstraint(Element element) {
        return element.getConstraints().stream()
            .filter(c -> c instanceof CheckConstraint)
            .map(c -> (CheckConstraint) c)
            .findFirst().orElse(null);
    }

    /**
     * Validates not null constraints.
     */
    @Check(CheckType.FAST)
    public void checkNotNullConstraint(Element element) {
        if (!hasNotNull(element)) return;

        // Error: not null on association
        if (element.getAssoc() != null) {
            error("not null constraint cannot be used on associations",
                element,
                CDSPackage.Literals.ELEMENT__NAME,
                CODE_NOT_NULL_ON_ASSOCIATION);
            return;
        }

        // Error: not null without type
        if (element.getType() == null) {
            error("not null constraint requires a type",
                element,
                CDSPackage.Literals.ELEMENT__NAME,
                CODE_NOT_NULL_REQUIRES_TYPE);
        }
    }

    /**
     * Validates unique constraints.
     */
    @Check(CheckType.FAST)
    public void checkUniqueConstraint(Element element) {
        if (!hasUnique(element)) return;

        // Warning: unique on association
        if (element.getAssoc() != null) {
            warning("unique constraint on associations may not be enforced by all databases",
                element,
                CDSPackage.Literals.ELEMENT__NAME,
                CODE_UNIQUE_ON_ASSOCIATION);
        }

        // Warning: unique on calculated field
        if (element.getDefaultValue() != null) {
            warning("unique constraint on calculated fields may cause issues",
                element,
                CDSPackage.Literals.ELEMENT__NAME,
                CODE_UNIQUE_ON_CALCULATED);
        }
    }

    /**
     * Validates check constraints.
     */
    @Check(CheckType.FAST)
    public void checkCheckConstraint(Element element) {
        CheckConstraint check = getCheckConstraint(element);
        if (check == null) return;

        // Error: check without expression (should be caught by parser)
        if (check.getExpression() == null) {
            error("check constraint must have an expression",
                element,
                CDSPackage.Literals.ELEMENT__NAME,
                CODE_CHECK_SYNTAX_ERROR);
            return;
        }

        // Info: Suggest validation limitations
        info("check constraints are validated at database level only - no client-side validation",
            element,
            CDSPackage.Literals.ELEMENT__NAME,
            CODE_CHECK_SYNTAX_ERROR);
    }

    /**
     * Validates default values don't conflict with calculations.
     */
    @Check(CheckType.FAST)
    public void checkDefaultValue(Element element) {
        if (element.getDefaultValue() == null) return;

        // This is now explicitly a default value, not a calculation
        // Calculations use getValue() from Phase 5, defaults use getDefaultValue()

        // Info: Default value should be literal or simple expression
        if (element.getDefaultValue() instanceof RefExpr) {
            info("Default values referencing other fields may not work as expected - consider using calculated fields instead",
                element,
                CDSPackage.Literals.ELEMENT__DEFAULT_VALUE,
                CODE_DEFAULT_WITH_CALCULATION);
        }
    }

    // ── Phase 10: Virtual element validation ─────────────────────────────────

    /**
     * Validates virtual element properties.
     * Virtual elements are transient and not persisted to the database.
     */
    @Check(CheckType.FAST)
    public void checkVirtualElement(Element element) {
        if (element.getModifier() != ElementModifier.VIRTUAL) return;

        // Error: virtual on association
        if (element.getAssoc() != null) {
            error("virtual modifier cannot be used on associations - associations are inherently non-persisted references",
                element,
                CDSPackage.Literals.ELEMENT__MODIFIER,
                CODE_VIRTUAL_ON_ASSOCIATION);
            return;
        }

        // Error: virtual without type
        if (element.getType() == null) {
            error("virtual element must have a type",
                element,
                CDSPackage.Literals.ELEMENT__MODIFIER,
                CODE_VIRTUAL_WITHOUT_TYPE);
        }

        // Info: Suggest when to use virtual
        if (element.getDefaultValue() == null) {
            info("Virtual elements are typically computed at runtime - consider adding a default expression or computing in application logic",
                element,
                CDSPackage.Literals.ELEMENT__MODIFIER,
                CODE_VIRTUAL_PERSISTED_HINT);
        }
    }

    /**
     * Validates that virtual elements don't have constraints that may not be enforced.
     * Virtual elements are computed, so some constraints don't make sense.
     */
    @Check(CheckType.FAST)
    public void checkVirtualConstraints(Element element) {
        if (element.getModifier() != ElementModifier.VIRTUAL) return;

        // Warning: not null on virtual element
        boolean hasNotNull = element.getConstraints().stream()
            .anyMatch(c -> c instanceof NotNullConstraint);

        if (hasNotNull) {
            warning("not null constraint on virtual elements may not be enforced - virtual elements are computed at runtime",
                element,
                CDSPackage.Literals.ELEMENT__NAME,
                CODE_VIRTUAL_PERSISTED_HINT);
        }

        // Info: unique on virtual element
        boolean hasUnique = element.getConstraints().stream()
            .anyMatch(c -> c instanceof UniqueConstraint);

        if (hasUnique) {
            info("unique constraint on virtual elements is unusual - virtual elements are not persisted",
                element,
                CDSPackage.Literals.ELEMENT__NAME,
                CODE_VIRTUAL_PERSISTED_HINT);
        }
    }

    // ── Phase 11: Localized data validation ──────────────────────────────────

    /**
     * Validates localized element properties.
     * Localized elements are translated fields stored in separate text tables.
     */
    @Check(CheckType.FAST)
    public void checkLocalizedElement(Element element) {
        if (element.getModifier() != ElementModifier.LOCALIZED) return;

        // Error: localized on association
        if (element.getAssoc() != null) {
            error("localized modifier cannot be used on associations - only regular fields can be localized",
                element,
                CDSPackage.Literals.ELEMENT__MODIFIER,
                CODE_LOCALIZED_ON_ASSOCIATION);
            return;
        }

        // Error: localized without type
        if (element.getType() == null) {
            error("localized element must have a type",
                element,
                CDSPackage.Literals.ELEMENT__MODIFIER,
                CODE_LOCALIZED_ON_NON_STRING);
            return;
        }

        // Warning: localized on non-String types
        TypeRef typeRef = element.getType();
        if (typeRef instanceof SimpleTypeRef) {
            SimpleTypeRef simpleTypeRef = (SimpleTypeRef) typeRef;
            if (simpleTypeRef.getRef() != null) {
                String typeName = getName(simpleTypeRef.getRef());
                if (typeName != null &&
                    !typeName.equals("String") &&
                    !typeName.equals("LargeString")) {
                    warning("localized is typically used with String or LargeString types - " +
                            "using it with '" + typeName + "' is unusual",
                        element,
                        CDSPackage.Literals.ELEMENT__MODIFIER,
                        CODE_LOCALIZED_ON_NON_STRING);
                }
            }
        }

        // Info: How localized works
        info("Localized fields are stored in separate text tables - " +
             "SAP CAP will generate a .texts entity for translations",
            element,
            CDSPackage.Literals.ELEMENT__MODIFIER,
            CODE_LOCALIZED_HINT);
    }

    // ── Phase 16: Enhanced Validation ────────────────────────────────────────

    /**
     * Validates that JOIN targets exist and are entities.
     */
    @Check(CheckType.NORMAL)
    public void checkJoinTarget(org.example.cds.cDS.JoinClause join) {
        if (join.getTarget() == null) {
            error("JOIN target must reference an entity",
                join,
                org.example.cds.cDS.CDSPackage.Literals.JOIN_CLAUSE__TARGET,
                CODE_JOIN_UNRESOLVED_TARGET);
            return;
        }

        Definition target = join.getTarget();
        if (!(target instanceof EntityDef)) {
            error("JOIN target must be an entity, but '" + getName(target) + "' is a " + target.eClass().getName(),
                join,
                org.example.cds.cDS.CDSPackage.Literals.JOIN_CLAUSE__TARGET,
                CODE_JOIN_UNRESOLVED_TARGET);
        }
    }

    /**
     * Validates JOIN conditions are valid expressions.
     */
    @Check(CheckType.FAST)
    public void checkJoinCondition(org.example.cds.cDS.JoinClause join) {
        if (join.getCondition() == null) {
            error("JOIN must have an ON condition",
                join,
                org.example.cds.cDS.CDSPackage.Literals.JOIN_CLAUSE__CONDITION,
                CODE_JOIN_INVALID_CONDITION);
        }
    }

    /**
     * Validates circular dependencies in entity relationships.
     */
    @Check(CheckType.NORMAL)
    public void checkCircularDependency(EntityDef entity) {
        Set<EntityDef> visited = new HashSet<>();
        Set<EntityDef> recursionStack = new HashSet<>();

        if (hasCircularDependency(entity, visited, recursionStack)) {
            warning("Entity '" + entity.getName() + "' may have circular dependency through associations - " +
                    "this is valid but may cause runtime issues",
                entity,
                org.example.cds.cDS.CDSPackage.Literals.ENTITY_DEF__NAME,
                CODE_CIRCULAR_DEPENDENCY);
        }
    }

    private boolean hasCircularDependency(EntityDef entity, Set<EntityDef> visited, Set<EntityDef> recursionStack) {
        if (entity == null) return false;
        if (recursionStack.contains(entity)) return true;
        if (visited.contains(entity)) return false;

        visited.add(entity);
        recursionStack.add(entity);

        for (Object member : entity.getMembers()) {
            if (member instanceof Element) {
                Element elem = (Element) member;
                if (elem.getAssoc() != null && elem.getAssoc().getTarget() != null) {
                    EntityDef target = elem.getAssoc().getTarget();
                    if (hasCircularDependency(target, visited, recursionStack)) {
                        recursionStack.remove(entity);
                        return true;
                    }
                }
            }
        }

        recursionStack.remove(entity);
        return false;
    }

    /**
     * Validates conflicting constraints on elements.
     */
    @Check(CheckType.FAST)
    public void checkConflictingConstraints(Element element) {
        boolean hasNotNull = element.getConstraints().stream()
            .anyMatch(c -> c instanceof NotNullConstraint);
        boolean hasDefaultValue = element.getDefaultValue() != null;

        // Info: not null with default is redundant but valid
        if (hasNotNull && hasDefaultValue) {
            info("Element has both 'not null' and a default value - the default ensures non-null so 'not null' is redundant",
                element,
                org.example.cds.cDS.CDSPackage.Literals.ELEMENT__NAME,
                CODE_CONFLICTING_CONSTRAINTS);
        }

        // Warning: virtual with not null is problematic
        if (element.getModifier() == ElementModifier.VIRTUAL && hasNotNull) {
            warning("Virtual elements with 'not null' constraint may cause issues - virtual values are computed at runtime",
                element,
                org.example.cds.cDS.CDSPackage.Literals.ELEMENT__MODIFIER,
                CODE_CONFLICTING_CONSTRAINTS);
        }
    }

    /**
     * Validates aggregation functions are used correctly in SELECT queries.
     */
    @Check(CheckType.FAST)
    public void checkAggregationUsage(org.example.cds.cDS.SelectQuery query) {
        if (query.getGroupBy() == null) {
            // Check if any column uses aggregation
            boolean hasAggregation = query.getColumns().stream()
                .anyMatch(col -> col.getExpression() != null && containsAggregation(col.getExpression()));

            if (hasAggregation) {
                // Check if there are non-aggregated columns
                boolean hasNonAggregated = query.getColumns().stream()
                    .anyMatch(col -> col.getExpression() != null &&
                              !containsAggregation(col.getExpression()) &&
                              !(col.getExpression() instanceof RefExpr));

                if (hasNonAggregated) {
                    info("SELECT uses aggregation functions without GROUP BY - " +
                         "non-aggregated columns should be included in GROUP BY",
                        query,
                        org.example.cds.cDS.CDSPackage.Literals.SELECT_QUERY__COLUMNS,
                        CODE_AGGREGATE_WITHOUT_GROUP);
                }
            }
        }
    }

    private boolean containsAggregation(Expression expr) {
        if (expr == null) return false;
        if (expr instanceof org.example.cds.cDS.AggregationExpr) return true;

        // Check nested expressions
        return expr.eAllContents().hasNext() &&
               StreamSupport.stream(
                   Spliterators.spliteratorUnknownSize(
                       org.eclipse.emf.ecore.util.EcoreUtil.getAllContents(expr, true),
                       0),
                   false)
                   .anyMatch(obj -> obj instanceof org.example.cds.cDS.AggregationExpr);
    }

    /**
     * Validates enum circular inheritance.
     */
    @Check(CheckType.NORMAL)
    public void checkEnumCircularInheritance(EnumDef enumDef) {
        Set<EnumDef> visited = new HashSet<>();
        EnumDef current = enumDef;

        while (current != null && current.getSuperType() != null) {
            if (!visited.add(current)) {
                error("Circular inheritance detected in enum '" + enumDef.getName() + "'",
                    enumDef,
                    org.example.cds.cDS.CDSPackage.Literals.ENUM_DEF__NAME,
                    CODE_CIRCULAR_INHERITANCE);
                return;
            }

            Definition superType = current.getSuperType();
            if (superType instanceof EnumDef) {
                current = (EnumDef) superType;
            } else {
                break;
            }
        }
    }

    // ── Phase 18: Type System ─────────────────────────────────────────────────

    private final ExpressionTypeComputer typeComputer = new ExpressionTypeComputer();

    /**
     * Validates binary expression operand types.
     */
    @Check(CheckType.FAST)
    public void checkBinaryExpressionTypes(BinaryExpr expr) {
        TypeInfo leftType = typeComputer.inferType(expr.getLeft());
        TypeInfo rightType = typeComputer.inferType(expr.getRight());

        if (leftType == null || rightType == null) return;  // Can't determine type

        String op = expr.getOp();
        OperatorRegistry opReg = typeComputer.getOperatorRegistry();
        TypeCompatibilityChecker compat = typeComputer.getCompatibilityChecker();

        // Numeric operators require numeric types
        if (opReg.isNumericOperator(op)) {
            if (!leftType.isNumeric()) {
                error("Operator '" + op + "' requires numeric type, but got " + leftType,
                    expr,
                    CDSPackage.Literals.BINARY_EXPR__LEFT,
                    CODE_TYPE_MISMATCH);
            }
            if (!rightType.isNumeric()) {
                error("Operator '" + op + "' requires numeric type, but got " + rightType,
                    expr,
                    CDSPackage.Literals.BINARY_EXPR__RIGHT,
                    CODE_TYPE_MISMATCH);
            }
            return;
        }

        // Logical operators require boolean types
        if (opReg.isLogicalOperator(op)) {
            if (!leftType.isBoolean()) {
                error("Operator '" + op + "' requires Boolean type, but got " + leftType,
                    expr,
                    CDSPackage.Literals.BINARY_EXPR__LEFT,
                    CODE_TYPE_MISMATCH);
            }
            if (!rightType.isBoolean()) {
                error("Operator '" + op + "' requires Boolean type, but got " + rightType,
                    expr,
                    CDSPackage.Literals.BINARY_EXPR__RIGHT,
                    CODE_TYPE_MISMATCH);
            }
            return;
        }

        // Comparison operators require compatible types
        if (opReg.isComparisonOperator(op)) {
            if (!compat.areCompatible(leftType, rightType)) {
                warning("Comparing incompatible types: " + leftType + " and " + rightType,
                    expr,
                    CDSPackage.Literals.BINARY_EXPR__OP,
                    CODE_INCOMPATIBLE_TYPES);
            }
            return;
        }
    }

    /**
     * Validates unary expression operand types.
     */
    @Check(CheckType.FAST)
    public void checkUnaryExpressionTypes(UnaryExpr expr) {
        TypeInfo operandType = typeComputer.inferType(expr.getOperand());
        if (operandType == null) return;

        String op = expr.getOp();

        // 'not' requires Boolean
        if ("not".equals(op) && !operandType.isBoolean()) {
            error("Operator 'not' requires Boolean type, but got " + operandType,
                expr,
                CDSPackage.Literals.UNARY_EXPR__OPERAND,
                CODE_TYPE_MISMATCH);
        }

        // '-' (negation) requires numeric
        if ("-".equals(op) && !operandType.isNumeric()) {
            error("Operator '-' requires numeric type, but got " + operandType,
                expr,
                CDSPackage.Literals.UNARY_EXPR__OPERAND,
                CODE_TYPE_MISMATCH);
        }
    }

    /**
     * Validates calculated field types.
     */
    @Check(CheckType.FAST)
    public void checkCalculatedFieldType(Element element) {
        // Only for calculated fields (value set, not default)
        Expression value = element.getDefaultValue();
        if (value == null) return;

        // Get element's declared type
        TypeRef declaredTypeRef = element.getType();
        if (declaredTypeRef == null) return;

        // Infer expression type
        TypeInfo exprType = typeComputer.inferType(value);
        if (exprType == null) return;  // Can't determine

        // Get declared type info
        TypeInfo declaredType = createTypeInfoFromRef(declaredTypeRef);
        if (declaredType == null) return;

        // Check compatibility
        if (!typeComputer.getCompatibilityChecker().areCompatible(declaredType, exprType)) {
            warning("Calculated field type mismatch: declared " + declaredType +
                    " but expression evaluates to " + exprType,
                element,
                CDSPackage.Literals.ELEMENT__DEFAULT_VALUE,
                CODE_TYPE_MISMATCH);
        }
    }

    private TypeInfo createTypeInfoFromRef(TypeRef typeRef) {
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
        return null;
    }

    // ── Phase 19: Scope Analysis ──────────────────────────────────────────────

    private final ScopeHelper scopeHelper = new ScopeHelper();

    /**
     * Validates that using directives can resolve their imports.
     * Checks both the file path and the imported definitions.
     */
    @Check(CheckType.NORMAL)
    public void checkUsingStatementResolution(UsingDecl usingDir) {
        // TODO: UsingDecl in current grammar is simpler (just importedNamespace)
        // This validation was written for a different grammar structure
        return;
        /*
        // Check if the import source file can be resolved
        Optional<String> importSource = scopeHelper.getImportSource(usingDir);

        if (importSource.isPresent()) {
            String path = importSource.get();
            org.eclipse.emf.ecore.resource.Resource currentResource = usingDir.eResource();

            if (!scopeHelper.canResolveFile(path, currentResource)) {
                warning("Cannot resolve import source: '" + path + "'",
                    usingDir,
                    CDSPackage.Literals.USING_DECL__IMPORTED_NAMESPACE,
                    CODE_UNRESOLVED_IMPORT);
            }
        }

        // Check if imported definitions are resolved
        for (Definition imported : usingDir.getImports()) {
            if (!scopeHelper.isResolved(imported)) {
                error("Cannot resolve imported definition: '" + getImportedName(usingDir, imported) + "'",
                    usingDir,
                    CDSPackage.Literals.USING_DECL__IMPORTED_NAMESPACE,
                    CODE_UNRESOLVED_IMPORT);
            }
        }
        */
    }

    /**
     * Extracts the name of an imported definition for error reporting.
     * Handles both resolved and unresolved references.
     */
    private String getImportedName(UsingDecl usingDir, Definition imported) {
        String importedName = getName(imported);
        if (imported != null && importedName != null) {
            return importedName;
        }
        // For unresolved references, try to get name from proxy
        return scopeHelper.getUnresolvedReferenceName(imported);
    }

    /**
     * Validates that type references are resolved.
     * Skips built-in types which are always available.
     */
    @Check(CheckType.FAST)
    public void checkTypeReferenceResolution(SimpleTypeRef typeRef) {
        Definition typeDef = typeRef.getRef();

        // Skip if already resolved
        if (scopeHelper.isResolved(typeDef)) {
            return;
        }

        // Get the type name for error reporting
        String typeName = getTypeRefName(typeRef);

        // Skip built-in types - they should be resolved by CDSBuiltInTypeProvider
        // If they're not resolved, it's a provider issue, not a user error
        if (scopeHelper.isBuiltInType(typeName)) {
            return;
        }

        // Report unresolved custom type
        error("Cannot resolve type: '" + typeName + "'",
            typeRef,
            CDSPackage.Literals.SIMPLE_TYPE_REF__REF,
            CODE_UNRESOLVED_TYPE);
    }

    /**
     * Extracts the type name from a type reference for error reporting.
     */
    private String getTypeRefName(SimpleTypeRef typeRef) {
        Definition typeDef = typeRef.getRef();
        String typeDefName = getName(typeDef);
        if (typeDef != null && typeDefName != null) {
            return typeDefName;
        }
        // For unresolved references, try to extract name from proxy
        return scopeHelper.getUnresolvedReferenceName(typeDef);
    }

    /**
     * Validates namespace consistency within a file.
     * Provides informational hints about namespace usage.
     */
    @Check(CheckType.FAST)
    public void checkNamespaceConsistency(CdsFile file) {
        String declaredNamespace = getNamespace(file);

        if (declaredNamespace != null && !declaredNamespace.isEmpty()) {
            // Check if all top-level definitions properly use namespace
            for (Definition def : file.getDefinitions()) {
                String defName = getName(def);
                if (defName != null) {
                    // In CDS, definitions can use short names even with namespace
                    // This is just an informational hint, not an error
                    String fullName = declaredNamespace + "." + defName;
                    if (!defName.equals(fullName) && !defName.startsWith(declaredNamespace + ".")) {
                        info("Definition '" + defName + "' uses short name. " +
                             "Fully qualified name would be: '" + fullName + "'",
                            def,
                            CDSPackage.Literals.ENTITY_DEF__NAME,  // Use specific subtype literal
                            CODE_NAMESPACE_HINT);
                    }
                }
            }
        }
    }

    /**
     * Detects ambiguous imports (same name imported from multiple sources).
     * This can lead to confusion about which definition is being used.
     */
    @Check(CheckType.NORMAL)
    public void checkAmbiguousImports(CdsFile file) {
        // TODO: Current grammar uses simple UsingDecl with importedNamespace
        // This validation was written for a more complex import structure
        return;
        /*
        Map<String, List<UsingDecl>> importsByName = new HashMap<>();

        // Collect all imported definitions by name
        for (UsingDecl usingDir : file.getImports()) {
            // ... rest commented out as UsingDecl doesn't have getImports()
        }
        */
    }

    /**
     * Validates that association targets are resolved.
     * Association targets must reference existing entities.
     */
    @Check(CheckType.FAST)
    public void checkAssociationTargetResolution(AssocDef assoc) {
        Definition target = assoc.getTarget();

        if (!scopeHelper.isResolved(target)) {
            error("Cannot resolve association target: '" + getAssocTargetName(assoc) + "'",
                assoc,
                CDSPackage.Literals.ASSOC_DEF__TARGET,
                CODE_UNRESOLVED_ASSOC);
        }
    }

    /**
     * Extracts the association target name for error reporting.
     */
    private String getAssocTargetName(AssocDef assoc) {
        Definition target = assoc.getTarget();
        String name = getName(target);
        if (target != null && name != null) {
            return name;
        }
        return scopeHelper.getUnresolvedReferenceName(target);
    }

    // ── Phase 20: Foreign Keys ────────────────────────────────────────────────

    private final KeyHelper keyHelper = new KeyHelper();

    /**
     * Validates ON condition type compatibility in associations.
     * Ensures left and right sides of comparisons are type-compatible.
     */
    @Check(CheckType.FAST)
    public void checkOnConditionTypes(AssocDef assoc) {
        // TODO: ON conditions not yet implemented in grammar
        return;
        /*
        Expression onCondition = assoc.getOnCondition();
        if (onCondition == null) {
            // Managed association - no explicit ON condition
            return;
        }

        // Use existing type checker from Phase 18
        // ON conditions should be boolean expressions with compatible types
        if (onCondition instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr) onCondition;
            String op = binExpr.getOp();

            // ON conditions typically use = or other comparison operators
            if (typeComputer.getOperatorRegistry().isComparisonOperator(op)) {
                TypeInfo leftType = typeComputer.inferType(binExpr.getLeft());
                TypeInfo rightType = typeComputer.inferType(binExpr.getRight());

                if (leftType != null && rightType != null) {
                    if (!typeComputer.getCompatibilityChecker().areCompatible(leftType, rightType)) {
                        error("ON condition compares incompatible types: " +
                              leftType + " and " + rightType,
                            assoc,
                            CDSPackage.Literals.ASSOC_DEF__ON_CONDITION,
                            CODE_ON_CONDITION_TYPE_MISMATCH);
                    }
                }
            }
        }
        */
    }

    /**
     * Validates key compatibility for managed associations.
     * Ensures target entities have keys defined.
     */
    @Check(CheckType.NORMAL)
    public void checkAssociationKeyCompatibility(AssocDef assoc) {
        // Only check managed associations (no explicit ON condition)
        if (!keyHelper.isManagedAssociation(assoc)) {
            return;
        }

        Definition target = assoc.getTarget();
        if (!(target instanceof EntityDef)) {
            return;  // Not an entity
        }

        EntityDef targetEntity = (EntityDef) target;

        // Check if target has a key
        if (!keyHelper.hasKey(targetEntity)) {
            warning("Association target '" + targetEntity.getName() +
                    "' has no key defined. Managed association requires target key.",
                assoc,
                CDSPackage.Literals.ASSOC_DEF__TARGET,
                CODE_MISSING_TARGET_KEY);
            return;
        }

        // For managed associations, CDS generates foreign key based on target key
        // Validate that key types are compatible
        int keyCount = keyHelper.getKeyCount(targetEntity);

        if (keyCount > 1) {
            // Composite key - provide info
            info("Association to entity with composite key (" + keyCount + " fields). " +
                 "CDS will generate corresponding foreign key fields.",
                assoc,
                CDSPackage.Literals.ASSOC_DEF__TARGET,
                CODE_COMPOSITE_KEY_INFO);
        }
    }

    /**
     * Gets the entity that contains an element (association).
     */
    private EntityDef getContainingEntity(Element element) {
        org.eclipse.emf.ecore.EObject container = element.eContainer();
        while (container != null) {
            if (container instanceof EntityDef) {
                return (EntityDef) container;
            }
            container = container.eContainer();
        }
        return null;
    }

    /**
     * Validates unmanaged associations have proper ON conditions.
     * Checks that ON condition references valid elements.
     */
    @Check(CheckType.FAST)
    public void checkUnmanagedAssociationOnCondition(AssocDef assoc) {
        // TODO: ON conditions not yet implemented in grammar
        return;
        /*
        Expression onCondition = assoc.getOnCondition();
        if (onCondition == null) {
            // Managed association - OK
            return;
        }

        // Extract references from ON condition
        List<String> refs = keyHelper.extractOnConditionReferences(onCondition);

        if (refs.isEmpty()) {
            warning("ON condition doesn't reference any fields. " +
                    "Expected comparison between source and target keys.",
                assoc,
                CDSPackage.Literals.ASSOC_DEF__ON_CONDITION,
                CODE_EMPTY_ON_CONDITION);
        }

        // Validate that referenced fields can be resolved
        // (This is already handled by Phase 19 scope validation for RefExpr)
        */
    }

    /**
     * Validates bidirectional associations for consistency.
     * If A -> B and B -> A, check that relationships are consistent.
     */
    @Check(CheckType.NORMAL)
    public void checkBacklinkConsistency(AssocDef assoc) {
        // Get target entity
        Definition target = assoc.getTarget();
        if (!(target instanceof EntityDef)) {
            return;
        }

        EntityDef targetEntity = (EntityDef) target;
        EntityDef sourceEntity = getContainingEntity(assoc);
        if (sourceEntity == null) {
            return;
        }

        // Check if target has association back to source
        for (Element elem : getElements(targetEntity)) {
            if (elem instanceof AssocDef) {
                AssocDef backlink = (AssocDef) elem;
                Definition backlinkTarget = backlink.getTarget();

                if (backlinkTarget == sourceEntity) {
                    // Found bidirectional relationship
                    // Check for consistency (both managed or both unmanaged)
                    boolean assocIsManaged = keyHelper.isManagedAssociation(assoc);
                    boolean backlinkIsManaged = keyHelper.isManagedAssociation(backlink);

                    if (assocIsManaged != backlinkIsManaged) {
                        info("Bidirectional association: one side is managed, other is unmanaged. " +
                             "Consider using consistent ON conditions.",
                            assoc,
                            CDSPackage.Literals.ASSOC_DEF__TARGET,
                            CODE_BIDIRECTIONAL_INCONSISTENCY);
                    }
                }
            }
        }
    }

    /**
     * Validates associations to many have proper backlink configuration.
     */
    @Check(CheckType.NORMAL)
    public void checkAssociationToMany(AssocDef assoc) {
        // Check if this is "Association to many"
        boolean isToMany = isMany(assoc);

        if (isToMany) {
            // For "to many", ON condition is recommended for clarity
            if (keyHelper.isManagedAssociation(assoc)) {
                info("Association to many without ON condition. " +
                     "CDS will generate foreign key automatically.",
                    assoc,
                    CDSPackage.Literals.ASSOC_DEF__CARDINALITY,
                    CODE_TO_MANY_WITHOUT_ON);
            }

            // Check if target references back to source
            Definition target = assoc.getTarget();
            if (target instanceof EntityDef) {
                EntityDef targetEntity = (EntityDef) target;
                EntityDef sourceEntity = getContainingEntity(assoc);

                if (sourceEntity != null) {
                    boolean hasBacklink = getElements(targetEntity).stream()
                        .filter(elem -> elem instanceof AssocDef)
                        .map(elem -> (AssocDef) elem)
                        .anyMatch(a -> a.getTarget() == sourceEntity);

                    if (!hasBacklink) {
                        info("Association to many without backlink in target entity. " +
                             "Consider adding association from '" + targetEntity.getName() +
                             "' back to '" + sourceEntity.getName() + "' for bidirectional navigation.",
                            assoc,
                            CDSPackage.Literals.ASSOC_DEF__TARGET,
                            CODE_TO_MANY_NO_BACKLINK);
                    }
                }
            }
        }
    }

    /**
     * Validates that ON conditions use key fields appropriately.
     */
    @Check(CheckType.FAST)
    public void checkOnConditionUsesKeys(AssocDef assoc) {
        // TODO: ON conditions not yet implemented in grammar
        return;
        /*
        Expression onCondition = assoc.getOnCondition();
        if (onCondition == null) {
            return;  // Managed association
        }

        Definition target = assoc.getTarget();
        if (!(target instanceof EntityDef)) {
            return;
        }

        EntityDef targetEntity = (EntityDef) target;
        EntityDef sourceEntity = getContainingEntity(assoc);

        // Extract referenced fields from ON condition
        List<String> refs = keyHelper.extractOnConditionReferences(onCondition);

        // Check if any referenced fields are keys
        boolean referencesTargetKey = false;
        if (keyHelper.hasKey(targetEntity)) {
            for (Element keyElem : keyHelper.getKeyElements(targetEntity)) {
                if (refs.contains(keyElem.getName())) {
                    referencesTargetKey = true;
                    break;
                }
            }
        }

        if (!referencesTargetKey && keyHelper.hasKey(targetEntity)) {
            info("ON condition doesn't reference target entity's key field(s). " +
                 "Target key: " + keyHelper.getKeyDescription(targetEntity),
                assoc,
                CDSPackage.Literals.ASSOC_DEF__ON_CONDITION,
                CODE_COMPOSITE_KEY_INFO);
        }
        */
    }

    // ── Phase 21: Annotations ─────────────────────────────────────────────────

    private final AnnotationHelper annotationHelper = new AnnotationHelper();

    /**
     * Validates that annotations are known or custom (not likely typos).
     * Provides info for unknown standard-looking annotations.
     */
    @Check(CheckType.FAST)
    public void checkAnnotationKnown(Annotation annotation) {
        String name = annotationHelper.getAnnotationName(annotation);
        if (name == null || name.isEmpty()) {
            return;
        }

        // Check if it's a known standard annotation
        if (AnnotationRegistry.isKnownAnnotation(name)) {
            return;  // ✅ Known annotation
        }

        // Check if it looks like a standard annotation (UI.*, Core.*, etc.)
        if (AnnotationRegistry.looksLikeStandardAnnotation(name)) {
            // Looks standard but not in our registry - might be typo or newer annotation
            info("Unknown standard annotation: '@" + name + "'. " +
                 "This might be a typo or an annotation not yet supported.",
                annotation,
                CDSPackage.Literals.ANNOTATION__NAME,
                CODE_UNKNOWN_ANNOTATION);
            return;
        }

        // Custom annotation (not starting with standard prefix) - OK, no warning
        // Users can define custom annotations like @MyApp.customField
    }

    /**
     * Validates annotation value types match expected types.
     * Ensures @readonly gets boolean, @title gets string, etc.
     */
    @Check(CheckType.FAST)
    public void checkAnnotationValueType(Annotation annotation) {
        String name = annotationHelper.getAnnotationName(annotation);
        if (name == null) {
            return;
        }

        // Only validate known annotations
        Optional<AnnotationDefinition> def = AnnotationRegistry.getAnnotation(name);
        if (!def.isPresent()) {
            return;  // Unknown annotation, skip value validation
        }

        AnnotationDefinition annotationDef = def.get();
        AnnotationValue value = annotation.getValue();

        if (value == null) {
            // Some annotations don't require values (e.g., @readonly same as @readonly: true)
            return;
        }

        // Check value type matches expected
        AnnotationDefinition.ValueType expectedType = annotationDef.getValueType();

        if (expectedType == AnnotationDefinition.ValueType.ANY) {
            return;  // Any value type is acceptable
        }

        boolean matches = false;
        String actualTypeName = annotationHelper.getValueTypeName(value);

        switch (expectedType) {
            case BOOLEAN:
                matches = annotationHelper.isBooleanValue(value);
                break;
            case STRING:
                matches = annotationHelper.isStringValue(value);
                break;
            case INTEGER:
            case DECIMAL:
                matches = annotationHelper.isNumberValue(value);
                break;
            case ARRAY:
                matches = annotationHelper.isArrayValue(value);
                break;
            case OBJECT:
                matches = annotationHelper.isObjectValue(value);
                break;
        }

        if (!matches) {
            error("Annotation '@" + name + "' expects " +
                  expectedType.toString().toLowerCase() + " value, " +
                  "but got " + actualTypeName,
                annotation,
                CDSPackage.Literals.ANNOTATION__VALUE,
                CODE_ANNOTATION_VALUE_TYPE);
        }
    }

    /**
     * Validates annotations are applied to correct targets.
     * E.g., @UI.LineItem can only be on entities, not elements.
     */
    @Check(CheckType.FAST)
    public void checkAnnotationTarget(Annotation annotation) {
        String name = annotationHelper.getAnnotationName(annotation);
        if (name == null) {
            return;
        }

        // Only validate known annotations
        Optional<AnnotationDefinition> def = AnnotationRegistry.getAnnotation(name);
        if (!def.isPresent()) {
            return;
        }

        AnnotationDefinition annotationDef = def.get();

        // Get the context where annotation is applied
        org.eclipse.emf.ecore.EObject context = annotation.eContainer();
        AnnotationDefinition.TargetType actualTarget = annotationHelper.getTargetType(context);

        // Check if annotation can be applied to this target
        if (!annotationDef.canApplyTo(actualTarget)) {
            String allowedTargets = annotationDef.getAllowedTargets().stream()
                .map(t -> t.toString().toLowerCase())
                .collect(Collectors.joining(", "));

            error("Annotation '@" + name + "' cannot be applied to " +
                  actualTarget.toString().toLowerCase() + ". " +
                  "Allowed targets: " + allowedTargets,
                annotation,
                CDSPackage.Literals.ANNOTATION__NAME,
                CODE_ANNOTATION_INVALID_TARGET);
        }
    }

    /**
     * Validates deprecated annotations and suggests alternatives.
     */
    @Check(CheckType.NORMAL)
    public void checkDeprecatedAnnotation(Annotation annotation) {
        String name = annotationHelper.getAnnotationName(annotation);
        if (name == null) {
            return;
        }

        Optional<AnnotationDefinition> def = AnnotationRegistry.getAnnotation(name);
        if (def.isPresent() && def.get().isDeprecated()) {
            warning("Annotation '@" + name + "' is deprecated. " +
                    def.get().getDescription(),
                annotation,
                CDSPackage.Literals.ANNOTATION__NAME,
                CODE_ANNOTATION_DEPRECATED);
        }
    }

    // ── Phase 22A: Advanced Projections ─────────────────────────────────────

    private final BuiltInFunctionRegistry functionRegistry = new BuiltInFunctionRegistry();

    /**
     * Validates built-in function calls.
     * Phase 22A: Checks function existence, argument count, and argument types.
     */
    @Check(CheckType.FAST)
    public void checkBuiltInFunctionCall(FuncExpr expr) {
        String funcName = expr.getFunc();
        if (funcName == null) return;

        // Check if function exists
        FunctionDefinition funcDef = functionRegistry.getFunction(funcName);
        if (funcDef == null) {
            // Check if it looks like a standard function (uppercase)
            if (funcName.equals(funcName.toUpperCase())) {
                info("Unknown built-in function: '" + funcName + "'. " +
                     "If this is a custom function, ignore this message. " +
                     "Known functions: " + getSimilarFunctions(funcName),
                    expr,
                    CDSPackage.Literals.FUNC_EXPR__FUNC,
                    CODE_UNKNOWN_FUNCTION);
            }
            return;  // Not a built-in function, might be custom
        }

        // Validate argument count
        int argCount = expr.getArgs().size();
        if (!funcDef.acceptsArgCount(argCount)) {
            String expected = funcDef.isVariadic()
                ? "at least " + funcDef.getMinArgs()
                : funcDef.getMinArgs() == funcDef.getMaxArgs()
                    ? String.valueOf(funcDef.getMinArgs())
                    : funcDef.getMinArgs() + "-" + funcDef.getMaxArgs();
            error("Function '" + funcName + "' expects " + expected +
                  " argument(s), but got " + argCount,
                expr,
                CDSPackage.Literals.FUNC_EXPR__FUNC,
                CODE_FUNCTION_ARG_COUNT);
            return;
        }

        // Validate argument types
        List<ArgType> expectedTypes = funcDef.getArgTypes();
        for (int i = 0; i < argCount && i < expectedTypes.size(); i++) {
            Expression argExpr = expr.getArgs().get(i);
            TypeInfo argType = typeComputer.inferType(argExpr);
            if (argType == null) continue;  // Can't determine type

            ArgType expectedType = expectedTypes.get(i);
            if (!matchesArgType(argType, expectedType)) {
                warning("Function '" + funcName + "' argument " + (i + 1) +
                        " expects " + expectedType + " type, but got " + argType,
                    argExpr,
                    null,
                    CODE_FUNCTION_ARG_TYPE);
            }
        }
    }

    /**
     * Checks if a TypeInfo matches an expected ArgType.
     */
    private boolean matchesArgType(TypeInfo actualType, ArgType expectedType) {
        switch (expectedType) {
            case STRING:
                return actualType.isString();
            case NUMERIC:
                return actualType.isNumeric();
            case TEMPORAL:
                return actualType.isTemporal();
            case BOOLEAN:
                return actualType.isBoolean();
            case ANY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Finds similar function names for suggestions.
     */
    private String getSimilarFunctions(String funcName) {
        Set<String> allFuncs = functionRegistry.getAllFunctionNames();
        String lower = funcName.toLowerCase();

        List<String> similar = allFuncs.stream()
            .filter(name -> name.toLowerCase().startsWith(lower) ||
                            lower.startsWith(name.toLowerCase()))
            .limit(3)
            .collect(Collectors.toList());

        return similar.isEmpty() ? "CONCAT, UPPER, LOWER, SUBSTRING, etc."
                                 : String.join(", ", similar);
    }

    /**
     * Validates that column aliases in SELECT are unique.
     * Phase 22A: Detects duplicate aliases within a SELECT clause.
     */
    @Check(CheckType.FAST)
    public void checkSelectColumnAliasUniqueness(SelectQuery query) {
        if (query.getColumns() == null || query.getColumns().isEmpty()) return;

        Map<String, SelectColumn> aliasMap = new HashMap<>();

        for (SelectColumn column : query.getColumns()) {
            String alias = column.getAlias();
            if (alias == null || alias.isEmpty()) continue;

            if (aliasMap.containsKey(alias)) {
                error("Duplicate column alias: '" + alias + "'. " +
                      "Each column must have a unique alias in the SELECT clause.",
                    column,
                    CDSPackage.Literals.SELECT_COLUMN__ALIAS,
                    CODE_DUPLICATE_COLUMN_ALIAS);

                // Also mark the first occurrence
                SelectColumn firstColumn = aliasMap.get(alias);
                error("Duplicate column alias: '" + alias + "'. " +
                      "First defined here.",
                    firstColumn,
                    CDSPackage.Literals.SELECT_COLUMN__ALIAS,
                    CODE_DUPLICATE_COLUMN_ALIAS);
            } else {
                aliasMap.put(alias, column);
            }
        }
    }

    // ── Phase 22B: Advanced Projections (CASE/CAST/excluding) ───────────────

    /**
     * Validates CASE expressions.
     * Phase 22B: Checks that CASE has at least one WHEN clause and type consistency.
     */
    @Check(CheckType.FAST)
    public void checkCaseExpression(CaseExpr expr) {
        if (expr.getWhenClauses() == null || expr.getWhenClauses().isEmpty()) {
            error("CASE expression must have at least one WHEN clause",
                expr,
                null,
                CODE_CASE_EMPTY);
            return;
        }

        // Check type consistency across all branches
        TypeInfo baseType = null;
        for (int i = 0; i < expr.getWhenClauses().size(); i++) {
            WhenClause whenClause = expr.getWhenClauses().get(i);
            Expression result = whenClause.getResult();
            if (result == null) continue;

            TypeInfo resultType = typeComputer.inferType(result);
            if (resultType == null) continue;

            if (baseType == null) {
                baseType = resultType;
            } else {
                TypeInfo commonType = typeComputer.getCompatibilityChecker()
                    .findCommonType(baseType, resultType);
                if (commonType == null) {
                    warning("CASE WHEN branch " + (i + 1) + " has type " + resultType +
                            " which is incompatible with previous branch type " + baseType,
                        result,
                        null,
                        CODE_CASE_TYPE_MISMATCH);
                } else {
                    baseType = commonType;
                }
            }
        }

        // Check ELSE clause type compatibility
        if (expr.getElseExpr() != null && baseType != null) {
            TypeInfo elseType = typeComputer.inferType(expr.getElseExpr());
            if (elseType != null) {
                TypeInfo commonType = typeComputer.getCompatibilityChecker()
                    .findCommonType(baseType, elseType);
                if (commonType == null) {
                    warning("CASE ELSE branch has type " + elseType +
                            " which is incompatible with WHEN branch types " + baseType,
                        expr.getElseExpr(),
                        null,
                        CODE_CASE_TYPE_MISMATCH);
                }
            }
        }
    }

    /**
     * Validates CAST expressions.
     * Phase 22B: Checks that target type is valid.
     */
    @Check(CheckType.FAST)
    public void checkCastExpression(CastExpr expr) {
        TypeRef targetType = expr.getTargetType();
        if (targetType == null) {
            error("CAST expression must specify a target type",
                expr,
                null,
                CODE_CAST_INVALID_TARGET);
            return;
        }

        // Check that target type is resolvable
        if (targetType instanceof SimpleTypeRef) {
            SimpleTypeRef simpleRef = (SimpleTypeRef) targetType;
            if (simpleRef.getRef() == null || simpleRef.getRef().eIsProxy()) {
                error("CAST target type '" + simpleRef.getRef() + "' cannot be resolved",
                    expr,
                    CDSPackage.Literals.CAST_EXPR__TARGET_TYPE,
                    CODE_CAST_INVALID_TARGET);
            }
        }
    }

    /**
     * Validates excluding clause in SELECT.
     * Phase 22B: Checks that excluded fields exist in source entity.
     */
    @Check(CheckType.FAST)
    public void checkExcludingClause(ExcludingClause excluding) {
        if (excluding == null || excluding.getFields() == null) return;

        // Get the parent SELECT query
        org.eclipse.emf.ecore.EObject container = excluding.eContainer();
        if (!(container instanceof SelectQuery)) return;

        SelectQuery query = (SelectQuery) container;
        Definition source = query.getFrom();
        if (source == null || source.eIsProxy()) return;

        // Check that all excluded fields exist in the source entity
        for (Element field : excluding.getFields()) {
            if (field == null || field.eIsProxy()) {
                error("Excluded field cannot be resolved",
                    excluding,
                    null,
                    CODE_EXCLUDING_UNRESOLVED);
            }
        }

        // Check that excluding is used with * (selectAll)
        if (!query.isSelectAll()) {
            warning("'excluding' clause should be used with SELECT * to exclude fields. " +
                    "Using it with explicit columns has no effect.",
                excluding,
                null,
                CODE_EXCLUDING_WITH_COLUMNS);
        }
    }

    // ── Phase 23: Subqueries, COALESCE, EXISTS ──────────────────────────────

    /**
     * Validates COALESCE expressions.
     * Phase 23: Checks that COALESCE has at least 2 arguments and type consistency.
     */
    @Check(CheckType.FAST)
    public void checkCoalesceExpression(CoalesceExpr expr) {
        if (expr.getExpressions() == null || expr.getExpressions().size() < 2) {
            error("COALESCE must have at least 2 arguments",
                expr,
                null,
                CODE_COALESCE_EMPTY);
            return;
        }

        // Check type consistency across all arguments
        TypeInfo baseType = null;
        for (int i = 0; i < expr.getExpressions().size(); i++) {
            Expression arg = expr.getExpressions().get(i);
            if (arg == null) continue;

            TypeInfo argType = typeComputer.inferType(arg);
            if (argType == null) continue;

            if (baseType == null) {
                baseType = argType;
            } else {
                TypeInfo commonType = typeComputer.getCompatibilityChecker()
                    .findCommonType(baseType, argType);
                if (commonType == null) {
                    warning("COALESCE argument " + (i + 1) + " has type " + argType +
                            " which is incompatible with previous argument types " + baseType,
                        arg,
                        null,
                        CODE_COALESCE_TYPE_MISMATCH);
                } else {
                    baseType = commonType;
                }
            }
        }
    }

    /**
     * Validates EXISTS expressions.
     * Phase 23: Checks that EXISTS has a valid subquery.
     */
    @Check(CheckType.FAST)
    public void checkExistsExpression(ExistsExpr expr) {
        SelectQuery subquery = expr.getSubquery();
        if (subquery == null) {
            error((expr.isNotExists() ? "NOT EXISTS" : "EXISTS") +
                  " must contain a subquery",
                expr,
                null,
                CODE_EXISTS_EMPTY);
            return;
        }

        // Validate the subquery has columns
        if (subquery.getColumns() == null || subquery.getColumns().isEmpty()) {
            error("Subquery in " + (expr.isNotExists() ? "NOT EXISTS" : "EXISTS") +
                  " must have at least one column",
                expr,
                null,
                CODE_SUBQUERY_EMPTY);
        }
    }

    /**
     * Validates subquery expressions.
     * Phase 23: Checks that subquery returns exactly one column.
     */
    @Check(CheckType.FAST)
    public void checkSubqueryExpression(SubqueryExpr expr) {
        SelectQuery subquery = expr.getSubquery();
        if (subquery == null) {
            error("Subquery expression must contain a valid SELECT query",
                expr,
                null,
                CODE_SUBQUERY_EMPTY);
            return;
        }

        // Check that subquery returns exactly one column
        if (subquery.getColumns() == null || subquery.getColumns().isEmpty()) {
            error("Subquery must return at least one column",
                expr,
                null,
                CODE_SUBQUERY_EMPTY);
        } else if (subquery.getColumns().size() > 1) {
            warning("Subquery returns multiple columns but only the first will be used. " +
                    "Consider selecting only one column.",
                expr,
                null,
                CODE_SUBQUERY_MULTIPLE_COLUMNS);
        }
    }

    /**
     * Validates IN expressions with subqueries.
     * Phase 23: Checks that IN uses either values or subquery, not both.
     */
    @Check(CheckType.FAST)
    public void checkInExpression(InExpr expr) {
        boolean hasValues = expr.getValues() != null && !expr.getValues().isEmpty();
        boolean hasSubquery = expr.getSubquery() != null;

        if (hasValues && hasSubquery) {
            error("IN expression cannot have both values and subquery",
                expr,
                null,
                CODE_IN_SUBQUERY_MIXED);
            return;
        }

        // If using subquery, validate it
        if (hasSubquery) {
            SelectQuery subquery = expr.getSubquery();
            if (subquery.getColumns() == null || subquery.getColumns().isEmpty()) {
                error("Subquery in IN expression must return at least one column",
                    expr,
                    null,
                    CODE_SUBQUERY_EMPTY);
            } else if (subquery.getColumns().size() > 1) {
                warning("Subquery in IN expression returns multiple columns but only the first will be used",
                    expr,
                    null,
                    CODE_SUBQUERY_MULTIPLE_COLUMNS);
            }
        }
    }
}
