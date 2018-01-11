// Copyright (C) 2005 - 2012 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import java.io.File;

import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.TokenReferenceType;
import net.grinder.tools.tcpproxy.ConnectionDetails;


/**
 * Interface for recording HTTP stream information.
 *
 * @author Philip Aston
 */
public interface HTTPRecording {

  /**
   * Return the parameters to use for the recording.
   *
   * @return The parameters.
   */
  HTTPRecordingParameters getParameters();

  /**
   * Add a new request to the recording.
   *
   * <p>
   * The request is returned to allow the caller to add things it doesn't know
   * yet, e.g. headers, body, response.
   * </p>
   *
   * @param connectionDetails
   *          The connection used to make the request.
   * @param method
   *          The HTTP method.
   * @param relativeURI
   *          The URI.
   * @return The request.
   */
  RequestType addRequest(
    ConnectionDetails connectionDetails, String method, String relativeURI);

  /**
   * Called when a response message starts. Because the test script represents a
   * single thread of control we need to calculate the sleep deltas using the
   * last time any response was received on any connection.
   */
  void markLastResponseTime();

  /**
   * Fill in token reference details, creating the token if necessary.
   *
   * <p>
   * The reference source is cached for use by
   * {@link #tokenReferenceExists(String, String)}, so it should be set before
   * this method is called.
   * </p>
   *
   * @param name The name.
   * @param value The value.
   * @param tokenReference This reference is set with the appropriate
   * token ID, and the new value is set if appropriate.
   */
  void setTokenReference(
    String name, String value, TokenReferenceType tokenReference);

  /**
   * Return the last value recorded for the given token.
   *
   * @param name The token name.
   * @return The last value, or <code>null</code> if no token reference
   * for this token has been seen.
   */
  String getLastValueForToken(String name);

  /**
   * Check for existence of token. The token must have at least one previous
   * reference with a source type of <code>source</code>.
   *
   * @param name
   *          Token name.
   * @param source
   *          Token source.
   * @return <code>true</code> if a token with name <code>name</code>
   *         exists, and has at least one reference with a source type of
   *         <code>source</code>.
   */
  boolean tokenReferenceExists(String name, String source);

  /**
   * Create a new file name for body data.
   *
   * @return The file name.
   */
  File createBodyDataFileName();
}
