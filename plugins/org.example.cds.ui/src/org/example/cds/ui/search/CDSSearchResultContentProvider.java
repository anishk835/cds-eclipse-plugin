package org.example.cds.ui.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.xtext.resource.IReferenceDescription;

/**
 * Content provider for CDS search results.
 *
 * Provides the structure for displaying search results in the Search view.
 */
public class CDSSearchResultContentProvider implements IStructuredContentProvider {

    private final CDSSearchResultPage page;
    private CDSSearchResult result;

    public CDSSearchResultContentProvider(CDSSearchResultPage page) {
        this.page = page;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (!(inputElement instanceof CDSSearchResult)) {
            return new Object[0];
        }

        CDSSearchResult searchResult = (CDSSearchResult) inputElement;
        Map<IFile, List<IReferenceDescription>> referencesByFile = searchResult.getReferencesByFile();

        // Return files that contain matches
        return referencesByFile.keySet().toArray();
    }

    @Override
    public void dispose() {
        // Nothing to dispose
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof CDSSearchResult) {
            this.result = (CDSSearchResult) newInput;
        }
    }

    /**
     * Gets the matches for a specific file.
     *
     * @param file The file
     * @return Array of matches
     */
    public Object[] getChildren(Object file) {
        if (result == null || !(file instanceof IFile)) {
            return new Object[0];
        }

        IFile iFile = (IFile) file;
        Map<IFile, List<IReferenceDescription>> referencesByFile = result.getReferencesByFile();
        List<IReferenceDescription> references = referencesByFile.get(iFile);

        if (references == null) {
            return new Object[0];
        }

        return references.toArray();
    }

    /**
     * Checks if an element has children.
     *
     * @param element The element
     * @return true if it has children
     */
    public boolean hasChildren(Object element) {
        if (element instanceof IFile) {
            return getChildren(element).length > 0;
        }
        return false;
    }
}
