package driverdownloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to download drivers from Sony
 * Note: this website uses AJAX, but it is easy to handle.
 * @author Mark Van Peteghem
 *
 */
public class SonyDriverDownloader extends DriverDownloader {
    private final String mainPageAddress = "http://esupport.sony.com/US/perl/select-system.pl?DIRECTOR=DRIVER&PRODTYPE=32,33,63,71,102,7,26,80,72,44,55,27,84,74,57,61,103,89,10,35,78,48,77,106,29,50,39,64,12,41,58,81,52,60,66,101,45,73,76,86,62,54,67,70,68,17,2,1,88,100,82,25,28,83,69,59,49,24,104,53,79,22,42,46,13,105,6,85,3,36,9,51,47,38,4,34,37,43,5";

    public String getName() {
        return "Sony";
    }

    public String getDataFilename() {
        return "sony.xml";
    }

    @Override
    public void download(PostableUrl url, String localPath) {
    	try {
	        DownloadedPage page = download.getPage(url);
            if (Interrupt.get()) {
                return;
            }
	        String content = page.getContent();
	        if (!content.contains("There are no files")) {
	            if (content.contains("Download Tips")) {
	                String linksPart = ParseUtils.findSubstringByHeaderString(content, "Download Tips", "Taxi Limitation", false);
	                ArrayList<String> links = ParseUtils.getSubStrings(linksPart, "href=\"/", "\"", false);
	
	                // every link is usually twice on the page,
	                // so we check that we don't do the same one twice
	                String prevLink = "";
	                for (String link: links) {
	                    if (!prevLink.equals(link) && !link.contains("support-info.pl")) {
	                        prevLink = link;
	                        String fileUrl = "http://esupport.sony.com/"+link;
	                        downloadSingleFile(fileUrl, localPath);
	                    }
	                    if (Interrupt.get())
	                    	break;
	                }
	            } else {
	                // we have to select the OS first
	                String osSection = ParseUtils.findSubstringByHeaderString(content, "Select Your Operating System", "</select>", false);
	                ArrayList<String> values = ParseUtils.getSubStrings(osSection, "value=\"", "\"", false);
	                for (String value: values) {
	                    if (!value.isEmpty()) {
	                        download(new PostableUrl(url.getLink()+"&SelectOS="+value), localPath);
	                    }
	                    if (Interrupt.get())
	                    	break;
	                }
	            }
	        }
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
    }

    private void downloadSingleFile(String url, String localPath) {
    	try {
	        DownloadedPage page = download.getPage(new PostableUrl(url));
            if (Interrupt.get()) {
                return;
            }
	        String content1 = page.getContent();
	        if (content1.contains("Download Now")) {
		        String link = ParseUtils.findSubstringByHeaderString(content1, "Download Now", "href=\"", "\"", false);
		        if (link.startsWith("ftp:")) {
		            // sometimes the link on this page is a link to the file on the ftp server
		            download.downloadFile(link, localPath);
		        } else if (link.startsWith("/")) {
		            // sometimes the link goes to another page where a user first has to accept an agreement
		            link = "http://esupport.sony.com"+link;
		            DownloadedPage page2 = download.getPage(new PostableUrl(link));
		            String content2 = page2.getContent();
		            String fileLink = ParseUtils.findSubstringByHeaderString(content2, "Accept agreement", "href=\"", "\"", false);
		            if (fileLink.contains("javascript"))
		                fileLink = ParseUtils.findSubstringByHeaderString(content2, "Accept agreement", "javascript", "href=\"", "\"", false);
		            download.downloadFile(fileLink, localPath);
		        } else if (link.contains("download.sony.com/")) {
		        	// sometimes it is a direct link to the file
		            download.downloadFile(link, localPath);
		        } else {
		            // and sometimes it is a link to another website
		            externalDownload(link, localPath);
		        }
	        }
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
    }

    @Override
    protected Map<String, PostableUrl> getCategories() throws Exception {
        DownloadedPage page = download.getPage(new PostableUrl(mainPageAddress));
        String content = page.getContent();
        
        Map<String, PostableUrl> categories = new HashMap<String, PostableUrl>();

        String selectPart = ParseUtils.findSubstringByHeaderString(content, "Option 3", "<select", "</select", false);
        ArrayList<String> options = ParseUtils.getSubStrings(selectPart, "<option", "</option", false);

        for (String option: options) {
            if (option.contains("value=")) {
                String value = ParseUtils.findSubstringByHeaderString(option, 0, "value=\"", "\"", false);
                String productType = ParseUtils.findSubstringByHeaderString(option, 0, ">", "", false);
                if (value.length()!=0) {
                    downloadObservable.updateFilename(productType);
                    driverData.addProductType(productType);
                    System.out.println("Adding product type "+productType);
                    categories.put(productType,
                        new PostableUrl("http://esupport.sony.com/US/perl/select-page-feed.pl?mdltype_id="+value+"&region_id=1&template_id=1"));
                }
            }
        }
        return categories;
    }

    @Override
	protected void updateCategory(DownloadedPage page) throws Exception {
        String xml = page.getContent();
        // note that the xml also contains series information, but this is only
        // used to allow the user select a subsection; all modelnames of all series follow in the xml
        ArrayList<String> modelNames = ParseUtils.getSubStrings(xml, "<MODELNAME>", "</MODELNAME>", false);

        for (String modelName: modelNames) {
            System.out.println("Adding model "+modelName);
            String modelAddress = "http://esupport.sony.com/US/perl/model-home.pl?mdl="+modelName+"&region_id=1";
            DownloadedPage modelPage = download.getPage(new PostableUrl(modelAddress));
            String content = modelPage.getContent();
            modelAddress = ParseUtils.findLastSubstringBySurroundingStrings(content, "Drivers &amp;", "href=\"", "\"", false);
            driverData.addModelNumber(modelName, "http://esupport.sony.com"+modelAddress);
        }
    }

	@Override
	public void correct() {
		
	}
}
