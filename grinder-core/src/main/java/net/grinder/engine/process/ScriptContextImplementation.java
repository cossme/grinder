// Copyright (C) 2001 - 2011 Philip Aston
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

package net.grinder.engine.process;

import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.CommunicationException;
import net.grinder.script.Barrier;
import net.grinder.script.InternalScriptContext;
import net.grinder.script.InvalidContextException;
import net.grinder.script.SSLControl;
import net.grinder.script.Statistics;
import net.grinder.script.TestRegistry;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.BarrierImplementation;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.util.Sleeper;

import org.slf4j.Logger;


/**
 * Implementation of {@link ScriptContext}.
 *
 * @author Philip Aston
 */
final class ScriptContextImplementation implements InternalScriptContext {

  private final WorkerIdentity m_workerIdentity;
  private final WorkerIdentity m_firstWorkerIdentity;
  private final ThreadContextLocator m_threadContextLocator;
  private final GrinderProperties m_properties;
  private final Logger m_logger;
  private final Sleeper m_sleeper;
  private final SSLControl m_sslControl;
  private final Statistics m_scriptStatistics;
  private final TestRegistry m_testRegistry;
  private final ThreadStarter m_threadStarter;
  private final ThreadStopper m_threadStopper;
  private final BarrierGroups m_barrierGroups;
  private final BarrierIdentity.Factory m_barrierIdentityFactory;

  public ScriptContextImplementation(
     WorkerIdentity workerIdentity,
     WorkerIdentity firstWorkerIdentity,
     ThreadContextLocator threadContextLocator,
     GrinderProperties properties,
     Logger logger,
     Sleeper sleeper,
     SSLControl sslControl,
     Statistics scriptStatistics,
     TestRegistry testRegistry,
     ThreadStarter threadStarter,
     ThreadStopper threadStopper,
     BarrierGroups barrierGroups,
     BarrierIdentity.Factory barrierIdentityFactory) {

    m_workerIdentity = workerIdentity;
    m_firstWorkerIdentity = firstWorkerIdentity;
    m_threadContextLocator = threadContextLocator;
    m_properties = properties;
    m_logger = logger;
    m_sleeper = sleeper;
    m_sslControl = sslControl;
    m_scriptStatistics = scriptStatistics;
    m_testRegistry = testRegistry;
    m_threadStarter = threadStarter;
    m_threadStopper = threadStopper;
    m_barrierGroups = barrierGroups;
    m_barrierIdentityFactory = barrierIdentityFactory;
  }

  public int getAgentNumber() {
    return m_workerIdentity.getAgentIdentity().getNumber();
  }

  public String getProcessName() {
    return m_workerIdentity.getName();
  }

  public int getProcessNumber() {
    return m_workerIdentity.getNumber();
  }

  public int getFirstProcessNumber() {
    return m_firstWorkerIdentity.getNumber();
  }

  public int getThreadNumber() {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext != null) {
      return threadContext.getThreadNumber();
    }

    return -1;
  }

  public int getRunNumber() {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext != null) {
      return threadContext.getRunNumber();
    }

    return -1;
  }

  public Logger getLogger() {
    return m_logger;
  }

  public void sleep(long meanTime) throws GrinderException {
    m_sleeper.sleepNormal(meanTime);
  }

  public void sleep(long meanTime, long sigma) throws GrinderException {
    m_sleeper.sleepNormal(meanTime, sigma);
  }

  public int startWorkerThread() throws GrinderException {
    return m_threadStarter.startThread(null);
  }

  public int startWorkerThread(Object testRunner) throws GrinderException {
    return m_threadStarter.startThread(testRunner);
  }

  public void stopThisWorkerThread() throws InvalidContextException {

    if (m_threadContextLocator.get() != null) {
      throw new ShutdownException("Thread has been shut down");
    }
    else {
      throw new InvalidContextException(
        "stopThisWorkerThread() must be called from  a worker thread");
    }
  }

  public boolean stopWorkerThread(int threadNumber) {
    return m_threadStopper.stopThread(threadNumber);
  }

  public GrinderProperties getProperties() {
    return m_properties;
  }

  public Statistics getStatistics() {
    return m_scriptStatistics;
  }

  public SSLControl getSSLControl() {
    return m_sslControl;
  }

  public TestRegistry getTestRegistry() {
    return m_testRegistry;
  }

  public Barrier barrier(String name) throws CommunicationException {
    return new BarrierImplementation(m_barrierGroups.getGroup(name),
                                     m_barrierIdentityFactory);
  }
}
