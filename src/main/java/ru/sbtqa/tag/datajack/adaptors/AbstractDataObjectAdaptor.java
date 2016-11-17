package ru.sbtqa.tag.datajack.adaptors;

import com.mongodb.BasicDBObject;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.DataException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * @author Konstantin Ulyanov <notasimplename@gmail.com>
 */
public abstract class AbstractDataObjectAdaptor {
    protected BasicDBObject basicObj;
    protected String way;
    protected String path;
    protected BasicDBObject rootObj;
    protected Class<? extends GeneratorCallback> callback;

    public Map<String, Object> toMap() throws DataException {
        if (basicObj == null) {
            throw new DataException("BasicDBObject is not initialized yet");
        }
        return basicObj.toMap();
    }

    public Set<String> getKeySet() throws DataException {
        if (basicObj == null) {
            throw new DataException("BasicDBObject is not initialized yet");
        }
        return basicObj.keySet();
    }

    public Collection<Object> getValues() throws DataException {
        if (basicObj == null) {
            throw new DataException("BasicDBObject is not initialized yet");
        }
        return basicObj.values();
    }

    public List<String> getStringValues() throws DataException {
        if (basicObj == null) {
            throw new DataException("BasicDBObject is not initialized yet");
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
