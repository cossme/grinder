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

import static net.grinder.testutility.AssertUtilities.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.StreamReceiver;
import net.grinder.engine.agent.DebugThreadWorker.IsolateGrinderProcessRunner;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.messages.InitialiseGrinderMessage;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RedirectStandardStreams;
import net.grinder.util.weave.agent.ExposeInstrumentation;

import org.junit.After;
import org.junit.Test;


/**
 * Unit tests for {@link DebugThreadWorkerFactory}/
 *
 * @author Philip Aston
 */
public class TestDebugThreadWorkerFactory extends AbstractJUnit4FileTestCase {

  private AgentIdentityImplementation m_agentIdentity =
    new AgentIdentityImplementation(getClass().getName());

  private FanOutStreamSender m_fanOutStreamSender = new FanOutStreamSender(1);
  private GrinderProperties m_properties = new GrinderProperties();

  @After public void shutdownStreamSender() throws Exception {
    m_fanOutStreamSender.shutdown();
  }

  @Test public void testFactory() throws Exception {
    m_properties.setProperty("grinder.logDirectory",
                           getDirectory().getAbsolutePath());

    final DebugThreadWorkerFactory factory =
      new DebugThreadWorkerFactory(m_agentIdentity,
                                   m_fanOutStreamSender,
                                   false,
                                   new ScriptLocation(new File("missing.py")),
                                   m_properties);

    final RedirectStandardStreams streams = new RedirectStandardStreams() {
      protected void runWithRedirectedStreams() throws Exception {
        final Worker worker = factory.create(null, null);
        worker.waitFor();
      }
    }.run();

    streams.assertNoStdout();

    assertContains(new String(streams.getStderrBytes()), "File not found");

    // Should have and output and a data file.
    final File[] files = getDirectory().listFiles();

    assertEquals(2, files.length);
  }

  @Test public void testWithBadIsolatedRunner() throws Exception {
    try {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(
        BadClassInaccesible.class.getName());

      final DebugThreadWorkerFactory factory =
        new DebugThreadWorkerFactory(m_agentIdentity,
                                     m_fanOutStreamSender,
                                     false,
                                     new ScriptLocation(new File(".")),
                                     m_properties);

      try {
        factory.create(null, null);
        fail("Expected EngineException");
      }
      catch (EngineException e) {
      }
    }
    finally {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
    }
  }

  @Test public void testWithBadIsolatedRunner2() throws Exception {
    try {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(
        BadClassCantInstantiate.class.getName());

      final DebugThreadWorkerFactory factory2 =
        new DebugThreadWorkerFactory(m_agentIdentity,
                                     m_fanOutStreamSender,
                                     false,
                                     new ScriptLocation(new File(".")),
                                     m_properties);

      try {
        factory2.create(null, null);
        fail("Expected EngineException");
      }
      catch (EngineException e) {
      }
    }
    finally {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
    }
  }

  @Test public void testWithBadIsolatedRunner3() throws Exception {
    try {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(
        BadClassNotAnIsolateGrinderProcessRunner.class.getName());

      final DebugThreadWorkerFactory factory3 =
        new DebugThreadWorkerFactory(m_agentIdentity,
                                     m_fanOutStreamSender,
                                     false,
                                     new ScriptLocation(new File(".")),
                                     m_properties);

      try {
        factory3.create(null, null);
        fail("Expected ClassCastException");
      }
      catch (ClassCastException e) {
      }
    }
    finally {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
    }
  }

  @Test public void testWithBadIsolatedRunner4() throws Exception {
    try {
      DebugThreadWorkerFactory.setIsolatedRunnerClass("Not a class");

      final DebugThreadWorkerFactory factory3 =
        new DebugThreadWorkerFactory(m_agentIdentity,
                                     m_fanOutStreamSender,
                                     false,
                                     new ScriptLocation(new File(".")),
                                     m_properties);

      try {
        factory3.create(null, null);
        fail("Expected AssertionError");
      }
      catch (AssertionError e) {
      }
    }
    finally {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
    }
  }

  @Test public void testIsolation() throws Exception {
    try {
      DebugThreadWorkerFactory
        .setIsolatedRunnerClass(GoodRunner.class.getName());

      final DebugThreadWorkerFactory factory =
        new DebugThreadWorkerFactory(m_agentIdentity,
                                     m_fanOutStreamSender,
                                     false,
                                     new ScriptLocation(new File(".")),
                                     m_properties);

      final RedirectStandardStreams rss0 = new RedirectStandardStreams() {
        protected void runWithRedirectedStreams() throws Exception {
          final Worker worker = factory.create(null, null);
          worker.waitFor();
        }
      };

      rss0.run();

      final RedirectStandardStreams rss1 = new RedirectStandardStreams() {
        protected void runWithRedirectedStreams() throws Exception {
          final Worker worker = factory.create(null, null);
          worker.waitFor();
        }
      };

      rss1.run();

      final String worker0Result = new String(rss0.getStdoutBytes());
      final String worker1Result = new String(rss1.getStdoutBytes());

      AssertUtilities.assertContains(worker0Result, "Hello from 0 count is 1");
      AssertUtilities.assertContains(worker1Result, "Hello from 1 count is 1");
    }
    finally {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
    }
  }

