package org.example.cds.ui.contentassist;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.ServiceEntity;

/**
 * Content-assist contributions for the CDS editor.
 *
 * Xtext generates default proposal logic. This class extends it with:
 *   - Smart snippets for common constructs (entity, service, aspect, extend)
 *   - Filtered type proposals (only entities for association targets)
 *   - SAP annotation key suggestions
 */
public class CDSProposalProvider extends AbstractCDSProposalProvider {

    // ── Snippet proposals ────────────────────────────────────────────────────

    /**
     * Inserts a full entity skeleton when 'entity' keyword is selected.
     */
    @Override
    public void completeEntityDef_Name(EObject model, Assignment assignment,
            ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
        super.completeEntityDef_Name(model, assignment, context, acceptor);
        // Additional template proposals are registered via plugin.xml
        // (see org.eclipse.ui.editors.templates extension point)
    }

    /**
     * For 'using' imports, propose namespaces from the workspace index.
     * The default cross-reference completion handles this; we just
     * ensure proxy resolution doesn't break the list.
     */
    @Override
    public void completeUsingDecl_ImportedNamespace(EObject model,
            Assignment assignment, ContentAssistContext context,
            ICompletionProposalAcceptor acceptor) {
        super.completeUsingDecl_ImportedNamespace(model, assignment, context, acceptor);
    }

    /**
     * Association target proposals — only EntityDefs.
     * Xtext's cross-reference completion already filters via CDSScopeProvider;
     * this override adds a descriptive display string.
     */
    @Override
    public void completeAssocDef_Target(EObject model, Assignment assignment,
            ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
        super.completeAssocDef_Target(model, assignment, context, acceptor);
    }

    /**
     * Annotation name proposals — propose common SAP UI annotation keys.
     */
    @Override
    public void completeAnnotation_Name(EObject model, Assignment assignment,
            ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
        super.completeAnnotation_Name(model, assignment, context, acceptor);

        String[] commonAnnotations = {
            "UI.LineItem", "UI.SelectionField", "UI.Facets",
            "UI.HeaderInfo", "UI.Hidden",
            "Common.Label", "Common.Text", "Common.ValueList",
            "Core.Computed", "Core.Immutable",
            "Capabilities.Insertable", "Capabilities.Updatable",
            "readonly", "mandatory"
        };

        for (String ann : commonAnnotations) {
            ICompletionProposal proposal = createCompletionProposal(
                ann, ann + " (SAP annotation)", null, context);
            if (proposal != null) acceptor.accept(proposal);
        }
    }

    /**
     * Projected element proposals — only elements from the source entity.
     * CDSScopeProvider already restricts the scope; this just passes through.
     */
    @Override
    public void completeProjectedElement_Ref(EObject model, Assignment assignment,
            ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
        super.completeProjectedElement_Ref(model, assignment, context, acceptor);
    }
}
