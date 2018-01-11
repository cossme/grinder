// Copyright (C) 2005 - 2011 Philip Aston
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

package net.grinder.engine.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.communication.StreamSender;
import net.grinder.engine.agent.AgentIdentityImplementation.WorkerIdentityImplementation;
import net.grinder.engine.agent.DebugThreadWorker.IsolateGrinderProcessRunner;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RedirectStandardStreams;

import org.junit.Test;


/**
 * Unit tests for {@link DebugThreadWorker}/
 *
 * @author Philip Aston
 */
public class TestDebugThreadWorker {

  private final WorkerIdentityImplementation m_workerIdentity =
    new AgentIdentityImplementation(getClass().getName())
    .createWorkerIdentity();

  @Test public void testDebugThreadWorker() throws Exception {

    final DelegatingStubFactory<IsolatedGrinderProcessRunner>
      isolateGrinderProcessRunnerStubFactory =
        DelegatingStubFactory.create(new IsolatedGrinderProcessRunner());

    final DebugThreadWorker worker =
      new DebugThreadWorker(m_workerIdentity,
                            isolateGrinderProcessRunnerStubFactory.getStub());

    assertEquals(m_workerIdentity, worker.getIdentity());
    assertNotNull(worker.getCommunicationStream());

    worker.start();

    final int[] resultHolder = { -1 };

    final Thread waitThread = new Thread() {
      public void run() {
        resultHolder[0] = worker.waitFor();
      }
    };

    waitThread.start();

    assertEquals(-1, resultHolder[0]);
    assertTrue(waitThread.isAlive());

    final RedirectStandardStreams streams = new RedirectStandardStreams() {
      protected void runWithRedirectedStreams() throws Exception {
        new StreamSender(worker.getCommunicationStream()).shutdown();
        waitThread.join();
      }
    };

    streams.run();

    isolateGrinderProcessRunnerStubFactory.assertSuccess(
      "run", InputStream.class);

    assertEquals(-2, resultHolder[0]);
    final String output = new String(streams.getStderrBytes());
    assertTrue(output.indexOf("No control stream from agent") > 0);

    worker.destroy();
  }

  @Test public void testInterruption() throws Exception {

    final DebugThreadWorker debugThreadWorker =
      new DebugThreadWorker(
        m_workerIdentity,
        new IsolateGrinderProcessRunner() {
          public int run(InputStream agentInputStream) {
            try {
              Thread.sleep(10000);
            }
            catch (InterruptedException e) {
            }
            return 0;
          }});

    debugThreadWorker.start();

    Thread.currentThread().interrupt();

    try {
      debugThreadWorker.waitFor();
      fail("Expected UncheckedInterruptedException");
    }
    catch (UncheckedInterruptedException e) {
    }

    debugThreadWorker.destroy();
    debugThreadWorker.waitFor();
  }
}
