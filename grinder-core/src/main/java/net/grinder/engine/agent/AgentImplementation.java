// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2013 Philip Aston
// Copyright (C) 2004 Bertrand Ave
// Copyright (C) 2008 Pawel Lacinski
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

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.GrinderProperties.PersistenceException;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.communication.ClientReceiver;
import net.grinder.communication.ClientSender;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.IgnoreShutdownSender;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.TeeSender;
import net.grinder.engine.common.ConnectorFactory;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.communication.ConsoleListener;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.AgentProcessReportMessage;
import net.grinder.util.Directory;
import net.grinder.util.thread.Condition;

import org.slf4j.Logger;


/**
 * This is the entry point of The Grinder agent process.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @author Bertrand Ave
 * @author Pawel Lacinski
 */
public final class AgentImplementation implements Agent {

  private final Logger m_logger;
  private final File m_alternateFile;
  private final boolean m_proceedWithoutConsole;

  private final Timer m_timer = new Timer(true);
  private final Condition m_eventSynchronisation = new Condition();
  private final AgentIdentityImplementation m_agentIdentity;
  private final ConsoleListener m_consoleListener;
  private final FanOutStreamSender m_fanOutStreamSender =
    new FanOutStreamSender(3);
  private final ConnectorFactory m_connectorFactory =
    new ConnectorFactory(ConnectionType.AGENT);

  /**
   * We use an most one file store throughout an agent's life, but can't
   * initialise it until we've read the properties and connected to the console.
   */
  private volatile FileStore m_fileStore;

  /**
   * Constructor.
   *
   * @param logger Logger.
   * @param alternateFile Alternative properties file, or {@code null}.
   * @param proceedWithoutConsole {@code true} => proceed if a console
   * connection could not be made.
   * @throws GrinderException If an error occurs.
   */
  public AgentImplementation(final Logger logger,
                             final File alternateFile,
                             final boolean proceedWithoutConsole)
    throws GrinderException {

    m_logger = logger;
    m_alternateFile = alternateFile;
    m_proceedWithoutConsole = proceedWithoutConsole;

    m_consoleListener = new ConsoleListener(m_eventSynchronisation, m_logger);
    m_agentIdentity = new AgentIdentityImplementation(getHostName());
  }

