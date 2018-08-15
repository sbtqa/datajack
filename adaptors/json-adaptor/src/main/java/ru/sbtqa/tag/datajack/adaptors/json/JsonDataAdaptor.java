package ru.sbtqa.tag.datajack.adaptors.json;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.adaptors.AbstractDataObjectAdaptor;
import ru.sbtqa.tag.datajack.callback.CallbackData;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.*;

import java.io.File;
import java.io.IOException;

import static com.mongodb.BasicDBObject.parse;
import static java.io.File.separator;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.readFileToString;

public class JsonDataAdaptor extends AbstractDataObjectAdaptor implements TestDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JsonDataAdaptor.class);
    private static final String DEFAULT_EXTENSION = "json";
    protected String collectionName;
    protected String testDataFolder;
    protected String extension;

    /**
     * Create JsonDataAdaptor instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName json file name
     * @throws DataException if file not found in testDataFolder
     */
    public JsonDataAdaptor(String testDataFolder, String collectionName) throws DataException {
        this.extension = DEFAULT_EXTENSION;
        String json = readFile(testDataFolder, collectionName);

        BasicDBObject parsed = parse(json);
        this.testDataFolder = testDataFolder;
        this.basicObj = parsed;
        this.collectionName = collectionName;
    }

    /**
     * Create JsonDataAdaptor instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName json file name
     * @param extension      custom file extension
     * @throws DataException if file not found in testDataFolder
     */
    public JsonDataAdaptor(String testDataFolder, String collectionName, String extension) throws DataException {
        this.extension = extension;

        String json = readFile(testDataFolder, collectionName);

        BasicDBObject parsed = parse(json);
        this.testDataFolder = testDataFolder;
        this.basicObj = parsed;
        this.collectionName = collectionName;
    }

    /**
     * Internal use only for adaptor overriding purposes
     *
     * @param testDataFolder path to data folder
     * @param obj            basic object
     * @param collectionName file name
     * @param extension      custom file extension
     */
    protected JsonDataAdaptor(String testDataFolder, BasicDBObject obj, String collectionName, String extension) {
        this.extension = extension;
        this.testDataFolder = testDataFolder;
        this.basicObj = obj;
        this.collectionName = collectionName;
    }

    /**
     * Internal use only for adaptor overriding purposes
     *
     * @param testDataFolder path to data folder
     * @param obj            basic object
     * @param collectionName file name
     * @param way            complex path to value
     * @param extension      custom file extension
     */
    protected JsonDataAdaptor(String testDataFolder, BasicDBObject obj, String collectionName, String way, String extension) {
        this.extension = extension;
        this.testDataFolder = testDataFolder;
        this.basicObj = obj;
        this.way = way;
        this.collectionName = collectionName;
    }

    public static boolean isArray(String key) {
        return key.matches("(.+\\[\\d+\\])");
    }

    /**
     * Internal use only for adaptor overriding purposes
     *
     * @param <T>            Overrider type
     * @param testDataFolder path to data folder
     * @param obj            Basic object
     * @param collectionName file name
     * @param way            complex path to value
     * @return
     */
    protected <T extends JsonDataAdaptor> T privateInit(String testDataFolder, BasicDBObject obj, String collectionName, String way) {
        return (T) new JsonDataAdaptor(testDataFolder, obj, collectionName, way, extension);
    }

    /**
     * Internal use only for adaptor overriding purposes
     *
     * @param <T>            Overrider type
     * @param testDataFolder path to data folder
     * @param obj            basic object
     * @param collectionName file name
     * @return
     */
    protected <T extends JsonDataAdaptor> T privateInit(String testDataFolder, BasicDBObject obj, String collectionName) {
        return (T) new JsonDataAdaptor(testDataFolder, obj, collectionName, extension);
    }

    @Override
    public JsonDataAdaptor fromCollection(String collName) throws DataException {
        String json = readFile(this.testDataFolder, collName);
        BasicDBObject parsed = parse(json);
        JsonDataAdaptor newObj = privateInit(this.testDataFolder, parsed, collName);
        newObj.applyGenerator(this.callback);
        return newObj;
    }

    @Override
    public TestDataProvider get(String key) throws DataException {
        this.way = key;
        return key.contains(".") ? getComplex(key) : getSimple(key);

    }

    private TestDataProvider getSimple(String key) throws FieldNotFoundException, DataException {
        Object result;

        if (isArray(key)) {
            result = parseArray(basicObj, key);
        } else {

            if (!basicObj.containsField(key)) {
                throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field in path \"%s\"",
                        this.collectionName, key, this.path));
            }
            result = this.basicObj.get(key);
        }
        if (!(result instanceof BasicDBObject)) {
            result = new BasicDBObject(key, result);
        }
        JsonDataAdaptor tdo = privateInit(this.testDataFolder, (BasicDBObject) result, this.collectionName, this.way);
        tdo.applyGenerator(this.callback);

        String rootObjValue;
        if (this.path != null) {
            rootObjValue = this.path + "." + key;
        } else {
            rootObjValue = this.collectionName + "." + key;
        }
        tdo.setRootObj(this.rootObj, rootObjValue);
        return tdo;
    }

    private TestDataProvider getComplex(String key) throws FieldNotFoundException, DataException {

        JsonDataAdaptor tdo = privateInit(this.testDataFolder, parseComplexDBObject(key), this.collectionName, this.way);
        tdo.applyGenerator(this.callback);
        tdo.setRootObj(this.rootObj, this.collectionName + "." + key);

        return tdo;
    }

    private BasicDBObject parseComplexDBObject(String key) throws DataException {
        String[] keys = key.split("[.]");
        StringBuilder partialBuilt = new StringBuilder();
        BasicDBObject basicObject = this.basicObj;

        for (String partialKey : keys) {
            partialBuilt.append(partialKey);

            if (isArray(partialKey)) {
                basicObject = (BasicDBObject) parseArray(basicObject, partialKey);
                continue;
            }

            if (!(basicObject.get(partialKey) instanceof BasicDBObject)) {
                if (null == basicObject.get(partialKey)) {
                    throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                            this.collectionName, partialKey, partialBuilt.toString()));
                }
                break;
            }

            basicObject = (BasicDBObject) basicObject.get(partialKey);
            partialBuilt.append(".");
        }
        return basicObject;
    }

    private Object parseArray(BasicDBObject basicO, String key) throws DataException {
        if (!isArray(key)) {
            throw new DataException(String.format("%s.%s is not an array!", this.collectionName, key));
        }
        String arrayKey = key.split("\\[")[0];
        String arrayIndex = key.split("\\[")[1].split("\\]")[0];
        Object listCandidate = basicO.get(arrayKey);

        if (!(listCandidate instanceof BasicDBList)) {
            throw new DataException(String.format("%s.%s is not an array!", this.collectionName, key));
        }

        return ((BasicDBList) listCandidate).get(arrayIndex);
    }

    @Override
    public String toString() {
        if (this.basicObj == null) {
            return "";
        }
        return this.basicObj.toString();
    }

    private void setRootObj(BasicDBObject obj, String path) {
        this.rootObj = obj;
        this.path = path;
    }

    @Override
    public String getValue() throws DataException {
        try {
            return this.getReference().getValue();
        } catch (ReferenceException e) {
            LOG.debug("Reference not found", e);
            String result = this.basicObj.getString(VALUE_TPL);
            if (result == null) {
                if (this.way.contains(".")) {
                    this.way = this.way.split("[.]")[this.way.split("[.]").length - 1];
                }
                result = this.basicObj.getString(this.way);
            }
            if (this.callback != null) {
                CallbackData generatorParams = new CallbackData(this.path, result);
                Object callbackResult = null;
                try {
                    callbackResult = callback.newInstance().call(generatorParams);
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new GeneratorException("Could not initialize callback", ex);
                }
                if (callbackResult instanceof Exception) {
                    throw (GeneratorException) callbackResult;
                } else {
                    result = (String) callbackResult;
                }
            }
            return result;
        }
    }

    @Override
    public TestDataProvider getReference() throws DataException {
        if (null != this.basicObj.get(VALUE_TPL) && !(this.basicObj.get(VALUE_TPL) instanceof String)
                && ((BasicDBObject) this.basicObj.get(VALUE_TPL)).containsField(COLLECTION_TPL)
                && ((BasicDBObject) this.basicObj.get(VALUE_TPL)).containsField("path")) {
            if (this.rootObj == null) {
                this.rootObj = this.basicObj;
            } else {
                String rootJson = this.rootObj.toJson();
                String baseJson = this.basicObj.toJson();
                if (rootJson.equals(baseJson)) {
                    throw new CyclicReferencesExeption("Cyclic references in database:\n" + rootJson);
                }
            }
            String referencedCollection = ((BasicBSONObject) this.basicObj.get(VALUE_TPL)).getString(COLLECTION_TPL);
            this.path = ((BasicBSONObject) this.basicObj.get(VALUE_TPL)).getString("path");
            JsonDataAdaptor reference = this.fromCollection(((BasicBSONObject) this.basicObj.get(VALUE_TPL)).getString(COLLECTION_TPL));
            reference.setRootObj(this.rootObj, referencedCollection + "." + this.path);
            return reference.get(this.path);
        } else {
            throw new ReferenceException(String.format("There is no reference in \"%s\". Collection \"%s\"",
                    this.path, this.collectionName));
        }
    }

    @Override
    public void applyGenerator(Class<? extends GeneratorCallback> callback) {
        this.callback = callback;
    }

    protected String readFile(String testDataFolder, String collectionName) throws CollectionNotfoundException {
        try {
            return readFileToString(new File(testDataFolder + separator + collectionName + "." + this.extension), "UTF-8");
        } catch (IOException ex) {
            throw new CollectionNotfoundException(String.format("File %s.json not found in %s",
                    collectionName, testDataFolder), ex);
        }
    }

    @Override
    public boolean isReference() throws DataException {
        Object value = this.basicObj.get("value");
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField("collection") && ((BasicDBObject) value).containsField("path");
    }
}
