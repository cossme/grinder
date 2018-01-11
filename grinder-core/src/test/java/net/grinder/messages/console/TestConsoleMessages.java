// Copyright (C) 2000 - 2013 Philip Aston
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

import static net.grinder.testutility.AssertUtilities.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashSet;

import net.grinder.common.processidentity.ProcessReport.State;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.Address;
import net.grinder.communication.CommunicationException;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.agent.StubCacheHighWaterMark;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticExpressionFactory;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.testutility.Serializer;

import org.junit.Test;


/**
 *  Unit test case for console messages.
 *
 * @author Philip Aston
 */
public class TestConsoleMessages {

  @Test public void testRegisterStatisticsViewMessage() throws Exception {

    final StatisticExpressionFactory statisticExpressionFactory =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory();

    final ExpressionView expressionView =
      statisticExpressionFactory.createExpressionView(
        "One", "userLong0", false);

    final RegisterExpressionViewMessage original =
      new RegisterExpressionViewMessage(expressionView);

    final RegisterExpressionViewMessage received =
      Serializer.serialize(original);

    assertEquals(original.getExpressionView(),
                 received.getExpressionView());

    final ExpressionView view2 =
      statisticExpressionFactory
        .createExpressionView("My view2",
          statisticExpressionFactory.createExpression("userLong0"));
    try {
      Serializer.serialize(new RegisterExpressionViewMessage(view2));
      fail("Expected IOException");
    }
    catch (final IOException e) {
    }

    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    final ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
    original.writeExternal(objectStream);
    objectStream.close();

    // Corrupt the serialised message.
    final byte[] bytes = byteStream.toByteArray();
    bytes[bytes.length - 5] = 'X';

    final RegisterExpressionViewMessage incomingMessage =
      new RegisterExpressionViewMessage();
    try {
      incomingMessage.readExternal(
        new ObjectInputStream(new ByteArrayInputStream(bytes)));

      fail("Expected IOException");
    } catch (final IOException e) {
    }
  }

  @Test public void testRegisterTestsMessage() throws Exception {

    final Collection<net.grinder.common.Test> c =
      new HashSet<net.grinder.common.Test>();

    final RegisterTestsMessage original = new RegisterTestsMessage(c);

    assertEquals(c, original.getTests());

    final RegisterTestsMessage received = Serializer.serialize(original);

    assertEquals(original.getTests(), received.getTests());
  }

  @Test public void testReportStatisticsMessage() throws Exception {

    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();
    final TestStatisticsMap statisticsDelta =
      new TestStatisticsMap(statisticsSetFactory);

    final ReportStatisticsMessage original =
      new ReportStatisticsMessage(statisticsDelta);

    assertEquals(statisticsDelta, original.getStatisticsDelta());

    final ReportStatisticsMessage received = Serializer.serialize(original);

    assertEquals(original.getStatisticsDelta(), received.getStatisticsDelta());
  }

  @Test public void testWorkerReportMessage() throws Exception {

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("Agent");
    final WorkerIdentity workerIdentity = agentIdentity.createWorkerIdentity();

    final WorkerProcessReportMessage original =
      new WorkerProcessReportMessage(State.RUNNING, (short)2, (short)3);

    final WorkerAddress address = new WorkerAddress(workerIdentity);
    original.setAddress(address);

    assertEquals(workerIdentity, original.getWorkerIdentity());
    assertEquals(address, original.getProcessAddress());
    assertEquals(State.RUNNING, original.getState());
    assertEquals(2, original.getNumberOfRunningThreads());
    assertEquals(3, original.getMaximumNumberOfThreads());

    final WorkerProcessReportMessage received = Serializer.serialize(original);

    assertEquals(workerIdentity, original.getWorkerIdentity());
    assertEquals(address, original.getProcessAddress());
    assertEquals(State.RUNNING, received.getState());
    assertEquals(2, received.getNumberOfRunningThreads());
    assertEquals(3, received.getMaximumNumberOfThreads());
  }

