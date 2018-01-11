<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:preserve-space elements = "*"/>

  <xsl:template match="scripts">
<document>
<header>
  <title>Script Gallery</title>
</header>

<body>

  <p>This page contains examples of Jython scripts and script snippets
  that can be used with The Grinder 3. The scripts can also be found
  in the <code>examples</code> directory of the distribution. To use
  one of these scripts, you'll need to set up a <a
  href="site:getting-started/properties"><code>grinder.properties</code></a>
  file. Please also make sure you are using the latest version of
  The Grinder 3.</p>

  <p>If you're new to Python, it might help to know that that blocks
  are delimited by lexical indentation.</p>

  <p>The scripts make use of The Grinder <a
  href="script-javadoc/index.html">script API</a>. The
  <code>grinder</code> object in the scripts is an instance of
  <code>ScriptContext</code> through which the script can obtain
  contextual information (such as the worker process ID) and services
  (such as logging).</p>

  <p>If you have a script that you would like to like to see to this
  page, please send it to <a
  href="ext:mail/grinder-use">grinder-use</a>.</p>

  <xsl:apply-templates select="*"/>
</body>
</document>
  </xsl:template>

  <xsl:template match="script">
    <section id="{@id}">
      <title><xsl:value-of select="@title"/></title>
      <source class="{@language}">
        <xsl:apply-templates/>
      </source>
    </section>
  </xsl:template>

  <xsl:template match="@* | node() | text() | comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
