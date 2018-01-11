// Copyright (C) 2000 - 2011 Philip Aston
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

package net.grinder.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map.Entry;
import java.util.Properties;

import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.FileUtilities;
import net.grinder.testutility.Serializer;

import org.junit.Before;
import org.junit.Test;


/**
 *  Unit test case for {@link GrinderProperties}.
 *
 * @author Philip Aston
 */
public class TestGrinderProperties extends AbstractJUnit4FileTestCase {

  private final static String s_prefix = "prefix.";

  private GrinderProperties m_emptyGrinderProperties;
  private GrinderProperties m_grinderProperties;

  private final Properties m_allSet = new Properties();
  private final Properties m_prefixSet = new Properties();
  private final Properties m_stringSet = new Properties();
  private final Properties m_intSet = new Properties();
  private final Properties m_brokenIntSet = new Properties();
  private final Properties m_longSet = new Properties();
  private final Properties m_brokenLongSet = new Properties();
  private final Properties m_shortSet = new Properties();
  private final Properties m_brokenShortSet = new Properties();
  private final Properties m_doubleSet = new Properties();
  private final Properties m_brokenDoubleSet = new Properties();
  private final Properties m_booleanSet = new Properties();
  private final Properties m_brokenBooleanSet = new Properties();
  private final Properties m_fileSet = new Properties();
  private final Properties m_grinderSet = new Properties();

  private final StringWriter m_stringWriter = new StringWriter();
  private final PrintWriter m_errorWriter = new PrintWriter(m_stringWriter);

  @Before public void setUp() throws Exception {
    m_emptyGrinderProperties = new GrinderProperties();

    m_emptyGrinderProperties.setErrorWriter(m_errorWriter);

    m_prefixSet.put(s_prefix + "A string", "Some more text");
    m_prefixSet.put(s_prefix + "An int", "9");

    m_stringSet.put("A_string", "Some text");
    m_stringSet.put("Another_String", "Some text");
    m_stringSet.put("", "Some text");
    m_stringSet.put("-83*(&(*991(*&(*", "\n\r\n");
    m_stringSet.put("Another_empty_string_test", "");

    // A couple of properties that are almost in m_grinderSet.
    m_stringSet.put("grinder", ".no_dot_suffix");
    m_stringSet.put("grinder_", "blah");

    m_intSet.put("An_integer", "9");
    m_intSet.put("Number", "-9");
    m_intSet.put("AnotherNumber", "-9  ");

    m_brokenIntSet.put("Broken_int_1", "9x");
    m_brokenIntSet.put("Broken_int_2", "");
    m_brokenIntSet.put("Broken_int_3", "1234567890123456");
    m_brokenIntSet.put("Broken_int_4", "1e-3");

    m_longSet.put("A_long", "1234542222");
    m_longSet.put("Another_long", "-19");
    m_longSet.put("YetAnother_long", "  -19");

    m_brokenLongSet.put("Broken_long_1", "0x9");
    m_brokenLongSet.put("Broken_long_2", "");
    m_brokenLongSet.put("Broken_long_3", "123456789012345612321321321321");
    m_brokenLongSet.put("Broken_long_4", "10.4");

    m_shortSet.put("A_short", "123");
    m_shortSet.put("Another_short", "0");
    m_shortSet.put("OneMore_short", " 0 ");

    m_brokenShortSet.put("Broken_short_1", "0x9");
    m_brokenShortSet.put("Broken_short_2", "1.4");
    m_brokenShortSet.put("Broken_short_3", "-0123456");

    m_doubleSet.put("A_double", "1.0");
    m_doubleSet.put("Another_double", "1");
    m_doubleSet.put("Mines_a_double", "  1");

    m_brokenDoubleSet.put("Broken_double_1", "0x9");
    m_brokenDoubleSet.put("Broken_double_2", "1/0");

    m_booleanSet.put("A_boolean", "true");
    m_booleanSet.put("Another_boolean", "false");
    m_booleanSet.put("Yet_another_boolean", "yes");
    m_booleanSet.put("One_more_boolean", "no");

    m_brokenBooleanSet.put("Broken_boolean_1", "abc");
    m_brokenBooleanSet.put("Broken_boolean_2", "019321 xx");
    m_brokenBooleanSet.put("Broken_boolean_3", "uhuh");

    m_fileSet.put("A_file", "a/b");
    m_fileSet.put("Another_file", "b");

    // All properties that begin with "grinder."
    m_grinderSet.put("grinder.abc", "xyz");
    m_grinderSet.put("grinder.blah.blah", "123");

    m_allSet.putAll(m_prefixSet);
    m_allSet.putAll(m_stringSet);
    m_allSet.putAll(m_intSet);
    m_allSet.putAll(m_brokenIntSet);
    m_allSet.putAll(m_longSet);
    m_allSet.putAll(m_brokenLongSet);
    m_allSet.putAll(m_shortSet);
    m_allSet.putAll(m_brokenShortSet);
    m_allSet.putAll(m_doubleSet);
    m_allSet.putAll(m_brokenDoubleSet);
    m_allSet.putAll(m_booleanSet);
    m_allSet.putAll(m_brokenBooleanSet);
    m_allSet.putAll(m_fileSet);
    m_allSet.putAll(m_grinderSet);

    m_grinderProperties = new GrinderProperties();
    m_grinderProperties.putAll(m_allSet);
    m_grinderProperties.setErrorWriter(m_errorWriter);
  }

