// Copyright (C) 2006, 2007 Philip Aston
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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.model.ConsoleProperties;


/**
 * Class that records and restores the console frame's size and location from
 * properties.
 *
 * @author Philip Aston
 */
final class FrameBounds {

  private final ConsoleProperties m_properties;
  private final JFrame m_frame;

  public FrameBounds(ConsoleProperties properties, JFrame frame) {
    m_properties = properties;
    m_frame = frame;
  }

  public void restore() {
    m_frame.pack();

    final Rectangle savedBounds = m_properties.getFrameBounds();
    final GraphicsEnvironment graphicsEnvironment =
      GraphicsEnvironment.getLocalGraphicsEnvironment();

    boolean setDefaultLocation = true;

    if (savedBounds != null) {
      m_frame.setBounds(
        savedBounds.x,
        savedBounds.y,
        Math.max(savedBounds.width, 300),
        Math.max(savedBounds.height, 200));

      // Check whether the saved bounds are visible on one of our screens,
      // if not we'll reset the location.
      final GraphicsDevice[] devices = graphicsEnvironment.getScreenDevices();

      FIND_CONFIGURATION:
      for (int i = 0; i < devices.length; ++i) {
        final GraphicsConfiguration[] configurations =
          devices[i].getConfigurations();

        for (int j = 0; j < configurations.length; ++j) {
          if (savedBounds.intersects(
            shrinkRectangle(configurations[j].getBounds(), 50))) {
            setDefaultLocation = false;
            break FIND_CONFIGURATION;
          }
        }
      }

    }
    else {
      m_frame.setSize(900, 600);
    }

    final Rectangle defaultScreen =
      graphicsEnvironment.getDefaultScreenDevice().getDefaultConfiguration()
      .getBounds();

    if (setDefaultLocation) {
      m_frame.setLocation(
        defaultScreen.x +
        defaultScreen.width / 2 - m_frame.getSize().width / 2,
        defaultScreen.y +
        defaultScreen.height / 2 - m_frame.getSize().height / 2);
    }
  }

  private Rectangle shrinkRectangle(Rectangle bounds, int amount) {
    return new Rectangle(bounds.x + amount,
                         bounds.y + amount,
                         bounds.width - 2 * amount,
                         bounds.height - 2 * amount);
  }

  public void store() throws ConsoleException {
    m_properties.setAndSaveFrameBounds(m_frame.getBounds());
  }
}
