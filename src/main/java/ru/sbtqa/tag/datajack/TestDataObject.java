package ru.sbtqa.tag.datajack;

import ru.sbtqa.tag.datajack.callback.GeneratorCallback;
import ru.sbtqa.tag.datajack.exceptions.DataException;

/**
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
 */
public interface TestDataObject {

    /**
     * Switch current collection
     *
     * @param collectionName name of data collection
     * @return test data object instance
     * @throws ru.sbtqa.tag.datajack.exceptions.DataException
     */
    TestDataObject fromCollection(String collectionName) throws DataException;

    /**
     * Get sub-object from test data object by key
     *
     * @param key key to get
     * @return test data object instance
     * @throws ru.sbtqa.tag.datajack.exceptions.DataException
     */
    TestDataObject get(String key) throws DataException;

    /**
     * Get value of current test data object
     *
     * @return
     * @throws DataException
     */
    String getValue() throws DataException;

    /**
     * Get reference from value
     *
     * @return Referenced object
     * @throws ru.sbtqa.tag.datajack.exceptions.DataException
     */
    public TestDataObject getReference() throws DataException;

    /**
     * Apply generator callback
     *
     * @param callback Generator callback class
     */
    void applyGenerator(Class<? extends GeneratorCallback> callback);

    /**
     *
     * @return
     */
    @Override
    String toString();

}
