// Copyright (C) 2008 - 2009 Philip Aston
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

package net.grinder.console.swingui;

import java.util.Enumeration;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;

import junit.framework.TestCase;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link IntegerField}.
 *
 * @author Philip Aston
 */
public class TestIntegerField extends TestCase {

  public void testBadConstruction() throws Exception {
    try {
      new IntegerField(11, 10);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    try {
      new IntegerField(1, 0);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    try {
      new IntegerField(3, -1);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }
  }

  public void testGetAndSet() throws Exception {
    final IntegerField field = new IntegerField(3, 7);

    assertEquals(3, field.getValue());

    try {
      field.setValue(2);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }

    try {
      field.setValue(8);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
    }


    field.setValue(7);
    assertEquals(7, field.getValue());


    field.setValue(3);
    assertEquals(3, field.getValue());


    field.setValue(4);
    assertEquals(4, field.getValue());


    final IntegerField field2 = new IntegerField(-1, 0);
    assertEquals(-1, field2.getValue());
  }

  public void testNumberOfColumns() throws Exception {
    assertEquals(1, new IntegerField(0, 1).getColumns());
    assertEquals(1, new IntegerField(0, 9).getColumns());
    assertEquals(2, new IntegerField(0, 10).getColumns());
    assertEquals(2, new IntegerField(98, 99).getColumns());
    assertEquals(3, new IntegerField(98, 100).getColumns());
    assertEquals(5, new IntegerField(-1010, 1000).getColumns());
  }

  public void testSetText() throws Exception {
    final IntegerField field = new IntegerField(10, 15);

    field.setText("abc");
    assertEquals(10, field.getValue());


    field.setText("14");
    assertEquals(14, field.getValue());
    assertEquals("14", field.getText());


    field.setText("20");
    assertEquals("", field.getText());


    field.setText("-");
    assertEquals("", field.getText());
    assertEquals(10, field.getValue());


    final IntegerField field2 = new IntegerField(-10, 10);
    field2.setText("-");
    assertEquals("-", field2.getText());
    assertEquals(-10, field2.getValue());

    field2.setText("10");
    assertEquals("10", field2.getText());
    assertEquals(10, field2.getValue());
  }

  public void testChangeListener() throws Exception {
    final IntegerField field = new IntegerField(0, 15);

    final RandomStubFactory<ChangeListener> listenerStubFactory =
      RandomStubFactory.create(ChangeListener.class);
    field.addChangeListener(listenerStubFactory.getStub());


    field.setText("10");
    listenerStubFactory.assertSuccess("stateChanged", ChangeEvent.class);
    listenerStubFactory.assertNoMoreCalls();


    field.getDocument().remove(0, 1);
    listenerStubFactory.assertSuccess("stateChanged", ChangeEvent.class);
    listenerStubFactory.assertNoMoreCalls();


    // Can test "changeUpdate" with default document,
    // never called for text area documents.
    final IntegerField field2 = new IntegerField(0, 10);
    final DefaultStyledDocument styledDocument = new DefaultStyledDocument();
    field2.setDocument(styledDocument);
    field2.addChangeListener(listenerStubFactory.getStub());
    listenerStubFactory.assertNoMoreCalls();

    final RandomStubFactory<AttributeSet> attributeSetStubFactory =
      RandomStubFactory.create(AttributeSet.class);

    attributeSetStubFactory.setResult("getAttributeNames",
      new Enumeration<String>() {
        public boolean hasMoreElements() { return false; }
        public String nextElement() { return null; }
      } );

    styledDocument.setCharacterAttributes(
      0, 1, attributeSetStubFactory.getStub(), false);
    listenerStubFactory.assertSuccess("stateChanged", ChangeEvent.class);

    listenerStubFactory.assertNoMoreCalls();
  }
}
