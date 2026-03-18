package org.example.cds.ui.hover;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.ui.editor.hover.html.DefaultEObjectHoverProvider;
import org.example.cds.cDS.AnnotationValue;
import org.example.cds.cDS.ArrayAnnotationValue;
import org.example.cds.cDS.AspectDef;
import org.example.cds.cDS.AssocDef;
import org.example.cds.cDS.BoolAnnotationValue;
import org.example.cds.cDS.Definition;
import org.example.cds.cDS.Element;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.EnumAnnotationValue;
import org.example.cds.cDS.EnumDef;
import org.example.cds.cDS.EnumValue;
import org.example.cds.cDS.NumberAnnotationValue;
import org.example.cds.cDS.RecordAnnotationValue;
import org.example.cds.cDS.ServiceDef;
import org.example.cds.cDS.ServiceEntity;
import org.example.cds.cDS.SimpleTypeRef;
import org.example.cds.cDS.StringAnnotationValue;
import org.example.cds.cDS.TypeDef;
import org.example.cds.cDS.TypeRef;

/**
 * Provides rich hover documentation for CDS elements.
 *
 * Shown in the Eclipse hover popup (F2 or mouse-over):
 *   - EntityDef:    element count, included aspects
 *   - TypeDef:      resolved type chain
 *   - Element:      full type signature including arguments
 *   - AssocDef:     association kind, cardinality, target entity
 *   - ServiceEntity: source entity and projection count
 *   - EnumDef:      enum type, value count, value list
 */
public class CDSHoverProvider extends DefaultEObjectHoverProvider {

    // Helper methods for AST compatibility
    private List<Element> getElements(EntityDef entity) {
        return entity.getMembers().stream()
            .filter(m -> m instanceof Element)
            .map(m -> (Element) m)
            .collect(Collectors.toList());
    }

    private List<Element> getElements(AspectDef aspect) {
        // AspectDef doesn't have getMembers() in current AST
        // Return empty list for now
        return new ArrayList<>();
    }

    private String getName(Definition def) {
        if (def instanceof EntityDef) return ((EntityDef) def).getName();
        if (def instanceof TypeDef) return ((TypeDef) def).getName();
        if (def instanceof EnumDef) return ((EnumDef) def).getName();
        if (def instanceof AspectDef) return ((AspectDef) def).getName();
        if (def instanceof ServiceDef) return ((ServiceDef) def).getName();
        return null;
    }

    @Override
    protected String getFirstLine(EObject obj) {
        if (obj instanceof EntityDef entity) {
            return buildEntityHover(entity);
        }
        if (obj instanceof TypeDef typedef) {
            return buildTypeDefHover(typedef);
        }
        if (obj instanceof EnumDef enumDef) {
            return buildEnumHover(enumDef);
        }
        if (obj instanceof EnumValue enumValue) {
            return buildEnumValueHover(enumValue);
        }
        if (obj instanceof Element element) {
            return buildElementHover(element);
        }
        if (obj instanceof AspectDef aspect) {
            return buildAspectHover(aspect);
        }
        if (obj instanceof ServiceDef service) {
            return "<b>service</b> " + service.getName();
        }
        if (obj instanceof ServiceEntity se) {
            return buildServiceEntityHover(se);
        }
        return super.getFirstLine(obj);
    }

    // ── Builders ─────────────────────────────────────────────────────────────

