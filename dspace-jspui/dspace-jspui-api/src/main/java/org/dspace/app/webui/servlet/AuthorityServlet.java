/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;

import org.dspace.content.authority.Choice;
import proj.oceandocs.authority.AuthorityManager;

/**
 *
 * @author Denys Slipetskyy
 */
public class AuthorityServlet extends DSpaceServlet {

    @Override
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        process(context, request, response);
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        process(context, request, response);
    }

    /**
     * Generate the <li> element for scripaculos autocompleter component
     *
     * Looks for request parameters:
     *  field - MD field key, i.e. form key, REQUIRED - derivated from url.
     *  query - string to match
     *  collection - db ID of Collection ot serve as context
     *  start - index to start from, default 0.
     *  limit - max number of lines, default 1000.
     *  format - opt. result XML/XHTML format: "select", "ul", "xml"(default)
     *  locale - explicit locale, pass to choice plugin
     */
    private void process(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        
        String[] paths = request.getPathInfo().split("/");
        String generator = paths[paths.length - 1];
        String input = request.getParameter("value") == null ? "": request.getParameter("value");

        response.setContentType("text/plain; charset=\"utf-8\"");
        Writer writer = response.getWriter();
        
        AuthorityManager am = new AuthorityManager(generator, context);
        if(am != null){
           ArrayList<Choice> choices = am.getAutocompleteSet(input); 
           writer.append("<ul>");
           for(Choice ch: choices)
            {
                writer.append("<li id=\"").append(ch.authority).append("\">").append(ch.value).append("</li>\n");
            }
            writer.append("</ul>");
        }else {
            writer.append("<ul></ul>");
        }
            writer.flush();
    }
}
