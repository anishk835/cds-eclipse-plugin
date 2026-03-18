package org.example.cds.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.resource.IReferenceDescription;

/**
 * Label provider for CDS search results.
 *
 * Provides labels and icons for search result elements.
 */
public class CDSSearchResultLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof IFile) {
            IFile file = (IFile) element;
            return file.getProjectRelativePath().toString();
        } else if (element instanceof IReferenceDescription) {
            IReferenceDescription ref = (IReferenceDescription) element;
            // Format: "line X: <context>"
            // Since we don't have line numbers easily, just show the reference type
            return "Reference in " + ref.getContainerEObjectURI().fragment();
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IFile) {
            return PlatformUI.getWorkbench().getSharedImages()
                .getImage(ISharedImages.IMG_OBJ_FILE);
        }
        return super.getImage(element);
    }
}
