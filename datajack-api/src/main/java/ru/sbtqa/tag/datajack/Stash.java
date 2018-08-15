package ru.sbtqa.tag.datajack;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary data storage. Put data to this storage as key-value pair to use it
 * in workflow of your tests
 */
public class Stash {

    private static final ThreadLocal<Map<String, Object>> THREAD_VAULT = new ThreadLocal<>();

    private Stash() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Thread-safety stash
     *
     * @return stash of current thread as a {@link java.util.Map} object
     */
    private static Map<String, Object> getThreadVault() {
        Map<String, Object> vault = THREAD_VAULT.get();
        if (vault == null) {
            vault = new HashMap<>();
            THREAD_VAULT.set(vault);
        }
        return vault;
    }

    /**
     * Gets stash as map
     *
     * @return stash as a {@link java.util.Map} object
     */
    public static Map<String, Object> asMap() {
        return getThreadVault();
    }

    /**
     * Puts value in stash
     *
     * @param key   the key as a {@link java.lang.String} object
     * @param value Any object
     */
    public static void put(String key, Object value) {
        getThreadVault().put(key, value);
    }

    /**
     * Gets a stash value by key
     *
     * @param <T> the type to return
     * @param key the key as a {@link java.lang.String} object
     * @return an object found by specified key
     */
    public static <T> T getValue(String key) {
        return (T) getThreadVault().get(key);
    }

    /**
     * Removes value from stash
     *
     * @param <T> the type to return
     * @param key the key as a {@link java.lang.String} object
     * @return an object removed by specified key
     */
    public static <T> T remove(String key) {
        return (T) getThreadVault().remove(key);
    }

    /**
     * Clear stash
     */
    public static void clear() {
        getThreadVault().clear();
    }
}
