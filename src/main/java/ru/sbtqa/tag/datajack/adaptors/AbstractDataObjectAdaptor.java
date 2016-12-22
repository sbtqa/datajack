package ru.sbtqa.tag.datajack.adaptors;

import com.mongodb.BasicDBObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.DataException;

public abstract class AbstractDataObjectAdaptor {

    protected BasicDBObject basicObj;
    protected String way;
    protected String path;
    protected BasicDBObject rootObj;
    protected Class<? extends GeneratorCallback> callback;
    protected static final String VALUE_TPL = "value";
    protected static final String COLLECTION_TPL = "collection";
    private static final String NOT_INITIALIZED_EXCEPTION = "BasicDBObject is not initialized yet";

    public Map<String, Object> toMap() throws DataException {
        if (basicObj == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObj.toMap();
    }

    public Set<String> getKeySet() throws DataException {
        if (basicObj == null) {
            throw new DataException(NOT_INITIALIZED_EXCEPTION);
        }
        return basicObj.keySet();
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
