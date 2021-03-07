/**
 * 
 */
package com.test.services.cache;

import java.util.HashMap;
import java.util.Map;

import com.test.cache.strategy.CacheStrategy;

/**
 * @author fnuashutosh
 *
 */
public class CacheTestStrategy implements CacheStrategy<String, CachePojo> {

	@Override
	public Map<String, CachePojo> buildCache() {
		Map<String, CachePojo> cacheMap = new HashMap<>();
		cacheMap.put("One", new CachePojo("One", "DummyOne"));
		cacheMap.put("Two", new CachePojo("Two", "DummyTwo"));
		return cacheMap;
	}
}
