<?xml version="1.0" encoding="UTF-8"?>

<!-- COMMON FUNCTIONS for use in XSL crosswalks -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dim_fun="http://www.dspace.org/xmlns/dspace/dim_fun"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:functx="http://www.functx.com">

  <!-- ==================== FUNCTION QDCELEMENTS_GET ==================== -->
  <!-- returns the DIM elements with given schema, element and qualifier. 
        When qualifier is empty or empty string, only schema and element are used. 
        I.e., only DIM fields with empty qualifier are returned. -->
    <xsl:function name="dim_fun:qdcelements_get">
        <xsl:param name="item"/>
        <xsl:param name="schema"/>
        <xsl:param name="element"/>
        <xsl:param name="qualifier"/>
    
        <xsl:choose>
            <xsl:when test="not(string($qualifier)) or compare($qualifier,'')=0">
                <xsl:copy-of select="$item/dim:field[@mdschema=$schema and @element=$element and empty(@qualifier)]"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$item/dim:field[@mdschema=$schema and @element=$element and @qualifier=$qualifier]"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
  <!-- ================== END FUNCTION QDCELEMENTS_GET ================== -->

  <!-- ==================== FUNCTION QDCELEMENT_NONEMPTY ==================== -->
  <!-- returns true when QDCELEMENTS_GET returns at least one field -->
    <xsl:function name="dim_fun:qdcelement_nonempty">
        <xsl:param name="item"/>
        <xsl:param name="schema"/>
        <xsl:param name="element"/>
        <xsl:param name="qualifier"/>
    
        <xsl:choose>
            <xsl:when test="not(empty(dim_fun:qdcelements_get($item, $schema, $element, $qualifier)))">
                true()
            </xsl:when>
        </xsl:choose>
    </xsl:function>
  <!-- ================== END FUNCTION QDCELEMENT_NONEMPTY ================== -->


  <!-- ==================== FUNCTION ATTRIBUTE_GET ==================== -->
  <!-- returns the string content of the DIM attribute with the given name from the given field-->
    <xsl:function name="dim_fun:attribute_get">
        <xsl:param name="field"/>
        <xsl:param name="attributename"/>
    
        <xsl:value-of select="$field/@*[name()=$attributename]"/>
    </xsl:function>
  <!-- ================== END FUNCTION ATTRIBUTE_GET ================== -->

  <!-- ==================== FUNCTION ATTRIBUTE_NONEMPTY ==================== -->
  <!-- returns true if the given DIM field has a nonempty value for the attribute with the given name -->
    <xsl:function name="dim_fun:attribute_nonempty">
        <xsl:param name="field"/>
        <xsl:param name="attributename"/>

        <xsl:choose>
            <xsl:when test="not(compare(dim_fun:attribute_get($field, $attributename),'')=0)"> 
                true()
            </xsl:when>
        </xsl:choose>
    </xsl:function>
  <!-- ================== END FUNCTION ATTRIBUTE_NONEMPTY ================== -->

  <!-- ==================== FUNCTION pad-string-to-length ==================== -->
    <xsl:function name="functx:pad-string-to-length" as="xs:string"  >
        <xsl:param name="stringToPad" as="xs:string?"/> 
        <xsl:param name="padChar" as="xs:string"/> 
        <xsl:param name="length" as="xs:integer"/>
        <xsl:param name="leftOrRight" as="xs:string">right</xsl:param>
 
        <xsl:choose>
            <xsl:when test="compare($leftOrRight,'right')=0">
                <xsl:sequence select=" 
   substring(
     string-join (
       ($stringToPad, for $i in (1 to $length) return $padChar)
       ,'')
    ,1,$length)
 "/>            
            </xsl:when>
            <xsl:when test="compare($leftOrRight,'left')=0">
                <xsl:sequence select=" 
   substring(
     string-join (
       (for $i in (1 to $length) return $padChar, $stringToPad)
       ,'')
    ,string-length($stringToPad)+1,$length + string-length($stringToPad))
 "/>
            </xsl:when>
        </xsl:choose>
   
    </xsl:function>
  <!-- ================== END FUNCTION pad-string-to-length ================== -->

</xsl:stylesheet>
