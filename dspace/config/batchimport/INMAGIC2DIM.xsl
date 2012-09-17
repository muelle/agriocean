<?xml version="1.0" encoding="UTF-8"?>

<!--
    Created on : August 17, 2012
    Author     : Denys Slipetskyy
    Description:
        Transform custom XML to DIM.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:dc="http://purl.org/dc/elements/1.1/" 
                xmlns:dcterms="http://purl.org/dc/terms/" 
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
                xmlns:inm="http://www.inmagic.com/webpublisher/query"
                version="1.0">
  
  <xsl:output method="xml" encoding="UTF-8"/>

  <xsl:template match="@* | node()">
    <!--  XXX don't copy everything by default.
           <xsl:copy>
                   <xsl:apply-templates select="@* | node()"/>
           </xsl:copy>
    -->
  </xsl:template>
  
  <xsl:template match="inm:Results">
    <xsl:apply-templates select="inm:Recordset"/>
  </xsl:template>
  
  <xsl:template match="inm:Recordset">  
    <result>
      <xsl:apply-templates select="inm:Record"/>
    </result>
  </xsl:template>
  
  <xsl:template match="inm:Record">
    <dim:dim xmlns:dim="http://www.dspace.org/xmlns/dspace/dim">
      <xsl:apply-templates/>
    </dim:dim>
  </xsl:template>
        
  <xsl:template match="inm:Copy-Management">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="description" qualifier="nrcopies">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="inm:Title">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="title">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="inm:Subtitle">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="title" qualifier="alternative">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="inm:Author">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="contributor" qualifier="author">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="inm:Corporate-Author">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="contributor" qualifier="corpauthor">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="inm:Source">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="identifier" qualifier="citation">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="inm:Place">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="publisher" qualifier="place">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Publisher">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="publisher">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Pub-Date">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="date" qualifier="issued">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Physical-Description">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="format" qualifier="pages">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Record-Type">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="type" qualifier="specified">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Series">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="relation" qualifier="ispartofseries">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Descriptors">
    <!--
     (! many values (inm:Descriptors) to be combined in one element in dspace (dc.description.others) separated by ';'
    -->
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="description" qualifier="other">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Abstract">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="description" qualifier="abstract">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Notes">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="description" qualifier="notes">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:ISBN">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="identifier" qualifier="isbn">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Loan-Policy">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="description" qualifier="loanpolicy">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:URL">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="identifier" qualifier="uri">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:ISSN">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="identifier" qualifier="issn">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Vol">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="bibliographicCitation" qualifier="volume">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:No">
    <xsl:if test="string-length(text()) &gt; 0"> 
      <dim:field mdschema="dc" element="bibliographicCitation" qualifier="issue">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

  <xsl:template match="inm:Pages">
    <xsl:if test="string-length(text()) &gt; 0">
      <dim:field mdschema="dc" element="bibliographicCitation" qualifier="stpage">
        <xsl:value-of select="text()"/>
      </dim:field>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
