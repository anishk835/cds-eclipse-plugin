package org.example.cds.ui.outline;

import java.util.stream.Collectors;

import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.impl.DefaultOutlineTreeProvider;
import org.eclipse.xtext.ui.editor.outline.impl.DocumentRootNode;
import org.example.cds.cDS.AspectDef;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.Element;
import org.example.cds.cDS.ExtendDef;
import org.example.cds.cDS.ServiceDef;
import org.example.cds.cDS.ServiceEntity;
import org.example.cds.cDS.TypeDef;

/**
 * Populates the Eclipse Outline view for CDS files.
 *
 * Structure shown:
 *   [file]
 *   ├── Entities
 *   │   └── Books
 *   │       ├── ID : Integer
 *   │       └── title : String
 *   ├── Types
 *   │   └── Currency = String
 *   ├── Aspects
 *   │   └── Timestamped
 *   └── Services
 *       └── CatalogService
 *           └── Books (projection on ...)
 */
public class CDSOutlineTreeProvider extends DefaultOutlineTreeProvider {

    // Helper method to get elements from an entity
    private java.util.List<Element> getElements(EntityDef entity) {
        return entity.getMembers().stream()
            .filter(m -> m instanceof Element)
            .map(m -> (Element) m)
            .collect(Collectors.toList());
    }

    // Helper method to get elements from an aspect
    private java.util.List<Element> getElements(AspectDef aspect) {
        // AspectDef doesn't have getMembers() in current AST
        // Return empty list for now
        return new java.util.ArrayList<>();
    }

    // Helper method to get elements from an extend
    private java.util.List<Element> getElements(ExtendDef extend) {
        // ExtendDef doesn't have getMembers() in current AST
        // Return empty list for now
        return new java.util.ArrayList<>();
    }

    // Helper method to get service entities
    private java.util.List<ServiceEntity> getEntities(ServiceDef service) {
        return service.getMembers().stream()
            .filter(m -> m instanceof ServiceEntity)
            .map(m -> (ServiceEntity) m)
            .collect(Collectors.toList());
    }

    // ── CdsFile: group into categories ──────────────────────────────────────

    protected void _createChildren(DocumentRootNode parentNode, CdsFile file) {
        boolean hasEntities = file.getDefinitions().stream()
            .anyMatch(d -> d instanceof EntityDef);
        boolean hasTypes = file.getDefinitions().stream()
            .anyMatch(d -> d instanceof TypeDef);
        boolean hasAspects = file.getDefinitions().stream()
            .anyMatch(d -> d instanceof AspectDef);
        boolean hasServices = file.getDefinitions().stream()
            .anyMatch(d -> d instanceof ServiceDef);
        boolean hasExtends = file.getDefinitions().stream()
            .anyMatch(d -> d instanceof ExtendDef);

        file.getDefinitions().forEach(def -> createNode(parentNode, def));
    }

    // ── EntityDef: show elements as children ────────────────────────────────

    protected void _createChildren(IOutlineNode parentNode, EntityDef entity) {
        // aspect includes - commented out due to ambiguous method signature
        // entity.getIncludes().forEach(aspect ->
        //     createEStructuralFeatureNode(parentNode, entity,
        //         org.example.cds.cDS.CDSPackage.Literals.ENTITY_DEF__INCLUDES,
        //         null, "includes: " + aspect.getName(), true)
        // );
        // elements
        getElements(entity).forEach(el -> createNode(parentNode, el));
    }

    // ── Element: leaf — shown as "name : Type" ───────────────────────────────

    protected boolean _isLeaf(Element element) {
        return true;
    }

    // ── AspectDef: show elements ─────────────────────────────────────────────

    protected void _createChildren(IOutlineNode parentNode, AspectDef aspect) {
        getElements(aspect).forEach(el -> createNode(parentNode, el));
    }

    // ── ServiceDef: show projected entities ─────────────────────────────────

    protected void _createChildren(IOutlineNode parentNode, ServiceDef service) {
        getEntities(service).forEach(entity -> createNode(parentNode, entity));
    }

    // ── ServiceEntity: leaf ──────────────────────────────────────────────────

    protected boolean _isLeaf(ServiceEntity entity) {
        return entity.getEntityBody() == null
            || entity.getEntityBody().getProjectedElements().isEmpty();
    }

    // ── ExtendDef: leaf ──────────────────────────────────────────────────────

    protected boolean _isLeaf(ExtendDef extend) {
        return getElements(extend).isEmpty();
    }

    // ── TypeDef: leaf ────────────────────────────────────────────────────────

    protected boolean _isLeaf(TypeDef typedef) {
        return true;
    }
}
