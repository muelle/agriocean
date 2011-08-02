/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.citation;

import java.util.HashMap;

/**
 *
 * @author Denys SLIPETSKYY
 */
public class CitationTemplatesCollection {
    private HashMap<String, CitationTemplate> templates;

    public CitationTemplatesCollection() {
        templates = new HashMap<String, CitationTemplate>();
    }

    public CitationTemplate getTemplateByName(String name) {
        if (templates.containsKey(name)) {
            return templates.get(name);
        } else {
            return null;
        }
    }

    public boolean addTemplate(String name, CitationTemplate template) {
        if (!templates.containsKey(name)) {
            templates.put(name, template);

            return true;
        } else {
            return false;
        }
    }

    public int getTemplatesCount() {
        return templates.size();
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
