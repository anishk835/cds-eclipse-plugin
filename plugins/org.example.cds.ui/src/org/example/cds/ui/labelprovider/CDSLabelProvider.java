package org.example.cds.ui.labelprovider;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ui.label.DefaultEObjectLabelProvider;
import org.example.cds.cDS.AspectDef;
import org.example.cds.cDS.AssocDef;
import org.example.cds.cDS.Element;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.EnumDef;
import org.example.cds.cDS.ExtendDef;
import org.example.cds.cDS.ServiceDef;
import org.example.cds.cDS.ServiceEntity;
import org.example.cds.cDS.SimpleTypeRef;
import org.example.cds.cDS.TypeDef;
import org.example.cds.cDS.TypeRef;

/**
 * Controls the text labels shown in:
 *   - Eclipse Outline view
 *   - Open CDS Definition dialog (Ctrl+Shift+F3)
 *   - Quick Outline (Ctrl+O)
 */
public class CDSLabelProvider extends DefaultEObjectLabelProvider {

    // Helper method for getting name from Definition
    private String getName(org.example.cds.cDS.Definition def) {
        if (def instanceof EntityDef) return ((EntityDef) def).getName();
        if (def instanceof TypeDef) return ((TypeDef) def).getName();
        if (def instanceof EnumDef) return ((EnumDef) def).getName();
        if (def instanceof AspectDef) return ((AspectDef) def).getName();
        if (def instanceof ServiceDef) return ((ServiceDef) def).getName();
        return null;
    }

    // ── Text labels ──────────────────────────────────────────────────────────

    public String text(EntityDef entity) {
        return "entity " + entity.getName();
    }

    public String text(TypeDef typedef) {
        String target = "";
        TypeRef typeRef = typedef.getType();
        if (typeRef instanceof SimpleTypeRef) {
            SimpleTypeRef simpleRef = (SimpleTypeRef) typeRef;
            if (simpleRef.getRef() != null && !simpleRef.getRef().eIsProxy()) {
                target = " = " + getName(simpleRef.getRef());
            }
        }
        return "type " + typedef.getName() + target;
    }

    public String text(AspectDef aspect) {
        return "aspect " + aspect.getName();
    }

    public String text(Element element) {
        StringBuilder sb = new StringBuilder(element.getName()).append(" : ");
        if (element.getAssoc() != null) {
            AssocDef assoc = element.getAssoc();
            sb.append(assoc.getKind().getLiteral());
            if (assoc.getTarget() != null && !assoc.getTarget().eIsProxy()) {
                sb.append(" to ").append(assoc.getTarget().getName());
            }
        } else {
            TypeRef typeRef = element.getType();
            if (typeRef instanceof SimpleTypeRef) {
                SimpleTypeRef simpleRef = (SimpleTypeRef) typeRef;
                if (simpleRef.getRef() != null && !simpleRef.getRef().eIsProxy()) {
                    sb.append(getName(simpleRef.getRef()));
                } else {
                    sb.append("?");
                }
            } else {
                sb.append("?");
            }
        }
        // getValue() doesn't exist in current AST - commenting out
        // if (element.getValue() != null) sb.append(" (calc)");
        return sb.toString();
    }

    public String text(ServiceDef service) {
        return "service " + service.getName();
    }

    public String text(ServiceEntity se) {
        String source = "?";
        if (se.getEntityBody() != null && se.getEntityBody().getSource() != null
                && !se.getEntityBody().getSource().eIsProxy()) {
            source = se.getEntityBody().getSource().getName();
        }
        return se.getName() + " → " + source;
    }

    public String text(ExtendDef extend) {
        String target = (extend.getTarget() != null && !extend.getTarget().eIsProxy())
            ? extend.getTarget().getName() : "?";
        return "extend " + target;
    }
}
