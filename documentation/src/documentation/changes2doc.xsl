<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:preserve-space elements = "*"/>

  <xsl:template match="changes">
    <document>
      <header>
	<title>The Grinder Change Log</title>
      </header>
      <body>
	<xsl:apply-templates select="*"/>
      </body>
    </document>
  </xsl:template>

  <xsl:template match="section">
    <section id="{@id}">
      <title><xsl:value-of select="@name"/></title>
      <xsl:if test="@date">
	<p><strong><sub>Released <xsl:value-of select="@date"/></sub></strong></p>
      </xsl:if>
      <xsl:apply-templates/>
    </section>
  </xsl:template>

  <xsl:template match="@* | node() | text() | comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
