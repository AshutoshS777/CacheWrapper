package com.test.cache.exception;

public class CacheNotFoundException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public CacheNotFoundException() {
        super();
    }

    public CacheNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public CacheNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheNotFoundException(String message) {
        super(message);
    }

    public CacheNotFoundException(Throwable cause) {
        super(cause);
    }

}
