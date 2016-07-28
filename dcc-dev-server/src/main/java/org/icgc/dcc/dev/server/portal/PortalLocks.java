package org.icgc.dcc.dev.server.portal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.Striped;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
public class PortalLocks {

  /**
   * State.
   */
  private final Striped<ReadWriteLock> locks = Striped.lazyWeakReadWriteLock(10); // Suitable count for application

  public PortalLock readLock(@NonNull String portalId) {
    return new PortalLock(locks.get(portalId).readLock());
  }

  public PortalLock readLock(@NonNull Portal portal) {
    return readLock(portal.getId());
  }

  public PortalLock writeLock(@NonNull String portalId) {
    return new PortalLock(locks.get(portalId).writeLock());
  }

  public PortalLock writeLock(@NonNull Portal portal) {
    return writeLock(portal.getId());
  }

  @RequiredArgsConstructor
  public static class PortalLock implements Lock, AutoCloseable {

    private final Lock delegate;
    
    public void lock() {
      delegate.lock();
    }

    public void lockInterruptibly() throws InterruptedException {
      delegate.lockInterruptibly();
    }

    public boolean tryLock() {
      return delegate.tryLock();
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return delegate.tryLock(time, unit);
    }

    public void unlock() {
      delegate.unlock();
    }

    public Condition newCondition() {
      return delegate.newCondition();
    }

    @Override
    public void close() {
      unlock();
    }

  }

}
