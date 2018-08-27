package ru.sbtqa.tag.datajack.callback;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static ru.sbtqa.tag.datajack.callback.SampleDataCache.getCache;

/**
 * Default sbt callback. Works with sbt-datagens and Init.stash as cache
 */
public class SampleDataGensCallback implements GeneratorCallback {

    @Override
    public Object call(CallbackData callbackData) {

        Map<String, Object> cache = getCache();
        String cacheKey = callbackData.getPath();
        String result = callbackData.getResult();

        if (result.startsWith("generate:")) {
            if (!cache.containsKey("testDataCache")) {
                cache.put("testDataCache", new HashMap<>());
            } else if (((Map) cache.get("testDataCache")).containsKey(cacheKey)) {
                return ((Map) cache.get("testDataCache")).get(cacheKey);
            }

            String genResult = null;

            try {
                genResult = valueOf(new Random().nextLong());
            } catch (Exception ex) {
                return ex;
            }

            ((Map) cache.get("testDataCache")).put(cacheKey, genResult);
            return genResult;

        } else {
            return result;
        }
    }
}
