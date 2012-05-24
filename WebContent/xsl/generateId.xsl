<?xml version="1.0"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" encoding="utf-8"/>

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="*:s|*:head">
  <xsl:variable name="elemName" select="name()"/>
  <xsl:variable name="docPos">
    <xsl:choose>
      <xsl:when test="$elemName = 's'">
        <xsl:value-of select="count(./preceding::*:s) + 1"/>
	    </xsl:when>
      <xsl:when test="$elemName = 'head'">
        <xsl:value-of select="count(./preceding::*:head) + 1"/>
      </xsl:when>
	    <xsl:otherwise></xsl:otherwise>
	  </xsl:choose>
  </xsl:variable>
  <xsl:copy>
    <xsl:attribute name="xml:id">
      <xsl:value-of select="concat($elemName, $docPos)"/>
    </xsl:attribute>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
