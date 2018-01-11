// Copyright (C) 2001 - 2010 Philip Aston
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

import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.statistics.StatisticsSet;
import net.grinder.translation.Translations;


/**
 * Table model for cumulative statistics table.
 *
 * @author Philip Aston
 */
final class CumulativeStatisticsTableModel extends DynamicStatisticsTableModel {

  private boolean m_includeTotals = true;
  private final String m_totalString;

  public CumulativeStatisticsTableModel(
    SampleModel model,
    SampleModelViews sampleModelViews,
    Translations translations,
    SwingDispatcherFactory swingDispatcherFactory) {

    super(model, sampleModelViews, translations, swingDispatcherFactory);

    m_totalString = translations.translate("console.term/total");

    resetStatisticsViews();
  }

  @Override
  public synchronized void resetStatisticsViews() {
    super.resetStatisticsViews();
    addColumns(getModelViews().getCumulativeStatisticsView());
  }

  @Override
  protected StatisticsSet getStatistics(int row) {
    return getLastModelTestIndex().getCumulativeStatistics(row);
  }

  @Override
  public synchronized int getRowCount() {
    return super.getRowCount() + (m_includeTotals ? 1 : 0);
  }

  @Override
  public synchronized Object getValueAt(int row, int column) {

    if (row < getLastModelTestIndex().getNumberOfTests()) {
      return super.getValueAt(row, column);
    }
    else {
      switch (column) {
      case 0:
        return m_totalString;

      case 1:
        return "";

      default:
        return getDynamicField(
          getModel().getTotalCumulativeStatistics(), column - 2);
      }
    }
  }

  @Override
  public boolean isBold(int row, int column) {

    if (row < getLastModelTestIndex().getNumberOfTests()) {
      return super.isBold(row, column);
    }
    else {
      return true;
    }
  }

  @Override
  public Color getForeground(int row, int column) {

    if (row < getLastModelTestIndex().getNumberOfTests()) {
      return super.getForeground(row, column);
    }
    else {
      if (column == 3 &&
          getModelViews().getTestStatisticsQueries().getNumberOfErrors(
              getModel().getTotalCumulativeStatistics()) > 0) {
        return Colours.RED;
      }
      else {
        return null;
      }
    }
  }

  @Override
  public Color getBackground(int row, int column) {

    if (row < getLastModelTestIndex().getNumberOfTests()) {
      return super.getBackground(row, column);
    }
    else {
      return null;
    }
  }

  public synchronized void writeWithoutTotals(Writer writer,
                                              String columnDelimiter,
                                              String lineDelimeter)
    throws IOException {

    try {
      m_includeTotals = false;
      super.write(writer, columnDelimiter, lineDelimeter);
    }
    finally {
      m_includeTotals = true;
    }
  }
}
