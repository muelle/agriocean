<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Footer for home page
  --%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%
    String sidebar = (String) request.getAttribute("dspace.layout.sidebar");
    int overallColSpan = 3;
    if (sidebar == null)
    {
        overallColSpan = 2;
    }
%>
                    <%-- End of page content --%>
                    <p>&nbsp;</p>
                </td>
            </tr>

            <%-- Page footer --%>
             <tr class="pageFooterBar">
                <td colspan="<%= overallColSpan %>" class="pageFootnote">
                    <table class="pageFooterBar" width="100%">
                        <tr>
                            <td align="left" width ="80">
			      <a href="http://www.fao.org"><img src="<%= request.getContextPath() %>/image/fao-footer.jpg" width="44" height="44" border="0"/></a>
                            </td>
                            <td width="200">
<%--							Integovemmental Oceanographic Commission of UNESCO <br/>
							International Oceanographic Data and Information Exchange  --%>
			      <a href="http://iode.org"><img src="<%= request.getContextPath() %>/image/footer.png" width="147" height="32" border="0"/></a>				
                            </td>
                        <td align="right">
<%--							Integovemmental Oceanographic Commission of UNESCO <br/>
							International Oceanographic Data and Information Exchange  --%>
				<a href="http://www.dspace.org"><img src="<%= request.getContextPath() %>/image/dspace-blue.gif" width="99" height="39" border="0"/></a>
                            </td>

                        </tr>
                    </table>
                </td>
            </tr>
                    </table>
                </td>
            </tr>
        </table>
        </div> <!-- ds-main -->
    </body>
</html>