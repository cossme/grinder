// Copyright (C) 2008 - 2011 Philip Aston
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

package net.grinder.console.textui;

import java.util.Arrays;
import java.util.Comparator;

import org.slf4j.Logger;

import net.grinder.common.GrinderBuild;
import net.grinder.common.processidentity.AgentProcessReport;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.ConsoleFoundation.UI;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.ProcessReportDescriptionFactory;
import net.grinder.console.common.Resources;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.model.SampleModel;


/**
 * Simple "read only" text interface that reports on console events.
 *
 * <p>
 * This is an alternative to the graphical UI. Features that are complementary
 * to any UI (e.g. statistic logging) should be implemented elsewhere.
 * </p>
 *
 * @author Philip Aston
 */
public class TextUI implements UI {
  private final Logger m_logger;
  private final ErrorHandler m_errorHandler;
  private final Thread m_shutdownHook;
  private final SampleModel m_sampleModel;

  /**
   * Constructor.
   *
   * @param resources Console resources.
   * @param processControl Console process control.
   * @param sampleModel Console sample model.
   * @param logger Logger.
   */
  public TextUI(Resources resources,
                ProcessControl processControl,
                SampleModel sampleModel,
                Logger logger) {

    m_logger = logger;
    m_logger.info(GrinderBuild.getName());

    m_shutdownHook = new Thread(new ShutdownHook(resources));
    Runtime.getRuntime().addShutdownHook(m_shutdownHook);

    m_errorHandler = new ErrorHandlerImplementation();

    processControl.addProcessStatusListener(new ProcessListener(resources));

    m_sampleModel = sampleModel;
    m_sampleModel.addModelListener(
      new SampleModel.AbstractListener() {
        public void stateChanged() {
            // Ome, (11/09/16) avoid having the INFO message appearing in console mode every second.
        	//m_logger.info(m_sampleModel.getState().getDescription());
        }
      });
  }

  /**
   * For unit tests.
   *
   * @return The shutdown hook.
   */
  Thread getShutdownHook() {
    return m_shutdownHook;
  }

  /**
   * Return our error handler.
   *
   * @return The error handler.
   */
  public ErrorHandler getErrorHandler() {
    return m_errorHandler;
  }

  private final class ProcessListener implements ProcessControl.Listener {
    private final Comparator<ProcessReport> m_processReportComparator =
      new ProcessReport.StateThenNameThenNumberComparator();

    private final Comparator<ProcessReports> m_processReportsComparator =
      new ProcessControl.ProcessReportsComparator();

    private final ProcessReportDescriptionFactory m_descriptionFactory;

    private final String m_noConnectedAgents;
    private String m_lastReport = null;

    public ProcessListener(Resources resources) {
      m_descriptionFactory = new ProcessReportDescriptionFactory(resources);
      m_noConnectedAgents = resources.getString("noConnectedAgents.text");
    }

    public void update(ProcessControl.ProcessReports[] processReports) {

      final String reportString;

      if (processReports.length == 0) {
        reportString = m_noConnectedAgents;
      }
      else {
        final StringBuilder report =
          new StringBuilder(processReports.length * 128);

        Arrays.sort(processReports, m_processReportsComparator);

        for (int i = 0; i < processReports.length; ++i) {
          if (i > 0) {
            report.append(", ");
          }

          final AgentProcessReport agentProcessStatus =
            processReports[i].getAgentProcessReport();
          report.append(
            m_descriptionFactory.create(agentProcessStatus).toString());

          final WorkerProcessReport[] workerProcessStatuses =
            processReports[i].getWorkerProcessReports();

          if (workerProcessStatuses.length > 0) {
            report.append(" { ");

            Arrays.sort(workerProcessStatuses, m_processReportComparator);

            for (int j = 0; j < workerProcessStatuses.length; ++j) {
              if (j > 0) {
                report.append(", ");
              }

              report.append(
                m_descriptionFactory.create(
                  workerProcessStatuses[j]).toString());
            }

            report.append(" }");
          }
        }

        reportString = report.toString();
      }

      if (!reportString.equals(m_lastReport)) {
        m_logger.info(reportString);
        m_lastReport = reportString;
      }
    }
  }

  private final class ErrorHandlerImplementation implements ErrorHandler {
    public void handleErrorMessage(String errorMessage) {
      m_logger.error(errorMessage);
    }

    public void handleErrorMessage(String errorMessage, String title) {
      m_logger.error("[" + title + "] " + errorMessage);
    }

    public void handleException(Throwable throwable) {
      m_logger.error(throwable.getMessage(), throwable);
    }

    public void handleException(Throwable throwable, String title) {
      m_logger.error(title, throwable);
    }

    public void handleInformationMessage(String informationMessage) {
      m_logger.info(informationMessage);
    }
  }

  private final class ShutdownHook implements Runnable {
    private final String m_shutdownMessage;
    private boolean m_stopped = false;

    public ShutdownHook(Resources resources) {
      m_shutdownMessage = resources.getString("finished.text");
    }

    public synchronized void run() {
      if (!m_stopped) {
        m_stopped = true;
        m_logger.info(m_shutdownMessage);
      }
    }
  }
}
