/**
 * 
 */
package com.test.services.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.test.cache.AppCacheManager;
import com.test.cache.exception.CacheNotFoundException;

/**
 * @author fnuashutosh
 *
 */
public class CacheTest {
	
	@BeforeAll
	public static void addCachePojoToCache() {
		AppCacheManager.INSTANCE.addToCacheManager(CachePojo.class);
	}
	
	@Test
	public void testGetObjectWithTypeAndKey() throws CacheNotFoundException {
		System.out.println("Test1");
		CachePojo one = AppCacheManager.INSTANCE.getObject(CachePojo.class, "One");
		assertNotNull(one);
		assertNotNull(one.getId());
		assertNotNull(one.getVal());
	}
	
	@Test
	public void testGetObjectListWithType() throws CacheNotFoundException {
		List<CachePojo> cacheList = AppCacheManager.INSTANCE.getObjectList(CachePojo.class);
		assertNotNull(cacheList);
		assertEquals(2, cacheList.size());
	}
	
	@Test
	public void testGetObjectListWithTypeAndKey() throws CacheNotFoundException {
		CachePojo pojo = AppCacheManager.INSTANCE.getObjectList(CachePojo.class, "One");
		assertNotNull(pojo);
		assertNotNull(pojo.getId());
	}
	
	@Test
	public void testGetObjectWithSearchableAttributes() throws CacheNotFoundException {
		Map<String, Object> searchMap = new HashMap<>();
		searchMap.put("val", "DummyTwo");
		List<CachePojo> twoList = AppCacheManager.INSTANCE.getObject(CachePojo.class, searchMap);
		assertNotNull(twoList);
		assertEquals(1,twoList.size());
	}
}
