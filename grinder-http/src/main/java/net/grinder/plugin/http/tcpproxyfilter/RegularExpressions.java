// Copyright (C) 2006 - 2009 Philip Aston
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

import java.util.regex.Pattern;

/**
 * Common regular expressions.
 *
 * @author Philip Aston
 */
public interface RegularExpressions {

  /**
   * A pattern that matches the first line of an HTTP request.
   *
   * @return The pattern.
   */
  Pattern getRequestLinePattern();

  /**
   * A pattern that matches the first line of an HTTP response.
   *
   * @return The pattern.
   */
  Pattern getResponseLinePattern();

  /**
   * A pattern that matches an HTTP message body.
   *
   * @return The pattern.
   */
  Pattern getMessageBodyPattern();

  /**
   * A pattern that matches an HTTP header.
   *
   * @return The pattern
   */
  Pattern getHeaderPattern();

  /**
   * A pattern that matches an HTTP Basic Authorization header.
   *
   * @return The pattern
   */
  Pattern getBasicAuthorizationHeaderPattern();

  /**
   * A pattern that matches the last element in a path.
   *
   * @return The pattern
   */
  Pattern getLastPathElementPathPattern();

  /**
   * A pattern that matches URLs referenced by hyperlinks.
   *
   * @return The pattern.
   */
  Pattern getHyperlinkURIPattern();

  /**
   * A pattern that matches input parameters in HTML forms.
   *
   * @return The pattern.
   */
  Pattern getInputPattern();

  /**
   * A pattern that matches hidden input parameters in HTML forms.
   *
   * @return The pattern.
   */
  Pattern getHiddenInputPattern();
}
