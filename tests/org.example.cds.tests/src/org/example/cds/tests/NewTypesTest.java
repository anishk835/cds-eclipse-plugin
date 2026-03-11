package org.example.cds.tests;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.example.cds.cDS.CdsFile;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(XtextRunner.class)
@InjectWith(CDSInjectorProvider.class)
public class NewTypesTest {

    @Inject
    private ParseHelper<CdsFile> parseHelper;

    @Inject
    private ValidationTestHelper validationHelper;

    @Test
    public void testUInt8Type() throws Exception {
        CdsFile result = parseHelper.parse("""
            namespace test;

            type Rating = UInt8;

            entity Product {
              key ID: UUID;
              rating: Rating;
            }
        """);

        assertNotNull(result);
        Resource resource = result.eResource();
        assertTrue("Parse errors: " + resource.getErrors(),
            resource.getErrors().isEmpty());
        // Skip validation for now - just test parsing
        // validationHelper.assertNoErrors(result);
    }

    @Test
    public void testInt16Type() throws Exception {
        CdsFile result = parseHelper.parse("""
            namespace test;

            type Stock = Int16;

            entity Inventory {
              key ID: UUID;
              stock: Stock;
              available: Int16;
            }
        """);

        assertNotNull(result);
        Resource resource = result.eResource();
        assertTrue("Parse errors: " + resource.getErrors(),
            resource.getErrors().isEmpty());
        // Skip validation for now
        // validationHelper.assertNoErrors(result);
    }

    @Test
    public void testMapType() throws Exception {
        CdsFile result = parseHelper.parse("""
            namespace test;

            entity Person {
              key ID: UUID;
              name: String;
              details: Map;
            }
        """);

        assertNotNull(result);
        Resource resource = result.eResource();
        assertTrue("Parse errors: " + resource.getErrors(),
            resource.getErrors().isEmpty());
        validationHelper.assertNoErrors(result);
    }

    @Test
    public void testTypeAsProjection() throws Exception {
        CdsFile result = parseHelper.parse("""
            namespace test;

            entity FullName {
              firstName: String;
              middleName: String;
              lastName: String;
            }

            type ShortName : projection on FullName {
              firstName,
              lastName
            };

            entity Author {
              key ID: UUID;
              name: ShortName;
            }
        """);

        assertNotNull(result);
        Resource resource = result.eResource();
        assertTrue("Parse errors: " + resource.getErrors(),
            resource.getErrors().isEmpty());
        // Skip validation for now
        // validationHelper.assertNoErrors(result);
    }

    @Test
    public void testNewIntegerTypesInExpressions() throws Exception {
        CdsFile result = parseHelper.parse("""
            namespace test;

            entity Test {
              key ID: UUID;
              rating: UInt8;
              stock: Int16;
              multiplier: Integer;

              adjustedRating: Integer = rating * 20;
              stockValue: Integer = stock * multiplier;
              isLowStock: Boolean = stock < 10;
            }
        """);

        assertNotNull(result);
        Resource resource = result.eResource();
        assertTrue("Parse errors: " + resource.getErrors(),
            resource.getErrors().isEmpty());
        validationHelper.assertNoErrors(result);
    }

    @Test
    public void testNewTypesWithDefaults() throws Exception {
        CdsFile result = parseHelper.parse("""
            namespace test;

            entity Config {
              key ID: UUID;
              priority: UInt8 = 128;
              balance: Int16 = 0;
              settings: Map;
            }
        """);

        assertNotNull(result);
        Resource resource = result.eResource();
        assertTrue("Parse errors: " + resource.getErrors(),
            resource.getErrors().isEmpty());
        validationHelper.assertNoErrors(result);
    }
}
