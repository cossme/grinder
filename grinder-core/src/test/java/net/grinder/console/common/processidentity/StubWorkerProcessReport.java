// Copyright (C) 2008 - 2012 Philip Aston
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

package net.grinder.console.common.processidentity;

import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.messages.console.WorkerAddress;


public final class StubWorkerProcessReport
  implements WorkerProcessReport {

  private final State m_state;
  private final short m_totalNumberOfThreads;
  private final short m_numberOfRunningThreads;
  private final WorkerAddress m_workerAddress;

  public StubWorkerProcessReport(WorkerIdentity workerIdentity,
                                 State finished,
                                 int runningThreads,
                                 int totalThreads) {
    m_workerAddress = new WorkerAddress(workerIdentity);
    m_state = finished;
    m_numberOfRunningThreads = (short)runningThreads;
    m_totalNumberOfThreads = (short)totalThreads;
  }

  public WorkerAddress getProcessAddress() {
    return m_workerAddress;
  }

  public WorkerIdentity getWorkerIdentity() {
    return m_workerAddress.getIdentity();
  }

  public State getState() {
    return m_state;
  }

  public short getNumberOfRunningThreads() {
    return m_numberOfRunningThreads;
  }

  public short getMaximumNumberOfThreads() {
    return m_totalNumberOfThreads;
  }

  public int hashCode() {
    return m_workerAddress.hashCode();
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof WorkerProcessReport)) {
      return false;
    }

    final WorkerProcessReport other = (WorkerProcessReport)o;

    return
      this.getState() == other.getState() &&
      this.getNumberOfRunningThreads() == other.getNumberOfRunningThreads() &&
      this.getMaximumNumberOfThreads() == other.getMaximumNumberOfThreads() &&
      this.getWorkerIdentity().equals(other.getWorkerIdentity());
  }

  public String toString() {
    return
      "StubWorkerProcessReport(" +
      getWorkerIdentity() + ", " +
      getState() + ", " +
      getNumberOfRunningThreads() + ", " +
      getMaximumNumberOfThreads() + ")";
  }
}
