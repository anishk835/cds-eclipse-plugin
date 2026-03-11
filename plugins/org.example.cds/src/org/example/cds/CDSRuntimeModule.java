package org.example.cds;

import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.example.cds.scoping.CDSScopeProvider;
import org.example.cds.scoping.CDSResourceDescriptionStrategy;
import org.example.cds.scoping.CDSQualifiedNameProvider;
import com.google.inject.Binder;

/**
 * Runtime Guice module.
 * Binds custom scoping, indexing and formatting implementations.
 * Generated stub — safe to edit; never overwritten by MWE2.
 */
public class CDSRuntimeModule extends AbstractCDSRuntimeModule {

    /** Cross-reference resolution and namespace scoping. */
    @Override
    public Class<? extends IScopeProvider> bindIScopeProvider() {
        return CDSScopeProvider.class;
    }

    /** Controls which objects are exported to the workspace index. */
    public Class<? extends IDefaultResourceDescriptionStrategy>
            bindIDefaultResourceDescriptionStrategy() {
        return CDSResourceDescriptionStrategy.class;
    }

    /** Provides qualified names for nested elements (e.g., Service.Entity). */
    @Override
    public Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {
        return CDSQualifiedNameProvider.class;
    }

    @Override
    public void configure(Binder binder) {
        super.configure(binder);
        // additional bindings can be added here
    }
}
