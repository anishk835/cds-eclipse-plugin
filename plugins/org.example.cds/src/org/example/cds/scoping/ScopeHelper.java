package org.example.cds.scoping;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.example.cds.cDS.Definition;
import org.example.cds.cDS.UsingDecl;

import java.util.Optional;

/**
 * Helper methods for scope validation and resolution.
 * Used by validators to check if cross-references are properly resolved.
 */
public class ScopeHelper {

    /**
     * Checks if a cross-reference is resolved (not a proxy).
     * In Xtext, unresolved references are represented as proxy objects.
     */
    public boolean isResolved(EObject obj) {
        return obj != null && !obj.eIsProxy();
    }

    /**
     * Extracts the import source path from a using directive.
     * Returns empty if no 'from' clause.
     *
     * Example: using { Currency } from './common' → Optional.of("./common")
     */
    public Optional<String> getImportSource(UsingDecl usingDir) {
        String from = usingDir.getImportedNamespace();
        if (from == null || from.isEmpty()) {
            return Optional.empty();
        }
        // Remove quotes: './common' -> ./common or "./common" -> ./common
        from = from.replaceAll("^['\"]|['\"]$", "");
        return Optional.of(from);
    }

    /**
     * Resolves a file path relative to the current resource.
     * Returns true if the resource can be loaded.
     *
     * Note: This is a simplified check. In production, you'd use IResourceDescriptions
     * for better workspace integration.
     */
    public boolean canResolveFile(String path, Resource currentResource) {
        if (path == null || currentResource == null) {
            return false;
        }

        try {
            // Normalize the path to absolute URI
            URI normalizedUri = normalizePath(path, currentResource);

            // Check if resource exists in the resource set
            if (currentResource.getResourceSet() instanceof XtextResourceSet) {
                XtextResourceSet resourceSet = (XtextResourceSet) currentResource.getResourceSet();

                // Try to load the resource (this checks if it exists)
                try {
                    Resource targetResource = resourceSet.getResource(normalizedUri, true);
                    return targetResource != null &&
                           !targetResource.getContents().isEmpty() &&
                           targetResource.getErrors().isEmpty();
                } catch (Exception e) {
                    // Resource not found or cannot be loaded
                    return false;
                }
            }
        } catch (Exception e) {
            // Path resolution failed
            return false;
        }

        return false;
    }

    /**
     * Normalizes a relative path to absolute URI based on current resource.
     *
     * Examples:
     * - "./common" relative to "file:///project/db/schema.cds"
     *   → "file:///project/db/common.cds"
     * - "../shared/types" relative to "file:///project/srv/service.cds"
     *   → "file:///project/shared/types.cds"
     */
    private URI normalizePath(String path, Resource currentResource) {
        URI currentUri = currentResource.getURI();

        // Add .cds extension if not present
        if (!path.endsWith(".cds")) {
            path = path + ".cds";
        }

        // Resolve relative to current resource
        URI relativeUri = URI.createURI(path);
        URI resolvedUri = relativeUri.resolve(currentUri);

        return resolvedUri;
    }

    /**
     * Checks if a definition is visible (not a proxy and has a name).
     * Used to filter out unresolved or invalid definitions.
     */
    public boolean isVisible(Definition def) {
        if (def == null || def.eIsProxy()) return false;
        String name = getDefinitionName(def);
        return name != null && !name.isEmpty();
    }

    /**
     * Helper to extract name from Definition subtypes.
     */
    private String getDefinitionName(Definition def) {
        if (def instanceof org.example.cds.cDS.EntityDef entity) return entity.getName();
        if (def instanceof org.example.cds.cDS.ViewDef view) return view.getName();
        if (def instanceof org.example.cds.cDS.TypeDef type) return type.getName();
        if (def instanceof org.example.cds.cDS.EnumDef enumDef) return enumDef.getName();
        if (def instanceof org.example.cds.cDS.ServiceDef service) return service.getName();
        if (def instanceof org.example.cds.cDS.AspectDef aspect) return aspect.getName();
        return null;
    }

    /**
     * Checks if a type reference points to a built-in type.
     * Built-in types are: UUID, Boolean, Integer, Integer64, Decimal, Double,
     * Date, Time, DateTime, Timestamp, String, LargeString, Binary, LargeBinary
     */
    public boolean isBuiltInType(String typeName) {
        if (typeName == null) return false;

        return "UUID".equals(typeName) ||
               "Boolean".equals(typeName) ||
               "Integer".equals(typeName) ||
               "Integer64".equals(typeName) ||
               "Decimal".equals(typeName) ||
               "Double".equals(typeName) ||
               "Date".equals(typeName) ||
               "Time".equals(typeName) ||
               "DateTime".equals(typeName) ||
               "Timestamp".equals(typeName) ||
               "String".equals(typeName) ||
               "LargeString".equals(typeName) ||
               "Binary".equals(typeName) ||
               "LargeBinary".equals(typeName);
    }

    /**
     * Extracts the name from an unresolved reference for error reporting.
     * This is a best-effort attempt to get a readable name even when resolution fails.
     */
    public String getUnresolvedReferenceName(EObject context) {
        if (context == null) return "unknown";

        // Try to get name from proxy URI
        if (context.eIsProxy()) {
            String proxyUri = context.toString();
            // Extract name from proxy URI format
            int fragmentIndex = proxyUri.indexOf('#');
            if (fragmentIndex >= 0 && fragmentIndex < proxyUri.length() - 1) {
                String fragment = proxyUri.substring(fragmentIndex + 1);
                // Fragment might be something like "//Books" or "//@definitions.0"
                if (fragment.startsWith("//")) {
                    return fragment.substring(2);
                }
            }
        }

        return "unknown";
    }
}
