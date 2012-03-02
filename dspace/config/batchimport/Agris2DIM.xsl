<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : Agris2DIM.xsl
    Created on : February 14, 2012, 11:35 AM
    Author     : Denys Slipetskyy
    Description:
        Transform Agris AP to DIM.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:ags="http://purl.org/agmes/1.1/" 
                xmlns:dc="http://purl.org/dc/elements/1.1/" 
                xmlns:agls="http://www.naa.gov.au/recordkeeping/gov_online/agls/1.2" 
                xmlns:dcterms="http://purl.org/dc/terms/" 
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
                version="1.0">
    
    <xsl:output method="xml" encoding="UTF-8"/>

    <xsl:template match="@* | node()">
        <!--  XXX don't copy everything by default.
                <xsl:copy>
                        <xsl:apply-templates select="@* | node()"/>
                </xsl:copy>
         -->
    </xsl:template>
    
    <xsl:template match="agrisResources">
        <result>
            <xsl:apply-templates select="agrisResource"/>
        </result>
    </xsl:template>

    <xsl:template match="agrisResource">
        <dim:dim xmlns:dim="http://www.dspace.org/xmlns/dspace/dim">
            <dim:field mdschema="dc" element="identifier" qualifier="arn">
                <xsl:value-of select="@ags:ARN"/>
            </dim:field>
            <xsl:apply-templates/>
        </dim:dim>
    </xsl:template>
    
    <xsl:template match="ags:resources">
        <result>
            <xsl:apply-templates select="ags:resource"/>
        </result>
    </xsl:template>

    <xsl:template match="ags:resource">
        <dim:dim xmlns:dim="http://www.dspace.org/xmlns/dspace/dim">
            <dim:field mdschema="dc" element="identifier" qualifier="arn">
                <xsl:value-of select="@ags:ARN"/>
            </dim:field>
            <xsl:apply-templates/>
        </dim:dim>
    </xsl:template>
    
    <!-- We need to test if dc:title was processed before, in this case 
    we will use dc.title.alternative instead of dc.title -->
    <xsl:template match="dc:title">
        <xsl:choose>
            <xsl:when test="count(preceding-sibling::node()[name()=name(current())])=0">
                <dim:field mdschema="dc" element="title">
                    <xsl:attribute name="lang">
                        <xsl:value-of select="@xml:lang"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </dim:field>
            </xsl:when>
            <xsl:otherwise>
                <dim:field mdschema="dc" element="title" qualifier="alternative">
                    <xsl:attribute name="lang">
                        <xsl:value-of select="@xml:lang"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </dim:field>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dc:creator">
        <xsl:apply-templates select="ags:creatorPersonal"/>
        <xsl:apply-templates select="ags:creatorCorporate"/>
        <xsl:apply-templates select="ags:creatorConference"/>
    </xsl:template>
    
    
    <xsl:template match="ags:creatorPersonal">
        <dim:field mdschema="dc" element="contributor" qualifier="author">
            <xsl:attribute name="lang">
                <xsl:value-of select="@xml:lang"/>
            </xsl:attribute>
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="ags:creatorCorporate">
        <dim:field mdschema="dc" element="contributor" qualifier="corpauthor">
            <xsl:attribute name="lang">
                <xsl:value-of select="@xml:lang"/>
            </xsl:attribute>
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="ags:creatorConference">
        <dim:field mdschema="dc" element="bibliographicCitation" qualifier="conferencename">
            <xsl:attribute name="lang">
                <xsl:value-of select="@xml:lang"/>
            </xsl:attribute>
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dc:publisher">
        <xsl:apply-templates select="ags:publisherName"/>
        <xsl:apply-templates select="ags:publisherPlace"/>
    </xsl:template>
    
    <xsl:template match="ags:publisherName">
        <dim:field mdschema="dc" element="publisher">
            <xsl:attribute name="lang">
                <xsl:value-of select="@xml:lang"/>
            </xsl:attribute>
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="ags:publisherPlace">
        <dim:field mdschema="dc" element="publisher" qualifier="place">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dc:date">
        <xsl:apply-templates select="dcterms:dateIssued"/>
    </xsl:template>
    
    <xsl:template match="dcterms:dateIssued">
        <dim:field mdschema="dc" element="date" qualifier="issued">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dc:subject">
        <xsl:if test="string-length(text()) > 0">
            <dim:field mdschema="dc" element="subject" qualifier="other">
                <xsl:attribute name="lang">
                    <xsl:value-of select="@xml:lang"/>
                </xsl:attribute>
                <xsl:value-of select="text()"/>
            </dim:field>
        </xsl:if>
        <xsl:apply-templates select="ags:subjectThesaurus"/>
    </xsl:template>
    
    <xsl:template match="ags:subjectThesaurus">
        <xsl:choose>
            <xsl:when test="@scheme = 'ags:AGROVOC'">
                <xsl:choose>
                    <xsl:when test="contains(text(),'agrovoc#c_')">
                        <dim:field mdschema="dc" element="subject" qualifier="agrovoc-uri">
                            <xsl:value-of select="text()"/>
                        </dim:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <dim:field mdschema="dc" element="subject" qualifier="agrovoc">
                            <xsl:attribute name="lang">
                                <xsl:value-of select="@xml:lang"/>
                            </xsl:attribute>
                            <xsl:value-of select="text()"/>
                        </dim:field>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="@scheme='ags:ASFAT'">
                    <dim:field mdschema="dc" element="subject" qualifier="asfa">
                        <xsl:attribute name="lang">
                            <xsl:value-of select="@xml:lang"/>
                        </xsl:attribute>
                        <xsl:value-of select="text()"/>
                    </dim:field>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dc:description">
        <xsl:apply-templates select="ags:descriptionNotes"/>
        <xsl:apply-templates select="dcterms:abstract"/>
        <xsl:apply-templates select="dcterms:provenance"/>
    </xsl:template>
    
    <xsl:template match="dcterms:abstract">
        <dim:field mdschema="dc" element="description" qualifier="abstract">
            <xsl:attribute name="lang">
                <xsl:value-of select="@xml:lang"/>
            </xsl:attribute>
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dcterms:provenance">
        <dim:field mdschema="dc" element="description" qualifier="provenance">
            <xsl:attribute name="lang">
                <xsl:value-of select="@xml:lang"/>
            </xsl:attribute>
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="ags:descriptionNotes">
        <xsl:choose>
            <xsl:when test="starts-with(text(),'Advisor:')">
                <dim:field mdschema="dc" element="contributor" qualifier="advisor">
                    <xsl:attribute name="lang">
                        <xsl:value-of select="@xml:lang"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </dim:field>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="starts-with(text(),'Funding Organizations:')">
                        <dim:field mdschema="dc" element="description" qualifier="sponsorship">
                            <xsl:attribute name="lang">
                                <xsl:value-of select="@xml:lang"/>
                            </xsl:attribute>
                            <xsl:value-of select="text()"/>
                        </dim:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:choose>
                            <xsl:when test="starts-with(text(),'Status:')">
                                <dim:field mdschema="dc" element="description" qualifier="status">
                                    <xsl:attribute name="lang">
                                        <xsl:value-of select="@xml:lang"/>
                                    </xsl:attribute>
                                    <xsl:value-of select="text()"/>
                                </dim:field>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:choose>
                                    <xsl:when test="starts-with(text(),'Free Keywords:')">
                                        <dim:field mdschema="dc" element="description" qualifier="other">
                                            <xsl:attribute name="lang">
                                                <xsl:value-of select="@xml:lang"/>
                                            </xsl:attribute>
                                            <xsl:value-of select="text()"/>
                                        </dim:field>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <dim:field mdschema="dc" element="description" qualifier="notes">
                                            <xsl:attribute name="lang">
                                                <xsl:value-of select="@xml:lang"/>
                                            </xsl:attribute>
                                            <xsl:value-of select="text()"/>
                                        </dim:field>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dc:identifier">
        <xsl:choose>
            <xsl:when test="@scheme = 'ags:ISBN'">
                <dim:field mdschema="dc" element="identifier" qualifier="isbn">
                    <xsl:value-of select="text()"/>
                </dim:field>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="@scheme = 'dcterms:URI'">
                        <dim:field mdschema="dc" element="identifier" qualifier="uri">
                            <xsl:value-of select="text()"/>
                        </dim:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="@scheme = 'ags:DOI'">
                            <dim:field mdschema="dc" element="relation" qualifier="doi">
                                <xsl:value-of select="text()"/>
                            </dim:field>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

   <!-- We will not process dc.type for now as it is used in completely different
   way than in AOD.
   <xsl:template match="dc:type">
        <dim:field mdschema="dc" element="type">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template> -->
    
    <xsl:template match="dc:format">
        <xsl:apply-templates select="dcterms:extent"/>
        <xsl:apply-templates select="dcterms:medium"/>
    </xsl:template>
    
    <xsl:template match="dcterms:extent">
        <xsl:choose>
            <xsl:when test="contains(text(),'p.')">
                <xsl:choose>
                    <xsl:when test="string-length(substring-after(text(),'p.')) = 0">
                        <dim:field mdschema="dc" element="format" qualifier="pages">
                            <xsl:value-of select="substring-before(text(),'p.')"/>
                        </dim:field>
                    </xsl:when>
            
            <!-- <xsl:when test="ends-with(text(),'p.')">
                <dim:field mdschema="dc" element="format" qualifier="pages">
                    <xsl:value-of select="substring-before(text(),'p.')"/>
                </dim:field>
            </xsl:when> -->
                    <xsl:when test="starts-with(normalize-space(text()),'p.')">
                        <xsl:choose>
                            <xsl:when test="contains(text(),'-')">
                                <dim:field mdschema="dc" element="bibliographicCitation" qualifier="stpage">
                                    <xsl:value-of select="substring-after(substring-before(text(),'-'),'p.')"/>
                                </dim:field>
                                <dim:field mdschema="dc" element="bibliographicCitation" qualifier="endpage">
                                    <xsl:value-of select="substring-before(substring-after(text(),'-'), ' ')"/>
                                </dim:field>
                            </xsl:when>

							            <xsl:otherwise>
                <dim:field mdschema="dc" element="format" qualifier="pages">
                    <xsl:value-of select="text()"/>
                </dim:field>
            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
            <xsl:otherwise>
                <dim:field mdschema="dc" element="format" qualifier="pages">
                    <xsl:value-of select="text()"/>
                </dim:field>
            </xsl:otherwise>					
                </xsl:choose>
            </xsl:when>
            
            <xsl:otherwise>
                <dim:field mdschema="dc" element="format" qualifier="pages">
                    <xsl:value-of select="text()"/>
                </dim:field>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <xsl:template match="dcterms:medium">
        <dim:field mdschema="dc" element="format" qualifier="mimetype">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dc:language[@scheme='ags:ISO639-1']">
        <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
        <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
        
        <dim:field mdschema="dc" element="language" qualifier="iso">
            <xsl:value-of select="translate(text(), $uppercase, $smallcase)" />
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dc:relation">
        <xsl:apply-templates select="dcterms:isPartOf"/>
    </xsl:template>
    
    <xsl:template match="dcterms:isPartOf[@scheme='dcterms:URI']">
        <dim:field mdschema="dc" element="relation" qualifier="uri">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dcterms:isPartOf">
        <dim:field mdschema="dc" element="relation" qualifier="ispartofseries">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dc:source">
        <dim:field mdschema="dc" element="source">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dc:coverage">
        <xsl:apply-templates select="dcterms:spatial"/>
        <xsl:apply-templates select="dcterms:temporal"/>
        
        <xsl:if test="string-length(text()) > 0">
            <dim:field mdschema="dc" element="coverage" qualifier="spatial">
                <xsl:value-of select="text()"/>
            </dim:field>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="dcterms:spatial">
        <dim:field mdschema="dc" element="coverage" qualifier="spatial">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dcterms:temporal[@scheme='dcterms:Period']">
        <dim:field mdschema="dc" element="coverage" qualifier="temporal">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="dc:rights">
        <xsl:apply-templates select="ags:rightsStatement"/>
    </xsl:template>
    
    <xsl:template match="ags:rightsStatement">
        <dim:field mdschema="dc" element="rights">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="ags:citation">
        <xsl:apply-templates select="ags:citationIdentifier"/>
        <xsl:apply-templates select="ags:citationNumber"/>
        <xsl:apply-templates select="ags:citationTitle"/>
    </xsl:template>
    
    <xsl:template match="ags:citationIdentifier[@scheme='ags:ISSN']">
        <dim:field mdschema="dc" element="identifier" qualifier="issn">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
    
    <xsl:template match="ags:citationNumber">
