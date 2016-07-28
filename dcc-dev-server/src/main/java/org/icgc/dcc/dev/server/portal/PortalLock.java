package org.icgc.dcc.dev.server.portal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Annotation signaling the application of a {@link ReadWriteLock} locking around methods that have a parameter that bears it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PortalLock {
  
    /**
     * By default will use read lock unless this is {@code true}.
     */
    boolean write() default false;
    
 }
