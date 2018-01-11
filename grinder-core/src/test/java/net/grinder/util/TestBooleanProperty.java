// Copyright (C) 2005 Philip Aston
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

import junit.framework.TestCase;


/**
 * Unit test case for {@link BooleanProperty}.
 *
 * @author Philip Aston
 */
public class TestBooleanProperty extends TestCase {

  public void testBooleanProperty() throws Exception {
    final Bean bean = new Bean();

    final BooleanProperty thingProperty = new BooleanProperty(bean, "thing");
    thingProperty.set(false);
    assertTrue(!thingProperty.get());
    thingProperty.set(true);
    assertTrue(thingProperty.get());

    try {
      new BooleanProperty(bean, "blah");
      fail("Expected a BooleanProperty.PropertyException");
    }
    catch (BooleanProperty.PropertyException e) {
    }

    try {
      new BooleanProperty(bean, "number");
      fail("Expected a BooleanProperty.PropertyException");
    }
    catch (BooleanProperty.PropertyException e) {
    }

    final BooleanProperty badProperty =
      new BooleanProperty(bean, "badProperty");

    try {
      badProperty.set(true);
    }
    catch (BooleanProperty.PropertyException e) {
      assertTrue(e.getCause() instanceof BeanException);
    }

    try {
      badProperty.get();
    }
    catch (BooleanProperty.PropertyException e) {
      assertTrue(e.getCause() instanceof BeanException);
    }

    // Can't think of a way to test the IllegalAccessException paths.
    // The PropertyDescriptor only finds public fields.
  }

  public static class Bean {

    public boolean m_thing;

    public boolean getThing() {
      return m_thing;
    }

    public void setThing(boolean b) {
      m_thing = b;
    }

    public int getNumber() {
      return 9;
    }

    public void setNumber(int n) {
    }

    public void setBadProperty(boolean b) throws BeanException {
      throw new BeanException();
    }

    public boolean getBadProperty() throws BeanException {
      throw new BeanException();
    }
  }

  private static final class BeanException extends Exception {
  }
}
