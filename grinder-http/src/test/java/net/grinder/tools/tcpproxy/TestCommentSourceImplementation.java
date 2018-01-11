// Copyright (C) 2007 Venelin Mitov
// Copyright (C) 2007 - 2013 Philip Aston
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit test case for {@linkplain CommentSource}.
 *
 * @author Venelin Mitov
 */
public class TestCommentSourceImplementation {

  /**
   * Single thread test
   *
   * @throws Exception
   */
  @Test
  public void testAddGetComments() throws Exception {
    final String[] comments = new String[] { "BEGIN Enter home URL",
                                            "END Enter home URL",
                                            "BEGIN Click Sign In",
                                            "END Click Sign In",
                                            "BEGIN Click Read Mails",
                                            "END Click Read Mails", };

    final CommentSourceImplementation csi = new CommentSourceImplementation();

    assertArrayEquals(new String[0], csi.getComments());

    csi.addComment(comments[0]);
    assertEquals(comments[0], csi.getComments()[0]);
    assertArrayEquals(new String[0], csi.getComments());

    csi.addComment(comments[1]);
    assertEquals(comments[1], csi.getComments()[0]);
    assertArrayEquals(new String[0], csi.getComments());

    csi.addComment(comments[2]);
    csi.addComment(comments[3]);
    csi.addComment(comments[4]);

    final String[] currentComments = csi.getComments();

    assertEquals(comments[2], currentComments[0]);
    assertEquals(comments[3], currentComments[1]);
    assertEquals(comments[4], currentComments[2]);

    assertArrayEquals(new String[0], csi.getComments());
    assertArrayEquals(new String[0], csi.getComments());

    csi.addComment(comments[5]);
    assertEquals(comments[5], csi.getComments()[0]);
    assertArrayEquals(new String[0], csi.getComments());
  }
}
