/**
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree and available
 * online at
 *
 * http://www.dspace.org/license/
 */
/**
 * @author Christof Verdonck (updated by Denys Slipetskyy)
 * @version aod1.0
 */
package proj.agriocean.batchimport;
//Imports
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.crosswalk.DIMIngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import proj.agriocean.batchimport.processors.RISBIBTEXProcessor;

public class BatchImportServlet extends DSpaceServlet {

    private static Logger log = Logger.getLogger(BatchImportServlet.class);
    private static long maxFileSize = 1024 * 1024 * 20; //20MB import file size limit
    private static final Namespace DIM_NS = Namespace.getNamespace("http://www.dspace.org/xmlns/dspace/dim");

    static enum MetadataType {
        MODS, AGRIS, XML, ENDNOTE, INMAGIC, UNKNOWN
    };

    static enum ImportMode {
        WORKSPASE, DIRECT
    };
    private MetadataType metadataType;
    private ImportMode importMode;
    private int collectionID;
    private File fileToImport = null;
    private File XSLTfile = null;
    private File resultFile = null;
    private String XSLTpath;
    private static String outputPath = ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator + "batchimport" + File.separator + "output" + File.separator;

    //doDSGET
    @Override
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
    }
//doDSPost

    @Override
    protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {

        // Check that we have a file upload request
        if (ServletFileUpload.isMultipartContent(request)) {
            metadataType = MetadataType.UNKNOWN;
            try {
                // Create a factory for disk-based file items
                DiskFileItemFactory factory = new DiskFileItemFactory();
                // Set factory constraints
                //factory.setSizeThreshold(1024*1024*10);
                //factory.setRepository(new File(getServletContext().getRealPath("/WEB-INF")+File.pathSeparator+"batchimport.temp"));
                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(factory);
                // Set overall request size constraint
                upload.setSizeMax(maxFileSize);
                // Parse the request
                List<FileItem> /*
                         * FileItem
                         */ items = upload.parseRequest(request);
                // Process the uploaded items
                //Iterator iter = items.iterator();

                String result;
                String name;
                String value;
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        if (item.isFormField()) {
                            name = item.getFieldName();
                            value = item.getString();
                            if ("metadataFormat".equals(name)) {
                                if ("MODS".equals(value)) {
                                    metadataType = MetadataType.MODS;
                                } else if ("AGRIS".equals(value)) {
                                    metadataType = MetadataType.AGRIS;
                                } else if ("ENDNOTE".equals(value)) {
                                    metadataType = MetadataType.ENDNOTE;
                                } else if ("XML".equals(value)) {
                                    metadataType = MetadataType.XML;
                                }else if ("INMAGIC".equals(value)) {
                                    metadataType = MetadataType.INMAGIC;
                                }
                            } else if ("collection_id".equals(name)) {
                                collectionID = Integer.parseInt(value);
                            } else if ("importmode".equals(name)) {
                                if ("WORKSPACE".equals(value)) {
                                    importMode = ImportMode.WORKSPASE;
                                } else if ("DIRECT".equals(value)) {
                                    importMode = ImportMode.DIRECT;
                                }
                            }
                        }
                    } else {
                        try {
                            name = item.getFieldName();
                            value = item.getName();

                            if ("file".equals(name)) {
                                if ("".equals(value)) {
                                    log.error(LogManager.getHeader(context, "batch import", "no file to import ... "));
                                    result = "no file to import";
                                    request.setAttribute("output", result);
                                    JSPManager.showJSP(request, response, "/submit/import-result.jsp");
                                }
                                fileToImport = new File(outputPath + value);
                                item.write(fileToImport);
                            } else if ("xslt".equals(name)) {
                                if (!"".equals(value)) {
                                    XSLTfile = new File(outputPath + value);
                                    item.write(XSLTfile);
                                }
                            }
                        } catch (Exception e) {
                            log.error(LogManager.getHeader(context, "batch import", "file write error (" + item.getName() + ") ... "), e);
                            JSPManager.showInternalError(request, response);
                        }
                    }
                }

                result = processUploadedFile(context, request, response);
                request.setAttribute("output", result);
                context.commit();
                JSPManager.showJSP(request, response, "/submit/import-result.jsp");

            } catch (FileUploadException e) {
                log.error(LogManager.getHeader(context, "upload file error", "batch import: file size exided maximum allowed " + maxFileSize + "bytes"), e);
                JSPManager.showInternalError(request, response);
            } catch (NumberFormatException e) {
                log.error(LogManager.getHeader(context, "wrong collection id", ""), e);
                JSPManager.showInternalError(request, response);
            }
        }
    }

    private String processUploadedFile(Context context, HttpServletRequest request, HttpServletResponse response) throws AuthorizeException, SQLException, ServletException, IOException {

        if (fileToImport != null) {
            Collection collection = Collection.find(context, collectionID);
            if (collection == null) {
                JSPManager.showInvalidIDError(request, response, String.valueOf(collectionID), Constants.COLLECTION);
            }
            if (!AuthorizeManager.authorizeActionBoolean(context, collection, Constants.ADD)) {
                throw new AuthorizeException("You are not authorized to perform this action.", collection, Constants.ADD);
            }
//            XSLTpath = (new StringBuilder()).append(ConfigurationManager.getProperty("dspace.dir")).append(File.separator )
//                    .append("config").append(File.separator).append("batchimport").append(File.separator);
            XSLTpath = ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator + "batchimport" + File.separator;
            switch (metadataType) {
                case AGRIS:
                    XSLTpath += "Agris2DIM.xsl";
                    return processXML(context, collection);
                case MODS:
                    XSLTpath += "MODS2DIM.xsl";
                    return processXML(context, collection);
                case ENDNOTE:
                    RISBIBTEXProcessor processor = new RISBIBTEXProcessor();
                    return processor.process(context, collection, fileToImport).toString();
                case INMAGIC:
                    XSLTpath += "INMAGIC2DIM.xsl";
                    return processXML(context, collection);

                case XML:
                    return processXML(context, collection);
                default:
                    return "Unknown metadata format";
            }
        } else {
            return "no file to import, error ?";
        }
    }

    private String processXML(Context context, Collection collection) {
        StringBuilder result = new StringBuilder();

        try {
            javax.xml.transform.TransformerFactory tFactory =
                    javax.xml.transform.TransformerFactory.newInstance();

            javax.xml.transform.Transformer transformer;

            if (metadataType != MetadataType.XML) {
                XSLTfile = new File(XSLTpath);
            }

            if (XSLTfile != null && XSLTfile.exists()) {
                transformer = tFactory.newTransformer(new javax.xml.transform.stream.StreamSource(XSLTfile));
                if (transformer == null) {
                    return "Couldn't create XSL transformer. Typically that is becuase of invalid XSLT file. (" + XSLTfile.getCanonicalPath() + ")";
                }
            } else {
                return "XSLT file not found: " + XSLTfile.getCanonicalPath();
            }


            resultFile = new File(outputPath + "xslt.out");

            if (resultFile.exists()) {
                resultFile.delete();
            }
            resultFile.createNewFile();

            if (resultFile != null && resultFile.exists()) {
                transformer.transform(new javax.xml.transform.stream.StreamSource(fileToImport),
                        new javax.xml.transform.stream.StreamResult(resultFile));
            } else {
                return "Can't create output file for XSLT transformation during batch import";
            }

            SAXBuilder builder = new SAXBuilder();
            Document document = (Document) builder.build(resultFile);
            Element rootNode = document.getRootElement();
            List<Element> list = rootNode.getChildren("dim", DIM_NS);

            DIMIngestionCrosswalk DIMcrosswalk = new DIMIngestionCrosswalk();

            WorkspaceItem workspaceItem;
            Item item = null;
            DCValue dcvs[];
            for (Element dim : list) {

                switch (importMode) {
                    case WORKSPASE:
                        workspaceItem = WorkspaceItem.create(context, collection, false);
                        item = workspaceItem.getItem();
                        DIMcrosswalk.ingest(context, item, dim);
                        item.update();
                        workspaceItem.update();

                        //fix authority values
                        KeywordsAuthority(context, item);
                        ISSNAuthority(context, item);
                        
                        workspaceItem.update();
                        break;
//                    case DIRECT:
//                        item = Item.create(context);
//                        DIMcrosswalk.ingest(context, item, dim);
//                        item.update();
//                        break;
                }

                context.commit();

                if (item != null) {
                    result.append("<p>");
                    dcvs = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
                    for (DCValue dcv : dcvs) {
                        result.append("<div>").append(dcv.schema).append(".").append(dcv.element);
                        if (dcv.qualifier != null) {
                            result.append(".").append(dcv.qualifier).append(" = ");
                        } else {
                            result.append(" = ");
                        }

                        result.append(dcv.value);
                        if (dcv.authority != null && !"".equals(dcv.authority)) {
                            result.append(" | authority = ").append(dcv.authority);
                        }
                        if (dcv.language != null && !"".equals(dcv.language)) {
                            result.append(" | language = ").append(dcv.language);
                        }
                        result.append("</div>");
                    }
                    result.append("</p> <hr/>");
                }
            }

            return result.toString();
        } catch (Exception e) {
            return "XML file processing error during batch import: " + e.getLocalizedMessage();
        }
    }

    
    protected void ISSNAuthority(Context context, Item item) throws SQLException, IOException {
        String sqlISSN = ConfigurationManager.getProperty("sql.onematch.issn");
        String issn = "";
        String language;

        if (sqlISSN != null && !"".equals(sqlISSN)) {
            DCValue[] titles = item.getMetadata(MetadataSchema.DC_SCHEMA, "bibliographicCitation", "title", Item.ANY);
            if (titles.length > 0) {
                DCValue[] dcvs = item.getMetadata("dc.identifier.issn");
                if (dcvs.length > 0) {
                    if (dcvs[0].value != null) {
                        issn = dcvs[0].value;
                    }
                }
                for (DCValue jtitle : titles) {
                    if (jtitle.value != null) {
                        if ("".equals(issn)) {
                            if (sqlISSN != null) {
                                issn = getExactMatch(context, sqlISSN, jtitle.value);
                            }
                        }
                        jtitle.authority = issn;
                        language = jtitle.language == null ? Item.ANY: jtitle.language;
                        item.clearMetadata(MetadataSchema.DC_SCHEMA, "bibliographicCitation", "title", language);
                        item.addMetadata(MetadataSchema.DC_SCHEMA, "bibliographicCitation", "title", language, jtitle.value, jtitle.authority, Choices.CF_ACCEPTED, true);
                    }
                }
            }
        }

        try {
            item.update();
        } catch (AuthorizeException ex) {
            log.error("Batch import: journal title & ISSN authority update exception: " + ex.getLocalizedMessage());
        }
    }

    protected void KeywordsAuthority(Context context, Item item) throws SQLException, IOException {
        String sqlASFA = ConfigurationManager.getProperty("sql.onematch.asfa");
        String sqlAgrovoc = ConfigurationManager.getProperty("sql.onematch.agrovoc");
        String language;

        if (sqlASFA != null && !"".equals(sqlASFA)) {
            DCValue dcvs[];

            dcvs = item.getMetadata("dc.subject.asfa");
            item.clearMetadata("dc", "subject", "asfa", Item.ANY);
            for (DCValue dcv : dcvs) {
                if (dcv.value != null) {
                    language = dcv.language == null ? Item.ANY: dcv.language;
                    dcv.authority = getExactMatch(context, sqlASFA, dcv.value);
                    item.addMetadata(MetadataSchema.DC_SCHEMA, "subject", "asfa", language, dcv.value, dcv.authority, Choices.CF_ACCEPTED, true);
                }
            }
        }

        if (sqlAgrovoc != null && !"".equals(sqlAgrovoc)) {
            DCValue dcvs[];

            dcvs = item.getMetadata("dc.subject.agrovoc");
            item.clearMetadata("dc", "subject", "agrovoc", Item.ANY);
            for (DCValue dcv : dcvs) {
                if (dcv.value != null) {
                    language = dcv.language == null ? "en": dcv.language;
                    dcv.authority = getExactMatchLang(context, sqlAgrovoc, dcv.value, language);
                    item.addMetadata(MetadataSchema.DC_SCHEMA, "subject", "agrovoc", language, dcv.value, dcv.authority, Choices.CF_ACCEPTED, true);
                }
            }
        }
        try {
            item.update();
        } catch (AuthorizeException ex) {
            log.error("Batch import: keywords authority update exception: " + ex.getLocalizedMessage());
        }
    }
    
    private String getExactMatch(Context context, String sql, String query) {
        String result = "";
        try {
            if (context != null) {
                TableRowIterator tri = DatabaseManager.query(context, sql, query);

                List<TableRow> trs = tri.toList();

                if (trs.size() == 1) {
                    result = trs.get(0).getStringColumn("authority");
                }
            }
        } catch (SQLException ex) {
            log.error("Batch import: authority update exception - getExactMatch: " + ex.getLocalizedMessage());
        } finally {
            return result;
        }
    }

    private String getExactMatchLang(Context context, String sql, String query, String language) {
        String result = "";
        try {
            if (context != null) {
                TableRowIterator tri = DatabaseManager.query(context, sql, query, language);

                List<TableRow> trs = tri.toList();

                if (trs.size() == 1) {
                    result = trs.get(0).getStringColumn("authority");
                }
            }
        } catch (SQLException ex) {
            log.error("Batch import: authority update exception - getExactMatchLang: " + ex.getLocalizedMessage());
        } finally {
            return result;
        }
    }

}
