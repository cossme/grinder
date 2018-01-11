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

package net.grinder.console.synchronisation;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.Handler;
import net.grinder.console.common.processidentity.StubWorkerProcessReport;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.StubProcessReports;
import net.grinder.console.communication.ProcessControl.Listener;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.console.WorkerAddress;
import net.grinder.synchronisation.BarrierGroup;
import net.grinder.synchronisation.BarrierGroups;
import net.grinder.synchronisation.messages.AddBarrierMessage;
import net.grinder.synchronisation.messages.AddWaiterMessage;
import net.grinder.synchronisation.messages.BarrierIdentity;
import net.grinder.synchronisation.messages.CancelWaiterMessage;
import net.grinder.synchronisation.messages.RemoveBarriersMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@link WireDistributedBarriers}.
 *
 * @author Philip Aston
 */
public class TestWireDistributedBarriers {

  @Mock private MessageDispatchRegistry m_messageDispatchRegistry;
  @Mock private ConsoleCommunication m_consoleCommunication;
  @Mock private ProcessControl m_processControl;
  @Mock private BarrierGroups m_barrierGroups;
  @Mock private BarrierIdentity m_barrierIdentity;

  @Captor private ArgumentCaptor<Handler<Message>> m_handlerCaptor;
  @Captor private ArgumentCaptor<Listener> m_processStatusListenerCaptor;

  @Before public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(m_consoleCommunication.getMessageDispatchRegistry())
      .thenReturn(m_messageDispatchRegistry);
  }

  @Test public void testBarrierMessageHandlers() throws Exception {

    final BarrierGroup barrierGroup = mock(BarrierGroup.class);

    when(m_barrierGroups.getGroup("hello")).thenReturn(barrierGroup);

    new WireDistributedBarriers(m_consoleCommunication,
                                m_processControl,
                                m_barrierGroups);

    // Add barrier.
    verify(m_messageDispatchRegistry).set(eq(AddBarrierMessage.class),
                                          m_handlerCaptor.capture());

    m_handlerCaptor.getValue().handle(new AddBarrierMessage("hello"));

    verify(barrierGroup).addBarrier();

    // Add waiter.
    verify(m_messageDispatchRegistry).set(eq(AddWaiterMessage.class),
                                          m_handlerCaptor.capture());

    m_handlerCaptor.getValue().handle(new AddWaiterMessage("hello",
                                                           m_barrierIdentity));
    verify(barrierGroup).addWaiter(m_barrierIdentity);

    // Cancel waiter.
    verify(m_messageDispatchRegistry).set(eq(CancelWaiterMessage.class),
                                          m_handlerCaptor.capture());

    m_handlerCaptor.getValue().handle(
      new CancelWaiterMessage("hello", m_barrierIdentity));

    verify(barrierGroup).cancelWaiter(m_barrierIdentity);

    // Remove barriers.
    verify(m_messageDispatchRegistry).set(eq(RemoveBarriersMessage.class),
                                          m_handlerCaptor.capture());

    m_handlerCaptor.getValue().handle(new RemoveBarriersMessage("hello", 1));

    verify(barrierGroup).removeBarriers(1);
  }

  @Test public void testBarriersCleanUp() throws Exception {
    final BarrierGroup group1 = mock(BarrierGroup.class);
    final BarrierGroup group2 = mock(BarrierGroup.class);

    when(m_barrierGroups.getGroup("g1")).thenReturn(group1);
    when(m_barrierGroups.getGroup("g2")).thenReturn(group2);

    new WireDistributedBarriers(m_consoleCommunication,
                                m_processControl,
                                m_barrierGroups);

    verify(m_messageDispatchRegistry).set(eq(AddBarrierMessage.class),
                                          m_handlerCaptor.capture());

    final Handler<Message> addBarrierHandler = m_handlerCaptor.getValue();

    verify(m_messageDispatchRegistry).set(eq(AddWaiterMessage.class),
                                          m_handlerCaptor.capture());

    final Handler<Message> addWaiterHandler = m_handlerCaptor.getValue();


    // Create a couple of barrier groups.
    final StubAgentIdentity agent = new StubAgentIdentity("agent");
    final WorkerIdentity worker1 = agent.createWorkerIdentity();
    final WorkerIdentity worker2 = agent.createWorkerIdentity();

    final AddBarrierMessage message1 = new AddBarrierMessage("g1");
    message1.setAddress(new WorkerAddress(worker1));
    addBarrierHandler.handle(message1);
    addBarrierHandler.handle(message1);

    final AddBarrierMessage message2 = new AddBarrierMessage("g2");
    message2.setAddress(new WorkerAddress(worker2));
    addBarrierHandler.handle(message2);

    final AddBarrierMessage message3 = new AddBarrierMessage("g1");
    message3.setAddress(new WorkerAddress(worker2));
    addBarrierHandler.handle(message3);

    final AddWaiterMessage message4 = new AddWaiterMessage("g1",
                                                           m_barrierIdentity);
    message4.setAddress(new WorkerAddress(worker2));
    addWaiterHandler.handle(message4);

    verify(group1, times(3)).addBarrier();
    verify(group1).addWaiter(m_barrierIdentity);

    verify(group2).addBarrier();

    verify(m_processControl).addProcessStatusListener(
      m_processStatusListenerCaptor.capture());

    final Listener listener = m_processStatusListenerCaptor.getValue();

    // Worker 1 has gone away.
    listener.update(new ProcessReports[] {
      new StubProcessReports(null,
        new WorkerProcessReport[] {
          new StubWorkerProcessReport(worker2,
                                      ProcessReport.State.RUNNING, 1, 1)
        }),
    });

    verify(group1).removeBarriers(2);

    // All workers have gone away.
    listener.update(new ProcessReports[0]);

    verify(group1).cancelWaiter(m_barrierIdentity);
    verify(group1).removeBarriers(1);

    verify(group2).removeBarriers(1);
  }

  @Test public void testBarriersCleanUpAssertion() throws Exception {

    final CommunicationException exception = new CommunicationException("");

    final BarrierGroup group1 = mock(BarrierGroup.class);
    doThrow(exception).when(group1).removeBarriers(1);

    when(m_barrierGroups.getGroup("g1")).thenReturn(group1);

    new WireDistributedBarriers(m_consoleCommunication,
                                m_processControl,
                                m_barrierGroups);

    verify(m_messageDispatchRegistry).set(eq(AddBarrierMessage.class),
                                          m_handlerCaptor.capture());

    final Handler<Message> addBarrierHandler = m_handlerCaptor.getValue();

    verify(m_messageDispatchRegistry).set(eq(AddWaiterMessage.class),
                                          m_handlerCaptor.capture());


    final AddBarrierMessage message1 = new AddBarrierMessage("g1");
    addBarrierHandler.handle(message1);

    verify(m_processControl)
      .addProcessStatusListener(m_processStatusListenerCaptor.capture());

    final Listener listener = m_processStatusListenerCaptor.getValue();

    try {
      listener.update(new ProcessReports[0]);
      fail("Expected AssertionError");
    }
    catch (AssertionError e) {
      assertSame(exception, e.getCause());
    }
  }
}
