// Copyright (C) 2005 - 2012 Philip Aston
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

import static net.grinder.testutility.SocketUtilities.findFreePort;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.Acceptor;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionIdentity;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.FanOutServerSender;
import net.grinder.communication.Message;
import net.grinder.communication.Sender;
import net.grinder.communication.ServerReceiver;
import net.grinder.communication.StreamReceiver;
import net.grinder.engine.agent.DebugThreadWorker.IsolateGrinderProcessRunner;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.testutility.AbstractJUnit4FileTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;


/**
 * Unit tests for <code>Agent</code>
 * TestAgent.
 *
 * @author Philip Aston
 */
public class TestAgentImplementation extends AbstractJUnit4FileTestCase {

  @Mock private Logger m_logger;

  @Before public void setUp() throws Exception {
    DebugThreadWorkerFactory.setIsolatedRunnerClass(TestRunner.class.getName());
    MockitoAnnotations.initMocks(this);
  }

  @Override
  @After public void tearDown() throws Exception {
    super.tearDown();
    DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
  }

  @Test public void testConstruction() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);
    agent.shutdown();

    verify(m_logger).info("finished");
    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testRunDefaultProperties() throws Exception {
    // Files in cwd.
    final File propertyFile = new File("grinder.properties");
    propertyFile.deleteOnExit();

    final File relativeScriptFile = new File("script/blah");
    relativeScriptFile.deleteOnExit();
    relativeScriptFile.getParentFile().mkdirs();
    relativeScriptFile.getParentFile().deleteOnExit();
    relativeScriptFile.createNewFile();

    try {
      final GrinderProperties properties = new GrinderProperties(propertyFile);

      final Agent agent = new AgentImplementation(m_logger, null, true);

      verifyNoMoreInteractions(m_logger);

      agent.run();

      verify(m_logger).info(contains("The Grinder"));
      verify(m_logger).warn(contains("proceeding"),
                            contains("Failed to connect"));
      verify(m_logger).error(contains("does not exist"));
      verifyNoMoreInteractions(m_logger);
      reset(m_logger);

      properties.setBoolean("grinder.useConsole", false);
      properties.save();

      properties.setFile("grinder.script", relativeScriptFile);
      properties.setInt("grinder.processes", 0);
      properties.save();

      agent.run();

      verify(m_logger).info(contains("The Grinder"));
      verify(m_logger).info(contains("command line"),
                            isA(WorkerProcessCommandLine.class));
      verifyNoMoreInteractions(m_logger);
      reset(m_logger);

      properties.setFile("grinder.logDirectory",
                         getDirectory().getAbsoluteFile());
      properties.save();

      agent.run();

      verify(m_logger).info(contains("The Grinder"));
      verify(m_logger).info(contains("command line"),
                            isA(WorkerProcessCommandLine.class));
      reset(m_logger);

      agent.shutdown();
    }
    finally {
      assertTrue(propertyFile.delete());
      assertTrue(relativeScriptFile.delete());
      assertTrue(relativeScriptFile.getParentFile().delete());
    }
  }

  @Test public void testRun() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);

    verifyNoMoreInteractions(m_logger);

    agent.run();

    verify(m_logger).info(contains("The Grinder"));
    verify(m_logger).warn(contains("proceeding"),
                          contains("Failed to connect"));
    verify(m_logger).error(contains("does not exist"));
    verifyNoMoreInteractions(m_logger);
    reset(m_logger);

    properties.setBoolean("grinder.useConsole", false);
    properties.save();

    agent.run();

    verify(m_logger).info(contains("The Grinder"));
    verify(m_logger).error(contains("does not exist"));
    verifyNoMoreInteractions(m_logger);
    reset(m_logger);

    final File scriptFile = new File(getDirectory(), "script");
    assertTrue(scriptFile.createNewFile());

    final File badFile = new File(scriptFile.getAbsoluteFile(), "blah");
    properties.setFile("grinder.script", badFile);
    properties.save();

    agent.run();

    verify(m_logger).info(contains("The Grinder"));
    verify(m_logger).error(contains("does not exist"));
    verifyNoMoreInteractions(m_logger);
    reset(m_logger);

    properties.setFile("grinder.script", scriptFile);
    properties.setInt("grinder.processes", 0);
    properties.save();

    agent.run();

    verify(m_logger).info(contains("The Grinder"));
    verify(m_logger).info(contains("command line"),
                          isA(WorkerProcessCommandLine.class));
    verifyNoMoreInteractions(m_logger);
    reset(m_logger);

    properties.setBoolean("grinder.debug.singleprocess", true);
    properties.save();

    agent.run();

    verify(m_logger).info(contains("The Grinder"));
    verify(m_logger).info(contains("threads rather than processes"));
    verifyNoMoreInteractions(m_logger);
    reset(m_logger);

    properties.setProperty("grinder.jvm.arguments", "-Dsome_stuff=blah");
    properties.save();

    agent.run();

    verify(m_logger).info(contains("The Grinder"));
    verify(m_logger).info(contains("threads rather than processes"));
    verify(m_logger).warn(contains("grinder.jvm.arguments"),
                          contains("some_stuff"));
    verifyNoMoreInteractions(m_logger);
    reset(m_logger);

    agent.shutdown();

    verify(m_logger).info(contains("finished"));
    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testWithConsole() throws Exception {
    final ConsoleStub console = new ConsoleStub() {
      @Override
      public void onConnect() throws Exception {
        // After we accept an agent connection...
        verify(m_logger).info(contains("The Grinder"));

        verify(m_logger, timeout(5000)).info(contains("connected"),
                                             contains("localhost"));
        verify(m_logger, timeout(5000)).info(contains("waiting"));

        // ...send a start message...
        reset(m_logger);
        final GrinderProperties grinderProperties = new GrinderProperties();
        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        verify(m_logger, timeout(5000)).info("received a start message");

        verify(m_logger, timeout(5000)).error(contains("grinder.py"));

        verify(m_logger, timeout(5000)).info(contains("waiting"));

        // ...send another start message...
        reset(m_logger);
        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        verify(m_logger, timeout(5000)).info("received a start message");

        verify(m_logger, timeout(5000)).info(contains("The Grinder"));

        verify(m_logger, timeout(5000)).error(contains("grinder.py"));

        verify(m_logger, timeout(5000)).info(contains("waiting"));

        // ...then a reset message...
        reset(m_logger);
        getSender().send(new ResetGrinderMessage());

        verify(m_logger, timeout(5000)).info("received a reset message");

        // Version string.
        verify(m_logger, timeout(5000)).info(contains("The Grinder"));

        verify(m_logger, timeout(5000)).info(contains("waiting"));

        // ...now try specifying the script...
        reset(m_logger);
        grinderProperties.setFile(GrinderProperties.SCRIPT, new File("foo.py"));
        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        verify(m_logger, timeout(5000)).info("received a start message");

        verify(m_logger, timeout(5000)).error(contains("foo.py"));

        verify(m_logger, timeout(5000)).info(contains("waiting"));

        // ..then a stop message.
        getSender().send(new StopGrinderMessage());
      }
    };

    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);

    properties.setInt("grinder.consolePort", console.getPort());
    properties.save();

    agent.run();

    console.shutdown();

    verify(m_logger).info("received a stop message");

    verify(m_logger, timeout(5000)).info("console connection shut down");

    agent.shutdown();

    verify(m_logger).info("finished");

    verifyNoMoreInteractions(m_logger);
  }

  @Test public void testRampUp() throws Exception {
    final ConsoleStub console = new ConsoleStub() {
      @Override
      public void onConnect() throws Exception {
        // After we accept an agent connection...
        verify(m_logger).info(contains("The Grinder"));

        verify(m_logger, timeout(5000)).info(contains("connected"),
                                             contains("localhost"));

        verify(m_logger, timeout(5000)).info(contains("waiting"));

        // ...send a start message...
        reset(m_logger);
        final GrinderProperties grinderProperties = new GrinderProperties();
        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        verify(m_logger, timeout(5000)).info("received a start message");

        verify(m_logger, timeout(5000)).info(contains("DEBUG MODE"));

        // 10 workers started.
        verify(m_logger, timeout(5000).times(10)).info(contains("started"));

        // Interrupt our workers.
        reset(m_logger);
        getSender().send(new ResetGrinderMessage());

        verify(m_logger, timeout(5000)).info(contains("reset"));

        verify(m_logger, timeout(5000)).info(contains("The Grinder"));

        verify(m_logger, timeout(5000)).info(contains("waiting"));

        // Now try again, with no ramp up.
        reset(m_logger);
        grinderProperties.setInt("grinder.initialProcesses", 10);

        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        verify(m_logger, timeout(5000)).info("received a start message");

        verify(m_logger, timeout(5000)).info(contains("DEBUG MODE"));

        // 10 workers started.
        verify(m_logger, timeout(5000).times(10)).info(contains("started"));

        // Shut down our workers.
        getSender().send(new StopGrinderMessage());
      }
    };

    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);

    final File script = new File(getDirectory(), "grinder.py");
    assertTrue(script.createNewFile());

    properties.setInt("grinder.consolePort", console.getPort());
    properties.setInt("grinder.initialProcesses", 0);
    properties.setInt("grinder.processes", 10);
    properties.setInt("grinder.processIncrement", 1);
    properties.setInt("grinder.processIncrementInterval", 10);
    properties.setBoolean("grinder.debug.singleprocess", true);
    properties.setFile("grinder.script", script);
    properties.save();

    agent.run();

    console.shutdown();
    agent.shutdown();
  }

  @Test public void testReconnect() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final boolean[] secondConsoleContacted = new boolean[1];

    final GrinderProperties startProperties = new GrinderProperties();

    final ConsoleStub console2 = new ConsoleStub() {
      @Override
      public void onConnect() throws Exception {

        startProperties.setFile("grinder.script", new File("not there"));

        getSender().send(new StartGrinderMessage(startProperties, 99));

        synchronized (secondConsoleContacted) {
          secondConsoleContacted[0] = true;
          secondConsoleContacted.notifyAll();
        }

        getSender().send(new StopGrinderMessage());
      }
    };

    final ConsoleStub console1 = new ConsoleStub() {
      @Override
      public void onConnect() throws Exception {
        startProperties.setInt("grinder.consolePort", console2.getPort());

        getSender().send(new StartGrinderMessage(startProperties, 22));
      }
    };

    properties.setInt("grinder.consolePort", console1.getPort());
    properties.save();

    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);

    agent.run();

    synchronized (secondConsoleContacted) {
      final long start = System.currentTimeMillis();

      while (!secondConsoleContacted[0] &&
             System.currentTimeMillis() < start + 10000) {
        secondConsoleContacted.wait(500);
      }
    }

    assertTrue(secondConsoleContacted[0]);

    console1.shutdown();
    console2.shutdown();

    agent.shutdown();
  }

  private abstract class ConsoleStub {
    private final Acceptor m_acceptor;
    private final ServerReceiver m_receiver;
    private final Sender m_sender;

    public ConsoleStub() throws CommunicationException, IOException {
      final int port = findFreePort();

      m_acceptor = new Acceptor("", port, 1, null);
      m_receiver = new ServerReceiver();
      m_receiver.receiveFrom(
        m_acceptor, new ConnectionType[] { ConnectionType.AGENT }, 1, 10, 1000);
      m_sender = new FanOutServerSender(m_acceptor, ConnectionType.AGENT, 3);

      m_acceptor.addListener(ConnectionType.AGENT, new Acceptor.Listener() {
        @Override
        public void connectionAccepted(final ConnectionType connectionType,
                                       final ConnectionIdentity connection) {
          try {
            onConnect();
          }
          catch (final Throwable e) {
            e.printStackTrace();
          }
        }

        @Override
        public void connectionClosed(final ConnectionType connectionType,
                                     final ConnectionIdentity connection) { }
      });
    }

    public int getPort() {
      return m_acceptor.getPort();
    }

    public final void shutdown() throws CommunicationException {
      m_acceptor.shutdown();
      m_receiver.shutdown();
      getSender().shutdown();
    }

    public final Sender getSender() {
      return m_sender;
    }

    public abstract void onConnect() throws Exception;
  }

  public static class TestRunner implements IsolateGrinderProcessRunner {

    @Override
    public int run(final InputStream in) {
      try {
        final StreamReceiver receiver = new StreamReceiver(in);
        while (true) {
          final Message message = receiver.waitForMessage();
          if (message == null ||
              message instanceof ResetGrinderMessage ||
              message instanceof StopGrinderMessage) {
            return 0;
          }
        }
      }
      catch (final Exception e) {
        e.printStackTrace();
        return -1;
      }
    }
  }
}
