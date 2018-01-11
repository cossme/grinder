// Copyright (C) 2000 Paco Gomez
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

package net.grinder.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;



/**
 * Extend {@link java.util.Properties} to add type safe accessors.
 * Has an optional associated file.
 *
 * @author Philip Aston
 * @see net.grinder.script.Grinder.ScriptContext#getProperties
 */
public class GrinderProperties extends Properties {
  private static final long serialVersionUID = 1;

  /**
   * Key to use for the script property.
   */
  public static final String SCRIPT = "grinder.script";

  /**
   * Key to use for the log directory property.
   */
  public static final String LOG_DIRECTORY = "grinder.logDirectory";

  /**
   * Key to use for the console host property.
   */
  public static final String CONSOLE_HOST = "grinder.consoleHost";

  /**
   * Key to use for the console host property.
   */
  public static final String CONSOLE_PORT = "grinder.consolePort";

  /**
   * Default file name for properties.
   */
  public static final File DEFAULT_PROPERTIES = new File("grinder.properties");

  /**
   * Default script file name.
   */
  public static final File DEFAULT_SCRIPT = new File("grinder.py");

  private transient PrintWriter m_errorWriter =
    new PrintWriter(System.err, true);

  /** @serial Associated file. */
  private File m_file;

  /**
   * Construct an empty GrinderProperties with no associated file.
   */
  public GrinderProperties() {
    m_file = null;
  }

  /**
   * Construct a GrinderProperties, reading initial values from the
   * specified file. System properties beginning with
   * "<code>grinder.</code>"are also added to allow values to be
   * overridden on the command line.
   * @param file The file to read the properties from.
   *
   * @exception PersistenceException If an error occurs reading from
   * the file.
   * @see #DEFAULT_PROPERTIES
   */
  public GrinderProperties(File file) throws PersistenceException {
    setAssociatedFile(file);

    if (m_file.exists()) {
      InputStream propertiesInputStream = null;
      try {
        propertiesInputStream = new FileInputStream(m_file);
        load(propertiesInputStream);
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
        throw new PersistenceException(
          "Error loading properties file '" + m_file.getPath() + '\'', e);
      }
      finally {
        Closer.close(propertiesInputStream);
      }
    }

    final Enumeration<?> systemProperties =
      System.getProperties().propertyNames();

    while (systemProperties.hasMoreElements()) {
      final String name = (String)systemProperties.nextElement();

      if (name.startsWith("grinder.")) {
        put(name, System.getProperty(name));
      }
    }
  }

  /**
   * Get the associated file.
   *
   * @return The associated file, or <code>null</code> if none.
   */
  public final File getAssociatedFile() {
    return m_file;
  }

  /**
   * Set the associated file. Does not cause the properties to be re-read from
   * the new file.
   *
   * @param file
   *            The associated file, or <code>null</code> if none.
   */
  public final void setAssociatedFile(File file) {
    m_file = file;
  }

  /**
   * Save our properties to our file.
   *
   * @exception PersistenceException If there is no file associated
   * with this {@link GrinderProperties} or an I/O exception occurs..
   */
  public final void save() throws PersistenceException {

    if (m_file == null) {
      throw new PersistenceException("No associated file");
    }

    OutputStream outputStream = null;

    try {
      outputStream = new FileOutputStream(m_file);
      store(outputStream, generateFileHeader());
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new PersistenceException(
        "Error writing properties file '" + m_file.getPath() + '\'', e);
    }
    finally {
      Closer.close(outputStream);
    }
  }

  /**
   * Save a single property to our file.
   *
   * @param name Property name.
   * @exception PersistenceException If there is no file associated with this
   * {@link GrinderProperties} or an I/O exception occurs.
   */
  public final void saveSingleProperty(String name)
    throws PersistenceException {

    if (m_file == null) {
      throw new PersistenceException("No associated file");
    }

    try {
      final Properties properties = new Properties();

      InputStream inputStream = null;

      try {
        inputStream = new FileInputStream(m_file);
        properties.load(inputStream);
      }
      catch (IOException e) {
        // Can't read the file, maybe its not there. Ignore.
        UncheckedInterruptedException.ioException(e);
      }
      finally {
        Closer.close(inputStream);
      }

      OutputStream outputStream = null;

      try {
        outputStream = new FileOutputStream(m_file);

        final String value = getProperty(name);

        if (value != null) {
          properties.setProperty(name, value);
        }
        else {
          properties.remove(name);
        }

        properties.store(outputStream, generateFileHeader());
      }
      finally {
        Closer.close(outputStream);
      }
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new PersistenceException(
        "Error writing properties file '" + m_file.getPath() + '\'', e);
    }
  }

  private String generateFileHeader() {
    return "Properties file for The Grinder";
  }

  /**
   * Set a writer to report warnings to.
   *
   * @param writer The writer.
   */
  public final void setErrorWriter(PrintWriter writer) {
    m_errorWriter = writer;
  }

  /**
   * Return a new GrinderProperties that contains the subset of our
   * Properties which begin with the specified prefix.
   * @param prefix The prefix.
   *
   * @return The subset.
   */
  public final synchronized GrinderProperties
    getPropertySubset(String prefix) {
    final GrinderProperties result = new GrinderProperties();

    final Enumeration<?> propertyNames = propertyNames();

    while (propertyNames.hasMoreElements()) {
      final String name = (String)propertyNames.nextElement();

      if (name.startsWith(prefix)) {
        result.setProperty(name.substring(prefix.length()),
                           getProperty(name));
      }
    }

    return result;
  }

