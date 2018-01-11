// Copyright (C) 2008 - 2013 Philip Aston
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Timer;

import net.grinder.common.StubTest;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelImplementation;
import net.grinder.console.model.SampleModelViews;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.TestStatisticsQueries;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.StubTimer;
import net.grinder.translation.Translations;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Unit tests for {@link CumulativeStatisticsTableModel}.
 *
 * @author Philip Aston
 */
public class TestSampleStatisticsTableModel extends AbstractJUnit4FileTestCase {

  @Mock
  private Translations m_translations;

  private File m_file;

  @Before
  public void setUp() throws Exception {
    initMocks(this);

    m_file = new File(getDirectory(), "properties");

    when(m_translations.translate("console.term/test"))
        .thenReturn("t3st");

    when(m_translations.translate("console.term/test-description"))
        .thenReturn("Test Description Column");

    when(m_translations.translate("console.term/total"))
        .thenReturn("Total Label");
  }

  public static class NullSwingDispatcherFactory
    implements SwingDispatcherFactory {

    @Override
    public <T> T create(Class<T> clazz, T delegate) {
      return delegate;
    }
  }

  private final SwingDispatcherFactory m_swingDispatcherFactoryDelegate =
      new NullSwingDispatcherFactory();

  private final DelegatingStubFactory<SwingDispatcherFactory> m_swingDispatcherFactoryStubFactory =
      DelegatingStubFactory.create(m_swingDispatcherFactoryDelegate);

  private final SwingDispatcherFactory m_swingDispatcherFactory =
      m_swingDispatcherFactoryStubFactory.getStub();

  private final RandomStubFactory<SampleModel> m_sampleModelStubFactory =
      RandomStubFactory.create(SampleModel.class);

  private final SampleModel m_sampleModel =
      m_sampleModelStubFactory.getStub();

  private final RandomStubFactory<SampleModelViews> m_sampleModelViewsStubFactory =
      RandomStubFactory.create(SampleModelViews.class);

  private final SampleModelViews m_sampleModelViews =
      m_sampleModelViewsStubFactory.getStub();

  private final StatisticsServices m_statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

  private final TestStatisticsQueries m_testStatisticsQueries =
      m_statisticsServices.getTestStatisticsQueries();

  {
    m_sampleModelViewsStubFactory.setResult("getIntervalStatisticsView",
      m_statisticsServices.getSummaryStatisticsView());
    m_sampleModelViewsStubFactory.setResult("getTestStatisticsQueries",
      m_testStatisticsQueries);
    m_sampleModelViewsStubFactory.setResult("getNumberFormat",
      new DecimalFormat("0.0"));

    m_sampleModelStubFactory.setResult("getTotalCumulativeStatistics",
      m_statisticsServices.getStatisticsSetFactory().create());
  }

  @Test
  public void testConstruction() throws Exception {
    final SampleStatisticsTableModel model =
        new SampleStatisticsTableModel(m_sampleModel,
          m_sampleModelViews,
          m_translations,
          m_swingDispatcherFactory);

    // The dispatcher factory is used a couple of times to wrap
    // listeners.
    m_swingDispatcherFactoryStubFactory.assertSuccess("create",
      Class.class,
      Object.class);

    m_swingDispatcherFactoryStubFactory.assertSuccess("create",
      Class.class,
      Object.class);
    m_swingDispatcherFactoryStubFactory.assertNoMoreCalls();

    assertSame(m_sampleModel, model.getModel());
    assertSame(m_sampleModelViews, model.getModelViews());

    assertEquals(7, model.getColumnCount());
    assertEquals(0, model.getRowCount());
    assertEquals(0, model.getLastModelTestIndex().getNumberOfTests());

    assertEquals("t3st", model.getColumnName(0));
    assertEquals("Test Description Column", model.getColumnName(1));
    assertEquals("Errors", model.getColumnName(3));
  }

  @Test
  public void testDefaultWrite() throws Exception {
    final SampleStatisticsTableModel model =
        new SampleStatisticsTableModel(m_sampleModel,
          m_sampleModelViews,
          m_translations,
          m_swingDispatcherFactory);

    final StringWriter writer = new StringWriter();

    model.write(writer, "::", "**");

    assertEquals(
      "t3st::Test Description Column::Tests::Errors::Mean Test Time (ms)::Test Time Standard Deviation (ms)::TPS::**",
      writer.toString());
  }

  @Test
  public void testAddColumns() throws Exception {
    final SampleStatisticsTableModel model =
        new SampleStatisticsTableModel(m_sampleModel,
          m_sampleModelViews,
          m_translations,
          m_swingDispatcherFactory);

    when(m_translations.translate("console.statistic/Errors"))
        .thenReturn("Blah");
    when(m_translations.translate("console.statistic/Mean-Test-Time-ms"))
        .thenReturn("meantime");

    assertEquals(7, model.getColumnCount());

    model.addColumns(m_statisticsServices.getSummaryStatisticsView());

    // Adding same columns again is a no-op.
    assertEquals(7, model.getColumnCount());
    assertEquals("Tests", model.getColumnName(2));
    assertEquals("Errors", model.getColumnName(3));
    assertEquals("Mean Test Time (ms)", model.getColumnName(4));

    model.addColumns(m_statisticsServices.getDetailStatisticsView());

    assertEquals(8, model.getColumnCount());
    assertEquals("Test time", model.getColumnName(2));
    assertEquals("Blah", model.getColumnName(4));
    assertEquals("meantime", model.getColumnName(5));
  }

  @Test
  public void testWithData() throws Exception {
    final Timer timer = new StubTimer();

    final SampleModelImplementation sampleModelImplementation =
        new SampleModelImplementation(
          new ConsoleProperties(m_translations, m_file),
          m_statisticsServices,
          timer,
          m_translations,
          null);

    final SampleStatisticsTableModel model =
        new SampleStatisticsTableModel(sampleModelImplementation,
          m_sampleModelViews,
          m_translations,
          m_swingDispatcherFactory);

    model.newTests(null, new ModelTestIndex());

    assertEquals(0, model.getRowCount());

    final net.grinder.common.Test[] tests = {
                                             new StubTest(1, "test 1"),
                                             new StubTest(2, "test 2"),
    };

    sampleModelImplementation.registerTests(Arrays.asList(tests));

    assertEquals(2, model.getRowCount());
    assertNull(model.getForeground(0, 0));
    assertNull(model.getBackground(0, 0));
    assertEquals("t3st 1", model.getValueAt(0, 0));
    assertEquals("test 1", model.getValueAt(0, 1));
    assertEquals("0", model.getValueAt(0, 3));
    assertNull(model.getForeground(0, 3));
  }
}
