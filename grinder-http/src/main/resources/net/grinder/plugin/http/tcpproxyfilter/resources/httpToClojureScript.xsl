<?xml version="1.0" encoding="UTF-8"?>

<!--
 Copyright (C) 2011 - 2012 Philip Aston
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

  <xsl:param name="testNumberOffset"
             select="/g:http-recording/g:metadata/g:test-number-offset"/>


  <xsl:template match="g:http-recording">
    <xsl:value-of select="helper:resetIndent()"/>
    <xsl:text>;; </xsl:text>
    <xsl:value-of select="g:metadata/g:version"/>

    <xsl:text>
;; HTTP script recorded by TCPProxy at </xsl:text>
    <xsl:value-of select="helper:formatTime(g:metadata/g:time)"/>

    <xsl:text>

(ns user
  (:import (net.grinder.script Test Grinder)
           (net.grinder.plugin.http HTTPPluginControl HTTPRequest)
           (HTTPClient NVPair Codecs)))

(def grinder (Grinder/grinder))
(def connectionDefaults (HTTPPluginControl/getConnectionDefaults))
(def httpUtilities (HTTPPluginControl/getHTTPUtilities))

; To use a proxy server, uncomment the next line and set the host and port.
; (.setProxyServer connectionDefaults "localhost" 8001)

; Worker thread state is stored in a map using a dynamic var.
(def ^:dynamic *tokens*)
(defn set-token [k v] (set! *tokens* (assoc *tokens* k v)))
(defn token [k] (*tokens* k))

(defn nvpairs [c] (into-array NVPair
  (map (fn [[k v]] (NVPair. k v)) (partition 2 c))))

(defn httprequest [url &amp; [headers]]
  (doto (HTTPRequest.) (.setUrl url) (.setHeaders (nvpairs headers))))

(defn basic-authorization [u p]
  (str "Basic " (Codecs/base64Encode  (str u ":" p))))

(defn to-bytes [s]
  (letfn [(to-byte[x] (byte (if (> x 0x7f) (- x 0x100) x)))]
    (byte-array (map to-byte s))))

(defmacro defrequest [name test &amp; args]
  `(do
     (def ~name (httprequest ~@args))
     (.record ~test ~name (HTTPRequest/getHttpMethodFilter))))

(defmacro defpage [name description test &amp; rest]
  `(do
     (defn ~name ~description ~@rest)
     (.record ~test ~name)))
</xsl:text>

<xsl:text>
; Offline debug
; (use '[clojure.string :only (join)])
; (defmacro .GET [&amp; k] `(.. grinder (getLogger) (debug (str "GET " (join ", " `(~~@k))))))
; (defmacro .POST [&amp; k] `(.. grinder (getLogger) (debug (str "POST " (join ", " `(~~@k))))))

