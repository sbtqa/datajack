package ru.sbtqa.tag.datajack.mongo;

import com.github.fakemongo.junit.FongoRule;
import com.mongodb.DB;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.callback.SampleDataGensCallback;
import ru.sbtqa.tag.datajack.exceptions.*;
import ru.sbtqa.tag.datajack.mongo.MongoDataProvider;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.none;
import static ru.sbtqa.tag.datajack.callback.SampleDataCache.getCache;

public class MongoDataTest {

    private final FongoRule fongoRule = new FongoRule(false);

    private final ExpectedException exception = ExpectedException.none();

    @Rule
    public TestRule rules = RuleChain.outerRule(exception).around(fongoRule);
    @Rule
    public ExpectedException expectDataExceptions = none();
    private DB mongoDb;

    /**
     * @throws IOException if no json found
     */
    @Before
    public void setUp() throws IOException {
        fongoRule.insertFile(fongoRule.newCollection("Tests"), "/mongo/Tests.json");
        fongoRule.insertFile(fongoRule.newCollection("DataBlocks"), "/mongo/DataBlocks.json");
        mongoDb = fongoRule.getDB();
        getCache().clear();
    }

    @Test
    public void getReference() throws DataException {
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, "DataBlocks");
        assertEquals("123qwe",
                testDataProvider.get("Common.password2").getValue());
    }

    @Test
    public void getDeepReferenceTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String deepReferenceValue = testDataProvider.get("Common.ref object data.gendata reference").getValue();
        String shortReferenceValue = testDataProvider.fromCollection("DataBlocks").get("Common.gendata reference").getValue();
        String shortComplexValue = testDataProvider.fromCollection("DataBlocks").get("Common.gen gen.gendata").getValue();
        String shortValue = testDataProvider.fromCollection("DataBlocks").get("Common").get("gen gen").get("gendata").getValue();

        assertEquals("Deep reference isn't equal direct value", shortValue, deepReferenceValue);
        assertEquals("Short reference isn't equal direct value", shortValue, shortReferenceValue);
        assertEquals("Short complex value isn't equal direct value", shortValue, shortComplexValue);
    }

    @Test
    public void getByPathTest() throws DataException {
        String collectionName = "Tests";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collectionName);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String fullPathValue = testDataProvider.getByPath("$Tests{Common.ref object data.gendata reference}").getValue();
        String fullPathReferenceValue = testDataProvider.getByPath("$DataBlocks{Common.gendata reference}").getValue();
        String shortPathValue = testDataProvider.getByPath("$DataBlocks").getByPath("${Common.gen gen.gendata}").getValue();
        String shortPathCombinedValue = testDataProvider.getByPath("$DataBlocks").getByPath("${Common}").getByPath("${gen gen}").get("gendata").getValue();

        assertEquals("Deep reference isn't equal direct value", shortPathCombinedValue, fullPathValue);
        assertEquals("Short reference isn't equal direct value", shortPathCombinedValue, fullPathReferenceValue);
        assertEquals("Short complex value isn't equal direct value", shortPathCombinedValue, shortPathValue);
    }

    @Test
    public void isReference() throws DataException {
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, "DataBlocks");
        assertTrue("Value is not reference",
                testDataProvider.get("Common.password2").isReference());
    }

    @Test
    public void getFromAnotherCollection() throws DataException {
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, "DataBlocks");
        assertEquals("123",
                testDataProvider.fromCollection("Tests").
                        get("dataBlocks").get("Common").get("password").getValue());
    }

    @Test
    public void getFromAnotherCollectionWithRefId() throws DataException {
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, "DataBlocks");
        assertEquals("@234234",
                testDataProvider.fromCollection("Tests").
                        get("Common").get("ref data").getValue());
    }

    @Test
    public void getNotValuedValue() throws DataException {
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, "DataBlocks");
        assertEquals("20.91",
                testDataProvider.get("Common.price").getValue());
    }

    @Test
    public void failInitWithNotExistenCollection() throws DataException {
        String wrongCollection = "sdfsfsdf";
        expectDataExceptions
                .expect(CollectionNotFoundException.class);
        expectDataExceptions.expectMessage(format("There is no \"%s\" collection or it's empty",
                wrongCollection));

        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, wrongCollection);
    }

    @Test
    public void failWithWrongPath() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        String wrongPath = "Common.password.paww";
        expectDataExceptions
                .expect(FieldNotFoundException.class);
        expectDataExceptions.expectMessage(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                collection, wrongPath.split("[.]")[wrongPath.split("[.]").length - 1], wrongPath));

        testDataProvider.get(wrongPath).getValue();
    }

    @Test
    public void failWithWrongGetGetPath() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        expectDataExceptions
                .expect(FieldNotFoundException.class);
        expectDataExceptions.expectMessage("Collection \"DataBlocks\" doesn't contain \"paww\" "
                + "field in path \"DataBlocks.Common.password\"");
        testDataProvider.get("Common").get("password").get("paww");
    }

    @Test
    public void failWithCyclicReference() throws DataException {
        String collection = "DataBlocks";
        String cyclicPath = "Common.cyclic";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);

        String cyclicObject = format("{ \"value\" : { \"collection\" : \"%s\", "
                + "\"path\" : \"Common.cyclic\" }, \"comment\" : \"Cyclic\"", collection);
        expectDataExceptions
                .expect(CyclicReferencesException.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        testDataProvider.get(cyclicPath).getValue();
    }

    @Test
    public void genDataSameCollection() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = testDataProvider.get("Common.gen gen.gendata").getValue();
        String genOneGenData = testDataProvider.get("Common.gendata").getValue();
        String genSpaced = testDataProvider.get("Common").get("gen gen").get("gendata").getValue();
        String genRef = testDataProvider.get("Common.gendata reference").getValue();
        String genSplitGenData = testDataProvider.get("Common").get("gendata").getValue();

        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, genSpaced);
        assertEquals(genGenOrgigin, genRef);
        assertEquals(genOneGenData, genSplitGenData);
    }

    @Test
    public void genDataDifferentCollections() throws DataException {
        String collection = "Tests";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = testDataProvider.get("Common.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(testDataProvider.get("Common.gendata").getValue(), testDataProvider.get("Common").get("gendata").getValue());

    }

    @Test
    public void genDataDifferentCollectionsSamePath() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        testDataProvider.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = testDataProvider.get("Common.gen gen.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));

        testDataProvider = testDataProvider.fromCollection("Tests");

        assertEquals(genGenOrgigin, testDataProvider.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(genGenOrgigin, testDataProvider.get("Common.gen gen.gendata").getValue());
    }

    @Test
    public void genDataDifferentCollectionWithRefId() throws DataException {
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, "DataBlocks").
                fromCollection("DataBlocks", "57a94a160a279ec293f61665");
        testDataProvider.applyGenerator(SampleDataGensCallback.class);
        String genGenOrigin = testDataProvider.get("Common.gen gen.gendata").getValue();
        String genDataTarget = testDataProvider.fromCollection("Tests").
                get("Common.ref data gen").getValue();
        assertEquals(genGenOrigin, genDataTarget);
    }

    @Test
    public void dataDifferentCollectionWithRefId() throws DataException {
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, "DataBlocks");
        testDataProvider.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = testDataProvider.get("testId").getValue();
        String genDataTarget = testDataProvider.fromCollection("Tests").
                get("Common.ref data").getValue();
        assertEquals(genGenOrgigin, genDataTarget);
    }

    @Test
    public void failCyclicReferenceDifferentCollectionWithRefId() throws DataException {
        String collection = "Tests";
        String cyclicPath = "Common.ref cyclic refid";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);

        String cyclicObject = "{ \"value\" : { \"path\" : \"Common.failCyclicReferenceDifferentCollectionWithRefId\", \"collection\" : \"DataBlocks\", \"refId\" : \"57a94a160a279ec293f61665\" } }";
        expectDataExceptions
                .expect(CyclicReferencesException.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        testDataProvider.get(cyclicPath).getValue();
    }

    @Test
    public void getRefAsObject() throws DataException {
        TestDataProvider originalProvider = new MongoDataProvider(mongoDb, "DataBlocks")
                .fromCollection("DataBlocks", "57a94a160a279ec293f61665").get("Common");
        TestDataProvider referencedProvider = new MongoDataProvider(mongoDb, "Tests")
                .get("Common.ref object data").getReference();
        assertEquals(originalProvider.toString(), referencedProvider.toString());
    }

    @Test
    public void failRefAsObject() throws DataException {
        String collection = "DataBlocks";
        String path = "testId";
        MongoDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        expectDataExceptions.expect(ReferenceException.class);
        expectDataExceptions.expectMessage(String.format("There is no reference in \"%s.%s\". Collection \"%s\"",
                collection, path, collection));
        testDataProvider.get(path).getReference();
    }

    @Test
    public void toMapTest() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        Object supposedToBeMap = testDataProvider.get("Common").toMap();

        assertTrue("Type of return value toMap() is not Map", supposedToBeMap instanceof Map);
        assertNotNull("Map object is null", supposedToBeMap != null);
        assertFalse("Map is empty", ((Map) supposedToBeMap).isEmpty());
    }

    @Test
    public void getKeySetTest() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        Object supposedToBeSet = testDataProvider.get("Common").getKeySet();

        assertTrue("Type of return value getKeySet() is not Set", supposedToBeSet instanceof Set);
        assertNotNull("Set object is null", supposedToBeSet != null);
        assertFalse("Set object shouldn't be empty", ((Set) supposedToBeSet).isEmpty());
    }

    @Test
    public void emptyKeySetForValueTest() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        Object supposedToBeSet = testDataProvider.get("Common.price").getKeySet();

        assertTrue("Type of return value getKeySet() is not Set", supposedToBeSet instanceof Set);
        assertNotNull("Set object is null", supposedToBeSet != null);
        assertTrue("Set object should be empty", ((Set) supposedToBeSet).isEmpty());
    }

    @Test
    public void getEmptySelfTest() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider testDataProvider = new MongoDataProvider(mongoDb, collection);
        TestDataProvider origin = testDataProvider.get("Common");
        TestDataProvider self = origin.get("");

        assertEquals("Objects are not same", origin, self);
    }
}
