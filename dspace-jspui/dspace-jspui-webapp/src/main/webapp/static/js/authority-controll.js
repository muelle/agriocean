/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
// Client-side scripting to support DSpace Choice Control

function authoritySuggest(field, div, url, indicator)
{
    new Ajax.Autocompleter(field, div, url, {paramName: "value", minChars: 3, 
        indicator: indicator, afterUpdateElement : updateAuthority});
}

function updateAuthority(field, li)
{
    var parts = field.id.split("_");
    var authority = field.id + '_authority';
    
    if (parts.length > 0){
        if(!isNaN(parseInt(parts[parts.length-1]))){
            authority = "";
            for(i=0; i < parts.length-1; i++){
                authority += parts[i] + "_";
            }
            authority += "authority_" + parts[parts.length-1];
        }
    }
        //var confidence = field.id + '_confidence';
        document.getElementById(authority).value = li.id;
}