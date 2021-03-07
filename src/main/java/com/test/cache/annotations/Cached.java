package com.test.cache.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.test.cache.strategy.CacheStrategy;
import com.test.cache.strategy.DefaultCacheStrategy;



@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Cached {
    public String name() default StringUtils.EMPTY;

    @SuppressWarnings("rawtypes")
    public Class<? extends CacheStrategy> cacheStrategy() default DefaultCacheStrategy.class;

    public int size() default 5000;

    public long refreshInterval() default 30;

    public long initialDelay() default 2;

    public TimeUnit timeUnit() default TimeUnit.MINUTES;

    public boolean refresh() default true;

    public String[] searchableAttributes() default {};
}
