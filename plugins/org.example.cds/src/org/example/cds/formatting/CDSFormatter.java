package org.example.cds.formatting;

import org.eclipse.xtext.formatting2.AbstractFormatter2;
import org.eclipse.xtext.formatting2.IFormattableDocument;
import org.example.cds.cDS.AnnotateDef;
import org.example.cds.cDS.Annotation;
import org.example.cds.cDS.AspectDef;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.Element;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.ExtendDef;
import org.example.cds.cDS.NamespaceDecl;
import org.example.cds.cDS.ServiceDef;
import org.example.cds.cDS.ServiceEntity;
import org.example.cds.cDS.TypeDef;
import org.example.cds.cDS.UsingDecl;
import org.example.cds.services.CDSGrammarAccess;

import com.google.inject.Inject;

/**
 * Formatter for CDS source files.
 *
 * Rules:
 *  - Blank line between top-level definitions
 *  - Annotations on their own line, above the declaration
 *  - Elements indented 2 spaces inside braces
 *  - Single space around ':' in element declarations
 *  - Semicolons always attached to their preceding token
 */
public class CDSFormatter extends AbstractFormatter2 {

    @Inject
    private CDSGrammarAccess grammar;

    @Override
    public void format(Object obj, IFormattableDocument doc) {
        // TODO: Fix formatter API compatibility
        // The formatter methods below use an incompatible or older API
        // For now, provide minimal formatting
        if (obj instanceof CdsFile) {
            formatCdsFile((CdsFile) obj, doc);
        }
    }

    private void formatCdsFile(CdsFile file, IFormattableDocument doc) {
        // Basic formatting - just format contained elements
        if (file.getNamespaceDecl() != null) {
            doc.format(file.getNamespaceDecl());
        }
        for (UsingDecl u : file.getImports()) {
            doc.format(u);
        }
        for (var def : file.getDefinitions()) {
            doc.format(def);
        }
    }

    // Original formatter code commented out due to API incompatibility
    /*
    // ── File level ───────────────────────────────────────────────────────────

    public void format(CdsFile file, IFormattableDocument doc) {
        if (file.getNamespaceDecl() != null) {
            format(file.getNamespaceDecl(), doc);
            doc.append(file.getNamespaceDecl(), it -> it.setNewLines(2, 2, 2));
        }

        for (UsingDecl u : file.getImports()) {
            format(u, doc);
            doc.append(u, it -> it.setNewLines(1, 1, 2));
        }

        if (!file.getImports().isEmpty() && !file.getDefinitions().isEmpty()) {
            doc.append(file.getImports().get(file.getImports().size() - 1),
                it -> it.setNewLines(2, 2, 2));
        }

        for (var def : file.getDefinitions()) {
            doc.format(def);
            doc.append(def, it -> it.setNewLines(2, 2, 2));
        }
    }

    // ── Namespace ────────────────────────────────────────────────────────────

    public void format(NamespaceDecl ns, IFormattableDocument doc) {
        doc.append(
            doc.regionFor(ns).keyword("namespace"),
            it -> it.oneSpace());
        doc.prepend(
            doc.regionFor(ns).keyword(";"),
            it -> it.noSpace());
    }

    // ── Entity ───────────────────────────────────────────────────────────────

    public void format(EntityDef entity, IFormattableDocument doc) {
        for (Annotation ann : entity.getAnnotations()) {
            doc.format(ann);
            doc.append(ann, it -> it.newLine());
        }
        doc.append(
            doc.regionFor(entity).keyword("entity"),
            it -> it.oneSpace());
        doc.surround(
            doc.regionFor(entity).keyword("{"),
            it -> it.oneSpace());
        doc.prepend(
            doc.regionFor(entity).keyword("}"),
            it -> it.newLine());

        for (Element el : entity.getElements()) {
            doc.format(el);
            doc.prepend(el, it -> {
                it.newLine();
                it.indent();
            });
        }
    }

    // ── Type alias ───────────────────────────────────────────────────────────

    public void format(TypeDef typedef, IFormattableDocument doc) {
        for (Annotation ann : typedef.getAnnotations()) {
            doc.format(ann);
            doc.append(ann, it -> it.newLine());
        }
        doc.append(doc.regionFor(typedef).keyword("type"), it -> it.oneSpace());
        doc.surround(doc.regionFor(typedef).keyword("="), it -> it.oneSpace());
        doc.prepend(doc.regionFor(typedef).keyword(";"), it -> it.noSpace());
    }

    // ── Aspect ───────────────────────────────────────────────────────────────

    public void format(AspectDef aspect, IFormattableDocument doc) {
        for (Annotation ann : aspect.getAnnotations()) {
            doc.format(ann);
            doc.append(ann, it -> it.newLine());
        }
        doc.append(doc.regionFor(aspect).keyword("aspect"), it -> it.oneSpace());
        doc.surround(doc.regionFor(aspect).keyword("{"), it -> it.oneSpace());
        doc.prepend(doc.regionFor(aspect).keyword("}"), it -> it.newLine());

        for (Element el : aspect.getElements()) {
            doc.format(el);
            doc.prepend(el, it -> {
                it.newLine();
                it.indent();
            });
        }
    }

    // ── Service ──────────────────────────────────────────────────────────────

    public void format(ServiceDef service, IFormattableDocument doc) {
        for (Annotation ann : service.getAnnotations()) {
            doc.format(ann);
            doc.append(ann, it -> it.newLine());
        }
        doc.append(doc.regionFor(service).keyword("service"), it -> it.oneSpace());
        doc.surround(doc.regionFor(service).keyword("{"), it -> it.oneSpace());
        doc.prepend(doc.regionFor(service).keyword("}"), it -> it.newLine());

        for (ServiceEntity se : service.getEntities()) {
            doc.format(se);
            doc.prepend(se, it -> {
                it.newLine();
                it.indent();
            });
        }
    }

    // ── Element ──────────────────────────────────────────────────────────────

    public void format(Element element, IFormattableDocument doc) {
        doc.surround(doc.regionFor(element).keyword(":"), it -> it.oneSpace());
        doc.prepend(doc.regionFor(element).keyword(";"), it -> it.noSpace());
        if (!element.getAnnotations().isEmpty()) {
            for (Annotation ann : element.getAnnotations()) {
                doc.format(ann);
                doc.append(ann, it -> it.newLine());
            }
        }
    }

    // ── Extend / Annotate ────────────────────────────────────────────────────

    public void format(ExtendDef extend, IFormattableDocument doc) {
        doc.append(doc.regionFor(extend).keyword("extend"), it -> it.oneSpace());
        doc.surround(doc.regionFor(extend).keyword("with"), it -> it.oneSpace());
    }

    public void format(AnnotateDef annotate, IFormattableDocument doc) {
        doc.append(doc.regionFor(annotate).keyword("annotate"), it -> it.oneSpace());
        doc.surround(doc.regionFor(annotate).keyword("with"), it -> it.oneSpace());
        doc.prepend(doc.regionFor(annotate).keyword(";"), it -> it.noSpace());
    }
    */
}
