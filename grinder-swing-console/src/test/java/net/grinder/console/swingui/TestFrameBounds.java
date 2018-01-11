// Copyright (C) 2006 - 2013 Philip Aston
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
import static org.mockito.MockitoAnnotations.initMocks;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;

import javax.swing.JFrame;

import net.grinder.console.model.ConsoleProperties;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit tests for {@link FrameBounds}.
 *
 * @author Philip Aston
 */
public class TestFrameBounds extends AbstractJUnit4FileTestCase {
  @Mock private Translations m_translations;

  private File m_file;

  @Before public void setUp() throws Exception {
    initMocks(this);

    m_file = new File(getDirectory(), "properties");
  }

  @Test public void testFrameBounds() throws Exception {
    final ConsoleProperties properties =
      new ConsoleProperties(m_translations, m_file);

    final JFrame frame = new JFrame();

    final FrameBounds frameBounds = new FrameBounds(properties, frame);
    frameBounds.restore();

    final Rectangle bounds1 = frame.getBounds();
    assertEquals(new Dimension(900, 600), frame.getSize());
    AssertUtilities.assertNotEquals(new Point(0, 0), frame.getLocation());

    frameBounds.store();

    final ConsoleProperties properties2 =
      new ConsoleProperties(m_translations, m_file);

    final FrameBounds frameBounds2 = new FrameBounds(properties2, frame);
    frameBounds2.restore();

    assertEquals(bounds1, frame.getBounds());

    frame.setLocation(-1000, -1000);
    frameBounds.store();

    frameBounds.restore();

    assertEquals(bounds1, frame.getBounds());
  }
}
