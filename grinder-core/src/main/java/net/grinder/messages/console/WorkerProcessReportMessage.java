// Copyright (C) 2001, 2002 Dirk Feufel
// Copyright (C) 2001 - 2013 Philip Aston
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

package net.grinder.messages.console;

import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.Address;
import net.grinder.communication.AddressAwareMessage;
import net.grinder.communication.CommunicationException;


/**
 * Message for informing the console of worker process status.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 */
public final class WorkerProcessReportMessage
  implements AddressAwareMessage, WorkerProcessReport {

  private static final long serialVersionUID = 3L;

  private final State m_state;
  private final short m_totalNumberOfThreads;
  private final short m_numberOfRunningThreads;

  private transient WorkerAddress m_processAddress;

  /**
   * Creates a new <code>WorkerProcessReportMessage</code> instance.
   *
   * @param finished
   *          The process state. See
   *          {@link net.grinder.common.processidentity.ProcessReport}.
   * @param totalThreads
   *          The total number of threads.
   * @param runningThreads
   *          The number of threads that are still running.
   */
  public WorkerProcessReportMessage(final State finished,
                                    final short runningThreads,
                                    final short totalThreads) {
    m_state = finished;
    m_numberOfRunningThreads = runningThreads;
    m_totalNumberOfThreads = totalThreads;
  }

  /**
   * {@inheritDoc}
   */
  @Override public void setAddress(final Address address)
    throws CommunicationException {

    try {
      m_processAddress = (WorkerAddress) address;
    }
    catch (final ClassCastException e) {
      throw new CommunicationException("Not a worker process address", e);
    }
  }

  /**
   * Accessor for the process identity.
   *
   * @return The process identity.
   */
  @Override
  public WorkerAddress getProcessAddress() {
    return m_processAddress;
  }

  /**
   * Accessor for the process identity.
   *
   * @return The process identity.
   */
  @Override
  public WorkerIdentity getWorkerIdentity() {
    return m_processAddress.getIdentity();
  }

  /**
   * Accessor for the process state.
   *
   * @return The process state.
   */
  @Override
  public State getState() {
    return m_state;
  }

  /**
   * Accessor for the number of running threads for the process.
   *
   * @return The number of running threads.
   */
  @Override
  public short getNumberOfRunningThreads() {
    return m_numberOfRunningThreads;
  }

  /**
   * Accessor for the maximum number of threads for the process.
   *
   * @return The maximum number of threads for the process.
   */
  @Override
  public short getMaximumNumberOfThreads() {
    return m_totalNumberOfThreads;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = m_state.hashCode();
    result = prime * result + m_numberOfRunningThreads;
    result = prime * result + m_totalNumberOfThreads;
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final WorkerProcessReportMessage other = (WorkerProcessReportMessage) o;
      return
          m_state == other.m_state &&
          m_numberOfRunningThreads == other.m_numberOfRunningThreads &&
          m_totalNumberOfThreads == other.m_totalNumberOfThreads;
  }
}
