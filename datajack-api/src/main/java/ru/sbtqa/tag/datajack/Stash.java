package ru.sbtqa.tag.datajack;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary data storage. Put data to this storage as key-value pair to use it
 * in workflow of your tests
 */
public class Stash {

    private static final ThreadLocal<Map<Object, Object>> THREAD_VAULT = new ThreadLocal<>();

    private Stash() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Thread-safety stash
     *
     * @return stash of current thread as a {@link java.util.Map} object
     */
    private static Map<Object, Object> getThreadVault() {
        Map<Object, Object> vault = THREAD_VAULT.get();
        if (vault == null) {
            vault = new HashMap<>();
            THREAD_VAULT.set(vault);
        }
        return vault;
    }

    /**
     * Gets stash as map
     *
     * @return stash as a object
     */
    public static Map<Object, Object> asMap() {
        return getThreadVault();
    }

    /**
     * Puts value in stash
     *
     * @param key the key as a object
     * @param value Any object
     */
    public static void put(Object key, Object value) {
        getThreadVault().put(key, value);
    }

    /**
     * Gets a stash value by key
     *
     * @param <T> the type to return
     * @param key the key as a object
     * @return an object found by specified key
     */
    public static <T> T getValue(Object key) {
        return (T) getThreadVault().get(key);
    }

    /**
     * Removes value from stash
     *
     * @param <T> the type to return
     * @param key the key as a object
     * @return an object removed by specified key
     */
    public static <T> T remove(Object key) {
        return (T) getThreadVault().remove(key);
    }

    /**
     * Clear stash
     */
    public static void clear() {
        getThreadVault().clear();
    }
}
