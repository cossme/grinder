// Copyright (C) 2005 - 2009 Philip Aston
// Copyright (C) 2007 Venelin Mitov
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

package net.grinder.tools.tcpproxy;

import java.awt.Component;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import junit.framework.TestCase;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for {@link TCPProxyConsole}.
 *
 * @author Philip Aston
 */
public class TestTCPProxyConsole extends TestCase {

  public void testConstructor() throws Exception {
    final RandomStubFactory<TCPProxyEngine> engineStubFactory =
      RandomStubFactory.create(TCPProxyEngine.class);

    final UpdatableCommentSource commentSource =
      new CommentSourceImplementation();

    final TCPProxyConsole console =
      new TCPProxyConsole(engineStubFactory.getStub(), commentSource);

    console.dispose();

    engineStubFactory.assertNoMoreCalls();
  }

  public void testButton() throws Exception {
    final RandomStubFactory<TCPProxyEngine> engineStubFactory =
      RandomStubFactory.create(TCPProxyEngine.class);

    final UpdatableCommentSource commentSource =
      new CommentSourceImplementation();

    final TCPProxyConsole console =
      new TCPProxyConsole(engineStubFactory.getStub(), commentSource);

    JButton stopButton = null;

    final Component[] components = console.getContentPane().getComponents();
    for (final Component component : components) {
      if (component instanceof JButton) {
        final JButton b = (JButton)component;
        if ("Stop".equals(b.getText())) {
          stopButton = (JButton)component;
        }
      }
    }

    assertNotNull(stopButton);

    if (stopButton != null) { // Shut up eclipse null warning.
      final JButton b = stopButton;

      SwingUtilities.invokeAndWait(new Runnable() {
          @Override
          public void run() {
            b.doClick();
          }
        });
    }
    engineStubFactory.assertSuccess("stop");
    engineStubFactory.assertNoMoreCalls();

    console.dispatchEvent(new WindowEvent(console, WindowEvent.WINDOW_CLOSING));
    engineStubFactory.assertSuccess("stop");
    engineStubFactory.assertNoMoreCalls();

    console.dispose();

    engineStubFactory.assertNoMoreCalls();


  }
}
