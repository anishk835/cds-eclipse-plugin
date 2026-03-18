package org.example.cds.ui.search;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com.google.inject.Inject;

/**
 * Handler for the "Find References" command (Ctrl+Shift+G).
 *
 * Finds all references to the CDS element at the current cursor position
 * and displays them in Eclipse's Search view.
 */
public class FindReferencesHandler extends AbstractHandler {

    @Inject
    private EObjectAtOffsetHelper eObjectAtOffsetHelper;

    @Inject
    private CDSReferenceFinder referenceFinder;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            // Get the active editor
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (!(editor instanceof XtextEditor)) {
                return null;
            }

            XtextEditor xtextEditor = (XtextEditor) editor;

            // Get the current selection
            ISelection selection = HandlerUtil.getCurrentSelection(event);
            if (!(selection instanceof ITextSelection)) {
                return null;
            }

            ITextSelection textSelection = (ITextSelection) selection;
            final int offset = textSelection.getOffset();

            // Find the EObject at the cursor position
            EObject targetElement = xtextEditor.getDocument().readOnly(
                new IUnitOfWork<EObject, XtextResource>() {
                    @Override
                    public EObject exec(XtextResource resource) throws Exception {
                        return eObjectAtOffsetHelper.resolveElementAt(resource, offset);
                    }
                }
            );

            if (targetElement == null) {
                return null;
            }

            // Check if this is a referenceable element
            if (!isReferenceable(targetElement)) {
                return null;
            }

            // Create and run the search query
            CDSSearchQuery query = new CDSSearchQuery(targetElement, referenceFinder);
            NewSearchUI.runQueryInBackground(query);

        } catch (Exception e) {
            throw new ExecutionException("Error finding references", e);
        }

        return null;
    }

    /**
     * Checks if the given element can have references.
     *
     * @param element The EObject to check
     * @return true if the element can be referenced
     */
    private boolean isReferenceable(EObject element) {
        return element instanceof org.example.cds.cDS.EntityDef
            || element instanceof org.example.cds.cDS.TypeDef
            || element instanceof org.example.cds.cDS.AspectDef
            || element instanceof org.example.cds.cDS.EnumDef
            || element instanceof org.example.cds.cDS.ServiceDef
            || element instanceof org.example.cds.cDS.ViewDef
            || element instanceof org.example.cds.cDS.Element;
    }
}
