/**
 * 
 */
package com.test.cache.strategy;

import java.util.Map;

/**
 * Define Cache Strategy for each com.cisco.b2bngx.cache to be loaded
 * 
 * @author fnuashutosh
 */
public interface CacheStrategy<T1, T2> {
    Map<T1, T2> buildCache();
}