  @Test public void testGetPropertySubset() throws Exception {
    final GrinderProperties all =
      m_grinderProperties.getPropertySubset("");

    assertEquals(all, m_allSet);
    assertEquals(all.hashCode(), m_allSet.hashCode());

    final GrinderProperties none =
      m_grinderProperties.getPropertySubset("Not there");

    assertEquals(0, none.size());

    final GrinderProperties prefixSet =
      m_grinderProperties.getPropertySubset(s_prefix);

    assertEquals(prefixSet.size(), m_prefixSet.size());

    for (Entry<?, ?> entry : prefixSet.entrySet()) {
      final String key = (String)entry.getKey();
      final String value = (String)entry.getValue();

      assertEquals(value, m_prefixSet.get(s_prefix + key));
    }
  }

  @Test public void testGetInt() throws Exception {
    assertEquals(1, m_grinderProperties.getInt("Not there", 1));

    (new IterateOverProperties(m_intSet) {
        void match(String key, String value) throws Exception {
          assertEquals(Integer.parseInt(value.trim()),
                       m_grinderProperties.getInt(key, 99));
        }
      }
     ).run();

    assertEquals(0, countErrorLines());
  }

  private int countErrorLines() {
    final String errorOutput = m_stringWriter.toString();

    if (errorOutput.length() == 0) {
      return 0;
    }

    return errorOutput.split("\n").length;
  }

  @Test public void testGetIntBroken() throws Exception {

    (new IterateOverProperties(m_brokenIntSet) {
        void match(String key, String value) {
          assertEquals(99, m_grinderProperties.getInt(key, 99));
        }
      }
     ).run();

    assertEquals(m_brokenIntSet.size(), countErrorLines());
  }

  @Test public void testGetLong() throws Exception {
    assertEquals(1, m_grinderProperties.getLong("Not there", 1));

    (new IterateOverProperties(m_longSet) {
        void match(String key, String value) throws Exception {
          assertEquals(Long.parseLong(value.trim()),
                       m_grinderProperties.getLong(key, 99));
        }
      }
     ).run();

    assertEquals(0, countErrorLines());
  }

  @Test public void testGetLongBroken() throws Exception {

    (new IterateOverProperties(m_brokenLongSet) {
        void match(String key, String value) {
          assertEquals(99, m_grinderProperties.getLong(key, 99));
        }
      }
     ).run();

    assertEquals(m_brokenLongSet.size(), countErrorLines());
  }

  @Test public void testGetShort() throws Exception {
    assertEquals((short)1, m_grinderProperties.getShort("Not there",
                                                        (short)1));

    (new IterateOverProperties(m_shortSet) {
        void match(String key, String value) throws Exception {
          assertEquals(key,
                      Short.parseShort(value.trim()),
                       m_grinderProperties.getShort(key, (short)99));
        }
      }
     ).run();

    assertEquals(0, countErrorLines());
  }

