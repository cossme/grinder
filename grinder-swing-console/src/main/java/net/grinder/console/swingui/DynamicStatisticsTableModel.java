// Copyright (C) 2001 - 2013 Philip Aston
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
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import net.grinder.common.Test;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.console.model.SampleModelViews.Listener;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsView;
import net.grinder.translation.Translations;


/**
 * Abstract table model for statistics tables.
 *
 * @author Philip Aston
 */
abstract class DynamicStatisticsTableModel
  extends AbstractTableModel
  implements SampleModel.Listener, Table.TableModel, Listener {

  private final SampleModel m_model;
  private final SampleModelViews m_modelViews;
  private final Translations m_translations;

  private final String m_testString;
  private final String m_testColumnString;
  private final String m_testDescriptionColumnString;

  // Guarded by this.
  private ModelTestIndex m_lastModelTestIndex;

  // Guarded by this.
  private StatisticsView m_statisticsView;

  // Guarded by this.
  private ExpressionView[] m_columnViews;

  // Guarded by this.
  private String[] m_columnLabels;

  protected DynamicStatisticsTableModel(
    SampleModel model,
    SampleModelViews modelViews,
    Translations translations,
    SwingDispatcherFactory swingDispatcherFactory) {

    m_model = model;
    m_modelViews = modelViews;
    m_translations = translations;

    m_testString = m_translations.translate("console.term/test") + ' ';
    m_testColumnString = m_translations.translate("console.term/test");
    m_testDescriptionColumnString =
      m_translations.translate("console.term/test-description");

    m_model.addModelListener(
      swingDispatcherFactory.create(SampleModel.Listener.class, this));
    m_modelViews.addListener(
      swingDispatcherFactory.create(Listener.class, this));

    resetTests();
  }

  protected abstract StatisticsSet getStatistics(int row);

  protected final SampleModel getModel() {
    return m_model;
  }

  protected final SampleModelViews getModelViews() {
    return m_modelViews;
  }

  protected final synchronized ModelTestIndex getLastModelTestIndex() {
    return m_lastModelTestIndex;
  }

  public synchronized void addColumns(StatisticsView statisticsView) {
    m_statisticsView.add(statisticsView);

    final int originalNumberOfColumns = m_columnViews.length;

    final ExpressionView[] newViews = m_statisticsView.getExpressionViews();

    if (newViews.length != originalNumberOfColumns) {
      m_columnViews = newViews;

      m_columnLabels = new String[m_columnViews.length];

      for (int i = 0; i < m_columnLabels.length; ++i) {
        final String resource =
          m_translations.translate(m_columnViews[i].getTranslationKey());

        m_columnLabels[i] =
          resource != null ?
          resource : m_columnViews[i].getDisplayName();
      }

      fireTableStructureChanged();
    }
  }

  /**
   * Called when a new statistic expression has been added to the model.
   * {@link net.grinder.console.model.SampleModel.Listener} interface.
   *
   * @param statisticExpression
   *          The new statistic expression.
   */
  public void newStatisticExpression(ExpressionView statisticExpression) {

    final StatisticsView statisticsView = new StatisticsView();
    statisticsView.add(statisticExpression);

    addColumns(statisticsView);
  }

  public final void stateChanged() {
    fireTableRowsUpdated(0, getRowCount());
  }

  public final void newSample() {
    // Do nothing - stateChanged will have been called.
  }

  public final synchronized void newTests(Set<Test> newTests,
                                          ModelTestIndex modelTestIndex) {

    m_lastModelTestIndex = modelTestIndex;

    // We've been reset, number of rows may have changed.
    fireTableDataChanged();
  }

  public final synchronized void resetTests() {
    m_lastModelTestIndex = new ModelTestIndex();
  }

  public synchronized void resetStatisticsViews() {
    m_statisticsView = new StatisticsView();
    m_columnViews = new ExpressionView[0];
    m_columnLabels = new String[0];
  }

  public final synchronized int getColumnCount() {
    return 2 + m_columnLabels.length;
  }

  @Override
  public final synchronized String getColumnName(int column) {
    switch (column) {
    case 0:
      return m_testColumnString;

    case 1:
      return m_testDescriptionColumnString;

    default:
      return m_columnLabels[column - 2];
    }
  }

  public synchronized int getRowCount() {
    return m_lastModelTestIndex.getNumberOfTests();
  }

  public synchronized Object getValueAt(int row, int column) {
    if (column == 0) {
      return m_testString + m_lastModelTestIndex.getTest(row).getNumber();
    }
    else if (column == 1) {
      return m_lastModelTestIndex.getTest(row).getDescription();
    }
    else {
      return getDynamicField(getStatistics(row), column - 2);
    }
  }

  protected synchronized String getDynamicField(StatisticsSet statistics,
                                                int dynamicColumn) {

    if (dynamicColumn < m_columnViews.length) {
      final StatisticExpression expression =
        m_columnViews[dynamicColumn].getExpression();

      if (expression.isDouble()) {
        final double value = expression.getDoubleValue(statistics);

        if (Double.isNaN(value)) {
          return "";
        }
        else {
          return getModelViews().getNumberFormat().format(value);
        }
      }
      else {
        return String.valueOf(expression.getLongValue(statistics));
      }
    }
    else {
      return "?";
    }
  }

  public boolean isBold(int row, int column) {
    return
    column == 3 &&
    getModelViews().getTestStatisticsQueries().getNumberOfErrors(
        getStatistics(row)) > 0;
  }

  public Color getForeground(int row, int column) {
    return isBold(row, column) ? Colours.RED : null;
  }

  public Color getBackground(int row, int column) {
    return getStatistics(row).isComposite() ? Colours.LIGHT_GREY : null;
  }

  public synchronized void write(Writer writer, String columnDelimiter,
                                 String lineDelimeter)
    throws IOException {
    final int numberOfRows = getRowCount();
    final int numberOfColumns = getColumnCount();

    writer.write(m_testColumnString);
    writer.write(columnDelimiter);
    writer.write(m_testDescriptionColumnString);
    writer.write(columnDelimiter);

    for (int dynamicColumn = 0; dynamicColumn < numberOfColumns - 2;
         dynamicColumn++) {
      writer.write(m_columnLabels[dynamicColumn]);
      writer.write(columnDelimiter);
    }

    writer.write(lineDelimeter);

    for (int row = 0; row < numberOfRows; row++) {
      for (int column = 0; column < numberOfColumns; column++) {
        final Object o = getValueAt(row, column);
        writer.write(o != null ? o.toString() : "");
        writer.write(columnDelimiter);
      }

      writer.write(lineDelimeter);
    }
  }
}
