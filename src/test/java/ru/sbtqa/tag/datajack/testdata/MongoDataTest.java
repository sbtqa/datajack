package ru.sbtqa.tag.datajack.testdata;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.junit.rules.ExpectedException.none;
import static ru.sbtqa.tag.datajack.callback.SampleDataCache.getCache;
import ru.sbtqa.tag.datajack.callback.SampleDataGensCallback;
import ru.sbtqa.tag.datajack.TestDataObject;
import ru.sbtqa.tag.datajack.adaptors.MongoDataObjectAdaptor;
import ru.sbtqa.tag.datajack.exceptions.CollectionNotfoundException;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesExeption;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;

@Ignore
public class MongoDataTest {

    private static DB mongoDb;

    /**
     *
     */
    @BeforeClass
    public static void setUpClass() {

        List<MongoCredential> credentialsList = new ArrayList<>();
        MongoCredential bddbCred = MongoCredential.createCredential("bddb", "bddb", "123qwe".toCharArray());
        credentialsList.add(bddbCred);

        MongoClient client = new MongoClient(new ServerAddress("mongo"), credentialsList);

        mongoDb = client.getDB("bddb");
    }

    @Before
    public void setUp() {
        getCache().clear();
    }

    @Rule
    public ExpectedException expectDataExceptions = none();

    @Test
    public void getReference() throws DataException {
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, "DataBlocks");
        assertEquals("123qwe",
                tdo.get("Common.password2").getValue());
    }

    @Test
    public void valuePath() throws DataException {
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, "DataBlocks");
        assertEquals("Params Group 1.password",
                tdo.get("Common").get("password2.value.path").getValue());
    }

    @Test
    public void getFromAnotherCollection() throws DataException {
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, "DataBlocks");
        assertEquals("123",
                tdo.fromCollection("Tests").
                get("dataBlocks").get("Common").get("password").getValue());
    }

    @Test
    public void getFromAnotherCollectionWithRefId() throws DataException {
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, "DataBlocks");
        assertEquals("@234234",
                tdo.fromCollection("Tests").
                get("Common").get("ref data").getValue());
    }

    @Test
    public void getNotValuedValue() throws DataException {
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, "DataBlocks");
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

        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, wrongCollection);
    }

    @Test
    public void failWithWrongPath() throws DataException {
        String collection = "DataBlocks";
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, collection);
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
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, collection);
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
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, collection);

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
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, collection);
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
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, collection);
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(tdo.get("Common.gendata").getValue(), tdo.get("Common").get("gendata").getValue());

    }

    @Test
    public void genDataDifferentCollectionsSamePath() throws DataException {
        String collection = "DataBlocks";
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, collection);
        tdo.applyGenerator(SampleDataGensCallback.class);

        String genGenOrgigin = tdo.get("Common.gen gen.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));

        tdo = tdo.fromCollection("Tests");

        assertEquals(genGenOrgigin, tdo.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(genGenOrgigin, tdo.get("Common.gen gen.gendata").getValue());
    }

    @Test
    public void genDataDifferentCollectionWithRefId() throws DataException {
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, "DataBlocks").
                fromCollection("DataBlocks", "57a94a160a279ec293f61665");
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gen gen.gendata").getValue();
        String genDataTarget = tdo.fromCollection("Tests").
                get("Common.ref data gen").getValue();
        assertEquals(genGenOrgigin, genDataTarget);
    }

    @Test
    public void dataDifferentCollectionWithRefId() throws DataException {
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, "DataBlocks");
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
        TestDataObject tdo = new MongoDataObjectAdaptor(mongoDb, collection);

        String cyclicObject = "{ \"value\" : { \"path\" : \"Common.failCyclicReferenceDifferentCollectionWithRefId\", "
                + "\"collection\" : \"DataBlocks\", \"refId\" : { \"$oid\" : \"57a94a160a279ec293f61665\" } } }";
        expectDataExceptions
                .expect(CyclicReferencesExeption.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        tdo.get(cyclicPath).getValue();
    }

    @Test
    public void getRefAsObject() throws DataException {
        TestDataObject originalTdo = new MongoDataObjectAdaptor(mongoDb, "DataBlocks").
                fromCollection("DataBlocks", "57a94a160a279ec293f61665").get("Common");
        TestDataObject referencedTdo = new MongoDataObjectAdaptor(mongoDb, "Tests").
                get("Common.ref object data").getReference();
        assertEquals(originalTdo.toString(), referencedTdo.toString());
    }

    @Test
    public void failRefAsObject() throws DataException {
        String collection = "DataBlocks";
        String path = "testId";
        MongoDataObjectAdaptor tdo = new MongoDataObjectAdaptor(mongoDb, collection);
        expectDataExceptions.expect(ReferenceException.class);
        expectDataExceptions.expectMessage(String.format("There is no reference in \"%s.%s\". Collection \"%s\"",
                collection, path, collection));
        tdo.get(path).getReference();
    }
}
