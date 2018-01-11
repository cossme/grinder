// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2013 Philip Aston
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.SystemColor;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsQueries;
import net.grinder.translation.Translations;

/**
 * This class is used graphically show statistics.
 *
 * @author Paco Gomez
 * @author Philip Aston
 */
class LabelledGraph extends JPanel {
  private static double s_peak = 0d;

  private static double s_lastPeak = 0d;

  private static Border s_thinBevelBorder;

  private static final Color[] s_colors = {
                                           new Color(0xF0, 0xFF, 0x00),
                                           new Color(0xF0, 0xF0, 0x00),
                                           new Color(0xF0, 0xE0, 0x00),
                                           new Color(0xF0, 0xD0, 0x00),
                                           new Color(0xF0, 0xC0, 0x00),
                                           new Color(0xF0, 0xB0, 0x00),
                                           new Color(0xF0, 0xA0, 0x00),
                                           new Color(0xF0, 0x90, 0x00),
                                           new Color(0xF0, 0x80, 0x00),
                                           new Color(0xF0, 0x70, 0x00),
                                           new Color(0xF0, 0x60, 0x00),
                                           new Color(0xF0, 0x50, 0x00),
                                           new Color(0xF0, 0x40, 0x00),
                                           new Color(0xF0, 0x30, 0x00),
                                           new Color(0xF0, 0x20, 0x00),
                                           new Color(0xF0, 0x10, 0x00),
                                           new Color(0xF0, 0x00, 0x00),
  };

  private final Color m_color;

  private final Graph m_graph;

  private final StatisticExpression m_tpsExpression;

  private final StatisticExpression m_peakTPSExpression;

  private final TestStatisticsQueries m_testStatisticsQueries;

  private static final class Label extends JLabel {

    private static final Font s_plainFont;

    private static final Font s_boldFont;

    static {
      final JLabel label = new JLabel();
      final Font defaultFont = label.getFont();
      final float size = defaultFont.getSize2D() - 1;

      s_plainFont = defaultFont.deriveFont(Font.PLAIN, size);
      s_boldFont = defaultFont.deriveFont(Font.BOLD, size);
    }

    private final String m_suffix;

    private final String m_unit;

    private final String m_units;

    public Label(String unit, String units, String suffix) {
      m_suffix = " " + suffix;
      m_unit = " " + unit;
      m_units = " " + units;
      setFont(s_plainFont);
      set(0);
    }

    public void set(long value) {
      super.setText(Long.toString(value) +
          (value == 1 ? m_unit : m_units) +
          m_suffix);
    }

    public void set(double value, NumberFormat numberFormat) {
      super.setText(numberFormat.format(value) +
          m_units +
          m_suffix);
    }

    public void set(String value) {
      super.setText(value + m_units + m_suffix);
    }

    /**
     * Make all labels the same width. Pack more tightly vertically.
     */
    @Override
    public Dimension getPreferredSize() {
      final Dimension d = super.getPreferredSize();
      d.width = 120;
      d.height -= 3;
      return d;
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }

    public void setHighlight(boolean highlight) {
      if (highlight) {
        setForeground(Colours.RED);
        setFont(s_boldFont);
      }
      else {
        setForeground(SystemColor.controlText);
        setFont(s_plainFont);
      }
    }
  }

  private final Label m_averageTimeLabel;

  private final Label m_averageTPSLabel;

  private final Label m_peakTPSLabel;

  private final Label m_testsLabel;

  private final Label m_errorsLabel;

  private final Dimension m_preferredSize = new Dimension(250, 110);

  public LabelledGraph(String title,
                       Translations translations,
                       StatisticExpression tpsExpression,
                       StatisticExpression peakTPSExpression,
                       TestStatisticsQueries testStatisticsQueries) {
    this(title, translations, null, tpsExpression, peakTPSExpression,
      testStatisticsQueries);
  }

