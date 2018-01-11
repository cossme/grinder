// Copyright (C) 2004 Philip Aston
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
import javax.swing.AbstractAction;
import javax.swing.Action;


/**
 * Composite <code>Action</code> which fans out events to two
 * delegates.
 *
 * @author Philip Aston
 */
final class TeeAction extends AbstractAction {

  private final Action m_action1;
  private final Action m_action2;

  public TeeAction(Action action1, Action action2) {
    m_action1 = action1;
    m_action2 = action2;
  }

  public void actionPerformed(ActionEvent e) {

    if (m_action1.isEnabled()) {
      m_action1.actionPerformed(e);
    }

    if (m_action2.isEnabled()) {
      m_action2.actionPerformed(e);
    }
  }

  public boolean isEnabled() {
    return m_action1.isEnabled() || m_action2.isEnabled();
  }
}