  /**
   * Run the Grinder agent process.
   *
   * @throws GrinderException
   *             If an error occurs.
   */
  // CHECKSTYLE.OFF: MethodLength
  @Override
  public void run() throws GrinderException {

    StartGrinderMessage startMessage = null;
    ConsoleCommunication consoleCommunication = null;

    try {
      while (true) {
        m_logger.info(GrinderBuild.getName());

        ScriptLocation script = null;
        GrinderProperties properties;

        do {
          properties =
            createAndMergeProperties(startMessage != null ?
                                     startMessage.getProperties() : null);

          m_agentIdentity.setName(
            properties.getProperty("grinder.hostID", getHostName()));

          final Connector connector =
            properties.getBoolean("grinder.useConsole", true) ?
            m_connectorFactory.create(properties) : null;

          // We only reconnect if the connection details have changed.
          if (consoleCommunication != null &&
              !consoleCommunication.getConnector().equals(connector)) {
            shutdownConsoleCommunication(consoleCommunication);
            consoleCommunication = null;
            // Accept any startMessage from previous console - see bug 2092881.
          }

          if (consoleCommunication == null && connector != null) {
            try {
              consoleCommunication = new ConsoleCommunication(connector);
              consoleCommunication.start();
              m_logger.info(
                "connected to console at {}", connector.getEndpointAsString());
            }
            catch (final CommunicationException e) {
              if (m_proceedWithoutConsole) {
                m_logger.warn(
                  "{}, proceeding without the console; set " +
                  "grinder.useConsole=false to disable this warning.",
                  e.getMessage());
              }
              else {
                m_logger.error(e.getMessage());
                return;
              }
            }
          }

          if (consoleCommunication != null && startMessage == null) {
            m_logger.info("waiting for console signal");
            m_consoleListener.waitForMessage();

            if (m_consoleListener.received(ConsoleListener.START)) {
              startMessage = m_consoleListener.getLastStartGrinderMessage();
              continue; // Loop to handle new properties.
            }
            else {
              break;    // Another message, check at end of outer while loop.
            }
          }

          if (startMessage != null) {
            final GrinderProperties messageProperties =
              startMessage.getProperties();
            final Directory fileStoreDirectory = m_fileStore.getDirectory();

            final File messagePropertiesFile =
                messageProperties.getAssociatedFile();

            if (messagePropertiesFile != null) {
              // If the properties has an associated file, we rebase its
              // script location, relative to the file.

              if (!messagePropertiesFile.isAbsolute()) {
                // If the properties file is relative, resolve it based
                // on the file store location. This will be the case with
                // the standard console, but accept absolute files - bug #232.
                messageProperties.setAssociatedFile(
                  fileStoreDirectory.getFile(messagePropertiesFile));
              }

              final File consoleScript =
                  messageProperties.resolveRelativeFile(
                    messageProperties.getFile(
                      GrinderProperties.SCRIPT,
                      GrinderProperties.DEFAULT_SCRIPT));

              // We fall back to the agent properties if the start message
              // doesn't specify a script and there is no default script.
              if (messageProperties.containsKey(GrinderProperties.SCRIPT) ||
                  consoleScript.canRead()) {
                // The script directory may not be the file's direct parent.
                script = new ScriptLocation(fileStoreDirectory, consoleScript);
              }
            }

            m_agentIdentity.setNumber(startMessage.getAgentNumber());
          }
          else {
            m_agentIdentity.setNumber(-1);
          }

          if (script == null) {
            final File scriptFile =
              properties.resolveRelativeFile(
                properties.getFile(GrinderProperties.SCRIPT,
                                   GrinderProperties.DEFAULT_SCRIPT));

            script = new ScriptLocation(scriptFile);
          }

          if (!script.getFile().canRead()) {
            m_logger.error("The script file '" + script +
                           "' does not exist or is not readable.");
            script = null;
            break;
          }
        }
        while (script == null);

        if (script != null) {
          final String jvmArguments =
            properties.getProperty("grinder.jvm.arguments");

          final WorkerFactory workerFactory;

          if (!properties.getBoolean("grinder.debug.singleprocess", false)) {

            final WorkerProcessCommandLine workerCommandLine =
              new WorkerProcessCommandLine(properties,
                                           System.getProperties(),
                                           jvmArguments,
                                           script.getDirectory());

            m_logger.info("Worker process command line: {}", workerCommandLine);

            workerFactory =
              new ProcessWorkerFactory(
                workerCommandLine, m_agentIdentity, m_fanOutStreamSender,
                consoleCommunication != null, script, properties);
          }
          else {
            m_logger.info(
              "DEBUG MODE: Spawning threads rather than processes");

            if (jvmArguments != null) {
              m_logger.warn(
                "grinder.jvm.arguments ({}) ignored in single process mode",
                jvmArguments);
            }

            workerFactory =
              new DebugThreadWorkerFactory(
                m_agentIdentity, m_fanOutStreamSender,
                consoleCommunication != null, script, properties);
          }

          final WorkerLauncher workerLauncher =
            new WorkerLauncher(properties.getInt("grinder.processes", 1),
                               workerFactory,
                               m_eventSynchronisation,
                               m_logger);

          final int increment =
            properties.getInt("grinder.processIncrement", 0);

          if (increment > 0) {
            final boolean moreProcessesToStart =
              workerLauncher.startSomeWorkers(
                properties.getInt("grinder.initialProcesses", increment));

            if (moreProcessesToStart) {
              final int incrementInterval =
                properties.getInt("grinder.processIncrementInterval", 60000);

              final RampUpTimerTask rampUpTimerTask =
                new RampUpTimerTask(workerLauncher, increment);

              m_timer.scheduleAtFixedRate(
                rampUpTimerTask, incrementInterval, incrementInterval);
            }
          }
          else {
            workerLauncher.startAllWorkers();
          }

          // Wait for a termination event.
          synchronized (m_eventSynchronisation) {
            final long maximumShutdownTime = 20000;
            long consoleSignalTime = -1;

            while (!workerLauncher.allFinished()) {
              if (consoleSignalTime == -1 &&
                  m_consoleListener.checkForMessage(ConsoleListener.ANY ^
                                                    ConsoleListener.START)) {
                workerLauncher.dontStartAnyMore();
                consoleSignalTime = System.currentTimeMillis();
              }

              if (consoleSignalTime >= 0 &&
                  System.currentTimeMillis() - consoleSignalTime >
                  maximumShutdownTime) {

                m_logger.info("forcibly terminating unresponsive processes");

                // destroyAllWorkers() prevents further workers from starting.
                workerLauncher.destroyAllWorkers();
              }

              m_eventSynchronisation.waitNoInterrruptException(
                maximumShutdownTime);
            }
          }

          workerLauncher.shutdown();
        }

        if (consoleCommunication == null) {
          break;
        }
        else {
          // Ignore any pending start messages.
          m_consoleListener.discardMessages(ConsoleListener.START);

          if (!m_consoleListener.received(ConsoleListener.ANY)) {
            // We've got here naturally, without a console signal.
            m_logger.info("finished, waiting for console signal");
            m_consoleListener.waitForMessage();
          }

          if (m_consoleListener.received(ConsoleListener.START)) {
            startMessage = m_consoleListener.getLastStartGrinderMessage();
          }
          else if (m_consoleListener.received(ConsoleListener.STOP |
                                              ConsoleListener.SHUTDOWN)) {
            break;
          }
          else {
            // ConsoleListener.RESET or natural death.
            startMessage = null;
          }
        }
      }
    }
    finally {
      shutdownConsoleCommunication(consoleCommunication);
    }
  }
  // CHECKSTYLE.ON: MethodLength

