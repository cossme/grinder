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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.MockitoAnnotations.initMocks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JFrame;

import net.grinder.statistics.PeakStatisticExpression;
import net.grinder.statistics.StatisticExpression;
import net.grinder.statistics.StatisticExpressionFactory;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.translation.Translations;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;



/**
 * Unit tests for {@link LabelledGraph}.
 *
 * @author Philip Aston
 */
public class TestGraph {

  @Mock private Translations m_translations;

  private final int m_pauseTime = 1;
  private final Random s_random = new Random();
  private JFrame m_frame;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    m_frame = new JFrame("Test Graph");
  }

  @After
  public void tearDown() throws Exception {
    m_frame.dispose();
  }

  private void createUI(JComponent component) throws Exception {
    m_frame.getContentPane().add(component, BorderLayout.CENTER);
    m_frame.pack();
    m_frame.setVisible(true);
  }

  @Test public void testRamp() throws Exception {
    final Graph graph = new Graph(25);
    createUI(graph);

    graph.setMaximum(150);

    for (int i = 0; i < 150; i++) {
      graph.add(i);
      pause();
    }

    graph.setMaximum(0);

    for (int i = 0; i < 10; i++) {
      graph.add(i);
      pause();
    }
  }

  @Test public void testRandom() throws Exception {
    final Graph graph = new Graph(100);
    createUI(graph);

    graph.setMaximum(1);

    for (int i = 0; i < 200; i++) {
      graph.add(s_random.nextDouble());
      pause();
    }
  }

  @Test public void testLabelledGraph() throws Exception {
    final StatisticsServices statisticsServices =
      StatisticsServicesImplementation.getInstance();

    final StatisticsIndexMap indexMap =
      statisticsServices.getStatisticsIndexMap();

    final StatisticsIndexMap.LongIndex periodIndex = indexMap
        .getLongIndex("period");
    final StatisticsIndexMap.LongIndex errorStatisticIndex = indexMap
        .getLongIndex("errors");
    final StatisticsIndexMap.LongIndex untimedTestsIndex = indexMap
        .getLongIndex("untimedTests");
    final StatisticsIndexMap.LongSampleIndex timedTestsIndex = indexMap
        .getLongSampleIndex("timedTests");

    final StatisticExpressionFactory statisticExpressionFactory =
      statisticsServices.getStatisticExpressionFactory();

    final StatisticExpression tpsExpression = statisticExpressionFactory
        .createExpression("(* 1000 (/(+ (count timedTests) untimedTests) period))");

    final PeakStatisticExpression peakTPSExpression = statisticExpressionFactory
        .createPeak(indexMap.getDoubleIndex("peakTPS"), tpsExpression);

    final LabelledGraph labelledGraph =
      new LabelledGraph(
        "Test",
        m_translations,
        tpsExpression,
        peakTPSExpression,
        statisticsServices.getTestStatisticsQueries());

    createUI(labelledGraph);

    final StatisticsSetFactory statisticsSetFactory =
      statisticsServices.getStatisticsSetFactory();

    final StatisticsSet cumulativeStatistics = statisticsSetFactory.create();

    final DecimalFormat format = new DecimalFormat();

    final int period = 1000;

    for (int i = 0; i < 200; i++) {
      final StatisticsSet intervalStatistics = statisticsSetFactory.create();

      intervalStatistics.setValue(periodIndex, period);

      while (s_random.nextInt() > 0) {
        intervalStatistics.addValue(untimedTestsIndex, 1);
      }

      long time;

      while ((time = s_random.nextInt()) > 0) {
        intervalStatistics.addSample(timedTestsIndex, time % 10000);
      }

      while (s_random.nextFloat() > 0.95) {
        intervalStatistics.addValue(errorStatisticIndex, 1);
      }

      cumulativeStatistics.add(intervalStatistics);
      cumulativeStatistics.setValue(periodIndex, (1 + i) * period);

      peakTPSExpression.update(intervalStatistics, cumulativeStatistics);
      labelledGraph.add(intervalStatistics, cumulativeStatistics, format);
      pause();
    }

    LabelledGraph.resetPeak();
    labelledGraph.calculateColour(100);
    LabelledGraph.resetPeak();

    final Color colour1 = labelledGraph.calculateColour(100);
    assertFalse(colour1.equals(labelledGraph.calculateColour(50)));
    assertEquals(colour1, labelledGraph.calculateColour(100));
    assertEquals(colour1, labelledGraph.calculateColour(150));
    assertEquals(colour1, labelledGraph.calculateColour(100));

    LabelledGraph.resetPeak();
    assertFalse(colour1.equals(labelledGraph.calculateColour(100)));
    assertEquals(colour1, labelledGraph.calculateColour(150));

    final LabelledGraph labelledGraph2 =
      new LabelledGraph(
        "Test",
        m_translations,
        Colours.DARK_GREEN,
        tpsExpression,
        peakTPSExpression,
        statisticsServices.getTestStatisticsQueries());
    assertEquals(Colours.DARK_GREEN, labelledGraph2.calculateColour(100));
    assertEquals(Colours.DARK_GREEN, labelledGraph2.calculateColour(0));
  }

  private void pause() throws Exception {
    if (m_pauseTime > 0) {
      Thread.sleep(m_pauseTime);
    }
  }
}

