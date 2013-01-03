<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:variable name="ns_dcterms">http://purl.org/dc/terms/</xsl:variable>
    <xsl:variable name="ns_xsi">http://www.w3.org/2001/XMLSchema-instance</xsl:variable>
    <xsl:variable name="ns_ese">http://www.europeana.eu/schemas/ese/</xsl:variable>
    <xsl:variable name="ns_xml">http://www.w3.org/TR/xml/</xsl:variable>
    <xsl:variable name="ns_foaf">http://xmlns.com/foaf/spec/</xsl:variable>
    <xsl:variable name="ns_voa3r">http://www.voa3r.eu/terms/</xsl:variable>

    <xsl:variable name="mapping">
        <createElements>
            <createElement name="dcterms:title" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="title">
                </basedOn>
                <addAttribute name="lang" namespace="{$ns_xml}">
                    <useDC attribute="lang"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:alternative" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="title" qualifier="alternative">
                </basedOn>
                <addAttribute name="lang" namespace="{$ns_xml}">
                    <useDC attribute="lang"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:creator" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="contributor" qualifier="author">
                </basedOn>
            </createElement>
            <createElement name="dcterms:creator" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="contributor" qualifier="advisor">
                </basedOn>
            </createElement>
            <createElement name="dcterms:creator" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="contributor" qualifier="editor">
                </basedOn>
            </createElement>
            <createElement name="dcterms:creator" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="contributor" qualifier="corpauthor">
                </basedOn>
            </createElement>
            <createElement name="dcterms:creator" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="contributor" qualifier="institution">
                </basedOn>
            </createElement>
            <createElement name="dcterms:publisher" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="publisher">
                </basedOn>
            </createElement>
            <createElement name="dcterms:date" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="date" qualifier="issued">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:W3CDTF"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:language" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="language" qualifier="iso">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:ISO639-2"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:identifier" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="identifier" qualifier="uri">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:URI"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:identifier" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="identifier" qualifier="isbn">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:ISBN"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:identifier" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="identifier" qualifier="issn">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:ISSN"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:format" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="format" qualifier="mimetype">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:IMT"/>
                </addAttribute>
            </createElement>
            <createElement name="ese:isShownBy" namespace="{$ns_ese}">
                <basedOn mdschema="voa3r" element="isShownBy">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:URI"/>
                </addAttribute>
            </createElement>
            <createElement name="ese:isShownAt" namespace="{$ns_ese}">
                <basedOn mdschema="dc" element="identifier" qualifier="uri">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:URI"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:subject" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="subject" qualifier="agrovoc">
                    <useAttribute name="authority"/>
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:URI"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:subject" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="subject" qualifier="asfa">
                    <useAttribute name="authority"/>
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:URI"/>
                </addAttribute>
            </createElement>
            <createElement name="voa3r:description" namespace="{$ns_voa3r}">
                <basedOn mdschema="dc" element="description" qualifier="abstract">
                </basedOn>
                <addAttribute name="lang" namespace="{$ns_xml}">
                    <useDC attribute="lang"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:abstract" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="description" qualifier="abstract">
                </basedOn>
                <addAttribute name="lang" namespace="{$ns_xml}">
                    <useDC attribute="lang"/>
                </addAttribute>
            </createElement>
            <createElement name="voa3r:bibliographicCitation" namespace="{$ns_voa3r}">
                <basedOn mdschema="voa3r" element="bibliographicCitation">
                </basedOn>
            </createElement>
            <createElement name="dcterms:type" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="type">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="voa3r:ResourceType"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:rights" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="rights">
                </basedOn>
                <addAttribute name="lang" namespace="{$ns_xml}">
                    <useDC attribute="lang"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:accessRights" namespace="{$ns_dcterms}">
                <basedOn mdschema="voa3r" element="accessRights">
                </basedOn>
                <addAttribute name="lang" namespace="{$ns_xml}">
                    <fixed value="en"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:license" namespace="{$ns_dcterms}">
                <basedOn mdschema="voa3r" element="license">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:URI"/>
                </addAttribute>
            </createElement>
            <createElement name="voa3r:reviewStatus" namespace="{$ns_voa3r}">
                <basedOn mdschema="dc" element="type" qualifier="refereed">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="voa3r:ReviewStatus"/>
                </addAttribute>
            </createElement>
            <createElement name="voa3r:publicationStatus" namespace="{$ns_voa3r}">
                <basedOn mdschema="dc" element="description" qualifier="status">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="voa3r:PublicationStatus"/>
                </addAttribute>
            </createElement>
            <createElement name="dcterms:relation" namespace="{$ns_dcterms}">
                <basedOn mdschema="dc" element="relation" qualifier="uri">
                </basedOn>
                <addAttribute name="xsi:type" namespace="{$ns_xsi}">
                    <fixed value="dcterms:URI"/>
                </addAttribute>
            </createElement>
        </createElements>
    </xsl:variable>
    
</xsl:stylesheet>
