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
     * @return test data provider instance
     * @throws DataException If no collection found
     */
    TestDataProvider fromCollection(String collectionName) throws DataException;

    /**
     * Get sub-object from test data object by key
     *
     * @param key key to get
     * @return test data provider instance
     * @throws DataException f no value
     */
    TestDataProvider get(String key) throws DataException;

    /**
     * Get sub-object from test data object by path
     *
     * @param key path to get like $Collection{path.to.smth} or ${path.to.smth} relative to current collection
     * @return test data provider instance
     * @throws DataException if no value
     */
    TestDataProvider getByPath(String key) throws DataException;

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
    boolean isReference() throws DataException;

    /**
     * Get reference from value
     *
     * @return Referenced object
     * @throws DataException if no reference
     */
    TestDataProvider getReference() throws DataException;

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
    Map toMap() throws DataException;

    /**
     * Get set of keys from TestDataProvider
     *
     * @return set of keys
     * @throws DataException if not initialized yet
     */
    Set<String> getKeySet() throws DataException;

    /**
     * Get list of values as Objects from TestDataProvider
     *
     * @return collection of Object values
     * @throws DataException if not initialized yet
     */
    Collection<Object> getValues() throws DataException;

    /**
     * Get list of String representations of all primitive values from
     * TestDataProvider
     *
     * @return list of String values
     * @throws DataException if not initialized yet
     */
    List<String> getStringValues() throws DataException;

    /**
     * @return List of parsed string values
     */
    @Override
    String toString();

}
