package org.example.cds.ui.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.IReferenceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.ui.editor.findrefs.DefaultReferenceFinder;
import org.eclipse.xtext.ui.editor.findrefs.IReferenceFinder;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Finds all references to CDS elements (entities, types, aspects, etc.) in the workspace.
 *
 * This class is used by the "Find References" action (Ctrl+Shift+G) to locate all places
 * where a CDS element is referenced, including:
 * - Association targets
 * - Type references
 * - Service entity sources
 * - Extend/annotate targets
 * - Projected elements
 * - Function/action parameters
 */
public class CDSReferenceFinder {

    @Inject
    private DefaultReferenceFinder referenceFinder;

    @Inject
    private Provider<IResourceDescriptions> resourceDescriptionsProvider;

    /**
     * Finds all references to the given target element.
     *
     * @param target The EObject to find references to (EntityDef, TypeDef, etc.)
     * @return List of reference descriptions with location info
     */
    public List<IReferenceDescription> findReferences(EObject target) {
        return findReferences(target, null);
    }

    /**
     * Finds all references to the given target element with progress monitoring.
     *
     * @param target The EObject to find references to
     * @param monitor Progress monitor (can be null)
     * @return List of reference descriptions with location info
     */
    public List<IReferenceDescription> findReferences(EObject target, IProgressMonitor monitor) {
        List<IReferenceDescription> references = new ArrayList<>();

        if (target == null || target.eResource() == null) {
            return references;
        }

        try {
            // Get target URI
            URI targetURI = EcoreUtil.getURI(target);
            List<URI> targetURIs = new ArrayList<>();
            targetURIs.add(targetURI);

            // Create acceptor to collect references
            IAcceptor<IReferenceDescription> acceptor = new IAcceptor<IReferenceDescription>() {
                @Override
                public void accept(IReferenceDescription reference) {
                    references.add(reference);
                }
            };

            // Create local resource access with proper IUnitOfWork implementation
            IReferenceFinder.ILocalResourceAccess localResourceAccess =
                new IReferenceFinder.ILocalResourceAccess() {
                    @Override
                    public <R> R readOnly(URI uri, IUnitOfWork<R, ResourceSet> work) {
                        ResourceSet resourceSet = target.eResource().getResourceSet();
                        if (resourceSet != null) {
                            try {
                                return work.exec(resourceSet);
                            } catch (Exception e) {
                                System.err.println("Error accessing resource: " + e.getMessage());
                                return null;
                            }
                        }
                        return null;
                    }
                };

            // Find all references
            referenceFinder.findAllReferences(
                targetURIs,
                localResourceAccess,
                acceptor,
                monitor
            );

        } catch (Exception e) {
            // Log error but return partial results
            System.err.println("Error finding references: " + e.getMessage());
            e.printStackTrace();
        }

        return references;
    }

    /**
     * Groups references by file for display in search results.
     *
     * @param references List of reference descriptions
     * @return Map of IFile to list of references in that file
     */
    public Map<IFile, List<IReferenceDescription>> groupByFile(List<IReferenceDescription> references) {
        Map<IFile, List<IReferenceDescription>> grouped = new HashMap<>();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        for (IReferenceDescription ref : references) {
            URI sourceURI = ref.getSourceEObjectUri();
            if (sourceURI != null && sourceURI.isPlatformResource()) {
                String path = sourceURI.toPlatformString(true);
                IFile file = root.getFile(new org.eclipse.core.runtime.Path(path));

                if (file.exists()) {
                    grouped.computeIfAbsent(file, k -> new ArrayList<>()).add(ref);
                }
            }
        }

        return grouped;
    }

    /**
     * Counts total number of references.
     *
     * @param references List of reference descriptions
     * @return Total count
     */
    public int countReferences(List<IReferenceDescription> references) {
        return references.size();
    }

    /**
     * Gets a human-readable description of the target element.
     *
     * @param target The target EObject
     * @return Description string (e.g., "Entity 'Product'", "Type 'Currency'")
     */
    public String getTargetDescription(EObject target) {
        if (target instanceof org.example.cds.cDS.EntityDef) {
            return "Entity '" + ((org.example.cds.cDS.EntityDef) target).getName() + "'";
        } else if (target instanceof org.example.cds.cDS.TypeDef) {
            return "Type '" + ((org.example.cds.cDS.TypeDef) target).getName() + "'";
        } else if (target instanceof org.example.cds.cDS.AspectDef) {
            return "Aspect '" + ((org.example.cds.cDS.AspectDef) target).getName() + "'";
        } else if (target instanceof org.example.cds.cDS.EnumDef) {
            return "Enum '" + ((org.example.cds.cDS.EnumDef) target).getName() + "'";
        } else if (target instanceof org.example.cds.cDS.ServiceDef) {
            return "Service '" + ((org.example.cds.cDS.ServiceDef) target).getName() + "'";
        } else if (target instanceof org.example.cds.cDS.ViewDef) {
            return "View '" + ((org.example.cds.cDS.ViewDef) target).getName() + "'";
        } else if (target instanceof org.example.cds.cDS.Element) {
            return "Element '" + ((org.example.cds.cDS.Element) target).getName() + "'";
        }
        return "CDS Element";
    }

    /**
     * Finds references in a specific project only.
     *
     * @param target The target element
     * @param project The project to search in
     * @return List of references in the project
     */
    public List<IReferenceDescription> findReferencesInProject(EObject target, IProject project) {
        List<IReferenceDescription> allReferences = findReferences(target);
        List<IReferenceDescription> projectReferences = new ArrayList<>();

        for (IReferenceDescription ref : allReferences) {
            URI sourceURI = ref.getSourceEObjectUri();
            if (sourceURI != null && sourceURI.isPlatformResource()) {
                String path = sourceURI.toPlatformString(true);
                if (path.startsWith("/" + project.getName() + "/")) {
                    projectReferences.add(ref);
                }
            }
        }

        return projectReferences;
    }
}
