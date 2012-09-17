<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
                xmlns:dim_fun="http://www.dspace.org/xmlns/dspace/dim_fun"
                xmlns="http://purl.org/dc/xmlns/2008/09/01/dc-ds-xml/"
                xmlns:dcds="http://purl.org/dc/xmlns/2008/09/01/dc-ds-xml/"
                exclude-result-prefixes="dim dim_fun voa3r"
                xmlns:voa3r="http://my.example.com"
                xmlns:saxon="http://icl.com/saxon"
                extension-element-prefixes="saxon">
  <!-- native : any char not in the ASCII set is output as is (as opposed to entity: then character references are used)
       decimal: when char can not be represented in output encoding (impossible for UTF-8), then output decimal char ref (instead of entity or hex)-->
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" saxon:character-representation="native;decimal" /> 

    <xsl:include href="common-functions.xsl"/>
    
    <xsl:include href="voa3r-level4-disseminate-mapping.xsl"/>    
    <xsl:variable name="item" select="/dim:dim"/>


    <xsl:template match="/">
        
        <dcds:descriptionSet>
            <dcds:description>
                <!-- URI MANDATORY -->
                <xsl:attribute name="dcds:resourceURI">
                    <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'identifier', 'uri')"/>
                </xsl:attribute>
                
                
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'title', '')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/title</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'title', '')">
                                <xsl:call-template name="literalValueString">
                                    <xsl:with-param name="lang">
                                        <xsl:value-of select="./@lang"/>
                                    </xsl:with-param>
                                    <xsl:with-param name="contents">
                                        <xsl:value-of select="."/>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:for-each>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'title', 'alternative')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/alternative</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'title', 'alternative')">
                                <xsl:call-template name="literalValueString">
                                    <xsl:with-param name="lang">
                                        <xsl:value-of select="./@lang"/>
                                    </xsl:with-param>
                                    <xsl:with-param name="contents">
                                        <xsl:value-of select="."/>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:for-each>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:variable name="contributors" select="('author', 'editor', 'advisor', 'corpauthor', 'institution')"></xsl:variable>
                <xsl:for-each select="$contributors">
                    <xsl:variable name="contrib" select="."/>
                    <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'contributor', $contrib)">
                        <xsl:variable name="i" select="position()"/>
                        <xsl:call-template name="statement">
                            <xsl:with-param name="propertyURI">http://purl.org/dc/terms/creator</xsl:with-param>
                            <xsl:with-param name="valueRef">
                                <xsl:value-of select="concat($contrib,$i)"/>
                            </xsl:with-param>
                            <xsl:with-param name="contents">
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:for-each>
                

                <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'publisher', '')">
                    <xsl:variable name="i" select="position()"/>
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/publisher</xsl:with-param>
                        <xsl:with-param name="valueRef">
                            <xsl:value-of select="concat('publisher',$i)"/>
                        </xsl:with-param>
                        <xsl:with-param name="contents">
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:for-each>
                

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','date','issued')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/date</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:call-template name="literalValueString">
                                <xsl:with-param name="sesURI">http://purl.org/dc/terms/W3CDTF</xsl:with-param>
                                <xsl:with-param name="contents">
                                    <xsl:value-of select="dim_fun:qdcelements_get($item,'dc','date','issued')"/>
                                </xsl:with-param>
                            </xsl:call-template>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','language','iso')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/language</xsl:with-param>
                        <xsl:with-param name="vesURI">http://purl.org/dc/terms/RFC5646</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:element name="dcds:valueString">
                                <xsl:value-of select="dim_fun:qdcelements_get($item,'dc','language','iso')"/>
                            </xsl:element>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','identifier','uri')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/identifier</xsl:with-param>
                        <xsl:with-param name="valueURI">
                            <xsl:value-of select="dim_fun:qdcelements_get($item,'dc','identifier','uri')"/>
                        </xsl:with-param>
                        <xsl:with-param name="contents">
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','identifier','isbn')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/identifier</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:call-template name="literalValueString">
                                <xsl:with-param name="sesURI">http://voa3r.eu/terms/EncodedSchema</xsl:with-param>
                                <xsl:with-param name="contents">
                                    <xsl:value-of select="concat('ISBN:', dim_fun:qdcelements_get($item,'dc','identifier','isbn'))"/>
                                </xsl:with-param>
                            </xsl:call-template>
                        
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','identifier','issn')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/identifier</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:call-template name="literalValueString">
                                <xsl:with-param name="sesURI">http://voa3r.eu/terms/EncodedSchema</xsl:with-param>
                                <xsl:with-param name="contents">
                                    <xsl:value-of select="concat('ISSN:', dim_fun:qdcelements_get($item,'dc','identifier','issn'))"/>
                                </xsl:with-param>
                            </xsl:call-template>
                        
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>
            
                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','format','mimetype')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/format</xsl:with-param>
                        <xsl:with-param name="vesURI">http://purl.org/dc/terms/IMT</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:element name="dcds:valueString">
                                <xsl:value-of select="dim_fun:qdcelements_get($item,'dc','format','mimetype')"/>
                            </xsl:element>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>
            
                <xsl:if test="dim_fun:qdcelement_nonempty($item,'voa3r','isShownBy','')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://www.europeana.eu/schemas/ese/isShownBy</xsl:with-param>
                        <xsl:with-param name="valueURI">
                            <xsl:value-of select="dim_fun:qdcelements_get($item,'voa3r','isShownBy','')"/>
                        </xsl:with-param>
                        <xsl:with-param name="contents">
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','identifier','uri')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://www.europeana.eu/schemas/ese/isShownAt</xsl:with-param>
                        <xsl:with-param name="valueURI">
                            <xsl:value-of select="dim_fun:qdcelements_get($item,'dc','identifier','uri')"/>
                        </xsl:with-param>
                        <xsl:with-param name="contents">
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:for-each select="dim_fun:qdcelements_get($item,'dc','subject','agrovoc')">
                    <xsl:if test="dim_fun:attribute_nonempty(.,'authority')">
                        <xsl:call-template name="statement">
                            <xsl:with-param name="propertyURI">http://purl.org/dc/terms/subject</xsl:with-param>
                            <xsl:with-param name="valueURI">
                                <xsl:value-of select="dim_fun:attribute_get(.,'authority')"/>
                            </xsl:with-param>
                            <xsl:with-param name="contents">
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:for-each>            

                <xsl:for-each select="dim_fun:qdcelements_get($item,'dc','subject','asfa')">
                    <xsl:if test="dim_fun:attribute_nonempty(.,'authority')">
                        <xsl:call-template name="statement">
                            <xsl:with-param name="propertyURI">http://purl.org/dc/terms/subject</xsl:with-param>
                            <xsl:with-param name="valueURI">
                                <xsl:value-of select="dim_fun:attribute_get(.,'authority')"/>
                            </xsl:with-param>
                            <xsl:with-param name="contents">
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:for-each>
        
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'description', 'abstract')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/description</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'description', 'abstract')">
                                <xsl:call-template name="literalValueString">
                                    <xsl:with-param name="lang">
                                        <xsl:value-of select="./@lang"/>
                                    </xsl:with-param>
                                    <xsl:with-param name="contents">
                                        <xsl:value-of select="."/>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:for-each>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>
        
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'description', 'abstract')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/abstract</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'description', 'abstract')">
                                <xsl:call-template name="literalValueString">
                                    <xsl:with-param name="lang">
                                        <xsl:value-of select="./@lang"/>
                                    </xsl:with-param>
                                    <xsl:with-param name="contents">
                                        <xsl:value-of select="."/>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:for-each>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'voa3r', 'bibliographicCitation', '')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/bibliographicCitation</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:for-each select="dim_fun:qdcelements_get($item, 'voa3r', 'bibliographicCitation', '')">
                                <xsl:call-template name="literalValueString">
                                    <xsl:with-param name="sesURI">http://voa3r.eu/terms/EncodedSchema</xsl:with-param>
                                    <xsl:with-param name="contents">
                                        <xsl:value-of select="concat('BibTEX:', .)"/>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:for-each>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','type','')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/type</xsl:with-param>
                        <xsl:with-param name="vesURI">http://voa3r.eu/terms/ResourceType</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:element name="dcds:valueString">
                                <xsl:value-of select="voa3r:getMappedVOA3RResourceType(dim_fun:qdcelements_get($item,'dc','type',''), dim_fun:qdcelements_get($item,'dc','type','specified'))"/>
                            </xsl:element>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','rights','')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/rights</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:for-each select="dim_fun:qdcelements_get($item,'dc','rights','')">
                                <xsl:call-template name="literalValueString">
                                    <xsl:with-param name="lang">
                                        <xsl:value-of select="./@lang"/>
                                    </xsl:with-param>
                                    <xsl:with-param name="contents">
                                        <xsl:value-of select="."/>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:for-each>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'voa3r','accessRights','')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/accessRights</xsl:with-param>
                        <xsl:with-param name="valueURI">
                            <xsl:value-of select="dim_fun:qdcelements_get($item,'voa3r','accessRights','')"/>
                        </xsl:with-param>
                        <xsl:with-param name="contents">
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'voa3r','license','')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://purl.org/dc/terms/license</xsl:with-param>
                        <xsl:with-param name="valueURI">
                            <xsl:value-of select="dim_fun:qdcelements_get($item,'voa3r','license','')"/>
                        </xsl:with-param>
                        <xsl:with-param name="contents">
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','type','refereed')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://voa3r.eu/terms/reviewStatus</xsl:with-param>
                        <xsl:with-param name="vesURI">http://voa3r.eu/terms/ReviewStatus</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:element name="dcds:valueString">
                                <xsl:value-of select="voa3r:getMappedVOA3RValue('reviewstatus',dim_fun:qdcelements_get($item,'dc','type','refereed'))"/>
                            </xsl:element>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>

                <xsl:if test="dim_fun:qdcelement_nonempty($item,'dc','description','status')">
                    <xsl:call-template name="statement">
                        <xsl:with-param name="propertyURI">http://voa3r.eu/terms/publicationStatus</xsl:with-param>
                        <xsl:with-param name="vesURI">http://voa3r.eu/terms/PublicationStatus</xsl:with-param>
                        <xsl:with-param name="contents">
                            <xsl:element name="dcds:valueString">
                                <xsl:value-of select="voa3r:getMappedVOA3RValue('publicationstatus',dim_fun:qdcelements_get($item,'dc','description','status'))"/>
                            </xsl:element>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:if>


            </dcds:description>

