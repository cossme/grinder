<?xml version="1.0" encoding="UTF-8"?>

<!--
 Copyright (C) 2006 - 2012 Philip Aston
 Copyright (C) 2007 Venelin Mitov
 All rights reserved.

 This file is part of The Grinder software distribution. Refer to
 the file LICENSE which is part of The Grinder distribution for
 licensing details. The Grinder distribution is available on the
 Internet at http:grinder.sourceforge.net/

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.
-->


<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
  xmlns:g="http://grinder.sourceforge.net/tcpproxy/http/1.0"
  xmlns:helper="net.grinder.plugin.http.tcpproxyfilter.XSLTHelper">

  <xsl:output method="text"/>
  <xsl:strip-space elements="*"/>

  <xsl:variable name="testNumberOffset"
             select="/g:http-recording/g:metadata/g:test-number-offset"/>


  <xsl:template match="g:http-recording">
    <xsl:value-of select="helper:resetIndent()"/>
    <xsl:text># </xsl:text>
    <xsl:value-of select="g:metadata/g:version"/>

    <xsl:text>
# HTTP script recorded by TCPProxy at </xsl:text>
    <xsl:value-of select="helper:formatTime(g:metadata/g:time)"/>

    <xsl:text>

from net.grinder.script import Test
from net.grinder.script.Grinder import grinder
from net.grinder.plugin.http import HTTPPluginControl, HTTPRequest
from HTTPClient import NVPair
connectionDefaults = HTTPPluginControl.getConnectionDefaults()
httpUtilities = HTTPPluginControl.getHTTPUtilities()

# To use a proxy server, uncomment the next line and set the host and port.
# connectionDefaults.setProxyServer("localhost", 8001)