  @Test public void testWorkerReportMessageEquality() throws Exception {

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("Agent");
    final WorkerIdentity workerIdentity = agentIdentity.createWorkerIdentity();

    final WorkerProcessReportMessage m1 =
      new WorkerProcessReportMessage(State.RUNNING, (short)2, (short)3);

    assertEquals(m1, m1);
    assertEquals(m1.hashCode(), m1.hashCode());
    assertNotEquals(m1, null);
    assertNotEquals(m1, this);

    final WorkerProcessReportMessage m2 =
        new WorkerProcessReportMessage(State.RUNNING, (short)2, (short)3);

    m2.setAddress(new WorkerAddress(workerIdentity));

    assertEquals(m1, m2);
    assertEquals(m2.hashCode(), m2.hashCode());

    assertNotEquals(m1,
                    new WorkerProcessReportMessage(State.STARTED,
                                                   (short)2,
                                                   (short)3));

    assertNotEquals(m1,
                    new WorkerProcessReportMessage(State.RUNNING,
                                                   (short)1,
                                                   (short)3));
    assertNotEquals(m1,
                    new WorkerProcessReportMessage(State.RUNNING,
                                                   (short)2,
                                                   (short)2));

  }

  @Test public void testWorkerReportMessageBadAddress() throws Exception {

    final WorkerProcessReportMessage message =
      new WorkerProcessReportMessage(State.RUNNING, (short)2, (short)3);

    final Address badAddress =
      new AgentAddress(new StubAgentIdentity("Agent"));

    try {
      message.setAddress(badAddress);
      fail("Expected CommunicationException");
    }
    catch (final CommunicationException e) {
    }
  }

  @Test public void testAgentReportMessage() throws Exception {

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("Agent");

    final CacheHighWaterMark cacheHighWaterMark =
      new StubCacheHighWaterMark("", 100);

    final AgentProcessReportMessage original =
      new AgentProcessReportMessage(State.RUNNING, cacheHighWaterMark);

    final AgentAddress address = new AgentAddress(agentIdentity);
    original.setAddress(address);

    assertEquals(agentIdentity, original.getAgentIdentity());
    assertEquals(address, original.getProcessAddress());
    assertEquals(cacheHighWaterMark, original.getCacheHighWaterMark());
    assertEquals(State.RUNNING, original.getState());

    final AgentProcessReportMessage received = Serializer.serialize(original);

    assertEquals(agentIdentity, original.getAgentIdentity());
    assertEquals(address, original.getProcessAddress());
    assertEquals(State.RUNNING, received.getState());
    assertEquals(cacheHighWaterMark, received.getCacheHighWaterMark());
  }

  @Test public void testAgentReportMessageEquality() throws Exception {

    final StubAgentIdentity agentIdentity =
        new StubAgentIdentity("Agent");

    final CacheHighWaterMark cacheHighWaterMark =
      new StubCacheHighWaterMark("", 100);

    final AgentProcessReportMessage m1 =
      new AgentProcessReportMessage(State.RUNNING, cacheHighWaterMark);

    assertEquals(m1, m1);
    assertEquals(m1.hashCode(), m1.hashCode());
    assertNotEquals(m1, null);
    assertNotEquals(m1, this);

    final AgentProcessReportMessage m2 =
        new AgentProcessReportMessage(State.RUNNING, cacheHighWaterMark);

    m2.setAddress(new AgentAddress(agentIdentity));

    assertEquals(m1, m2);
    assertEquals(m2.hashCode(), m2.hashCode());

    assertNotEquals(m1,
                    new AgentProcessReportMessage(State.UNKNOWN,
                                                  cacheHighWaterMark));

    assertNotEquals(m1,
                    new AgentProcessReportMessage(
                      State.RUNNING,
                      new StubCacheHighWaterMark("", 101)));
  }

  @Test public void testAgentReportMessageBadAddress() throws Exception {

    final AgentProcessReportMessage message =
      new AgentProcessReportMessage(State.RUNNING, null);

    final Address badAddress =
      new WorkerAddress(new StubAgentIdentity("Agent").createWorkerIdentity());

    try {
      message.setAddress(badAddress);
      fail("Expected CommunicationException");
    }
    catch (final CommunicationException e) {
    }
  }
}
