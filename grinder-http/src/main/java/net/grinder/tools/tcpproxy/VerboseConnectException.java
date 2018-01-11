// Copyright (C) 2005 Philip Aston
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

import java.net.ConnectException;


/**
 * VerboseConnectException: a {@link ConnectException} that has a more useful
 * message.
 *
 * @author Philip Aston
 */
public class VerboseConnectException extends ConnectException {

  private final Throwable m_cause;

  /**
   * Constructor.
   *
   * @param e Original {@link ConnectException}.
   * @param endPointDescription The end point that we were trying to contact.
   */
  public VerboseConnectException(ConnectException e,
                                 String endPointDescription) {
    super("Failed to connect to " + endPointDescription +
          " (" + e.getMessage() + ")");
    m_cause = e;
  }

  /**
   * Sigh, {@link ConnectException} doesn't have a constructor that takes a
   * cause.
   *
   * @return Our cause.
   */
  public Throwable getCause() {
    return m_cause;
  }
}
