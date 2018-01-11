// Copyright (C) 2007 Philip Aston
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

package net.grinder.plugin.http;

import net.grinder.common.GrinderException;


/**
 * Indicates that an HTTP request timed out.
 *
 * <p>
 * The Grinder throws this exception, rather than simply marking a test failure
 * and continuing, since the rest of the test script might be invalid if the
 * request didn't complete successfully. If you want to ignore whether a
 * particular request timed out or not, you can write something like:
 * </p>
 *
 * <pre>
 * from net.grinder.plugin.http import TimeoutException
 * # ...
 * try: myrequest.GET(&quot;http://myurl.com&quot;)
 * except TimeoutException: pass
 * </pre>
 *
 * <p>
 * If you'd also like to mark the current test as "bad", do this:
 * </p>
 *
 * <pre>
 * grinder.statistics.delayReports = 1
 * # ...
 * try: myrequest.GET(&quot;http://myurl.com&quot;)
 * except TimeoutException: grinder.statistics.forLastTest.success = 0
 * </pre>
 *
 * @author Philip Aston
 */
public class TimeoutException extends GrinderException {

  /**
   * Creates a new <code>TimeoutException</code> instance.
   *
   * @param t Cause.
   */
  public TimeoutException(Throwable t) {
    super(t.getMessage());
    setStackTrace(t.getStackTrace());
  }
}
