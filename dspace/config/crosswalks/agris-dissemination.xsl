<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
                xmlns:dim_fun="http://www.dspace.org/xmlns/dspace/dim_fun"
                xmlns:ags="http://purl.org/agmes/1.1/"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:dcterms="http://purl.org/dc/terms/"
                xmlns:agls="http://www.naa.gov.au/recordkeeping/gov_online/agls/1.2"
                xmlns:ags_fun="http://my.example.com"
                xmlns:functx="http://www.functx.com"
                exclude-result-prefixes="dim dim_fun xs xsi ags_fun functx"
                xmlns:saxon="http://icl.com/saxon"
                extension-element-prefixes="saxon">
  <!-- native : any char not in the ASCII set is output as is (as opposed to entity: then character references are used)
       decimal: when char can not be represented in output encoding (impossible for UTF-8), then output decimal char ref (instead of entity or hex)-->
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" saxon:character-representation="native;decimal" /> 

    <xsl:include href="common-functions.xsl"/>
    
    <xsl:variable name="item" select="/dim:dim"/>


    <xsl:template match="/">
	  <ags:resources>
        <ags:resource>
            <xsl:call-template name="addARNattribute"/>
            <xsl:call-template name="addTitles"/>
            <xsl:call-template name="addCreators"/>
            <xsl:call-template name="addPublishers"/>
            <xsl:call-template name="addDates"/>
            <xsl:call-template name="addSubjects"/>
            <xsl:call-template name="addDescriptions"/>
            <xsl:call-template name="addIdentifiers"/>
            <xsl:call-template name="addType"/>
            <xsl:call-template name="addFormats"/>
            <xsl:call-template name="addLanguage"/>
            <xsl:call-template name="addRelations"/>
            <xsl:call-template name="addSource"/>
            <xsl:call-template name="addCoverage"/>
            <xsl:call-template name="addRights"/>
            <xsl:call-template name="addCitation"/>
        </ags:resource>
	  </ags:resources>
    </xsl:template>
    
    <xsl:template name="addCitation">
        <xsl:variable name="citations" select="dim_fun:qdcelements_get($item, 'dc', 'identifier', 'issn')
                    | dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'title')
                    | dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'volume')"/>
        
        <xsl:if test="not(empty($citations))">
            <xsl:element name="ags:citation">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'identifier', 'issn')"/>
                    <xsl:with-param name="elementName">ags:citationIdentifier</xsl:with-param>
                    <xsl:with-param name="attName">scheme</xsl:with-param>
                    <xsl:with-param name="attConstantValue">ags:ISSN</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'title')"/>
                    <xsl:with-param name="elementName">ags:citationTitle</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="addCitationVolumeIssue"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addCitationVolumeIssue">
        <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'bibliographicCitation', 'volume')">
            <ags:citationNumber>
                <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'volume')"/>
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'bibliographicCitation', 'issue')">
                    <xsl:text> (</xsl:text>
                    <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'issue')"/>
                    <xsl:text>)</xsl:text>
                </xsl:if>
            </ags:citationNumber>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addRights">
        <xsl:variable name="rights" select="dim_fun:qdcelements_get($item, 'dc', 'rights', '')"/>
        
        <xsl:if test="not(empty($rights))">
            <xsl:element name="dc:rights">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'rights', '')"/>
                    <xsl:with-param name="elementName">ags:rightsStatement</xsl:with-param>
                </xsl:call-template>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addCoverage">
        <xsl:variable name="coverages" select="dim_fun:qdcelements_get($item, 'dc', 'coverage', 'spatial')
                    | dim_fun:qdcelements_get($item, 'dc', 'coverage', 'temporal')"/>
        
        <xsl:if test="not(empty($coverages))">
            <xsl:element name="dc:coverage">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'coverage', 'spatial')"/>
                    <xsl:with-param name="elementName">dcterms:spatial</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'coverage', 'temporal')"/>
                    <xsl:with-param name="elementName">dcterms:temporal</xsl:with-param>
                    <xsl:with-param name="attName">scheme</xsl:with-param>
                    <xsl:with-param name="attConstantValue">dcterms:Period</xsl:with-param>
                </xsl:call-template>
            </xsl:element>
        </xsl:if>
    </xsl:template>
        
    <xsl:template name="addSource">
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'source', '')"/>
            <xsl:with-param name="elementName">dc:source</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="addRelations">
        <xsl:variable name="relations" select="dim_fun:qdcelements_get($item,'dc', 'relation', 'uri')
                    | dim_fun:qdcelements_get($item,'dc', 'relation', 'data')"/>
        
        <xsl:if test="not(empty($relations))">
            <xsl:element name="dc:relation">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item,'dc', 'relation', 'uri')"/>
                    <xsl:with-param name="elementName">dcterms:hasVersion</xsl:with-param>
                    <xsl:with-param name="attName">scheme</xsl:with-param>
                    <xsl:with-param name="attConstantValue">dcterms:URI</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item,'dc', 'relation', 'data')"/>
                    <xsl:with-param name="elementName">dcterms:hasPart</xsl:with-param>
                    <xsl:with-param name="attName">scheme</xsl:with-param>
                    <xsl:with-param name="attConstantValue">dcterms:URI</xsl:with-param>
                </xsl:call-template>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addIsPartOfSeries">
        <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'relation', 'ispartofseries')">
            <xsl:element name="ags:descriptionNotes">
                <xsl:text>Series: </xsl:text>
                <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'relation', 'ispartofseries')"/>
                <!-- if there is relation.ispartofseriesnr, append a comma and append the seriesnr -->
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'relation', 'ispartofseriesnr')">
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'relation', 'ispartofseriesnr')"/>
                </xsl:if>
            </xsl:element>
        </xsl:if>
    </xsl:template>
        
    <xsl:template name="addLanguage">
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'language', 'iso')"/>
            <xsl:with-param name="elementName">dc:language</xsl:with-param>
            <xsl:with-param name="attName">scheme</xsl:with-param>
            <xsl:with-param name="attConstantValue">ags:ISO639-1</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="addFormats">
        <xsl:variable name="formats" select="dim_fun:qdcelements_get($item, 'dc', 'format', 'pages')
                    | dim_fun:qdcelements_get($item, 'dc', 'format', 'mimetype')
                    | dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'stpage')"/>
        
        <xsl:if test="not(empty($formats))">
            <xsl:element name="dc:format">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'format', 'pages')"/>
                    <xsl:with-param name="elementName">dcterms:extent</xsl:with-param>
                    <xsl:with-param name="suffix"> p.</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="distinct-values(dim_fun:qdcelements_get($item, 'dc', 'format', 'mimetype'))"/>
                    <xsl:with-param name="elementName">dcterms:medium</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="addFormatPageRange"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addFormatPageRange">
        <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'bibliographicCitation', 'stpage')">
            <xsl:element name="dcterms:extent">
                <xsl:text>p. </xsl:text>
                <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'stpage')"/>
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'bibliographicCitation', 'endpage')">
                    <xsl:text>-</xsl:text>
                    <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'endpage')"/>
                </xsl:if>
            </xsl:element>
        </xsl:if>
    </xsl:template>
        
    <xsl:template name="addType">
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'type', '')"/>
            <xsl:with-param name="elementName">dc:type</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="addIdentifiers">
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'identifier', 'isbn')"/>
            <xsl:with-param name="elementName">dc:identifier</xsl:with-param>
            <xsl:with-param name="attName">scheme</xsl:with-param>
            <xsl:with-param name="attConstantValue">ags:ISBN</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'identifier', 'uri')"/>
            <xsl:with-param name="elementName">dc:identifier</xsl:with-param>
            <xsl:with-param name="attName">scheme</xsl:with-param>
            <xsl:with-param name="attConstantValue">dcterms:URI</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'relation', 'doi')"/>
            <xsl:with-param name="elementName">dc:identifier</xsl:with-param>
            <xsl:with-param name="attName">scheme</xsl:with-param>
            <xsl:with-param name="attConstantValue">ags:DOI</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
        
    <xsl:template name="addDescriptions">
        <xsl:variable name="descriptionNotes" select="dim_fun:qdcelements_get($item, 'dc', 'contributor', 'advisor') 
                | dim_fun:qdcelements_get($item, 'dc', 'description', 'notes') 
                | dim_fun:qdcelements_get($item, 'dc', 'description', 'sponsorship')
                | dim_fun:qdcelements_get($item, 'dc', 'description', 'status')
                | dim_fun:qdcelements_get($item, 'dc', 'type', 'refereed')
                | dim_fun:qdcelements_get($item, 'dc', 'relation', 'ispartofseries')
                | dim_fun:qdcelements_get($item, 'dc', 'description', 'abstract')"/>

        <xsl:if test="not(empty($descriptionNotes))">
            <xsl:element name="dc:description">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'contributor', 'advisor')"/>
                    <xsl:with-param name="elementName">ags:descriptionNotes</xsl:with-param>
                    <xsl:with-param name="prefix">Advisor: </xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'description', 'abstract')"/>
                    <xsl:with-param name="elementName">dcterms:abstract</xsl:with-param>
		    <xsl:with-param name="attName">xml:lang</xsl:with-param>
		    <xsl:with-param name="attValueAttName">lang</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'description', 'notes')"/>
                    <xsl:with-param name="elementName">ags:descriptionNotes</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'description', 'sponsorship')"/>
                    <xsl:with-param name="elementName">ags:descriptionNotes</xsl:with-param>
                    <xsl:with-param name="prefix">Funding Organizations: </xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'description', 'status')"/>
                    <xsl:with-param name="elementName">ags:descriptionNotes</xsl:with-param>
                    <xsl:with-param name="prefix">Status: </xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'type', 'refereed')"/>
                    <xsl:with-param name="elementName">ags:descriptionNotes</xsl:with-param>
                    <xsl:with-param name="prefix">Status: </xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="addIsPartOfSeries"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
        
    <xsl:template name="addSubjects">
        <xsl:variable name="subjects" select="dim_fun:qdcelements_get($item, 'dc', 'subject', 'agrovoc') 
                    | dim_fun:qdcelements_get($item, 'dc', 'subject', 'asfa')"/>
                    
        <!-- add free key words from dc.description.other -->
        <xsl:call-template name="addFreeKeyWords"/>
        
        <xsl:if test="not(empty($subjects))">
            <xsl:element name="dc:subject">
 <!--               <xsl:call-template name="addSubjectURIs"/> --> <!-- currently, harvesters do not like URIs (?) -->
                <xsl:call-template name="addSubjectKeyWords"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addFreeKeyWords">
        <!-- add free key words from dc.description.other -->
        <!-- for each value, use language if available, split on comma and semicolon, and create a dc:subject for each part -->
        <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'description', 'other')">
            <xsl:variable name="field" select="."/>
            <xsl:for-each select="tokenize($field, '[,;]+')">
                <dc:subject>
                    <xsl:if test="string($field/@lang)">
                        <xsl:attribute name="xml:lang" select="$field/@lang"/>
                    </xsl:if>
                    <xsl:value-of select="normalize-space(.)"/>
                </dc:subject>
            </xsl:for-each>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="addSubjectKeyWords">
        <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'subject', 'agrovoc')">
            <ags:subjectThesaurus scheme="ags:AGROVOC">
                <xsl:if test="string(@lang)">
                    <xsl:attribute name="xml:lang"><xsl:value-of select="@lang"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="."/>
            </ags:subjectThesaurus>
        </xsl:for-each>
        <xsl:for-each select="dim_fun:qdcelements_get($item, 'dc', 'subject', 'asfa')">
            <ags:subjectThesaurus scheme="ags:ASFAT">
                <xsl:if test="string(@lang)">
                    <xsl:attribute name="xml:lang"><xsl:value-of select="@lang"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="."/>
            </ags:subjectThesaurus>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="addSubjectURIs">
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="distinct-values(dim_fun:qdcelements_get($item, 'dc', 'subject', 'agrovoc')/@authority)"/>
            <xsl:with-param name="elementName">ags:subjectThesaurus</xsl:with-param>
            <xsl:with-param name="attName">scheme</xsl:with-param>
            <xsl:with-param name="attConstantValue">ags:AGROVOC</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="distinct-values(dim_fun:qdcelements_get($item, 'dc', 'subject', 'asfa')/@authority)"/>
            <xsl:with-param name="elementName">ags:subjectThesaurus</xsl:with-param>
            <xsl:with-param name="attName">scheme</xsl:with-param>
            <xsl:with-param name="attConstantValue">ags:ASFAT</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="addDates">
        <xsl:variable name="dates" select="dim_fun:qdcelements_get($item, 'dc', 'date', 'issued')"/>
        <xsl:if test="not(empty($dates))">
            <xsl:element name="dc:date">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="$dates"/>
                    <xsl:with-param name="elementName">dcterms:dateIssued</xsl:with-param>
                </xsl:call-template>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addPublishers">
        <xsl:variable name="publishers" select="dim_fun:qdcelements_get($item, 'dc', 'publisher', '')"/>
        <xsl:if test="not(empty($publishers))">
            <xsl:element name="dc:publisher">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="$publishers"/>
                    <xsl:with-param name="elementName">ags:publisherName</xsl:with-param>
                </xsl:call-template>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addCreators">
        <xsl:if test="not(empty(dim_fun:qdcelements_get($item, 'dc', 'contributor', 'author') | dim_fun:qdcelements_get($item, 'dc', 'contributor', 'editor') | dim_fun:qdcelements_get($item, 'dc', 'creator', '') | dim_fun:qdcelements_get($item, 'dc', 'contributor', 'corpauthor') | dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'conferencename')))">
            <xsl:element name="dc:creator">
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'contributor', 'author') | dim_fun:qdcelements_get($item, 'dc', 'contributor', 'editor') | dim_fun:qdcelements_get($item, 'dc', 'creator', '')"/>
                    <xsl:with-param name="elementName">ags:creatorPersonal</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="elementConstruction">
                    <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'contributor', 'corpauthor')"/>
                    <xsl:with-param name="elementName">ags:creatorCorporate</xsl:with-param>
                </xsl:call-template>
                <xsl:call-template name="addCreatorConference"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="addCreatorConference">
        <xsl:variable name="conference" select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'conferencename')"/>
        
        <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'bibliographicCitation', 'conferencename')">
            <xsl:element name="ags:creatorConference">
                <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'conferencename')"/>
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'bibliographicCitation', 'conferenceplace')">
                    <xsl:text>, </xsl:text>
                    <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'conferenceplace')"/>
                </xsl:if>
                <xsl:if test="dim_fun:qdcelement_nonempty($item, 'dc', 'bibliographicCitation', 'conferencedate')">
                    <xsl:text> - </xsl:text>
                    <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'bibliographicCitation', 'conferencedate')"/>
                </xsl:if>
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <xsl:template name="addTitles">
        <xsl:call-template name="elementConstruction">
            <xsl:with-param name="fields" select="dim_fun:qdcelements_get($item, 'dc', 'title', '') | dim_fun:qdcelements_get($item, 'dc', 'title', 'alternative')"/>
            <xsl:with-param name="elementName">dc:title</xsl:with-param>
            <xsl:with-param name="attName">xml:lang</xsl:with-param>
            <xsl:with-param name="attValueAttName">lang</xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <!-- TEMPLATE elementConstruction 
        creates an element for each node in the given fields expression.
        the element has the provided name, with content of each field, possibly prefixed or suffixed with the provided string
        each element can also have an attribute with a provided name and with a value that either equals the provided constant value, or the value
          for the attribute with the given name of the context dim field.
    -->
    <xsl:template name="elementConstruction">
        <xsl:param name="fields"/> <!-- the DIM:field elements for which to create new elements -->
        <xsl:param name="elementName"/> <!-- the name of the new elements -->
        <xsl:param name="attName"/> <!-- the name of the attribute to add to each new element -->
        <xsl:param name="attConstantValue"/> <!-- the value to use for the attribute (same for all elements) -->
        <xsl:param name="attValueAttName"/> <!-- the name of the attribute of the field that will be used as a value for thet attribute 'attName' of the new element. Be sure to specify only one of attConstantValue and attValueAttName. -->
        <xsl:param name="prefix"/> <!-- the prefix and / or suffix to use as the text content for the new element (the content of the field is always included.) -->
        <xsl:param name="suffix"/>
        
        <xsl:for-each select="$fields">
            <xsl:element name="{$elementName}"> <!-- create the element -->
                <xsl:if test="string($attName)"> <!-- add attribute if needed -->
                    <xsl:choose> <!-- constant attribute value has precedence over dynamic value -->
                        <xsl:when test="string($attConstantValue)">
                            <xsl:attribute name="{$attName}">
                                <xsl:value-of select="$attConstantValue"/>
                            </xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:if test="dim_fun:attribute_nonempty(., $attValueAttName)"> <!-- only add dynamic attribute value if not empty -->
                                <xsl:attribute name="{$attName}">
                                    <xsl:value-of select="dim_fun:attribute_get(., $attValueAttName)"/>
                                </xsl:attribute>
                            </xsl:if>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <!-- the content: prefix, value, suffix -->
                <xsl:value-of select="$prefix"/>
                <xsl:value-of select="."/>
                <xsl:value-of select="$suffix"/>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>
    
    <!-- template addARNattribute
         when dc.identifier.arn is specified for the given item, its value is returned as an ARN attribute
         if not, an ARN attribute is created with the value : 'BE' + dateIssued + '7' + handleId (where handleId is left-padded with 0s to have a 5-length handle)
            -->
    <xsl:template name="addARNattribute">
        <xsl:choose>
            <xsl:when test="dim_fun:qdcelement_nonempty($item, 'dc', 'identifier', 'arn')">
                <xsl:attribute name="ags:ARN">
                    <xsl:value-of select="dim_fun:qdcelements_get($item, 'dc', 'identifier', 'arn')"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
                <xsl:attribute name="ags:ARN">
                    <xsl:value-of select="ags_fun:createARN($item)"/>
                </xsl:attribute>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- function ags_fun:createARN
         returned value : 'BE' + dateIssued + '7' + handleId (where handleId is left-padded with 0s to have a 5-lenght handle)
            -->
    <xsl:function name="ags_fun:createARN">
        <xsl:param name="item"/>
        
        <xsl:variable name="countryCode">BE</xsl:variable>
        <xsl:variable name="institCode">7</xsl:variable>
        <xsl:variable name="handlePrefix">http://hdl.handle.net/1834/</xsl:variable>
        
        <xsl:value-of select="concat($countryCode, 
                                concat( dim_fun:qdcelements_get($item, 'dc', 'date', 'issued'),
                                concat( $institCode,
                                functx:pad-string-to-length(replace( dim_fun:qdcelements_get($item, 'dc', 'identifier', 'uri'), $handlePrefix, ''),
                                                            '0', 5, 'left')
                                )))"/>
    </xsl:function>
    

</xsl:stylesheet>
