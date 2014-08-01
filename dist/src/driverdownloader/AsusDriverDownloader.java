package driverdownloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to download drivers from Asus
 * Note: The website contains some forms that use the POST method to
 *       submit, which makes it a little harder to scrape the pages.
 * @author Mark Van Peteghem
 *
 */
public class AsusDriverDownloader extends DriverDownloader {

	private String homepage = "http://support.asus.com/download/download_right.aspx?SLanguage=en-us";
	
	@Override
	public String getName() {
		return "Asus";
	}

	@Override
	public String getDataFilename() {
		return "asus.xml";
	}

	@Override
	public void download(PostableUrl url, String path) {
		try {
			DownloadedPage page = download.getPage(url);
            if (Interrupt.get()) {
                return;
            }
			String content = page.getContent();
			
			Map<String, String> postData = ParseUtils.getInputs(content, 0);
	
			findAndAddPostBackParams(content, postData);
			
			String osSection = ParseUtils.findSubstringByHeaderString(content, "Select OS</option>", "</select>", false);
			List<String> values = ParseUtils.getSubStrings(osSection, "value=\"", "\"", false);
			for (String value: values) {
				postData.put("ddlOS", value);
				DownloadedPage osPage = download.getPage(new PostableUrl(url.getLink(), postData));
	            if (Interrupt.get()) {
	                break;
	            }

	            downloadForOs(osPage, path);
	            
				if (Interrupt.get())
	            	break;
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}

	private void downloadForOs(DownloadedPage page, String path) {
		try {
			String content = page.getContent();
			
			Map<String, String> postData = ParseUtils.getInputs(content, 0);
	
			List<String> postBackParamSets = ParseUtils.getSubStrings(content, ":__doPostBack(", "\"", false);
			for (String postBackParams: postBackParamSets) {
				addPostBackParams(postData, postBackParams);
				DownloadedPage sectionPage = download.getPage(new PostableUrl(page.getUrl(), postData));
	            if (Interrupt.get()) {
	                break;
	            }
	            
				downloadFiles(sectionPage, path);
				if (Interrupt.get())
	            	break;
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}

	private void downloadFiles(DownloadedPage page, String path) {
		try {
			String content = page.getContent();
			
			List<String> downloadCalls = ParseUtils.getSubStrings(content, "dlm_click(", ");\"", false);
			for (String downloadCall: downloadCalls) {
				List<String> params = ParseUtils.getSubStrings(downloadCall, "'", "'", false);
				if (params.size()<9)
					throw new UnexpectedFormatException();
				String baseLink;
				if (params.get(8).equals("1"))
					baseLink = "http://dlcdnas.asus.com/pub/ASUS/";
				else {
					String domain = ParseUtils.findSubstringByHeaderString(content, "name=\"hld_AkamaiURL\"", "value=\"", "\"", false);
					baseLink = "http://"+domain+"/pub/ASUS/";
				}
				String link = baseLink+params.get(1);
				download.downloadFile(link, path);
	
				if (Interrupt.get())
	            	break;
			}
    	} catch (Exception ex) {
    		MessageHandler.addError(ex);
    	}
	}
	
	private void findAndAddPostBackParams(String content, Map<String, String> postData)
			throws UnexpectedFormatException {
		String postBackCall = ParseUtils.findSubstringByHeaderString(content, "onchange=\"__doPostBack", "\"", false);
		addPostBackParams(postData, postBackCall);
	}

	private void addPostBackParams(Map<String, String> postData,
			String postBackCall) throws UnexpectedFormatException {
		List<String> params = ParseUtils.getSubStrings(postBackCall, "'", "'", false);
		if (2!=params.size())
			throw new UnexpectedFormatException();
		String eventTarget = params.get(0).replaceAll("\\$", ":");
		String eventArgument = params.get(1);
		postData.put("__EVENTTARGET", eventTarget);
		postData.put("__EVENTARGUMENT", eventArgument);
	}

	@Override
	protected Map<String, PostableUrl> getCategories() throws Exception {
		Map<String, PostableUrl> categories = new HashMap<String, PostableUrl>();
		
		DownloadedPage page = download.getPage(new PostableUrl(homepage));
        if (Interrupt.get()) {
            return null;
        }
		String content = page.getContent();
		
		Map<String, String> post = new HashMap<String, String>();
		
        String form = ParseUtils.findSubstringByHeaderString(content, "<form", "</form>", false);
        List<String> inputs = ParseUtils.getSubStrings(form, "<input ", ">", false);
        for (String input: inputs) {
        	String name = ParseUtils.findSubstringByHeaderString(input, "name=\"", "\"", false);
        	if (!name.equals("btn_search"))
        	{
	        	String value = ParseUtils.findSubstringByHeaderString(input, "value=\"", "\"", false);
	        	post.put(name, value);
        	}
        }
        
        String productTypeSelection = ParseUtils.findSubstringByHeaderString(content, "<select", "</select>", false);
        List<String> options = ParseUtils.getSubStrings(productTypeSelection, "<option", "/option>", false);
        for (String option: options) {
        	String value = ParseUtils.findSubstringByHeaderString(option, "value=\"", "\"", false);
            if (!value.isEmpty() && !value.equals("0")) {
            	String name = ParseUtils.findSubstringByHeaderString(option, ">", "<", false);
            	String productLink = "http://support.asus.com/download/model_list.aspx?product="+value+"&SLanguage=en-us";
            	categories.put(name, new PostableUrl(productLink));
            }
        }
        return categories;
	}
	
	@Override
	protected void updateCategory(DownloadedPage page) throws Exception {
		String link = page.getUrl();
		int slashPos = link.lastIndexOf("/");
		String baseLink = page.getUrl().substring(0, slashPos+1);

		String content = page.getContent();
		String linksSection = ParseUtils.findSubstringByHeaderString(content, ">Series", "", false);
		List<String> modelLinks = ParseUtils.getSubStrings(linksSection, "href=\"JavaScript", "</a>", false);
		for (String modelLink: modelLinks) {
			String jsLink = ParseUtils.findSubstringByHeaderString(modelLink, ":", "\"", false);
			String name = ParseUtils.findSubstringByHeaderString(modelLink, ">", "", false);
			List<String> params = ParseUtils.getSubStrings(jsLink, "&quot;", "&quot", false);
			if (2!=params.size())
				throw new UnexpectedFormatException();
			String productType = params.get(0);
			String model       = params.get(1);
			if (!model.isEmpty()) {
				String modelUrl = baseLink+"download_item_dna.aspx?product="+productType+"&model="+model+"&type=Map&SLanguage=en-us";
				driverData.addModelNumber(name, modelUrl);
			}
		}
	}
	
	@Override
	public void correct() {
		
	}
}
