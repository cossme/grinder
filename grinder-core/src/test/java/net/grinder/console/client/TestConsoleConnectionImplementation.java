// Copyright (C) 2006 - 2009 Philip Aston
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

package net.grinder.console.client;

import java.io.Serializable;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.BlockingSender;
import net.grinder.communication.CommunicationException;
import net.grinder.console.communication.server.messages.GetNumberOfAgentsMessage;
import net.grinder.console.communication.server.messages.ResetRecordingMessage;
import net.grinder.console.communication.server.messages.ResetWorkerProcessesMessage;
import net.grinder.console.communication.server.messages.ResultMessage;
import net.grinder.console.communication.server.messages.StartRecordingMessage;
import net.grinder.console.communication.server.messages.StartWorkerProcessesMessage;
import net.grinder.console.communication.server.messages.StopAgentAndWorkerProcessesMessage;
import net.grinder.console.communication.server.messages.StopRecordingMessage;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * Unit tests for ConsoleConnectionImplementation.
 *
 * @author Philip Aston
 */
public class TestConsoleConnectionImplementation extends TestCase {

  private final RandomStubFactory<BlockingSender> m_senderStubFactory =
    RandomStubFactory.create(BlockingSender.class);
  private final BlockingSender m_sender = m_senderStubFactory.getStub();

  public void testRecordingControls() throws Exception {
    final ConsoleConnection consoleConnection =
      new ConsoleConnectionImplementation(m_sender);

    consoleConnection.startRecording();
    m_senderStubFactory.assertSuccess("blockingSend",
                                      StartRecordingMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    consoleConnection.stopRecording();
    m_senderStubFactory.assertSuccess("blockingSend",
                                      StopRecordingMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    consoleConnection.resetRecording();
    m_senderStubFactory.assertSuccess("blockingSend",
                                      ResetRecordingMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    final CommunicationException communicationException =
      new CommunicationException("");
    m_senderStubFactory.setThrows("blockingSend", communicationException);

    try {
      consoleConnection.resetRecording();
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      consoleConnection.stopRecording();
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      consoleConnection.startRecording();
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
      assertSame(communicationException, e.getCause());
    }

    consoleConnection.close();
  }

  public void testProcessMessages() throws Exception {
    final ConsoleConnection consoleConnection =
      new ConsoleConnectionImplementation(m_sender);

    m_senderStubFactory.setResult(
      "blockingSend", new ResultMessage(new Integer(10)));
    assertEquals(10, consoleConnection.getNumberOfAgents());
    m_senderStubFactory.assertSuccess("blockingSend",
      GetNumberOfAgentsMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    final GrinderProperties properties = new GrinderProperties();
    consoleConnection.startWorkerProcesses(properties);
    final CallData data =
      m_senderStubFactory.assertSuccess(
        "blockingSend", StartWorkerProcessesMessage.class);
    assertSame(properties,
      ((StartWorkerProcessesMessage)data.getParameters()[0]).getProperties());
    m_senderStubFactory.assertNoMoreCalls();

    consoleConnection.resetWorkerProcesses();
    m_senderStubFactory.assertSuccess(
      "blockingSend", ResetWorkerProcessesMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    consoleConnection.stopAgents();
    m_senderStubFactory.assertSuccess(
      "blockingSend", StopAgentAndWorkerProcessesMessage.class);
    m_senderStubFactory.assertNoMoreCalls();

    m_senderStubFactory.setResult("blockingSend", null);

    try {
      consoleConnection.getNumberOfAgents();
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
    }

    m_senderStubFactory.setResult(
      "blockingSend", new ResultMessage(new Serializable() {}));

    try {
      consoleConnection.getNumberOfAgents();
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
    }

    final CommunicationException communicationException =
      new CommunicationException("");
    m_senderStubFactory.setThrows("blockingSend", communicationException);

    try {
      consoleConnection.getNumberOfAgents();
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      consoleConnection.startWorkerProcesses(new GrinderProperties());
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      consoleConnection.resetWorkerProcesses();
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
      assertSame(communicationException, e.getCause());
    }

    try {
      consoleConnection.stopAgents();
      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
      assertSame(communicationException, e.getCause());
    }

    consoleConnection.close();
  }
}
