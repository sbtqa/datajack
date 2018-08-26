package ru.sbtqa.tag.datajack.providers;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.bson.BasicBSONObject;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.callback.CallbackData;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.*;

import java.util.*;

import static java.lang.String.format;

public abstract class AbstractDataProvider implements TestDataProvider {

    protected static final String VALUE_TPL = "value";
    protected static final String COLLECTION_TPL = "collection";
    protected BasicDBObject basicObject;
    protected String collectionName;
    protected String way;
    protected String path;
    protected Class<? extends GeneratorCallback> callback;
    protected BasicDBObject rootObject;
    private static final String NOT_INITIALIZED_EXCEPTION = "BasicDBObject is not initialized yet";

    public static boolean isArray(String key) {
        return key.matches("(.+\\[\\d+\\])");
    }

    protected abstract <T extends AbstractDataProvider> T createInstance(BasicDBObject basicObject, String collectionName, String way) throws DataException;

    protected abstract <T extends AbstractDataProvider> T createInstance(BasicDBObject basicObject, String collectionName) throws DataException;

    protected abstract <T extends AbstractDataProvider> T createInstance(String collectionName) throws DataException;


    @Override
    public TestDataProvider get(String key) throws DataException {
        if (key.isEmpty()) {
            return this;
        }
        this.way = key;
        return key.contains(".") ? getComplex(key) : getSimple(key);

    }

    public Map<String, Object> toMap() throws DataException {
        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObject.toMap();
    }

    public Set<String> getKeySet() throws DataException {
        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        } else if (getValue() != null) {
            return new HashSet<>();
        }
        if (isReference()) {
            this.rootObject = null;
            return getReference().toMap().keySet();
        } else {
            return basicObject.keySet();
        }
    }

    public Collection<Object> getValues() throws DataException {
        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObject.values();
    }

    public List<String> getStringValues() throws DataException {
        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        Collection<Object> values = basicObject.values();
        List<String> strings = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String
                    || value instanceof Character
                    || value instanceof Integer
                    || value instanceof Float
                    || value instanceof Double
                    || value instanceof Boolean) {
                strings.add(value.toString());
            } else {
                strings.add("");
            }
        }
        return strings;
    }

    private TestDataProvider getSimple(String key) throws DataException {
        Object result;

        if (isArray(key)) {
            result = parseArray(basicObject, key);
        } else {

            if (!basicObject.containsField(key)) {
                throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field in path \"%s\"",
                        this.collectionName, key, this.path));
            }
            result = this.basicObject.get(key);
        }
        if (!(result instanceof BasicDBObject)) {
            result = new BasicDBObject(key, result);
        }
        AbstractDataProvider dataProvider = createInstance((BasicDBObject) result, this.collectionName, this.way);
        dataProvider.applyGenerator(this.callback);

        String rootObjValue;
        if (this.path != null) {
            rootObjValue = this.path + "." + key;
        } else {
            rootObjValue = this.collectionName + "." + key;
        }
        dataProvider.setRootObject(this.rootObject, rootObjValue);
        if (dataProvider.isReference()) {
            return dataProvider.getReference();
        }
        return dataProvider;
    }

    private TestDataProvider getComplex(String key) throws DataException {

        AbstractDataProvider dataProvider = createInstance(parseComplexDBObject(key), this.collectionName, this.way);
        dataProvider.applyGenerator(this.callback);
        dataProvider.setRootObject(this.rootObject, this.collectionName + "." + key);

        return dataProvider;
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

    private BasicDBObject parseComplexDBObject(String key) throws DataException {
        String[] keys = key.split("[.]");
        StringBuilder partialBuilt = new StringBuilder();
        BasicDBObject basicObject = this.basicObject;

        for (int i=0; i< keys.length; i++) {
            String partialKey = keys[i];

            partialBuilt.append(partialKey);

            if (isArray(partialKey)) {
                basicObject = (BasicDBObject) parseArray(basicObject, partialKey);
                continue;
            }

            if (isReference(basicObject)) {
                String referenceCollection = ((BasicDBObject) basicObject.get("value")).getString("collection");
                String referencePath = ((BasicDBObject) basicObject.get("value")).getString("path");
                AbstractDataProvider dataProvider = (AbstractDataProvider) createInstance(referenceCollection).get(referencePath);
                basicObject = dataProvider.basicObject;
            }

            if (!(basicObject.get(partialKey) instanceof BasicDBObject)) {
                if (null == basicObject.get(partialKey) || i < keys.length - 1) {
                    String wrongField = basicObject.get(partialKey)==null ? partialKey : keys[keys.length-1];
                    throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                            this.collectionName, wrongField, partialBuilt.toString()));
                }
                break;
            }
            basicObject = (BasicDBObject) basicObject.get(partialKey);
            partialBuilt.append(".");
        }
        return basicObject;
    }

    @Override
    public String toString() {
        if (this.basicObject == null) {
            return "";
        }
        return this.basicObject.toString();
    }

    protected void setRootObject(BasicDBObject rootObject, String path) {
        this.rootObject = rootObject;
        this.path = path;
    }

    @Override
    public String getValue() throws DataException {
        try {
            return this.getReference().getValue();
        } catch (ReferenceException e) {
            String result = this.basicObject.getString(VALUE_TPL);

            if (result == null) {
                if (this.way != null && this.way.contains(".")) {
                    this.way = this.way.split("[.]")[this.way.split("[.]").length - 1];
                }

                result = this.basicObject.getString(this.way);
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
        if (null != this.basicObject.get(VALUE_TPL) && !(this.basicObject.get(VALUE_TPL) instanceof String)
                && ((BasicDBObject) this.basicObject.get(VALUE_TPL)).containsField(COLLECTION_TPL)
                && ((BasicDBObject) this.basicObject.get(VALUE_TPL)).containsField("path")) {
            if (this.rootObject == null) {
                this.rootObject = this.basicObject;
            } else {
                String rootJson = this.rootObject.toJson();
                String baseJson = this.basicObject.toJson();
                if (rootJson.equals(baseJson)) {
                    throw new CyclicReferencesExeption("Cyclic references in database:\n" + rootJson);
                }
            }
            String referencedCollection = ((BasicBSONObject) this.basicObject.get(VALUE_TPL)).getString(COLLECTION_TPL);
            this.path = ((BasicBSONObject) this.basicObject.get(VALUE_TPL)).getString("path");
            AbstractDataProvider reference = (AbstractDataProvider) this.fromCollection(((BasicBSONObject) this.basicObject.get(VALUE_TPL)).getString(COLLECTION_TPL));
            reference.setRootObject(this.rootObject, referencedCollection + "." + this.path);
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
        Object value = this.basicObject.get("value");
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField("collection") && ((BasicDBObject) value).containsField("path");
    }

    public boolean isReference(BasicDBObject basicDBObject) {
        Object value = basicDBObject.get("value");
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField("collection") && ((BasicDBObject) value).containsField("path");
    }
}
