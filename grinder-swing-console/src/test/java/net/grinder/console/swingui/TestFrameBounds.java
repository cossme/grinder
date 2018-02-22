// Copyright (C) 2006 - 2008 Philip Aston
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;

import javax.swing.JFrame;

import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;


/**
 * Unit tests for {@link FrameBounds}.
 *
 * @author Philip Aston
 */
public class TestFrameBounds extends AbstractFileTestCase {
  private static final Resources s_resources =
    new ResourcesImplementation(
      "net.grinder.console.common.resources.Console");

  private File m_file;

  protected void setUp() throws Exception {
    super.setUp();
    m_file = new File(getDirectory(), "properties");
  }

  public void testFrameBounds() throws Exception {
    if (!Boolean.getBoolean("build.travis")) {

      final ConsoleProperties properties =
              new ConsoleProperties(s_resources, m_file);

      final JFrame frame = new JFrame();

      final FrameBounds frameBounds = new FrameBounds(properties, frame);
      frameBounds.restore();

      final Rectangle bounds1 = frame.getBounds();
      assertEquals(new Dimension(900, 600), frame.getSize());
      AssertUtilities.assertNotEquals(new Point(0, 0), frame.getLocation());

      frameBounds.store();

      final ConsoleProperties properties2 =
              new ConsoleProperties(s_resources, m_file);

      final FrameBounds frameBounds2 = new FrameBounds(properties2, frame);
      frameBounds2.restore();

      assertEquals(bounds1, frame.getBounds());

      frame.setLocation(-1000, -1000);
      frameBounds.store();

      frameBounds.restore();

      assertEquals(bounds1, frame.getBounds());
    }
  }
}
