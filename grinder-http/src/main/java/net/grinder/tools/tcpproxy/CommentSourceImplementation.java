// Copyright (C) 2007 Venelin Mitov
// Copyright (C) 2007 - 2009 Philip Aston
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

import java.util.ArrayList;
import java.util.List;


/**
 * Thread-safe implementation of the CommentSource and UpdatableCommentSource
 * interfaces. Manages a FIFO of comments.
 *
 * @author Venelin Mitov
 */
public class CommentSourceImplementation implements UpdatableCommentSource {

  private final List<String> m_commentQueue = new ArrayList<String>(10);

  /**
   * @param comment The comment-string to be added.
   *
   * @see net.grinder.tools.tcpproxy.UpdatableCommentSource
   *    #addComment(java.lang.String)
   */
  public synchronized void addComment(String comment) {
    m_commentQueue.add(comment);
  }

  /**
   * Get the comments added by the user after the previous call to getComments()
   * up to now. The returned comments are excluded from the underlying
   * collection and will not be returned in subsequent calls.
   *
   * @return An array of all the comments inserted after the previous call to
   *         getComments() up to now. If no comments have been inserted an empty
   *         array is returned.
   */
  public synchronized String[] getComments() {
    final String[] res =
      m_commentQueue.toArray(new String[m_commentQueue.size()]);
    m_commentQueue.clear();
    return res;
  }
}
