/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package proj.oceandocs.submission;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
/**
 *
 * @author Denys Slipetskyy <d.slipetskiy@gmail.com>
 */
public class XMLReadErrorHandler extends DefaultHandler 
{
    private Logger log = Logger.getLogger(XMLReadErrorHandler.class);
    String filename = "";
    
    public XMLReadErrorHandler(Logger logger, String parsedFilename)
    {
        log = logger;
        filename = parsedFilename;
    }
    
    @Override
      public void warning(SAXParseException e) throws SAXException {
         log.warn("Parsing " + filename, e);
         printInfo(e);
      }
    @Override
      public void error(SAXParseException e) throws SAXException {
         log.error("Parsing " + filename, e);
         printInfo(e);
      }
    @Override
      public void fatalError(SAXParseException e) throws SAXException {
        log.fatal("Parsing " + filename, e); 
        printInfo(e);
      }
      private void printInfo(SAXParseException e) {
         log.info("   Line number: "+e.getLineNumber());
         log.info("   Column number: "+e.getColumnNumber());
         log.info("   Message: "+e.getMessage());
      }
   }
