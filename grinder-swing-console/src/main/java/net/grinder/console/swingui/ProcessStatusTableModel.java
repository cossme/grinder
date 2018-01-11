// Copyright (C) 2001 - 2013 Philip Aston
// Copyright (C) 2001, 2002 Dirk Feufel
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

package net.grinder.console.swingui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.grinder.common.processidentity.AgentProcessReport;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ProcessReportDescriptionFactory;
import net.grinder.console.common.ProcessReportDescriptionFactory.ProcessDescription;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.translation.Translations;


/**
 * TableModel for the process status table. No need to synchronise,
 * all calls after initialisation are dispatched to us in the
 * SwingThread.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 */
class ProcessStatusTableModel
  extends AbstractTableModel implements Table.TableModel {

  private static final int NAME_COLUMN_INDEX = 0;
  private static final int TYPE_COLUMN_INDEX = 1;
  private static final int STATE_COLUMN_INDEX = 2;

  private final Comparator<ProcessReport> m_processReportComparator =
    new ProcessReport.StateThenNameThenNumberComparator();

  private final Comparator<ProcessReports> m_processReportsComparator =
    new ProcessControl.ProcessReportsComparator();

  private final ProcessReportDescriptionFactory m_descriptionFactory;

  private final String[] m_columnHeadings;
  private final String m_workerProcessesString;
  private final String m_threadsString;

  private Row[] m_data = new Row[0];

  public ProcessStatusTableModel(Translations translations,
                                 ProcessControl processControl,
                                 SwingDispatcherFactory swingDispatcherFactory)
    throws ConsoleException {

    m_descriptionFactory =
      new ProcessReportDescriptionFactory(translations);

    m_columnHeadings = new String[3];
    m_columnHeadings[NAME_COLUMN_INDEX] =
      translations.translate("console.process/name");
    m_columnHeadings[TYPE_COLUMN_INDEX] =
      translations.translate("console.process/type");
    m_columnHeadings[STATE_COLUMN_INDEX] =
      translations.translate("console.process/state");

    m_workerProcessesString = translations.translate("console.process/label");
    m_threadsString = translations.translate("console.term/threads");

    processControl.addProcessStatusListener(
      swingDispatcherFactory.create(
        ProcessControl.Listener.class,
        new ProcessControl.Listener() {
          public void update(ProcessControl.ProcessReports[] processReports) {
            final List<Row> rows = new ArrayList<Row>();
            int runningThreads = 0;
            int totalThreads = 0;
            int workerProcesses = 0;

            Arrays.sort(processReports, m_processReportsComparator);

            for (int i = 0; i < processReports.length; ++i) {
              final AgentProcessReport agentProcessStatus =
                processReports[i].getAgentProcessReport();
              rows.add(new ProcessDescriptionRow(
                m_descriptionFactory.create(agentProcessStatus)));

              final WorkerProcessReport[] workerProcessStatuses =
                processReports[i].getWorkerProcessReports();

              Arrays.sort(workerProcessStatuses, m_processReportComparator);

              for (int j = 0; j < workerProcessStatuses.length; ++j) {
                runningThreads +=
                  workerProcessStatuses[j].getNumberOfRunningThreads();
                totalThreads +=
                  workerProcessStatuses[j].getMaximumNumberOfThreads();
                rows.add(
                  new IndentedNameRow(
                    "  ",
                    new ProcessDescriptionRow(
                      m_descriptionFactory.create(workerProcessStatuses[j]))));
              }

              workerProcesses += workerProcessStatuses.length;
            }

            rows.add(
              new TotalRow(runningThreads, totalThreads, workerProcesses));

            m_data = rows.toArray(new Row[rows.size()]);

            fireTableDataChanged();
          }
        }
      )
    );
  }

  public int getColumnCount() {
    return m_columnHeadings.length;
  }

  @Override
  public String getColumnName(int column) {
    return m_columnHeadings[column];
  }

  public int getRowCount() {
    return m_data.length;
  }

  public Object getValueAt(int row, int column) {

    if (row < m_data.length) {
      return m_data[row].getValueForColumn(column);
    }
    else {
      return "";
    }
  }

  public boolean isBold(int row, int column) {
    return row == m_data.length - 1;
  }

  public Color getForeground(int row, int column) {
    return null;
  }

  public Color getBackground(int row, int column) {
    return null;
  }

  private abstract class Row {
    abstract String getName();
    abstract String getProcessType();
    abstract String getState();

    public String getValueForColumn(int column) {
      switch (column) {
      case NAME_COLUMN_INDEX:
        return getName();

      case TYPE_COLUMN_INDEX:
        return getProcessType();

      case STATE_COLUMN_INDEX:
        return getState();

      default:
        return "?";
      }
    }
  }

  private class ProcessDescriptionRow extends Row {
    private final ProcessDescription m_description;

    public ProcessDescriptionRow(ProcessDescription description) {
      m_description = description;
    }

    @Override
    public String getName() {
      return m_description.getName();
    }

    @Override
    public String getProcessType() {
      return m_description.getProcessType();
    }

    @Override
    public String getState() {
      return m_description.getState();
    }
  }

  private class IndentedNameRow extends Row {
    private final String m_indent;
    private final Row m_delegate;

    public IndentedNameRow(String indent, Row row) {
      m_indent = indent;
      m_delegate = row;
    }

    @Override
    public String getName() {
      return m_indent + m_delegate.getName();
    }

    @Override
    public String getProcessType() {
      return m_delegate.getProcessType();
    }

    @Override
    public String getState() {
      return m_delegate.getState();
    }
  }

  private final class TotalRow extends Row {
    private final int m_runningThreads;
    private final int m_totalThreads;
    private final int m_workerProcesses;

    public TotalRow(int runningThreads, int totalThreads, int workerProcesses) {
      m_runningThreads = runningThreads;
      m_totalThreads = totalThreads;
      m_workerProcesses = workerProcesses;
    }

    @Override
    public String getName() {
      return "";
    }

    @Override
    public String getProcessType() {
      return "" + m_workerProcesses + " " + m_workerProcessesString;
    }

    @Override
    public String getState() {
      return
        "" + m_runningThreads + "/" + m_totalThreads + " " + m_threadsString;
    }
  }
}

