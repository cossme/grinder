// Copyright (C) 2003 Bertrand Ave
// Copyright (C) 2005, 2006, 2007 Philip Aston
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

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


/**
 * Console for the TCPProxy.
 *
 * @author Bertrand Ave
 * @author Venelin Mitov
 */
public final class TCPProxyConsole extends JFrame {
  /**
   * A store for user comments inserted during capture.
   */
  private final UpdatableCommentSource m_commentSource;

  /**
   * Constructor.
   *
   * @param proxyEngine
   *          The <code>TCPProxyEngine</code> we control.
   * @param commentSource
   *          The <code>UpdatableCommentSource</code> where user comments are
   *          inserted.
   */
  public TCPProxyConsole(final TCPProxyEngine proxyEngine,
                         UpdatableCommentSource commentSource) {

    super("TCPProxy Console");

    m_commentSource = commentSource;
    setResizable(false);

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        proxyEngine.stop();
      }
    });

    final Container content = getContentPane();
    content.setBackground(Color.white);
    content.setLayout(new FlowLayout());

    final JTextField commentTextField =
      new JTextField("Insert a comment and press enter", 25);
    content.add(commentTextField);

    final JButton commentButton = new JButton("Insert comment");
    content.add(commentButton);

    // Add the same ActionListener to both the commenTextField and button1
    final ActionListener addCommentActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_commentSource.addComment(commentTextField.getText());
        commentTextField.setText(null);
      }
    };
    commentButton.addActionListener(addCommentActionListener);
    commentTextField.addActionListener(addCommentActionListener);

    final JButton stopButton = new JButton("Stop");
    stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        proxyEngine.stop();
      }
    });

    stopButton.setHorizontalTextPosition(SwingConstants.LEFT);
    content.add(stopButton);

    pack();
    setVisible(true);
  }
}
