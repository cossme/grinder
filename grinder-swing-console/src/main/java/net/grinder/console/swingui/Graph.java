// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004 Philip Aston
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
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;


/**
 * This class is used graphically show statistics.
 *
 * @author Paco Gomez
 * @author Philip Aston
 */
class Graph extends JComponent {

  private final double[] m_values;
  private double m_maximum = 0d;
  private int m_cursor = 0;
  private Color m_color;

  private final int[] m_polygonX;
  private final int[] m_polygonY;
  private boolean m_recalculate = true;

  Graph(int numberOfValues) {

    m_values = new double[numberOfValues];

    // Add 2 for the end points of the polygon.
    m_polygonX = new int[2 * m_values.length + 2];
    m_polygonY = new int[2 * m_values.length + 2];

    // Set default so we're visable.
    setPreferredSize(new Dimension(200, 100));
  }

  public void add(double newValue) {

    m_values[m_cursor] = newValue;

    if (++m_cursor >= m_values.length) {
      m_cursor = 0;
    }

    m_recalculate = true;
    repaint();
  }

  public void setColor(Color color) {
    m_color = color;
  }

  public void setMaximum(double maximum) {
    m_maximum = maximum;
  }

  public void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);

    graphics.setColor(m_color);

    if (m_recalculate) {
      final double xScale = getWidth() / (double)m_values.length;

      for (int i = 0; i <= m_values.length; i++) {
        final int x = (int)(i * xScale);
        m_polygonX[2 * i] = x;
        m_polygonX[2 * i + 1] = x;
      }

      final double yScale =
        m_maximum > 0 ? getHeight() / m_maximum : 0d;

      int cursor = m_cursor;

      for (int i = 0; i < m_values.length; i++) {
        int y = (int)((m_maximum - m_values[cursor]) * yScale);

        if (y == 0 && m_maximum > m_values[cursor]) {
          y = 1;
        }

        m_polygonY[2 * i + 1] = y;
        m_polygonY[2 * i + 2] = y;

        if (++cursor >= m_values.length) {
          cursor = 0;
        }
      }

      m_polygonY[0] = (int)(m_maximum * yScale);
      m_polygonY[2 * m_values.length + 1] = m_polygonY[0];

      m_recalculate = false;
    }

    graphics.fillPolygon(m_polygonX, m_polygonY, 2 * m_values.length + 2);
  }
}
