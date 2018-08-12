package ru.sbtqa.tag.datajack.adaptors;

import com.mongodb.BasicDBObject;
import static com.mongodb.BasicDBObject.parse;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import static com.mongodb.QueryBuilder.start;
import static java.lang.String.format;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.bson.types.ObjectId;
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

public class MongoDataObjectAdaptor extends AbstractDataObjectAdaptor implements TestDataObject {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDataObjectAdaptor.class);
    private static final String REF_ID_TPL = "refId";
    private final DB db;
    private DBCollection coll;

    /**
     * Initialize new data object
     *
     * @param db Database object
     * @param coll Collection
     * @throws DataException if no collection or its empty
     */
    public MongoDataObjectAdaptor(DB db, String coll) throws DataException {
        this.db = db;
        this.coll = this.db.getCollection(coll);
        if (this.coll.count() == 0) {
            throw new CollectionNotfoundException(String.format("There is no \"%s\" collection or it's empty", coll));
        }
        this.way = coll;

    }

    private MongoDataObjectAdaptor(DB db, BasicDBObject obj, String way) {
        this.db = db;
        this.basicObj = obj;
        this.way = way;
    }

    @Override
    public MongoDataObjectAdaptor fromCollection(String collectionName) throws DataException {
        MongoDataObjectAdaptor tdo = new MongoDataObjectAdaptor(this.db, collectionName);
        tdo.applyGenerator(this.callback);
        return tdo;

    }

    /**
     *
     * @param collectionName mongodb collection
     * @param refId document id to reference to
     * @return test data object
     * @throws DataException if no collection or its empty
     */
    public MongoDataObjectAdaptor fromCollection(String collectionName, String refId) throws DataException {
        this.coll = db.getCollection(collectionName);
        DBObject referenceDocument = this.coll.findOne(new BasicDBObject("_id", new ObjectId(refId)));
        MongoDataObjectAdaptor tdo = new MongoDataObjectAdaptor(this.db, collectionName);
        tdo.basicObj = (BasicDBObject) referenceDocument;
        tdo.path = refId + "." + collectionName;
        tdo.applyGenerator(this.callback);
        return tdo;

    }

    @Override
    public MongoDataObjectAdaptor get(String key) throws DataException {
        this.way = key;
        String documentId = "HERE IS MUST BE ID";
        MongoDataObjectAdaptor tdo;
        if (this.basicObj == null) {

            try (DBCursor cursor = coll.find(start(key).exists(true).get()).limit(1).sort((DBObject) parse("{$natural:-1}"))) {
                if (cursor.hasNext()) {
                    this.basicObj = (BasicDBObject) cursor.next();
                    documentId = this.basicObj.getString("_id");
                } else {
                    throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                            this.coll.getName(), key.split("[.]")[key.split("[.]").length - 1], key));
                }
            }
        }
        if (key.contains(".")) {
            String[] keys = key.split("[.]");
            BasicDBObject basicO = this.basicObj;
            for (String partialKey : keys) {
                if (!(basicO.get(partialKey) instanceof BasicDBObject)) {
                    break;
                }
                basicO = (BasicDBObject) basicO.get(partialKey);
            }

            tdo = new MongoDataObjectAdaptor(this.db, basicO, this.way);

            if (this.path == null || (this.coll != null && this.path.equals(this.coll.getName()))) {
                this.path = documentId + "." + this.coll.getName();
            } else if (this.coll == null) {
                this.path = documentId + "." + this.path;
            }
            tdo.applyGenerator(this.callback);
            tdo.coll = coll;
            tdo.setRootObj(this.rootObj, this.path + "." + key);
            return tdo;
        }
        if (!basicObj.containsField(key)) {
            throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field in path \"%s\"",
                    this.coll.getName(), key, this.path));
        }
        Object result = basicObj.get(key);
        if (!(result instanceof BasicDBObject)) {
            result = new BasicDBObject(key, result);
        }

        tdo = new MongoDataObjectAdaptor(this.db, (BasicDBObject) result, this.way);

        String rootObjValue;
        if (this.path != null) {
            rootObjValue = this.path + "." + key;
        } else {
            rootObjValue = this.coll.getName() + "." + key;
        }
        tdo.applyGenerator(this.callback);
        tdo.coll = coll;
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
        if (obj != null && obj.containsField(VALUE_TPL)
                && ((BasicDBObject) obj.get(VALUE_TPL)).containsField(REF_ID_TPL)) {
            String colName = "";
            if (this.coll != null) {
                colName = "." + this.coll.getName();
            }
            this.path = ((BasicBSONObject) obj.get(VALUE_TPL)).getString(REF_ID_TPL) + colName + "." + this.way;
        } else {
            this.path = path;
        }
        this.rootObj = obj;
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
    public TestDataObject getReference() throws DataException {
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
            MongoDataObjectAdaptor reference;
            if (((BSONObject) this.basicObj.get(VALUE_TPL)).containsField(REF_ID_TPL)) {
                String refId = ((BasicBSONObject) this.basicObj.get(VALUE_TPL)).getString(REF_ID_TPL);
                reference = this.fromCollection(referencedCollection, refId);
            } else {
                reference = this.fromCollection(referencedCollection);
            }

            reference.setRootObj(this.rootObj, referencedCollection);
            return reference.get(this.path);
        } else {
            throw new ReferenceException(String.format("There is no reference in \"%s\". Collection \"%s\"",
                    this.path, this.coll.getName()));
        }
    }

    @Override
    public void applyGenerator(Class<? extends GeneratorCallback> callback) {
        this.callback = callback;
    }

    @Override
    public boolean isReference() throws DataException {
        Object value = this.basicObj.get(VALUE_TPL);
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField(COLLECTION_TPL) && ((BasicDBObject) value).containsField("path");
    }
}
