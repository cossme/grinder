// Copyright (C) 2005 - 2011 Philip Aston
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

package net.grinder.testutility;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.Assert.*;


/**
 * Utility template which redirects and restores stdout and stderr.
 *
 * @author Philip Aston
 */
public abstract class RedirectStandardStreams {

  private final ByteArrayOutputStream m_stdoutContent =
    new ByteArrayOutputStream();
  private final ByteArrayOutputStream m_stderrContent =
    new ByteArrayOutputStream();

  public final RedirectStandardStreams run() throws Exception {
    final PrintStream oldStdout = System.out;
    final PrintStream oldStderr = System.err;

    System.setOut(new PrintStream(m_stdoutContent));
    System.setErr(new PrintStream(m_stderrContent));

    try {
      runWithRedirectedStreams();
    }
    finally {
      System.setOut(oldStdout);
      System.setErr(oldStderr);
    }

    return this;
  }

  public final RedirectStandardStreams assertNoStdout() {
    final byte[] bytes = getStdoutBytes();

    assertTrue("stdout = " + new String(bytes), bytes.length == 0);

    return this;
  }

  public final RedirectStandardStreams assertNoStderr() {
    final byte[] bytes = getStderrBytes();

    assertTrue("stderr = " + new String(bytes), bytes.length == 0);

    return this;
  }

  public final byte[] getStdoutBytes() {
    return m_stdoutContent.toByteArray();
  }

  public final byte[] getStderrBytes() {
    return m_stderrContent.toByteArray();
  }

  protected abstract void runWithRedirectedStreams() throws Exception;

  public String toString() {
    final StringBuffer result = new StringBuffer();

    result.append("RedirectStandardStreams");
    result.append("\nstdout:\n");
    result.append(new String(m_stdoutContent.toByteArray()));
    result.append("\nstderr:\n");
    result.append(new String(m_stderrContent.toByteArray()));

    return result.toString();
  }
}
