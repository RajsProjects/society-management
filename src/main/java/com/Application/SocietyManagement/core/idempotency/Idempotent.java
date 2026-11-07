package com.Application.SocietyManagement.core.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint as supporting or requiring idempotency key semantics.
 * Ensures duplicate requests with the same Idempotency-Key return cached responses
 * without producing duplicate database records or external side-effects.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Time-to-live for the idempotency record in seconds.
     * Default is 86400 (24 hours).
     */
    long ttlSeconds() default 86400;

    /**
     * Whether the Idempotency-Key header is strictly required.
     * If true and header is missing, HTTP 400 Bad Request is returned.
     * Default is false (idempotency is applied whenever the header is supplied).
     */
    boolean required() default false;
}

