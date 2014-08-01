/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package driverdownloader;

import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.json.JSONObject;

/**
 * Class to download drivers from Ibm and Lenovo.
 * Note: there are two pages that we start from, because Lenovo was bought by IBM
 *       and the two site are entertwined.
 *       The websites are quite straight forward, but the part starting at homepage1
 *       is quite inconsistent, which makes the code ugly.
 *       The part starting at homepage2 uses JSON, which makes it easy to scrape the pages.
 * @author Mark Van Peteghem
 *
 */
public class IbmLenovoDriverDownloader extends DriverDownloader {

    private final String homepage1 = "http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=DRVR-MATRIX";
    // variation of homepage1, which is sometimes encountered in pages, which has to be ignored:
    private final String homepage1b = "http://www-307.ibm.com/pc/support/site.wss/DRVR-MATRIX.html";
    private final String homepage2 = "http://consumersupport.lenovo.com/ot/en/driversdownloads/drivers.html";

    @Override
    public void updateData(boolean updateOnlyCategory) {
        try {
            if (!updateOnlyCategory)
                driverData.clear();
            else
                driverData.clearProductType(driverData.getActiveProductType());

            updateDataOnFirstPage(updateOnlyCategory);
            updateDataOnSecondPage(updateOnlyCategory);

            if (Interrupt.get())
                loadData();
            else
                saveData();

        } catch (Exception ex) {
        	try {
        		loadData();
        	} catch (Exception ex2) {
        		MessageHandler.addError(ex2);
        	}
            MessageHandler.addError(ex);
        }
    }

    private static Map<String, String> getMapFromCommaString(String str) throws UnexpectedFormatException {
        Map<String, String> map = new TreeMap<String, String>();

        StringTokenizer stModels = new StringTokenizer(str, ",");
        while (stModels.hasMoreTokens()) {
            String part = stModels.nextToken();
            int pos = ParseUtils.safeIndexOf(part, "|", 0, true);
            String key = part.substring(0, pos);
            String value = part.substring(pos+1);
            map.put(key, value);
        }
        return map;
    }

    private String cutOffExtraneousPart(String linksSection) {
        String extraneousStartPhrases[] = new String[] {
            "Additional information",
            "Additional Information",
            "Additional Product information",
            "Additional Product Information",
            "Additional product information",
            "Additional product Information",
        };

        for (String extraneousStartPhrase: extraneousStartPhrases) {
            int pos = linksSection.indexOf(extraneousStartPhrase);
            if (pos >= 0) {
                linksSection = linksSection.substring(0, pos);
            }
        }
        return linksSection;
    }

    private void updateDataOnFirstPage(boolean updateOnlyCategory) throws Exception {

        visitedUrls.clear();

        DownloadedPage page = download.getPage(new PostableUrl(homepage1));
        if (Interrupt.get()) {
            return;
        }
        String content = page.getContent();

        String linksSection = ParseUtils.findSubstringByHeaderString(content, "Select your product below:", "Applicable countries", false);
        List<String> links = ParseUtils.getSubStrings(linksSection, "<a ", "</a>", false);
        String folder = ParseUtils.getUrlFolder(page.getUrl());

        Map<String, String> categories = new TreeMap<String, String>();
        for (String link: links) {
            String name = ParseUtils.findSubstringByHeaderString(link, ">", "", true);
            if (!name.equals("here") && link.contains("href")) {
                String url = ParseUtils.findSubstringByHeaderString(link, "href=\"", "\"", false);
                categories.put(name, folder+url);
            }
        }

        if (updateOnlyCategory) {
            // don't add product type because driverData was not cleared
            String name = driverData.getActiveProductType();
            String url = categories.get(name);
            if (null!=url) {
                updateCategoryFromFirstPage(url, name);
            }
        } else {
            for (Map.Entry<String, String> category: categories.entrySet()) {
                // add product type because driverData was cleared
                driverData.addProductType(category.getKey());
                String url = category.getValue();
                updateCategoryFromFirstPage(url, category.getKey());
                saveData();
            }
        }
    }
    
    
    HashSet<String> visitedUrls = new HashSet<String>();

    private static String prefixes[] = new String[]{
        "Drivers and software - ",
        "Drivers & Downloads - ",
        "Drivers & software - ",
        "Software and device drivers - ",
        "Software and drivers for ",
        "Index for ",
        "Virtual tour - ",
    };

    private static String suffixes[] = new String[]{
        " - Files",
        " - Service parts"
    };

