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

package net.grinder.messages.console;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.grinder.communication.Message;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsException;
import net.grinder.statistics.StatisticsServicesImplementation;


/**
 * Message used to register a expression view with Console.
 *
 * @author Philip Aston
 */
public final class RegisterExpressionViewMessage
  implements Message, Externalizable {

  private static final long serialVersionUID = 1L;

  private ExpressionView m_expressionView;

  /**
   * Constructor.
   *
   * @param statisticsView Definition of expression view.
   */
  public RegisterExpressionViewMessage(ExpressionView statisticsView) {
    m_expressionView = statisticsView;
  }

  /**
   * Default constructor for externalisation.
   */
  public RegisterExpressionViewMessage() {
  }

  /**
   * Get the expression view.
   *
   * @return The expression view.
   */
  public ExpressionView getExpressionView() {
    return m_expressionView;
  }


  /**
   * Externalisation method.
   *
   * @param out Handle to the output stream.
   * @exception IOException If an I/O error occurs.
   */
  public void writeExternal(ObjectOutput out) throws IOException {

    if (m_expressionView.getExpressionString() == null) {
      throw new IOException(
        "This expression view is not externalisable");
    }

    out.writeUTF(m_expressionView.getDisplayName());
    out.writeUTF(m_expressionView.getExpressionString());
  }

  /**
   * Externalisation method.
   *
   * @param in Handle to the input stream.
   * @exception IOException If an I/O error occurs.
   */
  public void readExternal(ObjectInput in) throws IOException {

    try {
      m_expressionView =
        StatisticsServicesImplementation.getInstance()
        .getStatisticExpressionFactory()
        .createExpressionView(in.readUTF(), in.readUTF(), false);
    }
    catch (StatisticsException e) {
      throw new IOException(
        "Could not instantiate ExpressionView: " + e.getMessage());
    }
  }
}
