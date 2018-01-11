// Copyright (C) 2009 Philip Aston
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

package grinder.test;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Test class used by
 * {@link net.grinder.scriptengine.jython.instrumentation.AbstractJythonInstrumenterTestCase}.
 *
 * <p>
 * Needs to be outside of the {@code net.grinder} package so it can be
 * instrumented.
 * </p>
 */
public class MyClass implements Adder {
  private int m_a;
  private int m_b;
  private int m_c;

  public MyClass() {
    this(0, 0, 0);
  }

  public MyClass(final int a, final int b, final int c) {
    m_a = a;
    m_b = b;
    m_c = c;
  }

  @Override
  public int addOne(final int i) {
    return i + 1;
  }

  public int sum(final int x, final int y) {
    return x + y;
  }

  public int sum3(final int x, final int y, final int z) {
    return x + y + z;
  }

  public static int addTwo(final int i) {
    return i + 2;
  }

  public static int staticSum(final int x, final int y) {
    return x + y;
  }

  public static int staticSum3(final int x, final int y, final int z) {
    return x + y + z;
  }

  public static int staticSix() {
    return 6;
  }

  public int getA() {
    return m_a;
  }

  public void setA(final int a) {
    m_a = a;
  }

  public int getB() {
    return m_b;
  }

  public void setB(final int b) {
    m_b = b;
  }

  public int getC() {
    return m_c;
  }

  public void setC(final int c) {
    m_c = c;
  }

  public interface IOOperation {
    int run() throws IOException;
  }

  public int topLevelExceptionHandler(final IOOperation r) {
    try {
      return r.run();
    }
    catch (final SocketTimeoutException e) {
      return 1;
    }
    catch (final Throwable e) {
      return -1;
    }
    finally {
    }
  }
}
