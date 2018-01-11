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

package net.grinder.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.grinder.engine.process.StubTestRegistry;
import net.grinder.script.Test.InstrumentationFilter;
import net.grinder.scriptengine.Instrumenter;
import net.grinder.scriptengine.Recorder;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for {@code Test}.
 *
 * @author Philip Aston
 */
public class TestTest {

  @Mock private Instrumenter m_instrumenter;
  @Mock private InstrumentationFilter m_instrumentationFilter;
  @Mock private InternalScriptContext m_scriptContext;


  @Before public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    final TestRegistry testRegistry =
      StubTestRegistry.stubTestRegistry(m_instrumenter);

    when(m_scriptContext.getTestRegistry()).thenReturn(testRegistry);

    Grinder.grinder = m_scriptContext;
  }

  @org.junit.Test public void testGetters() throws Exception {
    final Test test = new Test(1, "description");

    assertEquals(1, test.getNumber());
    assertEquals("description", test.getDescription());
  }

  @org.junit.Test public void testOrdering() throws Exception {
    final int size = 100;

    final Set<Test> sorted = new TreeSet<Test>();
    final List<Integer> keys = new ArrayList<Integer>(size);

    for (int i=0; i<size; i++) {
      keys.add(new Integer(i));
    }

    Collections.shuffle(keys);

    for (Integer i : keys) {
      sorted.add(new Test(i, i.toString()));
    }

    int i = 0;

    for (Test test : sorted) {
      assertEquals(i++, test.getNumber());
    }
  }

  @org.junit.Test public void testEquality() throws Exception {

    // Equality depends only on test number.
    final Test t1 = new Test(57, "one thing");
    final Test t2 = new Test(57, "leads to");
    final Test t3 = new Test(58, "another");

    assertEquals(t1, t2);
    assertEquals(t2, t1);
    assertTrue(!t1.equals(t3));
    assertTrue(!t3.equals(t1));
    assertTrue(!t2.equals(t3));
    assertTrue(!t3.equals(t2));
  }

  @org.junit.Test public void testIsSerializable() throws Exception {

    final Test test = new Test(123, "test");

    final ByteArrayOutputStream byteArrayOutputStream =
      new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream =
      new ObjectOutputStream(byteArrayOutputStream);

    objectOutputStream.writeObject(test);
  }

  @org.junit.Test public void testWrap() throws Exception {
    final Test t1 = new Test(1, "six cars");
    new Test(2, "house in ireland");

    final Integer i = new Integer(10);

    t1.wrap(i);

    verify(m_instrumenter).createInstrumentedProxy(same(t1),
                                                   isA(Recorder.class),
                                                   same(i));
  }

  @org.junit.Test public void testRecord() throws Exception {
    final Test t1 = new Test(1, "bigger than your dad");

    final Integer i = new Integer(10);

    t1.record(i);

    verify(m_instrumenter).instrument(same(t1), isA(Recorder.class), same(i));
  }

  @org.junit.Test public void testSelectiveRecord() throws Exception {
    final Test t1 = new Test(1, "travelling funk band");

    final Integer i = new Integer(10);

    t1.record(i, m_instrumentationFilter);

    verify(m_instrumenter).instrument(same(t1),
                                      isA(Recorder.class),
                                      same(i),
                                      same(m_instrumentationFilter));
  }
}
