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

    private static final String VALUE_TPL = "value";
    private static final String COLLECTION_TPL = "collection";
    protected BasicDBObject basicObject;
    protected String collectionName;
    protected String way;
    protected String path;
    protected Class<? extends GeneratorCallback> callback;
    private BasicDBObject rootObject;
    private static final String NOT_INITIALIZED_EXCEPTION = "BasicDBObject is not initialized yet";

    private static boolean isArray(String key) {
        return key.matches("(.+\\[\\d+\\])");
    }

    /**
     * Internal use only for provider overriding purposes
     * @param basicObject Current object
     * @param collectionName Name of collection
     * @param way Passed way
     * @param <T> Adaptor type
     * @return return Adaptor instance
     * @throws DataException
     */
    protected abstract <T extends AbstractDataProvider> T createInstance(BasicDBObject basicObject, String collectionName, String way) throws DataException;


    /**
     * Internal use only for provider overriding purposes
     * @param basicObject Current object
     * @param collectionName Name of collection
     * @param <T> Adaptor type
     * @return return Adaptor instance
     * @throws DataException
     */
    protected abstract <T extends AbstractDataProvider> T createInstance(BasicDBObject basicObject, String collectionName) throws DataException;

    /**
     * Internal use only for provider overriding purposes
     * @param collectionName Name of collection
     * @param <T> Adaptor type
     * @return Adaptor instance
     * @throws DataException
     */
    protected abstract <T extends AbstractDataProvider> T createInstance(String collectionName) throws DataException;


    @Override
    public TestDataProvider get(String key) throws DataException {
        if (key.isEmpty()) {
            return this;
        }
        this.way = key;
        return key.contains(".") ? getComplex(key) : getSimple(key);

    }

    @Override
    public Map<String, Object> toMap() throws DataException {
        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObject.toMap();
    }

    @Override
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

    @Override
    public Collection<Object> getValues() throws DataException {
        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObject.values();
    }

    @Override
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
        StringBuilder partialBuiltPath = new StringBuilder();
        BasicDBObject curentBasicObject = this.basicObject;

        for (int i = 0; i < keys.length; i++) {
            String partialKey = keys[i];

            partialBuiltPath.append(partialKey);

            if (isArray(partialKey)) {
                curentBasicObject = (BasicDBObject) parseArray(curentBasicObject, partialKey);
                continue;
            }

            if (isReference(curentBasicObject)) {
                String referenceCollection = ((BasicDBObject) curentBasicObject.get(VALUE_TPL)).getString(COLLECTION_TPL);
                String referencePath = ((BasicDBObject) curentBasicObject.get(VALUE_TPL)).getString("path");
                AbstractDataProvider dataProvider = (AbstractDataProvider) createInstance(referenceCollection).get(referencePath);
                curentBasicObject = dataProvider.basicObject;
            }

            Object currentValue = curentBasicObject.get(partialKey);
            if (!(currentValue instanceof BasicDBObject)) {
                if (null == currentValue || i < keys.length - 1) {
                    String lastKey = keys[keys.length - 1];
                    String wrongField = curentBasicObject.get(partialKey) == null ? partialKey : lastKey;

                    throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                            this.collectionName, wrongField, partialBuiltPath.toString()));
                }
                break;
            }
            curentBasicObject = (BasicDBObject) curentBasicObject.get(partialKey);
            partialBuiltPath.append(".");
        }
        return curentBasicObject;
    }

    @Override
    public String toString() {
        return this.basicObject == null ? "" : this.basicObject.toString();
    }

    private void setRootObject(BasicDBObject rootObject, String path) {
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

                try {
                    Object callbackResult = callback.newInstance().call(generatorParams);

                    if (callbackResult instanceof Exception) {
                        throw (GeneratorException) callbackResult;
                    } else {
                        result = (String) callbackResult;
                    }
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new GeneratorException("Could not initialize callback", ex);
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
    public boolean isReference() {
        Object value = this.basicObject.get(VALUE_TPL);
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField(COLLECTION_TPL) && ((BasicDBObject) value).containsField("path");
    }

    private boolean isReference(BasicDBObject basicDBObject) {
        Object value = basicDBObject.get(VALUE_TPL);
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField(COLLECTION_TPL) && ((BasicDBObject) value).containsField("path");
    }
}
