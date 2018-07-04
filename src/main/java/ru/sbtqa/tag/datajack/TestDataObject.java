package ru.sbtqa.tag.datajack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.DataException;

public interface TestDataObject {

    /**
     * Switch current collection
     *
     * @param collectionName name of data collection
     * @return test data object instance
     * @throws DataException If no collection found
     */
    TestDataObject fromCollection(String collectionName) throws DataException;

    /**
     * Get sub-object from test data object by key
     *
     * @param key key to get
     * @return test data object instance
     * @throws DataException f no value
     */
    TestDataObject get(String key) throws DataException;

    /**
     * Get value of current test data object
     *
     * @return String parsed from value
     * @throws DataException if no value
     */
    String getValue() throws DataException;

    /**
     * Check value is reference
     *
     * @return true if it is reference, false otherwise
     * @throws DataException if no reference
     */
    public boolean isReference() throws DataException;

    /**
     * Get reference from value
     *
     * @return Referenced object
     * @throws DataException if no reference
     */
    public TestDataObject getReference() throws DataException;

    /**
     * Apply generator callback
     *
     * @param callback Generator callback class
     */
    void applyGenerator(Class<? extends GeneratorCallback> callback);

    /**
     * Get map representation of TestDataObject
     *
     * @return map of objects
     * @throws DataException if not initialized yet
     */
    public Map<String, Object> toMap() throws DataException;

    /**
     * Get set of keys from TestDataObject
     *
     * @return set of keys
     * @throws DataException if not initialized yet
     */
    public Set<String> getKeySet() throws DataException;

    /**
     * Get list of values as Objects from TestDataObject
     *
     * @return collection of Object values
     * @throws DataException if not initialized yet
     */
    public Collection<Object> getValues() throws DataException;

    /**
     * Get list of String representations of all primitive values from
     * TestDataObject
     *
     * @return list of String values
     * @throws DataException if not initialized yet
     */
    public List<String> getStringValues() throws DataException;

    /**
     *
     * @return List of parsed string values
     */
    @Override
    String toString();

}
