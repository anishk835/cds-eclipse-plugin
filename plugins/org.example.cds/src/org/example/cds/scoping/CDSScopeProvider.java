package org.example.cds.scoping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.example.cds.cDS.AspectDef;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.CDSPackage;
import org.example.cds.cDS.Definition;
import org.example.cds.cDS.Element;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.EnumDef;
import org.example.cds.cDS.ServiceDef;
import org.example.cds.cDS.SimpleTypeRef;
import org.example.cds.cDS.TypeDef;
import org.example.cds.cDS.TypeRef;
import org.example.cds.cDS.ViewDef;

import com.google.inject.Inject;

/**
 * Custom scope provider for CDS cross-references.
 *
 * Handles:
 *   - TypeRef.ref              → all visible Definitions (entity, type, aspect, built-ins)
 *   - AssocDef.target          → EntityDef only
 *   - ServiceEntity.source     → EntityDef only
 *   - SelectQuery.from         → EntityDef and ViewDef (for SELECT FROM navigation)
 *   - JoinClause.target        → EntityDef and ViewDef (for JOIN navigation)
 *   - TypeDef.projectionSource → EntityDef, TypeDef, ViewDef (for type-as-projection)
 *   - TypeProjectionField.ref  → Elements within the projection source
 *   - ProjectedElement.ref     → Elements within the source entity
 *   - RefExpr.ref              → Elements in local scope
 *   - ExtendDef.target         → EntityDef only
 *   - AnnotateDef.target       → all Definitions
 *   - EnumDef.superType        → EnumDef and built-in types
 */
public class CDSScopeProvider extends AbstractCDSScopeProvider {

    @Inject
    private CDSBuiltInTypeProvider builtInTypes;

    // ── TypeRef.ref ─────────────────────────────────────────────────────────

