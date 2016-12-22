package ru.sbtqa.tag.datajack.adaptors;

import com.mongodb.BasicDBObject;
import static com.mongodb.BasicDBObject.parse;
import java.io.File;
import static java.io.File.separator;
import java.io.IOException;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.readFileToString;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbtqa.tag.datajack.TestDataObject;
import ru.sbtqa.tag.datajack.callback.CallbackData;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.CollectionNotfoundException;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesExeption;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.GeneratorException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;

public class JsonDataObjectAdaptor extends AbstractDataObjectAdaptor implements TestDataObject {

    private static final Logger LOG = LoggerFactory.getLogger(ExcelDataObjectAdaptor.class);
    private final String collectionName;
    private final String testDataFolder;

    /**
     * Create JsonDataObjectAdaptor instance
     *
     * @param testDataFolder path to data folder
     * @param collectionName json file name
     * @throws DataException if file not found in testDataFolder
     */
    public JsonDataObjectAdaptor(String testDataFolder, String collectionName) throws DataException {
        String json;
        try {
            json = readFileToString(new File(testDataFolder + separator + collectionName + ".json"));
        } catch (IOException ex) {
            throw new CollectionNotfoundException(String.format("File %s.json not found in %s",
                    collectionName, testDataFolder), ex);
        }
        BasicDBObject parsed = (BasicDBObject) parse(json);
        this.testDataFolder = testDataFolder;
        this.basicObj = parsed;
        this.collectionName = collectionName;
    }

    private JsonDataObjectAdaptor(String testDataFolder, BasicDBObject obj, String collectionName) {
        this.testDataFolder = testDataFolder;
        this.basicObj = obj;
        this.collectionName = collectionName;
    }

    private JsonDataObjectAdaptor(String testDataFolder, BasicDBObject obj, String collectionName, String way) {
        this.testDataFolder = testDataFolder;
        this.basicObj = obj;
        this.way = way;
        this.collectionName = collectionName;
    }

    @Override
    public JsonDataObjectAdaptor fromCollection(String collName) throws DataException {
        try {
            String json = readFileToString(new File(this.testDataFolder + separator + collName + ".json"));
            BasicDBObject parsed = (BasicDBObject) parse(json);
            JsonDataObjectAdaptor newObj = new JsonDataObjectAdaptor(this.testDataFolder, parsed, collName);
            newObj.applyGenerator(this.callback);
            return newObj;
        } catch (IOException ex) {
            throw new CyclicReferencesExeption("There is no file with " + collName + ".json name", ex);
        }

    }

    @Override
    public TestDataObject get(String key) throws DataException {
        this.way = key;
        JsonDataObjectAdaptor tdo;

        if (key.contains(".")) {
            String[] keys = key.split("[.]");
            StringBuilder partialBuilt = new StringBuilder();
            BasicDBObject basicO = this.basicObj;
            for (String partialKey : keys) {
                partialBuilt.append(partialKey);
                if (!(basicO.get(partialKey) instanceof BasicDBObject)) {
                    if (null == basicO.get(partialKey)) {
                        throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                                this.collectionName, partialKey, partialBuilt.toString()));
                    }
                    break;
                }
                basicO = (BasicDBObject) basicO.get(partialKey);
                partialBuilt.append(".");
            }

            tdo = new JsonDataObjectAdaptor(this.testDataFolder, basicO, this.collectionName, this.way);
            tdo.applyGenerator(this.callback);
            tdo.setRootObj(this.rootObj, this.collectionName + "." + key);
            return tdo;
        }
        if (!basicObj.containsField(key)) {
            throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field in path \"%s\"",
                    this.collectionName, key, this.path));
        }
        Object result = this.basicObj.get(key);
        if (!(result instanceof BasicDBObject)) {
            result = new BasicDBObject(key, result);
        }
        tdo = new JsonDataObjectAdaptor(this.testDataFolder, (BasicDBObject) result, this.collectionName, this.way);
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
            String result = this.basicObj.getString("value");
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
    public TestDataObject getReference() throws DataException {
        if (null != this.basicObj.get("value") && !(this.basicObj.get("value") instanceof String)
                && ((BasicDBObject) this.basicObj.get("value")).containsField("collection")
                && ((BasicDBObject) this.basicObj.get("value")).containsField("path")) {
            if (this.rootObj == null) {
                this.rootObj = this.basicObj;
            } else {
                String rootJson = this.rootObj.toJson();
                String baseJson = this.basicObj.toJson();
                if (rootJson.equals(baseJson)) {
                    throw new CyclicReferencesExeption("Cyclic references in database:\n" + rootJson);
                }
            }
            String referencedCollection = ((BasicBSONObject) this.basicObj.get("value")).getString("collection");
            this.path = ((BasicBSONObject) this.basicObj.get("value")).getString("path");
            JsonDataObjectAdaptor reference = this.fromCollection(((BasicBSONObject) this.basicObj.get("value")).getString("collection"));
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
}
