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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.grinder.common.Test;
import net.grinder.console.common.Resources;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.console.model.SampleListener;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.statistics.StatisticsSet;
import net.grinder.translation.Translations;


/**
 * A panel of test graphs.
 *
 * @author Philip Aston
 */
public class TestGraphPanel extends JPanel implements SampleModel.Listener {

  private final JComponent m_parentComponent;
  private final BorderLayout m_borderLayout = new BorderLayout();
  private final FlowLayout m_flowLayout = new FlowLayout(FlowLayout.LEFT);
  private final JLabel m_logoLabel;

  private final Dimension m_preferredSize = new Dimension();

  private final SampleModel m_model;
  private final SampleModelViews m_sampleModelViews;
  private final Translations m_translations;
  private final SwingDispatcherFactory m_swingDispatcherFactory;
  private final String m_testLabel;

  private final Map<Test, JComponent> m_components =
    new HashMap<Test, JComponent>();

  TestGraphPanel(JComponent parentComponent,
                 SampleModel model,
                 SampleModelViews sampleModelViews,
                 Resources resources,
                 Translations translations,
                 SwingDispatcherFactory swingDispatcherFactory) {

    m_parentComponent = parentComponent;
    m_model = model;
    m_sampleModelViews = sampleModelViews;
    m_translations = translations;
    m_swingDispatcherFactory = swingDispatcherFactory;

    m_testLabel = translations.translate("console.term/test") + " ";

    m_model.addModelListener(
      swingDispatcherFactory.create(SampleModel.Listener.class, this));

    m_model.addTotalSampleListener(
      new SampleListener() {
        public void update(StatisticsSet intervalStatistics,
                           StatisticsSet cumulativeStatistics) {
          // No requirement to dispatch in Swing thread.
          LabelledGraph.resetPeak();
        }
      });

    m_logoLabel = new JLabel(resources.getImageIcon("logo-large.image"));
  }

  /**
   * {@link net.grinder.console.model.SampleModel.Listener} interface.
   * Called when the model state has changed.
   */
  public void stateChanged() {
  }

  /**
   * {@link net.grinder.console.model.SampleModel.Listener} interface.
   * Called when the model has a new sample.
   */
  public final void newSample() {
  }

  /**
   * {@link net.grinder.console.model.SampleModel.Listener} interface.
   * Existing {@code Test}s and {@code StatisticsView}s have
   * been discarded.
   */
  public final void resetTests() {
    m_components.clear();
    removeAll();
    setLayout(m_borderLayout);
    add(m_logoLabel, BorderLayout.CENTER);
    invalidate();
  }

  /**
   * {@link net.grinder.console.model.SampleModel.Listener} interface.
   * Called when new tests have been registered.
   *
   * @param newTests
   *          The new tests.
   * @param modelTestIndex
   *          Updated test index.
   */
  public void newTests(Set<Test> newTests, ModelTestIndex modelTestIndex) {

    remove(m_logoLabel);
    setLayout(m_flowLayout);

    for (Test test : newTests) {
      final String description = test.getDescription();

      final String label =
        m_testLabel + test.getNumber() +
        (description != null ? " (" + description + ")" : "");

      final LabelledGraph testGraph =
        new LabelledGraph(label,
                          m_translations,
                          m_model.getTPSExpression(),
                          m_model.getPeakTPSExpression(),
                          m_sampleModelViews.getTestStatisticsQueries());

      m_model.addSampleListener(
        test,
        m_swingDispatcherFactory.create(
          SampleListener.class,
          new SampleListener() {
            public void update(final StatisticsSet intervalStatistics,
                               final StatisticsSet cumulativeStatistics) {
              testGraph.add(intervalStatistics, cumulativeStatistics,
                            m_sampleModelViews.getNumberFormat());
            }
          }));

      m_components.put(test, testGraph);
    }

    final int numberOfTests = modelTestIndex.getNumberOfTests();

    // We add all the tests components again. The container ignores
    // duplicates, but inserts the new components in the correct
    // order.
    for (int i = 0; i < numberOfTests; i++) {
      add(m_components.get(modelTestIndex.getTest(i)));
    }

    // Invalidate preferred size cache.
    m_preferredSize.width = -1;

    validate();
  }

  /**
   * Specify our preferred size to prevent our FlowLayout from laying
   * us out horizontally. We fix our width to that of our containing
   * tab, and calculate our vertical height. The intermediate scroll
   * pane uses the preferred size.
   *
   * @return The preferred size.
   */
  @Override
  public final Dimension getPreferredSize() {

    if (m_components.size() == 0) {
      return super.getPreferredSize();
    }

    // Width is whatever our parent says.
    final Insets parentComponentInsets = m_parentComponent.getInsets();

    final int preferredWidth =
      m_parentComponent.getWidth() -
      parentComponentInsets.left - parentComponentInsets.right;

    if (m_preferredSize.width == preferredWidth) {
      // Nothing's changed.
      return m_preferredSize;
    }

    m_preferredSize.width = preferredWidth;

    // Now ape the FlowLayout algorithm to calculate desired height,
    // *sigh*.
    final int n = getComponentCount();
    final int hgap = m_flowLayout.getHgap();

    final Insets insets = getInsets();

    final int fudgeFactor = 6;    // I've no idea where this extra space
    // comes from, but we need it.

    int availableWidth =
      preferredWidth - insets.left - insets.right - hgap + fudgeFactor;

    if (n > 0) {
      // Assume we have a homogeneous set of fixed size components.
      final int componentWidth = getComponent(0).getWidth();
      final int componentHeight = getComponent(0).getHeight();

      int numberAcross = -1;

      while (componentWidth > 0 && availableWidth > 0) {
        ++numberAcross;
        availableWidth -= componentWidth;
        availableWidth -= hgap;
      }

      if (numberAcross > 0) {
        final int numberDown = (n + numberAcross - 1) / numberAcross;

        // numberDown is always >= 1.
        m_preferredSize.height =
          numberDown * componentHeight +
          (numberDown - 1) * m_flowLayout.getVgap();
      }
    }

    return m_preferredSize;
  }
}
