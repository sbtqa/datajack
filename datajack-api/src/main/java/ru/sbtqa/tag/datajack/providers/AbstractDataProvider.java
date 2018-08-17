package ru.sbtqa.tag.datajack.providers;

import com.mongodb.BasicDBObject;
import ru.sbtqa.tag.datajack.TestDataProvider;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.DataException;

import java.util.*;

public abstract class AbstractDataProvider implements TestDataProvider {

    protected static final String VALUE_TPL = "value";
    protected static final String COLLECTION_TPL = "collection";
    private static final String NOT_INITIALIZED_EXCEPTION = "BasicDBObject is not initialized yet";
    protected BasicDBObject basicObj;
    protected String way;
    protected String path;
    protected BasicDBObject rootObj;
    protected Class<? extends GeneratorCallback> callback;

    public Map<String, Object> toMap() throws DataException {
        if (basicObj == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObj.toMap();
    }

    public Set<String> getKeySet() throws DataException {
        if (basicObj == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        } else if (getValue() != null) {
            return new HashSet<>();
        }
        if (isReference()) {
            this.rootObj = null;
            return getReference().getKeySet();
        } else {
            return basicObj.keySet();
        }
    }

    public Collection<Object> getValues() throws DataException {
        if (basicObj == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObj.values();
    }

    public List<String> getStringValues() throws DataException {
        if (basicObj == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        Collection<Object> values = basicObj.values();
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
}
