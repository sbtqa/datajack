package ru.sbtqa.tag.datajack.providers.json;

import com.mongodb.BasicDBObject;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.providers.AbstractDataProvider;
import ru.sbtqa.tag.datajack.exceptions.*;

import java.io.File;
import java.io.IOException;

import static com.mongodb.BasicDBObject.parse;
import static java.io.File.separator;
import static org.apache.commons.io.FileUtils.readFileToString;

public class JsonDataProvider extends AbstractDataProvider {

    private static final String DEFAULT_EXTENSION = "json";
    private static final String REF_TPL = "$ref";
    private final String extension;
    private String testDataFolder;

    /**
     * Create JsonDataProvider instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName json file name
     * @throws DataException if file not found in testDataFolder
     */
    public JsonDataProvider(String testDataFolder, String collectionName) throws DataException {
        this.extension = DEFAULT_EXTENSION;
        String json = readFile(testDataFolder, collectionName);

        BasicDBObject parsed = parse(json);
        this.testDataFolder = testDataFolder;
        this.basicObject = parsed;
        this.collectionName = collectionName;
    }

    /**
     * Create JsonDataProvider instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName json file name
     * @param extension custom file extension
     * @throws DataException if file not found in testDataFolder
     */
    public JsonDataProvider(String testDataFolder, String collectionName, String extension) throws DataException {
        this.extension = extension;

        String json = readFile(testDataFolder, collectionName);

        BasicDBObject parsed = parse(json);
        this.testDataFolder = testDataFolder;
        this.basicObject = parsed;
        this.collectionName = collectionName;
    }

    /**
     * Internal use only for provider overriding purposes
     *
     * @param testDataFolder path to data folder
     * @param obj basic object
     * @param collectionName file name
     * @param extension custom file extension
     */
    private JsonDataProvider(String testDataFolder, BasicDBObject obj, String collectionName, String extension) {
        this.extension = extension;
        this.testDataFolder = testDataFolder;
        this.basicObject = obj;
        this.collectionName = collectionName;
    }

    /**
     * Internal use only for provider overriding purposes
     *
     * @param testDataFolder path to data folder
     * @param obj basic object
     * @param collectionName file name
     * @param way complex path to value
     * @param extension custom file extension
     */
    private JsonDataProvider(String testDataFolder, BasicDBObject obj, String collectionName, String way, String extension) {
        this.extension = extension;
        this.testDataFolder = testDataFolder;
        this.basicObject = obj;
        this.way = way;
        this.collectionName = collectionName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonDataProvider createInstance(String collectionName) throws DataException {
        return new JsonDataProvider(testDataFolder, collectionName, extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonDataProvider createInstance(BasicDBObject obj, String collectionName, String way) {
        return new JsonDataProvider(testDataFolder, obj, collectionName, way, extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JsonDataProvider createInstance(BasicDBObject obj, String collectionName) {
        return new JsonDataProvider(testDataFolder, obj, collectionName, extension);
    }

    /**
     * {@inheritDoc}*
     */
    @Override
    public boolean isReference(BasicDBObject basicDBObject) {
        Object value = basicDBObject.get(REF_TPL);
        return value instanceof String;
    }

    public TestDataProvider getReference() throws DataException {
        if (isReference(this.basicObject)) {
            if (this.rootObject == null) {
                this.rootObject = this.basicObject;
            } else {
                String rootJson = this.rootObject.toJson();
                String baseJson = this.basicObject.toJson();
                if (rootJson.equals(baseJson)) {
                    throw new CyclicReferencesException("Cyclic references in database:\n" + rootJson);
                }
            }
            String refValue = this.basicObject.getString(REF_TPL);
            String referencedCollection = refValue.contains(":") ? refValue.split(":")[0] : this.collectionName;
            String collectionPrefix = refValue.startsWith("/") ? "" :this.collectionName.substring(0, this.collectionName.lastIndexOf("/") + 1);
            this.path = refValue.contains(":") ? refValue.split(":")[1] : refValue;
            AbstractDataProvider reference = (AbstractDataProvider) this.fromCollection(collectionPrefix + referencedCollection);
            reference.setRootObject(this.rootObject, collectionPrefix + referencedCollection + "." + this.path);
            return reference.get(this.path);
        } else {
            throw new ReferenceException(String.format("There is no reference in \"%s\". Collection \"%s\"",
                    this.path, this.collectionName));
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TestDataProvider fromCollection(String collName) throws DataException {
        String json = readFile(this.testDataFolder, collName);
        BasicDBObject parsed = parse(json);
        AbstractDataProvider dataProvider = createInstance(parsed, collName);
        dataProvider.applyGenerator(this.callback);
        return dataProvider;
    }

    private String readFile(String testDataFolder, String collectionName) throws CollectionNotFoundException {
        try {
            File targetFile = new File(testDataFolder + separator + collectionName + "." + this.extension);
            return readFileToString(targetFile, "UTF-8");
        } catch (IOException ex) {
            throw new CollectionNotFoundException(String.format("File %s.json not found in %s",
                    collectionName, testDataFolder), ex);
        }
    }
}