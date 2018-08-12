package ru.sbtqa.tag.datajack.callback;

import java.util.HashMap;
import java.util.Map;

public class SampleDataCache {

  private static final Map<String, Object> CACHE = new HashMap<>();

  /**
   * Return CACHE instance
   *
   * @return
   */
  public static Map<String, Object> getCache() {
    return CACHE;
  }
}
