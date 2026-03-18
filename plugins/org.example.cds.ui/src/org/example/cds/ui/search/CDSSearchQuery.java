package org.example.cds.ui.search;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.xtext.resource.IReferenceDescription;

import com.google.inject.Inject;

/**
 * Search query for finding CDS element references.
 *
 * This class integrates with Eclipse's search framework to provide
 * "Find References" functionality for CDS elements.
 */
public class CDSSearchQuery implements ISearchQuery {

    private final EObject target;
    private final String label;
    private CDSSearchResult searchResult;

    @Inject
    private CDSReferenceFinder referenceFinder;

    /**
     * Creates a new search query for the given target element.
     *
     * @param target The CDS element to find references for
     * @param referenceFinder The reference finder to use
     */
    public CDSSearchQuery(EObject target, CDSReferenceFinder referenceFinder) {
        this.target = target;
        this.referenceFinder = referenceFinder;
        this.label = "References to " + referenceFinder.getTargetDescription(target);
    }

    @Override
    public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
        try {
            monitor.beginTask("Searching for references...", IProgressMonitor.UNKNOWN);

            // Find all references
            List<IReferenceDescription> references = referenceFinder.findReferences(target);

            // Create search result
            searchResult = new CDSSearchResult(this, target, references, referenceFinder);

            monitor.done();

            return Status.OK_STATUS;

        } catch (OperationCanceledException e) {
            return Status.CANCEL_STATUS;
        } catch (Exception e) {
            return new Status(
                IStatus.ERROR,
                "org.example.cds.ui",
                "Error searching for references: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean canRerun() {
        return true;
    }

    @Override
    public boolean canRunInBackground() {
        return true;
    }

    @Override
    public ISearchResult getSearchResult() {
        if (searchResult == null) {
            searchResult = new CDSSearchResult(this, target, List.of(), referenceFinder);
        }
        return searchResult;
    }

    /**
     * Gets the target element this query searches for.
     *
     * @return The target EObject
     */
    public EObject getTarget() {
        return target;
    }
}