  private GrinderProperties createAndMergeProperties(
      final GrinderProperties startMessageProperties)
    throws PersistenceException {

    final GrinderProperties properties =
      new GrinderProperties(
        m_alternateFile != null ?
            m_alternateFile : GrinderProperties.DEFAULT_PROPERTIES);

    if (startMessageProperties != null) {
      properties.putAll(startMessageProperties);
    }

    // Ensure the log directory property is set and is absolute.
    final File nullFile = new File("");

    final File originalLogDirectory =
      properties.getFile(GrinderProperties.LOG_DIRECTORY, nullFile);

    if (!originalLogDirectory.isAbsolute()) {
      properties.setFile(GrinderProperties.LOG_DIRECTORY,
        new File(nullFile.getAbsoluteFile(), originalLogDirectory.getPath()));
    }

    return properties;
  }

  private void shutdownConsoleCommunication(
    final ConsoleCommunication consoleCommunication) {

    if (consoleCommunication != null) {
      consoleCommunication.shutdown();
    }

    m_consoleListener.discardMessages(ConsoleListener.ANY);
  }

  /**
   * Clean up resources.
   */
  @Override
  public void shutdown() {
    m_timer.cancel();
    m_fanOutStreamSender.shutdown();
    m_consoleListener.shutdown();

    m_logger.info("finished");
  }

  private static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    }
    catch (final UnknownHostException e) {
      return "UNNAMED HOST";
    }
  }

  private class RampUpTimerTask extends TimerTask {

    private final WorkerLauncher m_processLauncher;
    private final int m_processIncrement;

    public RampUpTimerTask(final WorkerLauncher processLauncher,
                           final int processIncrement) {
      m_processLauncher = processLauncher;
      m_processIncrement = processIncrement;
    }

    @Override
    public void run() {
      try {
        final boolean moreProcessesToStart =
          m_processLauncher.startSomeWorkers(m_processIncrement);

        if (!moreProcessesToStart) {
          super.cancel();
        }
      }
      catch (final EngineException e) {
        // Really an assertion.
        m_logger.error("Failed to start processes", e);
      }
    }
  }

  private final class ConsoleCommunication {
    private final ClientSender m_sender;
    private final Connector m_connector;
    private final TimerTask m_reportRunningTask;
    private final MessagePump m_messagePump;

    public ConsoleCommunication(final Connector connector)
        throws CommunicationException, FileStore.FileStoreException {

      final ClientReceiver receiver =
        ClientReceiver.connect(connector, new AgentAddress(m_agentIdentity));
      m_sender = ClientSender.connect(receiver);
      m_connector = connector;

      if (m_fileStore == null) {
        // Only create the file store if we connected.
        m_fileStore =
          new FileStore(
            new File("./" + m_agentIdentity.getName() + "-file-store"),
            m_logger);
      }

      m_sender.send(
        new AgentProcessReportMessage(ProcessReport.State.STARTED,
                                      m_fileStore.getCacheHighWaterMark()));

      final MessageDispatchSender fileStoreMessageDispatcher =
        new MessageDispatchSender();
      m_fileStore.registerMessageHandlers(fileStoreMessageDispatcher);

      final MessageDispatchSender messageDispatcher =
        new MessageDispatchSender();
      m_consoleListener.registerMessageHandlers(messageDispatcher);

      // Everything that the file store doesn't handle is tee'd to the
      // worker processes and our message handlers.
      fileStoreMessageDispatcher.addFallback(
        new TeeSender(messageDispatcher,
                      new IgnoreShutdownSender(m_fanOutStreamSender)));

      m_messagePump =
        new MessagePump(receiver, fileStoreMessageDispatcher, 1);

      m_reportRunningTask = new TimerTask() {
        @Override
        public void run() {
          try {
            m_sender.send(
              new AgentProcessReportMessage(
                ProcessReport.State.RUNNING,
                m_fileStore.getCacheHighWaterMark()));
          }
          catch (final CommunicationException e) {
            cancel();
            m_logger.error(e.getLocalizedMessage(), e);
          }
        }
      };
    }

    public void start() {
      m_messagePump.start();
      m_timer.schedule(m_reportRunningTask, 1000, 1000);
    }

    public Connector getConnector() {
      return m_connector;
    }

    public void shutdown() {
      m_reportRunningTask.cancel();

      try {
        m_sender.send(
          new AgentProcessReportMessage(
            ProcessReport.State.FINISHED,
            m_fileStore.getCacheHighWaterMark()));
      }
      catch (final CommunicationException e) {
        // Ignore - peer has probably shut down.
      }
      finally {
        m_messagePump.shutdown();
      }
    }
  }
}
