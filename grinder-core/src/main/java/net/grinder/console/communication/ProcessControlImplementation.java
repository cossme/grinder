// Copyright (C) 2007 - 2012 Philip Aston
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

package net.grinder.console.communication;

import java.io.File;
import java.util.Timer;

import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.AgentProcessReportMessage;
import net.grinder.messages.console.WorkerProcessReportMessage;
import net.grinder.translation.Translations;
import net.grinder.util.AllocateLowestNumber;
import net.grinder.util.AllocateLowestNumberImplementation;
import net.grinder.util.Directory;


/**
 * Implementation of {@link ProcessControl}.
 *
 * @author Philip Aston
 */
public class ProcessControlImplementation implements ProcessControl {

  private final ConsoleCommunication m_consoleCommunication;

  private final ProcessStatusImplementation m_processStatusSet;

  private final AllocateLowestNumber m_agentNumberMap =
    new AllocateLowestNumberImplementation();

  private final Translations m_translations;

  /**
   * Constructor.
   *
   * @param timer
   *          Timer that can be used to schedule housekeeping tasks.
   * @param consoleCommunication
   *          The console communication handler.
   * @param translations
   *          Translation service.
   */
  public ProcessControlImplementation(
    final Timer timer,
    final ConsoleCommunication consoleCommunication,
    final Translations translations) {

    m_consoleCommunication = consoleCommunication;
    m_translations = translations;
    m_processStatusSet =
      new ProcessStatusImplementation(timer, m_agentNumberMap);

    final MessageDispatchRegistry messageDispatchRegistry =
      consoleCommunication.getMessageDispatchRegistry();

    messageDispatchRegistry.set(
      AgentProcessReportMessage.class,
      new AbstractHandler<AgentProcessReportMessage>() {
        @Override
        public void handle(final AgentProcessReportMessage message) {
          m_processStatusSet.addAgentStatusReport(message);
        }
      }
    );

    messageDispatchRegistry.set(
      WorkerProcessReportMessage.class,
      new AbstractHandler<WorkerProcessReportMessage>() {
        @Override
        public void handle(final WorkerProcessReportMessage message) {
          m_processStatusSet.addWorkerStatusReport(message);
        }
      }
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startWorkerProcesses(final GrinderProperties properties) {

    m_agentNumberMap.forEach(new AllocateLowestNumber.IteratorCallback() {
      @Override
      public void objectAndNumber(final Object object, final int number) {
        m_consoleCommunication.sendToAddressedAgents(
          new AgentAddress((AgentIdentity)object),
          new StartGrinderMessage(properties, number));
        }
      });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startWorkerProcessesWithDistributedFiles(
    final Directory distributionDirectory,
    final GrinderProperties properties) throws ConsoleException {

    // Check that the script file in the supplied properties belongs to
    // the current distribution directory. The file may not exist.
    final File configuredScript =
        properties.getFile(GrinderProperties.SCRIPT,
                           GrinderProperties.DEFAULT_SCRIPT);

    final File resolvedScript =
        properties.resolveRelativeFile(configuredScript);

    final File path = distributionDirectory.relativeFile(resolvedScript, true);

    // Allow the path to not be a child of the directory if it is absolute,
    // since it is fairly obvious to the user what is going on.
    if (path == null && !configuredScript.isAbsolute()) {
      throw new DisplayMessageConsoleException(
        m_translations.translate(
          "console.phrase/script-not-in-directory-error"));
    }

    GrinderProperties propertiesToSend = properties;

    final File associatedFile = properties.getAssociatedFile();

    if (associatedFile != null) {
      // If the properties refer to a file, rebase it to the
      // distribution directory so relative script paths can be
      // resolved based on the properties file location.
      final File relativeFile =
          distributionDirectory.relativeFile(associatedFile, true);

      if (relativeFile != null && !relativeFile.equals(associatedFile)) {
        // Copy, to avoid modifying the parameter.
        propertiesToSend = new GrinderProperties();
        propertiesToSend.putAll(properties);
        propertiesToSend.setAssociatedFile(relativeFile);
      }
    }

    startWorkerProcesses(propertiesToSend);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetWorkerProcesses() {
    m_consoleCommunication.sendToAgents(new ResetGrinderMessage());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stopAgentAndWorkerProcesses() {
    m_consoleCommunication.sendToAgents(new StopGrinderMessage());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addProcessStatusListener(final Listener listener) {
    m_processStatusSet.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfLiveAgents() {
    return m_processStatusSet.getNumberOfLiveAgents();
  }
}
