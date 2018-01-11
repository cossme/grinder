// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;


/**
 * Unit test case for <code>StatisticsSetFactory</code>.
 *
 * @author Philip Aston
 * @see StatisticsSet
 */
public class TestStatisticsSetFactory extends TestCase {

  public TestStatisticsSetFactory(String name) {
    super(name);
  }

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  public void testCreation() throws Exception {
    final StatisticsSetFactory factory =
      m_statisticsServices.getStatisticsSetFactory();

    assertSame(factory, m_statisticsServices.getStatisticsSetFactory());
  }

  public void testFactory() throws Exception {
    final StatisticsSetFactory factory =
      m_statisticsServices.getStatisticsSetFactory();

    final StatisticsSet statistics = factory.create();
    assertTrue(statistics instanceof StatisticsSetImplementation);
  }

  public void testSerialisation() throws Exception {
    final StatisticsSetFactory factory =
      m_statisticsServices.getStatisticsSetFactory();

    final Random random = new Random();

    final StatisticsIndexMap indexMap =
      m_statisticsServices.getStatisticsIndexMap();

    final StatisticsIndexMap.LongIndex aIndex = indexMap
        .getLongIndex("userLong0");
    final StatisticsIndexMap.LongIndex bIndex = indexMap
        .getLongIndex("userLong1");
    final StatisticsIndexMap.LongIndex cIndex = indexMap
        .getLongIndex("userLong2");

    final StatisticsSet original0 = factory.create();
    original0.addValue(aIndex, Math.abs(random.nextLong()));
    original0.addValue(bIndex, Math.abs(random.nextLong()));
    original0.addValue(cIndex, Math.abs(random.nextLong()));

    final StatisticsSet original1 = factory.create();

    final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(
      byteOutputStream);

    factory.writeStatisticsExternal(objectOutputStream,
      (StatisticsSetImplementation) original0);
    factory.writeStatisticsExternal(objectOutputStream,
      (StatisticsSetImplementation) original1);

    objectOutputStream.close();

    final ObjectInputStream objectInputStream = new ObjectInputStream(
      new ByteArrayInputStream(byteOutputStream.toByteArray()));

    final StatisticsSet received0 = factory
        .readStatisticsExternal(objectInputStream);

    final StatisticsSet received1 = factory
        .readStatisticsExternal(objectInputStream);

    assertEquals(original0, received0);
    assertEquals(original1, received1);
  }
}
