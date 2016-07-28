package org.icgc.dcc.dev.server.portal;

import static com.google.common.base.Preconditions.checkState;

import java.lang.annotation.Annotation;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.Striped;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect for applying {@link ReadWriteLock} lockings around methods that have a parameter that bears the {@link @PortalLock} annotation.
 */
@Slf4j
@Aspect
@Component
public class PortalLockAspect {

  /**
   * State.
   */
  Striped<ReadWriteLock> locks = Striped.lazyWeakReadWriteLock(10); // Suitable count for application

  /**
   * Sub-package of {@code portal}.
   */
  @Pointcut("within(org.icgc.dcc.dev.server..*)")
  public void portalSubPackage() {
  }
  
  /**
   * When a executing public method has the {@link @PortalLock} annotation in any argument.
   */
  @Pointcut("execution(public * *(.., @PortalLock (*), ..)) || execution(public * *(@PortalLock (*)))")
  public void portalLockMethod() {
  }
  
  /**
   * Combined, target pointcut.
   */
  @Pointcut("portalSubPackage() && portalLockMethod()")
  public void portalLock() {
  }

  /**
   * Applied around advised method.
   */
  @Around("portalLock()")
  public Object processRequest(ProceedingJoinPoint pjp) throws Throwable {
    val lock = resolveLock(pjp);
    
    // Apply lock around the method
    log.info("Locking {}...", pjp);
    lock.lock();
    try {
      return pjp.proceed();
    } finally {
      log.info("Unlocking {}...", pjp);
      lock.unlock();
    }
  }

  private Lock resolveLock(ProceedingJoinPoint pjp) {
    val methodSignature = (MethodSignature) pjp.getSignature();
    Annotation[][] parameterAnnotations = methodSignature.getMethod().getParameterAnnotations();
    
    // Find the @PortalLock annotated parameter
    for (int i = 0; i < parameterAnnotations.length; i++) {
      for (int j = 0; j <  parameterAnnotations[i].length; j++) {
        val annotation = parameterAnnotations[i][j];
        if (annotation.annotationType() != PortalLock.class) continue;

        // Get the portal id to lock
        val arg = pjp.getArgs()[i];
        val portalId = resolvePortalId(arg);

        // Lock based on annotation specified lock type
        val rw = locks.get(portalId);
        return ((PortalLock) annotation).write() ? rw.writeLock() : rw.readLock();
      }
    }

    return null;
  }

  private String resolvePortalId(Object arg) {
    checkState(arg != null, "Cannot use @PortalLock on null parameter");
    
    if (arg instanceof Portal) {
      return ((Portal) arg).getId();
    } else if (arg instanceof String) {
      return ((String) arg);
    } else {
      throw new IllegalStateException("Cannot use @PortalLock on parameter of type " + arg.getClass().getName());
    }
  }

}
