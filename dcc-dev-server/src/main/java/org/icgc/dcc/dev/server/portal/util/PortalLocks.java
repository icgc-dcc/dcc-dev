/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.dev.server.portal.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.icgc.dcc.dev.server.portal.Portal;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.Striped;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Provides fine grained locking semantics at the level of read / write per portal instance.
 */
@Component
public class PortalLocks {

  /**
   * State.
   */
  private final Striped<ReadWriteLock> locks = Striped.lazyWeakReadWriteLock(10); // Suitable count for application

  public PortalLock lockWriting(@NonNull Portal portal) {
    return lockWriting(portal.getId());
  }

  public PortalLock lockWriting(@NonNull Integer portalId) {
    val lock = writeLock(portalId);
    lock.lock();

    return lock;
  }

  public PortalLock lockReading(@NonNull Portal portal) {
    return lockReading(portal.getId());
  }

  public PortalLock lockReading(@NonNull Integer portalId) {
    val lock = readLock(portalId);
    lock.lock();

    return lock;
  }

  public PortalLock readLock(@NonNull Integer portalId) {
    return new PortalLock(locks.get(portalId).readLock());
  }

  public PortalLock readLock(@NonNull Portal portal) {
    return readLock(portal.getId());
  }

  public PortalLock writeLock(@NonNull Integer portalId) {
    return new PortalLock(locks.get(portalId).writeLock());
  }

  public PortalLock writeLock(@NonNull Portal portal) {
    return writeLock(portal.getId());
  }

  /**
   * {@link Lock} implementation for use with {@link @Cleanup}.
   */
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
