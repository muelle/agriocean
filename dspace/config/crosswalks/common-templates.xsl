<?xml version="1.0" encoding="UTF-8"?>

<!-- COMMON TEMPLATES for use in XSL crosswalks -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
                xmlns:dim_fun="http://www.dspace.org/xmlns/dspace/dim_fun">

<xsl:include href="common-functions.xsl"/>

    <xsl:template name="output_elements_with_single_attribute">
        <xsl:param name="item"/> <!-- the dim element == the item -->
        <xsl:param name="mdSchema"/> <!-- which DC field to use = "defining DC field"-->
        <xsl:param name="element"/>
        <xsl:param name="qualifier"/>
        
        <xsl:param name="outname"/> <!-- the name of the output element -->
        <xsl:param name="outnamespace"/> <!-- the namespace of the output element  -->
        
        <xsl:param name="outcontent_dimattribute"/> <!-- the name of the attribute of the DC field element whose value to use as the content of the output element. If empty, the content of the defining DC field is used -->
        <xsl:param name="outattrname"/> <!-- the name of the attribute to add to the output element (if empty, no attribute is added) -->
        <xsl:param name="outattrnamespace"/> <!-- the namespace of outattrname  -->
        <xsl:param name="outattrvalue_dimattribute"/> <!-- the name of the attribute of the DC field element whose value to use as the value for the outattrname attribute of the output element. If param value is empty, the fixed value, passed in as outattrvalue_fixed, is used for the value of the output attribute -->
        <xsl:param name="outattrvalue_fixed"/> <!-- fixed value to use for the value of the output attribute. If both outattrvalue_fixed and outattrvalue_dimattribute are nonempty, the fixed value is used. -->
        
       
        
        <xsl:for-each select="dim_fun:qdcelements_get($item, $mdSchema, $element, $qualifier)">
            <xsl:variable name="field" select="."/>
            <!-- only return an element when the defining DC field is nonempty -->
        <xsl:if test="not(compare($field,'')=0)">
        <xsl:element name="{$outname}" namespace="{$outnamespace}">
            <!-- only add an attribute if we are given a name for the attribute and the value to use is nonempty -->
            <xsl:if test="not(compare($outattrname,'')=0)">
                <!-- if fixed value is passed, use that value. Otherwise, get the value of the DC field attribute -->
                <xsl:choose>
                    <xsl:when test="not(compare($outattrvalue_fixed,'')=0)">
                        <xsl:attribute name="{$outattrname}" namespace="{$outattrnamespace}" >
                            <xsl:value-of select="$outattrvalue_fixed"/>
                        </xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="dim_fun:attribute_nonempty($field, $outattrvalue_dimattribute)">
                            <xsl:attribute name="{$outattrname}" namespace="{$outattrnamespace}" >
                                <xsl:value-of select="dim_fun:attribute_get($field, $outattrvalue_dimattribute)"/>
                            </xsl:attribute>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <!-- the actual content of the output element depends on whether an dim attribute name is provided -->
            <xsl:choose>
                <xsl:when test="not(compare($outcontent_dimattribute,'')=0)">
                    <xsl:value-of select="dim_fun:attribute_get($field, $outcontent_dimattribute)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$field"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
            
        </xsl:if>
        </xsl:for-each>
    </xsl:template>    

    <xsl:template name="xform_dim_based_on_createElements">
        <xsl:param name="createElements"/> <!-- the createElements element. Each createElement child defines an output element -->
        <xsl:param name="item"/> <!-- the dim element with all metadata in DIM format -->
        
            <xsl:for-each select="$createElements/createElement">
                <xsl:variable name="out" select="."/>
                <xsl:call-template name="output_elements_with_single_attribute">
                    <xsl:with-param name="item" select="$item"/> <!-- the dim element == the item -->
                    <xsl:with-param name="mdSchema"><xsl:value-of select="$out/basedOn/@mdschema"/></xsl:with-param> <!-- which DC field to use = "defining DC field"-->
                    <xsl:with-param name="element"><xsl:value-of select="$out/basedOn/@element"/></xsl:with-param>
                    <xsl:with-param name="qualifier"><xsl:value-of select="$out/basedOn/@qualifier"/></xsl:with-param>

                    <xsl:with-param name="outname"><xsl:value-of select="$out/@name"/></xsl:with-param> <!-- the name of the output element -->
                    <xsl:with-param name="outnamespace"><xsl:value-of select="$out/@namespace"/></xsl:with-param> <!-- the namespace of the output element -->

                    <xsl:with-param name="outcontent_dimattribute"><xsl:value-of select="$out/basedOn/useAttribute/@name"/></xsl:with-param> <!-- the name of the attribute of the DC field element whose value to use as the content of the output element. If empty, the content of the defining DC field is used -->
                    <xsl:with-param name="outattrname"><xsl:value-of select="$out/addAttribute/@name"/></xsl:with-param> <!-- the name of the attribute to add to the output element (if empty, no attribute is added) -->
                    <xsl:with-param name="outattrnamespace"><xsl:value-of select="$out/addAttribute/@namespace"/></xsl:with-param> <!-- the namespace of outattrname -->
                    <xsl:with-param name="outattrvalue_dimattribute"><xsl:value-of select="$out/addAttribute/useDC/@attribute"/></xsl:with-param> <!-- the name of the attribute of the DC field element whose value -->
                    <xsl:with-param name="outattrvalue_fixed"><xsl:value-of select="$out/addAttribute/fixed/@value"/></xsl:with-param>
                </xsl:call-template>
                
            </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>