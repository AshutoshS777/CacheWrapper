/**
 * 
 */
package com.test.services.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.test.cache.annotations.Cached;

/**
 * @author fnuashutosh
 *
 */
@Cached(cacheStrategy = CacheTestStrategy.class, initialDelay = 1, refreshInterval = 1, size = 100, timeUnit = TimeUnit.SECONDS, searchableAttributes = {
		"val" })
public class CachePojo implements Serializable {
	private static final long serialVersionUID = 1L;
	private String id;
	private String val;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVal() {
		return val;
	}

	public void setVa(String val) {
		this.val = val;
	}

	public CachePojo(String id, String val) {
		super();
		this.id = id;
		this.val = val;
	}
}
