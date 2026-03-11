package org.example.cds.validation;

import org.example.cds.cDS.*;
import org.example.cds.typing.TypeCompatibilityChecker;
import org.example.cds.typing.TypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper methods for foreign key and association validation.
 * Used by validators to check key compatibility and ON condition correctness.
 */
public class KeyHelper {

    // Helper methods for AST compatibility
    private List<Element> getElements(EntityDef entity) {
        return entity.getMembers().stream()
            .filter(m -> m instanceof Element)
            .map(m -> (Element) m)
            .collect(Collectors.toList());
    }

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
     * Extracts all key elements from an entity.
     * Key elements are marked with the 'key' modifier.
     */
    public List<Element> getKeyElements(EntityDef entity) {
        if (entity == null) {
            return new ArrayList<>();
        }

        return getElements(entity).stream()
            .filter(elem -> elem.getModifier() == ElementModifier.KEY)
            .collect(Collectors.toList());
    }

    /**
     * Checks if an entity has at least one key defined.
     */
    public boolean hasKey(EntityDef entity) {
        return !getKeyElements(entity).isEmpty();
    }

    /**
     * Gets the primary key type for single-key entities.
     * Returns null if the entity has a composite key or no key.
     */
    public TypeRef getPrimaryKeyType(EntityDef entity) {
        List<Element> keys = getKeyElements(entity);
        if (keys.size() == 1) {
            return keys.get(0).getType();
        }
        return null;  // Composite key or no key
    }

    /**
     * Gets the number of key fields in an entity.
     * Useful for checking composite key size.
     */
    public int getKeyCount(EntityDef entity) {
        return getKeyElements(entity).size();
    }

    /**
     * Checks if two entities have compatible keys for foreign key relationship.
     *
     * Compatible means:
     * - Both have keys defined
     * - Same number of key fields (for composite keys)
     * - Corresponding key types are compatible
     */
    public boolean areKeysCompatible(EntityDef source, EntityDef target,
                                     TypeCompatibilityChecker typeChecker) {
        if (source == null || target == null) {
            return false;
        }

        List<Element> sourceKeys = getKeyElements(source);
        List<Element> targetKeys = getKeyElements(target);

        // Both must have keys
        if (sourceKeys.isEmpty() || targetKeys.isEmpty()) {
            return false;
        }

        // Must have same number of key fields
        if (sourceKeys.size() != targetKeys.size()) {
            return false;
        }

        // Check type compatibility for each key field
        for (int i = 0; i < sourceKeys.size(); i++) {
            TypeRef sourceType = sourceKeys.get(i).getType();
            TypeRef targetType = targetKeys.get(i).getType();

            if (sourceType == null || targetType == null) {
                return false;
            }

            // Convert to TypeInfo and check compatibility
            TypeInfo sourceTypeInfo = createTypeInfo(sourceType);
            TypeInfo targetTypeInfo = createTypeInfo(targetType);

            if (sourceTypeInfo == null || targetTypeInfo == null) {
                return false;
            }

            if (!typeChecker.areCompatible(sourceTypeInfo, targetTypeInfo)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extracts element references from ON condition expression.
     * Returns list of element names referenced in the condition.
     *
     * Example: "author.ID = ID" → ["ID", "ID"]
     */
    public List<String> extractOnConditionReferences(Expression expr) {
        List<String> refs = new ArrayList<>();
        if (expr == null) {
            return refs;
        }

        // Traverse expression tree and collect RefExpr names
        collectReferences(expr, refs);
        return refs;
    }

    /**
     * Recursively collects element references from an expression.
     */
    private void collectReferences(Expression expr, List<String> refs) {
        if (expr instanceof RefExpr) {
            RefExpr refExpr = (RefExpr) expr;
            if (refExpr.getRef() != null && refExpr.getRef().getName() != null) {
                refs.add(refExpr.getRef().getName());
            }
        }

        if (expr instanceof BinaryExpr) {
            BinaryExpr binExpr = (BinaryExpr) expr;
            collectReferences(binExpr.getLeft(), refs);
            collectReferences(binExpr.getRight(), refs);
        }

        if (expr instanceof UnaryExpr) {
            UnaryExpr unExpr = (UnaryExpr) expr;
            collectReferences(unExpr.getOperand(), refs);
        }
    }

    /**
     * Checks if an association is managed (no explicit ON condition).
     * Managed associations let CDS runtime generate the foreign key automatically.
     * TODO: ON conditions not in grammar yet
     */
    public boolean isManagedAssociation(AssocDef assoc) {
        return assoc != null; // All associations are managed for now
        // return assoc != null && assoc.getOnCondition() == null;
    }

    /**
     * Checks if an association is unmanaged (has explicit ON condition).
     * TODO: ON conditions not in grammar yet
     */
    public boolean isUnmanagedAssociation(AssocDef assoc) {
        return false; // No unmanaged associations for now
        // return assoc != null && assoc.getOnCondition() != null;
    }

    /**
     * Checks if an entity has a composite key (more than one key field).
     */
    public boolean hasCompositeKey(EntityDef entity) {
        return getKeyCount(entity) > 1;
    }

    /**
     * Gets a human-readable description of entity keys.
     * Example: "UUID" or "UUID, String" for composite keys.
     */
    public String getKeyDescription(EntityDef entity) {
        List<Element> keys = getKeyElements(entity);
        if (keys.isEmpty()) {
            return "no key";
        }

        return keys.stream()
            .map(elem -> {
                TypeRef type = elem.getType();
                if (type instanceof SimpleTypeRef) {
                    SimpleTypeRef simpleRef = (SimpleTypeRef) type;
                    Definition typeDef = simpleRef.getRef();
                    String typeDefName = getName(typeDef);
                    if (typeDef != null && typeDefName != null) {
                        return typeDefName;
                    }
                }
                return "unknown";
            })
            .collect(Collectors.joining(", "));
    }

    /**
     * Creates TypeInfo from TypeRef for compatibility checking.
     * Used internally to convert between type representations.
     */
    private TypeInfo createTypeInfo(TypeRef typeRef) {
        if (typeRef instanceof SimpleTypeRef) {
            SimpleTypeRef simpleRef = (SimpleTypeRef) typeRef;
            Definition typeDef = simpleRef.getRef();
            if (typeDef == null || typeDef.eIsProxy()) {
                return null;
            }
            return new TypeInfo(typeDef, false);
        }

        if (typeRef instanceof ArrayTypeRef) {
            ArrayTypeRef arrayRef = (ArrayTypeRef) typeRef;
            SimpleTypeRef elementType = arrayRef.getElementType();
            if (elementType == null) {
                return null;
            }
            Definition typeDef = elementType.getRef();
            if (typeDef == null || typeDef.eIsProxy()) {
                return null;
            }
            return new TypeInfo(typeDef, true);
        }

        return null;
    }

    /**
     * Checks if an element is a key field.
     */
    public boolean isKeyElement(Element element) {
        if (element == null || element.getModifier() == null) {
            return false;
        }

        return element.getModifier() == ElementModifier.KEY;
    }

    /**
     * Finds a key element by name in an entity.
     * Returns null if not found or not a key.
     */
    public Element findKeyElement(EntityDef entity, String name) {
        if (entity == null || name == null) {
            return null;
        }

        return getKeyElements(entity).stream()
            .filter(elem -> name.equals(elem.getName()))
            .findFirst()
            .orElse(null);
    }
}