  @Test public void testGetShortBroken() throws Exception {

    (new IterateOverProperties(m_brokenShortSet) {
        void match(String key, String value) {
          assertEquals(99, m_grinderProperties.getShort(key,
                                                        (short)99));
        }
      }
     ).run();

    assertEquals(m_brokenShortSet.size(), countErrorLines());
  }

  @Test public void testGetDouble() throws Exception {
    assertEquals(1.0, m_grinderProperties.getDouble("Not there", 1.0), 0);

    (new IterateOverProperties(m_doubleSet) {
        void match(String key, String value) throws Exception {
          assertEquals(Double.parseDouble(value),
                       m_grinderProperties.getDouble(key, 99.0), 0);
        }
      }
     ).run();

    assertEquals(0, countErrorLines());
  }

  @Test public void testGetDoubleBroken() throws Exception {

    (new IterateOverProperties(m_brokenDoubleSet) {
        void match(String key, String value) {
          assertEquals(99.0,
                       m_grinderProperties.getDouble(key, 99.0),
                       0);
        }
      }
     ).run();

    assertEquals(m_brokenDoubleSet.size(), countErrorLines());
  }

  @Test public void testGetBoolean() throws Exception {
    assertTrue(m_grinderProperties.getBoolean("Not there", true));
    assertTrue(!m_grinderProperties.getBoolean("Not there", false));

    (new IterateOverProperties(m_booleanSet) {
        void match(String key, String value) throws Exception {
          assertTrue(!(Boolean.valueOf(value).booleanValue() ^
                       m_grinderProperties.getBoolean(key, false)));
        }
      }
     ).run();

    (new IterateOverProperties(m_brokenBooleanSet) {
        void match(String key, String value) {
          // If the key exists, the boolean will always
          // parse as false.
          assertTrue(!m_grinderProperties.getBoolean(key, false));
        }
      }
     ).run();
  }

  @Test public void testGetFile() throws Exception {
    final File f = new File("foo");
    assertEquals(f, m_grinderProperties.getFile("Not there", f));

    (new IterateOverProperties(m_fileSet) {
        void match(String key, String value) throws Exception {
          assertEquals(new File(value),
                       m_grinderProperties.getFile(key, null));
        }
      }
     ).run();
  }

  @Test public void testSetInt() throws Exception {
    final GrinderProperties properties = new GrinderProperties();

    (new IterateOverProperties(m_intSet) {
        void match(String key, String value) throws Exception {
          properties.setInt(key, Integer.parseInt(value.trim()));
          assertEquals(value.trim(), properties.getProperty(key, null));
        }
      }
     ).run();
  }

  @Test public void testSetLong() throws Exception {
    final GrinderProperties properties = new GrinderProperties();

    (new IterateOverProperties(m_longSet) {
        void match(String key, String value) throws Exception {
          properties.setLong(key, Long.parseLong(value.trim()));
          assertEquals(value.trim(), properties.getProperty(key, null));
        }
      }
     ).run();
  }

  @Test public void testSetShort() throws Exception {
    final GrinderProperties properties = new GrinderProperties();

    (new IterateOverProperties(m_shortSet) {
        void match(String key, String value) throws Exception {
          properties.setShort(key, Short.parseShort(value.trim()));
          assertEquals(value.trim(), properties.getProperty(key, null));
        }
      }
     ).run();
  }

  @Test public void testSetDouble() throws Exception {
    final GrinderProperties properties = new GrinderProperties();

    (new IterateOverProperties(m_doubleSet) {
        void match(String key, String value) throws Exception {
          properties.setDouble(key, Double.parseDouble(value));
          assertEquals(Double.parseDouble(value),
                       Double.parseDouble(
                         properties.getProperty(key, null)),
                       0);
        }
      }
     ).run();
  }

