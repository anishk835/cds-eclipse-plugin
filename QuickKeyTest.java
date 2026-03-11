package org.example.cds.tests;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.example.cds.CDSStandaloneSetup;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.Element;
import org.example.cds.cDS.ElementModifier;
import org.example.cds.cDS.EntityDef;

import com.google.inject.Injector;

/**
 * Quick standalone test to verify key modifier parsing works.
 * Run with: java -cp ... QuickKeyTest
 */
public class QuickKeyTest {
    public static void main(String[] args) {
        // Initialize Xtext standalone
        Injector injector = new CDSStandaloneSetup().createInjectorAndDoEMFRegistration();
        ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);

        // Parse test CDS
        String cds = """
            entity Orders {
              key ID: UUID;
              key lineNo: Integer;
              customerName: String;
            }
            """;

        Resource resource = resourceSet.createResource(URI.createURI("temp.cds"));
        try {
            resource.load(new java.io.ByteArrayInputStream(cds.getBytes()), null);

            if (!resource.getErrors().isEmpty()) {
                System.err.println("Parse errors: " + resource.getErrors());
                System.exit(1);
            }

            CdsFile file = (CdsFile) resource.getContents().get(0);
            EntityDef entity = (EntityDef) file.getDefinitions().get(0);

            System.out.println("Entity: " + entity.getName());
            for (Element el : entity.getElements()) {
                System.out.println("  " + el.getName() + " : " +
                    (el.getModifier() == ElementModifier.KEY ? "KEY " : "") +
                    (el.getType() != null ? el.getType().getRef().getName() : "?"));
            }

            // Verify
            Element el1 = entity.getElements().get(0);
            Element el2 = entity.getElements().get(1);
            Element el3 = entity.getElements().get(2);

            if (el1.getModifier() == ElementModifier.KEY &&
                el2.getModifier() == ElementModifier.KEY &&
                el3.getModifier() == null) {
                System.out.println("\n✅ SUCCESS: Key modifier parsing works correctly!");
                System.exit(0);
            } else {
                System.err.println("\n❌ FAILED: Key modifiers not parsed correctly");
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