<!--
RESOURCES referenced above
-->

            <xsl:variable name="contributors" select="('author', 'editor', 'advisor')"></xsl:variable>
            <xsl:for-each select="$contributors">
                <xsl:variable name="contrib" select="."/>
                <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'contributor', $contrib)">
                    <xsl:variable name="i" select="position()"/>
                    <xsl:variable name="fullName" select="."/>
                    <xsl:element name="dcds:description">
                        <xsl:attribute name="dcds:resourceId">
                            <xsl:value-of select="concat($contrib,$i)"/>
                        </xsl:attribute>
                        <xsl:call-template name="foaf_person">
                            <xsl:with-param name="lastName">
                                <xsl:value-of select="substring-before($fullName, ', ')"/>
                            </xsl:with-param>
                            <xsl:with-param name="firstName">
                                <xsl:value-of select="substring-after($fullName, ', ')"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:element>
                </xsl:for-each>
            </xsl:for-each>
                
            <xsl:variable name="contributors" select="('corpauthor', 'institution')"></xsl:variable>
            <xsl:for-each select="$contributors">
                <xsl:variable name="contrib" select="."/>
                <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'contributor', $contrib)">
                    <xsl:variable name="i" select="position()"/>
                    <xsl:variable name="orgName" select="."/>
                    <xsl:element name="dcds:description">
                        <xsl:attribute name="dcds:resourceId">
                            <xsl:value-of select="concat($contrib,$i)"/>
                        </xsl:attribute>
                        <xsl:call-template name="foaf_org">
                            <xsl:with-param name="orgName">
                                <xsl:value-of select="$orgName"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:element>
                </xsl:for-each>
            </xsl:for-each>

            <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'publisher', '')">
                <xsl:variable name="i" select="position()"/>
                <xsl:variable name="orgName" select="."/>
                <xsl:element name="dcds:description">
                    <xsl:attribute name="dcds:resourceId">
                        <xsl:value-of select="concat('publisher',$i)"/>
                    </xsl:attribute>
                    <xsl:call-template name="foaf_org">
                        <xsl:with-param name="orgName">
                            <xsl:value-of select="$orgName"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:element>
            </xsl:for-each>

