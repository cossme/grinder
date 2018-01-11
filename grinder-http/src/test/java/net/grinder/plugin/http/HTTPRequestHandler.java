// Copyright (C) 2004 - 2013 Philip Aston
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

package net.grinder.plugin.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.grinder.common.UncheckedInterruptedException;

import org.junit.Assert;

import HTTPClient.NVPair;


/**
 * Active class that accepts a connection on a socket, reads an HTTP request,
 * and returns a response. The details of the request can then be retrieved.
 */
class HTTPRequestHandler extends Assert implements Runnable {
  private static final Pattern s_contentLengthPattern;

  private final List<NVPair> m_headers = new ArrayList<NVPair>();

  static {
    try {
      s_contentLengthPattern =
        Pattern.compile("^Content-Length:[ \\t]*(.*)\\r?$",
                        Pattern.MULTILINE |
                        Pattern.CASE_INSENSITIVE);
    }
    catch (final Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final ServerSocket m_serverSocket;
  private String m_lastRequestHeaders;
  private byte[] m_lastRequestBody;
  private String m_body;
  private final AtomicBoolean m_started = new AtomicBoolean();

  private long m_responseDelay = 0;

  public HTTPRequestHandler() throws Exception {
    m_serverSocket = new ServerSocket(0);
  }

  public void start() throws InterruptedException {
    if (m_started.get()) {
      throw new AssertionError("Already started");
    }

    new Thread(this, getClass().getName()).start();

    while (!m_started.get()) {
      Thread.sleep(1);
    }
  }

  public final void shutdown() throws Exception {
    m_serverSocket.close();
  }

  public final String getURL() {
    return "http://localhost:" + m_serverSocket.getLocalPort();
  }

  public final String getLastRequestHeaders() {
    return m_lastRequestHeaders;
  }

  public final byte[] getLastRequestBody() {
    return m_lastRequestBody;
  }

  public final String getRequestFirstHeader() {
    final String text = getLastRequestHeaders();

    final int i = text.indexOf("\r\n");
    assertTrue("Has at least one line", i>=0);
    return text.substring(0, i);
  }

  public final void assertRequestContainsHeader(final String line) {
    final String text = getLastRequestHeaders();

    int start = 0;
    int i;

    while ((i = text.indexOf("\r\n", start)) != -1) {
      if (text.substring(start, i).equals(line)) {
        return;
      }

      start = i + 2;
    }

    if (text.substring(start).equals(line)) {
      return;
    }

    fail(text + " does not contain " + line);
  }

  public final void assertRequestDoesNotContainHeader(final String line) {
    final String text = getLastRequestHeaders();

    int start = 0;
    int i;

    while((i = text.indexOf("\r\n", start)) != -1) {
      assertTrue(!text.substring(start, i).equals(line));
      start = i + 2;
    }

    assertTrue(!text.substring(start).equals(line));
  }

  @Override
  public final void run() {
    try {
      m_started.set(true);

      while (true) {
        final Socket localSocket;

        try {
          localSocket = m_serverSocket.accept();
        }
        catch (final SocketException e) {
          // Socket's been closed, lets quit.
          break;
        }

        final InputStream in = localSocket.getInputStream();

        final StringBuffer headerBuffer = new StringBuffer();
        final byte[] buffer = new byte[1000];
        int n;
        int bodyStart = -1;

        READ_HEADERS:
        while ((n = in.read(buffer, 0, buffer.length)) != -1) {

          for (int i=0; i<n-3; ++i) {
            if (buffer[i] == '\r' &&
                buffer[i+1] == '\n' &&
                buffer[i+2] == '\r' &&
                buffer[i+3] == '\n') {

              headerBuffer.append(new String(buffer, 0, i));
              bodyStart = i + 4;
              break READ_HEADERS;
            }
          }

          headerBuffer.append(new String(buffer, 0, n));
        }

        if (bodyStart == -1) {
          throw new IOException("No header boundary");
        }

        m_lastRequestHeaders = headerBuffer.toString();

        final Matcher matcher =
          s_contentLengthPattern.matcher(m_lastRequestHeaders);

        if (matcher.find()) {
          final int contentLength =
            Integer.parseInt(matcher.group(1).trim());

          m_lastRequestBody = new byte[contentLength];

          int bodyBytes = n - bodyStart;

          System.arraycopy(buffer, bodyStart, m_lastRequestBody, 0,
                           bodyBytes);

          while (bodyBytes < m_lastRequestBody.length) {
            final int bytesRead =
              in.read(m_lastRequestBody, bodyBytes,
                      m_lastRequestBody.length - bodyBytes);

            if (bytesRead == -1) {
              throw new IOException("Content-length too large");
            }

            bodyBytes += bytesRead;
          }

          if (in.available() > 0) {
            throw new IOException("Content-length too small");
          }
        }
        else {
          m_lastRequestBody = null;
        }

        try {
          Thread.sleep(m_responseDelay);
        }
        catch (final InterruptedException e) {
          throw new UncheckedInterruptedException(e);
        }

        final OutputStream out = localSocket.getOutputStream();

        final StringBuffer response = new StringBuffer();
        writeHeaders(response);
        response.append("\r\n");

        if (m_body != null) {
          response.append(m_body);
        }

        out.write(response.toString().getBytes());
        out.flush();

        localSocket.close();
      }
    }
    catch (final IOException e) {
      // Ignore, it might be expected The caller will have to call start()
      // again.
    }
    finally {
      try {
        m_serverSocket.close();
      }
      catch (final IOException e) {
        // Whatever.
      }
    }
  }

  /**
   * Subclass HTTPRequestHandler to change these default headers.
   */
  protected void writeHeaders(final StringBuffer response) {
    response.append("HTTP/1.0 200 OK\r\n");

    for (final NVPair pair : m_headers) {
      response.append(pair.getName()).append(": ").append(pair.getValue());
      response.append("\r\n");
    }
  }

  public void clearHeaders() {
    m_headers.clear();
  }

  public void addHeader(final String name, final String value) {
    m_headers.add(new NVPair(name, value));
  }

  public void setBody(final String body) {
    m_body = body;
  }

  public void setResponseDelay(final long responseDelay) {
    m_responseDelay = responseDelay;
  }
}
