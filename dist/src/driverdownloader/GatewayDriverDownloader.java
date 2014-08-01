package driverdownloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to download drivers from Gateway
 * Note: this site uses frames, but this is not a big issue.
 * @author Mark Van Peteghem
 *
 */
public class GatewayDriverDownloader extends DriverDownloader {
    private final String mainPageAddress = "http://support.gateway.com/support/drivers/ddaStepMain.asp?Tab=All";

    public String getName() {
        return "Gateway";
    }

    public String getDataFilename() {
        return "gateway.xml";
    }

    @Override
    protected Map<String, PostableUrl> getCategories() throws Exception {
        DownloadedPage page = download.getPage(new PostableUrl(mainPageAddress));
        String content = page.getContent();
        
        Map<String, PostableUrl> categories = new HashMap<String, PostableUrl>();

        int formStart = ParseUtils.safeIndexOf(content, "<form name=\"frmBrowse\"", 0, false);
        int formEnd = ParseUtils.safeIndexOf(content, "</select>", formStart, false);

        int pos = formStart;
        while (true) {
            int optionPos = content.indexOf("<option", pos);
            if (optionPos>formEnd || optionPos<0)
                break;
            String value = ParseUtils.findSubstringByHeaderString(content, optionPos, "value=\"", "\"", false);
            String productType = ParseUtils.findSubstringByHeaderString(content, optionPos, ">", "<", true);
            if (value.length()!=0) {
                downloadObservable.updateFilename(productType);
                categories.put(productType, new PostableUrl(mainPageAddress+"&platform="+value));
            }

            pos = optionPos+7;
        }
        
        return categories;
    }

    @Override
	protected void updateCategory(DownloadedPage page) throws Exception {
        String content = page.getContent();
        String url = page.getUrl();

        String platform = ParseUtils.findSubstringByHeaderString(url, "&platform=", "", false);

        int formStart = ParseUtils.safeIndexOf(content, "<form name=\"frmBrowse\"", 0, false);
        int formEnd = ParseUtils.safeIndexOf(content, "</select>", formStart, false);
        String formSection = content.substring(formStart, formEnd);
        List<String> options = ParseUtils.getSubStrings(formSection, "<option", "</option>", false);

        for (String option: options) {
            String value = ParseUtils.findSubstringByHeaderString(option, "value=\"", "\"", false);
            if (value.length()!=0) {
                String modelName = ParseUtils.findSubstringByHeaderString(option, ">", "", true);
                String newUrl = "http://support.gateway.com/support/drivers/search.asp?ref=step&st=browse&platform="+platform+"&model="+value+"&os=&type=";
                driverData.addModelNumber(modelName, newUrl);
            }

            if (Interrupt.get())
                break;
        }
    }

    public void download(PostableUrl url, String localPath) {
    	try {
	    	String link = url.getLink();
	        int slashPos = link.lastIndexOf("/");
	        String basePath = link.substring(0, slashPos+1);
	
	        DownloadedPage page = download.getPage(url);
	        String content = page.getContent();
	        if (Interrupt.get()) {
	            return;
	        }
	
	        int pos = 0;
	        String prevUrl = "";
	        while (true) {
	            pos = content.indexOf("<a ", pos);
	            if (pos<0)
	                break;
	            String url2 = ParseUtils.findSubstringByHeaderString(content, pos, "href=\"", "\"", false);
	
	            // Every url with a download that we need starts with getFile.asp,
	            // and is twice in the page.
	            if (url2.startsWith("getFile.asp") && !url2.equals(prevUrl)) {
	                prevUrl = url2;
	
	                downloadSingleFile(basePath + url2, localPath);
	                if (Interrupt.get()) {
	                    break;
	                }
	            }
	            pos += 10;
	        }
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
    }

    protected void downloadSingleFile(String url, String localPath) {
    	try {
	        DownloadedPage page = download.getPage(new PostableUrl(url));
	        String content = page.getContent();
	
	        if (Interrupt.get())
	            return;
	
	        String url2 = ParseUtils.findSubstringByHeaderString(content, "href=\"javascript:", "'ftp://", "'", false);
	        url2 = "ftp://"+url2.replaceAll("&amp;", "&");
	
	        download.downloadFile(url2, localPath);
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
    }

	@Override
	public void correct() {
		
	}
}
