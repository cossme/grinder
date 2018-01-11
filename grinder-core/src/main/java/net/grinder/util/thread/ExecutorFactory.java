// Copyright (C) 2011 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.util.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Create suitable {@link java.util.concurrent.Executor} implementations.
 *
 * @author Philip Aston
 */
public class ExecutorFactory {

  private static ScheduledExecutorService s_scheduler;

  private static class NamedThreadFactory implements ThreadFactory {

    private final ThreadGroup m_group;
    private final AtomicInteger m_threadNumber = new AtomicInteger(1);

    NamedThreadFactory(String name) {
      m_group = new ThreadGroup(name);
      m_group.setDaemon(true);
    }

    public Thread newThread(Runnable runnable) {
      final String threadName =
        m_group.getName() + "-" + m_threadNumber.getAndIncrement();
      return new Thread(m_group, runnable, threadName);
    }

  }

  /**
   * Create a fixed size thread pool.
   *
   * @param name
   *          Pool name.
   * @param numberOfThreads
   *          The number of threads.
   * @return The thread pool.
   */
  public static ExecutorService createThreadPool(String name,
                                                 int numberOfThreads) {
    return Executors.newFixedThreadPool(numberOfThreads,
                                        new NamedThreadFactory(name));
  }

  /**
   * Create a cached thread pool.
   *
   * @param name
   *          Pool name.
   * @return The thread pool.
   */
  public static ExecutorService createCachedThreadPool(String name) {
    return Executors.newCachedThreadPool(new NamedThreadFactory(name));
  }

  /**
   * Return a a shared scheduled executor for general timer tasks.
   *
   * @return The scheduled executor.
   */
  public static synchronized ScheduledExecutorService
    getUtilityScheduledExecutor() {

    if (s_scheduler == null) {
      s_scheduler =
       Executors.newScheduledThreadPool(1,
                                        new NamedThreadFactory("scheduler"));
    }

    return s_scheduler;
  }
}
