// Copyright (C) 2001 - 2009 Philip Aston
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

package net.grinder.statistics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>TestStatisticsMap</code>.
 *
 * @author Philip Aston
 * @see TestStatisticsMap
 */
public class TestTestStatisticsMap extends TestCase {

  private final Test m_test0 = new StubTest(0, "foo");
  private final Test m_test1 = new StubTest(1, "");
  private final Test m_test2 = new StubTest(2, "");
  private StatisticsSet m_statistics0;
  private StatisticsSet m_statistics1;
  private StatisticsIndexMap.LongIndex m_index;

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  protected void setUp() throws Exception {
    final StatisticsSetFactory factory =
      m_statisticsServices.getStatisticsSetFactory();

    m_statistics0 = factory.create();
    m_statistics1 = factory.create();

    m_index =
      m_statisticsServices.getStatisticsIndexMap().getLongIndex("userLong0");

    m_statistics0.addValue(m_index, 10);
  }

  public void testPut() throws Exception {

    final TestStatisticsMap map =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());

    assertEquals(0, map.size());

    map.put(m_test0, m_statistics0);
    assertEquals(1, map.size());

    map.put(m_test0, m_statistics1);
    assertEquals(1, map.size());

    map.put(m_test1, m_statistics1);
    assertEquals(2, map.size());

    final StatisticsSet bogusStatisticsSetImplementation =
      RandomStubFactory.create(StatisticsSet.class).getStub();