<!--        <xsl:choose> 
            <xsl:when test="contains(text(), 'vol')"> -->
                <dim:field mdschema="dc" element="bibliographicCitation" qualifier="volume">
                    <xsl:value-of select="text()"/>
                </dim:field>
 <!--           </xsl:when>
            <xsl:otherwise>
                <dim:field mdschema="dc" element="bibliographicCitation" qualifier="volume">
                    <xsl:value-of select="substring-before(text(),'(')"/>
                </dim:field>
                <xsl:if test="string-length(substring-before(substring-after(text(),'('),')')) > 0">
                    <dim:field mdschema="dc" element="bibliographicCitation" qualifier="issue">
                        <xsl:value-of select="substring-before(substring-after(text(),'('),')')"/>
                    </dim:field>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose> -->
        
    </xsl:template>
    
    <xsl:template match="ags:citationTitle">
        <xsl:choose>
            <xsl:when test="count(preceding-sibling::node()[name()=name(current())])=0">
                <dim:field mdschema="dc" element="bibliographicCitation" qualifier="title">
                    <xsl:value-of select="text()"/>
                </dim:field>
            </xsl:when>
            <xsl:otherwise>
                <!-- It is a question what to do if this element have duplications -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="dc:source">
        <dim:field mdschema="dc" element="bibliographicCitation" qualifier="title">
            <xsl:value-of select="text()"/>
        </dim:field>
    </xsl:template>
     
    
    
</xsl:stylesheet>