  @Test public void testSetBoolean() throws Exception {
    final GrinderProperties properties = new GrinderProperties();

    (new IterateOverProperties(m_booleanSet) {
        void match(String key, String value) throws Exception {
          properties.setBoolean(key,
                                Boolean.valueOf(value).
                                booleanValue());
          assertEquals(Boolean.valueOf(value).toString(),
                       properties.getProperty(key, null));
        }
      }
     ).run();
  }

  @Test public void testSetFile() throws Exception {
    final GrinderProperties properties = new GrinderProperties();

    (new IterateOverProperties(m_fileSet) {
        void match(String key, String value) throws Exception {
          properties.setFile(key, new File(value));

          assertEquals(new File(value).getPath(),
                       properties.getProperty(key, null));
        }
      }
     ).run();
  }

  @Test public void testDefaultProperties() throws Exception {
    setSystemProperties();

    try {
      // Default constructor doesn't add system properties.
      final GrinderProperties properties = new GrinderProperties();
      assertEquals(new Properties(), properties);
    }
    finally {
      restoreSystemProperties();
    }
  }

  @Test public void testPropertiesFileHanding() throws Exception {
    setSystemProperties();

    try {
      final File file = File.createTempFile("testing", "12", getDirectory());

      final PrintWriter writer =
        new PrintWriter(new FileWriter(file), true);

      (new IterateOverProperties(m_grinderSet) {
          void match(String key, String value) throws Exception {
            writer.println(key + ":" + "should be overridden");
          }
        }
       ).run();

      (new IterateOverProperties(m_stringSet) {
          void match(String key, String value) throws Exception {
            writer.println(key + ":" + "not overridden");
          }
        }
       ).run();

      writer.close();

      // Constructor that takes a file adds system properties
      // beginning with "grinder.", and nothing else.
      final GrinderProperties properties = new GrinderProperties(file);

      (new IterateOverProperties(m_grinderSet) {
          void match(String key, String value) throws Exception {
            assertEquals(value, properties.getProperty(key, null));
            properties.remove(key);
          }
        }
       ).run();

      (new IterateOverProperties(m_stringSet) {
          void match(String key, String value) throws Exception {
            assertEquals("not overridden", properties.getProperty(key, null));
            properties.remove(key);
          }
        }
       ).run();

      // All other properties must have been picked up from other
      // System grinder.properties.
      (new IterateOverProperties(properties) {
          void match(String key, String value) throws Exception {
            assertTrue(key.startsWith("grinder."));
            assertEquals(value, System.getProperty(key));
          }
        }).run();
    }
    finally {
      restoreSystemProperties();
    }
  }

  @Test public void testSave() throws Exception {

    try {
      assertNull(m_grinderProperties.getAssociatedFile());
      m_grinderProperties.save();
      fail("Expected GrinderException as no associated file");
    }
    catch (GrinderException e) {
    }

    final GrinderProperties defaultFileProperties =
      new GrinderProperties(GrinderProperties.DEFAULT_PROPERTIES);
    assertEquals(new File("grinder.properties"),
                 defaultFileProperties.getAssociatedFile());

    final File file = File.createTempFile("testing", "123", getDirectory());

    final GrinderProperties properties = new GrinderProperties(file);
    assertEquals(file, properties.getAssociatedFile());
    properties.putAll(m_allSet);

    properties.save();

    final Properties readProperties = new Properties();
    final InputStream in = new FileInputStream(file);
    readProperties.load(in);
    in.close();

    (new IterateOverProperties(m_allSet) {
        void match(String key, String value) throws Exception {
          assertEquals(value, readProperties.getProperty(key));
        }
      }
     ).run();
  }