    private String cleanupName(String name) {
        for (String prefix: prefixes) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
                break;
            }
        }
        for (String suffix: suffixes) {
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length()-suffix.length());
                break;
            }
        }
        return name;
    }

    private boolean looksLikeGeneralPage(String name) {
        return name.equals("here") ||
                name.startsWith("Having trouble") ||
                name.equals("Find a service provider") ||
                name.equals("Learn") ||
                name.startsWith("What is") ||
                name.startsWith("How to") ||
                name.startsWith("Order ") ||
                name.startsWith("When to ") ||
                name.startsWith("How can I ") ||
                name.startsWith("Support phone list") ||
                name.startsWith("Options Continuation Program") ||
                name.startsWith("Warranty lookup") ||
                name.startsWith("status of your") ||
                name.startsWith("Troubleshooting by symptom") ||
                name.startsWith("Check warranty") ||
                name.startsWith("Get help at") ||
                name.startsWith("Submit a ") ||
                name.startsWith("Contact your local") ||
                name.startsWith("Provide feedback") ||
                name.equals("Troubleshooting") ||
                name.equals("ThinkPad Troubleshooting") ||
                name.equals("Publications") ||
                name.startsWith("searching support") ||
                name.contains("Purchase parts") ||
                name.contains("Need more help") ||
                name.contains("phone list") ||
                name.contains("optionscontinuation") ||
                name.contains("/training") ||
                name.contains("Overview movie") ||
                name.contains("Printable version");
    }

    private boolean hasFileDownloads(List<String> links) {
        for (String link: links) {
            if (link.contains(".exe<") ||
                    link.contains(".zip<") ||
                    link.contains(".exe\">") ||
                    link.contains(".zip\">"))
                return true;
        }
        return false;
    }

    Set<String> urlsToIgnore = new HashSet<String>();

    private void setUrlsToIgnore(String url) {
        urlsToIgnore.clear();
        if (url.contains("MIGR-4ZTPEF")) {
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-4U9PZ7");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-4U9PZ7.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-58256");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-58256.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-58442");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-58442.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-73286");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-73286.html");

            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-4TNTT5");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-62014");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-61982");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-62055");
        } else if (url.contains("MIGR-4U9PZ7") || url.contains("MIGR-58256") || url.contains("MIGR-58442")
                || url.contains("MIGR-73286")) {
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-4ZTPEF");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-4ZTPEF.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-50454");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-50454.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-51369");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-51369.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-43331");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-43331.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-43294");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-43294.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-4TPJF3");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-4TPJF3.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-4ZUPEH");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-4ZUPEH.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-43299");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-43299.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-4ZLKLK");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-4ZLKLK.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-43301");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-43301.html");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/document.do?lndocid=MIGR-42680");
            urlsToIgnore.add("http://www-307.ibm.com/pc/support/site.wss/MIGR-42680.html");
        }
    }

    private void updateCategoryFromFirstPage(String url, String name) throws Exception {
        System.out.println("Updating category "+name);
        setUrlsToIgnore(url);
        int maxRecursiveLevel = 8;
        if (url.contains("4ZTPEF"))
            maxRecursiveLevel = 4;
        updateCategoryFromFirstPageRecursively(url, maxRecursiveLevel);
        System.out.println(driverData.getNrModelNumbers()+" models found");
    }

    private void updateCategoryFromFirstPageRecursively(String url, int recursionLevel) throws Exception {

        // ignore links to the drivers home page
        if (url.equalsIgnoreCase(homepage1) || url.equalsIgnoreCase(homepage1b) || url.equalsIgnoreCase(homepage2)
                ||url.equalsIgnoreCase("http://www.ibm.com/storage/") )
            return;

        // check that we're not going in circles:
        if (visitedUrls.contains(url))
            return;
        visitedUrls.add(url);

        DownloadedPage page = download.getPage(new PostableUrl(url));
        String content = page.getContent();
        if (Interrupt.get()) {
            return;
        }
        url = page.getUrl();
        page = null;

        if (content.contains("There is a problem retrieving the document") ||
                content.contains("Movie matrices"))
            return;
        
        String metaDesc = "<meta name=\"Description\" content=\"";
        if (content.contains(metaDesc)) {
            String description = ParseUtils.findSubstringByHeaderString(content, metaDesc, "\"", false);
            if (description.contains("Troubleshooting") ||
                    description.contains("Product Recalls") ||
                    description.contains("phone list"))
                return;
        }

        if (content.contains("Download individual drivers") ||
                content.contains("Only the operating systems") ||
                content.contains("File link") ||
                content.contains("Click the driver category") ||
                content.contains("jump to the driver you need") ||
                content.contains("The link below") ||
                content.contains("install this driver") ||
                content.contains("Download multiple files")) {
            // It's a product page with links to files to download
            String name = ParseUtils.findSubstringByHeaderString(content, "<meta name=\"Description\" content=\"", "\"/>", false);
            name = cleanupName(name);
            driverData.addModelNumber(name, url);
            System.out.println("Adding "+name);
        } else {
            // It's probably a page with links to several products

            String linksSection;
            if (content.contains("Applicable countries"))
                linksSection = ParseUtils.findSubstringByHeaderString(content, "Applicable countries", "Applicable countries", false);
            else
                linksSection = ParseUtils.findSubstringByHeaderString(content, ">Drivers", "Copyright", false);
            
            linksSection = cutOffExtraneousPart(linksSection);

            String domain = ParseUtils.getUrlDomain(url);
            String folder = ParseUtils.getUrlFolder(url);
            List<String> links = ParseUtils.getSubStrings(linksSection, "<a ", "</a>", false);

            if (hasFileDownloads(links)) {
                // It's a product page with links to files to download
                String name = ParseUtils.findSubstringByHeaderString(content, "<meta name=\"Description\" content=\"", "\"/>", false);
                name = cleanupName(name);
                driverData.addModelNumber(name, url);
                System.out.println("Adding "+name);
            } else if (recursionLevel>0) {
                Level.Enter();
                for (String link: links) {
                    String name = ParseUtils.findSubstringByHeaderString(link, ">", "", true);
                    if (!looksLikeGeneralPage(name) && link.contains("href")) {
                        String nextUrl = ParseUtils.findSubstringByHeaderString(link, "href=\"", "\"", false).trim();
                        if ((!nextUrl.startsWith("http:") || nextUrl.contains("ibm.com")) &&
                                !nextUrl.endsWith(".jpg") && !nextUrl.endsWith(".jpeg") && !nextUrl.endsWith(".gif") &&
                                !nextUrl.endsWith(".png") && !nextUrl.startsWith("%") && !nextUrl.startsWith("javascript:") &&
                                !nextUrl.startsWith("mailto:") && !nextUrl.contains("/smartctr/")) {
                            int hashPos = nextUrl.indexOf("#");
                            if (hashPos==0)
                                continue; // it's something like href="#before"
                            if (hashPos>0)
                                nextUrl = nextUrl.substring(0, hashPos);
                            if (nextUrl.startsWith("/"))
                                nextUrl = domain+nextUrl;
                            else if (!nextUrl.contains("://"))
                                nextUrl = folder+nextUrl;

                            // necessary because otherwise two categories get mixed up
                            if (urlsToIgnore.contains(nextUrl))
                                continue;

                            content = null; // necessary to free up memory

                            updateCategoryFromFirstPageRecursively(nextUrl, recursionLevel-1);
                            if (Interrupt.get()) {
                                break;
                            }
                        }
                    }
                }
                Level.Leave();
            }
        }
    }

    private void updateDataOnSecondPage(boolean updateOnlyCategory) throws Exception {
        DownloadedPage page = download.getPage(new PostableUrl(homepage2));
        if (Interrupt.get()) {
            return;
        }
        String content = page.getContent();

        String data = ParseUtils.findSubstringByHeaderString(content, "hash=$H(", ");", true);
        JSONObject map = new JSONObject(data);
        String categories = map.getString("0");
        Map<String, String> categoriesMap = getMapFromCommaString(categories);
        for (Map.Entry<String, String> cat: categoriesMap.entrySet()) {
            String categoryId = cat.getKey();
            String categoryName = cat.getValue();

            if (!updateOnlyCategory)
                driverData.addProductType(categoryName);
            else
                driverData.selectProductType(categoryName);

            String models = map.getString(categoryId);
            Map<String, String> modelsMap = getMapFromCommaString(models);
            for (Map.Entry<String, String> mod: modelsMap.entrySet()) {
                String modelId = mod.getKey();
                String modelName = mod.getValue();

                if (map.has(modelId)) {
                    String subModels = map.getString(modelId);
                    Map<String, String> subModelsMap = getMapFromCommaString(subModels);
                    for (Map.Entry<String, String> subMod: subModelsMap.entrySet()) {
                        String subModelId = subMod.getKey();
                        String subModelName = subMod.getValue();
                        if (modelId.equals("607007")) {
                            // in this case subModelName is one of Nivdia, Intel and VIA
                            // which don't say much
                            subModelName = modelName+" "+subModelName;
                        }

                        String link = "http://consumersupport.lenovo.com/ot/en/driversdownloads/APR_Driver_List.aspx?CategoryID="+subModelId+"&OS=0";
                        driverData.addModelNumber(subModelName, link);
                    }
                } else {
                    String link = "http://consumersupport.lenovo.com/ot/en/driversdownloads/APR_Driver_List.aspx?CategoryID="+modelId+"&OS=0";
                    driverData.addModelNumber(modelName, link);
                }
            }
        }
    }

    @Override
    protected Map<String, PostableUrl> getCategories() throws Exception {
        // This method does nothing because we overloaded updateData
        return null;
    }

    @Override
    protected void updateCategory(DownloadedPage page) throws Exception {
        // This method does nothing because we overloaded updateData
    }

    public void download(PostableUrl url, String path) {
        try {
            if (url.getLink().contains("lenovo.com")) {
                DownloadedPage page = download.getPage(url);
                if (Interrupt.get()) {
                    return;
                }
                String content = page.getContent();

                String folder = ParseUtils.getUrlFolder(url.getLink());

                List<String> links = ParseUtils.getSubStrings(content, "\"window.open('", "'", true);
                for (String link: links) {
                    downloadFileOfSecondType(folder+link, path);
                    if (Interrupt.get()) {
                        return;
                    }
                }
            } else {
                visitedUrls.clear();
                processFilesPageOfFirstType(url, path);
            }
    	} catch (Exception ex) {
            MessageHandler.addError(ex);
    	}
    }

    private void processFilesPageOfFirstType(PostableUrl url, String path) throws Exception, UnexpectedFormatException {
        if (visitedUrls.contains(url.getLink()))
            return;
        visitedUrls.add(url.getLink());

        DownloadedPage page = download.getPage(url);
        if (Interrupt.get()) {
            return;
        }
        String content = page.getContent();

        String title = ParseUtils.findSubstringByHeaderString(content, "<title>", "</title>", false);
        if (title.contains("Building diskettes from the Web") ||
                title.contains("Find a service provider") ||
                title.contains("Having trouble downloading") ||
                content.contains("There is a problem retrieving"))
            return;

        String folder = ParseUtils.getUrlFolder(url.getLink());
        String linksSection;
        if (content.contains("Applicable countries")) {
            linksSection = ParseUtils.findSubstringByHeaderString(content, "Applicable countries", "Applicable countries", false);
        } else if (content.contains(">Drivers")) {
            linksSection = ParseUtils.findSubstringByHeaderString(content, ">Drivers", "Copyright", false);
        } else {
            return; // unknown type of page
        }
        linksSection = cutOffExtraneousPart(linksSection);

        String domain = ParseUtils.getUrlDomain(page.getUrl());
        List<String> linkParts = ParseUtils.getSubStrings(linksSection, "<a ", "</a>", false);
        for (String linkPart : linkParts) {
            if (linkPart.contains("Having trouble downloading") || !linkPart.contains("href=")) {
                continue;
            }
            String link = ParseUtils.findSubstringByHeaderString(linkPart, "href=\"", "\"", false);
            int hashPos = link.indexOf("#");
            if (hashPos == 0) {
                continue;
            }
            if (hashPos > 0) {
                link = link.substring(0, hashPos);
            }
            if (link.startsWith("/")) {
                link = domain + link;
            } else if (!link.contains("://")) {
                link = folder + link;
            }
            if (link.equals(page.getUrl())) {
                continue; // it's probably an anchor link into the same page, otherwise a link to the initial pages
            }

            int slashPos = link.indexOf("/", 8);
            if (slashPos<0)
                continue; // this happens sometimes, e.g. when it is http://www.adobe.com

            if (link.contains("/product.do?") || link.contains("/products.nsf/") ||
                    link.equalsIgnoreCase(homepage1) || link.equalsIgnoreCase(homepage1b) || link.equalsIgnoreCase(homepage2)){
                continue;
            }

            String linkWithoutParameters = link;
            int parametersStart = link.indexOf("?");
            if (parametersStart>=0)
                linkWithoutParameters = linkWithoutParameters.substring(0, parametersStart);
            if (link.contains("/document.do?") || link.endsWith(".html") || link.endsWith(".htm")
                     || linkWithoutParameters.endsWith(".asp") || linkWithoutParameters.endsWith(".aspx")) {
                // check if it is still on the ibm domain
                if (ParseUtils.getUrlDomain(link).contains("ibm.com")) {
                    // it's a link to a page with the same structure as this one
                    // so we can process this recursively
                    processFilesPageOfFirstType(new PostableUrl(link), path);
                }
            } else {
                if (!link.endsWith("/support"))
                    download.downloadFile(link, path);
            }
        }
    }

    private void downloadFileOfSecondType(String url, String path) throws Exception {
        DownloadedPage page = download.getPage(new PostableUrl(url));
        if (Interrupt.get()) {
            return;
        }
        String content = page.getContent();
        String links = ParseUtils.findSubstringByHeaderString(content, "var h=\"", "\";", true);
        StringTokenizer st = new StringTokenizer(links, "|");

        // we only try the first link
        if (st.hasMoreElements()) {
            String link = st.nextToken();
            link = URLDecoder.decode(link, "UTF-8").replaceAll(" ", "%20");
            download.downloadFile(link, path);
        }
    }

    public String getName() {
        return "Lenovo";
    }

    public String getDataFilename() {
        return "lenovo.xml";
    }

    public void correct() {
    }
}