  public LabelledGraph(String title,
                       Translations translations,
                       Color color,
                       StatisticExpression tpsExpression,
                       StatisticExpression peakTPSExpression,
                       TestStatisticsQueries testStatisticsQueries) {
    m_tpsExpression = tpsExpression;
    m_peakTPSExpression = peakTPSExpression;
    m_testStatisticsQueries = testStatisticsQueries;

    final String msUnit = translations.translate("console.term/millisecond");
    final String msUnits = translations.translate("console.term/milliseconds");
    final String tpsUnits = translations.translate("console.term/tps");
    final String testUnit = translations.translate("console.term/test");
    final String testUnits = translations.translate("console.term/tests");
    final String errorUnit = translations.translate("console.term/error");
    final String errorUnits = translations.translate("console.term/errors");

    final String averageSuffix =
        "(" + translations.translate("console.term/mean") + ")";
    final String peakSuffix =
        "(" + translations.translate("console.term/peak") + ")";

    m_averageTimeLabel = new Label(msUnit, msUnits, averageSuffix);
    m_averageTPSLabel = new Label(tpsUnits, tpsUnits, averageSuffix);
    m_peakTPSLabel = new Label(tpsUnits, tpsUnits, peakSuffix);
    m_testsLabel = new Label(testUnit, testUnits, "");
    m_errorsLabel = new Label(errorUnit, errorUnits, "");

    m_color = color;
    m_graph = new Graph(25);
    m_graph.setPreferredSize(null); // We are the master now.
    final JPanel graphPanel = new JPanel();
    graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.Y_AXIS));
    graphPanel.add(m_graph);

    if (s_thinBevelBorder == null) {
      s_thinBevelBorder =
          BorderFactory.createBevelBorder(BevelBorder.LOWERED,
            getBackground(),
            getBackground().brighter(),
            getBackground(),
            getBackground().darker());
    }

    graphPanel.setBorder(s_thinBevelBorder);

    final JPanel labelPanel = new JPanel();
    labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
    labelPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 0, 0));

    final JLabel titleLabel = new JLabel();
    titleLabel.setText(title);
    titleLabel.setForeground(SystemColor.textText);
    titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

    labelPanel.add(m_averageTimeLabel);
    labelPanel.add(m_averageTPSLabel);
    labelPanel.add(m_peakTPSLabel);
    labelPanel.add(m_testsLabel);
    labelPanel.add(m_errorsLabel);

    setLayout(new BorderLayout());

    add(titleLabel, BorderLayout.NORTH);
    add(labelPanel, BorderLayout.WEST);
    add(graphPanel, BorderLayout.CENTER);

    final Border border = getBorder();
    final Border margin = new EmptyBorder(10, 10, 10, 10);
    setBorder(new CompoundBorder(border, margin));
  }

  @Override
  public Dimension getPreferredSize() {
    return m_preferredSize;
  }

  public void add(StatisticsSet intervalStatistics,
    StatisticsSet cumulativeStatistics,
    NumberFormat numberFormat) {

    final double averageTime =
        m_testStatisticsQueries.getAverageTestTime(cumulativeStatistics);
    final long errors =
        m_testStatisticsQueries.getNumberOfErrors(cumulativeStatistics);
    final double peakTPS =
        m_peakTPSExpression.getDoubleValue(cumulativeStatistics);

    m_graph.setMaximum(peakTPS);
    m_graph.add(m_tpsExpression.getDoubleValue(intervalStatistics));
    m_graph.setColor(calculateColour(averageTime));

    if (!Double.isNaN(averageTime)) {
      m_averageTimeLabel.set(averageTime, numberFormat);
    }
    else {
      m_averageTimeLabel.set("----");
    }

    m_averageTPSLabel.set(
      m_tpsExpression.getDoubleValue(cumulativeStatistics),
      numberFormat);

    m_peakTPSLabel.set(peakTPS, numberFormat);

    m_testsLabel.set(
        m_testStatisticsQueries.getNumberOfTests(cumulativeStatistics));

    m_errorsLabel.set(errors);
    m_errorsLabel.setHighlight(errors > 0);
  }

  /**
   * Package scope for unit tests.
   */
  Color calculateColour(double time) {
    if (m_color != null) {
      return m_color;
    }
    else {
      if (time > s_peak) { // Not worth the cost of synchronisation.
        s_peak = time;
      }

      final int colorIndex = (int) (s_colors.length * (time / s_lastPeak));

      if (colorIndex >= s_colors.length) {
        return s_colors[s_colors.length - 1];
      }

      return s_colors[colorIndex];
    }
  }

  public static void resetPeak() {
    s_lastPeak = s_peak;
    s_peak = 0;
  }
}
