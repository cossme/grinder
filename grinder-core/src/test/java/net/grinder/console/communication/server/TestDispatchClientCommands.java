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

package net.grinder.console.communication.server;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.BlockingSender;
import net.grinder.communication.BlockingSenderWrapper;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.server.messages.GetNumberOfAgentsMessage;
import net.grinder.console.communication.server.messages.ResetRecordingMessage;
import net.grinder.console.communication.server.messages.ResetWorkerProcessesMessage;
import net.grinder.console.communication.server.messages.ResultMessage;
import net.grinder.console.communication.server.messages.StartRecordingMessage;
import net.grinder.console.communication.server.messages.StartWorkerProcessesMessage;
import net.grinder.console.communication.server.messages.StopAgentAndWorkerProcessesMessage;
import net.grinder.console.communication.server.messages.StopRecordingMessage;
import net.grinder.console.communication.server.messages.SuccessMessage;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * Unit tests for {@link DispatchClientCommands}.
 *
 * @author Philip Aston
 */
public class TestDispatchClientCommands extends TestCase {

  public void testRegisterMessageHandlers() throws Exception {

    final RandomStubFactory<SampleModel> modelStubFactory =
      RandomStubFactory.create(SampleModel.class);

    final RandomStubFactory<SampleModelViews> sampleModelViewsStubFactory =
      RandomStubFactory.create(SampleModelViews.class);

    final RandomStubFactory<ProcessControl> processControlStubFactory =
      RandomStubFactory.create(ProcessControl.class);

    final DispatchClientCommands dispatchClientCommands =
      new DispatchClientCommands(modelStubFactory.getStub(),
                                 sampleModelViewsStubFactory.getStub(),
                                 processControlStubFactory.getStub());

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();

    dispatchClientCommands.registerMessageHandlers(messageDispatcher);

    final BlockingSender blockingSender =
      new BlockingSenderWrapper(messageDispatcher);

    assertTrue(blockingSender.blockingSend(new ResetRecordingMessage())
               instanceof SuccessMessage);

    modelStubFactory.assertSuccess("reset");
    modelStubFactory.assertNoMoreCalls();

    sampleModelViewsStubFactory.assertSuccess("resetStatisticsViews");
    sampleModelViewsStubFactory.assertNoMoreCalls();

    assertTrue(blockingSender.blockingSend(new StopRecordingMessage())
      instanceof SuccessMessage);

    assertTrue(blockingSender.blockingSend(new StartRecordingMessage())
      instanceof SuccessMessage);

    modelStubFactory.assertSuccess("stop");
    modelStubFactory.assertSuccess("start");
    modelStubFactory.assertNoMoreCalls();

    processControlStubFactory.setResult(
      "getNumberOfLiveAgents", new Integer(3));
    final ResultMessage message =
      (ResultMessage)blockingSender.blockingSend(
        new GetNumberOfAgentsMessage());
    assertEquals(new Integer(3), message.getResult());

    final GrinderProperties grinderProperties = new GrinderProperties();
    assertTrue(
      blockingSender.blockingSend(
        new StartWorkerProcessesMessage(grinderProperties))
      instanceof SuccessMessage);

    assertTrue(blockingSender.blockingSend(new ResetWorkerProcessesMessage())
      instanceof SuccessMessage);

    assertTrue(
      blockingSender.blockingSend(new StopAgentAndWorkerProcessesMessage())
      instanceof SuccessMessage);

    processControlStubFactory.assertSuccess("getNumberOfLiveAgents");
    processControlStubFactory.assertSuccess("startWorkerProcesses",
      grinderProperties);
    processControlStubFactory.assertSuccess("resetWorkerProcesses");
    processControlStubFactory.assertSuccess("stopAgentAndWorkerProcesses");
    processControlStubFactory.assertNoMoreCalls();
  }
}