    @Override
    public IScope getScope(EObject context, EReference reference) {

        if (reference == CDSPackage.Literals.SIMPLE_TYPE_REF__REF) {
            return scopeForDefinitions(context,
                EntityDef.class, TypeDef.class, AspectDef.class, EnumDef.class);
        }

        if (reference == CDSPackage.Literals.ASSOC_DEF__TARGET) {
            return scopeForDefinitions(context, EntityDef.class);
        }

        if (reference == CDSPackage.Literals.SERVICE_ENTITY_BODY__SOURCE) {
            return scopeForDefinitions(context, EntityDef.class);
        }

        if (reference == CDSPackage.Literals.EXTEND_DEF__TARGET) {
            return scopeForDefinitions(context, EntityDef.class);
        }

        if (reference == CDSPackage.Literals.ANNOTATE_DEF__TARGET) {
            return scopeForDefinitions(context,
                EntityDef.class, TypeDef.class, AspectDef.class, EnumDef.class);
        }

        if (reference == CDSPackage.Literals.PROJECTED_ELEMENT__REF) {
            return scopeForProjectedElements(context);
        }

        if (reference == CDSPackage.Literals.REF_EXPR__REF) {
            return scopeForLocalElements(context);
        }

        // Navigation for SELECT FROM queries
        if (reference == CDSPackage.Literals.SELECT_QUERY__FROM) {
            return scopeForDefinitions(context, EntityDef.class, ViewDef.class);
        }

        // Navigation for JOIN clauses
        if (reference == CDSPackage.Literals.JOIN_CLAUSE__TARGET) {
            return scopeForDefinitions(context, EntityDef.class, ViewDef.class);
        }

        // Navigation for type-as-projection syntax
        if (reference == CDSPackage.Literals.TYPE_DEF__PROJECTION_SOURCE) {
            return scopeForDefinitions(context,
                EntityDef.class, TypeDef.class, ViewDef.class);
        }

        // Navigation for projected fields in type-as-projection
        if (reference == CDSPackage.Literals.TYPE_PROJECTION_FIELD__REF) {
            return scopeForTypeProjectionFields(context);
        }

        // Navigation for "redirected to" in SELECT columns (service entities)
        if (reference == CDSPackage.Literals.SELECT_COLUMN__REDIRECT_TARGET) {
            return scopeForRedirectedEntities(context);
        }

        // Navigation for "redirected to" in SELECT fields (service entity body)
        if (reference == CDSPackage.Literals.SELECT_FIELD__TARGET) {
            return scopeForRedirectedEntities(context);
        }

        // TODO: EnumRef type doesn't exist in generated AST
        // if (reference == CDSPackage.Literals.ENUM_REF__VALUE) {
        //     return scopeForEnumValues(context);
        // }

        if (reference == CDSPackage.Literals.ENUM_DEF__SUPER_TYPE) {
            return scopeForEnumSuperType(context);
        }

        return super.getScope(context, reference);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Safely gets the name of a Definition.
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
     * Extracts Elements from EntityDef members.
     */
    private List<Element> getElements(EntityDef entity) {
        return entity.getMembers().stream()
            .filter(m -> m instanceof Element)
            .map(m -> (Element) m)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Builds a scope containing all Definitions of the requested types,
     * collected from the local file first, then the workspace index,
     * then built-in primitives.
     */
    @SafeVarargs
    private IScope scopeForDefinitions(EObject context,
            Class<? extends Definition>... types) {

        CdsFile file = EcoreUtil2.getContainerOfType(context, CdsFile.class);
        List<IEObjectDescription> local = new ArrayList<>();

        if (file != null) {
            for (Definition def : file.getDefinitions()) {
                for (Class<? extends Definition> type : types) {
                    if (type.isInstance(def)) {
                        local.add(org.eclipse.xtext.resource.EObjectDescription
                            .create(QualifiedName.create(getName(def)), def));
                        break;
                    }
                }
            }
        }

        // Workspace-global scope (populated by CDSResourceDescriptionStrategy)
        IScope globalScope = super.getScope(context,
            CDSPackage.Literals.SIMPLE_TYPE_REF__REF);

        // Built-in types as outermost fallback scope
        IScope builtInScope = builtInTypes.getBuiltInScope(globalScope);

        return new SimpleScope(new SimpleScope(builtInScope, local), List.of());
    }

    /**
     * Scope for projected elements — only Elements from the source entity.
     */
    private IScope scopeForProjectedElements(EObject context) {
        var serviceEntity = EcoreUtil2.getContainerOfType(
            context, org.example.cds.cDS.ServiceEntity.class);
        if (serviceEntity == null || serviceEntity.getEntityBody() == null
                || serviceEntity.getEntityBody().getSource() == null
                || serviceEntity.getEntityBody().getSource().eIsProxy()) {
            return IScope.NULLSCOPE;
        }
        List<Element> elements = getElements(serviceEntity.getEntityBody().getSource());
        return Scopes.scopeFor(elements);
    }

    /**
     * Scope for expression RefExpr — local elements in the containing entity.
     */
    private IScope scopeForLocalElements(EObject context) {
        EntityDef entity = EcoreUtil2.getContainerOfType(context, EntityDef.class);
        if (entity == null) return IScope.NULLSCOPE;
        return Scopes.scopeFor(getElements(entity));
    }

    /**
     * Scope for enum value references (#EnumValue).
     * Provides enum values from the containing element's type if it's an enum.
     */
    private IScope scopeForEnumValues(EObject context) {
        // Find the containing Element
        Element element = EcoreUtil2.getContainerOfType(context, Element.class);
        if (element == null || element.getType() == null) {
            return IScope.NULLSCOPE;
        }

        // Resolve the element's type
        TypeRef typeRef = element.getType();
        if (!(typeRef instanceof SimpleTypeRef)) {
            return IScope.NULLSCOPE;
        }

        SimpleTypeRef simpleTypeRef = (SimpleTypeRef) typeRef;
        if (simpleTypeRef.getRef() == null || simpleTypeRef.getRef().eIsProxy()) {
            return IScope.NULLSCOPE;
        }

        Definition typeDef = simpleTypeRef.getRef();

        // If the type is an enum, provide its values
        if (typeDef instanceof EnumDef enumDef) {
            return Scopes.scopeFor(enumDef.getValues());
        }

        return IScope.NULLSCOPE;
    }

    /**
     * Scope for enum superType.
     * Provides both built-in types (String, Integer) and other EnumDef instances.
     */
    private IScope scopeForEnumSuperType(EObject context) {
        // Include built-in types and other enum definitions
        return scopeForDefinitions(context, EnumDef.class);
    }

    /**
     * Scope for type projection fields (type-as-projection).
     * Returns elements from the projection source entity/type.
     */
    private IScope scopeForTypeProjectionFields(EObject context) {
        TypeDef typeDef = EcoreUtil2.getContainerOfType(context, TypeDef.class);
        if (typeDef == null || typeDef.getProjectionSource() == null
                || typeDef.getProjectionSource().eIsProxy()) {
            return IScope.NULLSCOPE;
        }

        Definition source = typeDef.getProjectionSource();

        // If source is an EntityDef, return its elements
        if (source instanceof EntityDef entity) {
            return Scopes.scopeFor(getElements(entity));
        }

        // If source is a TypeDef with structured type, return its elements
        if (source instanceof TypeDef sourceType && sourceType.getType() != null) {
            TypeRef typeRef = sourceType.getType();
            if (typeRef instanceof org.example.cds.cDS.StructuredTypeRef structType) {
                return Scopes.scopeFor(structType.getElements());
            }
        }

        return IScope.NULLSCOPE;
    }

    /**
     * Scope for "redirected to" targets in SELECT columns.
     * Provides both:
     * - Service entities within the same service
     * - Global entity definitions (EntityDef, ViewDef)
     */
    private IScope scopeForRedirectedEntities(EObject context) {
        List<EObject> targets = new ArrayList<>();

        // Find containing service
        ServiceDef service = EcoreUtil2.getContainerOfType(context, ServiceDef.class);
        if (service != null) {
            // Add all service entities from the same service
            targets.addAll(service.getMembers().stream()
                .filter(m -> m instanceof org.example.cds.cDS.ServiceEntity)
                .toList());
        }

        // Add global entity and view definitions
        CdsFile file = EcoreUtil2.getContainerOfType(context, CdsFile.class);
        if (file != null) {
            for (Definition def : file.getDefinitions()) {
                if (def instanceof EntityDef || def instanceof ViewDef) {
                    targets.add(def);
                }
            }
        }

        return Scopes.scopeFor(targets);
    }
}
