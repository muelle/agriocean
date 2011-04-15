/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package proj.oceandocs.citation;

import java.util.ArrayList;

/**
 *
 * @author Denys SLIPETSKYY
 */
public class CitationTemplate {
    public String type;
    public String name;
    public String language;
    public ArrayList<String> template;

    public CitationTemplate()
    {
        template = new ArrayList<String>();
    }
}
