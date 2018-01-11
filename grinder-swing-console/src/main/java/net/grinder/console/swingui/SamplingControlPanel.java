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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.translation.Translations;


/**
 * Panel containing sampling controls.
 *
 * @author Philip Aston
 */
class SamplingControlPanel extends JPanel {
  private final JSlider m_intervalSlider  = new JSlider(100, 10000, 100);
  private final IntegerField m_collectSampleField =
    new IntegerField(0, 999999);
  private final IntegerField m_ignoreSampleField = new IntegerField(0, 999999);

  private final String m_sampleIntervalString;
  private final String m_ignoreSampleString;
  private final String m_collectSampleZeroString;
  private final String m_collectSampleString;
  private final String m_msUnit;
  private final String m_msUnits;
  private final String m_sampleUnit;
  private final String m_sampleUnits;

  private ConsoleProperties m_properties = null;

  public SamplingControlPanel(Translations translations) {
    m_sampleIntervalString =
      translations.translate("console.option/sample-interval") + ": ";

    m_ignoreSampleString =
      translations.translate("console.option/ignore-sample-count") + " ";

    m_collectSampleZeroString =
      translations.translate("console.option/collect-count-zero", false);
    m_collectSampleString =
      translations.translate("console.option/collect-sample-count") + " ";

    m_msUnit =
        " " + translations.translate("console.term/millisecond").toLowerCase();
    m_msUnits =
        " " + translations.translate("console.term/milliseconds").toLowerCase();
    m_sampleUnit =
        " " + translations.translate("console.term/sample").toLowerCase();
    m_sampleUnits =
        " " + translations.translate("console.term/samples").toLowerCase();

    m_intervalSlider.setMajorTickSpacing(1000);
    m_intervalSlider.setMinorTickSpacing(100);
    m_intervalSlider.setPaintTicks(true);
    m_intervalSlider.setSnapToTicks(true);

    final JLabel intervalLabel = new JLabel();

    m_intervalSlider.addChangeListener(
      new ChangeListener() {
        public void stateChanged(ChangeEvent event) {
          final int minimum = m_intervalSlider.getMinimum();
          final int spacing = m_intervalSlider.getMinorTickSpacing();

          final int value =
            ((m_intervalSlider.getValue() - minimum) / spacing) * spacing +
            minimum;

          setIntervalLabelText(intervalLabel, value);

          if (m_properties != null) {
            try {
              m_properties.setSampleInterval(value);
            }
            catch (ConsoleException e) {
              throw new AssertionError(e.getMessage());
            }
          }
        }
      }
      );

    final JLabel ignoreSampleLabel = new JLabel();

    m_ignoreSampleField.addChangeListener(
      new ChangeListener() {
        public void stateChanged(ChangeEvent event) {
          final int value = m_ignoreSampleField.getValue();

          setIgnoreSampleLabelText(ignoreSampleLabel, value);

          if (m_properties != null) {
            try {
              m_properties.setIgnoreSampleCount(value);
            }
            catch (ConsoleException e) {
              throw new AssertionError(e.getMessage());
            }
          }
        }
      }
      );

    final JLabel collectSampleLabel = new JLabel();

    m_collectSampleField.addChangeListener(
      new ChangeListener() {
        public void stateChanged(ChangeEvent event) {
          final int value = m_collectSampleField.getValue();

          setCollectSampleLabelText(collectSampleLabel, value);

          if (m_properties != null) {
            try {
              m_properties.setCollectSampleCount(value);
            }
            catch (ConsoleException e) {
              throw new AssertionError(e.getMessage());
            }
          }
        }
      }
      );

    final JPanel textFieldLabelPanel = new JPanel(new GridLayout(0, 1, 0, 1));
    textFieldLabelPanel.add(ignoreSampleLabel);
    textFieldLabelPanel.add(collectSampleLabel);

    final JPanel textFieldControlPanel =
      new JPanel(new GridLayout(0, 1, 0, 1));
    textFieldControlPanel.add(m_ignoreSampleField);
    textFieldControlPanel.add(m_collectSampleField);

    final JPanel textFieldPanel = new JPanel(new BorderLayout());
    textFieldPanel.add(textFieldLabelPanel, BorderLayout.WEST);
    textFieldPanel.add(textFieldControlPanel, BorderLayout.EAST);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    intervalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    m_intervalSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
    textFieldPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    add(intervalLabel);
    add(Box.createRigidArea(new Dimension(0, 5)));
    add(m_intervalSlider);
    add(Box.createRigidArea(new Dimension(0, 10)));
    add(textFieldPanel);
  }

  public void setProperties(ConsoleProperties properties) {
    // Disable updates to associated properties.
    m_properties = null;

    m_intervalSlider.setValue(properties.getSampleInterval());
    m_ignoreSampleField.setValue(properties.getIgnoreSampleCount());
    m_collectSampleField.setValue(properties.getCollectSampleCount());

    // Enable updates to new associated properties.
    m_properties = properties;
  }

  public void refresh() {
    setProperties(m_properties);
  }

  private void setIntervalLabelText(JLabel label, int sampleInterval) {
    label.setText(m_sampleIntervalString + sampleInterval +
                  (sampleInterval == 1 ? m_msUnit : m_msUnits));
  }

  private void setIgnoreSampleLabelText(JLabel label, int ignoreSample) {
    label.setText(m_ignoreSampleString + ignoreSample +
                  (ignoreSample == 1 ? m_sampleUnit : m_sampleUnits));
  }

  private void setCollectSampleLabelText(JLabel label, int collectSample) {
    if (collectSample == 0 && m_collectSampleZeroString != null) {
      label.setText(m_collectSampleZeroString);
    }
    else {
      label.setText(m_collectSampleString + collectSample +
                    (collectSample == 1 ? m_sampleUnit : m_sampleUnits));
    }
  }
}

