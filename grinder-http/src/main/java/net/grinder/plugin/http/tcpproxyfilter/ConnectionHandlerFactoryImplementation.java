// Copyright (C) 2006 - 2011 Philip Aston
// Copyright (C) 2007 Venelin Mitov
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

import org.slf4j.Logger;

import net.grinder.tools.tcpproxy.CommentSource;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.util.AttributeStringParser;
import net.grinder.util.StringEscaper;
import net.grinder.util.http.URIParser;


/**
 * Factory for {@link ConnectionHandler}s.
 *
 * @author Philip Aston
 */
public final class ConnectionHandlerFactoryImplementation
  implements ConnectionHandlerFactory {

  private final Logger m_logger;
  private final HTTPRecording m_httpRecording;
  private final RegularExpressions m_regularExpressions;
  private final URIParser m_uriParser;
  private final AttributeStringParser m_attributeStringParser;
  private final StringEscaper m_postBodyStringEscaper;
  private final CommentSource m_commentSource;

  /**
   * Constructor.
   *
   * @param httpRecording
   *          Common recording state.
   * @param logger
   *          Logger.
   * @param regularExpressions
   *          Compiled regular expressions.
   * @param uriParser
   *          A URI parser.
   * @param attributeStringParser
   *          An AttributeStringParser.
   * @param postBodyStringEscaper
   *          A StringCodec used to escape post body strings.
   * @param commentSource
   *          A CommentSource containing comments inserted by the users
   *          during capture.
   */
  public ConnectionHandlerFactoryImplementation(
    HTTPRecording httpRecording,
    Logger logger,
    RegularExpressions regularExpressions,
    URIParser uriParser,
    AttributeStringParser attributeStringParser,
    StringEscaper postBodyStringEscaper,
    CommentSource commentSource) {

    m_logger = logger;
    m_httpRecording = httpRecording;
    m_regularExpressions = regularExpressions;
    m_uriParser = uriParser;
    m_attributeStringParser = attributeStringParser;
    m_postBodyStringEscaper = postBodyStringEscaper;
    m_commentSource = commentSource;
  }

  /**
   * Factory method.
   *
   * @param connectionDetails Connection details.
   * @return A new ConnectionHandler.
   */
  public ConnectionHandler create(ConnectionDetails connectionDetails) {
    return new ConnectionHandlerImplementation(m_httpRecording,
                                               m_logger,
                                               m_regularExpressions,
                                               m_uriParser,
                                               m_attributeStringParser,
                                               m_postBodyStringEscaper,
                                               m_commentSource,
                                               connectionDetails);
  }
}
