package ru.sbtqa.tag.datajack.testdata;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.sbtqa.tag.datajack.callback.SampleDataGensCallback;
import ru.sbtqa.tag.datajack.TestDataObject;
import ru.sbtqa.tag.datajack.adaptors.ExcelDataObjectAdaptor;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesExeption;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;

import java.io.IOException;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.rules.ExpectedException.none;

/**
 * 
 * Created by sbt-anikeev-ae on 30.08.2016.
 */
public class ExcellDataTest {

    private final String excellDataPath = "src/test/resources/excell/TestData";
    private final String collectionName = "Tests";

    @Rule
    public ExpectedException expectDataExceptions = none();

    @Test
    public void getReferenceTest() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        assertEquals("value",
                tdo.get("Common.linkWithValue").getValue());
    }

    @Test
    public void valuePathTest() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        assertEquals("justSomeTestId",
                tdo.get("Common").get("id").getValue());
    }

    @Test
    public void getFromAnotherCollectionTest() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        assertEquals("HELLO WORLD",
                tdo.fromCollection("DataBlocks").
                        get("AnotherObject").get("anotherValue").getValue());
    }

    @Test
    public void getNotValuedValueTest() throws DataException, IOException, InvalidFormatException {
        // TODO: 02.09.2016 is that r'ly actual for xls? Looks like not, cus all values there are strings
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        assertEquals("20.91",
                tdo.get("Common.price").getValue());
    }

    @Test
    public void failWithWrongPath() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        String wrongPath = "Common.password.paww";
        expectDataExceptions
                .expect(FieldNotFoundException.class
                );
        expectDataExceptions.expectMessage(format("Field 'password' in 'Common' object on sheet '%s' " +
                        "is not an object. Cannot find any nested fields inside it", collectionName));

        tdo.get(wrongPath).getValue();
    }

    @Test
    public void failWithWrongGetGetPath() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        String wrongPath = "Common.password.paww";
        expectDataExceptions
                .expect(FieldNotFoundException.class);
        expectDataExceptions.expectMessage("Sheet 'Tests' doesn't contain 'paww' field in path 'Tests.Common.password'");
        tdo.get("Common").get("password").get("paww");
    }

    @Test
    public void failWithCyclicReference() throws DataException, IOException, InvalidFormatException {
        String cyclicPath = "Common.cyclic";
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        String cyclicObject = "{ \"value\" : { \"sheetName\" : \"DataBlocks\", "
                + "\"path\" : \"AnotherObject.cyclicRef\" }";
        expectDataExceptions
                .expect(CyclicReferencesExeption.class);
        expectDataExceptions.expectMessage(format("Cyclic references in database:\n%s", cyclicObject));

        tdo.get(cyclicPath).getValue();
    }

    @Test
    public void genDataSameCollectionTest() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrigin = tdo.get("Uncommon.reftestGen").getValue();
        assertFalse("Generator is not applied", genGenOrigin.contains("generate:"));
        assertEquals(genGenOrigin, tdo.get("Common.linkToGenerator").getValue());
        assertEquals(genGenOrigin, tdo.get("Uncommon").get("reftestGen").getValue());
        assertEquals(tdo.get("Common.linkToGenerator").getValue(), tdo.get("Common").get("linkToGenerator").getValue());
    }

    @Test
    public void genDataDifferentCollections() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        assertEquals(genGenOrgigin, tdo.get("Common").get("gendata").getValue());
    }

    @Test
    public void genDataDifferentCollectionsReference() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        tdo.applyGenerator(SampleDataGensCallback.class);
        String genGenOrgigin = tdo.get("Common.gendata").getValue();
        assertFalse("Generator is not applied", genGenOrgigin.contains("generate:"));
        tdo = tdo.fromCollection("DataBlocks");

        assertEquals(genGenOrgigin, tdo.get("AnotherObject").get("generatedInOtherCollection").getValue());
        assertEquals(genGenOrgigin, tdo.get("AnotherObject.generatedInOtherCollection").getValue());
    }

    @Test
    public void getRefAsObject() throws DataException, IOException, InvalidFormatException {
        TestDataObject originalTdo = new ExcelDataObjectAdaptor(this.excellDataPath, "DataBlocks").get("NewObject");
        TestDataObject referencedTdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName).
                get("Common.ref object data").getReference();
        assertEquals(originalTdo.toString(), referencedTdo.toString());
    }

    @Test
    public void failRefAsObject() throws DataException, IOException, InvalidFormatException {
        String path = "Common.id";
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, collectionName);
        expectDataExceptions.expect(ReferenceException.class);
        expectDataExceptions.expectMessage(String.format("There is no reference in '%s.%s'. Collection '%s'",
                collectionName, path, collectionName));
        tdo.get(path).getReference();
    }

    @Test
    public void generatorCacheDiffFiles() throws DataException, IOException, InvalidFormatException {
        TestDataObject tdo = new ExcelDataObjectAdaptor(this.excellDataPath, this.collectionName);
        TestDataObject tdoNew = new ExcelDataObjectAdaptor("src/test/resources/excell/TestDataNew", this.collectionName);
        tdo.applyGenerator(SampleDataGensCallback.class);
        tdoNew.applyGenerator(SampleDataGensCallback.class);
        assertNotEquals(tdo.get("Common.gendata").getValue(), tdoNew.get("Common.gendata").getValue());
    }
}