    private String buildEntityHover(EntityDef entity) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>entity</b> ").append(entity.getName());
        if (!entity.getIncludes().isEmpty()) {
            sb.append(" : ");
            entity.getIncludes().forEach(a -> sb.append(a.getName()).append(", "));
            sb.setLength(sb.length() - 2); // trim trailing ", "
        }
        sb.append("<br/><i>")
          .append(getElements(entity).size()).append(" element(s)")
          .append("</i>");
        return sb.toString();
    }

    private String buildTypeDefHover(TypeDef typedef) {
        String typeName = "?";
        TypeRef typeRef = typedef.getType();
        if (typeRef instanceof SimpleTypeRef) {
            SimpleTypeRef simpleRef = (SimpleTypeRef) typeRef;
            if (simpleRef.getRef() != null && !simpleRef.getRef().eIsProxy()) {
                typeName = getName(simpleRef.getRef());
            }
        }
        return "<b>type</b> " + typedef.getName() + " = " + typeName;
    }

    private String buildElementHover(Element element) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(element.getName()).append("</b> : ");

        if (element.getAssoc() != null) {
            AssocDef assoc = element.getAssoc();
            sb.append(assoc.getKind().getLiteral());
            if (assoc.getCardinality() != null) {
                sb.append(" of ")
                  .append(assoc.getCardinality().isMany() ? "many" : "one");
            }
            sb.append(" to ");
            if (assoc.getTarget() != null && !assoc.getTarget().eIsProxy()) {
                sb.append(assoc.getTarget().getName());
            } else {
                sb.append("?");
            }
        } else if (element.getType() != null) {
            TypeRef ref = element.getType();
            if (ref instanceof SimpleTypeRef) {
                SimpleTypeRef simpleRef = (SimpleTypeRef) ref;
                if (simpleRef.getRef() != null && !simpleRef.getRef().eIsProxy()) {
                    sb.append(getName(simpleRef.getRef()));
                } else {
                    sb.append("?");
                }
            } else {
                sb.append("?");
            }
            // getArgs() doesn't exist in current AST - commenting out
            // if (!ref.getArgs().isEmpty()) {
            //     sb.append("(");
            //     ref.getArgs().forEach(a -> sb.append(a.getValue()).append(", "));
            //     sb.setLength(sb.length() - 2);
            //     sb.append(")");
            // }
        }

        // getValue() doesn't exist in current AST - commenting out
        // if (element.getValue() != null) {
        //     sb.append(" <i>(calculated)</i>");
        // }
        return sb.toString();
    }

    private String buildAspectHover(AspectDef aspect) {
        return "<b>aspect</b> " + aspect.getName()
            + "<br/><i>" + getElements(aspect).size() + " element(s)</i>";
    }

    private String buildServiceEntityHover(ServiceEntity se) {
        String sourceName = "?";
        if (se.getEntityBody() != null && se.getEntityBody().getSource() != null
                && !se.getEntityBody().getSource().eIsProxy()) {
            sourceName = se.getEntityBody().getSource().getName();
        }
        return "<b>entity</b> " + se.getName()
            + " <i>as projection on</i> " + sourceName;
    }

    private String buildEnumHover(EnumDef enumDef) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>enum</b> ").append(enumDef.getName());

        // Get base type
        String baseType = getEnumBaseType(enumDef);
        if (baseType != null) {
            sb.append(" : ").append(baseType);
        }

        // Count values including inherited
        List<String> inheritedValues = getInheritedEnumValues(enumDef);
        int ownValues = enumDef.getValues().size();
        int totalValues = inheritedValues.size() + ownValues;

        sb.append("<br/><i>");
        if (inheritedValues.isEmpty()) {
            sb.append(totalValues).append(" value");
            if (totalValues != 1) sb.append("s");
        } else {
            sb.append(totalValues).append(" value");
            if (totalValues != 1) sb.append("s");
            sb.append(" (").append(ownValues).append(" own, ");
            sb.append(inheritedValues.size()).append(" inherited)");
        }
        sb.append("</i>");

        // List values (limit to 10 for readability)
        if (totalValues > 0 && totalValues <= 10) {
            sb.append("<br/><br/>Values: ");
            List<String> allValueNames = new ArrayList<>(inheritedValues);
            for (EnumValue v : enumDef.getValues()) {
                if (v.getName() != null) {
                    allValueNames.add(v.getName());
                }
            }
            sb.append(String.join(", ", allValueNames));
        } else if (totalValues > 10) {
            sb.append("<br/><i>(").append(totalValues).append(" values - hover over individual values to see details)</i>");
        }

        // Add range info for integer enums
        if (baseType != null && baseType.equals("Integer")) {
            List<Integer> intValues = new ArrayList<>();
            for (EnumValue v : enumDef.getValues()) {
                if (v.getValue() != null) {
                    intValues.add(v.getValue().getIntValue());
                }
            }
            if (!intValues.isEmpty()) {
                int min = intValues.stream().min(Integer::compareTo).orElse(0);
                int max = intValues.stream().max(Integer::compareTo).orElse(0);
                sb.append("<br/>Range: [").append(min).append("..").append(max).append("]");
            }
        }

        return sb.toString();
    }

    private String buildEnumValueHover(EnumValue enumValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(enumValue.getName()).append("</b>");

        // Show explicit value if present
        if (enumValue.getValue() != null) {
            if (enumValue.getValue().getStringValue() != null) {
                sb.append(" = ").append(enumValue.getValue().getStringValue());
            } else {
                sb.append(" = ").append(enumValue.getValue().getIntValue());
            }
        }

        // Show parent enum info
        EObject container = enumValue.eContainer();
        if (container instanceof EnumDef enumDef) {
            sb.append("<br/><i>in enum </i>").append(enumDef.getName());
            String baseType = getEnumBaseType(enumDef);
            if (baseType != null) {
                sb.append(" : ").append(baseType);
            }
        }

        // Show annotations
        if (!enumValue.getAnnotations().isEmpty()) {
            sb.append("<br/><br/><b>Annotations:</b>");
            for (var annotation : enumValue.getAnnotations()) {
                sb.append("<br/>  @").append(annotation.getName());
                if (annotation.getValue() != null) {
                    sb.append(": ").append(formatAnnotationValue(annotation.getValue()));
                }
            }
        }

        return sb.toString();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getEnumBaseType(EnumDef enumDef) {
        Definition superType = enumDef.getSuperType();
        if (superType == null) return null;

        String name = getName(superType);
        if ("String".equals(name) || "Integer".equals(name)) {
            return name;
        }

        if (superType instanceof EnumDef parent) {
            return getEnumBaseType(parent);
        }

        return null;
    }

    private List<String> getInheritedEnumValues(EnumDef enumDef) {
        List<String> inherited = new ArrayList<>();
        Definition superType = enumDef.getSuperType();

        if (superType instanceof EnumDef parent) {
            inherited.addAll(getInheritedEnumValues(parent));
            for (EnumValue value : parent.getValues()) {
                if (value.getName() != null) {
                    inherited.add(value.getName());
                }
            }
        }

        return inherited;
    }

    /**
     * Formats an annotation value for display in hover.
     */
    private String formatAnnotationValue(AnnotationValue value) {
        if (value instanceof StringAnnotationValue str) {
            return str.getValue();
        }
        if (value instanceof NumberAnnotationValue num) {
            String result = String.valueOf(num.getValue());
            if (num.getDecimals() != 0) {
                result += "." + num.getDecimals();
            }
            return result;
        }
        if (value instanceof BoolAnnotationValue bool) {
            return bool.getValue();
        }
        if (value instanceof EnumAnnotationValue enumVal) {
            return enumVal.getRef();
        }
        if (value instanceof ArrayAnnotationValue arr) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.getValues().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatAnnotationValue(arr.getValues().get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (value instanceof RecordAnnotationValue rec) {
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < rec.getEntries().size(); i++) {
                if (i > 0) sb.append(", ");
                var entry = rec.getEntries().get(i);
                sb.append(entry.getKey()).append(": ");
                sb.append(formatAnnotationValue(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        return "?";
    }
}
