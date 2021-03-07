package com.test.cache.strategy;

import java.util.HashMap;
import java.util.Map;

public class DefaultCacheStrategy implements CacheStrategy<String, Object> {

    @Override
    public Map<String, Object> buildCache() {
        return new HashMap<String, Object>();
    }
}
