<?xml version="1.0"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!--
This stylesheet contains templates for converting documentv11 to HTML.  See the
imported document-to-html.xsl for details.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:import href="lm://transform.skin.common.html.document-to-html"/>

   <xsl:template match="source[@class='sh']">
    <xsl:apply-templates select="@id"/>
    <div class="shcontainer">
      <pre class="brush: bash; gutter: false">
	<xsl:copy-of select="@id"/>
	<xsl:apply-templates/>
      </pre>
    </div>
  </xsl:template>

   <xsl:template match="source[@class='cmd']">
    <xsl:apply-templates select="@id"/>
    <div class="shcontainer">
      <!-- No cmd brush available. -->
      <pre class="brush: bash; gutter: false">
	<xsl:copy-of select="@id"/>
	<xsl:apply-templates/>
      </pre>
    </div>
  </xsl:template>

  <xsl:template match="source[@class='text']">
    <xsl:apply-templates select="@id"/>
    <div class="shcontainer">
      <pre class="brush: text; gutter: false;">
	<xsl:copy-of select="@id"/>
	<xsl:apply-templates/>
      </pre>
    </div>
  </xsl:template>

  <xsl:template match="source[@class='java']">
    <xsl:apply-templates select="@id"/>
    <div class="shcontainer">
      <pre class="brush: java">
	<xsl:copy-of select="@id"/>
	<xsl:apply-templates/>
      </pre>
    </div>
  </xsl:template>

  <xsl:template match="source[@class='jython']">
    <xsl:apply-templates select="@id"/>
    <div class="shcontainer">
      <pre class="brush: python">
	<xsl:copy-of select="@id"/>
	<xsl:apply-templates/>
      </pre>
    </div>
  </xsl:template>

  <xsl:template match="source[@class='clojure']">
    <xsl:apply-templates select="@id"/>
    <div class="shcontainer">
      <pre class="brush: clojure">
	<xsl:copy-of select="@id"/>
	<xsl:apply-templates/>
    </pre>
    </div>
  </xsl:template>

  <xsl:template match="source[@class='xml']">
    <xsl:apply-templates select="@id"/>
    <div class="shcontainer">
      <pre class="brush: xml">
	<xsl:copy-of select="@id"/>
	<xsl:apply-templates/>
    </pre>
    </div>
  </xsl:template>

  <xsl:template match="source">
    <H1>FIXME</H1>
  </xsl:template>


  <xsl:template match="document">
    <meta-data>
      <xsl:apply-templates select="header/meta"/>
      <xsl:apply-templates select="header/link"/>
    </meta-data>
    <div id="content">
      <xsl:apply-templates select="body" mode="carry-body-attribs"/>
      <div id="skinconf-printlink"/>
      <div id="skinconf-xmllink"/>
      <div id="skinconf-podlink"/>
      <div id="skinconf-txtlink"/>
      <div id="skinconf-pdflink"/>
      <div id="disable-font-script"/>
      <xsl:if test="normalize-space(header/title)!=''">
        <h1>
          <xsl:value-of select="header/title"/>
        </h1>
      </xsl:if>
      <xsl:if test="normalize-space(header/subtitle)!=''">
        <h3>
          <xsl:value-of select="header/subtitle"/>
        </h3>
      </xsl:if>
<!--
      <xsl:apply-templates select="header/type"/>
      <xsl:apply-templates select="header/notice"/>
      <xsl:apply-templates select="header/abstract"/>
      <xsl:apply-templates select="body"/>

      <div class="attribution">
        <xsl:apply-templates select="header/authors"/>
        <xsl:if test="header/authors and header/version">
          <xsl:text>; </xsl:text>
        </xsl:if>
        <xsl:apply-templates select="header/version"/>
      </div>
    -->
      <div id="front-matter">
        <div id="motd-page"/>
        <xsl:if test="header/abstract">
          <div class="abstract">
            <xsl:value-of select="header/abstract"/>
          </div>
        </xsl:if>
        <div id="skinconf-toc-page"/>
      </div>
      <xsl:apply-templates select="body"/>
      <xsl:if test="header/authors">
        <p align="right">
          <font size="-2">
            <xsl:for-each select="header/authors/person">
              <xsl:choose>
                <xsl:when test="position()=1">by&#160;</xsl:when>
                <xsl:otherwise>,&#160;</xsl:otherwise>
              </xsl:choose>
              <xsl:value-of select="@name"/>
            </xsl:for-each>
          </font>
        </p>
      </xsl:if>
      <xsl:if test="header/version">
        <xsl:apply-templates select="header/version"/>
      </xsl:if>
    </div>
  </xsl:template>
  <xsl:template match="body">
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template match="@id">
    <xsl:apply-imports/>
  </xsl:template>
  <xsl:template match="section">
    <xsl:apply-templates select="@id"/>
    <xsl:variable name = "level" select = "count(ancestor::section)+1" />
    <xsl:choose>
      <xsl:when test="$level=1">
        <div class="skinconf-heading-{$level}">
          <h1>
            <xsl:value-of select="title"/>
          </h1>
        </div>
        <div class="section">
          <xsl:apply-templates select="*[not(self::title)]"/>
        </div>
      </xsl:when>
      <xsl:when test="$level=2">
        <div class="skinconf-heading-{$level}">
          <h2>
            <xsl:value-of select="title"/>
          </h2>
        </div>
        <xsl:apply-templates select="*[not(self::title)]"/>
      </xsl:when>
<!-- If a faq, answer sections will be level 3 (1=Q/A, 2=part) -->
      <xsl:when test="$level=3 and $notoc='true'">
        <h4 class="faq">
          <xsl:value-of select="title"/>
        </h4>
        <div align="right"><a href="#{@id}-menu">^</a>
        </div>
        <div style="margin-left: 15px">
          <xsl:apply-templates select="*[not(self::title)]"/>
        </div>
      </xsl:when>
      <xsl:when test="$level=3">
        <h4>
          <xsl:value-of select="title"/>
        </h4>
        <xsl:apply-templates select="*[not(self::title)]"/>
      </xsl:when>
      <xsl:otherwise>
        <h5>
          <xsl:value-of select="title"/>
        </h5>
        <xsl:apply-templates select="*[not(self::title)]"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="figure">
    <xsl:apply-templates select="@id"/>
    <div style="text-align: center;">
        <xsl:if test="@id">
	  <xsl:attribute name="id">
	    <xsl:value-of select="@id"/>
	  </xsl:attribute>
        </xsl:if>
      <img src="{@src}" alt="{@alt}" class="figure">
        <xsl:if test="@id">
          <xsl:attribute name="id">
          <xsl:value-of select="@id"/>-figure</xsl:attribute>
        </xsl:if>
        <xsl:if test="@height">
          <xsl:attribute name="height">
            <xsl:value-of select="@height"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:if test="@width">
          <xsl:attribute name="width">
            <xsl:value-of select="@width"/>
          </xsl:attribute>
        </xsl:if>
      </img>
    </div>
  </xsl:template>
</xsl:stylesheet>
