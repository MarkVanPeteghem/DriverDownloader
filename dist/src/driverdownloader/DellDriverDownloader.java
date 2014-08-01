package driverdownloader;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to download drivers from Dell
 * Note: this website uses AJAX, but it is easy to handle.
 * @author Mark Van Peteghem
 *
 */
public class DellDriverDownloader extends DriverDownloader {

	private final String homepage = "http://support.dell.com/support/downloads/index.aspx";
	
	@Override
	public String getName() {
		return "Dell";
	}

	@Override
	public String getDataFilename() {
		return "dell.xml";
	}

	@Override
	public void download(PostableUrl url, String path) {
		try {
			DownloadedPage page = download.getPage(url);
            if (Interrupt.get()) {
                return;
            }
			String content = page.getContent();
			
			String modelLink = page.getUrl();
			int osStart = modelLink.indexOf("&os=");
			int osEnd = modelLink.indexOf("&", osStart+3);
			if (osStart<0 || osEnd<0)
				throw new UnexpectedFormatException();
			String protoLink1 = modelLink.substring(0, osStart+4);
			String protoLink2 = modelLink.substring(osEnd);
			
			String osSection = ParseUtils.findSubstringByHeaderString(content, "Operating System:", "<select", "</select>", false);
			List<String> oses = ParseUtils.getSubStrings(osSection, "value=\"", "\"", false);
			for (String os: oses) {
				String osLink = protoLink1+os+protoLink2;
				downloadForOs(osLink, path);
	            if (Interrupt.get()) {
	                return;
	            }
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}			
	}
	
	private void downloadForOs(String osLink, String path) {
		try {
			DownloadedPage page = download.getPage(new PostableUrl(osLink));
            if (Interrupt.get()) {
                return;
            }
			String content = page.getContent();
			
			List<String> links = ParseUtils.getSubStrings(content, "href=\"", "\"", false);
			String prevLink = "";
			String ignoreEnd1 = "&checkFormat=true";
			String ignoreEnd2 = "&checkFormat=false";
			for (String link: links) {
				if (!link.startsWith("java") && link.contains("/downloads/format")) {
					if (link.endsWith(ignoreEnd1))
						link = link.substring(0, link.length()-ignoreEnd1.length());
					if (link.endsWith(ignoreEnd2))
						link = link.substring(0, link.length()-ignoreEnd2.length());
					if (!link.equals(prevLink)) {
						downloadFile(link, path);
						prevLink = link;
					}
				}
	            if (Interrupt.get()) {
	                return;
	            }
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}			
	}

	private void downloadFile(String filePageLink, String path) {
		try {
			DownloadedPage page = download.getPage(new PostableUrl(filePageLink));
			String content = page.getContent();
            if (Interrupt.get()) {
                return;
            }
			
			if (page.getUrl().contains("/format.aspx")) {
				// we are not redirected, so get the link in the page and open that page
				List<String> links = ParseUtils.getSubStrings(content, "href=\"", "\"", false);
				List<String> goodLinks = new ArrayList<String>();
				for (String link: links) {
					if (link.contains("/download.aspx"))
						goodLinks.add(link);
				}
				if (goodLinks.isEmpty())
					throw new UnexpectedFormatException();
				String link = goodLinks.get(goodLinks.size()-1);
				if (link.startsWith("/"))
					link = ParseUtils.getUrlDomain(filePageLink)+link;
				
				page = download.getPage(new PostableUrl(link));
	            if (Interrupt.get()) {
	                return;
	            }
				content = page.getContent();
			}
			
			String fileLink = ParseUtils.findSubstringByHeaderString(content, "javascript:downloadslink('", "'", false);
			fileLink = fileLink.replaceAll(" ", "%20");
			download.downloadFile(fileLink, path);
		} catch (Exception ex) {
			MessageHandler.addError(ex);
		}		
	}
	
	@Override
	protected Map<String, PostableUrl> getCategories() throws Exception {
		
		Map<String, PostableUrl> categories = new HashMap<String, PostableUrl>();
		
		DownloadedPage page = download.getPage(new PostableUrl(homepage));
		String content = page.getContent();
		String productTypesSection = ParseUtils.findSubstringByHeaderString(content, "\"ProductLobs\"", "</div>", false);
		List<String> productTypes = ParseUtils.getSubStrings(productTypesSection, "tier1(", ");", false);
		for (String productType: productTypes) {
			List<String> productTypeParts = ParseUtils.getSubStrings(productType, "'", "'", true);
			if (productTypeParts.size()!=2)
				throw new UnexpectedFormatException();
			String id = productTypeParts.get(0);
			String name = productTypeParts.get(1);
			String link = "http://support.dell.com/support/BPSAjax.aspx?c=us&l=en&s=gen&select=families&Product_GroupId="+id+"&_=";
			categories.put(name, new PostableUrl(link));
		}
		
		return categories;
	}
	
	@Override
	protected void updateCategory(DownloadedPage page) throws Exception {
		String content = page.getContent();
		String protoLink = page.getUrl().replace("select=families", "select=models");
		
		List<String> subCategories = ParseUtils.getSubStrings(content, "getFamilySystems(", ");", true);
		for (String subCategory: subCategories) {
			List<String> subCategoryParts = ParseUtils.getSubStrings(subCategory, "'", "'", true);
			if (subCategoryParts.size()!=3)
				throw new UnexpectedFormatException();
			String name = subCategoryParts.get(1);
			
			String link = protoLink+"&product_family="+URLEncoder.encode(name, "UTF-8");
			updateSubCategory(link);
		}
	}
	
	private void updateSubCategory(String link) throws Exception {
		DownloadedPage page = download.getPage(new PostableUrl(link));
		String content = page.getContent();

		List<String> subCategories = ParseUtils.getSubStrings(content, "tier3(", ");", false);
		for (String subCategory: subCategories) {
			List<String> productTypeParts = ParseUtils.getSubStrings(subCategory, "'", "'", true);
			if (productTypeParts.size()!=3)
				throw new UnexpectedFormatException();
			String systemId = productTypeParts.get(0);
			String name = productTypeParts.get(1);
			String modelLink = "http://support.dell.com/support/downloads/driverslist.aspx?c=us&l=en&s=gen&ServiceTag=&SystemID="+systemId+"&os=W764&osl=en&catid=&impid=";
			driverData.addModelNumber(name, modelLink);
		}
	}
	
	public void correct() {
		
	}
}
