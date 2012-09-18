<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
                xmlns:dcterms="http://purl.org/dc/terms/"  
                xmlns:ese="http://www.europeana.eu/schemas/ese/" 
                xmlns:ags="http://www.purl.org/agmes/agrisap/schema" 
                xmlns:marcrel="http://www.loc.gov/marc.relators/"
                xmlns:foaf="http://xmlns.com/foaf/spec/"
                xmlns:dim_fun="http://www.dspace.org/xmlns/dspace/dim_fun"
                xmlns:voa3r="http://www.voa3r.eu/terms/"
                xmlns="http://www.voa3r.eu/terms/"
                exclude-result-prefixes="dim dim_fun"
                xmlns:saxon="http://icl.com/saxon"
                extension-element-prefixes="saxon">
  <!-- native : any char not in the ASCII set is output as is (as opposed to entity: then character references are used)
       decimal: when char can not be represented in output encoding (impossible for UTF-8), then output decimal char ref (instead of entity or hex)-->
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" saxon:character-representation="native;decimal" /> 

    <xsl:include href="common-templates.xsl"/>
    
    <xsl:include href="voa3r-disseminate-mapping.xsl"/>
    
    <xsl:template match="/dim:dim">
        <xsl:variable name="item" select="."/>
        <resource xsi:schemaLocation="http://www.voa3r.eu/terms/ http://www.voa3r.eu/terms/VOA3R-level2_SLU_2.xsd">
            <!-- output single occurrence elements, whose content is based on a single dim:field (DC field) -->
            <xsl:call-template name="xform_dim_based_on_createElements">
                <xsl:with-param name="createElements" select="$mapping/createElements"/>
                <xsl:with-param name="item" select="$item"/>
            </xsl:call-template>
        
            <!-- output other elements: elements whose content is a combination of multiple DC fields -->
            
            <!-- elements whose content is a combination of multiple DC fields -->
            <xsl:call-template name="relation_ispartof">
                <xsl:with-param name="item" select="$item"/>
            </xsl:call-template>
        </resource>
    </xsl:template>
    
    
    <!-- ================= TEMPLATE personalAuthors ================================== -->
    <!-- dc.contributor.{author, editor, advisor} into foaf:Person with lastName and firstName -->
    <xsl:template name="personalAuthors">
        <xsl:param name="item"/>
        
        <xsl:variable name="contributors" select="('author', 'editor', 'advisor')"></xsl:variable>
        
        <xsl:for-each select="$contributors">
            <xsl:variable name="contrib" select="."/>
            <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'contributor', $contrib)">
                <xsl:element name="dcterms:creator" namespace="{$ns_dcterms}">
                    <xsl:element name="foaf:Person" namespace="{$ns_foaf}">
                    <xsl:variable name="fullName" select="."/>
                        <xsl:choose>
                            <xsl:when test="contains($fullName, ', ')">
                                <xsl:element name="foaf:lastName" namespace="{$ns_foaf}">
                                    <xsl:value-of select="substring-before($fullName, ', ')"/>
                                </xsl:element>
                                <xsl:element name="foaf:firstName" namespace="{$ns_foaf}">
                                    <xsl:value-of select="substring-after($fullName, ', ')"/>
                                </xsl:element>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:element name="foaf:lastName" namespace="{$ns_foaf}">
                                    <xsl:value-of select="$fullName"/>
                                </xsl:element>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:element>
                </xsl:element>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>
    <!-- ================= end TEMPLATE personalAuthors ================================== -->
    
    <!-- ================= TEMPLATE organizations and organizational authors ================================== -->
    <!-- dc.contributor.{corpauthor, institution} into foaf:Organization with name -->
    <xsl:template name="organizationalAuthors">
        <xsl:param name="item"/>
        
        <xsl:variable name="contributors" select="('corpauthor', 'institution')"></xsl:variable>
        
        <xsl:for-each select="$contributors">
            <xsl:variable name="contrib" select="."/>
            <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'contributor', $contrib)">
                <xsl:element name="dcterms:creator" namespace="{$ns_dcterms}">
                    <xsl:element name="foaf:Organization" namespace="{$ns_foaf}">
                        <xsl:element name="foaf:name" namespace="{$ns_foaf}">
                            <xsl:value-of select="."/>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>
    <!-- ================= end TEMPLATE organizations and organizational authors ================================== -->

    <!-- ================= TEMPLATE publisher ================================== -->
    <!-- dc.publisher into foaf:Organization with name -->
    <xsl:template name="publisher">
        <xsl:param name="item"/>
            <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'publisher', '')">
                <xsl:element name="dcterms:publisher" namespace="{$ns_dcterms}">
                    <xsl:element name="foaf:Organization" namespace="{$ns_foaf}">
                        <xsl:element name="foaf:name" namespace="{$ns_foaf}">
                            <xsl:value-of select="."/>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>
            </xsl:for-each>
    </xsl:template>
    <!-- ================= end TEMPLATE publisher ================================== -->

    <!-- ================= TEMPLATE relation_ispartof ================================== -->
    <!-- combination of dc.relation.ispartofseries and dc.relation.ispartofseriesnr into
         dcterms:isPartOf -->
    <xsl:template name="relation_ispartof">
        <xsl:param name="item"/>
        <!-- only output isPartOf when there is a value for relation.ispartofseries -->
        <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'relation', 'ispartofseries')">
            <xsl:element name="dcterms:isPartOf" namespace="{$ns_dcterms}">
                <xsl:value-of select="dim_fun:qdcfield_get($item, 'dc', 'relation', 'ispartofseries')"/>
                <!-- if there is relation.ispartofseriesnr, append a comma and append the seriesnr -->
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'relation', 'ispartofseriesnr')">
                    <xsl:text>. </xsl:text>
                    <xsl:value-of select="dim_fun:qdcfield_get($item, 'dc', 'relation', 'ispartofseriesnr')"/>
                </xsl:if>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!-- ================= end TEMPLATE relation_ispartof ================================== -->

</xsl:stylesheet>
