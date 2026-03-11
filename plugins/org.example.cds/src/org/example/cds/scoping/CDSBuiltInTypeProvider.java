package org.example.cds.scoping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.example.cds.cDS.CDSFactory;
import org.example.cds.cDS.TypeDef;

/**
 * Provides SAP CAP built-in primitive types as a synthetic scope,
 * so they resolve without an explicit 'using' import.
 *
 * Covered types mirror the SAP CDS 7.x spec:
 *   Scalar: UUID, Boolean, Integer, Integer64, UInt8, Int16,
 *           Decimal, Double, Date, Time, DateTime, Timestamp,
 *           String, LargeString, Binary, LargeBinary, Map
 */
public class CDSBuiltInTypeProvider {

    private static final String[] BUILT_IN_NAMES = {
        "UUID", "Boolean", "Integer", "Integer64", "UInt8", "Int16",
        "Decimal", "Double", "Date", "Time",
        "DateTime", "Timestamp", "String", "LargeString",
        "Binary", "LargeBinary", "Map"
    };

    private final List<IEObjectDescription> builtInDescriptions;

    public CDSBuiltInTypeProvider() {
        builtInDescriptions = new ArrayList<>();
        // Synthetic resource so EMF doesn't complain about uncontained objects
        ResourceImpl syntheticResource = new ResourceImpl();

        for (String name : BUILT_IN_NAMES) {
            TypeDef typedef = CDSFactory.eINSTANCE.createTypeDef();
            typedef.setName(name);
            syntheticResource.getContents().add(typedef);
            builtInDescriptions.add(
                EObjectDescription.create(QualifiedName.create(name), typedef));
        }
    }

    /**
     * Wraps the provided outer scope with a scope containing all built-in types.
     */
    public IScope getBuiltInScope(IScope outerScope) {
        return new SimpleScope(outerScope, builtInDescriptions);
    }
}
