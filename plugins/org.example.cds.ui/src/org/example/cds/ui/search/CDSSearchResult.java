package org.example.cds.ui.search;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.xtext.resource.IReferenceDescription;

/**
 * Search result for CDS reference searches.
 *
 * Contains the matches found by a CDSSearchQuery and provides
 * adapters for displaying results in Eclipse's Search view.
 */
public class CDSSearchResult extends AbstractTextSearchResult {

    private final CDSSearchQuery query;
    private final EObject target;
    private final List<IReferenceDescription> references;
    private final CDSReferenceFinder referenceFinder;
    private final Map<IFile, List<IReferenceDescription>> referencesByFile;

    /**
     * Creates a new search result.
     *
     * @param query The query that produced this result
     * @param target The target element
     * @param references List of found references
     * @param referenceFinder The reference finder
     */
    public CDSSearchResult(
            CDSSearchQuery query,
            EObject target,
            List<IReferenceDescription> references,
            CDSReferenceFinder referenceFinder) {
        this.query = query;
        this.target = target;
        this.references = references;
        this.referenceFinder = referenceFinder;
        this.referencesByFile = referenceFinder.groupByFile(references);

        // Add matches
        for (Map.Entry<IFile, List<IReferenceDescription>> entry : referencesByFile.entrySet()) {
            IFile file = entry.getKey();
            for (IReferenceDescription ref : entry.getValue()) {
                // Create a match for each reference
                // We use the file as the element since we don't have line/column info easily
                Match match = new Match(file, 0, 0);
                addMatch(match);
            }
        }
    }

    @Override
    public String getLabel() {
        int count = getMatchCount();
        String targetDesc = referenceFinder.getTargetDescription(target);
        return String.format("%s - %d %s in workspace",
            targetDesc,
            count,
            count == 1 ? "reference" : "references"
        );
    }

    @Override
    public String getTooltip() {
        return getLabel();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        // Could return a custom icon here
        return null;
    }

    @Override
    public ISearchQuery getQuery() {
        return query;
    }

    @Override
    public IEditorMatchAdapter getEditorMatchAdapter() {
        // Not implemented - could provide editor-specific matching
        return null;
    }

    @Override
    public IFileMatchAdapter getFileMatchAdapter() {
        return new IFileMatchAdapter() {
            @Override
            public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
                return getMatches(file);
            }

            @Override
            public IFile getFile(Object element) {
                if (element instanceof IFile) {
                    return (IFile) element;
                }
                return null;
            }
        };
    }

    /**
     * Gets the list of all references found.
     *
     * @return List of reference descriptions
     */
    public List<IReferenceDescription> getReferences() {
        return references;
    }

    /**
     * Gets references grouped by file.
     *
     * @return Map of files to references
     */
    public Map<IFile, List<IReferenceDescription>> getReferencesByFile() {
        return referencesByFile;
    }

    /**
     * Gets the target element.
     *
     * @return The target EObject
     */
    public EObject getTarget() {
        return target;
    }
}
