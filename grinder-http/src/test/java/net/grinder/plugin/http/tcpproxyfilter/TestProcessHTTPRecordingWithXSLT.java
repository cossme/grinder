// Copyright (C) 2005 - 2011 Philip Aston
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

import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT.BuiltInStyleSheet;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT.StyleSheetFile;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.ObjectFactory;
import net.grinder.testutility.AbstractJUnit4FileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RedirectStandardStreams;
import net.grinder.util.StreamCopier;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

/**
 * Unit tests for {@link ProcessHTTPRecordingWithXSLT}.
 *
 * @author Philip Aston
 */
public class TestProcessHTTPRecordingWithXSLT extends AbstractJUnit4FileTestCase {

  @Mock
  private Logger m_logger;
  private StringWriter m_stringOut = new StringWriter();
  private PrintWriter m_out = new PrintWriter(m_stringOut);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testWithIdentityTransform() throws Exception {

    final StreamCopier streamCopier = new StreamCopier(4096, true);

    final InputStream identityStyleSheetStream = getClass().getResourceAsStream("resources/identity.xsl");

    final File identityStyleSheetFile = new File(getDirectory(), "identity.xsl");

    streamCopier.copy(identityStyleSheetStream, new FileOutputStream(identityStyleSheetFile));

    final ProcessHTTPRecordingWithXSLT processor = new ProcessHTTPRecordingWithXSLT(
        new StyleSheetFile(identityStyleSheetFile), m_out, m_logger);

    final HTTPRecordingType recording = new HTTPRecordingType();
    HTTPRecordingType.Metadata metadata = new HTTPRecordingType.Metadata();
    metadata.setVersion("blah");
    recording.setMetadata(metadata);

    processor.process(recording);

    final String output2 = m_stringOut.toString();
    AssertUtilities.assertContainsPattern(output2,
        "^<\\?xml version=.*\\?>\\s*" + "<http-recording .*?>\\s*" + "<metadata>\\s*" + "<version>blah</version>\\s*"
            + "<test-number-offset>0</test-number-offset>\\s*" + "</metadata>\\s*" + "</http-recording>\\s*$");

    verifyNoMoreInteractions(m_logger);
  }

  @Test
  @Ignore
  public void testWithStandardTransform() throws Exception {
    final ProcessHTTPRecordingWithXSLT processor = new ProcessHTTPRecordingWithXSLT(m_out, m_logger);

    final HTTPRecordingType recording = new HTTPRecordingType();
    HTTPRecordingType.Metadata metadata = new HTTPRecordingType.Metadata();
    metadata.setVersion("blah");
    recording.setMetadata(metadata);

    // Will fail with an un-parseable date TransformerException
    processor.process(recording);

    final String output = m_stringOut.toString();
    AssertUtilities.assertContains(output, "# blah");

    verify(m_logger).error(contains("Unparseable date"));

    // This time it will work.
    metadata.setTime(getTime());

    final ProcessHTTPRecordingWithXSLT processor2 = new ProcessHTTPRecordingWithXSLT(m_out, m_logger);

    processor2.process(recording);

    verifyNoMoreInteractions(m_logger);
  }

  private XMLGregorianCalendar getTime() {
    try {
      DatatypeFactory dtf = DatatypeFactory.newInstance();
      return dtf.newXMLGregorianCalendar(
              Calendar.getInstance().get(Calendar.YEAR),
              Calendar.getInstance().get(Calendar.MONTH) + 1,
              Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
              Calendar.getInstance().get(Calendar.HOUR),
              Calendar.getInstance().get(Calendar.MINUTE),
              Calendar.getInstance().get(Calendar.SECOND),
              Calendar.getInstance().get(Calendar.MILLISECOND),
              Calendar.getInstance().get(Calendar.ZONE_OFFSET) / (1000 * 60));
    } catch (DatatypeConfigurationException e) {
      m_logger.error(e.getMessage());
      return null;
    }
  }

  @Test
  @Ignore
  public void testWithClojureTransform() throws Exception {
    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(BuiltInStyleSheet.Clojure,
                                       m_out,
                                       m_logger);

    final HTTPRecordingType recording = new HTTPRecordingType();
    HTTPRecordingType.Metadata metadata = new HTTPRecordingType.Metadata();
    metadata.setVersion("blah");
    metadata.setTime(getTime());
    recording.setMetadata(metadata);

    processor.process(recording);
    verifyNoMoreInteractions(m_logger);

    AssertUtilities.assertContains(m_stringOut.toString(), ";; blah");
  }

  @Test 
  @Ignore
   public void testWithBadTransform() throws Exception {
    final File badStyleSheetFile = new File(getDirectory(), "bad.xsl");
    badStyleSheetFile.createNewFile();

    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(
        new StyleSheetFile(badStyleSheetFile),
        m_out,
        m_logger);

    final HTTPRecordingType emptyDocument = new HTTPRecordingType();

    // Redirect streams, because XSLTC still chucks some stuff out to stderr.
    new RedirectStandardStreams() {
      protected void runWithRedirectedStreams() throws Exception {
        processor.process(emptyDocument);
    }}.run();

    verify(m_logger, atLeastOnce()).error(isA(String.class));

    // Processor might log multiple messages; ignore.
    // m_loggerStubFactory.assertNoMoreCalls();
  }
}
