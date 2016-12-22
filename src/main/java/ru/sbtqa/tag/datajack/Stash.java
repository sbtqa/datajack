package ru.sbtqa.tag.datajack;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary data storage. Put data to this storage as key-value pair to use it
 * in workflow of your tests
 */
public class Stash {

    private static final Map<String, Object> VAULT = new HashMap<>();

    private Stash() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Gets stash as map
     *
     * @return stash as a {@link java.util.Map} object
     */
    public static Map<String, Object> asMap() {
        return VAULT;
    }

    /**
     * Puts value in stash
     *
     * @param key the key as a {@link java.lang.String} object
     * @param value Any object
     */
    public static void put(String key, Object value) {
        VAULT.put(key, value);
    }

    /**
     * Gets a stash value by key
     *
     * @param <T> the type to return
     * @param key the key as a {@link java.lang.String} object
     * @return an object found by specified key
     */
    public static <T> T getValue(String key) {
        return (T) VAULT.get(key);
    }
}
