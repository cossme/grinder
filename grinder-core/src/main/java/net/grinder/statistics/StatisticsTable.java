// Copyright (C) 2000 - 2011 Philip Aston
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

package net.grinder.statistics;

import java.io.PrintWriter;
import java.text.DecimalFormat;

import net.grinder.common.Test;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.util.FixedWidthFormatter;


/**
 * <p>Format a textual table of a {@link TestStatisticsMap} using a
 * {@link StatisticsView}.</p>
 *
 * <p>Package scope</p>.
 *
 * @author Philip Aston
 */
public class StatisticsTable {

  private static final int COLUMN_WIDTH = 12;
  private static final String COLUMN_SEPARATOR = " ";

  private final TestStatisticsMap m_testStatisticsMap;

  private final DecimalFormat m_twoDPFormat = new DecimalFormat("0.00");

  private final String m_lineSeparator = System.getProperty("line.separator");

  private final FixedWidthFormatter m_headingFormatter =
    new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                            FixedWidthFormatter.Flow.WORD_WRAP,
                            COLUMN_WIDTH);

  private final FixedWidthFormatter m_rowLabelFormatter =
    new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                            FixedWidthFormatter.Flow.OVERFLOW,
                            COLUMN_WIDTH);

  private final FixedWidthFormatter m_rowCellFormatter =
    new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                            FixedWidthFormatter.Flow.TRUNCATE,
                            COLUMN_WIDTH);

  private final FixedWidthFormatter m_freeTextFormatter =
    new FixedWidthFormatter(FixedWidthFormatter.Align.LEFT,
                            FixedWidthFormatter.Flow.WORD_WRAP,
                            72);

  private final StatisticsView m_statisticsView;
  private final LongIndex m_periodIndex;

  /**
   * Creates a new <code>StatisticsTable</code> instance.
   *
   * @param statisticsView Views.
   * @param statisticsIndexMap The statistics index map.
   * @param testStatisticsMap Tests and associated statistics.
   */
  public StatisticsTable(final StatisticsView statisticsView,
                         final StatisticsIndexMap statisticsIndexMap,
                         final TestStatisticsMap testStatisticsMap) {
    m_statisticsView = statisticsView;
    m_testStatisticsMap = testStatisticsMap;

    m_periodIndex = statisticsIndexMap.getLongIndex("period");
  }

  /**
   * Write the table out an output writer.
   *
   * @param out The output writer
   * @param elapsedTime  The elapsed time.
   */
  public final void print(final PrintWriter out, final long elapsedTime) {
  final ExpressionView[] expressionViews =
      m_statisticsView.getExpressionViews();

    final int numberOfHeaderColumns = expressionViews.length + 1;

    StringBuilder[] cells = new StringBuilder[numberOfHeaderColumns];
    StringBuilder[] remainders = new StringBuilder[numberOfHeaderColumns];

    for (int i = 0; i < numberOfHeaderColumns; i++) {
      cells[i] = new StringBuilder(
        i == 0 ? "" : expressionViews[i - 1].getDisplayName());

      remainders[i] = new StringBuilder();
    }

    boolean wrapped;

    do {
      wrapped = false;

      for (int i = 0; i < numberOfHeaderColumns; ++i) {
        remainders[i].setLength(0);
        m_headingFormatter.transform(cells[i], remainders[i]);

        out.print(cells[i].toString());
        out.print(COLUMN_SEPARATOR);

        if (remainders[i].length() > 0) {
          wrapped = true;
        }
      }

      out.println();

      final StringBuilder[] otherArray = cells;
      cells = remainders;
      remainders = otherArray;
    }
    while (wrapped);

    out.println();

    final LineFormatter formatter = new LineFormatter(expressionViews);

    final LineFormatter compositeTotalsFormatter =
      new CompositeStatisticsLineFormater(expressionViews);

    synchronized (m_testStatisticsMap) {

      m_testStatisticsMap.new ForEach() {
        @Override
        public void next(final Test test, final StatisticsSet statistics) {
          statistics.setValue(m_periodIndex, elapsedTime);
          out.print(formatter.format("Test " + test.getNumber(), statistics));

          final String testDescription = test.getDescription();

          if (testDescription != null) {
            out.println(" \"" + testDescription + "\"");
          }
          else {
            out.println();
          }
        }
      }
      .iterate();

      out.println();

      final StatisticsSet nonCompositeStatisticsTotals =
        m_testStatisticsMap.nonCompositeStatisticsTotals();
      nonCompositeStatisticsTotals.setValue(m_periodIndex, elapsedTime);

      out.println(
        formatter.format(
          "Totals", nonCompositeStatisticsTotals));

      final StatisticsSet compositeStatisticsTotals =
        m_testStatisticsMap.compositeStatisticsTotals();

      if (!compositeStatisticsTotals.isZero()) {
        compositeStatisticsTotals.setValue(m_periodIndex, elapsedTime);

        out.println(
          compositeTotalsFormatter.format("", compositeStatisticsTotals));
      }
    }

    out.println();

    final StringBuilder text = new StringBuilder();
    StringBuilder buffer = new StringBuilder(
      "Tests resulting in error only contribute to the Errors column. " +
      "Statistics for individual tests can be found in the data file, " +
      "including (possibly incomplete) statistics for erroneous tests. " +
      "Composite tests are marked with () and not included in the totals.");

    while (buffer.length() > 0) {
      final StringBuilder remainder = new StringBuilder();
      m_freeTextFormatter.transform(buffer, remainder);

      if (text.length() > 0) {
    text.append(m_lineSeparator);
      }

      text.append("  ");
      text.append(buffer.toString());

      buffer = remainder;
    }

    out.println(text);
  }

  private class LineFormatter {
    private final ExpressionView[] m_expressionViews;

    public LineFormatter(final ExpressionView[] expressionViews) {
      m_expressionViews = expressionViews;
    }

    public String format(final String rowLabel,
                         final StatisticsSet statistics) {

      final StringBuilder result = new StringBuilder();

      final StringBuilder cell = new StringBuilder();

      cell.append(startOfLine(statistics));

      cell.append(rowLabel);

      final StringBuilder remainder = new StringBuilder();

      m_rowLabelFormatter.transform(cell, remainder);
      result.append(cell.toString());
      result.append(COLUMN_SEPARATOR);

      for (int i = 0; i < m_expressionViews.length; ++i) {
        final StringBuilder statisticsCell =
          new StringBuilder(formatExpression(m_expressionViews[i], statistics));

        if (i == m_expressionViews.length - 1) {
          statisticsCell.append(endOfLine(statistics));
        }

        m_rowCellFormatter.transform(statisticsCell, remainder);
        result.append(statisticsCell.toString());

        result.append(COLUMN_SEPARATOR);
      }

      return result.toString();
    }

    protected String startOfLine(final StatisticsSet statistics) {
      if (statistics.isComposite()) {
        return "(";
      }

      return "";
    }

    protected String endOfLine(final StatisticsSet statistics) {
      if (statistics.isComposite()) {
        return ")";
      }

      return "";
    }

    protected String formatExpression(final ExpressionView expressionView,
                                      final StatisticsSet statistics) {

      final StatisticExpression expression = expressionView.getExpression();

      if (expression.isDouble()) {
        final double value = expression.getDoubleValue(statistics);

        if (Double.isNaN(value)) {
          return "-";
        }

        return m_twoDPFormat.format(value);
      }
      else {
        return String.valueOf(expression.getLongValue(statistics));
      }
    }
  }

  private class CompositeStatisticsLineFormater extends LineFormatter {

    public CompositeStatisticsLineFormater(
      final ExpressionView[] expressionViews) {
      super(expressionViews);
    }

    @Override
    protected String formatExpression(final ExpressionView expressionView,
                                      final StatisticsSet statistics) {

      if (expressionView.getShowForCompositeStatistics()) {
        final StringBuilder result = new StringBuilder("(");
        result.append(super.formatExpression(expressionView, statistics));
        result.append(")");

        return result.toString();
      }
      else {
        return "";
      }
    }

    @Override
    protected String startOfLine(final StatisticsSet statistics) {
      return "";
    }

    @Override
    protected String endOfLine(final StatisticsSet statistics) {
      return "";
    }
  }
}
