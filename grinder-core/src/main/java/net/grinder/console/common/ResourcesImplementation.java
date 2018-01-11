// Copyright (C) 2000 - 2008 Philip Aston
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

package net.grinder.console.common;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;

import net.grinder.common.Closer;
import net.grinder.common.UncheckedInterruptedException;


/**
 * Type safe interface to resource bundle.
 *
 * @author Philip Aston
 */
public final class ResourcesImplementation implements Resources {

  private PrintWriter m_errorWriter = new PrintWriter(System.err, true);
  private final ResourceBundle m_resources;
  private final String m_package;

  /**
   * Constructor.
   *
   * @param bundleName Name of resource bundle. The package part of
   * the name is used to resolve the location of resources referred to
   * in the resource bundle.
   */
  public ResourcesImplementation(String bundleName) {

    m_resources = ResourceBundle.getBundle(bundleName);

    final int lastDot = bundleName.lastIndexOf(".");

    if (lastDot > 0) {
      m_package = "/" + bundleName.substring(0, lastDot + 1).replace('.', '/');
    }
    else {
      m_package = "/";
    }
  }

  /**
   * Set a writer to report warnings to.
   *
   * @param writer The writer.
   */
  public void setErrorWriter(PrintWriter writer) {
    m_errorWriter = writer;
  }

  /**
   * Overloaded version of {@link #getString(String, boolean)} which
   * writes out a waning if the resource is missing.
   * @param key The resource key.
   * @return The string.
   */
  public String getString(String key) {
    return getString(key, true);
  }

  /**
   * Use key to look up resource which names image URL. Return the image.
   * @param key The resource key.
   * @param warnIfMissing true => write out an error message if the
   * resource is missing.
   * @return The string.
   */
  public String getString(String key, boolean warnIfMissing) {

    try {
      return m_resources.getString(key);
    }
    catch (MissingResourceException e) {
      if (warnIfMissing) {
        m_errorWriter.println(
          "Warning - resource " + key + " not specified");
        return "";
      }

      return null;
    }
  }

  /**
   * Overloaded version of {@link #getImageIcon(String, boolean)}
   * which doesn't write out a waning if the resource is missing.
   *
   * @param key The resource key.
   * @return The image.
   */
  public ImageIcon getImageIcon(String key) {
    return getImageIcon(key, false);
  }

  /**
   * Use key to look up resource which names image URL. Return the image.
   *
   * @param key The resource key.
   * @param warnIfMissing true => write out an error message if the
   * resource is missing.
   * @return The image
   */
  public ImageIcon getImageIcon(String key, boolean warnIfMissing) {
    final URL resource = get(key, warnIfMissing);

    return resource != null ? new ImageIcon(resource) : null;
  }

  /**
   * Use <code>key</code> to identify a file by URL. Return contents
   * of file as a String.
   *
   * @param key Resource key used to look up URL of file.
   * @param warnIfMissing true => write out an error message if the
   * resource is missing.
   * @return Contents of file.
   */
  public String getStringFromFile(String key, boolean warnIfMissing) {

    final URL resource = get(key, warnIfMissing);

    if (resource != null) {
      Reader in = null;

      try {
        in = new InputStreamReader(resource.openStream());

        final StringWriter out = new StringWriter();

        final char[] buffer = new char[128];

        while (true) {
          final int n = in.read(buffer);

          if (n == -1) {
            break;
          }

          out.write(buffer, 0, n);
        }

        out.close();

        return out.toString();
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
        m_errorWriter.println("Warning - could not read " + resource);
      }
      finally {
        Closer.close(in);
      }
    }

    return null;
  }

  private URL get(String key, boolean warnIfMissing) {
    final String name = getString(key, warnIfMissing);

    if (name == null || name.length() == 0) {
      return null;
    }

    final URL url = this.getClass().getResource(m_package + name);

    if (url == null) {
      m_errorWriter.println("Warning - could not load resource " + name);
    }

    return url;
  }
}