  @Test public void testSaveSingleProperty() throws Exception {

    try {
      assertNull(m_grinderProperties.getAssociatedFile());
      m_grinderProperties.setProperty("foo", "bah");
      m_grinderProperties.saveSingleProperty("foo");
      fail("Expected GrinderException as no associated file");
    }
    catch (GrinderException e) {
    }

    final File file = File.createTempFile("testing", "1234", getDirectory());

    final Properties plainProperties = new Properties();
    plainProperties.setProperty("existing", "property");
    final OutputStream out = new FileOutputStream(file);
    plainProperties.store(out, "");
    out.close();

    final GrinderProperties properties = new GrinderProperties(file);
    assertEquals(file, properties.getAssociatedFile());
    properties.putAll(m_allSet);

    properties.setProperty("foo", "bah");
    properties.saveSingleProperty("foo");

    properties.setBoolean("blah", true);
    properties.saveSingleProperty("blah");

    final InputStream in = new FileInputStream(file);
    plainProperties.load(in);
    in.close();

    assertEquals(3, plainProperties.size());
    assertEquals("bah", plainProperties.getProperty("foo"));
    assertEquals("property", plainProperties.getProperty("existing"));
    assertEquals("true", plainProperties.getProperty("blah"));
  }

  private void setSystemProperties() throws Exception {
    (new IterateOverProperties(m_grinderProperties) {
        void match(String key, String value) throws Exception
        {
          if (key.length() > 0) {
            System.setProperty(key, value);
          }
        }
      }
     ).run();
  }

  @Test public void testFileHandingWithBadFiles() throws Exception {
    final File readOnlyFile =
      File.createTempFile("testing", "", getDirectory());

    final GrinderProperties properties1 = new GrinderProperties(readOnlyFile);

    FileUtilities.setCanAccess(readOnlyFile, false);

    try {
      properties1.save();
      fail("Expected GrinderException");
    }
    catch (GrinderException e) {
    }

    try {
      properties1.saveSingleProperty("foo");
      fail("Expected GrinderException");
    }
    catch (GrinderException e) {
    }

    try {
      new GrinderProperties(readOnlyFile);
      fail("Expected GrinderException");
    }
    catch (GrinderException e) {
    }
  }

  private void restoreSystemProperties() {
    // Do nothing! When run under Ant, System.getProperties()
    // returns an empty object, so we can't cache/restore the old
    // properties.
  }

  private abstract class IterateOverProperties {
    private final Properties m_properties;

    IterateOverProperties(Properties properties) {
      m_properties = properties;
    }

    void run() throws Exception {
      for (Entry<?, ?> entry : m_properties.entrySet()) {
        final String key = (String)entry.getKey();
        final String value = (String)entry.getValue();

        match(key, value);
      }
    }

    abstract void match(String key, String value) throws Exception;
  }

  @Test public void testSerialisation() throws Exception {
    final GrinderProperties properties = new GrinderProperties();
    properties.setProperty("Hello", "World");

    final GrinderProperties properties2 = Serializer.serialize(properties);

    assertEquals(properties, properties2);
  }

  @Test public void testResolveRelativeFile() throws Exception {
    final File relativeDirectory = new File("d");
    final File absoluteDirectory = relativeDirectory.getCanonicalFile();
    final File absolute1 = new File(absoluteDirectory, "blah");
    final File relative1 = new File("winterlong");
    final File relative2 = new File(relative1, "i wait for you");

    final GrinderProperties properties = new GrinderProperties();

    assertNull(properties.resolveRelativeFile(null));
    assertEquals(relative1, properties.resolveRelativeFile(relative1));
    assertEquals(absolute1, properties.resolveRelativeFile(absolute1));

    properties.setAssociatedFile(new File(relativeDirectory, "my.properties"));

    assertEquals(new File(relativeDirectory, relative1.getPath()),
                 properties.resolveRelativeFile(relative1));
    assertEquals(new File(relativeDirectory, relative2.getPath()),
      properties.resolveRelativeFile(relative2));
    assertEquals(absolute1, properties.resolveRelativeFile(absolute1));
    assertNull(properties.resolveRelativeFile(null));

    properties.setAssociatedFile(new File(absoluteDirectory, "my.properties"));

    assertEquals(new File(absoluteDirectory, relative1.getPath()),
      properties.resolveRelativeFile(relative1));
    assertEquals(new File(absoluteDirectory, relative2.getPath()),
      properties.resolveRelativeFile(relative2));
    assertEquals(absolute1, properties.resolveRelativeFile(absolute1));
    assertNull(properties.resolveRelativeFile(null));
  }
}

