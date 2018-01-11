// Copyright (C) 2004 Philip Aston
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

package net.grinder.console.common;

import junit.framework.TestCase;

import net.grinder.testutility.StubPrintWriter;


/**
 *  Unit test case for {@link DisplayMessageConsoleException}.
 *
 * @author Philip Aston
 */
public class TestDisplayMessageConsoleException extends TestCase {

  public void testDisplayMessageConsoleException() throws Exception {
    final ResourcesImplementation resources = new ResourcesImplementation(getClass().getName());
    resources.setErrorWriter(new StubPrintWriter());

    final DisplayMessageConsoleException e1 =
      new DisplayMessageConsoleException(resources, "notthere");

    assertTrue(e1.getMessage().startsWith("No message"));
    assertTrue(e1.getMessage().indexOf("notthere") >= 0);
    assertNull(e1.getCause());

    final DisplayMessageConsoleException e2 =
      new DisplayMessageConsoleException(resources, "helloworld");

    assertEquals("Hello world", e2.getMessage());
    assertNull(e2.getCause());

    final DisplayMessageConsoleException e3 =
      new DisplayMessageConsoleException(resources, "sum");

    assertEquals("{0} plus {1} is {2}", e3.getMessage());
    assertNull(e2.getCause());

    final DisplayMessageConsoleException e4 =
      new DisplayMessageConsoleException(resources, "sum", 
                                         new Object[]{
                                           new Integer(1),
                                           new Integer(2),
                                           "three"
                                         });

    assertEquals("1 plus 2 is three", e4.getMessage());
    assertNull(e2.getCause());

    final DisplayMessageConsoleException e5 =
      new DisplayMessageConsoleException(resources, "notthere", e4);

    assertTrue(e5.getMessage().startsWith("No message"));
    assertTrue(e5.getMessage().indexOf("notthere") >= 0);
    assertSame(e4, e5.getCause());

    final DisplayMessageConsoleException e6 =
      new DisplayMessageConsoleException(resources, "helloworld", e5);

    assertEquals("Hello world", e6.getMessage());
    assertSame(e5, e6.getCause());

    final DisplayMessageConsoleException e7 =
      new DisplayMessageConsoleException(resources,
                                         "numberOfFiles",
                                         new Object[] { new Integer(2) },
                                         e6);

    assertEquals("There are 2 files", e7.getMessage());
    assertSame(e6, e7.getCause());
  }
}
