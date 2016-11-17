package ru.sbtqa.tag.datajack.testdata;

import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.junit.rules.ExpectedException.none;
import static ru.sbtqa.tag.datajack.callback.SampleDataCache.getCache;
import ru.sbtqa.tag.datajack.callback.SampleDataGensCallback;
import ru.sbtqa.tag.datajack.TestDataObject;
import ru.sbtqa.tag.datajack.adaptors.JsonDataObjectAdaptor;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesExeption;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;

public class JsonDataTest {

    private final String jsonDataPath = "src/test/resources/json";

    /**
     *
     */
    @Before
    public void setUp() {
        getCache().clear();
    }

    /**
     *
     */
    @Rule
    public ExpectedException expectDataExceptions = none();

    @Test
    public void getReferenceTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        assertEquals("123qwe",
                tdo.get("Common.password2").getValue());
    }

    @Test
    public void valuePathTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        assertEquals("Params Group 1.password",
                tdo.get("Common").get("password2.value.path").getValue());
    }

    @Test
    public void getFromAnotherCollectionTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        assertEquals("123",
                tdo.fromCollection("Tests").
                get("dataBlocks").get("Common").get("password").getValue());
    }

    @Test
    public void getNotValuedValueTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        assertEquals("20.91",
                tdo.get("Common.price").getValue());
    }

    @Test
    public void failWithWrongPath() throws DataException {
        String collectionName = "DataBlocks";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        String wrongPath = "Common.password.paww";
        expectDataExceptions
                .expect(FieldNotFoundException.class
                );
        expectDataExceptions.expectMessage(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                collectionName, wrongPath.split("[.]")[wrongPath.split("[.]").length - 1], wrongPath));

        tdo.get(wrongPath).getValue();
    }

    @Test
    public void failWithWrongGetGetPath() throws DataException {
        String collection = "DataBlocks";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collection);
        expectDataExceptions
                .expect(FieldNotFoundException.class);
        expectDataExceptions.expectMessage("Collection \"DataBlocks\" doesn't contain \"paww\" "
                + "field in path \"DataBlocks.Common.password\"");
        tdo.get("Common").get("password").get("paww");
    }

    @Test
    public void failWithCyclicReference() throws DataException {
        String collectionName = "DataBlocks";
        String cyclicPath = "Common.cyclic";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        String cyclicObject = format("{ \"value\" : { \"collection\" : \"%s\", "
                + "\"path\" : \"Common.cyclic\" }, \"comment\" : \"Cyclic\"", collectionName);
        expectDataExceptions
                .expect(CyclicReferencesExeption.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        tdo.get(cyclicPath).getValue();
    }

    @Test
    public void genDataSameCollectionTest() throws DataException {
        String collectionName = "DataBlocks";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gen gen.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, tdo.get("Common.gendata reference").getValue());
        assertEquals(genGenOrgigin, tdo.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(tdo.get("Common.gendata").getValue(), tdo.get("Common").get("gendata").getValue());

    }

    @Test
    public void genDataDifferentCollections() throws DataException {
        String collectionName = "Tests";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, tdo.get("Common").get("gendata").getValue());

    }

    @Test
    public void genDataDifferentCollectionsReference() throws DataException {
        String collectionName = "DataBlocks";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collectionName);
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gen gen.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        tdo = tdo.fromCollection("Tests");

        assertEquals(genGenOrgigin, tdo.get("Common").get("gen gen").get("gendata").getValue());
        assertEquals(genGenOrgigin, tdo.get("Common.gen gen.gendata").getValue());
    }

    @Test
    public void getRefAsObject() throws DataException {
        TestDataObject originalTdo = new JsonDataObjectAdaptor(this.jsonDataPath, "DataBlocks").get("Common");
        TestDataObject referencedTdo = new JsonDataObjectAdaptor(this.jsonDataPath, "Tests").
                get("Common.ref object data").getReference();
        assertEquals(originalTdo.toString(), referencedTdo.toString());
    }

    @Test
    public void failRefAsObject() throws DataException {
        String collection = "DataBlocks";
        String path = "testId";
        TestDataObject tdo = new JsonDataObjectAdaptor(this.jsonDataPath, collection);
        expectDataExceptions.expect(ReferenceException.class);
        expectDataExceptions.expectMessage(String.format("There is no reference in \"%s.%s\". Collection \"%s\"",
                collection, path, collection));
        tdo.get(path).getReference();
    }
}