  @Test public void testIsolationWithSharedClasses() throws Exception {
    try {
      m_properties.setProperty("grinder.debug.singleprocess.sharedclasses",
                               MyStaticHolder.class.getName());

      DebugThreadWorkerFactory
        .setIsolatedRunnerClass(GoodRunner.class.getName());

      final DebugThreadWorkerFactory factory =
        new DebugThreadWorkerFactory(m_agentIdentity,
                                     m_fanOutStreamSender,
                                     false,
                                     new ScriptLocation(new File(".")),
                                     m_properties);

      final RedirectStandardStreams rss0 = new RedirectStandardStreams() {
        protected void runWithRedirectedStreams() throws Exception {
          final Worker worker = factory.create(null, null);
          worker.waitFor();
        }
      };

      rss0.run();

      final RedirectStandardStreams rss1 = new RedirectStandardStreams() {
        protected void runWithRedirectedStreams() throws Exception {
          final Worker worker = factory.create(null, null);
          worker.waitFor();
        }
      };

      rss1.run();

      final String worker0Result = new String(rss0.getStdoutBytes());
      final String worker1Result = new String(rss1.getStdoutBytes());

      AssertUtilities.assertContains(worker0Result, "Hello from 0 count is 1");
      AssertUtilities.assertContains(worker1Result, "Hello from 1 count is 2");
    }
    finally {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
    }
  }

  // Bug 2958145.
  @Test public void testExposeInstrumentationNotIsolated() throws Exception {

    final Instrumentation originalInstrumentation =
      ExposeInstrumentation.getInstrumentation();

    final Instrumentation instrumentation = mock(Instrumentation.class);

    DebugThreadWorkerFactory.setIsolatedRunnerClass(
      AccessInstrumentationRunner.class.getName());

    try {
      ExposeInstrumentation.premain("", instrumentation);

      final DebugThreadWorkerFactory factory =
        new DebugThreadWorkerFactory(m_agentIdentity,
                                     m_fanOutStreamSender,
                                     false,
                                     new ScriptLocation(new File(".")),
                                     m_properties);

      final RedirectStandardStreams rss0 = new RedirectStandardStreams() {
        protected void runWithRedirectedStreams() throws Exception {
          final Worker worker = factory.create(null, null);
          worker.waitFor();
        }
      };

      rss0.run();

      final String worker0Result = new String(rss0.getStdoutBytes());

      AssertUtilities.assertContains(
        worker0Result,
        Integer.toString(instrumentation.hashCode()));
    }
    finally {
      DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
      ExposeInstrumentation.premain("", originalInstrumentation);
    }
  }


  public static class BadClassInaccesible {
    BadClassInaccesible() { }
  }

  public static abstract class BadClassCantInstantiate {
    public BadClassCantInstantiate() { }
  }

  public static class BadClassNotAnIsolateGrinderProcessRunner { }

  public static class MyStaticHolder {
    private static int s_number = 0;

    public static void incrementNumber() {
      ++s_number;
    }

    public static int getNumber() {
      return s_number;
    }
  }

  public abstract static class AbstractGoodRunner
    implements IsolateGrinderProcessRunner {
    protected InitialiseGrinderMessage initialise(InputStream inputStream) {

      final StreamReceiver streamReceiver = new StreamReceiver(inputStream);

      try {
        return (InitialiseGrinderMessage)streamReceiver.waitForMessage();
      }
      catch (CommunicationException e) {
        throw new AssertionError(e);
      }
    }
  }

  public static class GoodRunner extends AbstractGoodRunner{
    public int run(InputStream agentInputStream) {
      final InitialiseGrinderMessage message = initialise(agentInputStream);

      MyStaticHolder.incrementNumber();

      System.out.println(
        "Hello from " + message.getWorkerIdentity().getNumber() +
        " count is " + MyStaticHolder.getNumber());

      return 0;
    }
  }

  public static class AccessInstrumentationRunner
    extends AbstractGoodRunner {

    public int run(InputStream agentInputStream) {
      initialise(agentInputStream);

      System.out.println(ExposeInstrumentation.getInstrumentation().hashCode());

      return 0;
    }
  }
}
