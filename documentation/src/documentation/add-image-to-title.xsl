<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:preserve-space elements = "*"/>

  <xsl:template match="header/title">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
    <!-- Can't put the figure in the title because the fo.xsl doesn't
    	 apply templates to that. -->
    <abstract>
      <figure src="../images/logo.png" width="166" height="217"/>
      <p/><p/><p/>
      <p/><p/><p/>
    </abstract>
  </xsl:template>

  <xsl:template match="@* | node() | text() | comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
