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
    var authority = field.id + '_authority';
    //var confidence = field.id + '_confidence';
    
    document.getElementById(authority).value = li.id;
}

function clearAuthority(authority)
{
    document.getElementById(authority).value ="";
}