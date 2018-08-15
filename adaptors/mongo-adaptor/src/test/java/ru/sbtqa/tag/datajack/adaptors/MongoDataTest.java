package ru.sbtqa.tag.datajack.adaptors;

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

import java.io.IOException;

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
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks");
        assertEquals("123qwe",
                tdo.get("Common.password2").getValue());
    }

    @Test
    public void isReference() throws DataException {
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks");
        assertTrue("Value is not reference",
                tdo.get("Common.password2").isReference());
    }

    @Test
    public void valuePath() throws DataException {
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks");
        assertEquals("Params Group 1.password",
                tdo.get("Common").get("password2.value.path").getValue());
    }

    @Test
    public void getFromAnotherCollection() throws DataException {
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks");
        assertEquals("123",
                tdo.fromCollection("Tests").
                        get("dataBlocks").get("Common").get("password").getValue());
    }

    @Test
    public void getFromAnotherCollectionWithRefId() throws DataException {
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks");
        assertEquals("@234234",
                tdo.fromCollection("Tests").
                        get("Common").get("ref data").getValue());
    }

    @Test
    public void getNotValuedValue() throws DataException {
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks");
        assertEquals("20.91",
                tdo.get("Common.price").getValue());
    }

    @Test
    public void failInitWithNotExistenCollection() throws DataException {
        String wrongCollection = "sdfsfsdf";
        expectDataExceptions
                .expect(CollectionNotfoundException.class);
        expectDataExceptions.expectMessage(format("There is no \"%s\" collection or it's empty",
                wrongCollection));

        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, wrongCollection);
    }

    @Test
    public void failWithWrongPath() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, collection);
        String wrongPath = "Common.password.paww";
        expectDataExceptions
                .expect(FieldNotFoundException.class);
        expectDataExceptions.expectMessage(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                collection, wrongPath.split("[.]")[wrongPath.split("[.]").length - 1], wrongPath));

        tdo.get(wrongPath).getValue();
    }

    @Test
    public void failWithWrongGetGetPath() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, collection);
        expectDataExceptions
                .expect(FieldNotFoundException.class);
        expectDataExceptions.expectMessage("Collection \"DataBlocks\" doesn't contain \"paww\" "
                + "field in path \"DataBlocks.Common.password\"");
        tdo.get("Common").get("password").get("paww");
    }

    @Test
    public void failWithCyclicReference() throws DataException {
        String collection = "DataBlocks";
        String cyclicPath = "Common.cyclic";
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, collection);

        String cyclicObject = format("{ \"value\" : { \"collection\" : \"%s\", "
                + "\"path\" : \"Common.cyclic\" }, \"comment\" : \"Cyclic\"", collection);
        expectDataExceptions
                .expect(CyclicReferencesExeption.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        tdo.get(cyclicPath).getValue();
    }

    @Test
    public void genDataSameCollection() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, collection);
        tdo.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = tdo.get("Common.gen gen.gendata").getValue();
        String genOneGenData = tdo.get("Common.gendata").getValue();
        String genSpaced = tdo.get("Common").get("gen gen").get("gendata").getValue();
        String genRef = tdo.get("Common.gendata reference").getValue();
        String genSplitGenData = tdo.get("Common").get("gendata").getValue();

        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, genSpaced);
        assertEquals(genGenOrgigin, genRef);
        assertEquals(genOneGenData, genSplitGenData);
    }

    @Test
    public void genDataDifferentCollections() throws DataException {
        String collection = "Tests";
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, collection);
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(tdo.get("Common.gendata").getValue(), tdo.get("Common").get("gendata").getValue());

    }

    @Test
    public void genDataDifferentCollectionsSamePath() throws DataException {
        String collection = "DataBlocks";
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, collection);
        tdo.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = tdo.get("Common.gen gen.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));

        tdo = tdo.fromCollection("Tests");

        assertEquals(genGenOrgigin, tdo.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(genGenOrgigin, tdo.get("Common.gen gen.gendata").getValue());
    }

    @Test
    public void genDataDifferentCollectionWithRefId() throws DataException {
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks").
                fromCollection("DataBlocks", "57a94a160a279ec293f61665");
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gen gen.gendata").getValue();
        String genDataTarget = tdo.fromCollection("Tests").
                get("Common.ref data gen").getValue();
        assertEquals(genGenOrgigin, genDataTarget);
    }

    @Test
    public void dataDifferentCollectionWithRefId() throws DataException {
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks");
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("testId").getValue();
        String genDataTarget = tdo.fromCollection("Tests").
                get("Common.ref data").getValue();
        assertEquals(genGenOrgigin, genDataTarget);
    }

    @Test
    public void failCyclicReferenceDifferentCollectionWithRefId() throws DataException {
        String collection = "Tests";
        String cyclicPath = "Common.ref cyclic refid";
        TestDataProvider tdo = new MongoDataProviderAdaptor(mongoDb, collection);

        String cyclicObject = "{ \"value\" : { \"path\" : \"Common.failCyclicReferenceDifferentCollectionWithRefId\", \"collection\" : \"DataBlocks\", \"refId\" : \"57a94a160a279ec293f61665\" } }";
        expectDataExceptions
                .expect(CyclicReferencesExeption.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        tdo.get(cyclicPath).getValue();
    }

    @Test
    public void getRefAsObject() throws DataException {
        TestDataProvider originalTdo = new MongoDataProviderAdaptor(mongoDb, "DataBlocks").
                fromCollection("DataBlocks", "57a94a160a279ec293f61665").get("Common");
        TestDataProvider referencedTdo = new MongoDataProviderAdaptor(mongoDb, "Tests").
                get("Common.ref object data").getReference();
        assertEquals(originalTdo.toString(), referencedTdo.toString());
    }

    @Test
    public void failRefAsObject() throws DataException {
        String collection = "DataBlocks";
        String path = "testId";
        MongoDataProviderAdaptor tdo = new MongoDataProviderAdaptor(mongoDb, collection);
        expectDataExceptions.expect(ReferenceException.class);
        expectDataExceptions.expectMessage(String.format("There is no reference in \"%s.%s\". Collection \"%s\"",
                collection, path, collection));
        tdo.get(path).getReference();
    }
}
