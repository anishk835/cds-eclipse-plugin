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
 *   - TypeRef.ref           → all visible Definitions (entity, type, aspect, built-ins)
 *   - AssocDef.target       → EntityDef only
 *   - ServiceEntity.source  → EntityDef only
 *   - ProjectedElement.ref  → Elements within the source entity
 *   - ExtendDef.target      → EntityDef only
 *   - AnnotateDef.target    → all Definitions
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

        if (reference == CDSPackage.Literals.SERVICE_ENTITY__SOURCE) {
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
        if (serviceEntity == null || serviceEntity.getSource() == null
                || serviceEntity.getSource().eIsProxy()) {
            return IScope.NULLSCOPE;
        }
        List<Element> elements = getElements(serviceEntity.getSource());
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
}
