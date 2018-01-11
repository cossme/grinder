// Copyright (C) 2004 - 2009 Philip Aston
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

import junit.framework.TestCase;

import java.awt.event.ActionEvent;
import javax.swing.Action;

import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test for {@link TeeAction}.
 *
 * @author Philip Aston
 */
public class TestTeeAction extends TestCase {

  public void testTeeAction() throws Exception {
    final ActionStubFactory action1StubFactory = new ActionStubFactory();
    final Action action1 = action1StubFactory.getStub();

    final ActionStubFactory action2StubFactory = new ActionStubFactory();
    final Action action2 = action2StubFactory.getStub();

    final TeeAction teeAction = new TeeAction(action1, action2);

    final ActionEvent actionEvent =
      new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "foo");

    assertTrue(!teeAction.isEnabled());

    action1StubFactory.assertSuccess("isEnabled");
    action2StubFactory.assertSuccess("isEnabled");

    teeAction.actionPerformed(actionEvent);

    action1StubFactory.assertSuccess("isEnabled");
    action1StubFactory.assertNoMoreCalls();
    action2StubFactory.assertSuccess("isEnabled");
    action2StubFactory.assertNoMoreCalls();

    action2StubFactory.setEnabled(true);

    assertTrue(teeAction.isEnabled());

    action1StubFactory.resetCallHistory();
    action2StubFactory.resetCallHistory();

    teeAction.actionPerformed(actionEvent);

    action1StubFactory.assertSuccess("isEnabled");
    action1StubFactory.assertNoMoreCalls();
    action2StubFactory.assertSuccess("isEnabled");
    action2StubFactory.assertSuccess("actionPerformed", actionEvent);
    action2StubFactory.assertNoMoreCalls();

    action1StubFactory.setEnabled(true);

    assertTrue(teeAction.isEnabled());

    action1StubFactory.resetCallHistory();
    action2StubFactory.resetCallHistory();

    teeAction.actionPerformed(actionEvent);

    action1StubFactory.assertSuccess("isEnabled");
    action1StubFactory.assertSuccess("actionPerformed", actionEvent);
    action1StubFactory.assertNoMoreCalls();
    action2StubFactory.assertSuccess("isEnabled");
    action2StubFactory.assertSuccess("actionPerformed", actionEvent);
    action2StubFactory.assertNoMoreCalls();
  }

  public final static class ActionStubFactory
    extends RandomStubFactory<Action> {

    private boolean m_isEnabled = false;

    public ActionStubFactory() {
      super(Action.class);
    }

    public boolean override_isEnabled(Object proxy) {
      return m_isEnabled;
    }

    public void setEnabled(boolean b) {
      m_isEnabled = b;
    }
  }
}
