package ru.sbtqa.tag.datajack.providers.json;

import com.mongodb.BasicDBObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.callback.SampleDataGensCallback;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesExeption;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;

import java.util.*;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.none;
import static ru.sbtqa.tag.datajack.callback.SampleDataCache.getCache;

public class JsonDataTest {

    private static final String JSON_DATA_PATH = "src/test/resources/json";
    @Rule
    public ExpectedException expectDataExceptions = none();

    @Before
    public void setUp() {
        getCache().clear();
    }

    @Test
    public void differentExtensionTest() throws DataException {
        String collectionName = "JsonP";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName, "jsonp");

        assertEquals("123qwe",
                testDataProvider.get("Common.password2").getValue());
    }

    @Test
    public void simpleArrayTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertEquals("a",
                testDataProvider.get("array[0]").getValue());
    }

    @Test
    public void arrayTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertEquals("1",
                testDataProvider.get("array[1].b").getValue());
    }

    @Test
    public void deepArrayTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertEquals("1",
                testDataProvider.get("array[2].b[0].b.c").getValue());
    }

    @Test
    public void arrayReferenceTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertEquals("123qwe",
                testDataProvider.get("array[3].ref").getValue());
    }

    @Test
    public void arrayGeneratorTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = testDataProvider.get("Common.gen gen.gendata").getValue();

        assertEquals(genGenOrgigin,
                testDataProvider.get("array[3].genRef").getValue());
    }

    @Test
    public void getReferenceTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertEquals("123qwe",
                testDataProvider.get("Common.password2").getValue());
    }

    @Test
    public void isReferenceTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertTrue("This isn't reference",
                testDataProvider.get("Common.password2").isReference());
    }

    @Test
    public void valuePathTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertEquals("Params Group 1.password",
                testDataProvider.get("Common").get("password2.value.path").getValue());
    }

    @Test
    public void getFromAnotherCollectionTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertEquals("123",
                testDataProvider.fromCollection("Tests").
                        get("dataBlocks").get("Common").get("password").getValue());
    }

    @Test
    public void getNotValuedValueTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        assertEquals("20.91",
                testDataProvider.get("Common.price").getValue());
    }

    @Test
    public void failWithWrongPath() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);
        String wrongPath = "Common.password.paww";

        expectDataExceptions
                .expect(FieldNotFoundException.class
                );
        expectDataExceptions.expectMessage(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                collectionName, wrongPath.split("[.]")[wrongPath.split("[.]").length - 1], wrongPath));

        testDataProvider.get(wrongPath).getValue();
    }

    @Test
    public void failWithWrongGetGetPath() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collection);

        expectDataExceptions
                .expect(FieldNotFoundException.class);

        expectDataExceptions.expectMessage("Collection \"DataBlocks\" doesn't contain \"paww\" "
                + "field in path \"DataBlocks.Common.password\"");
        testDataProvider.get("Common").get("password").get("paww");
    }

    @Test
    public void failWithCyclicReference() throws DataException {
        String collectionName = "DataBlocks";
        String cyclicPath = "Common.cyclic";

        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);

        String cyclicObject = format("{ \"value\" : { \"collection\" : \"%s\", "
                + "\"path\" : \"Common.cyclic\" }, \"comment\" : \"Cyclic\"", collectionName);

        expectDataExceptions
                .expect(CyclicReferencesExeption.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        testDataProvider.get(cyclicPath).getValue();
    }

    @Test
    public void genDataSameCollectionTest() throws DataException {
        String collectionName = "DataBlocks";

        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = testDataProvider.get("Common.gen gen.gendata").getValue();

        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, testDataProvider.get("Common.gendata reference").getValue());
        assertEquals(genGenOrgigin, testDataProvider.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(testDataProvider.get("Common.gendata").getValue(), testDataProvider.get("Common").get("gendata").getValue());

    }

    @Test
    public void genDataDifferentCollections() throws DataException {
        String collectionName = "Tests";

        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = testDataProvider.get("Common.gendata").getValue();

        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, testDataProvider.get("Common").get("gendata").getValue());

    }

    @Test
    public void genDataDifferentCollectionsReference() throws DataException {
        String collectionName = "DataBlocks";

        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = testDataProvider.get("Common.gen gen.gendata").getValue();

        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));

        testDataProvider = testDataProvider.fromCollection("Tests");

        assertEquals(genGenOrgigin, testDataProvider.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(genGenOrgigin, testDataProvider.get("Common.gen gen.gendata").getValue());
    }

    @Test
    public void getRefAsObject() throws DataException {
        TestDataProvider originalTdo = new JsonDataProvider(JSON_DATA_PATH, "DataBlocks");
        String original = originalTdo.get("Common").toString();

        TestDataProvider referencedTdo = new JsonDataProvider(JSON_DATA_PATH, "Tests");
        String referenced = referencedTdo.get("Common.ref object data").getReference().toString();

        assertEquals(original, referenced);
    }

    @Test
    public void failRefAsObject() throws DataException {
        String collection = "DataBlocks";
        String path = "testId";

        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, collection);

        expectDataExceptions.expect(ReferenceException.class);
        expectDataExceptions.expectMessage(String.format("There is no reference in \"%s.%s\". Collection \"%s\"",
                collection, path, collection));

        testDataProvider.get(path).getReference();
    }

    @Test
    public void toMapTest() throws DataException {
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, "DataBlocks");
        Object supposedToBeMap = testDataProvider.toMap();

        assertTrue("Type of return value toMap() is not Map", supposedToBeMap instanceof Map);
        assertNotNull("Map object is null", supposedToBeMap != null);
        assertFalse("Map is empty", ((Map) supposedToBeMap).isEmpty());
    }

    @Test
    public void getKeySetTest() throws DataException {
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, "DataBlocks");
        Object supposedToBeSet = testDataProvider.getKeySet();

        assertTrue("Type of return value getKeySet() is not Set", supposedToBeSet instanceof Set);
        assertNotNull("Set object is null", supposedToBeSet != null);
        assertFalse("Set object shouldn't be empty", ((Set) supposedToBeSet).isEmpty());
    }

    @Test
    public void emptyKeySetForValueTest() throws DataException {
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, "DataBlocks");
        Object supposedToBeSet = testDataProvider.get("Common.price").getKeySet();

        assertTrue("Type of return value getKeySet() is not Set", supposedToBeSet instanceof Set);
        assertNotNull("Set object is null", supposedToBeSet != null);
        assertTrue("Set object should be empty", ((Set) supposedToBeSet).isEmpty());
    }

    @Test
    public void getEmptySelfTest() throws DataException {
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, "DataBlocks");
        TestDataProvider origin = testDataProvider.get("Common");
        TestDataProvider self = origin.get("");

        assertEquals("Objects are not same", origin, self);
    }

    @Test
    public void getValuesTest() throws DataException {
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, "DataBlocks");
        Object rawValues = testDataProvider.getValues();

        assertTrue("Type of return value getValues() is not Collection", rawValues instanceof Collection);
        assertNotNull("Return value is null", rawValues != null);
        assertFalse("Collection of values is empty", ((Collection) rawValues).isEmpty());
    }

    @Test
    public void getStringValuesTest() throws DataException {
        TestDataProvider testDataProvider = new JsonDataProvider(JSON_DATA_PATH, "DataBlocks").get("MapTests");
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
