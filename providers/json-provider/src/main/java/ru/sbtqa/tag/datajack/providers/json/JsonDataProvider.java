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
            return readFileToString(new File(testDataFolder + separator + collectionName + "." + this.extension), "UTF-8");
        } catch (IOException ex) {
            throw new CollectionNotFoundException(String.format("File %s.json not found in %s",
                    collectionName, testDataFolder), ex);
        }
    }

}
