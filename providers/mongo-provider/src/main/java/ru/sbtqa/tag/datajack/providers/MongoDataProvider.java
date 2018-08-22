package ru.sbtqa.tag.datajack.providers;

import com.mongodb.*;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.callback.CallbackData;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.*;

import static com.mongodb.BasicDBObject.parse;
import static com.mongodb.QueryBuilder.start;
import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.readFileToString;

public class MongoDataProvider extends AbstractDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDataProvider.class);
    protected String collectionName;
    private static final String REF_ID_TPL = "refId";
    private final DB db;
    private DBCollection collection;


    /**
     * Initialize new data object
     *
     * @param db Database object
     * @throws DataException if no collection or its empty
     */
    public MongoDataProvider(DB db, String collectionName) throws DataException {
        this.db = db;
        this.collection = this.db.getCollection(collectionName);
        if (this.collection.count() == 0) {
            throw new CollectionNotfoundException(String.format("There is no \"%s\" collection or it's empty", collectionName));
        }
        this.basicObj = (BasicDBObject) collection.find().sort(new BasicDBObject("_id",-1)).limit(1).next();
        this.way = collectionName;
        this.path = collectionName;
        this.collectionName = collectionName;

    }

    /**
     * Initialize new data object
     *
     * @param db Database object
     * @throws DataException if no collection or its empty
     */
    public MongoDataProvider(DB db, String collectionName, String refId) throws DataException {
        this.db = db;
        this.collection = this.db.getCollection(collectionName);
        if (this.collection.count() == 0) {
            throw new CollectionNotfoundException(String.format("There is no \"%s\" collection or it's empty", collectionName));
        }
        this.basicObj = (BasicDBObject) this.collection.findOne(new BasicDBObject("_id", new ObjectId(refId)));
        this.way = collectionName;
        this.collectionName = collectionName;
        this.path="";

    }

    /**
     * Internal use only
     *
     * @param obj            basic object
     * @param collectionName file name
     * @param way            complex path to value
     */
    protected MongoDataProvider(DB db, BasicDBObject obj, String collectionName, String way) {
        this.db = db;
        this.basicObj = obj;
        this.way = way;
        this.collectionName = collectionName;
    }

    public BasicDBObject getBasicDBObject() {
        return this.basicObj;
    }

    public static boolean isArray(String key) {
        return key.matches("(.+\\[\\d+\\])");
    }


    @Override
    public MongoDataProvider fromCollection(String collName) throws DataException {

        MongoDataProvider dataProvider = new MongoDataProvider(this.db, collName);
        dataProvider.applyGenerator(this.callback);
        return dataProvider;
    }

    /**
     * @param collectionName mongodb collection
     * @param refId          document id to reference to
     * @return test data object
     * @throws DataException if no collection or its empty
     */
    public MongoDataProvider fromCollection(String collectionName, String refId) throws DataException {
        this.collection = db.getCollection(collectionName);

        ObjectId id= new ObjectId(refId);
        BasicDBObject obj = new BasicDBObject();
        obj.append("_id", id);
        BasicDBObject query = new BasicDBObject();
        query.putAll((BSONObject)query);
        DBObject referenceDocument = collection.findOne(query);

        MongoDataProvider dataProvider = new MongoDataProvider(this.db, collectionName, refId);
        dataProvider.basicObj = (BasicDBObject) referenceDocument;
        dataProvider.path = refId + "." + collectionName;
        dataProvider.applyGenerator(this.callback);
        return dataProvider;

    }

    @Override
    public TestDataProvider get(String key) throws DataException {
        if (key.isEmpty()) {
            return this;
        }
        this.way = key;
        return key.contains(".") ? getComplex(key) : getSimple(key);

    }

    private TestDataProvider getSimple(String key) throws DataException {
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
        MongoDataProvider dataProvider = new MongoDataProvider(db, (BasicDBObject) result, this.collectionName, this.way);
        dataProvider.applyGenerator(this.callback);

        String rootObjValue;
        if (this.path != null) {
            rootObjValue = this.path + "." + key;
        } else {
            rootObjValue = this.collectionName + "." + key;
        }
        dataProvider.setRootObj(this.rootObj, rootObjValue);
        if (dataProvider.isReference()) {
            return dataProvider.getReference();
        }
        return dataProvider;
    }

    private TestDataProvider getComplex(String key) throws DataException {

        MongoDataProvider dataProvider = new MongoDataProvider(db, parseComplexDBObject(key), this.collectionName, this.way);
        dataProvider.applyGenerator(this.callback);
        dataProvider.setRootObj(this.rootObj, this.collectionName + "." + key);

        return dataProvider;
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

            if (isReference(basicObject)) {
                String collection = ((BasicDBObject) basicObject.get("value")).getString("collection");
                String path = ((BasicDBObject) basicObject.get("value")).getString("path");
                TestDataProvider dataProvider = new MongoDataProvider(db, collection).get(path);
                basicObject = ((MongoDataProvider) dataProvider).basicObj;
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
                if (this.way != null && this.way.contains(".")) {
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
            MongoDataProvider reference = this.fromCollection(((BasicBSONObject) this.basicObj.get(VALUE_TPL)).getString(COLLECTION_TPL));
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

    @Override
    public boolean isReference() throws DataException {
        Object value = this.basicObj.get("value");
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField("collection") && ((BasicDBObject) value).containsField("path");
    }

    public boolean isReference(BasicDBObject basicDBObject) throws DataException {
        Object value = basicDBObject.get("value");
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField("collection") && ((BasicDBObject) value).containsField("path");
    }
}
