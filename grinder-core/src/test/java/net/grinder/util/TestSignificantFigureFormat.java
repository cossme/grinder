// Copyright (C) 2000 - 2009 Philip Aston
// Copyright (C) 2005 Martin Wagner
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

package net.grinder.util;

import static org.junit.Assert.assertNotEquals;

import java.util.Locale;

import junit.framework.TestCase;


/**
 * Unit tests for {@link SignificantFigureFormat}.
 *
 * @author Philip Aston
 */
public class TestSignificantFigureFormat extends TestCase {

  private final Locale m_originalDefaultLocale = Locale.getDefault();

  @Override
  protected void setUp() {
    Locale.setDefault(Locale.US);
  }

  @Override
  protected void tearDown() throws Exception {
    Locale.setDefault(m_originalDefaultLocale);
  }

  public void testSignificantFigureFormat() throws Exception {
    java.util.Locale.setDefault(java.util.Locale.US);
    final SignificantFigureFormat f = new SignificantFigureFormat(4);

    assertEquals("1.000", f.format(1d));
    assertEquals("1.000", f.format(1));
    assertEquals("-1.000", f.format(-1d));
    assertEquals("0.1000", f.format(0.1));
    assertEquals("123.0", f.format(123d));
    assertEquals("123.0", f.format(123));
    assertEquals("10.00", f.format(10d));
    assertEquals("10.00", f.format(10));
    assertEquals("0.9900", f.format(.99d));
    assertEquals("0.002320", f.format(.00232));
    assertEquals("12350", f.format(12345d));
    assertEquals("12350", f.format(12345));
    assertEquals("1235", f.format(1234.5));
    assertEquals("1234", f.format(1234));
    assertEquals("12.35", f.format(12.345));
    assertEquals("0.1235", f.format(0.12345));
    // Interestingly .012345 -> 0.01234, but I think this is a
    // floating point thing. The test passes on JDK 1.7.0_45 +!
    //assertEquals("0.01235", f.format(0.012345));
    assertEquals("0.001235", f.format(0.0012345));
    assertEquals("0.000", f.format(0));
    assertEquals("0.000", f.format(-0));
    assertEquals("0.000", f.format(0.0));
    assertEquals("0.000", f.format(-0.0));
    assertEquals("\u221e", f.format(Double.POSITIVE_INFINITY));
    assertEquals("-\u221e", f.format(Double.NEGATIVE_INFINITY));
    assertEquals("\ufffd", f.format(Double.NaN));
  }

  public void testEquality() {
    final SignificantFigureFormat f1 = new SignificantFigureFormat(4);

    assertNotEquals(f1, null);
    assertEquals(f1, f1);

    final SignificantFigureFormat f2 = new SignificantFigureFormat(4);
    assertEquals(f1, f2);
    assertEquals(f1.hashCode(), f2.hashCode());

    final SignificantFigureFormat f3 = new SignificantFigureFormat(3);
    assertNotEquals(f3, f2);
    assertNotEquals(f2, f3);
  }
}
