package ru.sbtqa.tag.datajack;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Konstantin Maltsev <mkypers@gmail.com>
 */
public class Stash {

    private static final Map<String, Object> stash = new HashMap<>();

    /**
     * Temporary data storage. Put data to this storage as key-value pair to use
     * it in workflow of your tests
     *
     * @return
     */
    public static Map<String, Object> getInstance() {
        return stash;
    }
}
