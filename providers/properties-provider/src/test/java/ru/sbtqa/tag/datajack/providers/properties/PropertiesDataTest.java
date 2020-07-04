package ru.sbtqa.tag.datajack.providers.properties;

import com.mongodb.BasicDBObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.callback.SampleDataGensCallback;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesException;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;

import java.util.*;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.none;
import static ru.sbtqa.tag.datajack.callback.SampleDataCache.getCache;

public class PropertiesDataTest {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesDataTest.class);

    private final String propertiesDataPath = "src/test/resources/properties";
    @Rule
    public ExpectedException expectDataExceptions = none();

    @Before
    public void setUp() {
        getCache().clear();
    }

    @Test
    public void differentExtensionTest() throws DataException {
        String collectionName = "Config";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName, "conf", ",");

        assertEquals("123qwe",
                dataProvider.get("Common.password2").getValue());
    }

    @Test
    public void simpleArrayTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("a",
                dataProvider.get("array[0]").getValue());
    }

    @Test
    public void arrayTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("1",
                dataProvider.get("array[1].b").getValue());
    }

    @Test
    public void notArrayTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName, "properties", "~");

        assertEquals("a,b,c,2",
                dataProvider.get("Common.notArray").getValue());
    }

    @Test
    public void deepArrayTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("1",
                dataProvider.get("array[2].b[0].b.c").getValue());
    }

    @Test
    public void arrayReferenceTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("123qwe",
                dataProvider.get("array[3].ref").getValue());
    }


    @Test
    public void arrayReferencedTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("a",
                testDataProvider.get("ref array[0]").getValue());
    }

    @Test
    public void arrayDeepReferencedTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("1",
                testDataProvider.get("ref array[2].b[0].b.c").getValue());
    }

    @Test
    public void arrayDeepReferencedToReferenceTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider testDataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("123qwe",
                testDataProvider.get("ref array[2].b[0].b.d").getValue());
    }

    @Test
    public void arrayGeneratorTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        dataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = dataProvider.get("Common.gen gen.gendata").getValue();

        assertEquals(genGenOrgigin,
                dataProvider.get("array[3].genRef").getValue());
    }

    @Test
    public void getReferenceTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("123qwe",
                dataProvider.get("Common.password2").getValue());
    }

    @Test
    public void getDeepReferenceTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String deepReferenceValue = testDataProvider.get("Common.ref object data.gendata reference").getValue();
        String shortReferenceValue = testDataProvider.fromCollection("DataBlocks").get("Common.gendata reference").getValue();
        String shortComplexValue = testDataProvider.fromCollection("DataBlocks").get("Common.gen gen.gendata").getValue();
        String shortValue = testDataProvider.fromCollection("DataBlocks").get("Common").getByPath("${gen gen}").get("gendata").getValue();

        assertEquals("Deep reference isn't equal direct value", shortValue, deepReferenceValue);
        assertEquals("Short reference isn't equal direct value", shortValue, shortReferenceValue);
        assertEquals("Short complex value isn't equal direct value", shortValue, shortComplexValue);
    }

    @Test
    public void getByPathTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String fullPathValue = testDataProvider.getByPath("$Tests{Common.ref object data.gendata reference}").getValue();
        String fullPathReferenceValue = testDataProvider.getByPath("$DataBlocks{Common.gendata reference}").getValue();
        String shortPathValue = testDataProvider.getByPath("$DataBlocks").getByPath("${Common.gen gen.gendata}").getValue();
        String shortPathCombinedValue = testDataProvider.getByPath("$DataBlocks").getByPath("${Common}").get("gen gen").get("gendata").getValue();

        assertEquals("Deep reference isn't equal direct value", shortPathCombinedValue, fullPathValue);
        assertEquals("Short reference isn't equal direct value", shortPathCombinedValue, fullPathReferenceValue);
        assertEquals("Short complex value isn't equal direct value", shortPathCombinedValue, shortPathValue);
    }

    @Test
    public void getByPathArrayTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String value = testDataProvider.getByPath("$Tests{array[0]}").getValue();
        String valueArrayObject = testDataProvider.getByPath("${array[1].b}").getValue();

        assertEquals("a", value);
        assertEquals("1", valueArrayObject);
        assertEquals(testDataProvider.get("array").toString(), testDataProvider.getByPath("$Tests{array}").toString());
    }

    @Test
    public void isReferenceTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertTrue("This isn't reference",
                dataProvider.get("Common.password2").isReference());
    }

    @Test
    public void getFromAnotherCollectionTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("123",
                dataProvider.fromCollection("Tests").
                        get("dataBlocks").get("Common").get("password").getValue());
    }

    @Test
    public void getNotValuedValueTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("20.91",
                dataProvider.get("Common.price").getValue());
    }

    @Test
    public void failWithWrongPath() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);
        String wrongPath = "Common.password.paww";

        expectDataExceptions
                .expect(FieldNotFoundException.class
                );
        expectDataExceptions.expectMessage(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                collectionName, wrongPath.split("[.]")[wrongPath.split("[.]").length - 1], wrongPath));

        dataProvider.get(wrongPath).getValue();
    }

    @Test
    public void failWithWrongGetGetPath() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collection);

        expectDataExceptions
                .expect(FieldNotFoundException.class);
        expectDataExceptions.expectMessage("Collection \"DataBlocks\" doesn't contain \"paww\" "
                + "field in path \"DataBlocks.Common.password\"");

        dataProvider.get("Common").get("password").get("paww");
    }

    @Test
    public void failWithCyclicReference() throws DataException {
        String collectionName = "DataBlocks";
        String cyclicPath = "Common.cyclic";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);
        String cyclicObject = format("{ \"comment\" : \"Cyclic\", \"$ref\" : \"%s:Common.cyclic\" }", collectionName);
        expectDataExceptions
                .expect(CyclicReferencesException.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        dataProvider.get(cyclicPath).getValue();
    }

    @Test
    public void genDataSameCollectionTest() throws DataException {
        String collectionName = "DataBlocks";
        PropertiesDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        dataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = dataProvider.get("Common.gen gen.gendata").getValue();

        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, dataProvider.get("Common.gendata reference").getValue());
        assertEquals(genGenOrgigin, dataProvider.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(dataProvider.get("Common.gendata").getValue(), dataProvider.get("Common").get("gendata").getValue());
    }

    @Test
    public void genDataDifferentCollections() throws DataException {
        String collectionName = "Tests";

        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        dataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = dataProvider.get("Common.gendata").getValue();

        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, dataProvider.get("Common").get("gendata").getValue());
    }

    @Test
    public void genDataDifferentCollectionsReference() throws DataException {
        String collectionName = "DataBlocks";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        dataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = dataProvider.get("Common.gen gen.gendata").getValue();

        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));

        dataProvider = dataProvider.fromCollection("Tests");

        assertEquals(genGenOrgigin, dataProvider.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(genGenOrgigin, dataProvider.get("Common.gen gen.gendata").getValue());
    }

    @Test
    public void getRefAsObject() throws DataException {
        TestDataProvider originalProvider = new PropertiesDataProvider(this.propertiesDataPath, "DataBlocks");
        String original = originalProvider.get("Common").toString();

        TestDataProvider referencedProvider = new PropertiesDataProvider(this.propertiesDataPath, "Tests");
        String referenced = referencedProvider.get("Common.ref object data").getReference().toString();

        assertEquals(original, referenced);
    }

    @Test
    public void failRefAsObject() throws DataException {
        String collection = "DataBlocks";
        String path = "testId";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collection);

        expectDataExceptions.expect(ReferenceException.class);
        expectDataExceptions.expectMessage(String.format("There is no reference in \"%s.%s\". Collection \"%s\"",
                collection, path, collection));

        dataProvider.get(path).getReference();
    }

    @Test
    public void toMapTest() throws DataException {
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, "DataBlocks");
        Object supposedToBeMap = dataProvider.toMap();

        assertTrue("Type of return value toMap() is not Map", supposedToBeMap instanceof Map);
        assertNotNull("Map object is null", supposedToBeMap != null);
        assertFalse("Map is empty", ((Map) supposedToBeMap).isEmpty());
    }

    @Test
    public void getKeySetTest() throws DataException {
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, "DataBlocks");
        Object supposedToBeSet = dataProvider.getKeySet();

        assertTrue("Type of return value getKeySet() is not Set", supposedToBeSet instanceof Set);
        assertNotNull("Set object is null", supposedToBeSet != null);
        assertFalse("Set object shouldn't be empty", ((Set) supposedToBeSet).isEmpty());
    }

    @Test
    public void emptyKeySetForValueTest() throws DataException {
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, "DataBlocks");
        Object supposedToBeSet = dataProvider.get("Common.price").getKeySet();

        assertTrue("Type of return value getKeySet() is not Set", supposedToBeSet instanceof Set);
        assertNotNull("Set object is null", supposedToBeSet != null);
        assertTrue("Set object should be empty", ((Set) supposedToBeSet).isEmpty());
    }

    @Test
    public void getEmptySelfTest() throws DataException {
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, "DataBlocks");
        TestDataProvider origin = dataProvider.get("Common");
        TestDataProvider self = origin.get("");

        assertEquals("Objects are not same", origin, self);
    }

    @Test
    public void getValuesTest() throws DataException {
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, "DataBlocks");
        Object rawValues = dataProvider.getValues();

        assertTrue("Type of return value getValues() is not Collection", rawValues instanceof Collection);
        assertNotNull("Return value is null", rawValues != null);
        assertFalse("Collection of values is empty", ((Collection) rawValues).isEmpty());
    }

    @Test
    public void getStringValuesTest() throws DataException {
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, "DataBlocks").get("MapTests");
        Object stringValues = dataProvider.getStringValues();

        assertTrue("Type of return value getStringValues() is not List", stringValues instanceof List);
        assertNotNull("Return value is null", stringValues != null);
        assertFalse("Collection of values is empty", ((Collection) stringValues).isEmpty());

        int resultCollectionSize = ((Collection) stringValues).size();
        int originalMapSize = dataProvider.toMap().size();

        assertEquals(format("getStringValuesTest method has returned incorrect number of elements. Expected {0}, but was {1}", originalMapSize, resultCollectionSize), resultCollectionSize, originalMapSize);

        Iterator resultIterator = ((Collection) stringValues).iterator();
        Iterator originalIterator = dataProvider.toMap().values().iterator();

        while (resultIterator.hasNext()) {
            Object currentResValue = resultIterator.next();
            Object currentOrigValue = originalIterator.next();
            if (!(currentOrigValue instanceof BasicDBObject)) {
                assertEquals("Unexpected value transformation", currentOrigValue.toString(), currentResValue.toString());
            }
        }
    }

    @Test
    public void getJsonTest() throws DataException {
        TestDataProvider testDataProvider = new PropertiesDataProvider(propertiesDataPath, "DataBlocks");
        String stringJson = testDataProvider.get("Params Group 1").getValue();
        String expectedJson = "{ \"password\" : 123 , \"login\" : 123}";
        Assert.assertEquals(expectedJson, stringJson);
    }

    @Test
    public void relativeTest() throws DataException {
        TestDataProvider testDataProvider = new PropertiesDataProvider(propertiesDataPath, "relative/relative1");
        String relativeValue = testDataProvider.get("relates to relative2").getValue();
        String expected = "123";
        Assert.assertEquals(expected, relativeValue);
    }

    @Test
    public void relativeRootTest() throws DataException {
        TestDataProvider testDataProvider = new PropertiesDataProvider(propertiesDataPath, "relative/relative1");
        String relativeValue = testDataProvider.get("relates to root").getValue();
        String expected = "20.91";
        Assert.assertEquals(expected, relativeValue);
    }

    @Test
    public void relativeParentTest() throws DataException {
        TestDataProvider testDataProvider = new PropertiesDataProvider(propertiesDataPath, "relative/relative1");
        String relativeValue = testDataProvider.get("relates to parent").getValue();
        String expected = "20.91";
        Assert.assertEquals(expected, relativeValue);
    }

    @Test
    public void issue290Test() throws Exception {
        TestDataProvider testDataProvider = new PropertiesDataProvider(propertiesDataPath,
                "issue-290", "properties", ";");

        final List<String> stringValues = testDataProvider.get("afd.db.save.hosts").getStringValues();
        final String[] expectedValues = new String[]{"server1", "server2", "server3"};

        Assert.assertArrayEquals(expectedValues, stringValues.toArray(new String[0]));

    }

    @Test
    public void containsArrayTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider dataProvider = new PropertiesDataProvider(this.propertiesDataPath, collectionName);

        assertEquals("a",
                dataProvider.get("containsArray.array[0]").getValue());
        assertEquals("1",
                dataProvider.get("containsArray.array[1]").getValue());
    }
}
