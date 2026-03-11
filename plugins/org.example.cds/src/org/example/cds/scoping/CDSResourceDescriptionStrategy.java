package org.example.cds.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescriptionStrategy;
import org.eclipse.xtext.util.IAcceptor;
import org.example.cds.cDS.Definition;
import org.example.cds.cDS.NamespaceDecl;
import org.example.cds.cDS.CdsFile;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Exports named CDS definitions to the global workspace index so that
 * other .cds files can reference them through cross-file scoping.
 *
 * Both the simple name and the fully qualified name (namespace.Name)
 * are exported so that both 'using ns.*' and qualified references work.
 */
@Singleton
public class CDSResourceDescriptionStrategy
        extends DefaultResourceDescriptionStrategy {

    @Inject
    private IQualifiedNameProvider nameProvider;

    @Override
    public boolean createEObjectDescriptions(EObject obj,
            IAcceptor<IEObjectDescription> acceptor) {

        if (obj instanceof Definition def) {
            String simpleName = getDefinitionName(def);
            if (simpleName == null || simpleName.isBlank()) return true;

            // Simple name export
            acceptor.accept(EObjectDescription.create(
                QualifiedName.create(simpleName), def));

            // Fully qualified name export (namespace.DefinitionName)
            QualifiedName fqn = nameProvider.getFullyQualifiedName(def);
            if (fqn != null && !fqn.equals(QualifiedName.create(simpleName))) {
                acceptor.accept(EObjectDescription.create(fqn, def));
            }

            return true; // recurse into children (e.g. ServiceEntity)
        }

        // Export ServiceEntity members with qualified names (Service.Entity)
        if (obj instanceof org.example.cds.cDS.ServiceEntity serviceEntity) {
            String name = serviceEntity.getName();
            if (name != null && !name.isBlank()) {
                // Simple name export
                acceptor.accept(EObjectDescription.create(
                    QualifiedName.create(name), serviceEntity));

                // Qualified name export (Service.Entity)
                QualifiedName fqn = nameProvider.getFullyQualifiedName(serviceEntity);
                if (fqn != null && !fqn.equals(QualifiedName.create(name))) {
                    acceptor.accept(EObjectDescription.create(fqn, serviceEntity));
                }
            }
            return false; // Don't recurse into ServiceEntity children
        }

        // Don't export Elements, TypeRefs, etc. — too noisy
        if (obj instanceof CdsFile || obj instanceof NamespaceDecl) {
            return true;
        }

        return false;
    }

    /**
     * Helper to extract name from Definition subtypes.
     */
    private String getDefinitionName(Definition def) {
        if (def instanceof org.example.cds.cDS.EntityDef entity) return entity.getName();
        if (def instanceof org.example.cds.cDS.ViewDef view) return view.getName();
        if (def instanceof org.example.cds.cDS.TypeDef type) return type.getName();
        if (def instanceof org.example.cds.cDS.EnumDef enumDef) return enumDef.getName();
        if (def instanceof org.example.cds.cDS.ServiceDef service) return service.getName();
        if (def instanceof org.example.cds.cDS.AspectDef aspect) return aspect.getName();
        // ExtendDef and AnnotateDef don't have names - they reference existing definitions
        return null;
    }
}
