// Copyright (C) 2008 - 2009 Philip Aston
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

import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.grinder.console.common.Resources;
import net.grinder.console.common.StubResources;
import junit.framework.TestCase;


/**
 * Unit tests for {@link CustomAction}.
 *
 * @author Philip Aston
 */
public class TestCustomAction extends TestCase {

  private static ImageIcon s_image1 = new ImageIcon();
  private static ImageIcon s_image2 = new ImageIcon();

  private static Resources s_resources =
    new StubResources<Object>(
      new HashMap<String, Object>() { {
        put("blah.label", "lah");
        put("blah.rollover-image", s_image2);
        put("x.label", "X");
        put("x.tip", "X Tip");
        put("x.image", s_image1);
      } }
    );

  private static class MyAction extends CustomAction {

    public MyAction(String key) {
      super(s_resources, key);
    }

    public MyAction(String key, boolean isDialogAction) {
      super(s_resources, key, isDialogAction);
    }

    public void actionPerformed(ActionEvent e) {
    }

    public void firePropertyChange(
      String propertyName, Object oldValue, Object newValue) {
      super.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  public void testConstruction() throws Exception {
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

  public void testRegisterButton() throws Exception {
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

  public void testRelevantToSelection() throws Exception {
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
