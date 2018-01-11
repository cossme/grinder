// Copyright (C) 2000 - 2009 Philip Aston
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

package net.grinder.engine.messages;

import java.io.File;

import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.Serializer;
import net.grinder.util.Directory;


/**
 * Unit test case for messages that are sent to the worker processes.
 *
 * @author Philip Aston
 */
public class TestWorkerMessages extends AbstractFileTestCase {

  public void testInitialiseGrinderMessage() throws Exception {

    final ScriptLocation script =
      new ScriptLocation(new Directory(new File("d:/foo/bah")),
                         new File("/foo"));

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("Agent");
    final WorkerIdentity workerIdentity = agentIdentity.createWorkerIdentity();
    final WorkerIdentity workerIdentity2 = agentIdentity.createWorkerIdentity();

    final GrinderProperties properties = new GrinderProperties();

    final InitialiseGrinderMessage original =
      new InitialiseGrinderMessage(
        workerIdentity, workerIdentity2, false, script, properties);

    final InitialiseGrinderMessage received = Serializer.serialize(original);

    assertEquals(workerIdentity, received.getWorkerIdentity());
    assertEquals(workerIdentity2, received.getFirstWorkerIdentity());
    assertTrue(!received.getReportToConsole());
    assertEquals(script, received.getScript());
    assertEquals(properties, received.getProperties());

    final InitialiseGrinderMessage another =
      new InitialiseGrinderMessage(
        workerIdentity, workerIdentity2, true, script, properties);

    assertEquals(workerIdentity, another.getWorkerIdentity());
    assertEquals(workerIdentity2, another.getFirstWorkerIdentity());
    assertTrue(another.getReportToConsole());
    assertEquals(script, another.getScript());
  }
}
