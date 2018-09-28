package ru.sbtqa.tag.datajack.providers.excel;

import com.mongodb.BasicDBObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.callback.SampleDataGensCallback;
import ru.sbtqa.tag.datajack.providers.excel.ExcelDataProvider;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesException;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;

import java.util.*;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.none;
import static ru.sbtqa.tag.datajack.callback.SampleDataCache.getCache;

public class ExcelDataTest {

    private final String excelDataPath = "src/test/resources/excell/TestData";
    private final String collectionName = "Tests";
    @Rule
    public ExpectedException expectDataExceptions = none();

    @Before
    public void setUp() {
        getCache().clear();
    }

    @Test
    public void isReferenceTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        assertTrue("Value is not reference",
                testDataProvider.get("Common.linkWithValue").isReference());
    }

    @Test
    public void getReferenceTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        assertEquals("value",
                testDataProvider.get("Common.linkWithValue").getValue());
    }

    @Test
    public void getDeepReferenceTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String deepReferenceValue = testDataProvider.get("Common.ref object data.refToAnother").getValue();
        String shortReferenceValue = testDataProvider.fromCollection("DataBlocks").get("NewObject.refToAnother").getValue();
        String shortValue = testDataProvider.fromCollection("DataBlocks").get("AnotherObject").get("anotherValue").getValue();
        String shortComplexValue = testDataProvider.fromCollection("DataBlocks").get("AnotherObject.anotherValue").getValue();

        assertEquals("Deep reference isn't equal direct value", shortValue, deepReferenceValue);
        assertEquals("Short reference isn't equal direct value", shortValue, shortReferenceValue);
        assertEquals("Short complex value isn't equal direct value", shortValue, shortComplexValue);
    }

    @Test
    public void getByPathTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String fullPathValue = testDataProvider.getByPath("$Tests{Common.ref object data.refToAnother}").getValue();
        String fullPathReferenceValue = testDataProvider.getByPath("$DataBlocks{NewObject.refToAnother}").getValue();
        String shortPathValue = testDataProvider.getByPath("$DataBlocks").getByPath("${AnotherObject.anotherValue}").getValue();
        String shortPathCombinedValue = testDataProvider.getByPath("$DataBlocks").getByPath("${AnotherObject}").getByPath("${anotherValue}").getValue();

        assertEquals("Deep reference isn't equal direct value", shortPathCombinedValue, fullPathValue);
        assertEquals("Short reference isn't equal direct value", shortPathCombinedValue, fullPathReferenceValue);
        assertEquals("Short complex value isn't equal direct value", shortPathCombinedValue, shortPathValue);
    }

    @Test
    public void valuePathTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        assertEquals("justSomeTestId",
                testDataProvider.get("Common").get("id").getValue());
    }

    @Test
    public void getFromAnotherCollectionTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        assertEquals("HELLO WORLD",
                testDataProvider.fromCollection("DataBlocks").
                        get("AnotherObject").get("anotherValue").getValue());
    }

    @Test
    public void getNotValuedValueTest() throws DataException {
        // TODO: 02.09.2016 is that r'ly actual for xls? Looks like not, cus all values there are strings
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        assertEquals("20.91",
                testDataProvider.get("Common.price").getValue());
    }

    @Test
    public void failWithWrongPath() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        String wrongPath = "Common.password.paww";
        expectDataExceptions
                .expect(FieldNotFoundException.class
                );
        expectDataExceptions.expectMessage(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                collectionName, "paww", "Common.password"));

        testDataProvider.get(wrongPath).getValue();
    }

    @Test
    public void failWithWrongGetGetPath() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        String wrongPath = "Common.password.paww";
        expectDataExceptions
                .expect(FieldNotFoundException.class);
        expectDataExceptions.expectMessage("Collection \"Tests\" doesn't contain \"paww\" field in path \"Tests.Common.password\"");
        testDataProvider.get("Common").get("password").get("paww");
    }

    @Test
    public void failWithCyclicReference() throws DataException {
        String cyclicPath = "Common.cyclic";
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        String cyclicObject = "{ \"value\" : { \"collection\" : \"DataBlocks\", "
                + "\"path\" : \"AnotherObject.cyclicRef\" }";
        expectDataExceptions
                .expect(CyclicReferencesException.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        testDataProvider.get(cyclicPath).getValue();
    }

    @Test
    public void genDataSameCollectionTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);
        String genGenOrigin = testDataProvider.get("Uncommon.reftestGen").getValue();
        assertFalse("Generator is not applied", genGenOrigin.contains("generate:"));
        assertEquals(genGenOrigin, testDataProvider.get("Common.linkToGenerator").getValue());
        assertEquals(genGenOrigin, testDataProvider.get("Uncommon").get("reftestGen").getValue());
        assertEquals(testDataProvider.get("Common.linkToGenerator").getValue(), testDataProvider.get("Common").get("linkToGenerator").getValue());
    }

    @Test
    public void genDataDifferentCollections() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = testDataProvider.get("Common.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, testDataProvider.get("Common").get("gendata").getValue());
    }

    @Test
    public void genDataDifferentCollectionsReference() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = testDataProvider.get("Common.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        testDataProvider = testDataProvider.fromCollection("DataBlocks");

        assertEquals(genGenOrgigin, testDataProvider.get("AnotherObject").get("generatedInOtherCollection").getValue());
        assertEquals(genGenOrgigin, testDataProvider.get("AnotherObject.generatedInOtherCollection").getValue());
    }

    @Test
    public void getRefAsObject() throws DataException {
        TestDataProvider originalProvider = new ExcelDataProvider(this.excelDataPath, "DataBlocks").get("NewObject");
        TestDataProvider referencedProvider = new ExcelDataProvider(this.excelDataPath, collectionName).
                get("Common.ref object data").getReference();
        assertEquals(originalProvider.toString(), referencedProvider.toString());
    }

    @Test
    public void failRefAsObject() throws DataException {
        String path = "Common.id";
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        expectDataExceptions.expect(ReferenceException.class);
        expectDataExceptions.expectMessage(String.format("There is no reference in \"%s.%s\". Collection \"%s\"",
                collectionName, path, collectionName));
        testDataProvider.get(path).getReference();
    }

    @Test
    public void toMapTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        Object supposedToBeMap = testDataProvider.toMap();

        assertTrue("Type of return value toMap() is not Map", supposedToBeMap instanceof Map);
        assertNotNull("Map object is null", supposedToBeMap != null);
        assertFalse("Map is empty", ((Map) supposedToBeMap).isEmpty());
    }

    @Test
    public void getKeySetTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        Object supposedToBeSet = testDataProvider.getKeySet();

        assertTrue("Type of return value getKeySet() is not Set", supposedToBeSet instanceof Set);
        assertNotNull("Set object is null", supposedToBeSet != null);
        assertFalse("Set object shouldn't be empty", ((Set) supposedToBeSet).isEmpty());
    }

    @Test
    public void emptyKeySetForValueTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        Object supposedToBeSet = testDataProvider.get("Common.price").getKeySet();

        assertTrue("Type of return value getKeySet() is not Set", supposedToBeSet instanceof Set);
        assertNotNull("Set object is null", supposedToBeSet != null);
        assertTrue("Set object should be empty", ((Set) supposedToBeSet).isEmpty());
    }

    @Test
    public void getValuesTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        Object rawValues = testDataProvider.getValues();

        assertTrue("Type of return value getValues() is not Collection", rawValues instanceof Collection);
        assertNotNull("Return value is null", rawValues != null);
        assertFalse("Collection of values is empty", ((Collection) rawValues).isEmpty());
    }

    @Test
    public void getEmptySelfTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName);
        TestDataProvider origin = testDataProvider.get("Common");
        TestDataProvider self = origin.get("");

        assertEquals("Objects are not same", origin, self);
    }

    @Test
    public void getStringValuesTest() throws DataException {
        TestDataProvider testDataProvider = new ExcelDataProvider(this.excelDataPath, collectionName).get("MapTests");
        Object stringValues = testDataProvider.getStringValues();

        assertTrue("Type of return value getStringValues() is not List", stringValues instanceof List);
        assertNotNull("Return value is null", stringValues != null);
        assertFalse("Collection of values is empty", ((Collection) stringValues).isEmpty());

        int resultCollectionSize = ((Collection) stringValues).size();
        int originalMapSize = testDataProvider.toMap().size();
        assertEquals(format("getStringValuesTest method has returned incorrect number of elements. Expected {0}, but was {1}", originalMapSize, resultCollectionSize), resultCollectionSize, originalMapSize);

        Iterator resultIterator = ((Collection) stringValues).iterator();
        Iterator originalIterator = testDataProvider.toMap().values().iterator();
        while (resultIterator.hasNext()) {
            Object currentResValue = resultIterator.next();
            Object currentOrigValue = originalIterator.next();
            if (!(currentOrigValue instanceof BasicDBObject)) {
                assertEquals("Unexpected value transformation", currentOrigValue.toString(), currentResValue.toString());
            }
        }
    }
}