  /**
   * Get the value of the property with the given name, return the
   * value as an <code>int</code>.
   * @param propertyName The property name.
   * @param defaultValue The value to return if a property with the
   * given name does not exist or is not an integer.
   *
   * @return The value.
   */
  public final int getInt(String propertyName, int defaultValue) {
    final String s = getProperty(propertyName);

    if (s != null) {
      try {
        return Integer.parseInt(s.trim());
      }
      catch (NumberFormatException e) {
        m_errorWriter.println("Warning, property '" + propertyName +
                              "' does not specify an integer value");
      }
    }

    return defaultValue;
  }

  /**
   * Set the property with the given name to an <code>int</code>
   * value.
   * @param propertyName The property name.
   * @param value The value to set.
   */
  public final void setInt(String propertyName, int value) {
    setProperty(propertyName, Integer.toString(value));
  }


  /**
   * Get the value of the property with the given name, return the
   * value as a <code>long</code>.
   * @param propertyName The property name.
   * @param defaultValue The value to return if a property with the
   * given name does not exist or is not a long.
   *
   * @return The value.
   */
  public final long getLong(String propertyName, long defaultValue) {
    final String s = getProperty(propertyName);

    if (s != null) {
      try {
        return Long.parseLong(s.trim());
      }
      catch (NumberFormatException e) {
        m_errorWriter.println("Warning, property '" + propertyName +
                              "' does not specify an integer value");
      }
    }

    return defaultValue;
  }

  /**
   * Set the property with the given name to a <code>long</code>
   * value.
   * @param propertyName The property name.
   * @param value The value to set.
   */
  public final void setLong(String propertyName, long value) {
    setProperty(propertyName, Long.toString(value));
  }

  /**
   * Get the value of the property with the given name, return the
   * value as a <code>short</code>.
   * @param propertyName The property name.
   * @param defaultValue The value to return if a property with the
   * given name does not exist or is not a short.
   *
   * @return The value.
   */
  public final short getShort(String propertyName, short defaultValue) {
    final String s = getProperty(propertyName);

    if (s != null) {
      try {
        return Short.parseShort(s.trim());
      }
      catch (NumberFormatException e) {
        m_errorWriter.println("Warning, property '" + propertyName +
                              "' does not specify a short value");
      }
    }

    return defaultValue;
  }

  /**
   * Set the property with the given name to a <code>short</code>
   * value.
   * @param propertyName The property name.
   * @param value The value to set.
   */
  public final void setShort(String propertyName, short value) {
    setProperty(propertyName, Short.toString(value));
  }

  /**
   * Get the value of the property with the given name, return the
   * value as a <code>double</code>.
   * @param propertyName The property name.
   * @param defaultValue The value to return if a property with the
   * given name does not exist or is not a double.
   *
   * @return The value.
   */
  public final double getDouble(String propertyName, double defaultValue) {
    final String s = getProperty(propertyName);

    if (s != null) {
      try {
        return Double.parseDouble(s.trim());
      }
      catch (NumberFormatException e) {
        m_errorWriter.println("Warning, property '" + propertyName +
                              "' does not specify a double value");
      }
    }

    return defaultValue;
  }

  /**
   * Set the property with the given name to a <code>double</code>
   * value.
   * @param propertyName The property name.
   * @param value The value to set.
   */
  public final void setDouble(String propertyName, double value) {
    setProperty(propertyName, Double.toString(value));
  }

  /**
   * Get the value of the property with the given name, return the
   * value as a <code>boolean</code>.
   * @param propertyName The property name.
   * @param defaultValue The value to return if a property with the
   * given name does not exist.
   *
   * @return The value.
   */
  public final boolean getBoolean(String propertyName, boolean defaultValue) {
    final String s = getProperty(propertyName);

    if (s != null) {
      return Boolean.valueOf(s).booleanValue();
    }

    return defaultValue;
  }

  /**
   * Set the property with the given name to a <code>boolean</code>
   * value.
   * @param propertyName The property name.
   * @param value The value to set.
   */
  public final void setBoolean(String propertyName, boolean value) {
    setProperty(propertyName, String.valueOf(value));
  }

  /**
   * Get the value of the property with the given name, return the value as a
   * <code>File</code>.
   *
   * @param propertyName
   *            The property name.
   * @param defaultValue
   *            The value to return if a property with the given name does not
   *            exist.
   * @return The value.
   */
  public final File getFile(String propertyName, File defaultValue) {
    final String s = getProperty(propertyName);

    if (s != null) {
      return new File(s);
    }

    return defaultValue;
  }

  /**
   * Returns a {@link File} representing the combined path of our associated
   * property file directory and the passed <code>file</code>.
   *
   * <p>
   * If <code>file</code> is absolute, or this <code>GrinderProperties</code>
   * has no associated property file, return <code>file</code>.
   * </p>
   *
   * @param file
   *            The file.
   * @return If possible, <code>file</code> resolved relative to the directory
   *         of our associated properties file, or <code>file</code>.
   */
  public final File resolveRelativeFile(File file) {

    if (m_file != null && file != null && !file.isAbsolute()) {
      return new File(m_file.getParentFile(), file.getPath());
    }

    return file;
  }

  /**
   * Set the property with the given name to a <code>File</code>
   * value.
   * @param propertyName The property name.
   * @param value The value to set.
   */
  public final void setFile(String propertyName, File value) {
    setProperty(propertyName, value.getPath());
  }

  /**
   * Exception indicating a problem in persisting properties.
   */
  public static final class PersistenceException extends GrinderException {
    private PersistenceException(String message) {
      super(message);
    }

    private PersistenceException(String message, Throwable t) {
      super(message, t);
    }
  }

  /**
   * Override to restore error writer to <code>System.err</code>.
   */
  private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    setErrorWriter(new PrintWriter(System.err, true));
  }
}