    try {
      map.put(m_test1, bogusStatisticsSetImplementation);
      fail("Expected AssertionError");
    }
    catch (AssertionError e) {
    }
  }

  public void testEqualsAndHashCode() throws Exception {

    final TestStatisticsMap map0 =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());
    final TestStatisticsMap map1 =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());

    assertEquals(map0, map0);
    assertEquals(map0, map1);

    map0.put(m_test0, m_statistics0);
    assertFalse(map0.equals(map1));

    map1.put(m_test1, m_statistics0);
    assertFalse(map0.equals(map1));

    map0.put(m_test1, m_statistics0);
    map1.put(m_test0, m_statistics0);
    assertEquals(map0, map0);
    assertEquals(map0, map1);

    map1.put(m_test0, m_statistics1);
    assertFalse(map0.equals(map1));

    assertFalse(map0.equals(this));
    assertEquals(map0.hashCode(), map0.hashCode());
    assertFalse(map0.hashCode() == map1.hashCode());
  }

  public void testAdd() throws Exception {
    final TestStatisticsMap map0 =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());
    final TestStatisticsMap map1 =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());

    assertEquals(map0, map1);

    map0.put(m_test0, m_statistics0);
    assertFalse(map0.equals(map1));

    map1.add(map0);

    assertEquals(map0, map1);

    map1.add(map0);

    assertEquals(1, map1.size());

    final Pair content = extract(map1).get(0);
    assertEquals(m_test0, content.getTest());
    assertEquals(20, content.getStatisticsSet().getValue(m_index));
  }

  public void testReset() throws Exception {
    final TestStatisticsMap map =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());
    assertEquals(0, map.size());

    final TestStatisticsMap snapshot0 = map.reset();
    assertEquals(map, snapshot0);

    map.put(m_test0, m_statistics0);
    assertFalse(map.equals(snapshot0));

    final TestStatisticsMap snapshot1 = map.reset();
    assertFalse(snapshot1.equals(snapshot0));
    assertFalse(snapshot1.equals(map));
    assertEquals(1, snapshot1.size());
    assertEquals(1, map.size());

    final Pair content = extract(map).get(0);
    assertEquals(m_test0, content.getTest());
    assertEquals(0, content.getStatisticsSet().getValue(m_index));

    final Pair snapShotContent = extract(snapshot1).get(0);
    assertEquals(m_test0, snapShotContent.getTest());
    assertEquals(10, snapShotContent.getStatisticsSet().getValue(m_index));

    // The map was reset, so the statistics will be zero and reset()
    // will not copy them through.
    final TestStatisticsMap snapshot2 = map.reset();
    assertFalse(snapshot2.equals(snapshot1));
    assertFalse(snapshot2.equals(map));
    assertEquals(0, snapshot2.size());
    assertEquals(1, map.size());
  }

  public void testToString() throws Exception {
    final TestStatisticsMap map =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());
    final String s0 = map.toString();
    assertEquals("TestStatisticsMap = {}", s0);

    map.put(m_test0, m_statistics0);
    final String s1 = map.toString();
    assertTrue(s1.indexOf("(") > 0);
    assertTrue(s1.indexOf("10") > 0);
  }

  public void testSerialisation() throws Exception {

    final TestStatisticsMap original0 =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());
    original0.put(m_test0, m_statistics0);
    original0.put(m_test1, m_statistics0);

    final TestStatisticsMap original1 =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());

    final ByteArrayOutputStream byteOutputStream =
      new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream =
      new ObjectOutputStream(byteOutputStream);

    objectOutputStream.writeObject(original0);
    objectOutputStream.writeObject(original1);

    objectOutputStream.close();

    final ObjectInputStream objectInputStream =
      new ObjectInputStream(
        new ByteArrayInputStream(byteOutputStream.toByteArray()));

    final TestStatisticsMap received0 =
      (TestStatisticsMap)objectInputStream.readObject();

    final TestStatisticsMap received1 =
      (TestStatisticsMap)objectInputStream.readObject();

    assertEquals(original0, received0);
    assertEquals(original1, received1);

    assertEquals(2, received0.size());

    for (Pair pair : extract(received0)) {
      assertEquals(m_statistics0, pair.getStatisticsSet());
      // We get a simplified test object.
      assertEquals("", pair.getTest().getDescription());
    }
  }

  public void testTotalsMethods() throws Exception {
    final TestStatisticsMap map =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());
    map.put(m_test0, m_statistics0);
    map.put(m_test1, m_statistics0);
    map.put(m_test2, m_statistics1);

    m_statistics1.setValue(m_index, 3);
    m_statistics1.setIsComposite();

    final StatisticsSet compositeTotals = map.compositeStatisticsTotals();
    assertEquals(3, compositeTotals.getValue(m_index));

    final StatisticsSet nonCompositeTotals = map.nonCompositeStatisticsTotals();
    assertEquals(20, nonCompositeTotals.getValue(m_index));
  }

  public void testSynchronisation() throws Exception {
    final TestStatisticsMap map =
      new TestStatisticsMap(m_statisticsServices.getStatisticsSetFactory());

    final Thread[] threads = new Thread[10];
    final Boolean[] start = { Boolean.FALSE };
    final Boolean[] sucess = { Boolean.TRUE };

    for (int i=0; i<threads.length; ++i) {
      final int n = i;

      threads[i] = new Thread() {
        public void run() {
          synchronized (start) {
            while (!start[0].booleanValue()) {
              try {
                start.wait();
              }
              catch (InterruptedException e) {
                Thread.interrupted();
              }
            }
          }

          try {
            for (int j=0; j<100; ++j) {
              map.reset();
              map.put(new StubTest(n*100 + j, ""), m_statistics0);
            }
          }
          catch (Exception e) {
            e.printStackTrace();
            synchronized (sucess) {
              sucess[0] = Boolean.FALSE;
            }
          }
        }
      };
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].start();
    }

    synchronized (start) {
      start[0] = Boolean.TRUE;
      start.notifyAll();
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].join();
    }

    synchronized (sucess) {
      assertTrue(sucess[0].booleanValue());
    }

    assertEquals(1000, map.size());
  }

  private static final class Pair {
    private final Test m_test;
    private final StatisticsSet m_statisticsSet;

    public Pair(Test test, StatisticsSet statisticsSet) {
      m_test = test;
      m_statisticsSet = statisticsSet;
    }

    public Test getTest() { return m_test; }
    public StatisticsSet getStatisticsSet() { return m_statisticsSet; }
  }

  private static final List<Pair> extract(final TestStatisticsMap map) {

    final List<Pair> result = new ArrayList<Pair>();

    map.new ForEach() {
      @Override
      protected void next(Test test, StatisticsSet statistics) {
        result.add(new Pair(test, statistics));
      }
    }.iterate();

    return result;
  }
}
