package org.example.cds.tests;

import static org.junit.Assert.*;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.example.cds.cDS.CdsFile;
import org.example.cds.cDS.EntityDef;
import org.example.cds.cDS.TypeDef;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.eclipse.xtext.testing.XtextRunner;

import com.google.inject.Inject;

import java.util.stream.Collectors;

/**
 * Basic grammar parsing tests to verify build is working.
 * Tests parsing of simple CDS constructs.
 */
@RunWith(XtextRunner.class)
@InjectWith(CDSInjectorProvider.class)
public class BasicCDSParsingTest {

    @Inject ParseHelper<CdsFile> parseHelper;
    @Inject ValidationTestHelper validationHelper;

    @Test
    public void testParseNamespace() throws Exception {
        CdsFile file = parse("namespace my.bookshop;");
        assertEquals("my.bookshop", file.getNamespaceDecl().getName());
    }

    @Test
    public void testParseSimpleEntity() throws Exception {
        CdsFile file = parse("""
            entity Books {
              ID : Integer;
              title : String;
            }
            """);
        assertEquals(1, file.getDefinitions().size());
        EntityDef entity = (EntityDef) file.getDefinitions().get(0);
        assertEquals("Books", entity.getName());
        assertEquals(2, getElements(entity).size());
    }

    @Test
    public void testParseType() throws Exception {
        CdsFile file = parse("type Currency = String;");
        assertTrue(file.getDefinitions().get(0) instanceof TypeDef);
        TypeDef typedef = (TypeDef) file.getDefinitions().get(0);
        assertEquals("Currency", typedef.getName());
    }

    @Test
    public void testParseMultipleDefinitions() throws Exception {
        CdsFile file = parse("""
            namespace my.app;

            type Status = String;

            entity Books {
              ID : Integer;
            }

            entity Authors {
              ID : Integer;
            }
            """);
        assertEquals(3, file.getDefinitions().size());
    }

    @Test
    public void testNoParseErrors() throws Exception {
        CdsFile file = parseHelper.parse("""
            namespace bookshop;

            entity Books {
              ID : Integer;
              title : String;
            }
            """);
        assertNotNull(file);
        assertTrue("Should have no parse errors",
            file.eResource().getErrors().isEmpty());
    }

    // Helper method for AST compatibility
    private java.util.List<org.example.cds.cDS.Element> getElements(EntityDef entity) {
        return entity.getMembers().stream()
            .filter(m -> m instanceof org.example.cds.cDS.Element)
            .map(m -> (org.example.cds.cDS.Element) m)
            .collect(Collectors.toList());
    }

    private CdsFile parse(String src) throws Exception {
        CdsFile file = parseHelper.parse(src);
        assertNotNull(file);
        assertTrue("Parse errors: " + file.eResource().getErrors(),
            file.eResource().getErrors().isEmpty());
        return file;
    }
}