<!--
end of resources referenced above
-->

        </dcds:descriptionSet>    
    </xsl:template>
    
    <!-- TEMPLATE FOAF PERSON
        creates 3 statements: type (fixed=foaf person), and lastName and firstName (parameter)
    -->
    <xsl:template name="foaf_person">
        <xsl:param name="lastName"/>
        <xsl:param name="firstName"/>
        
        <xsl:call-template name="statement">
            <xsl:with-param name="propertyURI">http://purl.org/dc/terms/type</xsl:with-param>
            <xsl:with-param name="valueURI">http://xmlns.com/foaf/0.1/Person</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="statement">
            <xsl:with-param name="propertyURI">http://xmlns.com/foaf/0.1/lastName</xsl:with-param>
            <xsl:with-param name="contents">
                <xsl:call-template name="literalValueString">
                    <xsl:with-param name="contents">
                        <xsl:value-of select="$lastName"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>

        <xsl:call-template name="statement">
            <xsl:with-param name="propertyURI">http://xmlns.com/foaf/0.1/firstName</xsl:with-param>
            <xsl:with-param name="contents">
                <xsl:call-template name="literalValueString">
                    <xsl:with-param name="contents">
                        <xsl:value-of select="$firstName"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <!-- TEMPLATE FOAF ORGANIZATION
        creates 2 statements: type (fixed=foaf organization), and orgName (parameter)
    -->
    <xsl:template name="foaf_org">
        <xsl:param name="orgName"/>
        
        <xsl:call-template name="statement">
            <xsl:with-param name="propertyURI">http://purl.org/dc/terms/type</xsl:with-param>
            <xsl:with-param name="valueURI">http://xmlns.com/foaf/0.1/Organization</xsl:with-param>
        </xsl:call-template>
        
        <xsl:call-template name="statement">
            <xsl:with-param name="propertyURI">http://xmlns.com/foaf/0.1/name</xsl:with-param>
            <xsl:with-param name="contents">
                <xsl:call-template name="literalValueString">
                    <xsl:with-param name="contents">
                        <xsl:value-of select="$orgName"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!--
        TEMPLATE STATEMENT
        creates a dcds:statement with attributes propertyURI, valueRef, valueURI, vesURI (any attribute passed as parameter will have its value used as the value for that attribute)
         and with the given contents
    -->
    <xsl:template name="statement">
        <xsl:param name="propertyURI"/>
        <xsl:param name="valueRef"/>
        <xsl:param name="valueURI"/>
        <xsl:param name="vesURI"/>
        <xsl:param name="contents"/>
        
        <xsl:element name="dcds:statement">
            <xsl:attribute name="dcds:propertyURI">
                <xsl:value-of select="$propertyURI"/>
            </xsl:attribute>
            <xsl:if test="$valueRef">
                <xsl:attribute name="dcds:valueRef">
                    <xsl:value-of select="$valueRef"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="$valueURI">
                <xsl:attribute name="dcds:valueURI">
                    <xsl:value-of select="$valueURI"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="$vesURI">
                <xsl:attribute name="dcds:vesURI">
                    <xsl:value-of select="$vesURI"/>
                </xsl:attribute>
            </xsl:if>
            
            <xsl:copy-of select="$contents"/>
        </xsl:element>
    </xsl:template>
    
    <!--
        TEMPLATE literalValueString
        creates a dcds:literalValueString element with given contents
        attributes lang and/or sesURI are added with given values (or no attributes are added)
    -->
    <xsl:template name="literalValueString">
        <xsl:param name="lang"/>
        <xsl:param name="sesURI"/>
        <xsl:param name="contents"/>
        
        <xsl:element name="dcds:literalValueString">
            <xsl:if test="not(compare($lang,'')=0)">
                <xsl:attribute name="xml:lang">
                    <xsl:value-of select="$lang"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="$sesURI">
                <xsl:attribute name="dcds:sesURI">
                    <xsl:value-of select="$sesURI"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:value-of select="$contents"/>

        </xsl:element>
        
    </xsl:template>
    
    <!-- FUNCTION getMappedVOA3RValue
        given the name of a context (e.g., reviewstatus, publicationstatus)
        and the internal value in that context,
        returns
        the VOA3R value registered as a translation for this internal value
        (if more than one is found, the first mapping is returned.
         if no mapping is found, the internal-value is returned)
    -->
    <xsl:function name="voa3r:getMappedVOA3RValue">
        <xsl:param name="context-name"/>
        <xsl:param name="internal-value"/>
        
        <xsl:variable name="result">
            <xsl:value-of select="$mapping/*[local-name()=$context-name]/voa3r[internal-value[lower-case(.)=lower-case($internal-value)]][position()=1]/@value"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="string($result)">
                <xsl:value-of select="$result"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$internal-value"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!-- FUNCTION getMappedVOA3RResourceType
        given the type and type specified of the given item
        returns
        the VOA3R value registered as a translation for this type and type specified
        (if more than one is found, the first mapping is returned.
         if no mapping is found, the internal type is returned)
    -->
    <xsl:function name="voa3r:getMappedVOA3RResourceType">
        <xsl:param name="typename"/>
        <xsl:param name="subtype"/>
        
        <xsl:variable name="result_subtype">
            <xsl:value-of select="$mapping/resourcetype/voa3r[internal-value[lower-case(type)=lower-case($typename) and lower-case(subtype)=lower-case($subtype)]][position()=1]/@value"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="string($result_subtype)">
                <xsl:value-of select="$result_subtype"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="result_type">
                    <xsl:value-of select="$mapping/resourcetype/voa3r[internal-value[lower-case(type)=lower-case($typename)]][position()=1]/@value"/>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="string($result_type)">
                        <xsl:value-of select="$result_type"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$typename"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</xsl:stylesheet>
