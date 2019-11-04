package ru.sbtqa.tag.datajack.providers;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.bson.BasicBSONObject;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.callback.CallbackData;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.CyclicReferencesException;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import ru.sbtqa.tag.datajack.exceptions.FieldNotFoundException;
import ru.sbtqa.tag.datajack.exceptions.GeneratorException;
import ru.sbtqa.tag.datajack.exceptions.ReferenceException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public abstract class AbstractDataProvider implements TestDataProvider {

    protected static final String VALUE_TPL = "value";
    protected static final String COLLECTION_TPL = "collection";

    private static final String ARRAY_MATCHER_REGEX = "(.+\\[\\d+\\])";
    private static final String NOT_INITIALIZED_EXCEPTION = "BasicDBObject is not initialized yet. Try to get some path first.";
    public static final String COLLECTION_PARSE_REGEX = "\\$([^\\{]+)";
    public static final String PATH_PARSE_REGEX = "(?:\\$([^\\{]+)?(\\{([^\\}]+)\\}))";

    protected BasicDBObject basicObject;
    protected String collectionName;
    protected String way;
    protected String path;
    protected Class<? extends GeneratorCallback> callback;
    protected BasicDBObject rootObject;

    private static boolean isArray(String key) {
        return key.matches(ARRAY_MATCHER_REGEX);
    }

    /**
     * Internal use only for provider overriding purposes
     *
     * @param basicObject Current object
     * @param collectionName Name of collection
     * @param way Passed way
     * @param <T> Adaptor type
     * @return return Adaptor instance
     * @throws DataException In case provider  could not be initialized
     */
    protected abstract <T extends AbstractDataProvider> T createInstance(BasicDBObject basicObject, String collectionName, String way) throws DataException;

    /**
     * Internal use only for provider overriding purposes
     *
     * @param basicObject Current object
     * @param collectionName Name of collection
     * @param <T> Adaptor type
     * @return return Adaptor instance
     * @throws DataException In case provider c
     */
    protected abstract <T extends AbstractDataProvider> T createInstance(BasicDBObject basicObject, String collectionName) throws DataException;

    /**
     * Internal use only for provider overriding purposes
     *
     * @param collectionName Name of collection
     * @param <T> Adaptor type
     * @return Adaptor instance
     * @throws DataException In case provider could not be initialized
     */
    protected abstract <T extends AbstractDataProvider> T createInstance(String collectionName) throws DataException;

    /**
     * {@inheritDoc}
     */
    @Override
    public TestDataProvider get(String key) throws DataException {
        if (key.isEmpty()) {
            return this;
        }
        this.way = key;
        return key.contains(".") ? getComplex(key) : getSimple(key);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestDataProvider getByPath(String path) throws DataException {
        if (path == null) {
            throw new DataException("The path is null. Check your configuration");
        }
        return parseTestDataProvider(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> toMap() throws DataException {
        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObject.toMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getKeySet() throws DataException {

        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        } else if (way != null && basicObject.toMap().containsKey(getWayTail()) &&
                !(basicObject.get(way) instanceof BasicDBObject)) {
            return new HashSet<>();
        }
        if (isReference()) {
            rootObject = null;
            return getReference().toMap().keySet();
        } else {
            return basicObject.keySet();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Object> getValues() throws DataException {
        if (basicObject == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObject.values();
    }

    /**
     * {@inheritDoc}
     */
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
            } else if (value == null) {
                strings.add("null");
            } else if (value instanceof BasicDBList) {
                ((BasicDBList) value).forEach(val -> strings.add(val.toString()));
            } else {
                strings.add("");
            }
        }
        return strings;
    }

    private TestDataProvider getSimple(String key) throws DataException {

        AbstractDataProvider dataProvider = createInstance(parseSimpleResult(key), this.collectionName, this.way);
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

    private BasicDBObject parseSimpleResult(String key) throws DataException {
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
        return (BasicDBObject) result;
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

        // If object passed by key is a reference to array, extract reference and replace current context by it
        if (basicO.get(arrayKey) instanceof BasicDBObject && isReference((BasicDBObject) basicO.get(arrayKey))) {
            basicO = ((AbstractDataProvider) get(arrayKey)).basicObject;
            arrayKey = basicO.keySet().iterator().next();
            way = key;
        } else {
            way += "." + key;
        }

        Object listCandidate = basicO.get(arrayKey);

        if (!(listCandidate instanceof BasicDBList)) {
            throw new DataException(String.format("%s.%s is not an array!", this.collectionName, key));
        }

        return ((BasicDBList) listCandidate).get(arrayIndex);
    }

    private BasicDBObject parseComplexDBObject(String key) throws DataException {
        String[] keys = key.split("[.]");
        StringBuilder partialBuiltPath = new StringBuilder();
        BasicDBObject currentBasicObject = this.basicObject;

        for (int i = 0; i < keys.length; i++) {
            String partialKey = keys[i];

            partialBuiltPath.append(partialKey);

            if (isArray(partialKey)) {
                currentBasicObject = (BasicDBObject) parseArray(currentBasicObject, partialKey);
                partialBuiltPath.append(".");
                continue;
            }

            if (isReference(currentBasicObject)) {
                AbstractDataProvider dataProvider = (AbstractDataProvider) createInstance(currentBasicObject, collectionName);
                currentBasicObject = ((AbstractDataProvider)dataProvider.getReference()).basicObject;
            }

            Object currentValue = currentBasicObject.get(partialKey);
            this.way += "." + partialKey;
            if (!(currentValue instanceof BasicDBObject)) {
                if (null == currentValue || i < keys.length - 1) {
                    String lastKey = keys[keys.length - 1];
                    String wrongField = currentBasicObject.get(partialKey) == null ? partialKey : lastKey;

                    throw new FieldNotFoundException(format("Collection \"%s\" doesn't contain \"%s\" field on path \"%s\"",
                            this.collectionName, wrongField, partialBuiltPath.toString()));
                }
                return currentBasicObject;
            }
            currentBasicObject = (BasicDBObject) currentBasicObject.get(partialKey);
            partialBuiltPath.append(".");
        }
        return currentBasicObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.basicObject == null ? "" : this.basicObject.toString();
    }

    public void setRootObject(BasicDBObject rootObject, String path) {
        this.rootObject = rootObject;
        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue() throws DataException {
        if (this.isReference()) {
            return this.getReference().getValue();
        }

        String result = this.basicObject.getString(VALUE_TPL);

        if (result == null) {
            way = getWayTail();

            if (!(basicObject.get(way) instanceof BasicDBObject)) {
                result = basicObject.getString(way);
            }

            if (result == null) {
                result = resolveDbObject().toString();
            }
        }
        return applyCallBackData(result);
    }

    private String applyCallBackData(String result) throws GeneratorException {
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

    /**
     * {@inheritDoc}
     */
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
                    throw new CyclicReferencesException("Cyclic references in database:\n" + rootJson);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyGenerator(Class<? extends GeneratorCallback> callback) {
        this.callback = callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReference() {
        return isReference(this.basicObject);
    }

    protected boolean isReference(BasicDBObject basicDBObject) {
        Object value = basicDBObject.get(VALUE_TPL);
        if (!(value instanceof BasicDBObject)) {
            return false;
        }
        return ((BasicDBObject) value).containsField(COLLECTION_TPL) && ((BasicDBObject) value).containsField("path");
    }

    private TestDataProvider parseTestDataProvider(String path) throws DataException {
        Pattern collectionPattern = Pattern.compile(COLLECTION_PARSE_REGEX);
        Matcher collectionMatcher = collectionPattern.matcher(path.trim());

        Pattern pathPattern = Pattern.compile(PATH_PARSE_REGEX);
        Matcher pathMatcher = pathPattern.matcher(path.trim());

        if (collectionMatcher.matches()) {
            String collectionToParse = collectionMatcher.group(1);

            return fromCollection(collectionToParse);
        } else if (pathMatcher.matches()) {
            String collectionToParse = pathMatcher.group(1);
            String pathValueToParse = pathMatcher.group(3);
            TestDataProvider testDataProvider = parseCollection(collectionToParse);

            if (pathValueToParse != null) {
                return testDataProvider.get(pathValueToParse);
            }
        }
        throw new DataException(format("Could not parse path %s", path));
    }

    private TestDataProvider parseCollection(String collectionToParse) throws DataException {
        if (collectionToParse != null) {
            return fromCollection(collectionToParse);
        } else if (this.collectionName == null) {
            throw new DataException("Trying to parse object with uninitialized collection.");
        } else {
            return this;
        }
    }


    /**
     * Get after last dot string value
     *
     * @return tail or null
     */
    private String getWayTail() {
        if (way == null) {
            return null;
        } else {
            String[] wayArray = way.split("[.]");
            return wayArray[wayArray.length - 1];
        }
    }

    /**
     * Walk into current {@link DBObject} and resolve all generators and references
     *
     * @return resolved {@link DBObject}
     * @throws DataException =
     */
    private DBObject resolveDbObject() throws DataException {
        BasicDBObject resolvedDbObject = new BasicDBObject();

        for (String key : basicObject.keySet()) {
            BasicDBObject target = basicObject.get(key) instanceof BasicDBObject ?
                    (BasicDBObject) basicObject.get(key) :
                    basicObject;
            AbstractDataProvider instance = createInstance(target, collectionName, way + "." + key);
            instance.applyGenerator(callback);

            if (basicObject.get(key) instanceof BasicDBObject) {
                try {
                    resolvedDbObject.put(key, JSON.parse(instance.getValue()));
                } catch (Exception e) {
                    resolvedDbObject.put(key, instance.getValue());
                }
            } else {
                if (instance.basicObject.keySet().contains(key) && instance
                        .basicObject.get(key) == null) {
                    resolvedDbObject.put(key, null);
                } else {
                    resolvedDbObject.put(key, instance.getValue());
                }
            }

        }
        return resolvedDbObject;
    }
}
