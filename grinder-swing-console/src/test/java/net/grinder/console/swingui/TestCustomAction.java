// Copyright (C) 2008 - 2013 Philip Aston
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.grinder.console.common.Resources;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


/**
 * Unit tests for {@link CustomAction}.
 *
 * @author Philip Aston
 */
public class TestCustomAction {

  private static ImageIcon s_image1 = new ImageIcon();
  private static ImageIcon s_image2 = new ImageIcon();

  @Mock
  private Resources m_resources;

  @Mock
  private Translations m_translations;

  @Before
  public void setUp() {
    initMocks(this);

    when(m_resources.getImageIcon("x.image")).thenReturn(s_image1);
    when(m_resources.getImageIcon("blah.rollover-image")).thenReturn(s_image2);

    when(m_translations.translate("console.action/blah"))
    .thenReturn("lah");

    when(m_translations.translate("console.action/x"))
    .thenReturn("X");

    when(m_translations.translate("console.action/x-detail"))
    .thenReturn("X Tip");
  }

  private class MyAction extends CustomAction {

    public MyAction(String key) {
      super(m_resources, m_translations, key);
    }

    public MyAction(String key, boolean isDialogAction) {
      super(m_resources, m_translations, key, isDialogAction);
    }

    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public void firePropertyChange(
      String propertyName, Object oldValue, Object newValue) {
      super.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  @Test public void testConstruction() throws Exception {
    final MyAction action1 = new MyAction("blah");
    assertEquals("lah", action1.getValue(Action.NAME));
    assertNull(action1.getValue(Action.SHORT_DESCRIPTION));
    assertNull(action1.getValue(Action.SMALL_ICON));
    assertSame(s_image2, action1.getValue(CustomAction.ROLLOVER_ICON));
    assertEquals("blah", action1.getKey());

    final MyAction action2 = new MyAction("blah", true);
    assertEquals("lah...", action2.getValue(Action.NAME));

    final MyAction action3 = new MyAction("notthere", true);
    assertNull(action3.getValue(Action.NAME));

    final MyAction action4 = new MyAction("x");
    assertEquals("X", action4.getValue(Action.NAME));
    assertEquals("X Tip", action4.getValue(Action.SHORT_DESCRIPTION));
    assertSame(s_image1, action4.getValue(Action.SMALL_ICON));
  }

  @Test public void testRegisterButton() throws Exception {
    final MyAction action1 = new MyAction("blah");
    final MyAction action2 = new MyAction("blah");

    final JButton button = new JButton();
    button.setAction(action1);
    action1.registerButton(button);

    // Its a little sick to abuse the property listener when there's no
    // corresponding property.
    action1.firePropertyChange("setAction", null, action2);
    assertSame(action2, button.getAction());

    action1.firePropertyChange("whatever", null, action1);
    assertSame(action2, button.getAction());

    action1.registerButton(button);
    assertSame(action2, button.getAction());
  }

  @Test public void testRelevantToSelection() throws Exception {
    final MyAction action1 = new MyAction("blah");
    final MyAction action2 = new MyAction("x");

    assertFalse(action1.isRelevantToSelection());
    assertFalse(action2.isRelevantToSelection());

    action1.setRelevantToSelection(true);
    assertTrue(action1.isRelevantToSelection());
    assertFalse(action2.isRelevantToSelection());

    action1.setRelevantToSelection(false);
    assertFalse(action1.isRelevantToSelection());
    assertFalse(action2.isRelevantToSelection());
  }

}
