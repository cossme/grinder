// Copyright (C) 2005 - 2012 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.grinder.plugin.http.xml.HttpRecordingDocument;

import org.slf4j.Logger;

/**
 * Output an {@link HTTPRecordingImplementation} result as a script using an
 * XSLT transformation.
 *
 * @author Philip Aston
 */
public class ProcessHTTPRecordingWithXSLT implements
                                         HTTPRecordingResultProcessor {

  /**
   * Name of System property that specifies the style sheet resource to be
   * loaded from classpath. If the property is set to the empty string, the raw
   * XML will be output.
   */
  public static final String STYLESHEET_NAME_PROPERTY =
      "transformHTTPRecordingToScript";

  private final TransformerFactory m_transformerFactory = TransformerFactory
      .newInstance();

  private final InputStream m_styleSheetInputStream;

  private final PrintWriter m_output;

  private final Logger m_logger;

  private ProcessHTTPRecordingWithXSLT(InputStream styleSheetInputStream,
                                       PrintWriter output,
                                       Logger logger) {
    m_styleSheetInputStream = styleSheetInputStream;
    m_output = output;
    m_logger = logger;

    // We set our own ErrorListener because the behaviour (e.g. logging to
    // System.err) of the default ErrorListener depends on the JDK. This is not
    // perfect (JDK 1.5.0_3 still logs some things to stderr).
    m_transformerFactory.setErrorListener(new LoggingErrorListener());
  }

  /**
   * Constructor.
   *
   * @param output
   *          Where to direct the output.
   * @param logger
   *          Where to log errors.
   */
  public ProcessHTTPRecordingWithXSLT(PrintWriter output, Logger logger) {
    this(BuiltInStyleSheet.Jython, output, logger);
  }

  /**
   * Constructor.
   *
   * @param styleSheet
   *          Built in style sheet.
   * @param output
   *          Where to direct the output.
   * @param logger
   *          Where to log errors.
   */
  public ProcessHTTPRecordingWithXSLT(BuiltInStyleSheet styleSheet,
                                      PrintWriter output,
                                      Logger logger) {
    this(styleSheet.open(), output, logger);
  }

  /**
   * Constructor.
   *
   * @param styleSheet
   *          File name of an alternative style sheet.
   * @param output
   *          Where to direct the output.
   * @param logger
   *          Where to log errors.
   */
  public ProcessHTTPRecordingWithXSLT(StyleSheetFile styleSheet,
                                      PrintWriter output,
                                      Logger logger) {
    this(styleSheet.open(), output, logger);
  }

  /**
   * Produce output.
   *
   * @param result
   *          The result to process.
   * @throws IOException
   *           If an output error occurred.
   */
  public void process(HttpRecordingDocument result) throws IOException {

    try {
      final Transformer transformer = m_transformerFactory
          .newTransformer(new StreamSource(m_styleSheetInputStream));

      // One might expect this to be the default, but it's not.
      transformer.setErrorListener(m_transformerFactory.getErrorListener());

      transformer.transform(new StAXSource(result.newXMLStreamReader()),
                            new StreamResult(m_output));

      m_output.println();
    }
    catch (TransformerException e) {
      // ErrorListener will have logged.
    }
    finally {
      m_styleSheetInputStream.close();
    }
  }

  private final class LoggingErrorListener implements ErrorListener {

    private void logTransformerException(TransformerException e) {
      final StringBuilder message = new StringBuilder(e.getMessage());

      if (e.getLocationAsString() != null) {
        message.append(" at ").append(e.getLocationAsString());
      }

      m_logger.error(message.toString());
    }

    public void warning(TransformerException e) throws TransformerException {
      logTransformerException(e);
    }

    public void error(TransformerException e) throws TransformerException {
      logTransformerException(e);
      throw e;
    }

    public void fatalError(TransformerException e) throws TransformerException {
      logTransformerException(e);
      throw e;
    }
  }

  /**
   * Built in style sheets.
   */
  public enum BuiltInStyleSheet {
    /** Generate a Jython script using the old (non-DCR) instrumentation. */
    TraditionalJython("resources/httpToJythonScriptOldInstrumentation.xsl"),

    /** Generate a Jython script. */
    Jython("resources/httpToJythonScript.xsl"),

    /** Generate a Clojure script. */
    Clojure("resources/httpToClojureScript.xsl");

    private final String m_resourceName;

    private BuiltInStyleSheet(String resourceName) {
      m_resourceName = resourceName;
    }

    InputStream open() {
      return getClass().getResourceAsStream(m_resourceName);
    }
  }

  /**
   * Wrapper for an {@link InputStream} to a style sheet.
   *
   * @author Philip Aston
   */
  public static final class StyleSheetFile {
    private final InputStream m_inputStream;

    /**
     * Constructor.
     *
     * @param file
     *          The file.
     * @throws FileNotFoundException
     *           If {@code file} cannot be read.
     */
    public StyleSheetFile(File file) throws FileNotFoundException {
      m_inputStream = new FileInputStream(file);
    }

    InputStream open() {
      return m_inputStream;
    }
  }
}
