// Copyright (C) 2004 - 2011 Philip Aston
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

package net.grinder.util.html;


/**
 * Abstract node for simple HTML model.
 *
 *  @author Philip Aston
 */
abstract class AbstractHTMLNode {

  /**
   * Write HTML to string buffer.
   *
   * @param buffer The <code>StringBuilder</code>.
   * @param html <code>true</code> => format as HTML;
   * <code>false</code> => format as plain text.
   */
  protected abstract void toStringBuilder(StringBuilder buffer, boolean html);

  /**
   * Return a plain text version of the node.
   *
   * @return The text.
   */
  public String toText() {
    final StringBuilder result = new StringBuilder();
    toStringBuilder(result, false);
    return result.toString();
  }

  /**
   * Return the HTML text.
   *
   * @return The HTML text.
   */
  public String toHTML() {
    final StringBuilder result = new StringBuilder();
    toStringBuilder(result, true);
    return result.toString();
  }

  /**
   * Override for debugging.
   *
   * @return Useful description of object.
   */
  public String toString() {
    return toText();
  }
}