# These definitions at the top level of the file are evaluated once,
# when the worker process is started.
</xsl:text>

    <xsl:apply-templates select="*" mode="file"/>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>class TestRunner:</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""A TestRunner instance is created for each worker thread."""</xsl:text>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:apply-templates select="*" mode="TestRunner"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>def __call__(self):</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""Called for every run performed by the worker thread."""</xsl:text>

    <xsl:apply-templates select="*" mode="__call__"/>

    <xsl:if test="not(//g:request)">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># Empty recording!</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text>pass</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLine()"/>

    <xsl:value-of select="helper:changeIndent(-2)"/>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:text>def instrumentMethod(test, method_name, c=TestRunner):</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""Instrument a method with the given Test."""</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>unadorned = getattr(c, method_name)</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>import new</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>method = new.instancemethod(test.wrap(unadorned), None, c)</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>setattr(c, method_name, method)</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:apply-templates select="*" mode="instrumentMethod"/>
    <xsl:value-of select="helper:newLine()"/>

  </xsl:template>


  <xsl:template match="g:base-uri" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="@uri-id"/>
    <xsl:text> = '</xsl:text>
    <xsl:value-of select="concat(g:scheme, '://', g:host, ':', g:port)"/>
    <xsl:text>'</xsl:text>

    <xsl:if test="not(following::g:base-uri)">
      <xsl:value-of select="helper:newLine()"/>
    </xsl:if>
  </xsl:template>


  <xsl:template match="g:common-headers[@headers-id='defaultHeaders']" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>connectionDefaults.defaultHeaders = \</xsl:text>
    <xsl:call-template name="list"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:common-headers" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="@headers-id"/>
    <xsl:text>= \</xsl:text>
    <xsl:call-template name="list"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:request" mode="generate-test-number">
    <!--  Numbers sequentially follow the requests' page test number. -->

    <xsl:variable name="request-number"
                  select="count(preceding-sibling::g:request) + 1 + $testNumberOffset"/>

    <xsl:variable name="page-test-number">
      <xsl:apply-templates select=".." mode="generate-test-number"/>
    </xsl:variable>

    <xsl:value-of select="$page-test-number + $request-number"/>
  </xsl:template>


  <xsl:template match="g:request" mode="file">
    <xsl:variable name="request-number">
      <xsl:apply-templates select ="." mode="generate-test-number"/>
    </xsl:variable>
    <xsl:variable name="request-name" select="concat('request', $request-number)"/>

    <xsl:if test="not(preceding::g:request)">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># Create an HTTPRequest for each request, then replace the</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># reference to the HTTPRequest with an instrumented version.</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># You can access the unadorned instance using </xsl:text>
      <xsl:value-of select="$request-name"/>
      <xsl:text>.__target__.</xsl:text>
    </xsl:if>

    <xsl:for-each select="g:comment">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># </xsl:text>
      <xsl:value-of select="."/>
    </xsl:for-each>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="$request-name"/>

    <xsl:value-of select="concat(' = HTTPRequest(url=', g:uri/@extends)"/>

    <xsl:if test="g:headers/@extends[. != 'defaultHeaders']">
      <xsl:text>, headers=</xsl:text>
      <xsl:value-of select="g:headers/@extends"/>
    </xsl:if>

    <xsl:text>)</xsl:text>

    <xsl:if test="g:body/g:file">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:value-of select="$request-name"/>
      <xsl:text>.setDataFromFile('</xsl:text>
      <xsl:value-of select="g:body/g:file"/>
      <xsl:text>')</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="concat($request-name, ' = Test(')"/>
    <xsl:value-of select="concat($request-number, ', ')"/>
    <xsl:value-of select="helper:quoteForPython(g:description)"/>
    <xsl:value-of select="concat(').wrap(', $request-name, ')')"/>

    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:request[position() = 1 and position() = last()]" mode="page-description">
    <xsl:value-of select="g:description"/>
  </xsl:template>

  <xsl:template match="g:request[position() = 1]" mode="page-description">
    <xsl:value-of select="g:description"/>

    <xsl:variable name="request-number">
      <xsl:apply-templates select ="." mode="generate-test-number"/>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="position() = last()">
        <xsl:text> (request </xsl:text>
        <xsl:value-of select="$request-number"/>
        <xsl:text>)</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text> (requests </xsl:text>
        <xsl:value-of select="$request-number"/>
        <xsl:text>-</xsl:text>
        <xsl:apply-templates select ="following-sibling::g:request[position()=last()]" mode="generate-test-number"/>
        <xsl:text>)</xsl:text>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>


  <xsl:template match="g:request" mode="page-function">
    <xsl:apply-templates select="g:sleep-time" mode="request"/>

    <xsl:apply-templates select=".//g:token-reference[not(../../g:response)]" mode="request"/>

    <xsl:apply-templates select="g:annotation" mode="request"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:if test="position() = 1">
      <xsl:text>result = </xsl:text>
    </xsl:if>
    <xsl:text>request</xsl:text>
    <xsl:apply-templates select="." mode="generate-test-number"/>
    <xsl:text>.</xsl:text>
    <xsl:value-of select="g:method"/>
    <xsl:text>(</xsl:text>

    <xsl:apply-templates select="g:uri/g:path" mode="request-uri"/>
    <xsl:apply-templates select="g:uri/g:query-string" mode="request-uri"/>
    <xsl:apply-templates select="g:uri/g:fragment" mode="request-uri"/>
    <xsl:apply-templates select="g:body" mode="request-parameter"/>
    <xsl:apply-templates select="g:headers" mode="request-parameter"/>

    <xsl:if test="string(g:body/g:form/@multipart) = 'true'">
      <xsl:text>,</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text>  True</xsl:text>
    </xsl:if>

    <xsl:text>)</xsl:text>

    <!--  Response token references may also supply new token values. -->
    <xsl:apply-templates select="g:response/g:token-reference" mode="request"/>
    <xsl:value-of select="helper:newLine()"/>

  </xsl:template>


  <!-- Spacing for page test numbers. -->
  <xsl:variable name="page-test-number-increment" select="100"/>


  <xsl:template match="g:page" mode="generate-number">
    <!--  We number page tests 100, 200, ... and request tests 101, 102; 201,
          202, ... There's a correspondance between request and page test
          numbers (request test numbers sequentially follow their page Test
          number). We cope gracefully with pages that have more than 100 tests.
          The page number is the page test number / 100.
     -->

    <xsl:value-of select=
      "count(preceding::g:page) +
       count(
         preceding::g:page/g:request[position() mod $page-test-number-increment = 0])
       + 1"/>
  </xsl:template>


  <xsl:template match="g:page" mode="generate-test-number">
    <!--  We ignore the @page-id attribute, and calculate our own number. -->

    <xsl:variable name="page-number">
      <xsl:apply-templates select="." mode="generate-number"/>
    </xsl:variable>

    <xsl:value-of select="$page-number * $page-test-number-increment"/>
  </xsl:template>


  <xsl:template match="g:page" mode="generate-function-name">
    <xsl:text>page</xsl:text>
    <xsl:apply-templates select="." mode="generate-number"/>
  </xsl:template>


  <xsl:template match="g:page" mode="file">
    <xsl:apply-templates select="*" mode="file"/>
  </xsl:template>


  <xsl:template match="g:page" mode="TestRunner">
    <xsl:apply-templates select="*" mode="TestRunner"/>

    <xsl:variable name="page-function-name">
      <xsl:apply-templates select="." mode="generate-function-name"/>
    </xsl:variable>

    <xsl:if test="not(preceding::g:page)">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># A method for each recorded page.</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>def </xsl:text>
    <xsl:value-of select="$page-function-name"/>
    <xsl:text>(self):</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"""</xsl:text>
    <xsl:apply-templates select="*" mode="page-description"/>
    <xsl:text>."""</xsl:text>
    <xsl:apply-templates select="*" mode="page-function"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>return result</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:page" mode="instrumentMethod">
    <xsl:variable name="page-number">
      <xsl:apply-templates select="." mode="generate-number"/>
    </xsl:variable>

    <xsl:variable name="page-test-number">
      <xsl:apply-templates select="." mode="generate-test-number"/>
    </xsl:variable>

    <xsl:variable name="page-function-name">
      <xsl:apply-templates select="." mode="generate-function-name"/>
    </xsl:variable>

    <xsl:if test="not(preceding::g:page)">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># Replace each method with an instrumented version.</xsl:text>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text># You can call the unadorned method using </xsl:text>
      <xsl:text>self.</xsl:text>
      <xsl:value-of select="$page-function-name"/>
      <xsl:text>.__target__().</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>instrumentMethod(Test(</xsl:text>
    <xsl:value-of select="$page-test-number"/>
    <xsl:text>, 'Page </xsl:text>
    <xsl:value-of select="$page-number"/>
    <xsl:text>'), '</xsl:text>
    <xsl:value-of select="$page-function-name"/>
    <xsl:text>')</xsl:text>
  </xsl:template>


  <xsl:template match="g:page" mode="__call__">
    <xsl:apply-templates select="*" mode="__call__"/>

    <xsl:variable name="page-function-name">
      <xsl:apply-templates select="." mode="generate-function-name"/>
    </xsl:variable>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>self.</xsl:text>
    <xsl:value-of select="$page-function-name"/>
    <xsl:text>()</xsl:text>

    <xsl:call-template name="indent">
      <xsl:with-param name="characters" select="12-string-length($page-function-name)"/>
    </xsl:call-template>
    <xsl:text># </xsl:text>
    <xsl:apply-templates select="*" mode="page-description"/>
  </xsl:template>


  <xsl:template match="g:sleep-time[../preceding-sibling::g:request]" mode="request">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>grinder.sleep(</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <xsl:template match="g:annotation" mode="request">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text># </xsl:text>
    <xsl:value-of select ="."/>
  </xsl:template>


  <!--  First sleep() for a page appears in the __call__ block. -->
  <xsl:template match="g:sleep-time[not(../preceding-sibling::g:request)]" mode="__call__">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>grinder.sleep(</xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <!-- token-reference with a new value. -->
  <xsl:template match="g:token-reference[g:new-value]" mode="request">
    <xsl:apply-templates select=".//g:conflicting-value" mode="request"/>

    <xsl:variable name="token-id" select="@token-id"/>
    <xsl:variable name="name" select="//g:token[@token-id=$token-id]/g:name"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>self.</xsl:text>
    <xsl:value-of select="$token-id"/>
    <xsl:text> = \</xsl:text>

    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:choose>
      <xsl:when test="@source">
        <xsl:text>httpUtilities.</xsl:text>
        <xsl:choose>
          <xsl:when test="@source = 'RESPONSE_LOCATION_HEADER_PATH_PARAMETER' or
                          @source = 'RESPONSE_LOCATION_HEADER_QUERY_STRING'">
            <xsl:text>valueFromLocationURI(</xsl:text>
          </xsl:when>
          <xsl:when test="@source = 'RESPONSE_BODY_HIDDEN_INPUT'">
            <xsl:text>valueFromHiddenInput(</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>valueFromBodyURI(</xsl:text>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:value-of select="helper:quoteForPython($name)"/>
        <xsl:text>)</xsl:text>

        <xsl:text> # </xsl:text>
        <xsl:value-of select="helper:quoteForPython(helper:summariseAsLine(g:new-value, 40))"/>
        <xsl:value-of select="helper:changeIndent(-1)"/>
      </xsl:when>

      <xsl:otherwise>
        <xsl:value-of select="helper:quoteForPython(g:new-value)"/>
        <xsl:value-of select="helper:changeIndent(-1)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="g:conflicting-value[not(preceding-sibling::g:conflicting-value)]" mode="request">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text># </xsl:text>
    <xsl:value-of select="count(../*)"/>
    <xsl:text> different values for </xsl:text>
    <xsl:value-of select="../@token-id"/>
    <xsl:text> found in response</xsl:text>

    <xsl:choose>
      <xsl:when test="preceding-sibling::g:new-value">
        <xsl:text>, using the first one.</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>; the first matched</xsl:text>
        <xsl:value-of select="helper:newLineAndIndent()"/>
        <xsl:text># the last known value of </xsl:text>
        <xsl:value-of select="../@token-id"/>
        <xsl:text> - don't update the variable.</xsl:text>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>


  <xsl:template match="g:path" mode="request-uri">
    <!-- Open quote here, last g:text or g:token-reference will close. -->
    <xsl:text>'</xsl:text>
    <xsl:apply-templates mode="request-uri"/>
  </xsl:template>


  <xsl:template match="g:query-string" mode="request-uri">
    <xsl:text> +</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <!-- Open quote here, last g:text or g:token-reference will close. -->
    <xsl:text>'?</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
    <xsl:apply-templates mode="request-uri"/>
  </xsl:template>


  <xsl:template match="g:path/g:text|g:query-string/g:text" mode="request-uri">
    <xsl:variable
      name="preceding-sibling-name"
      select="local-name(preceding-sibling::node()[1])"/>

    <xsl:if test="position() != 1 and $preceding-sibling-name != 'text'">
      <xsl:text> +</xsl:text>
      <xsl:value-of select="helper:changeIndent(1)"/>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text>'</xsl:text>
      <xsl:value-of select="helper:changeIndent(-1)"/>
    </xsl:if>

    <xsl:value-of select="helper:escape(.)"/>

    <xsl:if test="position() = last()">
      <!--  Final sibling, close quotes. -->
      <xsl:text>'</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="g:path/g:token-reference|g:query-string/g:token-reference" mode="request-uri">
    <xsl:variable name="token-id" select="@token-id"/>

    <!-- A previous token-reference will have defined a variable. -->
    <xsl:value-of select="//g:token[@token-id=$token-id]/g:name"/>
    <xsl:text>=' +</xsl:text>

    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>self.</xsl:text>
    <xsl:value-of select="$token-id"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>

  </xsl:template>


  <!-- Browsers, and HTTPClient, strip fragments from URIs they send to the
  wire, so we don't bother putting it into the script. If a browser was used
  for the TCPProxy recording, there won't be any fragments in the incoming
  stream anyway. -->
  <xsl:template match="g:fragment" mode="request-uri"/>

  <xsl:template match="g:body/g:binary" mode="request-parameter">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:base64ToPython(.)"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:body/g:file" mode="request-parameter">
    <!-- Data file is read at top level. We provide a parameter here
    to disambiguate the POST call if per-request headers are
    specified.-->
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>request</xsl:text>
    <xsl:apply-templates select="../.." mode="generate-test-number"/>
    <xsl:text>.__target__.data</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>

 </xsl:template>


  <xsl:template match="g:body/g:form" mode="request-parameter">
    <xsl:text>,</xsl:text>
    <xsl:call-template name="tuple"/>
  </xsl:template>


  <xsl:template match="g:body/g:escaped-string" mode="request-parameter">
    <xsl:text>,</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:quoteEOLEscapedStringForPython(.)"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:body/g:content-type" mode="request-parameter"/>


  <xsl:template match="g:headers[node()]" mode="request-parameter">
    <xsl:if test="../g:method='GET' or
                  ../g:method='HEAD' or
                  (../g:method='OPTIONS' or
                   ../g:method='POST' or
                   ../g:method='PUT') and not(../g:body)">
      <!-- No keyword arguments for methods, insert dummy parameter. -->
      <!-- The query string argument is alwasy null for GET, HEAD, as we pass
           query information via the uri. -->
      <xsl:text>, None</xsl:text>
    </xsl:if>

    <xsl:text>,</xsl:text>

    <xsl:call-template name="tuple"/>
  </xsl:template>


  <xsl:template match="g:header|g:parameter|g:form-field" mode="list-item">
    <xsl:call-template name="indent-list-item"/>

    <xsl:text>NVPair(</xsl:text>
    <xsl:value-of select="helper:quoteForPython(@name)"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="helper:quoteForPython(@value)"/>
    <xsl:text>),</xsl:text>
  </xsl:template>

  <xsl:template match="g:token-reference" mode="list-item">
    <xsl:variable name="token-id" select="@token-id"/>
    <xsl:variable name="name" select="//g:token[@token-id=$token-id]/g:name"/>

    <xsl:call-template name="indent-list-item"/>

    <xsl:text>NVPair(</xsl:text>
    <xsl:value-of select="helper:quoteForPython($name)"/>
    <xsl:text>, </xsl:text>
    <xsl:text>self.</xsl:text>
    <xsl:value-of select="$token-id"/>
    <xsl:text>),</xsl:text>
  </xsl:template>

  <xsl:template match="g:authorization/g:basic" mode="list-item">
    <xsl:call-template name="indent-list-item">
      <xsl:with-param name="first-item" select="not(../preceding-sibling::*)"/>
    </xsl:call-template>

    <xsl:text>httpUtilities.basicAuthorizationHeader(</xsl:text>
    <xsl:value-of select="helper:quoteForPython(@userid)"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="helper:quoteForPython(@password)"/>
    <xsl:text>),</xsl:text>
  </xsl:template>


  <xsl:template name="list">
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>[</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>

    <xsl:apply-templates mode="list-item"/>

    <xsl:text> ]</xsl:text>
    <xsl:value-of select="helper:changeIndent(-2)"/>
  </xsl:template>


  <xsl:template name="tuple">
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>

    <xsl:apply-templates mode="list-item"/>

    <xsl:text> )</xsl:text>
    <xsl:value-of select="helper:changeIndent(-2)"/>
  </xsl:template>


  <xsl:template name="indent-list-item">
    <xsl:param name="first-item" select="not(preceding-sibling::*)"/>

    <xsl:choose>
      <xsl:when test="$first-item"><xsl:text> </xsl:text></xsl:when>
      <xsl:otherwise><xsl:value-of select="helper:newLineAndIndent()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="indent">
    <xsl:param name="characters" select="1"/>
    <xsl:value-of select="substring('                      ', 0, $characters)"/>
  </xsl:template>


  <xsl:template match="text()|@*"/>
  <xsl:template match="text()|@*" mode="file"/>
  <xsl:template match="text()|@*" mode="TestRunner"/>
  <xsl:template match="text()|@*" mode="__call__"/>
  <xsl:template match="text()|@*" mode="page-function"/>
  <xsl:template match="text()|@*" mode="page-description"/>
  <xsl:template match="text()|@*" mode="request"/>
  <xsl:template match="text()|@*" mode="request-uri"/>
  <xsl:template match="text()|@*" mode="request-parameter"/>
  <xsl:template match="text()|@*" mode="instrumentMethod"/>

</xsl:stylesheet>
