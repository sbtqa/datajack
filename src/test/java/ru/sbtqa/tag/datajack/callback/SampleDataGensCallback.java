package ru.sbtqa.tag.datajack.callback;

import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import java.util.HashMap;
import java.util.Map;
import static ru.sbtqa.tag.datajack.callback.SampleDataCache.getCache;
import ru.sbtqa.tag.datajack.callback.CallbackData;
import ru.sbtqa.tag.datajack.callback.GeneratorCallback;

/**
 * Default sbt callback. Works with sbt-datagens and Init.stash as cache
 *
 * @author Viktor Sidochenko <viktor.sidochenko@gmail.com>
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
                genResult = valueOf(currentTimeMillis());
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
