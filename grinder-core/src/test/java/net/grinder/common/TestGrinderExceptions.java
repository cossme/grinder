// Copyright (C) 2002 - 2011 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import net.grinder.testutility.RedirectStandardStreams;

import org.junit.Test;


/**
 * Unit test for {@link GrinderException}.
 *
 * @author Philip Aston
 */
public class TestGrinderExceptions {

  // Calculate the callers method name. This is so the tests still work when
  // this class is instrumented by Clover.
  private static final String getMethodName() {
    final StackTraceElement callersFrame = new Exception().getStackTrace()[1];
    return callersFrame.toString().replaceAll("\\(.*", "");
  }

  @Test public void testPrintStackTrace() throws Exception {
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);

    final GrinderException e1 = createDeeperException();
    final GrinderException e2 = new MyGrinderException("Exception 2", e1);

    e2.printStackTrace(printWriter);
    final String s = stringWriter.toString();

    assertEquals(1, countOccurrences("createException", s));
    assertEquals(1, countOccurrences("createDeeperException", s));
    assertEquals(2, countOccurrences(getMethodName(), s));
  }

  @Test public void testPrintStackTraceWithNestedNonGrinderException()
    throws Exception {

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);

    final Exception e1 = new RuntimeException();
    final GrinderException e2 = new MyGrinderException("Exception 2", e1);

    e2.printStackTrace(printWriter);
    final String s = stringWriter.toString();

    assertEquals(1, countOccurrences("RuntimeException", s));
    assertEquals(2, countOccurrences(getMethodName(), s));
  }

  private static class WeirdException extends RuntimeException {
    public void printStackTrace(PrintWriter w) {
      w.println("Unconventional stack trace");
      w.flush();
    }
  }

  @Test public void testPrintStackTraceWithNestedUnconventionalException()
    throws Exception {

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);

    final Exception e1 = new WeirdException();
    final GrinderException e2 = new MyGrinderException("Exception 2", e1);

    e2.printStackTrace(printWriter);
    final String s = stringWriter.toString();

    assertEquals(2, countOccurrences(getMethodName(), s));
    assertEquals(1, countOccurrences("...", s));
  }

  @Test public void testPrintStackTraceWithPrintStream() throws Exception {
    final ByteArrayOutputStream byteArrayOutputStream =
      new ByteArrayOutputStream();
    final PrintStream printStream = new PrintStream(byteArrayOutputStream);

    final GrinderException e1 = createDeeperException();
    final GrinderException e2 = new MyGrinderException("Exception 2", e1);

    e2.printStackTrace(printStream);
    final String s = new String(byteArrayOutputStream.toByteArray());

    assertEquals(1, countOccurrences("createException", s));
    assertEquals(1, countOccurrences("createDeeperException", s));
    assertEquals(2, countOccurrences(getMethodName(), s));
  }

  @Test public void testPrintStackTraceWithDefaultStream() throws Exception {

    final GrinderException e1 = createDeeperException();
    final GrinderException e2 = new MyGrinderException("Exception 2", e1);

    final RedirectStandardStreams streams = new RedirectStandardStreams() {
      protected void runWithRedirectedStreams() throws Exception {
        e2.printStackTrace();
      }
    };

    streams.run();

    final String s = new String(streams.getStderrBytes());

    assertEquals(1, countOccurrences("createException", s));
    assertEquals(1, countOccurrences("createDeeperException", s));
    assertEquals(2, countOccurrences(getMethodName(), s));
  }

  private GrinderException createException() {
    return new MyGrinderException("an exception");
  }

  private GrinderException createDeeperException() {
    return createException();
  }

  private int countOccurrences(String pattern, String original) {
    int result = 0;
    int p = -1;

    while ((p=original.indexOf(pattern, p + 1)) >= 0) {
      ++result;
    }

    return result;
  }

  private static final class MyGrinderException extends GrinderException {

    public MyGrinderException(String message) {
      super(message);
    }

    public MyGrinderException(String string, Throwable e1) {
      super(string, e1);
    }
  }

  @Test public void testUncheckedGrinderException() throws Exception {
    final RuntimeException e1 = new UncheckedGrinderException("test") {};
    assertEquals("test", e1.getMessage());

    final UncheckedGrinderException e2 =
      new UncheckedGrinderException("test2", e1) {};
    assertEquals("test2", e2.getMessage());
    assertEquals(e1, e2.getCause());

    try {
      UncheckedGrinderException.class.newInstance();
      fail("UncheckedGrinderException is not abstract");
    }
    catch (InstantiationException e) {
    }
  }
}
