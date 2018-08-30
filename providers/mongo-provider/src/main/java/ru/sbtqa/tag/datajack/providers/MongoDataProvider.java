package ru.sbtqa.tag.datajack.providers;

import com.mongodb.*;
import org.bson.BSONObject;
import org.bson.types.ObjectId;
import ru.sbtqa.tag.datajack.exceptions.*;

public class MongoDataProvider extends AbstractDataProvider {

    private static final String MONGO_ID = "_id";
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
            throw new CollectionNotFoundException(String.format("There is no \"%s\" collection or it's empty", collectionName));
        }
        this.basicObject = (BasicDBObject) collection.find().sort(new BasicDBObject(MONGO_ID, -1)).limit(1).next();
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
            throw new CollectionNotFoundException(String.format("There is no \"%s\" collection or it's empty", collectionName));
        }
        this.basicObject = (BasicDBObject) this.collection.findOne(new BasicDBObject(MONGO_ID, new ObjectId(refId)));
        this.way = collectionName;
        this.collectionName = collectionName;
        this.path = "";

    }

    /**
     * Internal use only
     *
     * @param obj basic object
     * @param collectionName file name
     * @param way complex path to value
     */
    private MongoDataProvider(DB db, BasicDBObject obj, String collectionName, String way) {
        this.db = db;
        this.basicObject = obj;
        this.way = way;
        this.collectionName = collectionName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MongoDataProvider createInstance(BasicDBObject basicObject, String collectionName, String way) {
        return new MongoDataProvider(db, basicObject, collectionName, way);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MongoDataProvider createInstance(BasicDBObject basicObject, String collectionName) {
        return new MongoDataProvider(db, basicObject, collectionName, way);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MongoDataProvider createInstance(String collectionName) throws DataException {
        return new MongoDataProvider(db, collectionName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MongoDataProvider fromCollection(String collName) throws DataException {

        MongoDataProvider dataProvider = createInstance(collName);
        dataProvider.applyGenerator(this.callback);
        return dataProvider;
    }

    /**
     * @param collectionName mongodb collection
     * @param refId document id to reference to
     * @return test data object
     * @throws DataException if no collection or its empty
     */
    public MongoDataProvider fromCollection(String collectionName, String refId) throws DataException {
        this.collection = db.getCollection(collectionName);

        ObjectId id = new ObjectId(refId);
        BasicDBObject obj = new BasicDBObject();
        obj.append(MONGO_ID, id);
        BasicDBObject query = new BasicDBObject();
        query.putAll((BSONObject) query);
        DBObject referenceDocument = collection.findOne(query);

        MongoDataProvider dataProvider = new MongoDataProvider(this.db, collectionName, refId);
        dataProvider.basicObject = (BasicDBObject) referenceDocument;
        dataProvider.path = refId + "." + collectionName;
        dataProvider.applyGenerator(this.callback);
        return dataProvider;

    }

}
