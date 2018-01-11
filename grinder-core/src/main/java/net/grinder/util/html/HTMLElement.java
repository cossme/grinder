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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Element node for simple HTML model.
 *
 *  @author Philip Aston
 */
public class HTMLElement extends AbstractHTMLNode {

  private final String m_name;
  private final List<AbstractHTMLNode> m_children =
    new ArrayList<AbstractHTMLNode>();

  /**
   * Constructor.
   *
   * @param name Element name.
   */
  public HTMLElement(String name) {
    m_name = name;
  }

  /**
   * Constructor for HTML fragment. {@link #toText()} and {@link #toHTML()}
   * methods return representations of children without an enclosing
   * node.
   */
  public HTMLElement() {
    m_name = null;
  }

  /**
   * Add a child element.
   *
   * @param childText Child element name.
   * @return The added child.
   */
  public final HTMLElement addElement(String childText) {
    final HTMLElement child = new HTMLElement(childText);
    m_children.add(child);
    return child;
  }

  /**
   * Add child text.
   *
   * @param text The text value.
   */
  public final void addText(String text) {
    m_children.add(new HTMLText(text));
  }

  /**
   * Write HTML to string buffer.
   *
   * @param buffer The <code>StringBuilder</code>.
   * @param html <code>true</code> => format as HTML;
   * <code>false</code> => format as plain text.
   */
  protected final void toStringBuilder(StringBuilder buffer, boolean html) {
    if (m_children.size() > 0) {
      if (!isFragment()) {
        if (html) {
          buffer.append('<');
          buffer.append(m_name);
          buffer.append(">");
        }
      }

      final Iterator<AbstractHTMLNode> iterator = m_children.iterator();

      while (iterator.hasNext()) {
        iterator.next().toStringBuilder(buffer, html);
      }

      if (!isFragment()) {
        if (html) {
          buffer.append("</");
          buffer.append(m_name);
          buffer.append(">");
        }
        else if (isNewLineElement()) {
          buffer.append("\n");
        }
      }
    }
    else {
      if (!isFragment()) {
        if (html) {
          buffer.append("<");
          buffer.append(m_name);
          buffer.append("/>");
        }
        else if (isNewLineElement()) {
          buffer.append("\n");
        }
      }
    }
  }

  private boolean isNewLineElement() {
    return
      "p".equalsIgnoreCase(m_name) ||
      "br".equalsIgnoreCase(m_name);
  }

  private boolean isFragment() {
    return m_name == null;
  }
}
