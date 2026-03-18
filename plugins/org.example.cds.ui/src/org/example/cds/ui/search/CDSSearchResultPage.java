package org.example.cds.ui.search;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;

/**
 * Search results page for displaying CDS reference search results.
 *
 * This page is displayed in Eclipse's Search view when a Find References
 * search is performed.
 */
public class CDSSearchResultPage extends AbstractTextSearchViewPage {

    public CDSSearchResultPage() {
        super(AbstractTextSearchViewPage.FLAG_LAYOUT_FLAT);
    }

    @Override
    protected void elementsChanged(Object[] objects) {
        if (getViewer() != null) {
            getViewer().refresh();
        }
    }

    @Override
    protected void clear() {
        if (getViewer() != null) {
            getViewer().refresh();
        }
    }

    @Override
    protected void configureTableViewer(TableViewer viewer) {
        viewer.setContentProvider(new CDSSearchResultContentProvider(this));
        viewer.setLabelProvider(new CDSSearchResultLabelProvider());
    }

    @Override
    protected void configureTreeViewer(org.eclipse.jface.viewers.TreeViewer viewer) {
        viewer.setContentProvider(new CDSSearchResultContentProvider(this));
        viewer.setLabelProvider(new CDSSearchResultLabelProvider());
    }
}