</xsl:text>


    <xsl:apply-templates select="*" mode="file"/>

    <xsl:value-of select="helper:newLine()"/>

    <xsl:apply-templates select="*" mode="page-function"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(defn run</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"Called for every run performed by the worker thread." [] </xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:apply-templates select="*" mode="run-function"/>

    <xsl:if test="not(//g:request)">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text>; Empty recording!</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="helper:changeIndent(-1)"/>

    <xsl:text>)</xsl:text>
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="helper:newLine()"/>

    <xsl:text>(defn runner-factory</xsl:text>

    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"Create a run function. Called for each worker thread." []</xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(binding [*tokens* {}] (bound-fn* run)))</xsl:text>

    <xsl:value-of select="helper:changeIndent(-1)"/>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:base-uri" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>(def </xsl:text>
    <xsl:value-of select="@uri-id"/>
    <xsl:text> "</xsl:text>
    <xsl:value-of select="concat(g:scheme, '://', g:host, ':', g:port)"/>
    <xsl:text>")</xsl:text>

    <xsl:if test="not(following::g:base-uri)">
      <xsl:value-of select="helper:newLine()"/>
    </xsl:if>
  </xsl:template>


  <xsl:template match="g:common-headers[@headers-id='defaultHeaders']" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>(.setDefaultHeaders connectionDefaults (nvpairs </xsl:text>
    <xsl:call-template name="list"/>
    <xsl:text>))</xsl:text>
    <xsl:value-of select="helper:newLine()"/>
  </xsl:template>


  <xsl:template match="g:common-headers" mode="file">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:text>(def </xsl:text>
    <xsl:value-of select="@headers-id"/>
    <xsl:text> </xsl:text>

    <xsl:call-template name="list"/>
    <xsl:text>)</xsl:text>
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

    <xsl:for-each select="g:comment">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text>; </xsl:text>
      <xsl:value-of select="."/>
    </xsl:for-each>
    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:text>(defrequest </xsl:text>
    <xsl:value-of select="$request-name"/>
    <xsl:text> (Test. </xsl:text>
    <xsl:value-of select="$request-number"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="helper:quoteForClojure(g:description)"/>
    <xsl:text>) </xsl:text>
    <xsl:value-of select="g:uri/@extends"/>

    <xsl:if test="g:headers/@extends[. != 'defaultHeaders']">
      <xsl:text> </xsl:text>
      <xsl:value-of select="g:headers/@extends"/>
    </xsl:if>

    <xsl:text>)</xsl:text>

    <xsl:if test="g:body/g:file">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text>(.setDataFromFile </xsl:text>
      <xsl:value-of select="$request-name"/>
      <xsl:text> "</xsl:text>
      <xsl:value-of select="g:body/g:file"/>
      <xsl:text>")</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLine()"/>
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

    <xsl:text>(.</xsl:text>
    <xsl:value-of select="g:method"/>
    <xsl:text> </xsl:text>
    <xsl:text>request</xsl:text>
    <xsl:apply-templates select="." mode="generate-test-number"/>

    <xsl:choose>
      <xsl:when test="count(g:uri/*[not(g:unparsed)]/*) > 1">
        <xsl:value-of select="helper:changeIndent(1)"/>
        <xsl:value-of select="helper:newLineAndIndent()"/>
        <xsl:text>(str </xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text> </xsl:text>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:apply-templates select="g:uri/g:path" mode="request-uri"/>
    <xsl:apply-templates select="g:uri/g:query-string" mode="request-uri"/>
    <xsl:apply-templates select="g:uri/g:fragment" mode="request-uri"/>

    <xsl:if test="count(g:uri/*[not(g:unparsed)]/*) > 1">
      <xsl:text>)</xsl:text>
      <xsl:value-of select="helper:changeIndent(-1)"/>
    </xsl:if>

    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:apply-templates select="g:body" mode="request-parameter"/>
    <xsl:apply-templates select="g:headers" mode="request-parameter"/>

    <xsl:if test="string(g:body/g:form/@multipart) = 'true'">
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text>true</xsl:text>
    </xsl:if>

    <xsl:text>)</xsl:text>

    <xsl:value-of select="helper:changeIndent(-1)"/>

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


  <xsl:template match="g:page" mode="page-function">

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
      <xsl:text>; A function for each recorded page.</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(defpage </xsl:text>
    <xsl:value-of select="$page-function-name"/>
    <xsl:text> "</xsl:text>
    <xsl:apply-templates select="*" mode="page-description"/>
    <xsl:text>." (Test. </xsl:text>
    <xsl:value-of select="$page-test-number"/>
    <xsl:text> "Page </xsl:text>
    <xsl:value-of select="$page-number"/>
    <xsl:text>") []</xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:apply-templates select="*" mode="page-function"/>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
    <xsl:value-of select="helper:newLine()"/>

  </xsl:template>

  <xsl:template match="g:page" mode="run-function">
    <xsl:apply-templates select="*" mode="run-function"/>

    <xsl:variable name="page-function-name">
      <xsl:apply-templates select="." mode="generate-function-name"/>
    </xsl:variable>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(</xsl:text>
    <xsl:value-of select="$page-function-name"/>
    <xsl:text>)</xsl:text>

    <xsl:call-template name="indent">
      <xsl:with-param name="characters" select="12-string-length($page-function-name)"/>
    </xsl:call-template>
    <xsl:text>; </xsl:text>
    <xsl:apply-templates select="*" mode="page-description"/>
  </xsl:template>


  <xsl:template match="g:sleep-time[../preceding-sibling::g:request]" mode="request">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(.sleep grinder </xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <xsl:template match="g:annotation" mode="request">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>; </xsl:text>
    <xsl:value-of select ="."/>
  </xsl:template>


  <!--  First sleep() for a page appears in the run-function block. -->
  <xsl:template match="g:sleep-time[not(../preceding-sibling::g:request)]" mode="run-function">
    <xsl:value-of select="helper:newLine()"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(.sleep grinder </xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <!-- token-reference with a new value. -->
  <xsl:template match="g:token-reference[g:new-value]" mode="request">
    <xsl:apply-templates select=".//g:conflicting-value" mode="request"/>

    <xsl:variable name="token-id" select="@token-id"/>
    <xsl:variable name="name" select="//g:token[@token-id=$token-id]/g:name"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(set-token :</xsl:text>
    <xsl:value-of select="$token-id"/>
    <xsl:text> </xsl:text>

    <xsl:choose>
      <xsl:when test="@source">
        <xsl:choose>
          <xsl:when test="@source = 'RESPONSE_LOCATION_HEADER_PATH_PARAMETER' or
                          @source = 'RESPONSE_LOCATION_HEADER_QUERY_STRING'">
            <xsl:text>(.valueFromLocationURI</xsl:text>
          </xsl:when>
          <xsl:when test="@source = 'RESPONSE_BODY_HIDDEN_INPUT'">
            <xsl:text>(.valueFromHiddenInput</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>(.valueFromBodyURI</xsl:text>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:text> httpUtilities </xsl:text>

        <xsl:value-of select="helper:quoteForClojure($name)"/>
        <xsl:text>)</xsl:text>

        <xsl:text> ; </xsl:text>
        <xsl:value-of select="helper:quoteForClojure(helper:summariseAsLine(g:new-value, 40))"/>
	<xsl:text>
	</xsl:text>
      </xsl:when>

      <xsl:otherwise>
        <xsl:value-of select="helper:quoteForClojure(g:new-value)"/>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:template match="g:conflicting-value[not(preceding-sibling::g:conflicting-value)]" mode="request">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>; </xsl:text>
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
        <xsl:text>; the last known value of </xsl:text>
        <xsl:value-of select="../@token-id"/>
        <xsl:text> - don't update the variable.</xsl:text>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>


  <xsl:template match="g:path" mode="request-uri">
    <!-- Open quote here, last g:text or g:token-reference will close. -->
    <xsl:text>"</xsl:text>
    <xsl:apply-templates mode="request-uri"/>
  </xsl:template>


  <xsl:template match="g:query-string" mode="request-uri">
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>"?</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
    <xsl:apply-templates mode="request-uri"/>
  </xsl:template>


  <xsl:template match="g:path/g:text|g:query-string/g:text" mode="request-uri">
    <xsl:variable
      name="preceding-sibling-name"
      select="local-name(preceding-sibling::node()[1])"/>

    <xsl:if test="position() != 1 and $preceding-sibling-name != 'text'">
      <xsl:value-of select="helper:changeIndent(1)"/>
      <xsl:value-of select="helper:newLineAndIndent()"/>
      <xsl:text>"</xsl:text>
      <xsl:value-of select="helper:changeIndent(-1)"/>
    </xsl:if>

    <xsl:value-of select="helper:escape(.)"/>

    <xsl:if test="position() = last()">
      <!--  Final sibling, close quotes. -->
      <xsl:text>"</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="g:path/g:token-reference|g:query-string/g:token-reference" mode="request-uri">
    <xsl:variable name="token-id" select="@token-id"/>

    <!-- A previous token-reference will have defined a variable. -->
    <xsl:value-of select="//g:token[@token-id=$token-id]/g:name"/>
    <xsl:text>=" (token :</xsl:text>
    <xsl:value-of select="$token-id"/>
    <xsl:text>)</xsl:text>

  </xsl:template>


  <!-- Browsers, and HTTPClient, strip fragments from URIs they send to the
  wire, so we don't bother putting it into the script. If a browser was used
  for the TCPProxy recording, there won't be any fragments in the incoming
  stream anyway. -->
  <xsl:template match="g:fragment" mode="request-uri"/>

  <xsl:template match="g:body/g:binary" mode="request-parameter">
    <xsl:text> </xsl:text>
    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:text>(to-bytes </xsl:text>
    <xsl:value-of select="helper:base64ToClojure(.)"/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <xsl:template match="g:body/g:file" mode="request-parameter">
    <!-- Data file is read at top level. We provide a parameter here
    to disambiguate the POST call if per-request headers are
    specified.-->
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(.getData </xsl:text>
    <xsl:text>request</xsl:text>
    <xsl:apply-templates select="../.." mode="generate-test-number"/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <xsl:template match="g:body/g:form" mode="request-parameter">
    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:text>(nvpairs </xsl:text>
    <xsl:call-template name="list"/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <xsl:template match="g:body/g:escaped-string" mode="request-parameter">
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:text>(.getBytes </xsl:text>
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:value-of select="helper:newLineAndIndent()"/>
    <xsl:value-of select="helper:quoteEOLEscapedStringForClojure(.)"/>
    <xsl:text>)</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template match="g:headers[node()]" mode="request-parameter">
    <xsl:if test="../g:method='GET' or
                  ../g:method='HEAD' or
                  (../g:method='OPTIONS' or
                   ../g:method='POST' or
                   ../g:method='PUT') and not(../g:body)">
      <!-- No keyword arguments for methods, insert dummy parameter. -->
      <!-- The query string argument is always null for GET, HEAD, as we pass
           query information via the uri. -->
      <xsl:text> nil</xsl:text>
    </xsl:if>

    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:text>(nvpairs </xsl:text>
    <xsl:call-template name="list"/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <xsl:template match="g:header|g:parameter|g:form-field" mode="list-item">

    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:value-of select="helper:quoteForClojure(@name)"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="helper:quoteForClojure(@value)"/>
  </xsl:template>

  <xsl:template match="g:token-reference" mode="list-item">
    <xsl:variable name="token-id" select="@token-id"/>
    <xsl:variable name="name" select="//g:token[@token-id=$token-id]/g:name"/>

    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:value-of select="helper:quoteForClojure($name)"/>
    <xsl:text> (token :</xsl:text>
    <xsl:value-of select="$token-id"/>
    <xsl:text>)</xsl:text>
  </xsl:template>

  <xsl:template match="g:authorization/g:basic" mode="list-item">
    <xsl:value-of select="helper:newLineAndIndent()"/>

    <xsl:text>"Authorization", (basic-authorization </xsl:text>
    <xsl:value-of select="helper:quoteForClojure(@userid)"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="helper:quoteForClojure(@password)"/>
    <xsl:text>)</xsl:text>
  </xsl:template>


  <xsl:template name="list">
    <xsl:value-of select="helper:changeIndent(1)"/>
    <xsl:text>[</xsl:text>
    <xsl:apply-templates mode="list-item"/>
    <xsl:text>]</xsl:text>
    <xsl:value-of select="helper:changeIndent(-1)"/>
  </xsl:template>


  <xsl:template name="indent">
    <xsl:param name="characters" select="1"/>
    <xsl:value-of select="substring('                      ', 0, $characters)"/>
  </xsl:template>


  <xsl:template match="text()|@*"/>
  <xsl:template match="text()|@*" mode="file"/>
  <xsl:template match="text()|@*" mode="run-function"/>
  <xsl:template match="text()|@*" mode="page-function"/>
  <xsl:template match="text()|@*" mode="page-description"/>
  <xsl:template match="text()|@*" mode="request"/>
  <xsl:template match="text()|@*" mode="request-uri"/>
  <xsl:template match="text()|@*" mode="request-parameter"/>

</xsl:stylesheet>
