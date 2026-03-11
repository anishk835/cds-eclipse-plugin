package org.example.cds.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.Definition;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.ServiceDef;
import org.example.cds.cDS.ServiceEntity;
import org.example.cds.cDS.ViewDef;

/**
 * Provides qualified names for CDS elements, supporting:
 * - Top-level definitions: MyEntity
 * - Nested service entities: MyService.MyEntity
 * - Namespace-qualified: my.namespace.MyEntity
 */
public class CDSQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {

    /**
     * Compute qualified name for ServiceEntity (nested in Service).
     * Returns: ServiceName.EntityName
     */
    public QualifiedName qualifiedName(ServiceEntity entity) {
        if (entity.getName() == null) return null;

        // Get parent service
        EObject parent = entity.eContainer();
        if (parent instanceof ServiceDef service) {
            String serviceName = service.getName();
            if (serviceName != null) {
                return QualifiedName.create(serviceName, entity.getName());
            }
        }

        return QualifiedName.create(entity.getName());
    }

    /**
     * Compute qualified name for top-level Definition.
     * Returns namespace.DefinitionName if namespace exists, otherwise just DefinitionName.
     */
    public QualifiedName qualifiedName(Definition def) {
        String name = getDefinitionName(def);
        if (name == null) return null;

        // Check for namespace
        CdsFile file = getContainingFile(def);
        if (file != null && file.getNamespaceDecl() != null) {
            String namespace = file.getNamespaceDecl().getName();
            if (namespace != null && !namespace.isEmpty()) {
                return QualifiedName.create(namespace).append(name);
            }
        }

        return QualifiedName.create(name);
    }

    /**
     * Helper to extract name from Definition subtypes.
     */
    private String getDefinitionName(Definition def) {
        if (def instanceof EntityDef entity) return entity.getName();
        if (def instanceof ViewDef view) return view.getName();
        if (def instanceof org.example.cds.cDS.TypeDef type) return type.getName();
        if (def instanceof org.example.cds.cDS.EnumDef enumDef) return enumDef.getName();
        if (def instanceof ServiceDef service) return service.getName();
        if (def instanceof org.example.cds.cDS.AspectDef aspect) return aspect.getName();
        return null;
    }

    /**
     * Get the containing CdsFile.
     */
    private CdsFile getContainingFile(EObject obj) {
        EObject current = obj;
        while (current != null) {
            if (current instanceof CdsFile) {
                return (CdsFile) current;
            }
            current = current.eContainer();
        }
        return null;
    }
}
