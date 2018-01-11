// Copyright (C) 2000 - 2012 Philip Aston
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

package net.grinder.console.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.grinder.common.GrinderProperties;
import net.grinder.communication.CommunicationDefaults;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import static net.grinder.testutility.AssertUtilities.*;
import net.grinder.testutility.FileUtilities;
import net.grinder.util.Directory;

import org.junit.Before;
import org.junit.Test;


/**
 * Unit test for {@link ConsoleProperties}.
 *
 * @author Philip Aston
 */
public class TestConsoleProperties extends AbstractJUnit4FileTestCase {

  private static final Resources s_resources = new ResourcesImplementation(
    "net.grinder.console.common.resources.Console");

  private File m_file;

  private Random m_random = new Random();

  @Before public void setup() throws Exception {
    m_file = new File(getDirectory(), "properties");
  }

  @Test public void testCollectSamples() throws Exception {

    new TestIntTemplate(ConsoleProperties.COLLECT_SAMPLES_PROPERTY, 0,
      Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
        return properties.getCollectSampleCount();
      }

      protected void set(ConsoleProperties properties, int i)
        throws ConsoleException {
        properties.setCollectSampleCount(i);
      }
    }.doTest();
  }

  @Test public void testIgnoreSamples() throws Exception {

    new TestIntTemplate(ConsoleProperties.IGNORE_SAMPLES_PROPERTY, 0,
      Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
        return properties.getIgnoreSampleCount();
      }

      protected void set(ConsoleProperties properties, int i)
        throws ConsoleException {
        properties.setIgnoreSampleCount(i);
      }
    }.doTest();
  }

  @Test public void testSampleInterval() throws Exception {

    new TestIntTemplate(ConsoleProperties.SAMPLE_INTERVAL_PROPERTY, 1,
      Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
        return properties.getSampleInterval();
      }

      protected void set(ConsoleProperties properties, int i)
        throws ConsoleException {
        properties.setSampleInterval(i);
      }
    }.doTest();
  }

  @Test public void testSignificantFigures() throws Exception {

    new TestIntTemplate(ConsoleProperties.SIG_FIG_PROPERTY, 0,
      Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
        return properties.getSignificantFigures();
      }

      protected void set(ConsoleProperties properties, int i)
        throws ConsoleException {
        properties.setSignificantFigures(i);
      }
    }.doTest();
  }

  @Test public void testConsoleHost() throws Exception {

    final String propertyName = ConsoleProperties.CONSOLE_HOST_PROPERTY;

    final String s1 = "123.1.2.3";

    writePropertyToFile(propertyName, s1);

    final ConsoleProperties properties = new ConsoleProperties(s_resources,
      m_file);

    assertEquals(s1, properties.getConsoleHost());

    final String s2 = "123.99.33.11";

    properties.setConsoleHost(s2);
    assertEquals(s2, properties.getConsoleHost());

    properties.save();

    final ConsoleProperties properties2 = new ConsoleProperties(s_resources,
      m_file);

    assertEquals(s2, properties2.getConsoleHost());

    final String s3 = "1.46.68.80";

    final PropertyChangeEvent expected = new PropertyChangeEvent(properties2,
      propertyName, s2, s3);

    final ChangeListener listener = new ChangeListener(expected);
    final ChangeListener listener2 = new ChangeListener(expected);

    properties2.addPropertyChangeListener(listener);
    properties2.addPropertyChangeListener(propertyName, listener2);

    properties2.setConsoleHost(s3);

    listener.assertCalledOnce();
    listener2.assertCalledOnce();

    try {
      properties.setConsoleHost("234.12.23.2");
      fail("Expected a DisplayMessageConsoleException for multicast address");
    }
    catch (DisplayMessageConsoleException e) {
    }

    try {
      properties.setConsoleHost("not a host");
      fail("Expected a DisplayMessageConsoleException for unknown host");
    }
    catch (DisplayMessageConsoleException e) {
    }

    properties.setConsoleHost("");
    assertEquals("", properties.getConsoleHost());
  }

  @Test public void testConsolePort() throws Exception {

    new TestIntTemplate(ConsoleProperties.CONSOLE_PORT_PROPERTY,
      CommunicationDefaults.MIN_PORT, CommunicationDefaults.MAX_PORT) {

      protected int get(ConsoleProperties properties) {
        return properties.getConsolePort();
      }

      protected void set(ConsoleProperties properties, int i)
        throws ConsoleException {
        properties.setConsolePort(i);
      }
    }.doTest();
  }

  @Test public void testHttpHost() throws Exception {

    final String propertyName = ConsoleProperties.HTTP_HOST_PROPERTY;

    final String s1 = "123.1.2.3";

    writePropertyToFile(propertyName, s1);

    final ConsoleProperties properties = new ConsoleProperties(s_resources,
      m_file);

    assertEquals(s1, properties.getHttpHost());

    final String s2 = "123.99.33.11";

    properties.setHttpHost(s2);
    assertEquals(s2, properties.getHttpHost());

    properties.save();

    final ConsoleProperties properties2 = new ConsoleProperties(s_resources,
      m_file);

    assertEquals(s2, properties2.getHttpHost());

    final String s3 = "1.46.68.80";

    final PropertyChangeEvent expected = new PropertyChangeEvent(properties2,
      propertyName, s2, s3);

    final ChangeListener listener = new ChangeListener(expected);
    final ChangeListener listener2 = new ChangeListener(expected);

    properties2.addPropertyChangeListener(listener);
    properties2.addPropertyChangeListener(propertyName, listener2);

    properties2.setHttpHost(s3);

    listener.assertCalledOnce();
    listener2.assertCalledOnce();

    try {
      properties.setHttpHost("234.12.23.2");
      fail("Expected a DisplayMessageConsoleException for multicast address");
    }
    catch (DisplayMessageConsoleException e) {
    }

    try {
      properties.setHttpHost("not a host");
      fail("Expected a DisplayMessageConsoleException for unknown host");
    }
    catch (DisplayMessageConsoleException e) {
    }

    properties.setHttpHost("");
    assertEquals("", properties.getHttpHost());
  }

  @Test public void testHttpPort() throws Exception {

    new TestIntTemplate(ConsoleProperties.HTTP_PORT_PROPERTY,
      CommunicationDefaults.MIN_PORT, CommunicationDefaults.MAX_PORT) {

      protected int get(ConsoleProperties properties) {
        return properties.getHttpPort();
      }

      protected void set(ConsoleProperties properties, int i)
        throws ConsoleException {
        properties.setHttpPort(i);
      }
    }.doTest();
  }

  @Test public void testResetConsoleWithProcesses() throws Exception {
    new TestBooleanTemplate(
      ConsoleProperties.RESET_CONSOLE_WITH_PROCESSES_PROPERTY) {

      protected boolean get(ConsoleProperties properties) {
        return properties.getResetConsoleWithProcesses();
      }

      protected void set(ConsoleProperties properties, boolean b) {
        properties.setResetConsoleWithProcesses(b);
      }

    }.doTest();
  }

  @Test public void testResetConsoleWithProcessesAsk() throws Exception {

    new TestBooleanTemplate(
      ConsoleProperties.RESET_CONSOLE_WITH_PROCESSES_ASK_PROPERTY) {

      protected boolean get(ConsoleProperties properties) {
        return properties.getResetConsoleWithProcessesAsk();
      }

      protected void set(ConsoleProperties properties, boolean b)
        throws Exception {
        properties.setResetConsoleWithProcessesAsk(b);
      }
    }.doTest();
  }

  @Test public void testSetScriptNotSetAsk() throws Exception {

    new TestBooleanTemplate(ConsoleProperties.PROPERTIES_NOT_SET_ASK_PROPERTY) {

      protected boolean get(ConsoleProperties properties) {
        return properties.getPropertiesNotSetAsk();
      }

      protected void set(ConsoleProperties properties, boolean b)
        throws Exception {
        properties.setPropertiesNotSetAsk(b);
      }
    }.doTest();
  }

  @Test public void testStartWithUnsavedBuffersAsk() throws Exception {

    new TestBooleanTemplate(
      ConsoleProperties.START_WITH_UNSAVED_BUFFERS_ASK_PROPERTY) {

      protected boolean get(ConsoleProperties properties) {
        return properties.getStartWithUnsavedBuffersAsk();
      }

      protected void set(ConsoleProperties properties, boolean b)
        throws Exception {
        properties.setStartWithUnsavedBuffersAsk(b);
      }
    }.doTest();
  }

  @Test public void testStopProcessesAsk() throws Exception {

    new TestBooleanTemplate(ConsoleProperties.STOP_PROCESSES_ASK_PROPERTY) {

      protected boolean get(ConsoleProperties properties) {
        return properties.getStopProcessesAsk();
      }

      protected void set(ConsoleProperties properties, boolean b)
        throws Exception {
        properties.setStopProcessesAsk(b);
      }
    }.doTest();
  }

  @Test public void testDistributeOnStartAsk() throws Exception {

    new TestBooleanTemplate(
      ConsoleProperties.DISTRIBUTE_ON_START_ASK_PROPERTY) {

      protected boolean get(ConsoleProperties properties) {
        return properties.getDistributeOnStartAsk();
      }

      protected void set(ConsoleProperties properties, boolean b)
        throws Exception {
        properties.setDistributeOnStartAsk(b);
      }
    }.doTest();
  }

  @Test public void testPropertiesFile() throws Exception {

    new TestFileTemplate(ConsoleProperties.PROPERTIES_FILE_PROPERTY) {

      protected File get(ConsoleProperties properties) {
        return properties.getPropertiesFile();
      }

      protected void set(ConsoleProperties properties, File file)
        throws ConsoleException {
        properties.setAndSavePropertiesFile(file);
      }
    }.doTest();

    final File file = new File(getDirectory(), "testing");

    final ConsoleProperties properties =
      new ConsoleProperties(s_resources, file);

    assertNull(properties.getPropertiesFile());

    final File propertiesFile = new File(getDirectory(), "foo");
    properties.setAndSavePropertiesFile(propertiesFile);
    assertEquals(propertiesFile, properties.getPropertiesFile());

    final ConsoleProperties properties2 =
      new ConsoleProperties(s_resources, file);
    assertEquals(propertiesFile, properties2.getPropertiesFile());

    properties.setAndSavePropertiesFile(null);
    assertNull(properties.getPropertiesFile());

    final ConsoleProperties properties3 =
      new ConsoleProperties(s_resources, file);
    assertNull(properties3.getPropertiesFile());
  }

  @Test public void testDistributionDirectory() throws Exception {

    new TestDirectoryTemplate(
      ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY) {

      protected Directory getDirectory(ConsoleProperties properties) {
        return properties.getDistributionDirectory();
      }

      protected void setDirectory(ConsoleProperties properties,
        Directory directory) throws Exception {
        properties.setAndSaveDistributionDirectory(directory);
      }
    }.doTest();

    // Check default is not null.
    final File file = new File(getDirectory(), "testing");

    final ConsoleProperties properties =
      new ConsoleProperties(s_resources, file);

    assertNotNull(properties.getDistributionDirectory());
    final Directory defaultDirectory = properties.getDistributionDirectory();

    final Directory directory = new Directory(new File("getDirectory()", "d"));
    properties.setAndSaveDistributionDirectory(directory);

    final Properties rawProperties = new Properties();
    final FileInputStream in = new FileInputStream(file);
    rawProperties.load(in);
    in.close();
    assertEquals(1, rawProperties.size());
    assertEquals(rawProperties
        .getProperty(ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY),
      directory.getFile().getPath());

    // Check load a non-directory, should give default.
    final File file2 = new File(getDirectory(), "testing2");
    properties.setAndSaveDistributionDirectory(new Directory(file2));
    assertTrue(file2.createNewFile());

    final ConsoleProperties p2 =
      new ConsoleProperties(s_resources, file);
    assertEquals(defaultDirectory, p2.getDistributionDirectory());

    FileUtilities.setCanAccess(file, false);
    try {
      properties.setAndSaveDistributionDirectory(directory);
      fail("Expected DisplayMessageConsoleException");
    }
    catch (DisplayMessageConsoleException e) {
    }
  }

  @Test public void testDistributionFileFilterPattern() throws Exception {

    new TestPatternTemplate(
      ConsoleProperties.DISTRIBUTION_FILE_FILTER_EXPRESSION_PROPERTY) {

      @Override protected Pattern get(ConsoleProperties properties) {
        return properties.getDistributionFileFilterPattern();
      }

      @Override protected String getExpression(ConsoleProperties properties) {
        return properties.getDistributionFileFilterExpression();
      }

      @Override protected void set(ConsoleProperties properties, String expression)
        throws ConsoleException {
        properties.setDistributionFileFilterExpression(expression);
      }
    }.doTest();
  }

  @Test public void testScanDistributionFilesPeriod() throws Exception {

    new TestIntTemplate(
      ConsoleProperties.SCAN_DISTRIBUTION_FILES_PERIOD_PROPERTY, 0,
      Integer.MAX_VALUE) {

      protected int get(ConsoleProperties properties) {
        return properties.getScanDistributionFilesPeriod();
      }

      protected void set(ConsoleProperties properties, int i)
        throws ConsoleException {
        properties.setScanDistributionFilesPeriod(i);
      }
    }.doTest();
  }

  @Test public void testLookAndFeel() throws Exception {

    new TestStringTemplate(ConsoleProperties.LOOK_AND_FEEL_PROPERTY, true) {

      protected String get(ConsoleProperties properties) {
        return properties.getLookAndFeel();
      }

      protected void set(ConsoleProperties properties, String name) {
        properties.setLookAndFeel(name);
      }
    }.doTest();
  }


  @Test public void testNullLookAndFeel() throws Exception {

    final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

    assertNull(properties.getLookAndFeel());

    final PropertyChangeListener listener = mock(PropertyChangeListener.class);
    properties.addPropertyChangeListener(listener);

    properties.setLookAndFeel(null);
    assertNull(properties.getLookAndFeel());
    verifyNoMoreInteractions(listener);
  }

  @Test public void testExternalEditorCommand() throws Exception {

    new TestFileTemplate(
      ConsoleProperties.EXTERNAL_EDITOR_COMMAND_PROPERTY) {

      protected File get(ConsoleProperties properties) {
        return properties.getExternalEditorCommand();
      }

      protected void set(ConsoleProperties properties, File file) {
        properties.setExternalEditorCommand(file);
      }
    }.doTest();
  }

  @Test public void testExternalEditorArguments() throws Exception {

    new TestStringTemplate(
      ConsoleProperties.EXTERNAL_EDITOR_ARGUMENTS_PROPERTY, true) {

      protected String get(ConsoleProperties properties) {
        return properties.getExternalEditorArguments();
      }

      protected void set(ConsoleProperties properties, String name) {
        properties.setExternalEditorArguments(name);
      }
    }.doTest();
  }

  @Test public void testFrameBounds() throws Exception {

    final ConsoleProperties properties =
      new ConsoleProperties(s_resources, m_file);

    assertNull(properties.getFrameBounds());

    final Rectangle rectangle = new Rectangle(12, 42, 311, 1322);

    properties.setAndSaveFrameBounds(rectangle);
    assertEquals(rectangle, properties.getFrameBounds());

    final ConsoleProperties properties2 =
      new ConsoleProperties(s_resources, m_file);

    properties.setAndSaveFrameBounds(null);
    assertNull(properties.getFrameBounds());

    assertEquals(rectangle, properties2.getFrameBounds());

    FileUtilities.setCanAccess(m_file, false);

    try {
      properties.setAndSaveFrameBounds(rectangle);
      fail("Expected DisplayMessageConsoleException");
    }
    catch (DisplayMessageConsoleException e) {
    }

    FileUtilities.setCanAccess(m_file, true);

    final GrinderProperties writeProperties =
      new GrinderProperties(m_file);
    writeProperties.setProperty(
      ConsoleProperties.FRAME_BOUNDS_PROPERTY, "1,2,3");
    writeProperties.save();

    assertNull(new ConsoleProperties(s_resources, m_file).getFrameBounds());

    writeProperties.setProperty(
      ConsoleProperties.FRAME_BOUNDS_PROPERTY, "A,2,3");
    writeProperties.save();

    assertNull(new ConsoleProperties(s_resources, m_file).getFrameBounds());

    writeProperties.setProperty(
      ConsoleProperties.FRAME_BOUNDS_PROPERTY, "1,2,3,4");
    writeProperties.save();

    assertEquals(new Rectangle(1, 2, 3, 4),
                 new ConsoleProperties(s_resources, m_file).getFrameBounds());
  }

  @Test public void testSaveTotalsWithResults() throws Exception {
    new TestBooleanTemplate(
      ConsoleProperties.SAVE_TOTALS_WITH_RESULTS_PROPERTY) {

      protected boolean get(ConsoleProperties properties) {
        return properties.getSaveTotalsWithResults();
      }

      protected void set(ConsoleProperties properties, boolean b)
        throws ConsoleException {

        properties.setSaveTotalsWithResults(b);
      }

    }.doTest();
  }

  @Test public void testCopyConstructor() throws Exception {
    final ConsoleProperties p1 = new ConsoleProperties(s_resources, m_file);
    final ConsoleProperties p2 = new ConsoleProperties(p1);

    assertEquals(p1.getCollectSampleCount(), p2.getCollectSampleCount());
    assertEquals(p1.getIgnoreSampleCount(), p2.getIgnoreSampleCount());
    assertEquals(p1.getSampleInterval(), p2.getSampleInterval());
    assertEquals(p1.getSignificantFigures(), p2.getSignificantFigures());
    assertEquals(p1.getConsoleHost(), p2.getConsoleHost());
    assertEquals(p1.getConsolePort(), p2.getConsolePort());
    assertEquals(p1.getResetConsoleWithProcesses(),
      p2.getResetConsoleWithProcesses());
    assertEquals(p1.getResetConsoleWithProcessesAsk(),
      p2.getResetConsoleWithProcessesAsk());
    assertEquals(p1.getPropertiesNotSetAsk(), p2.getPropertiesNotSetAsk());
    assertEquals(p1.getStartWithUnsavedBuffersAsk(),
      p2.getStartWithUnsavedBuffersAsk());
    assertEquals(p1.getStopProcessesAsk(), p2.getStopProcessesAsk());
    assertEquals(p1.getDistributeOnStartAsk(), p2.getDistributeOnStartAsk());
    assertEquals(p1.getPropertiesFile(), p2.getPropertiesFile());
    assertEquals(p1.getDistributionDirectory(), p2.getDistributionDirectory());
    assertEquals(p1.getDistributionFileFilterPattern().pattern(),
      p2.getDistributionFileFilterPattern().pattern());
    assertEquals(p1.getScanDistributionFilesPeriod(),
      p2.getScanDistributionFilesPeriod());
    assertEquals(p1.getLookAndFeel(), p2.getLookAndFeel());
    assertEquals(p1.getSaveTotalsWithResults(), p2.getSaveTotalsWithResults());

    p1.setCollectSampleCount(99);
    assertNotEquals(p1.getCollectSampleCount(), p2.getCollectSampleCount());
  }

  @Test public void testAssignment() throws Exception {
    final ConsoleProperties p1 = new ConsoleProperties(s_resources, m_file);
    final ConsoleProperties p2 = new ConsoleProperties(s_resources, m_file);
    p2.setCollectSampleCount(99);
    p2.setIgnoreSampleCount(99);
    p2.setSampleInterval(99);
    p2.setSignificantFigures(99);
    p2.setConsoleHost("99.99.99.99");
    p2.setConsolePort(99);
    p2.setResetConsoleWithProcesses(true);
    p2.setResetConsoleWithProcessesAsk(false);
    p2.setPropertiesNotSetAsk(false);
    p2.setStartWithUnsavedBuffersAsk(false);
    p2.setStopProcessesAsk(false);
    p2.setDistributeOnStartAsk(false);
    p2.setAndSavePropertiesFile(new File("foo"));
    p2.setAndSaveDistributionDirectory(new Directory(new File("bah")));
    p2.setDistributionFileFilterExpression(".*");
    p2.setScanDistributionFilesPeriod(100);
    p2.setLookAndFeel("something");
    p2.setExternalEditorCommand(new File("bah"));
    p2.setExternalEditorArguments("foo");
    p2.setSaveTotalsWithResults(true);

    assertTrue(p1.getCollectSampleCount() != p2.getCollectSampleCount());
    assertTrue(p1.getIgnoreSampleCount() != p2.getIgnoreSampleCount());
    assertTrue(p1.getSampleInterval() != p2.getSampleInterval());
    assertTrue(p1.getSignificantFigures() != p2.getSignificantFigures());
    assertTrue(!p1.getConsoleHost().equals(p2.getConsoleHost()));
    assertTrue(p1.getConsolePort() != p2.getConsolePort());
    assertTrue(p1.getResetConsoleWithProcesses() !=
      p2.getResetConsoleWithProcesses());
    assertTrue(p1.getResetConsoleWithProcessesAsk() !=
      p2.getResetConsoleWithProcessesAsk());
    assertTrue(p1.getPropertiesNotSetAsk() != p2.getPropertiesNotSetAsk());
    assertTrue(p1.getStartWithUnsavedBuffersAsk() !=
      p2.getStartWithUnsavedBuffersAsk());
    assertTrue(p1.getStopProcessesAsk() != p2.getStopProcessesAsk());
    assertTrue(p1.getDistributeOnStartAsk() != p2.getDistributeOnStartAsk());
    assertNotEquals(p1.getPropertiesFile(), p2.getPropertiesFile());
    assertNotEquals(p1.getDistributionDirectory(),
      p2.getDistributionDirectory());
    assertNotEquals(p1.getDistributionFileFilterPattern(),
      p2.getDistributionFileFilterPattern());
    assertTrue(p1.getScanDistributionFilesPeriod() !=
      p2.getScanDistributionFilesPeriod());
    assertNotEquals(p1.getLookAndFeel(), p2.getLookAndFeel());
    assertNotEquals(p1.getExternalEditorCommand(),
                    p2.getExternalEditorCommand());
    assertNotEquals(p1.getExternalEditorArguments(),
                    p2.getExternalEditorArguments());
    assertTrue(p1.getSaveTotalsWithResults() != p2.getSaveTotalsWithResults());

    p2.set(p1);

    assertEquals(p1.getCollectSampleCount(), p2.getCollectSampleCount());
    assertEquals(p1.getIgnoreSampleCount(), p2.getIgnoreSampleCount());
    assertEquals(p1.getSampleInterval(), p2.getSampleInterval());
    assertEquals(p1.getSignificantFigures(), p2.getSignificantFigures());
    assertEquals(p1.getConsoleHost(), p2.getConsoleHost());
    assertEquals(p1.getConsolePort(), p2.getConsolePort());
    assertTrue(p1.getResetConsoleWithProcesses() ==
      p2.getResetConsoleWithProcesses());
    assertTrue(p1.getResetConsoleWithProcessesAsk() ==
      p2.getResetConsoleWithProcessesAsk());
    assertTrue(p1.getPropertiesNotSetAsk() == p2.getPropertiesNotSetAsk());
    assertTrue(p1.getStartWithUnsavedBuffersAsk() ==
      p2.getStartWithUnsavedBuffersAsk());
    assertTrue(p1.getStopProcessesAsk() == p2.getStopProcessesAsk());
    assertTrue(p1.getDistributeOnStartAsk() == p2.getDistributeOnStartAsk());
    assertEquals(p1.getPropertiesFile(), p2.getPropertiesFile());
    assertEquals(p1.getDistributionDirectory(), p2.getDistributionDirectory());
    assertEquals(p1.getDistributionFileFilterPattern().pattern(),
                 p2.getDistributionFileFilterPattern().pattern());
    assertEquals(p1.getScanDistributionFilesPeriod(),
                 p2.getScanDistributionFilesPeriod());
    assertEquals(p1.getLookAndFeel(), p2.getLookAndFeel());
    assertEquals(p1.getExternalEditorCommand(), p2.getExternalEditorCommand());
    assertEquals(p1.getExternalEditorArguments(),
                 p2.getExternalEditorArguments());
    assertTrue(p1.getSaveTotalsWithResults() == p2.getSaveTotalsWithResults());
  }

  @Test public void testWithBadFile() throws Exception {

    final File badFile = new File(getDirectory(), "bad");
    assertTrue(badFile.createNewFile());
    FileUtilities.setCanAccess(badFile, false);

    try {
      new ConsoleProperties(s_resources, badFile);
      fail("Expected DisplayMessageConsoleException");
    }
    catch (DisplayMessageConsoleException e) {
    }

    FileUtilities.setCanAccess(badFile, true);
    final ConsoleProperties p = new ConsoleProperties(s_resources, badFile);
    FileUtilities.setCanAccess(badFile, false);

    try {
      p.save();
      fail("Expected DisplayMessageConsoleException");
    }
    catch (DisplayMessageConsoleException e) {
    }

    try {
      p.setResetConsoleWithProcessesAsk(false);
      fail("Expected DisplayMessageConsoleException");
    }
    catch (DisplayMessageConsoleException e) {
    }
  }

  private abstract class TestIntTemplate {
    private final String m_propertyName;

    private final int m_minimum;

    private final int m_maximum;

    public TestIntTemplate(String propertyName, int minimum, int maximum) {
      if (maximum <= minimum) {
        throw new IllegalArgumentException("Minimum not less than maximum");
      }

      m_propertyName = propertyName;
      m_minimum = minimum;
      m_maximum = maximum;
    }

    private int getRandomInt() {
      return getRandomInt(m_minimum, m_maximum);
    }

    private int getRandomInt(int minimum, int maximum) {
      // Valid values are in [minimum, maximum], so range is 1
      // more than maximum value.Will not fit in an int, use a
      // long.
      long range = (long) maximum + 1 - minimum;

      return (int) (minimum + Math.abs(m_random.nextLong()) % range);
    }

    public void doTest() throws Exception {
      final int i1 = getRandomInt();

      writePropertyToFile(m_propertyName, Integer.toString(i1));

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(i1, get(properties));

      final int i2 = getRandomInt();

      set(properties, i2);
      assertEquals(i2, get(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(i2, get(properties2));

      int i3;

      do {
        i3 = getRandomInt();
      }
      while (i3 == i2);

      final PropertyChangeEvent expected = new PropertyChangeEvent(properties2,
        m_propertyName, new Integer(i2), new Integer(i3));

      final ChangeListener listener = new ChangeListener(expected);
      final ChangeListener listener2 = new ChangeListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, i3);

      if (m_minimum > Integer.MIN_VALUE) {
        try {
          set(properties, m_minimum - 1);
          fail("Should not reach");
        }
        catch (DisplayMessageConsoleException e) {
        }

        try {
          set(properties, Integer.MIN_VALUE);
          fail("Should not reach");
        }
        catch (DisplayMessageConsoleException e) {
        }

        try {
          set(properties, getRandomInt(Integer.MIN_VALUE, m_minimum - 1));
          fail("Should not reach");
        }
        catch (DisplayMessageConsoleException e) {
        }
      }

      if (m_maximum < Integer.MAX_VALUE) {
        try {
          set(properties, m_maximum + 1);
          fail("Should not reach");
        }
        catch (DisplayMessageConsoleException e) {
        }

        try {
          set(properties, Integer.MAX_VALUE);
          fail("Should not reach");
        }
        catch (DisplayMessageConsoleException e) {
        }

        try {
          set(properties, getRandomInt(m_maximum + 1, Integer.MAX_VALUE));
          fail("Should not reach");
        }
        catch (DisplayMessageConsoleException e) {
        }
      }

      listener.assertCalled();
      listener2.assertCalled();
    }

    protected abstract int get(ConsoleProperties properties);

    protected abstract void set(ConsoleProperties properties, int i)
      throws ConsoleException;
  }

  private abstract class TestBooleanTemplate {
    private final String m_propertyName;

    public TestBooleanTemplate(String propertyName) {
      m_propertyName = propertyName;
    }

    public void doTest() throws Exception {

      writePropertyToFile(m_propertyName, "false");

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertTrue(!get(properties));

      set(properties, true);
      assertTrue(get(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertTrue(get(properties2));

      final PropertyChangeEvent expected = new PropertyChangeEvent(properties2,
        m_propertyName, Boolean.TRUE, Boolean.FALSE);

      final ChangeListener listener = new ChangeListener(expected);
      final ChangeListener listener2 = new ChangeListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, false);

      listener.assertCalledOnce();
      listener2.assertCalledOnce();
    }

    protected abstract boolean get(ConsoleProperties properties)
      throws Exception;

    protected abstract void set(ConsoleProperties properties, boolean b)
      throws Exception;
  }

  private String getRandomString() {
    final int length = m_random.nextInt(200);
    final char[] characters = new char[length];

    for (int i = 0; i < characters.length; ++i) {
      characters[i] = (char) (0x20 + m_random.nextInt(0x60));
    }

    return new String(characters);
  }

  private abstract class TestStringTemplate {
    private final String m_propertyName;

    private final boolean m_testNulls;

    public TestStringTemplate(String propertyName, boolean testNulls) {
      m_propertyName = propertyName;
      m_testNulls = testNulls;
    }

    public void doTest() throws Exception {
      if (m_testNulls) {
        final ConsoleProperties properties =
            new ConsoleProperties(s_resources, m_file);

        assertNull(get(properties));

        final String s = getRandomString();
        set(properties, s);
        assertNotNull(get(properties));

        set(properties, null);
        assertNull(get(properties));

        properties.save();

        final ConsoleProperties properties2 =
          new ConsoleProperties(s_resources, m_file);

        assertNull(get(properties2));
      }

      final String s1 = getRandomString();

      writePropertyToFile(m_propertyName, s1);

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(s1, get(properties));

      final String s2 = getRandomString();

      set(properties, s2);
      assertEquals(s2, get(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(s2, get(properties2));

      String s3 = getRandomString();

      do {
        s3 = getRandomString();
      }
      while (s3.equals(s2));

      final PropertyChangeEvent expected =
        new PropertyChangeEvent(properties2, m_propertyName, s2, s3);

      final ChangeListener listener = new ChangeListener(expected);
      final ChangeListener listener2 = new ChangeListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, s3);

      listener.assertCalledOnce();
      listener2.assertCalledOnce();
    }

    protected abstract String get(ConsoleProperties properties);

    protected abstract void set(ConsoleProperties properties, String i)
      throws ConsoleException;
  }

  private abstract class TestFileTemplate {

    private String m_propertyName;

    public TestFileTemplate(String propertyName) {
      m_propertyName = propertyName;
    }

    private File getRandomFile() {
      return new File(getRandomString());
    }

    public void doTest() throws Exception {

      final File f1 = getRandomFile();

      writePropertyToFile(m_propertyName, f1.getPath());

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(f1, get(properties));

      final File f2 = getRandomFile();

      set(properties, f2);
      assertEquals(f2, get(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(f2, get(properties2));

      File f3 = getRandomFile();

      do {
        f3 = getRandomFile();
      }
      while (f3.equals(f2));

      final PropertyChangeEvent expected =
        createPropertyChangeEvent(properties2, f2, f3);

      final ChangeListener listener = new ChangeListener(expected);
      final ChangeListener listener2 = new ChangeListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, f3);

      listener.assertCalledOnce();
      listener2.assertCalledOnce();
    }

    protected PropertyChangeEvent createPropertyChangeEvent(
      ConsoleProperties properties2, File f2, File f3) throws Exception {
      return new PropertyChangeEvent(properties2, getPropertyName(), f2, f3);
    }

    protected final String getPropertyName() {
      return m_propertyName;
    }

    protected abstract File get(ConsoleProperties properties);

    protected abstract void set(ConsoleProperties properties, File i)
      throws Exception;
  }

  private abstract class TestDirectoryTemplate extends TestFileTemplate {

    public TestDirectoryTemplate(String propertyName) {
      super(propertyName);
    }

    protected File get(ConsoleProperties properties) {
      return getDirectory(properties).getFile();
    }

    protected void set(ConsoleProperties properties, File i) throws Exception {
      setDirectory(properties, new Directory(i));
    }

    protected PropertyChangeEvent createPropertyChangeEvent(
      ConsoleProperties properties, File f2, File f3) throws Exception {
      return new PropertyChangeEvent(properties, getPropertyName(),
        new Directory(f2), new Directory(f3));
    }

    protected abstract Directory getDirectory(ConsoleProperties properties);

    protected abstract void setDirectory(ConsoleProperties properties,
      Directory i) throws Exception;
  }

  private abstract class TestPatternTemplate {
    private final String m_propertyName;

    public TestPatternTemplate(String propertyName) {
      m_propertyName = propertyName;
    }

    public void doTest() throws Exception {

      final String s1 = "[a-z]*";

      writePropertyToFile(m_propertyName, s1);

      final ConsoleProperties properties =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(s1, get(properties).pattern());
      assertEquals(s1, getExpression(properties));

      final String s2 = "(some|a)\\w*pattern";

      set(properties, s2);
      assertEquals(s2, get(properties).pattern());
      assertEquals(s2, getExpression(properties));

      properties.save();

      final ConsoleProperties properties2 =
        new ConsoleProperties(s_resources, m_file);

      assertEquals(s2, get(properties2).pattern());
      assertEquals(s2, getExpression(properties2));

      final String s3 = "^abc$";

      final PropertyChangeEvent expected = new PropertyChangeEvent(properties2,
        m_propertyName, s2, s3);

      final ChangeListener listener = new PatternChangeListener(expected);
      final ChangeListener listener2 = new PatternChangeListener(expected);

      properties2.addPropertyChangeListener(listener);
      properties2.addPropertyChangeListener(m_propertyName, listener2);

      set(properties2, s3);

      listener.assertCalledOnce();
      listener2.assertCalledOnce();

      set(properties, null);
      assertEquals(
        ConsoleProperties.DEFAULT_DISTRIBUTION_FILE_FILTER_EXPRESSION,
        get(properties).pattern());

      assertEquals(
        ConsoleProperties.DEFAULT_DISTRIBUTION_FILE_FILTER_EXPRESSION,
        getExpression(properties));

      try {
        set(properties, "malformed(((");
        fail("Malformed expression, expected DisplayMessageConsoleException");
      }
      catch (DisplayMessageConsoleException e) {
        assertTrue("Nested exception is a PatternSyntaxException",
          e.getCause() instanceof PatternSyntaxException);
      }

      assertEquals(
        ConsoleProperties.DEFAULT_DISTRIBUTION_FILE_FILTER_EXPRESSION,
        get(properties).pattern());
    }

    protected abstract Pattern get(ConsoleProperties properties);

    protected abstract void set(ConsoleProperties properties, String i)
      throws ConsoleException;

    protected String getExpression(ConsoleProperties properties) {
      return get(properties).pattern();
    }
  }

  private static class ChangeListener implements PropertyChangeListener {
    private final PropertyChangeEvent m_expected;

    private int m_callCount;

    ChangeListener(PropertyChangeEvent expected) {
      m_expected = expected;
    }

    public void propertyChange(PropertyChangeEvent event) {
      ++m_callCount;
      assertAreEqual(m_expected.getOldValue(), event.getOldValue());
      assertEquals(m_expected.getPropertyName(), event.getPropertyName());
    }

    public void assertCalledOnce() {
      assertEquals(1, m_callCount);
    }

    public void assertCalled() {
      assertTrue(m_callCount > 0);
    }

    public void assertAreEqual(Object expected, Object result) {
      assertEquals(expected, result);
    }
  }

  private static class PatternChangeListener extends ChangeListener {

    /**
     * For convenience, the expectedExpressions object contains old and new
     * attributes as Strings, rather than compiled Patterns.
     */
    PatternChangeListener(PropertyChangeEvent expectedExpressions) {
      super(expectedExpressions);
    }

    public void assertAreEqual(Object expected, Object result) {
      if (expected == null) {
        assertNull(result);
      }
      else {
        assertNotNull(result);
        assertEquals(expected, ((Pattern) result).pattern());
      }
    }
  }

  /**
   * Write a property key/value pair to our temporary file. Use Properties so we
   * get the correct escaping.
   */
  private final void writePropertyToFile(String name, String value)
    throws Exception {

    final FileOutputStream outputStream = new FileOutputStream(m_file);

    final Properties properties = new Properties();
    properties.setProperty(name, value);
    properties.store(outputStream, "");
    outputStream.close();
  }
}

