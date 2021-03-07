/**

 * 
 */

package com.test.cache;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.test.cache.annotations.Cached;
import com.test.cache.exception.CacheNotFoundException;
import com.test.cache.strategy.CacheStrategy;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.expression.EqualTo;

/**
 * Cache Manager Singleton instance for all Caches
 * 
 * @author fnuashutosh
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public enum AppCacheManager {
	INSTANCE;

	private final Logger logger = LoggerFactory.getLogger(AppCacheManager.class);
	private CacheManager cacheManager;
	private final Map<String, CacheStrategy> strategyMap;
	private final Map<Class<Serializable>, String> cacheNameMap;
	private final ScheduledExecutorService executor;

	// For testing
	public void addToCacheManager(Class<? extends Serializable> cacheableClazz) {
		processCacheable(1, cacheableClazz);
	}

	// Constructor for Initialization
	private AppCacheManager() {
		logger.info("Started loading {}", "AppCacheManager");
		System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
		strategyMap = new HashMap<>();
		cacheNameMap = new HashMap<>();
		cacheManager = CacheManager.create();
		Collection<URL> packages = ClasspathHelper.forPackage("com.test.services");
		Reflections scanner = new Reflections(packages);
		ClasspathHelper.forPackage("com.test.services");
		executor = Executors.newScheduledThreadPool(builCacheMaps(scanner));
		if (!cacheNameMap.isEmpty()) {
			for (Class<Serializable> cacheClass : cacheNameMap.keySet()) {
				addToExecutor(cacheClass);
			}
		}
	}

	private int builCacheMaps(Reflections scanner) {
		int cacheCount = 0;
		cacheManager.removeAllCaches();
		Set<Class<?>> cachedClasses = scanner.getTypesAnnotatedWith(Cached.class);
		for (Class<?> cls : cachedClasses) {
			cacheCount = processCacheable(cacheCount, cls);
		}
		return cacheCount;
	}

	private int processCacheable(int cacheCount, Class<?> cls) {
		try {
			Class<Serializable> cacheableClazz = (Class<Serializable>) cls;
			if (cacheableClazz.isAnnotationPresent(Cached.class)) {
				String cacheName = getCacheName(cacheableClazz);
				Class<CacheStrategy> cacheStrategy = getStrategyClass(cacheableClazz);
				CacheStrategy strategy = getCacheableInstance(cacheStrategy);
				CacheConfiguration cacheConfig = new CacheConfiguration(cacheName, 0).eternal(true);
				Searchable searchable = new Searchable();
				cacheConfig.addSearchable(searchable);
				String[] searchablaeAttrs = getRSearchableAttributes(cacheableClazz);
				if (searchablaeAttrs != null && searchablaeAttrs.length > 0) {
					for (String attribute : searchablaeAttrs) {
						searchable.addSearchAttribute(new SearchAttribute().name(attribute));
					}
				}
				Cache cache = new Cache(cacheConfig);
				cacheManager.addCache(cache);
				strategyMap.put(cacheName, strategy);
				cacheNameMap.put(cacheableClazz, cacheName);
				cacheCount++;
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			logger.error("Error while loading CacheManager", e);
		}
		return cacheCount;
	}

	public <T> List<T> getObject(Class<T> expectedType, Map<String, Object> criteriaMap) throws CacheNotFoundException {
		String cacheName = cacheNameMap.get(expectedType);
		List<T> cacheList = new ArrayList<>();
		if (StringUtils.isEmpty(cacheName)) {
			throw new CacheNotFoundException(CacheConstants.CACHE_DEFINITION_NOT_FOUND_MSG + expectedType.getName());
		}
		if (criteriaMap == null || criteriaMap.isEmpty()) {
			return cacheList;
		}
		logger.info("Getting elements for criteria : {}", criteriaMap);
		Cache cache = cacheManager.getCache(cacheNameMap.get(expectedType));
		Query query = cache.createQuery();
		for (Map.Entry<String, Object> criteriaEntry : criteriaMap.entrySet()) {
			query.addCriteria(new EqualTo(criteriaEntry.getKey(), criteriaEntry.getValue()));
		}
		query.includeKeys().includeValues();
		Results results = query.execute();
		if (!results.hasValues()) {
			logger.info("Element not found for criteria : {}. refreshing com.cisco.buff.cache", criteriaMap);
			refresh(cacheName);
			results = query.execute();
		}
		if (results != null && results.size() > 0) {
			for (Result result : results.all()) {
				cacheList.add((T) result.getValue());
			}
		}
		return cacheList;
	}

	public <T> T getObject(Class<T> expectedType, String key) throws CacheNotFoundException {
		Element element = getElementObj(expectedType, key);

		if (element != null) {
			return (T) element.getObjectValue();
		}
		return null;
	}

	public <T> List<T> getObjectList(Class<T> expectedType) throws CacheNotFoundException {
		String cacheName = cacheNameMap.get(expectedType);
		if (StringUtils.isEmpty(cacheName)) {
			throw new CacheNotFoundException(CacheConstants.CACHE_DEFINITION_NOT_FOUND_MSG + expectedType.getName());
		}
		List<T> cacheList = new ArrayList<>();
		Cache cache = cacheManager.getCache(cacheNameMap.get(expectedType));
		Map<Object, Element> cacheMap = cache.getAll(cache.getKeys());
		if (cacheMap == null || cacheMap.isEmpty()) {
			logger.info(
					"Element not found for com.cisco.buff.cache : {} . Refreshing com.cisco.buff.cache for class : {} ",
					cacheName, expectedType.getName());
			refresh(cacheName);
			cacheMap = cache.getAll(cache.getKeys());
		}
		if (cacheMap != null) {
			for (Map.Entry<Object, Element> cacheEntry : cacheMap.entrySet()) {
				Element element = cacheEntry.getValue();
				if (element != null) {
					cacheList.add((T) element.getObjectValue());
				}
			}
			return cacheList;
		}
		return cacheList;
	}

	private <T> Element getElementObj(Class<T> expectedType, String key) throws CacheNotFoundException {
		String cacheName = cacheNameMap.get(expectedType);
		if (StringUtils.isEmpty(cacheName)) {
			throw new CacheNotFoundException(CacheConstants.CACHE_DEFINITION_NOT_FOUND_MSG + expectedType.getName());
		}
		Cache cache = cacheManager.getCache(cacheNameMap.get(expectedType));
		Element element = cache.get(key);
		if (element == null) {
			logger.info("Element not found for key : {} . Refreshing com.cisco.buff.cache for class : {} ", key,
					expectedType.getName());
			refresh(cacheName);
			element = cache.get(key);
		}
		return element;
	}

	public <T> T getObjectList(Class<T> expectedType, String key) throws CacheNotFoundException {
		Element element = getElementObj(expectedType, key);
		T obj = null;
		if (element != null) {
			obj = (T) element.getObjectValue();
		}
		return obj;
	}

	private String getCacheName(Class<Serializable> clazz) {
		logger.info("Getting com.cisco.buff.cache name for class : {} ", clazz);
		String cacheName = clazz.getAnnotation(Cached.class).name();
		if (StringUtils.isEmpty(cacheName)) {
			cacheName = clazz.getName();
		}
		return cacheName;
	}

	private CacheStrategy getCacheableInstance(Class<CacheStrategy> clazz)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		return clazz.getConstructor().newInstance();
	}

	private Class<CacheStrategy> getStrategyClass(Class<Serializable> clazz) {
		return (Class<CacheStrategy>) clazz.getAnnotation(Cached.class).cacheStrategy();
	}

	private long getInitlaDelay(Class<Serializable> clazz) {
		long initialDelay = clazz.getAnnotation(Cached.class).initialDelay();
		if (initialDelay == 0L) {
			initialDelay = 2;
		}
		return initialDelay;
	}

	private long getRefreshInterval(Class<Serializable> clazz) {
		long refreshInterval = clazz.getAnnotation(Cached.class).refreshInterval();
		if (refreshInterval == 0L) {
			refreshInterval = 30;
		}
		return refreshInterval;
	}

	private String[] getRSearchableAttributes(Class<Serializable> clazz) {
		return clazz.getAnnotation(Cached.class).searchableAttributes();
	}

	private TimeUnit getTimeUnit(Class<Serializable> clazz) {
		TimeUnit timeUnit = clazz.getAnnotation(Cached.class).timeUnit();
		if (timeUnit == null) {
			timeUnit = TimeUnit.MINUTES;
		}
		return timeUnit;
	}

	/**
	 * Refresh com.cisco.buff.cache with given name
	 * 
	 * @param cacheName
	 */
	public void refresh(String cacheName) {
		logger.info("Refreshing com.cisco.buff.cache : {} ", cacheName);
		CacheStrategy cacheStrategy = strategyMap.get(cacheName);
		Map<String, Serializable> refreshedMap = cacheStrategy.buildCache();
		Cache cache = cacheManager.getCache(cacheName);
		for (Map.Entry<String, Serializable> valueEntry : refreshedMap.entrySet()) {
			cache.put(new Element(valueEntry.getKey(), valueEntry.getValue()));
		}
		logger.info("Refreshed com.cisco.buff.cache : {} ", cacheName);
	}

	private void addToExecutor(Class<Serializable> clazz) {
		long initialDelay = getInitlaDelay(clazz);
		long refreshInterval = getRefreshInterval(clazz);
		TimeUnit timeUnit = getTimeUnit(clazz);
		final String cacheName = getCacheName(clazz);
		boolean refresh = clazz.getAnnotation(Cached.class).refresh();
		logger.info(
				"Cache Properties for cache : {} are initialDelay : {} , refreshInterval : {}, timeUnit : {} ,refresh : {}",
				cacheName, initialDelay, refreshInterval, timeUnit.name(), refresh);
		if (refresh) {
			executor.scheduleAtFixedRate(new Runnable() {
				@Override
				// @LogExecutionTime
				public void run() {
					Thread.currentThread().setName(cacheName);
					refresh(cacheName);
				}
			}, initialDelay, refreshInterval, timeUnit);
		} else {
			executor.submit(new Runnable() {
				@Override
				// @LogExecutionTime
				public void run() {
					Thread.currentThread().setName(cacheName);
					refresh(cacheName);
				}
			});
		}

	}
}