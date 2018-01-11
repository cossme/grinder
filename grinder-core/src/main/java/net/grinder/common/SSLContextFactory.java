// Copyright (C) 2005 - 2010 Philip Aston
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

package net.grinder.common;

import javax.net.ssl.SSLContext;


/**
 * Factory for {@link SSLContext}s.
 *
 * @author Philip Aston
 */
public interface SSLContextFactory {

  /**
   * Returns an appropriate JSSE {@link SSLContext}. This can be used to obtain
   * an {@link javax.net.ssl.SSLSocketFactory}.
   *
   * <p>
   * The Grinder optimises client SSL processing to increase the number of
   * simultaneous client threads it is reasonable to run. It uses an insecure
   * source of random information, and does not perform checks on the
   * certificates presented by a server. <b>Do not use The Grinder to implement
   * any SSL communication that you want to be secure.</b>
   * </p>
   *
   * @return The SSL context.
   * @exception SSLContextFactory.SSLContextFactoryException
   *              If the SSLContext could not be found/created.
   * @see net.grinder.script.SSLControl
   */
  SSLContext getSSLContext()
    throws SSLContextFactory.SSLContextFactoryException;

  /**
   * Exception that indicates problem creating an SSLContext.
   */
  public static final class SSLContextFactoryException
    extends GrinderException {

    /**
     * Constructor.
     *
     * @param message Helpful message.
     */
    public SSLContextFactoryException(String message) {
      super(message);
    }

    /**
     * Constructor.
     *
     * @param message Helpful message.
     * @param t A nested <code>Throwable</code>
     */
    public SSLContextFactoryException(String message, Throwable t) {
      super(message, t);
    }
  }
}

