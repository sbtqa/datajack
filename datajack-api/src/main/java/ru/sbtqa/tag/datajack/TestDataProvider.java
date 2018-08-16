package ru.sbtqa.tag.datajack;

import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.DataException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TestDataProvider {

    /**
     * Switch current collection
     *
     * @param collectionName name of data collection
     * @return test data object instance
     * @throws DataException If no collection found
     */
    TestDataProvider fromCollection(String collectionName) throws DataException;

    /**
     * Get sub-object from test data object by key
     *
     * @param key key to get
     * @return test data object instance
     * @throws DataException f no value
     */
    TestDataProvider get(String key) throws DataException;

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
    public TestDataProvider getReference() throws DataException;

    /**
     * Apply generator callback
     *
     * @param callback Generator callback class
     */
    void applyGenerator(Class<? extends GeneratorCallback> callback);

    /**
     * Get map representation of TestDataProvider
     *
     * @return map of objects
     * @throws DataException if not initialized yet
     */
    public Map<String, Object> toMap() throws DataException;

    /**
     * Get set of keys from TestDataProvider
     *
     * @return set of keys
     * @throws DataException if not initialized yet
     */
    public Set<String> getKeySet() throws DataException;

    /**
     * Get list of values as Objects from TestDataProvider
     *
     * @return collection of Object values
     * @throws DataException if not initialized yet
     */
    public Collection<Object> getValues() throws DataException;

    /**
     * Get list of String representations of all primitive values from
     * TestDataProvider
     *
     * @return list of String values
     * @throws DataException if not initialized yet
     */
    public List<String> getStringValues() throws DataException;

    /**
     * @return List of parsed string values
     */
    @Override
    String toString();

}
